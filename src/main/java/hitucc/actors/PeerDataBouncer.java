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
import hitucc.model.*;

import java.util.*;

public class PeerDataBouncer extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-data-bouncer";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());

	private int neededLocalWorkerCount;
	private int registeredSystems;

	private List<ActorRef> remoteDataBouncer = new ArrayList<>();
	private List<ActorRef> localWorker = new ArrayList<>();
	private List<ActorRef> remoteWorker = new ArrayList<>();

	private List<ActorWaitsForBatchModel> workerWaitsForBatch = new ArrayList<>();
	private List<RegisterSystemMessage> systemsToRegister = new ArrayList<>();

	private BatchRoutingTable routingTable;
	private EncodedBatches batches;

	private TaskMessage task;
	private boolean started;

	private List<List<ActorRef>> workerPerSystem = new ArrayList<>();

	public PeerDataBouncer(Integer localWorkerCount) {
		this.neededLocalWorkerCount = localWorkerCount;
	}

	public static Props props(Integer localWorkerCount) {
		return Props.create(PeerDataBouncer.class, () -> new PeerDataBouncer(localWorkerCount));
	}

	private int workerInCluster() {
		return localWorker.size() + remoteWorker.size();
	}

	@Override
	public void preStart() {
//		Reaper.watchWithDefaultReaper(this);
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	private String getActorSystemID() {
		return this.self().path().name().substring(this.self().path().name().indexOf(":"));
	}

	private String getMemberPort(Member member) {
		return member.address().hostPort().substring(member.address().hostPort().indexOf(":"));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MemberUp.class, this::handle)
				.match(MemberJoined.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(RegisterSystemMessage.class, this::handle)
				.match(TaskMessage.class, this::handle)
				.match(SetupDataBouncerMessage.class, this::handle)
				.match(AddBatchRouteMessage.class, this::handle)
				.match(RequestDataBatchMessage.class, this::handle)
				.match(PreRequestDataBatchMessage.class, this::handle)
				.match(SendEncodedDataBatchMessage.class, this::handle)
				.match(StartTreeSearchMessage.class, this::handle)
				.match(Terminated.class, terminated -> {
				})
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\" from \"{}\"", object.getClass().getName(), this.sender().path().name()))
				.build();
	}

	private boolean isValidMember(Member member) {
		return member.hasRole(HitUCCPeerHostSystem.PEER_HOST_ROLE) || member.hasRole(HitUCCPeerSystem.PEER_ROLE);
	}

	private void register(Member member) {
		if (isValidMember(member)) {
			if (getActorSystemID().equals(getMemberPort(member))) {
				this.getContext()
						.actorSelection(member.address() + "/user/*")
						.tell(new RegistrationMessage(), this.self());
			} else {
				this.getContext()
						.actorSelection(member.address() + "/user/" + PeerDataBouncer.DEFAULT_NAME + getMemberPort(member))
						.tell(new RegistrationMessage(), this.self());
			}
		}
	}

	private void handle(MemberUp message) {
//		this.log.info("Member Up {}", message.member());
		register(message.member());
	}

	private void handle(MemberJoined message) {
//		this.log.info("Member Joined {}", message.member());
//		register(message.member());
	}

	private void handle(RegistrationMessage message) {
		if (this.sender().equals(this.self())) return;
		if (remoteDataBouncer.contains(this.sender())) return;
		if (localWorker.contains(this.sender())) return;

//		this.context().watch(this.sender());

		if (this.sender().path().name().contains(PeerDataBouncer.DEFAULT_NAME)) {
			remoteDataBouncer.add(this.sender());
//			this.log.info("Registered {}; {} registered data bouncer", this.sender().path().name(), remoteDataBouncer.size());
			if (localWorker.size() == neededLocalWorkerCount) {
				this.sender().tell(new RegisterSystemMessage(this.self(), localWorker, getLeadingWorker()), this.self());
			}
		} else if (this.sender().path().name().startsWith(PeerWorker.DEFAULT_NAME)) {
			localWorker.add(this.sender());
//			this.log.info("Registered {} local worker {}", localWorker.size(), this.sender().path().name());
			if (localWorker.size() == neededLocalWorkerCount) {
				for (RegisterSystemMessage m : systemsToRegister) {
					registerSystem(m);
				}
				systemsToRegister.clear();
				for (ActorRef bouncer : remoteDataBouncer) {
					bouncer.tell(new RegisterSystemMessage(this.self(), localWorker, getLeadingWorker()), this.self());
				}
			}
		}

		this.sender().tell(new RegistrationMessage(), this.self());

		if (localWorker.size() == neededLocalWorkerCount) {
			if (task != null) {
				handle(task);
			}
		}
	}

	private ActorRef getLeadingWorker() {
		ActorRef leadingWorker = localWorker.get(0);
		for (int i = 1; i < localWorker.size(); i++) {
			if (leadingWorker.toString().compareTo(localWorker.get(i).toString()) > 0) {
				leadingWorker = localWorker.get(i);
			}
		}

		return leadingWorker;
	}

	private void handle(RegisterSystemMessage message) {
		if (localWorker.size() == neededLocalWorkerCount) registerSystem(message);
		else systemsToRegister.add(message);
	}

	private void registerSystem(RegisterSystemMessage message) {
		this.log.info("register system #{}", registeredSystems + 1);

		remoteWorker.addAll(message.getWorker());
		workerPerSystem.add(new ArrayList<>(message.getWorker()));

		for (ActorRef actor : localWorker) {
			actor.tell(message, this.self());
		}

		registeredSystems += 1;

		if (task != null && registeredSystems + 1 == task.getMinSystems()) {
			handle(task);
		}
	}

	private void handle(TaskMessage task) {
		this.task = task;

		if (localWorker.size() + 1 < neededLocalWorkerCount) {
			this.log.info("{} local worker missing before the algorithm can be started", neededLocalWorkerCount - localWorker.size() - 1);
			return;
		}

		if (registeredSystems + 1 < task.getMinSystems()) {
			this.log.info("{} systems missing before the algorithm can be started", task.getMinSystems() - registeredSystems - 1);
			return;
		}

		AlgorithmTimerObject timerObject = new AlgorithmTimerObject();

		workerPerSystem.add(new ArrayList<>(localWorker));

		if (started) return;
		started = true;
		this.log.info("Received Task Message with table of size [row: {}, columns: {}]", task.getInputFile().length, task.getInputFile()[0].length);

		String[][] table = task.getInputFile();
		int batchCount = task.getDataDuplicationFactor();

		if (batchCount < 1) {
			this.log.info("{} Worker connected to the cluster", workerInCluster());
			batchCount = 0;
			int triangleCount = 0;
			while (triangleCount < workerInCluster() + 1) {
				batchCount++;
				triangleCount = 0;
				for (int i = 0; i < batchCount; i++) {
					triangleCount += batchCount - i;
				}
			}
			this.log.info("The Data Duplication Factor is set to auto: Factor is set to {}. Use {} work packages.", batchCount, triangleCount);
		}
		if (batchCount == 1) {
			this.log.info("The Data Duplication Factor is set to 1. The program therefore cannot distribute the algorithm. This is not that bad, but it slows down the execution time significantly.");
		}

		routingTable = new BatchRoutingTable(batchCount, this.self());
		for (ActorRef remoteDataBouncer : remoteDataBouncer) {
			remoteDataBouncer.tell(new SetupDataBouncerMessage(batchCount), this.self());
		}

		batches = new EncodedBatches(batchCount);

		Random random = new Random();
		int columnCount = task.getAttributes();

		timerObject.setDictionaryStartTime();
		DictionaryEncoder[] encoder = new DictionaryEncoder[columnCount];
//			DictionaryEncoder encoder[] = new BitCompressedDictionaryEncoder[columnCount];
		for (int i = 0; i < columnCount; i++) encoder[i] = new DictionaryEncoder(table.length);
		for (String[] row : table) {
			for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
				encoder[columnIndex].addValue(row[columnIndex]);
			}
		}
		for (int rowIndex = 0; rowIndex < table.length; rowIndex++) {
			int[] intRow = new int[columnCount];
			for (int i = 0; i < columnCount; i++) {
				IColumn column = encoder[i].getColumn();
				intRow[i] = column.getValue(rowIndex);
			}
			batches.getBatch(random.nextInt(batchCount)).add(new EncodedRow(intRow));
		}
		timerObject.setPhaseOneStartTime();

		for (int i = 0; i < batchCount; i++) {
			this.log.info("Batch {} has {} rows", i, batches.getBatch(i).size());
		}
		List<SingleDifferenceSetTask>[] tasksPerWorker = new List[workerInCluster()];
		for (int i = 0; i < workerInCluster(); i += 1) {
			tasksPerWorker[i] = new ArrayList<>();
		}

		this.log.info("Calculate Task Distribution");
		int workerIndex = 0;
		for (int i = 0; i < batchCount; i += 1) {
			for (int k = i; k < batchCount; k += 1) {
				tasksPerWorker[workerIndex % workerInCluster()].add(new SingleDifferenceSetTask(i, k));
				workerIndex += 1;
			}
		}

		if (task.isGreedyTaskDistribution() && registeredSystems > 0) {
			this.log.info("Redistribute Tasks");
			redistributeTasks(tasksPerWorker);
		}

		this.log.info("Start Main Algorithm");
		for (int i = 0; i < workerInCluster(); i += 1) {
			List<Integer> tasksA = new ArrayList<>();
			List<Integer> tasksB = new ArrayList<>();
			for (SingleDifferenceSetTask singleTask : tasksPerWorker[i]) {
				tasksA.add(singleTask.getSetA());
				tasksB.add(singleTask.getSetB());
			}
			ActorRef worker;
			if (i < localWorker.size()) {
				worker = localWorker.get(i);
			} else {
				worker = remoteWorker.get(i - localWorker.size());
			}
			worker.tell(new FindDifferenceSetFromBatchMessage(tasksA, tasksB, batchCount, task.isNullEqualsNull(), timerObject), this.self());
		}
	}

	private boolean isValidSwap(List<SingleDifferenceSetTask>[] tasksPerWorker, int[] actorSystemIndexToWorkerIndex, int systemIndexA, int taskListA, int listAIndex, int systemIndexB, int taskListB, int listBIndex) {
		Set<Integer> scoreListA = new HashSet<>();
		if (systemIndexA != registeredSystems) {
			for (int i = actorSystemIndexToWorkerIndex[systemIndexA]; i < actorSystemIndexToWorkerIndex[systemIndexA] + workerPerSystem.get(systemIndexA).size(); i++) {
				List<SingleDifferenceSetTask> currentTaskList = tasksPerWorker[i];
				for (int k = 0; k < currentTaskList.size(); k++) {
					if (i == taskListA && k == listAIndex) continue;
					scoreListA.add(currentTaskList.get(k).getSetA());
					scoreListA.add(currentTaskList.get(k).getSetB());
				}
			}
		}

		Set<Integer> scoreListB = new HashSet<>();
		if (systemIndexA != registeredSystems) {
			for (int i = actorSystemIndexToWorkerIndex[systemIndexB]; i < actorSystemIndexToWorkerIndex[systemIndexB] + workerPerSystem.get(systemIndexB).size(); i++) {
				List<SingleDifferenceSetTask> currentTaskList = tasksPerWorker[i];
				for (int k = 0; k < currentTaskList.size(); k++) {
					if (i == taskListB && k == listBIndex) continue;
					scoreListB.add(currentTaskList.get(k).getSetA());
					scoreListB.add(currentTaskList.get(k).getSetB());
				}
			}
		}

		int aScore = scoreListA.size();
		int aScoreAfterSwap = scoreListA.size();
		if (systemIndexA != registeredSystems) {
			if (!scoreListA.contains(tasksPerWorker[taskListA].get(listAIndex).getSetA())) aScore += 1;
			if (!scoreListA.contains(tasksPerWorker[taskListA].get(listAIndex).getSetB())) aScore += 1;
			if (!scoreListA.contains(tasksPerWorker[taskListB].get(listBIndex).getSetA())) aScoreAfterSwap += 1;
			if (!scoreListA.contains(tasksPerWorker[taskListB].get(listBIndex).getSetB())) aScoreAfterSwap += 1;
		}

		int bScore = scoreListB.size();
		int bScoreAfterSwap = scoreListA.size();
		if (systemIndexB != registeredSystems) {
			if (!scoreListB.contains(tasksPerWorker[taskListB].get(listBIndex).getSetA())) bScore += 1;
			if (!scoreListB.contains(tasksPerWorker[taskListB].get(listBIndex).getSetB())) bScore += 1;
			if (!scoreListB.contains(tasksPerWorker[taskListA].get(listAIndex).getSetA())) bScoreAfterSwap += 1;
			if (!scoreListB.contains(tasksPerWorker[taskListA].get(listAIndex).getSetB())) bScoreAfterSwap += 1;
		}
		return aScore + bScore > aScoreAfterSwap + bScoreAfterSwap;
	}

	private void redistributeTasks(List<SingleDifferenceSetTask>[] tasksPerWorker) {
		if (registeredSystems == 0) return;

		int swaps = remoteWorker.size() + localWorker.size();

		Random random = new Random();

		int[] actorSystemIndexToWorkerIndex = new int[registeredSystems + 1];
		actorSystemIndexToWorkerIndex[0] = 0;
		for (int i = 1; i < actorSystemIndexToWorkerIndex.length; i++) {
			actorSystemIndexToWorkerIndex[i] = actorSystemIndexToWorkerIndex[i - 1] + workerPerSystem.get(i - 1).size();
		}

		int[] actorSystemIndices = new int[registeredSystems + 1];
		for (int i = 0; i < actorSystemIndices.length; i++) {
			actorSystemIndices[i] = i;
		}

		for (int i = 0; i < swaps; i++) {
			int firstSystemIndex = actorSystemIndices[random.nextInt(actorSystemIndices.length)];
			actorSystemIndices[firstSystemIndex] = actorSystemIndices[actorSystemIndices.length - 1];
			int secondSystemIndex = actorSystemIndices[random.nextInt(actorSystemIndices.length - 1)];
			actorSystemIndices[firstSystemIndex] = firstSystemIndex;

			int firstSystemWorkerIndex = random.nextInt(workerPerSystem.get(firstSystemIndex).size()) + actorSystemIndexToWorkerIndex[firstSystemIndex];
			int secondSystemWorkerIndex = random.nextInt(workerPerSystem.get(secondSystemIndex).size()) + actorSystemIndexToWorkerIndex[secondSystemIndex];

			int firstWorkerTaskListIndex = random.nextInt(tasksPerWorker[firstSystemWorkerIndex].size());
			int secondWorkerTaskListIndex = random.nextInt(tasksPerWorker[secondSystemWorkerIndex].size());

			boolean validSwap = isValidSwap(tasksPerWorker, actorSystemIndexToWorkerIndex, firstSystemIndex, firstSystemWorkerIndex, firstWorkerTaskListIndex, secondSystemIndex, secondSystemWorkerIndex, secondWorkerTaskListIndex);
			if (validSwap) {
				SingleDifferenceSetTask tmp = tasksPerWorker[firstSystemWorkerIndex].get(firstWorkerTaskListIndex);
				tasksPerWorker[firstSystemWorkerIndex].set(firstWorkerTaskListIndex, tasksPerWorker[secondSystemWorkerIndex].get(secondWorkerTaskListIndex));
				tasksPerWorker[secondSystemWorkerIndex].set(secondWorkerTaskListIndex, tmp);
			}
		}
	}

	private ActorRef getDataBouncerWithBatch(int batchIdentifier) {
		return routingTable.routeToRandomActor(batchIdentifier);
	}

	private void handle(AddBatchRouteMessage message) {
		routingTable.addRoute(message.getBatchIdentifier(), this.sender());
	}

	private void handle(SetupDataBouncerMessage message) {
		this.log.info("Setup Data Bouncer[{} batches]. Can now request and send batches.", message.getBatchCount());
		this.log.info("Connected to {} local worker, {} remote worker and {} remote data-bouncer", localWorker.size(), remoteWorker.size(), remoteDataBouncer.size());
		batches = new EncodedBatches(message.getBatchCount());
		routingTable = new BatchRoutingTable(message.getBatchCount(), this.sender());

		for (ActorWaitsForBatchModel waitFor : workerWaitsForBatch) {
			if (batches.isBatchLoading(waitFor.getBatchIdentifier())) continue;

			batches.setBatchLoading(waitFor.getBatchIdentifier());
			getDataBouncerWithBatch(waitFor.getBatchIdentifier()).tell(new RequestDataBatchMessage(waitFor.getBatchIdentifier(), 0), this.self());
		}
	}

	private void handle(RequestDataBatchMessage message) {
		if (this.sender().path().name().startsWith(PeerDataBouncer.DEFAULT_NAME)) {
			// other data bouncer wants data
			List<EncodedRow> batch = batches.getBatch(message.getBatchIdentifier());
			int MAX_ROWS_PER_SPLIT = 100;
			int i = MAX_ROWS_PER_SPLIT * message.getNextSplit();
			List<EncodedRow> split = new ArrayList<>();
			for (int k = i; k - i < MAX_ROWS_PER_SPLIT && k < batch.size(); k++) {
				split.add(batch.get(k));
			}
			this.sender().tell(new SendEncodedDataBatchMessage(message.getBatchIdentifier(), split,
					((i + 1) / MAX_ROWS_PER_SPLIT) + 1, (int) Math.ceil(1f * batch.size() / MAX_ROWS_PER_SPLIT)), this.self());
			if (((i + 1) / MAX_ROWS_PER_SPLIT) + 1 == split.size()) {
				this.log.info("Send {} splits to remote data bouncer[{} rows] BatchID: {}", ((i + 1) / MAX_ROWS_PER_SPLIT) + 1, split.size(), message.getBatchIdentifier());
			}
		} else {
			// local worker wants data
			if (batches == null) {
				workerWaitsForBatch.add(new ActorWaitsForBatchModel(this.sender(), message.getBatchIdentifier()));
				return;
			}

			if (batches.hasBatch(message.getBatchIdentifier())) {
				List<EncodedRow> batch = batches.getBatch(message.getBatchIdentifier());
				this.sender().tell(new SendEncodedDataBatchMessage(message.getBatchIdentifier(), batch, 1, 1), this.self());
			} else {
				// load data from other dataBouncer first
				workerWaitsForBatch.add(new ActorWaitsForBatchModel(this.sender(), message.getBatchIdentifier()));
				// is already loading batch
				if (batches.isBatchLoading(message.getBatchIdentifier())) return;

				batches.setBatchLoading(message.getBatchIdentifier());
				getDataBouncerWithBatch(message.getBatchIdentifier()).tell(new RequestDataBatchMessage(message.getBatchIdentifier(), 0), this.self());
			}
		}
	}

	private void handle(PreRequestDataBatchMessage message) {
		if (!batches.hasBatch(message.getBatchIdentifier())) {
//			 is already loading batch
			if (batches.isBatchLoading(message.getBatchIdentifier())) return;

			batches.setBatchLoading(message.getBatchIdentifier());
			getDataBouncerWithBatch(message.getBatchIdentifier()).tell(new RequestDataBatchMessage(message.getBatchIdentifier(), 0), this.self());
		}
	}

	private void handle(SendEncodedDataBatchMessage message) {
		batches.addToBatch(message.getBatchIdentifier(), message.getBatch());
		if (message.getCurrentSplit() == message.getSplitCount()) {
			// notify every other dataBouncer that I have the full batch
			for (ActorRef actorRef : remoteDataBouncer) {
				actorRef.tell(new AddBatchRouteMessage(message.getBatchIdentifier()), this.sender());
			}

			this.log.info("Received {} splits for data batch {}, {} rows", message.getSplitCount(), message.getBatchIdentifier(), batches.getBatch(message.getBatchIdentifier()).size());
			batches.isBatchLoadingFinished(message.getBatchIdentifier());
			for (int i = 0; i < workerWaitsForBatch.size(); i++) {
				ActorWaitsForBatchModel waitFor = workerWaitsForBatch.get(i);
				if (waitFor.getBatchIdentifier() == message.getBatchIdentifier()) {
					waitFor.getActor().tell(new SendEncodedDataBatchMessage(message.getBatchIdentifier(), batches.getBatch(message.getBatchIdentifier()), 1, 1), this.self());
					workerWaitsForBatch.remove(i);
					i -= 1;
				}
			}
		} else {
			// immediately request new data batch
			this.sender().tell(new RequestDataBatchMessage(message.getBatchIdentifier(), message.getCurrentSplit()), this.self());
		}
	}

