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

	private final int neededLocalWorkerCount;
	private final List<ActorRef> remoteDataBouncer = new ArrayList<>();
	private final List<ActorRef> localWorker = new ArrayList<>();
	private final List<ActorRef> remoteWorker = new ArrayList<>();
	private final List<ActorWaitsForBatchModel> workerWaitsForBatch = new ArrayList<>();
	private final List<RegisterSystemMessage> systemsToRegister = new ArrayList<>();
	private final List<List<ActorRef>> workerPerSystem = new ArrayList<>();
	private int registeredSystems;
	private BatchRoutingTable routingTable;
	private EncodedBatches batches;
	private TaskMessage task;
	private boolean started;

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
//			this.log.info("{} local worker missing before the algorithm can be started", neededLocalWorkerCount - localWorker.size() - 1);
			return;
		}

		if (registeredSystems + 1 < task.getMinSystems()) {
//			this.log.info("{} systems missing before the algorithm can be started", task.getMinSystems() - registeredSystems - 1);
			return;
		}

		AlgorithmTimerObject timerObject = task.getTimerObject();

		workerPerSystem.add(new ArrayList<>(localWorker));

		if (started) return;
		started = true;
		this.log.info("Received Task Message with table of size [row: {}, columns: {}]", task.getInputFile().length, task.getInputFile()[0].length);

		String[][] table = task.getInputFile();
		int batchCount = task.getDataDuplicationFactor();

		if (batchCount < 1) {
			this.log.info("{} Worker connected to the cluster", workerInCluster());
			batchCount = 0;
			float val = 0.5f;
			while (val != (int) val) {
				batchCount += 1;
				val = (batchCount * batchCount + batchCount) / 2f / workerInCluster();
			}
			this.log.info("The Data Duplication Factor is set to auto: Factor is set to {}. Use {} work packages.", batchCount, (batchCount * batchCount + batchCount) / 2f);
		}
		if (batchCount == 1) {
			this.log.info("The Data Duplication Factor is set to 1. The program therefore cannot distribute the algorithm. This is not that bad, but it slows down the execution time significantly.");
		}

		routingTable = new BatchRoutingTable(batchCount, this.self());

		Random random = new Random();
		int columnCount = task.getAttributes();

		timerObject.setDictionaryStartTime();
		DictionaryEncoder[] encoder = new DictionaryEncoder[columnCount];
		for (int i = 0; i < columnCount; i++) encoder[i] = new DictionaryEncoder(table.length, task.isNullEqualsNull());
		for (String[] row : table) {
			for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
				encoder[columnIndex].addValue(row[columnIndex]);
			}
		}

		ColumnCardinality[] columnAssignment = new ColumnCardinality[columnCount];
		for (int i = 0; i < columnAssignment.length; i++)
			columnAssignment[i] = new ColumnCardinality(i, encoder[i].getDictionary().size());
		if (timerObject.settingsSortColumnsInPhaseOne()) {
			if (timerObject.settingsSortNegatively()) {
				Arrays.sort(columnAssignment, Comparator.comparingInt(ColumnCardinality::getCardinality));
			} else {
				Arrays.sort(columnAssignment, (o1, o2) -> o2.getCardinality() - o1.getCardinality());
			}
		}

		List<EncodedRow>[] listBatches = new List[batchCount];
		for (int i = 0; i < batchCount; i++) {
			listBatches[i] = new ArrayList<>();
		}
		for (int rowIndex = 0; rowIndex < table.length; rowIndex++) {
			int[] intRow = new int[columnCount];
			for (int i = 0; i < columnCount; i++) {
				IColumn column = encoder[columnAssignment[i].getColumnIndex()].getColumn();
				intRow[i] = column.getValue(rowIndex);
			}
			listBatches[random.nextInt(batchCount)].add(new EncodedRow(intRow));
		}
		int[] batchSizes = new int[batchCount];
		for (int i = 0; i < batchCount; i++) batchSizes[i] = listBatches[i].size();
		batches = new EncodedBatches(batchCount, batchSizes);
		for (int i = 0; i < batchCount; i++) {
			batches.addToBatch(i, listBatches[i]);
		}

		batches.updateBatchSizes();
		for (ActorRef remoteDataBouncer : remoteDataBouncer) {
			remoteDataBouncer.tell(new SetupDataBouncerMessage(batchCount, batches.getBatchSizes()), this.self());
		}

		List<SingleDifferenceSetTask>[] tasksPerWorker = new List[workerInCluster()];
		for (int i = 0; i < workerInCluster(); i += 1) {
			tasksPerWorker[i] = new ArrayList<>();
		}

		if (task.isGreedyTaskDistribution() && registeredSystems > 0) {
			this.log.info("Greedy Tasks Distribution");

			greedyTaskDistribution(tasksPerWorker, batchCount);
		} else {
			this.log.info("Round-Robin Task Distribution");
			int workerIndex = 0;
			for (int i = 0; i < batchCount; i += 1) {
				for (int k = i; k < batchCount; k += 1) {
					tasksPerWorker[workerIndex % workerInCluster()].add(new SingleDifferenceSetTask(i, k));
					workerIndex += 1;
				}
			}
		}

		this.log.info("Start Main Algorithm");
		timerObject.setPhaseOneStartTime();
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
			worker.tell(new FindDifferenceSetFromBatchMessage(tasksA, tasksB, batchCount, task.isNullEqualsNull(), timerObject.clone(), batches.getBatchSizes()), this.self());
		}
	}

	private void greedyTaskDistribution(List<SingleDifferenceSetTask>[] tasksPerWorker, int batchCount) {
		Random r = new Random();
		List<SingleDifferenceSetTask> taskList = new ArrayList<>();
		for (int i = 0; i < batchCount; i += 1) {
			for (int k = i; k < batchCount; k += 1) {
				taskList.add(new SingleDifferenceSetTask(i, k));
			}
		}
		List<SingleDifferenceSetTask>[] tasksPerActorSystem = new List[remoteDataBouncer.size() + 1];
		Set<Integer>[] scoreListPerActorSystem = new Set[remoteDataBouncer.size() + 1];
		for (int i = 0; i < remoteDataBouncer.size() + 1; i += 1) {
			tasksPerActorSystem[i] = new ArrayList<>();
			scoreListPerActorSystem[i] = new HashSet<>();
		}
		int taskCount = taskList.size();
		int actorSystemIndex = 1;
		while (taskList.size() > taskCount / tasksPerActorSystem.length) {
			List<SingleDifferenceSetTask> actorSystemList = tasksPerActorSystem[actorSystemIndex];
			Set<Integer> scoreList = scoreListPerActorSystem[actorSystemIndex];

			SingleDifferenceSetTask task = taskList.get(r.nextInt(taskList.size()));
			int score = (scoreList.contains(task.getSetA()) ? 0 : 1) + (scoreList.contains(task.getSetB()) ? 0 : 1);
			for (int i = 0; i < taskList.size(); i++) {
				SingleDifferenceSetTask testTask = taskList.get(i);
				int testScore = (scoreList.contains(testTask.getSetA()) ? 0 : 1) + (scoreList.contains(testTask.getSetB()) ? 0 : 1);
				if (testScore < score) {
					task = testTask;
					score = testScore;
				}
			}

			taskList.remove(task);
			actorSystemList.add(task);
			scoreList.add(task.getSetA());
			scoreList.add(task.getSetB());

			actorSystemIndex += 1;
			if (actorSystemIndex == tasksPerActorSystem.length) actorSystemIndex = 1;
		}

		for (SingleDifferenceSetTask task : taskList) {
			tasksPerActorSystem[0].add(task);
		}

		for (int i = 0; i < tasksPerActorSystem.length; i++) {
			if (i == 0) {
				HashSet<Integer> scoreList = new HashSet<>();
				for (SingleDifferenceSetTask task : tasksPerActorSystem[i]) {
					scoreList.add(task.getSetA());
					scoreList.add(task.getSetB());
				}
				this.log.info("ActorSystem {} has {} Task and {} different data batches", i, tasksPerActorSystem[i].size(), scoreList.size());
			} else {
				this.log.info("ActorSystem {} has {} Task and {} different data batches", i, tasksPerActorSystem[i].size(), scoreListPerActorSystem[i].size());
			}

		}

		int workerPerSystem = (remoteWorker.size() + localWorker.size()) / (remoteDataBouncer.size() + 1);
		for (int k = 0; k < tasksPerActorSystem.length; k++) {
			List<SingleDifferenceSetTask> actorSystemTasks = tasksPerActorSystem[k];

			int workerIndex = 0;
			while (!actorSystemTasks.isEmpty()) {
				tasksPerWorker[k * workerPerSystem + workerIndex].add(actorSystemTasks.remove(0));

				workerIndex += 1;
				workerIndex %= workerPerSystem;
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
//		this.log.info("Setup Data Bouncer[{} batches]. Can now request and send batches.", message.getBatchCount());
//		this.log.info("Connected to {} local worker, {} remote worker and {} remote data-bouncer", localWorker.size(), remoteWorker.size(), remoteDataBouncer.size());
		batches = new EncodedBatches(message.getBatchCount(), message.getBatchSizes());
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
			EncodedRow[] batch = batches.getBatch(message.getBatchIdentifier());
			int MAX_ROWS_PER_SPLIT = 100;
			int i = MAX_ROWS_PER_SPLIT * message.getNextSplit();

//			List<EncodedRow> split = new ArrayList<>();
//			for (int k = i; k - i < MAX_ROWS_PER_SPLIT && k < batch.length; k++) {
//				split.add(batch[k]);
//			}

			EncodedRow[] split = new EncodedRow[Math.min(MAX_ROWS_PER_SPLIT, batch.length - i)];
			int o = 0;
			for (int k = i; k - i < MAX_ROWS_PER_SPLIT && k < batch.length; k++) {
				split[o] = batch[k];
				o += 1;
			}

			this.sender().tell(new SendEncodedDataBatchMessage(message.getBatchIdentifier(), split,
					((i + 1) / MAX_ROWS_PER_SPLIT) + 1), this.self());
		} else {
			// local worker wants data
			if (batches == null) {
				workerWaitsForBatch.add(new ActorWaitsForBatchModel(this.sender(), message.getBatchIdentifier()));
				return;
			}

			if (batches.hasBatch(message.getBatchIdentifier())) {
				EncodedRow[] batch = batches.getBatch(message.getBatchIdentifier());
				EncodedRow[] clonedBatch = Arrays.copyOf(batch, batch.length);
				this.sender().tell(new SendEncodedDataBatchMessage(message.getBatchIdentifier(), clonedBatch, 1), this.self());
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
		if (batches.hasBatch(message.getBatchIdentifier())) {
			// notify every other dataBouncer that I have the full batch
			for (ActorRef actorRef : remoteDataBouncer) {
				actorRef.tell(new AddBatchRouteMessage(message.getBatchIdentifier()), this.sender());
			}

//			this.log.info("Received all splits for data batch {}, {} rows", message.getBatchIdentifier(), batches.getBatch(message.getBatchIdentifier()).size());
			batches.isBatchLoadingFinished(message.getBatchIdentifier());
			for (int i = 0; i < workerWaitsForBatch.size(); i++) {
				ActorWaitsForBatchModel waitFor = workerWaitsForBatch.get(i);
				if (waitFor.getBatchIdentifier() == message.getBatchIdentifier()) {
					EncodedRow[] clonedBatch = Arrays.copyOf(batches.getBatch(message.getBatchIdentifier()), batches.getBatch(message.getBatchIdentifier()).length);
					waitFor.getActor().tell(new SendEncodedDataBatchMessage(message.getBatchIdentifier(), clonedBatch, 1), this.self());
					workerWaitsForBatch.remove(i);
					i -= 1;
				}
			}
		} else {
			// immediately request new data batch
			this.sender().tell(new RequestDataBatchMessage(message.getBatchIdentifier(), message.getCurrentSplit()), this.self());
		}
	}

	private void handle(StartTreeSearchMessage message) {
		getContext().getSystem().actorOf(Reaper.props(), Reaper.DEFAULT_NAME);
//		getContext().getSystem().actorOf(PeerTreeSearchWorker.props(this.sender(), message.getMinimalDifferenceSets(), message.getColumnsInTable(), message.getWorkerInCluster()), PeerTreeSearchWorker.DEFAULT_NAME + localWorker.size() + getActorSystemID());

		getContext().stop(this.self());
//		this.log.info("Stop dataBouncer and create Reaper");
	}
}
