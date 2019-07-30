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
import hit_ucc.actors.messages.FindDifferenceSetFromBatchMessage;
import hit_ucc.actors.messages.FindDifferenceSetFromBatchSplitMessage;
import hit_ucc.actors.messages.TaskMessage;
import hit_ucc.behaviour.differenceSets.BucketingCalculateMinimalSetsStrategy;
import hit_ucc.behaviour.differenceSets.DifferenceSetDetector;
import hit_ucc.behaviour.differenceSets.HashAddDifferenceSetStrategy;
import hit_ucc.behaviour.differenceSets.TwoSidedMergeMinimalSetsStrategy;
import hit_ucc.behaviour.oracle.HittingSetOracle;
import hit_ucc.model.Row;
import hit_ucc.model.TreeSearchNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

public class PeerWorker extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());

	private Random random = new Random();

	private List<ActorRef> colleagues = new ArrayList<>();
	private List<WorkerState> colleaguesStates = new ArrayList<>();

	private WorkerState selfState = WorkerState.NOT_STARTED;
	private int splitCount = 0;
	private int columnsInTable = 0;
	private int maxLocalTreeDepth = 10;
	private int currentLocalTreeDepth = 0;

	private BitSet[] minimalDifferenceSets = new BitSet[0];
	private List<BitSet> discoveredUCCs = new ArrayList<>();

	private DifferenceSetDetector differenceSetDetector;

	private List<TreeSearchNode> treeSearchNodes = new ArrayList<>();
	private TreeSearchNode currentTreeNode;
	private long currentTreeNodeId = 0;

	private TaskMessage task;

	public static Props props() {
		return Props.create(PeerWorker.class);
	}

	private void createDifferenceSetDetector() {
		differenceSetDetector = new DifferenceSetDetector(new HashAddDifferenceSetStrategy(), new BucketingCalculateMinimalSetsStrategy(columnsInTable), new TwoSidedMergeMinimalSetsStrategy());
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
				.match(TaskMessage.class, this::handle)
				.match(FindDifferenceSetFromBatchMessage.class, this::handle)
				.match(FindDifferenceSetFromBatchSplitMessage.class, this::handle)
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
			this.log.info(member.address().toString());
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

		colleagues.add(this.sender());
		colleaguesStates.add(WorkerState.NOT_STARTED);
		this.log.info("Registered {}; {} registered colleagues", this.sender().path().name(), colleagues.size());
		this.sender().tell(new RegistrationMessage(), this.self());

//		if (task != null) handle(task);
	}

	private void handle(TaskMessage task) {
		this.task = task;
//		if (colleagues.size() < 1) return;
		this.log.info("Received Task Message with table of size [row: {}, columns: {}]", task.getInputFile().length, task.getInputFile()[0].length);

		String[][] table = task.getInputFile();
		int anchorCount = task.getDataDuplicationFactor();

		if (anchorCount <= 1) {
			if (anchorCount < 1) {
				this.log.error("Data Duplication Factor is lower than 1. Can't discover UCCs without without a single data batch. Factor is set to 1");
			}
			anchorCount = 1;
			this.log.info("The Data Duplication Factor is set to 1. The program therefore cannot distribute the algorithm. This is not that bad, but it slows down the execution time significantly.");
		}

		int batchCount = 0;
		for (int i = 0; i < anchorCount; i++) {
			batchCount += i + 1;
		}

		this.log.info("Divided data into {} batches", batchCount);

		List<Row>[] batches = new ArrayList[batchCount];
		for (int i = 0; i < batchCount; i++) batches[i] = new ArrayList<>();

		int triangleIndex = 0;
		for (String[] rowData : table) {
			int anchor = 0;
			for (int i = 0; i < triangleIndex; i++) {
				anchor += anchorCount - i;
			}

			Row row = new Row(anchor, rowData);
			int lastIndex = 0;
			for (int i = 0; i < anchorCount; i++) {
				if (i == 0) {
					lastIndex = triangleIndex;
					batches[lastIndex].add(row);
				} else if (i <= triangleIndex) {
					lastIndex += anchorCount - i;
					batches[lastIndex].add(row);
				} else {
					lastIndex += 1;
					batches[lastIndex].add(row);
				}
			}

			triangleIndex++;
			if (triangleIndex >= anchorCount) triangleIndex = 0;
		}

		int MAX_SPLIT = 100;
		int workerIndex = 0;
		for (int i = 0; i < batches.length; i++) {
			List<Row> batch = batches[i];
			Row[] arrayBatch = new Row[batch.size()];
			batch.toArray(arrayBatch);
			if (workerIndex == colleagues.size()) {
				int splitCount = (int) Math.ceil((double) arrayBatch.length / MAX_SPLIT);
				for (int k = 0; k < arrayBatch.length; k += MAX_SPLIT) {
					Row[] arrayBatchSplit = Arrays.copyOfRange(arrayBatch, k, Math.min(k + MAX_SPLIT, arrayBatch.length));
					this.self().tell(new FindDifferenceSetFromBatchSplitMessage(splitCount, arrayBatchSplit, i, task.isNullEqualsNull()), this.self());
				}
			} else {
				int splitCount = (int) Math.ceil((double) arrayBatch.length / MAX_SPLIT);
				for (int k = 0; k < arrayBatch.length; k += MAX_SPLIT) {
					Row[] arrayBatchSplit = Arrays.copyOfRange(arrayBatch, k, Math.min(k + MAX_SPLIT, arrayBatch.length));
					colleagues.get(workerIndex).tell(new FindDifferenceSetFromBatchSplitMessage(splitCount, arrayBatchSplit, i, task.isNullEqualsNull()), this.self());
				}
			}

			workerIndex++;
			if (workerIndex > colleagues.size()) workerIndex = 0;
		}

		this.log.info("Dispatched {} rows. (duplication factor of {})", anchorCount * table.length, anchorCount);
	}

	private void handle(FindDifferenceSetFromBatchSplitMessage message) {
		if (selfState != WorkerState.DISCOVERING_DIFFERENCE_SETS) {
			broadcastAndSetState(WorkerState.DISCOVERING_DIFFERENCE_SETS);
		}
		columnsInTable = message.getRows()[0].values.length;
		if (differenceSetDetector == null) createDifferenceSetDetector();

		this.log.info("Received Row Batch Split {}/{} [id:{}] of size {}", splitCount + 1, message.getSplitCount(), message.getBatchId(), message.getRows().length);

		for (int indexA = 0; indexA < message.getRows().length; indexA++) {
			for (int indexB = indexA + 1; indexB < message.getRows().length; indexB++) {
				Row rowA = message.getRows()[indexA];
				Row rowB = message.getRows()[indexB];
				if (rowA.anchor == rowB.anchor && rowA.anchor != message.getBatchId()) continue;

				differenceSetDetector.addDifferenceSet(rowA.values, rowB.values, message.isNullEqualsNull());
			}
		}

		splitCount++;
		if (splitCount < message.getSplitCount()) return;
		splitCount = 0;

		minimalDifferenceSets = differenceSetDetector.getMinimalDifferenceSets();
		this.log.info("Calculated {} minimal difference sets | Batch[id:{}]", minimalDifferenceSets.length, message.getBatchId());

//		broadcastAndSetState(WorkerState.READY_TO_MERGE);
		this.self().tell(new WorkerStateChangedMessage(WorkerState.READY_TO_MERGE), this.self());
	}

	private void handle(FindDifferenceSetFromBatchMessage message) {
		if (selfState != WorkerState.DISCOVERING_DIFFERENCE_SETS) {
			broadcastAndSetState(WorkerState.DISCOVERING_DIFFERENCE_SETS);
		}
		columnsInTable = message.getRows()[0].values.length;
		if (differenceSetDetector == null) createDifferenceSetDetector();

		this.log.info("Received Row Batch[id:{}] of size {}", message.getBatchId(), message.getRows().length);

		for (int indexA = 0; indexA < message.getRows().length; indexA++) {
			for (int indexB = indexA + 1; indexB < message.getRows().length; indexB++) {
				Row rowA = message.getRows()[indexA];
				Row rowB = message.getRows()[indexB];
				if (rowA.anchor == rowB.anchor && rowA.anchor != message.getBatchId()) continue;

				differenceSetDetector.addDifferenceSet(rowA.values, rowB.values, message.isNullEqualsNull());
			}
		}

		minimalDifferenceSets = differenceSetDetector.getMinimalDifferenceSets();
		this.log.info("Calculated {} minimal difference sets | Batch[id:{}]", minimalDifferenceSets.length, message.getBatchId());

//		broadcastAndSetState(WorkerState.READY_TO_MERGE);
		this.self().tell(new WorkerStateChangedMessage(WorkerState.READY_TO_MERGE), this.self());
	}

	private void handle(WorkerStateChangedMessage message) {
//		this.log.info("Received New State Message {}", message.state);

		for (int i = 0; i < colleaguesStates.size(); i++) {
			if (colleagues.get(i).equals(this.sender())) {
				colleaguesStates.set(i, message.state);
			}
		}

		if (this.self().equals(this.sender())) {
			if (selfState != WorkerState.TREE_TRAVERSAL || message.state != WorkerState.READY_TO_MERGE) {
				selfState = message.state;
			}
		}

		if (selfState == WorkerState.READY_TO_MERGE && message.state == WorkerState.READY_TO_MERGE) {
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
					worker.tell(new SyncDifferenceSetsMessage(minimalDifferenceSets, columnsInTable), this.self());
				}

				this.log.info("Start Tree Search");
				BitSet x = new BitSet(columnsInTable);
				BitSet y = new BitSet(columnsInTable);
				addRootTreeSearchNode();
				addChildToTreeSearchNode();
				this.self().tell(new TreeNodeWorkMessage(x, y, 0, minimalDifferenceSets, columnsInTable, currentTreeNodeId), this.self());
			}
		}

		if (selfState == WorkerState.DONE && message.state == WorkerState.DONE) {
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
		columnsInTable = message.columnsInTable;
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

			for (ActorRef actorRef : colleagues) {
				actorRef.tell(new ReportAndShutdownMessage(), this.self());
			}
			this.self().tell(new ReportAndShutdownMessage(), this.self());
			getContext().getSystem().terminate();
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

		HittingSetOracle.Status result = HittingSetOracle.extendable(x, y, length, differenceSets, columnsInTable);
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
//		this.log.info("SET {}", DifferenceSetDetector.bitSetToString(ucc, columnsInTable));
		this.log.info("UCC: {}", toUCC(ucc));

		discoveredUCCs.add(ucc);
		for (ActorRef worker : colleagues) {
			worker.tell(new UCCDiscoveredMessage(ucc), this.self());
		}
	}

	private void split(BitSet x, BitSet y, int next, BitSet[] differenceSets) {
		if (next < columnsInTable) {
			BitSet xNew = copyBitSet(x, next);
			xNew.set(next);
			ActorRef randomRef = getRandomColleague();
			addChildToTreeSearchNode();
			randomRef.tell(new TreeNodeWorkMessage(xNew, y, next + 1, minimalDifferenceSets, columnsInTable, currentTreeNodeId), this.self());

			BitSet yNew = copyBitSet(y, next);
			yNew.set(next);
			if (currentLocalTreeDepth >= maxLocalTreeDepth) {
				randomRef = getRandomColleague();
				addChildToTreeSearchNode();
				randomRef.tell(new TreeNodeWorkMessage(x, yNew, next + 1, minimalDifferenceSets, columnsInTable, currentTreeNodeId), this.self());
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

	enum WorkerState {NOT_STARTED, DISCOVERING_DIFFERENCE_SETS, READY_TO_MERGE, WAITING_FOR_MERGE, ACCEPTED_MERGE, MERGING, DONE_MERGING, TREE_TRAVERSAL, DONE}

	@Data
	@AllArgsConstructor
	private static class RegistrationMessage implements Serializable {
		private static final long serialVersionUID = 4545299661052071337L;
	}

	@Data
	@AllArgsConstructor
	private static class WorkerStateChangedMessage implements Serializable {
		private static final long serialVersionUID = 4037295208965201337L;
		private WorkerState state;

		private WorkerStateChangedMessage() {
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

	@Data
	@AllArgsConstructor
	private class ReportAndShutdownMessage implements Serializable {
		private static final long serialVersionUID = 1337457603749641337L;
	}
}