//	private void encodeBatches() {
//		batches = new EncodedBatches(rawBatches.count());
//		for(int i = 0; i < rawBatches.count(); i++) {
//			encodeBatch(i);
//		}
//	}
//
//	private void encodeBatch(int batchId) {
//		int columnCount = rawBatches.count();
//		List<Row> rows = rawBatches.getBatch(batchId);
//		DictionaryEncoder[] encoder = new DictionaryEncoder[columnCount];
////			DictionaryEncoder encoder[] = new BitCompressedDictionaryEncoder[columnCount];
//		for (int i = 0; i < columnCount; i++) encoder[i] = new DictionaryEncoder(rows.size());
//		for (Row row : rows) {
//			for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
//				encoder[columnIndex].addValue(row.values[columnIndex]);
//			}
//		}
//		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
//			int[] intRow = new int[columnCount];
//			for (int i = 0; i < columnCount; i++) {
//				IColumn column = encoder[i].getColumn();
//				intRow[i] = column.getValue(rowIndex);
//			}
//			batches.addToBatch(batchId, new EncodedRow(intRow));
//		}
//		log.info("{} -> {}", rawBatches.getBatch(batchId).size(), batches.getBatch(batchId).size());
//	}

	private void handle(StartTreeSearchMessage message) {
		getContext().getSystem().actorOf(Reaper.props(), Reaper.DEFAULT_NAME);
		getContext().stop(this.self());
		this.log.info("Stop dataBouncer and create Reaper");
	}
}
