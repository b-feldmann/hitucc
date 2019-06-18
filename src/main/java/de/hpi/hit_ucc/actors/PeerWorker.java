package de.hpi.hit_ucc.actors;

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
import de.hpi.hit_ucc.DifferenceSetDetector;
import de.hpi.hit_ucc.HitUCCPeerSystem;
import de.hpi.hit_ucc.HittingSetOracle;
import de.hpi.hit_ucc.actors.messages.FindDifferenceSetFromBatchMessage;
import de.hpi.hit_ucc.actors.messages.IWorkMessage;
import de.hpi.hit_ucc.actors.messages.TaskMessage;
import de.hpi.hit_ucc.model.Row;
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

	private WorkerState selfState = WorkerState.DISCOVERING;
	private int columnsInTable = 0;

	private LinkedHashSet<BitSet> uniqueDifferenceSets = new LinkedHashSet<>();
	private BitSet[] minimalDifferenceSets = new BitSet[0];
	private List<BitSet> discoveredUCCs = new ArrayList<>();

	public static Props props() {
		return Props.create(PeerWorker.class);
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
		if (this.self().toString().equals(this.sender().toString())) {
			this.log.error("Actor String is not unique >.<");
		}

		return this.self().toString().compareTo(other.toString()) < 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CurrentClusterState.class, this::handle)
				.match(MemberUp.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(TaskMessage.class, this::handle)
				.match(FindDifferenceSetFromBatchMessage.class, this::handle)
				.match(AskForMergeMessage.class, this::handle)
				.match(AcceptMergeMessage.class, this::handle)
				.match(DeclineMergeMessage.class, this::handle)
				.match(WorkerStateChangedMessage.class, this::handle)
				.match(MergeDifferenceSetsMessage.class, this::handle)
				.match(TreeOracleMessage.class, this::handle)
				.match(UCCDiscoveredMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void register(Member member) {
		if (member.hasRole(HitUCCPeerSystem.PEER_ROLE))
			this.getContext()
					.actorSelection(member.address() + "/user/*")
					.tell(new RegistrationMessage(), this.self());
	}

	private void handle(CurrentClusterState message) {
		message.getMembers().forEach(member -> {
			if (member.status().equals(MemberStatus.up())) {
				register(member);
			}
		});
	}

	private void handle(MemberUp message) {
		Member member = message.member();
		if (member.hasRole(HitUCCPeerSystem.PEER_ROLE)) {
			register(member);
		}
	}

	private void handle(RegistrationMessage message) {
		if (this.sender().equals(this.self())) return;

		this.context().watch(this.sender());

		colleagues.add(this.sender());
		colleaguesStates.add(WorkerState.DISCOVERING);
//		this.log.info("Registered {}; {} registered colleagues", this.sender(), colleagues.size());
	}

	private void handle(TaskMessage task) {
		this.log.info("Received Task Message with table of size [row: {}, columns: {}]", task.getInputFile().length, task.getInputFile()[0].length);

		String[][] table = task.getInputFile();

		int batchCount = 6;
		List<Row>[] batches = new ArrayList[batchCount];
		for (int i = 0; i < batchCount; i++) batches[i] = new ArrayList<>();

		int triangleIndex = 0;
		for (String[] rowData : table) {
			switch (triangleIndex) {
				case 0: {
					Row row = new Row(0, rowData);
					batches[0].add(row);
					batches[1].add(row);
					batches[2].add(row);
					break;
				}
				case 1: {
					Row row = new Row(3, rowData);
					batches[1].add(row);
					batches[3].add(row);
					batches[4].add(row);
					break;
				}
				case 2: {
					Row row = new Row(5, rowData);
					batches[2].add(row);
					batches[4].add(row);
					batches[5].add(row);
					break;
				}
			}
			triangleIndex++;
			if (triangleIndex > 2) triangleIndex = 0;
		}

		int workerIndex = 0;
		for (int i = 0; i < batches.length; i++) {
			List<Row> batch = batches[i];
			Row[] arrayBatch = new Row[batch.size()];
			batch.toArray(arrayBatch);
			if (workerIndex == colleagues.size()) {
				this.self().tell(new FindDifferenceSetFromBatchMessage(arrayBatch, i), this.self());
			} else {
				colleagues.get(workerIndex).tell(new FindDifferenceSetFromBatchMessage(arrayBatch, i), this.self());
			}

			workerIndex++;
			if (workerIndex > colleagues.size()) workerIndex = 0;
		}
	}

	private void handle(FindDifferenceSetFromBatchMessage message) {
		this.log.info("Received Row Batch[id:{}] of size {}", message.getBatchId(), message.getRows().length);

		columnsInTable = message.getRows()[0].values.length;

		for (int indexA = 0; indexA < message.getRows().length; indexA++) {
			for (int indexB = indexA + 1; indexB < message.getRows().length; indexB++) {
				Row rowA = message.getRows()[indexA];
				Row rowB = message.getRows()[indexB];
				if (rowA.anchor == rowB.anchor && rowA.anchor != message.getBatchId()) continue;

				BitSet differenceSet = DifferenceSetDetector.calculateHittingset(rowA.values, rowB.values);
				uniqueDifferenceSets.add(differenceSet);
			}
		}

		this.log.info("Found {} unique sets", uniqueDifferenceSets.size());

		minimalDifferenceSets = DifferenceSetDetector.GetMinimalDifferenceSets(uniqueDifferenceSets);

		log.info("Found {} minimal difference sets from {} actual sets", minimalDifferenceSets.length, uniqueDifferenceSets.size());

		uniqueDifferenceSets.clear();

		broadcastAndSetState(WorkerState.READY_TO_MERGE);
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
		if (selfState == WorkerState.READY_TO_MERGE || (selfState == WorkerState.WAITING_FOR_MERGE && otherHasPriority())) {
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
		tryToMerge();
	}

	private void handle(MergeDifferenceSetsMessage message) {
		broadcastAndSetState(WorkerState.MERGING);
		this.log.info("Received Merge Message from {}", this.sender().path().name());

		Set<BitSet> mergedSets = new HashSet<>();
		mergedSets.addAll(Arrays.asList(minimalDifferenceSets));
		mergedSets.addAll(Arrays.asList(message.differenceSets));

		minimalDifferenceSets = DifferenceSetDetector.GetMinimalDifferenceSets(mergedSets);
		this.log.info("Merged into {} difference sets", minimalDifferenceSets.length);

		broadcastAndSetState(WorkerState.READY_TO_MERGE);
	}

	private void handle(WorkerStateChangedMessage message) {
//		this.log.info("Received New State Message {}", message.state);
		for (int i = 0; i < colleaguesStates.size(); i++) {
			if (colleagues.get(i).equals(this.sender())) {
				colleaguesStates.set(i, message.state);
			}
		}
		if (this.self().equals(this.sender())) selfState = message.state;

		if (selfState == WorkerState.READY_TO_MERGE && message.state == WorkerState.READY_TO_MERGE) {
			tryToMerge();
		}

		if (selfState == WorkerState.READY_TO_MERGE) {
			boolean finishedMerge = true;
			for (WorkerState colleaguesState : colleaguesStates) {
				if (colleaguesState != WorkerState.DONE_MERGING) {
					finishedMerge = false;
				}
			}
			if (finishedMerge) {
				this.log.info("Finished Merging!");

				this.log.info("Found following minimal difference sets:");
				for (BitSet differenceSet : minimalDifferenceSets) {
					log.info(DifferenceSetDetector.bitSetToString(differenceSet));
				}

				BitSet x = new BitSet(columnsInTable);
				BitSet y = new BitSet(columnsInTable);
				handle(new TreeOracleMessage(x, y, 0, minimalDifferenceSets, columnsInTable));
			}
		}
	}

	private void handle(TreeOracleMessage message) {
		if (selfState != WorkerState.TREE_TRAVERSAL) broadcastAndSetState(WorkerState.TREE_TRAVERSAL);

		BitSet x = message.getX();
		BitSet y = message.getY();
		int length = message.getLength();
		BitSet[] differenceSets = message.getDifferenceSets();

		HittingSetOracle.Status result = HittingSetOracle.extendable(x, y, length, differenceSets, columnsInTable);
		switch (result) {
			case MINIMAL:
				this.report(x);
				break;
			case EXTENDABLE:
				this.split(message);
				break;
			case NOT_EXTENDABLE:
				// Ignore
				break;
			case FAILED:
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

	private void split(TreeOracleMessage work) {
		BitSet x = work.getX();
		BitSet y = work.getY();
		BitSet[] minimalDifferenceSets = work.getDifferenceSets();

		int next = work.getLength();

		if (next < columnsInTable) {
			BitSet xNew = copyBitSet(x, next);
			xNew.set(next);
			ActorRef randomRef = colleagues.get(random.nextInt(colleagues.size()));
			randomRef.tell(new TreeOracleMessage(xNew, y, next + 1, minimalDifferenceSets, columnsInTable), this.self());

			BitSet yNew = copyBitSet(y, next);
			yNew.set(next);
			handle(new TreeOracleMessage(x, yNew, next + 1, minimalDifferenceSets, columnsInTable));
		} else {
			System.out.println("WHY IS THIS? ########################################################################");
		}
	}

	private void handle(UCCDiscoveredMessage message) {
		discoveredUCCs.add(message.ucc);
	}

	private void handle(ReportAndShutdownMessage message) {
		for (BitSet ucc : discoveredUCCs) {
			this.log.info("UCC: {}", toUCC(ucc));
		}

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

	enum WorkerState {DISCOVERING, READY_TO_MERGE, WAITING_FOR_MERGE, ACCEPTED_MERGE, MERGING, DONE_MERGING, TREE_TRAVERSAL, DONE}

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
	private static class UCCDiscoveredMessage implements Serializable {
		private static final long serialVersionUID = 997981649989901337L;
		private BitSet ucc;

		private UCCDiscoveredMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private class TreeOracleMessage implements Serializable, IWorkMessage {
		private static final long serialVersionUID = 2360129506196901337L;
		private BitSet x;
		private BitSet y;
		private int length;
		private BitSet[] differenceSets;
		private int numAttributes;

		private TreeOracleMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private class ReportAndShutdownMessage implements Serializable {
		private static final long serialVersionUID = 1337457603749641337L;
	}
}
