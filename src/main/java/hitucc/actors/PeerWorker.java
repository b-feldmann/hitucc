package hitucc.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberJoined;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hitucc.HitUCCPeerHostSystem;
import hitucc.HitUCCPeerSystem;
import hitucc.actors.messages.*;
import hitucc.behaviour.dictionary.DictionaryEncoder;
import hitucc.behaviour.dictionary.IColumn;
import hitucc.behaviour.differenceSets.BucketingCalculateMinimalSetsStrategy;
import hitucc.behaviour.differenceSets.DifferenceSetDetector;
import hitucc.behaviour.differenceSets.HashAddDifferenceSetStrategy;
import hitucc.behaviour.differenceSets.TwoSidedMergeMinimalSetsStrategy;
import hitucc.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PeerWorker extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());
	private final boolean useDictionaryEncoding = true;

	private Random random = new Random();
	private List<ActorRef> remoteWorker = new ArrayList<>();
	private List<ActorRef> remoteLeadingWorker = new ArrayList<>();
	private List<WorkerState> remoteLeadingWorkerStates = new ArrayList<>();
	private List<ActorRef> remoteDataBouncer = new ArrayList<>();
	private ActorRef dataBouncer = null;
	private List<ActorRef> localWorker = new ArrayList<>();
	private List<WorkerState> localWorkerStates = new ArrayList<>();
	private WorkerState selfState = WorkerState.NOT_STARTED;
	private int columnCount = 0;

	private SerializableBitSet[] minimalDifferenceSets = new SerializableBitSet[0];
	private List<SerializableBitSet> discoveredUCCs = new ArrayList<>();
	private DifferenceSetDetector differenceSetDetector;
	private Batches batches;
	private List<SingleDifferenceSetTask> tasks;
	private boolean nullEqualsNull = false;
	private int missingBatchSplits = 0;

	private NETWORK_ACTION currentNetworkAction = NETWORK_ACTION.LOCAL;

	public static Props props() {
		return Props.create(PeerWorker.class);
	}

	public static int log2nlz(int bits) {
		if (bits == 0)
			return 0; // or throw exception
		return 31 - Integer.numberOfLeadingZeros(bits);
	}

	private String getActorSystemID() {
		return this.self().path().name().substring(this.self().path().name().indexOf(":"));
	}

	private String getWorkerIndexInSystem() {
		return this.self().path().name().substring(this.self().path().name().lastIndexOf(PeerWorker.DEFAULT_NAME) + PeerWorker.DEFAULT_NAME.length(), this.self().path().name().indexOf(":"));
	}

	private void createDifferenceSetDetector() {
		differenceSetDetector = new DifferenceSetDetector(new HashAddDifferenceSetStrategy(), new BucketingCalculateMinimalSetsStrategy(columnCount), new TwoSidedMergeMinimalSetsStrategy());
	}

	@Override
	public void preStart() {
		this.cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	private boolean otherHasPriority(ActorRef other) {
		if (this.self().path().name().equals(other.path().name())) {
			this.log.error("Actor String is not unique >.<");
		}

//		this.log.info("{} priority over {} => {}", other.path().name(), this.self().path().name(), this.self().path().name().compareTo(other.path().name()) < 0);
//		this.log.info("{} priority over {} => {}", this.self().path().name(), other.path().name(), other.path().name().compareTo(this.self().path().name()) < 0);

		return this.self().path().name().compareTo(other.path().name()) < 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MemberUp.class, this::handle)
				.match(MemberJoined.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(RegisterSystemMessage.class, this::handle)
				.match(FindDifferenceSetFromBatchMessage.class, this::handle)
				.match(SendDataBatchMessage.class, this::handle)
				.match(AskForMergeMessage.class, this::handle)
				.match(AcceptMergeMessage.class, this::handle)
				.match(DeclineMergeMessage.class, this::handle)
				.match(WorkerStateChangedMessage.class, this::handle)
				.match(MergeDifferenceSetsMessage.class, this::handle)
				.match(StartTreeSearchMessage.class, this::handle)
				.match(Terminated.class, terminated -> {})
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\" from \"{}\"", object.getClass().getName(), this.sender().path().name()))
				.build();
	}

	private boolean isValidMember(Member member) {
		return (member.hasRole(HitUCCPeerHostSystem.PEER_HOST_ROLE) || member.hasRole(HitUCCPeerSystem.PEER_ROLE)) && getActorSystemID().equals(member.address().hostPort().substring(member.address().hostPort().indexOf(":")));
	}

	private void register(Member member) {
		if (isValidMember(member)) {
			this.getContext()
					.actorSelection(member.address() + "/user/*")
					.tell(new RegistrationMessage(), this.self());
		}
	}

	private void handle(MemberUp message) {
		register(message.member());
	}

	private void handle(MemberJoined message) {
//		register(message.member());
	}

	private void handle(RegistrationMessage message) {
		if (this.sender().equals(this.self())) return;
		if (this.sender().equals(dataBouncer)) return;
		if (localWorker.contains(this.sender())) return;
		if (remoteWorker.contains(this.sender())) return;
		if (remoteDataBouncer.contains(this.sender())) return;

		this.context().watch(this.sender());

		if (this.sender().path().name().contains(PeerDataBouncer.DEFAULT_NAME)) {
			if (this.sender().path().name().equals(PeerDataBouncer.DEFAULT_NAME + getActorSystemID())) {
				dataBouncer = this.sender();
//				this.log.info("Registered local DataBouncer");
			} else {
				remoteDataBouncer.add(this.sender());
				this.log.info("Registered {} remote DataBouncer", remoteWorker.size());
			}
		} else {
			if (this.sender().path().name().startsWith(PeerWorker.DEFAULT_NAME) && this.sender().path().name().endsWith(getActorSystemID())) {
				localWorker.add(this.sender());
				localWorkerStates.add(WorkerState.NOT_STARTED);
//				this.log.info("Registered {} colleagues", colleagues.size());
			} else {
				remoteWorker.add(this.sender());
//				this.log.info("Registered {} remote Worker", remoteWorker.size());
			}

		}

		this.sender().tell(new RegistrationMessage(), this.self());
	}

	private void handle(RegisterSystemMessage message) {
		remoteDataBouncer.add(message.getDataBouncer());
		remoteWorker.addAll(message.getWorker());

		remoteLeadingWorker.add(message.getLeadingWorker());
		remoteLeadingWorkerStates.add(WorkerState.NOT_STARTED);
	}

	private List<SingleDifferenceSetTask> convertListToTasks(List<Integer> tasksA, List<Integer> tasksB) {
		List<SingleDifferenceSetTask> tasks = new ArrayList<>();
		for (int i = 0; i < tasksA.size(); i++) {
			tasks.add(new SingleDifferenceSetTask(tasksA.get(i), tasksB.get(i)));
		}
		return tasks;
	}

	private void handle(FindDifferenceSetFromBatchMessage message) {
		if (selfState != WorkerState.DISCOVERING_DIFFERENCE_SETS) {
			broadcastAndSetState(WorkerState.DISCOVERING_DIFFERENCE_SETS);
		}

		batches = new Batches(message.getBatchCount());
		tasks = convertListToTasks(message.getDifferenceSetTasksA(), message.getDifferenceSetTasksB());
		nullEqualsNull = message.isNullEqualsNull();

		tryToFindDifferenceSets();
	}

	private void handle(SendDataBatchMessage message) {
		if (missingBatchSplits == 0) missingBatchSplits = message.getSplitCount();

		batches.addToBatch(message.getBatchIdentifier(), message.getBatch());

		missingBatchSplits -= 1;

		if (missingBatchSplits == 0) {
//			this.log.info("Received {} splits for data batch {}, {} rows", message.getSplitCount(), message.getBatchIdentifier(), batches.getBatch(message.getBatchIdentifier()).size());
			tryToFindDifferenceSets();
		} else {
			dataBouncer.tell(new RequestDataBatchMessage(message.getBatchIdentifier(), message.getCurrentSplit()), this.self());
		}
	}

	private void tryToFindDifferenceSets() {
		if (tasks.size() == 0) {
			broadcastAndSetState(WorkerState.DONE_MERGING);
			return;
		}

		SingleDifferenceSetTask currentTask = tasks.get(0);
		if (!batches.hasBatch(currentTask.getSetA())) {
			this.log.info("Request Data Batch with id {}", currentTask.getSetA());
			dataBouncer.tell(new RequestDataBatchMessage(currentTask.getSetA(), 0), this.self());
			return;
		}
		if (!batches.hasBatch(currentTask.getSetB())) {
			this.log.info("Request Data Batch with id {}", currentTask.getSetB());
			dataBouncer.tell(new RequestDataBatchMessage(currentTask.getSetB(), 0), this.self());
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
				long count = 0;
				for (int indexA = 0; indexA < rows.length; indexA++) {
					for (int indexB = indexA + 1; indexB < rows.length; indexB++, count += 1) {
						if (count % 100000000 == 0) {
							this.log.info("Added {}/{} difference sets | cached {}, minimal {}", count, (1 + rows.length) * rows.length / 2, differenceSetDetector.getCachedDifferenceSetCount(), differenceSetDetector.getLastCountedMinimalDifferenceSetCount());
						}
						differenceSetDetector.addDifferenceSet(intRows[indexA], intRows[indexB], nullEqualsNull);
					}
				}
			} else {
				int firstBatchSize = batches.getBatch(currentTask.getSetA()).size();
				long count = 0;
				for (int indexA = 0; indexA < firstBatchSize; indexA++) {
					for (int indexB = firstBatchSize; indexB < rows.length; indexB++, count += 1) {
						if (count % 100000000 == 0) {
							this.log.info("Added {}/{} difference sets | cached {}, minimal {}", count, ((long) firstBatchSize) * (rows.length - firstBatchSize), differenceSetDetector.getCachedDifferenceSetCount(), differenceSetDetector.getLastCountedMinimalDifferenceSetCount());
						}
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

		tasks.remove(0);
		if (tasks.size() == 0) {
			this.self().tell(new WorkerStateChangedMessage(WorkerState.READY_TO_MERGE, currentNetworkAction), this.self());
		} else {
			List<Integer> tasksA = new ArrayList<>();
			List<Integer> tasksB = new ArrayList<>();
			for (SingleDifferenceSetTask task : tasks) {
				tasksA.add(task.getSetA());
				tasksB.add(task.getSetB());
			}
			this.self().tell(new FindDifferenceSetFromBatchMessage(tasksA, tasksB, batches.count(), nullEqualsNull), this.self());
		}
	}

	private void handle(WorkerStateChangedMessage message) {
//		this.log.info("Received new state \"{}\" from {}", message.getState(), this.sender().toString());
		List<ActorRef> worker = message.getNetworkAction() == NETWORK_ACTION.LOCAL ? localWorker : remoteLeadingWorker;
		List<WorkerState> workerStates = message.getNetworkAction() == NETWORK_ACTION.LOCAL ? localWorkerStates : remoteLeadingWorkerStates;

		for (int i = 0; i < workerStates.size(); i++) {
			if (worker.get(i).equals(this.sender())) {
				workerStates.set(i, message.getState());
			}
		}

		if (this.self().equals(this.sender())) {
			selfState = message.getState();
		}

		if (selfState == WorkerState.READY_TO_MERGE && message.getState() == WorkerState.READY_TO_MERGE) {
			tryToMerge();
		}

		if (selfState == WorkerState.READY_TO_MERGE && currentNetworkAction == message.getNetworkAction()) {
			boolean finishedMerge = true;
			for (WorkerState colleaguesState : workerStates) {
				if (currentNetworkAction == NETWORK_ACTION.LOCAL) {
					if (colleaguesState != WorkerState.DONE_MERGING && colleaguesState != WorkerState.NOT_STARTED) {
						finishedMerge = false;
					}
				} else {
					this.log.info("remote worker state: {}", colleaguesState);
					if (colleaguesState != WorkerState.DONE_MERGING) {
						finishedMerge = false;
					}
				}
			}
			if (finishedMerge) {
				if (currentNetworkAction == NETWORK_ACTION.LOCAL) {
					this.log.info("Finished Local Merging!");
					currentNetworkAction = NETWORK_ACTION.REMOTE;
					this.self().tell(new WorkerStateChangedMessage(WorkerState.READY_TO_MERGE, currentNetworkAction), this.self());
				} else {
//					selfState = WorkerState.TREE_TRAVERSAL;
//					for (ActorRef w : localWorker) {
//						w.tell(new StartTreeSearchMessage(minimalDifferenceSets, columnCount), this.self());
//					}

					this.log.info("Finished Global Merging!");

//					ActorRef[] allOtherActors = new ActorRef[localWorker.size() + 1 + remoteWorker.size() + remoteDataBouncer.size()];
					ActorRef[] allOtherActors = new ActorRef[localWorker.size() + remoteWorker.size()];
					int index = 0;
					for (ActorRef actor : localWorker) {
						allOtherActors[index] = actor;
						index++;
					}
//					allOtherActors[index] = dataBouncer;
//					index++;
					for (ActorRef actor : remoteWorker) {
						allOtherActors[index] = actor;
						index++;
					}
//					for (ActorRef actor : remoteDataBouncer) {
//						allOtherActors[index] = actor;
//						index++;
//					}

					dataBouncer.tell(new StartTreeSearchMessage(), this.self());
					for(ActorRef bouncer : remoteDataBouncer) bouncer.tell(new StartTreeSearchMessage(), this.self());

					getContext().getSystem().actorOf(PeerTreeSearchWorker.props(allOtherActors, minimalDifferenceSets, columnCount), PeerTreeSearchWorker.DEFAULT_NAME + getWorkerIndexInSystem() + getActorSystemID());
					this.log.info("Stopping myself..");
					getContext().stop(this.self());
//					treeSearchStart = System.currentTimeMillis();
//					SerializableBitSet x = new SerializableBitSet(columnCount);
//					SerializableBitSet y = new SerializableBitSet(columnCount);
//					addRootTreeSearchNode();
//					addChildToTreeSearchNode();
//					this.self().tell(new TreeNodeWorkMessage(x, y, 0, minimalDifferenceSets, columnCount, currentTreeNodeId), this.self());
				}
			}
		}
	}

	private void handle(StartTreeSearchMessage message) {
		getContext().getSystem().actorOf(PeerTreeSearchWorker.props(this.sender(), message.getMinimalDifferenceSets(), message.getColumnsInTable(), message.getWorkerInCluster()), PeerTreeSearchWorker.DEFAULT_NAME + getWorkerIndexInSystem() + getActorSystemID());
		this.log.info("Stopping myself..");
		getContext().stop(this.self());
	}

	private void tryToMerge() {
		List<ActorRef> worker = currentNetworkAction == NETWORK_ACTION.LOCAL ? localWorker : remoteLeadingWorker;
		List<WorkerState> workerStates = currentNetworkAction == NETWORK_ACTION.LOCAL ? localWorkerStates : remoteLeadingWorkerStates;

		List<ActorRef> waitingWorkers = new ArrayList<>();
		for (int i = 0; i < worker.size(); i++) {
			if (workerStates.get(i) == WorkerState.READY_TO_MERGE && !otherHasPriority(worker.get(i))) {
				waitingWorkers.add(worker.get(i));
			}
		}

//		this.log.info("Try to merge with {}/{} actors on the {} network", waitingWorkers.size(), worker.size(), currentNetworkAction == NETWORK_ACTION.LOCAL ? "local" : "remote");

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
		this.log.info("Merge {} and {} minimal sets together", minimalDifferenceSets.length, message.differenceSets.length);

		minimalDifferenceSets = differenceSetDetector.mergeMinimalDifferenceSets(minimalDifferenceSets, message.differenceSets);
		this.log.info("Merged into {} difference sets", minimalDifferenceSets.length);

		broadcastAndSetState(WorkerState.READY_TO_MERGE);
	}

	private void broadcastState(WorkerState state) {
		List<ActorRef> worker = currentNetworkAction == NETWORK_ACTION.LOCAL ? localWorker : remoteLeadingWorker;

		for (ActorRef w : worker) {
			w.tell(new WorkerStateChangedMessage(state, currentNetworkAction), this.self());
		}
	}

	private void broadcastAndSetState(WorkerState state) {
		selfState = state;
		broadcastState(state);
	}

	public static enum NETWORK_ACTION {LOCAL, REMOTE}

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
		private SerializableBitSet[] differenceSets;

		private MergeDifferenceSetsMessage() {
		}
	}
}
