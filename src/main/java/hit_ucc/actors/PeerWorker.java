package hit_ucc.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hit_ucc.HitUCCPeerHostSystem;
import hit_ucc.HitUCCPeerSystem;
import hit_ucc.actors.messages.*;
import hit_ucc.behaviour.dictionary.DictionaryEncoder;
import hit_ucc.behaviour.dictionary.IColumn;
import hit_ucc.behaviour.differenceSets.BucketingCalculateMinimalSetsStrategy;
import hit_ucc.behaviour.differenceSets.DifferenceSetDetector;
import hit_ucc.behaviour.differenceSets.HashAddDifferenceSetStrategy;
import hit_ucc.behaviour.differenceSets.TwoSidedMergeMinimalSetsStrategy;
import hit_ucc.behaviour.oracle.HittingSetOracle;
import hit_ucc.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

public class PeerWorker extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());
	private final boolean useDictionaryEncoding = true;
	long treeSearchStart = 0;
	private Random random = new Random();
	private List<ActorRef> colleagues = new ArrayList<>();
	private ActorRef dataBouncer = null;
	private List<WorkerState> colleaguesStates = new ArrayList<>();
	private WorkerState selfState = WorkerState.NOT_STARTED;
	private int columnCount = 0;
	private int maxLocalTreeDepth = 10;
	private int currentLocalTreeDepth = 0;
	private BitSet[] minimalDifferenceSets = new BitSet[0];
	private List<BitSet> discoveredUCCs = new ArrayList<>();
	private DifferenceSetDetector differenceSetDetector;
	private List<TreeSearchNode> treeSearchNodes = new ArrayList<>();
	private TreeSearchNode currentTreeNode;
	private long currentTreeNodeId = 0;

	private Batches batches;
	private List<SingleDifferenceSetTask> tasks;
	private boolean nullEqualsNull = false;

	public static Props props() {
		return Props.create(PeerWorker.class);
	}

	private void createDifferenceSetDetector() {
		differenceSetDetector = new DifferenceSetDetector(new HashAddDifferenceSetStrategy(), new BucketingCalculateMinimalSetsStrategy(columnCount), new TwoSidedMergeMinimalSetsStrategy());
//		differenceSetDetector = new DifferenceSetDetector(new JustAddSortDifferenceSetStrategy(), new BucketingCalculateMinimalSetsStrategy(columnCount), new TwoSidedMergeMinimalSetsStrategy());
	}

	@Override
	public void preStart() {
		this.cluster.subscribe(this.self(), MemberUp.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	private boolean otherHasPriority() {
		return otherHasPriority(this.sender());
	}

	private boolean otherHasPriority(ActorRef other) {
		if (this.self().toString().equals(other.toString())) {
			this.log.error("Actor String is not unique >.<");
		}

		return this.self().toString().compareTo(other.toString()) < 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CurrentClusterState.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(FindDifferenceSetFromBatchMessage.class, this::handle)
				.match(SendDataBatchMessage.class, this::handle)
				.match(AskForMergeMessage.class, this::handle)
				.match(AcceptMergeMessage.class, this::handle)
				.match(DeclineMergeMessage.class, this::handle)
				.match(WorkerStateChangedMessage.class, this::handle)
				.match(MergeDifferenceSetsMessage.class, this::handle)
				.match(SyncDifferenceSetsMessage.class, this::handle)
				.match(TreeNodeWorkMessage.class, this::handle)
				.match(TreeNodeFulfilledMessage.class, this::handle)
				.match(UCCDiscoveredMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private boolean isValidMember(Member member) {
		return member.hasRole(HitUCCPeerHostSystem.PEER_HOST_ROLE) || member.hasRole(HitUCCPeerSystem.PEER_ROLE);
	}

	private void register(Member member) {
		if (isValidMember(member)) {
			this.log.info("Register {}", member.address().toString());
			this.getContext()
					.actorSelection(member.address() + "/user/*")
					.tell(new RegistrationMessage(), this.self());
		}
	}

	private void handle(CurrentClusterState message) {
		message.getMembers().forEach(member -> {
			if (member.status().equals(MemberStatus.up())) {
				register(member);
			}
		});
	}

	private void handle(RegistrationMessage message) {
		if (this.sender().equals(this.self())) return;
		if (colleagues.contains(this.sender())) return;

		this.context().watch(this.sender());

		if (this.sender().path().name().equals(PeerDataBouncer.DEFAULT_NAME)) {
			dataBouncer = this.sender();
			this.log.info("Registered {} DataBouncer", this.sender().path().name());
		} else {
			colleagues.add(this.sender());
			colleaguesStates.add(WorkerState.NOT_STARTED);
			this.log.info("Registered {}; {} registered colleagues", this.sender().path().name(), colleagues.size());
		}

		this.sender().tell(new RegistrationMessage(), this.self());
	}

	private void handle(FindDifferenceSetFromBatchMessage message) {
		if (selfState != WorkerState.DISCOVERING_DIFFERENCE_SETS) {
			broadcastAndSetState(WorkerState.DISCOVERING_DIFFERENCE_SETS);
		}

		batches = new Batches(message.getBatchCount());
		tasks = message.getDifferenceSetTasks();
		nullEqualsNull = message.isNullEqualsNull();

		tryToFindDifferenceSets();
	}

	private void handle(SendDataBatchMessage message) {
		batches.setBatch(message.getBatchIdentifier(), message.getBatch());
		tryToFindDifferenceSets();
	}

	private void tryToFindDifferenceSets() {
		if (tasks.size() == 0) {
			broadcastAndSetState(WorkerState.DONE_MERGING);
			return;
		}

		SingleDifferenceSetTask currentTask = tasks.get(0);
		if (!batches.hasBatch(currentTask.getSetA())) {
			dataBouncer.tell(new RequestDataBatchMessage(currentTask.getSetA()), this.self());
			return;
		}
		if (!batches.hasBatch(currentTask.getSetB())) {
			dataBouncer.tell(new RequestDataBatchMessage(currentTask.getSetB()), this.self());
			return;
		}

		findDifferenceSets();
	}

	private void findDifferenceSets() {
		if (tasks.size() == 0) {
			broadcastAndSetState(WorkerState.DONE_MERGING);
			return;
		}

		SingleDifferenceSetTask currentTask = tasks.get(0);
		columnCount = batches.getBatch(currentTask.getSetA()).get(0).values.length;
		if (differenceSetDetector == null) createDifferenceSetDetector();

		if (useDictionaryEncoding) {
			int rowCount = batches.getBatch(currentTask.getSetA()).size() + (currentTask.getSetA() != currentTask.getSetB() ? batches.getBatch(currentTask.getSetB()).size() : 0);
			Row[] rows = new Row[rowCount];
			for (int i = 0; i < batches.getBatch(currentTask.getSetA()).size(); i++) {
				rows[i] = batches.getBatch(currentTask.getSetA()).get(i);
			}
			if (currentTask.getSetA() != currentTask.getSetB()) {
				for (int i = 0; i < batches.getBatch(currentTask.getSetB()).size(); i++) {
					rows[i + batches.getBatch(currentTask.getSetA()).size()] = batches.getBatch(currentTask.getSetB()).get(i);
				}
			}

			DictionaryEncoder[] encoder = new DictionaryEncoder[columnCount];
//			DictionaryEncoder encoder[] = new BitCompressedDictionaryEncoder[columnCount];
			for (int i = 0; i < columnCount; i++) encoder[i] = new DictionaryEncoder(rows.length);
			for (Row row : rows) {
				for (int columnIndex = 0; columnIndex < row.values.length; columnIndex++) {
					encoder[columnIndex].addValue(row.values[columnIndex]);
				}
			}
			int[][] intRows = new int[rows.length][columnCount];
			for (int i = 0; i < columnCount; i++) {
				IColumn column = encoder[i].getColumn();
				for (int rowIndex = 0; rowIndex < column.size(); rowIndex++) {
					intRows[rowIndex][i] = column.getValue(rowIndex);
				}
			}
			this.log.info("Dictionary Encoded Data");

			if (currentTask.getSetA() == currentTask.getSetB()) {
				for (int indexA = 0; indexA < rows.length; indexA++) {
					for (int indexB = indexA + 1; indexB < rows.length; indexB++) {
						differenceSetDetector.addDifferenceSet(intRows[indexA], intRows[indexB], nullEqualsNull);
					}
				}
			} else {
				int firstBatchSize = batches.getBatch(currentTask.getSetA()).size();
				for (int indexA = 0; indexA < firstBatchSize; indexA++) {
					for (int indexB = firstBatchSize; indexB < rows.length; indexB++) {
						differenceSetDetector.addDifferenceSet(intRows[indexA], intRows[indexB], nullEqualsNull);
					}
				}
			}

		} else {
			if (currentTask.getSetA() == currentTask.getSetB()) {
				List<Row> batch = batches.getBatch(currentTask.getSetA());
				for (int indexA = 0; indexA < batch.size(); indexA++) {
					for (int indexB = indexA + 1; indexB < batch.size(); indexB++) {
						differenceSetDetector.addDifferenceSet(batch.get(indexA).values, batch.get(indexB).values, nullEqualsNull);
					}
				}
			} else {
				List<Row> batchA = batches.getBatch(currentTask.getSetA());
				List<Row> batchB = batches.getBatch(currentTask.getSetB());
				for (Row rowA : batchA) {
					for (Row rowB : batchB) {
						differenceSetDetector.addDifferenceSet(rowA.values, rowB.values, nullEqualsNull);
					}
				}
			}
		}

		minimalDifferenceSets = differenceSetDetector.getMinimalDifferenceSets();
		this.log.info("Calculated {} minimal difference sets | Batch[{}|{}]", minimalDifferenceSets.length, currentTask.getSetA(), currentTask.getSetB());

		if (tasks.size() == 1) {
			this.self().tell(new WorkerStateChangedMessage(WorkerState.READY_TO_MERGE), this.self());
		} else {
			tasks.remove(0);
			this.self().tell(new FindDifferenceSetFromBatchMessage(tasks, batches.count(), nullEqualsNull), this.self());
		}
	}

	private void handle(WorkerStateChangedMessage message) {
//		this.log.info("Received New State Message {}", message.state);

		for (int i = 0; i < colleaguesStates.size(); i++) {
			if (colleagues.get(i).equals(this.sender())) {
				colleaguesStates.set(i, message.getState());
			}
		}

		if (this.self().equals(this.sender())) {
			if (selfState != WorkerState.TREE_TRAVERSAL || message.getState() != WorkerState.READY_TO_MERGE) {
				selfState = message.getState();
			}
		}

		if (selfState == WorkerState.READY_TO_MERGE && message.getState() == WorkerState.READY_TO_MERGE) {
			tryToMerge();
		}

		if (selfState == WorkerState.READY_TO_MERGE) {
			boolean finishedMerge = true;
			for (WorkerState colleaguesState : colleaguesStates) {
				if (colleaguesState != WorkerState.DONE_MERGING && colleaguesState != WorkerState.NOT_STARTED) {
					finishedMerge = false;
				}
			}
			if (finishedMerge) {
				selfState = WorkerState.TREE_TRAVERSAL;
				this.log.info("Finished Merging!");

				this.log.info("Found following minimal difference sets:");
				for (BitSet differenceSet : minimalDifferenceSets) {
					log.info(DifferenceSetDetector.bitSetToString(differenceSet));
				}

				for (ActorRef worker : colleagues) {
					worker.tell(new SyncDifferenceSetsMessage(minimalDifferenceSets, columnCount), this.self());
				}

				this.log.info("Start Tree Search");
				treeSearchStart = System.currentTimeMillis();
				BitSet x = new BitSet(columnCount);
				BitSet y = new BitSet(columnCount);
				addRootTreeSearchNode();
				addChildToTreeSearchNode();
				this.self().tell(new TreeNodeWorkMessage(x, y, 0, minimalDifferenceSets, columnCount, currentTreeNodeId), this.self());
			}
		}

		if (selfState == WorkerState.DONE && message.getState() == WorkerState.DONE) {
			boolean finished = true;
			for (WorkerState colleaguesState : colleaguesStates) {
				if (colleaguesState != WorkerState.DONE) {
					finished = false;
				}
			}
			if (finished) {
				this.log.info("Finished Collecting {} UCCs from table!", discoveredUCCs.size());
			}
		}
	}

	private void handle(SyncDifferenceSetsMessage message) {
		minimalDifferenceSets = message.differenceSets;
		columnCount = message.columnsInTable;
	}

	private void tryToMerge() {
		List<ActorRef> waitingWorkers = new ArrayList<>();
		for (int i = 0; i < colleagues.size(); i++) {
			if (colleaguesStates.get(i) == WorkerState.READY_TO_MERGE && !otherHasPriority(colleagues.get(i))) {
				waitingWorkers.add(colleagues.get(i));
			}
		}

		if (waitingWorkers.size() > 0) {
			ActorRef randomRef = waitingWorkers.get(random.nextInt(waitingWorkers.size()));
			randomRef.tell(new AskForMergeMessage(), this.self());
			broadcastAndSetState(WorkerState.WAITING_FOR_MERGE);
		} else {
			broadcastAndSetState(WorkerState.READY_TO_MERGE);
		}
	}

	private void handle(AskForMergeMessage message) {
		this.log.info("Received Ask for Merge Message from {}", this.sender().path().name());
		// TODO test whether 'selfState == WorkerState.READY_TO_MERGE' is enough of if we need 'selfState == WorkerState.WAITING_FOR_MERGE' as well
		if (selfState == WorkerState.READY_TO_MERGE || selfState == WorkerState.WAITING_FOR_MERGE) {
			this.sender().tell(new AcceptMergeMessage(), this.self());
			broadcastAndSetState(WorkerState.ACCEPTED_MERGE);
		} else {
			this.sender().tell(new DeclineMergeMessage(), this.self());
		}
	}

	private void handle(AcceptMergeMessage message) {
		if (selfState != WorkerState.WAITING_FOR_MERGE) {
			this.log.info("Received Accept Merge Message but are are not waiting for an accept from {}", this.sender().path().name());
			this.sender().tell(new DeclineMergeMessage(), this.self());
			return;
		}

		this.log.info("Received Accept Merge Message from {}", this.sender().path().name());
		this.sender().tell(new MergeDifferenceSetsMessage(minimalDifferenceSets), this.self());
		broadcastAndSetState(WorkerState.DONE_MERGING);
	}

	private void handle(DeclineMergeMessage message) {
		this.log.info("Received Decline Merge Message from {}", this.sender().path().name());
		if (selfState == WorkerState.READY_TO_MERGE || selfState == WorkerState.WAITING_FOR_MERGE || selfState == WorkerState.ACCEPTED_MERGE) {
			tryToMerge();
		}
	}

	private void handle(MergeDifferenceSetsMessage message) {
//		if(differenceSetDetector == null) createDifferenceSetDetector();
		broadcastAndSetState(WorkerState.MERGING);
		this.log.info("Received Merge Message from {}", this.sender().path().name());

		minimalDifferenceSets = differenceSetDetector.mergeMinimalDifferenceSets(message.differenceSets);
		this.log.info("Merged into {} difference sets", minimalDifferenceSets.length);

		broadcastAndSetState(WorkerState.READY_TO_MERGE);
	}

	private void addRootTreeSearchNode() {
		currentTreeNode = new TreeSearchNode(this.sender(), ++currentTreeNodeId);
		treeSearchNodes.add(currentTreeNode);
	}

	private void addNewTreeSearchNode(long nodeId) {
		currentTreeNode = new TreeSearchNode(nodeId, this.sender(), ++currentTreeNodeId);
		treeSearchNodes.add(currentTreeNode);
	}

	private void addChildToTreeSearchNode() {
		currentTreeNode.addChild();
	}

	private TreeSearchNode childFulfilledTreeSearchNode(long nodeId) {
		for (TreeSearchNode node : treeSearchNodes) {
			node.childFinished(nodeId);
			if (node.isFulfilled()) return node;
		}

		return null;
	}

	private void fulfillCurrentTreeNode() {
		if (this.currentTreeNode.isFulfilled()) {
			treeSearchNodes.remove(currentTreeNode);

			currentTreeNode.getParent().tell(new TreeNodeFulfilledMessage(currentTreeNode.getParentNodeId()), this.self());
		}
	}

	private void handle(TreeNodeFulfilledMessage message) {
		currentTreeNode = childFulfilledTreeSearchNode(message.nodeId);

		if (currentTreeNode == null) return;
		if (currentTreeNode.isRoot()) {
			this.log.info("Finished UCC discovery!");

			dataBouncer.tell(new ReportAndShutdownMessage(), this.self());
//			for (ActorRef actorRef : colleagues) {
//				actorRef.tell(new ReportAndShutdownMessage(), this.self());
//			}
//			this.self().tell(new ReportAndShutdownMessage(), this.self());
//			getContext().getSystem().terminate();
		} else {
			fulfillCurrentTreeNode();
		}

	}

	private void handle(TreeNodeWorkMessage message) {
		if (selfState != WorkerState.TREE_TRAVERSAL) broadcastAndSetState(WorkerState.TREE_TRAVERSAL);

		currentLocalTreeDepth = 1;
		addNewTreeSearchNode(message.nodeId);

		BitSet x = message.getX();
		BitSet y = message.getY();
		int length = message.getLength();
		BitSet[] differenceSets = message.getDifferenceSets();

		handleLocal(x, y, length, differenceSets);
	}

	private void handleLocal(BitSet x, BitSet y, int length, BitSet[] differenceSets) {
		currentLocalTreeDepth += 1;

		HittingSetOracle.Status result = HittingSetOracle.extendable(x, y, length, differenceSets, columnCount);
		switch (result) {
			case MINIMAL:
				this.report(x);
				fulfillCurrentTreeNode();
				break;
			case EXTENDABLE:
				this.split(x, y, length, differenceSets);
				break;
			case NOT_EXTENDABLE:
				fulfillCurrentTreeNode();
				// Ignore
				break;
			case FAILED:
				fulfillCurrentTreeNode();
				this.log.error("Oracle failed :(");
				break;
		}
	}

	private void report(BitSet ucc) {
//		this.log.info("SET {}", DifferenceSetDetector.bitSetToString(ucc, columnCount));
//		this.log.info("UCC: {}", toUCC(ucc));

		discoveredUCCs.add(ucc);
		for (ActorRef worker : colleagues) {
			worker.tell(new UCCDiscoveredMessage(ucc), this.self());
		}
	}

	private void split(BitSet x, BitSet y, int next, BitSet[] differenceSets) {
		if (next < columnCount) {
			BitSet xNew = copyBitSet(x, next);
			xNew.set(next);
			ActorRef randomRef = getRandomColleague();
			addChildToTreeSearchNode();
			randomRef.tell(new TreeNodeWorkMessage(xNew, y, next + 1, minimalDifferenceSets, columnCount, currentTreeNodeId), this.self());

			BitSet yNew = copyBitSet(y, next);
			yNew.set(next);
			if (currentLocalTreeDepth >= maxLocalTreeDepth) {
				randomRef = getRandomColleague();
				addChildToTreeSearchNode();
				randomRef.tell(new TreeNodeWorkMessage(x, yNew, next + 1, minimalDifferenceSets, columnCount, currentTreeNodeId), this.self());
			} else {
				handleLocal(x, yNew, next + 1, minimalDifferenceSets);
			}
		} else {
			this.log.info("WHY IS THIS? ################################### This is not an error - just wanted to check if this branch can actually be reached ;)");
			fulfillCurrentTreeNode();
		}
	}

	private ActorRef getRandomColleague() {
		if (colleagues.size() > 0) return colleagues.get(random.nextInt(colleagues.size()));

		return this.self();
	}

	private void handle(UCCDiscoveredMessage message) {
		discoveredUCCs.add(message.ucc);
	}

	private void handle(ReportAndShutdownMessage message) {
		if (treeSearchStart != 0) {
			this.log.info("Tree Search Cost: {}", System.currentTimeMillis() - treeSearchStart);
		}

		for (BitSet ucc : discoveredUCCs) {
//			this.log.info("UCC: {}", toUCC(ucc));
		}
		this.log.info("Discovered {} UCCs", discoveredUCCs.size());

		this.getContext().stop(this.self());
	}

	private BitSet copyBitSet(BitSet set, int newLength) {
		BitSet copy = new BitSet(newLength);
		for (int i = 0; i < set.length(); i++) {
			if (set.get(i)) copy.set(i);
		}

		return copy;
	}

	private String toUCC(BitSet bitSet) {
		if (bitSet.length() == 0) return "";

		String output = "";
		for (int i = 0; i < bitSet.length() - 1; i++) {
			if (bitSet.get(i)) {
				output += i + ", ";
			}
		}
		if (bitSet.get(bitSet.length() - 1)) {
			output += (bitSet.length() - 1) + ", ";
		}
		return output;
	}

	private void broadcastAndSetState(WorkerState state) {
		selfState = state;
		broadcastState(state);
	}

	private void broadcastState(WorkerState state) {
		for (ActorRef worker : colleagues) {
			worker.tell(new WorkerStateChangedMessage(state), this.self());
		}
	}

	@Data
	@AllArgsConstructor
	private static class AskForMergeMessage implements Serializable {
		private static final long serialVersionUID = 2914610592052201337L;
	}

	@Data
	@AllArgsConstructor
	private static class AcceptMergeMessage implements Serializable {
		private static final long serialVersionUID = 1238901023948721337L;
	}

	@Data
	@AllArgsConstructor
	private static class DeclineMergeMessage implements Serializable {
		private static final long serialVersionUID = 2110462002134951337L;
	}

	@Data
	@AllArgsConstructor
	private static class MergeDifferenceSetsMessage implements Serializable {
		private static final long serialVersionUID = 2192568355722201337L;
		private BitSet[] differenceSets;

		private MergeDifferenceSetsMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private static class SyncDifferenceSetsMessage implements Serializable {
		private static final long serialVersionUID = 7331387404648201337L;
		private BitSet[] differenceSets;
		private int columnsInTable;

		private SyncDifferenceSetsMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private static class UCCDiscoveredMessage implements Serializable {
		private static final long serialVersionUID = 997981649989901337L;
		private BitSet ucc;

		private UCCDiscoveredMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private class TreeNodeWorkMessage implements Serializable {
		private static final long serialVersionUID = 2360129506196901337L;
		private BitSet x;
		private BitSet y;
		private int length;
		private BitSet[] differenceSets;
		private int numAttributes;

		private long nodeId;

		private TreeNodeWorkMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private class TreeNodeFulfilledMessage implements Serializable {
		private static final long serialVersionUID = 1085642539643901337L;

		private long nodeId;

		private TreeNodeFulfilledMessage() {
		}
	}
}
