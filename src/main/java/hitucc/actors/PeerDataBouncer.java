package hitucc.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberJoined;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hitucc.HitUCCPeerHostSystem;
import hitucc.HitUCCPeerSystem;
import hitucc.actors.messages.*;
import hitucc.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	private BatchRoutingTable routingTable;
	private Batches batches;

	private TaskMessage task;

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
				.match(SendDataBatchMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\" from \"{}\"", object.getClass().getName(), this.sender().path().name()))
				.build();
	}

	private boolean isValidMember(Member member) {
		return member.hasRole(HitUCCPeerHostSystem.PEER_HOST_ROLE) || member.hasRole(HitUCCPeerSystem.PEER_ROLE);
	}

	private void register(Member member) {
		if (isValidMember(member)) {
			if(getActorSystemID().equals(getMemberPort(member))) {
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
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		register(message.member());
	}

	private void handle(MemberJoined message) {
//		register(message.member());
	}

	private void handle(RegistrationMessage message) {
		if (this.sender().equals(this.self())) return;
		if (remoteDataBouncer.contains(this.sender())) return;
		if (localWorker.contains(this.sender())) return;

		this.context().watch(this.sender());

		if (this.sender().path().name().contains(PeerDataBouncer.DEFAULT_NAME)) {
			remoteDataBouncer.add(this.sender());
			this.log.info("Registered {}; {} registered data bouncer", this.sender().path().name(), remoteDataBouncer.size());
			if(localWorker.size() == neededLocalWorkerCount) {
				this.sender().tell(new RegisterSystemMessage(this.self(), localWorker, getLeadingWorker()), this.self());
			}
		} else if (this.sender().path().name().startsWith(PeerWorker.DEFAULT_NAME)) {
			localWorker.add(this.sender());
			this.log.info("Registered {} local worker {}", localWorker.size(), this.sender().path().name());
			if(localWorker.size() == neededLocalWorkerCount) {
				for(ActorRef bouncer : remoteDataBouncer) {
					bouncer.tell(new RegisterSystemMessage(this.self(), localWorker, getLeadingWorker()), this.self());
				}
			}
		}

		this.sender().tell(new RegistrationMessage(), this.self());

		if(localWorker.size() == neededLocalWorkerCount) {
			if (task != null) {
				handle(task);
			}
		}
	}

	private ActorRef getLeadingWorker() {
		ActorRef leadingWorker = localWorker.get(0);
		for(int i = 1; i < localWorker.size(); i++) {
			if(leadingWorker.toString().compareTo(localWorker.get(i).toString()) > 0) {
				leadingWorker = localWorker.get(i);
			}
		}

		return leadingWorker;
	}

	private void handle(RegisterSystemMessage message) {
		this.log.info("register system");

		remoteDataBouncer.add(message.getDataBouncer());
		remoteWorker.addAll(message.getWorker());

		for(ActorRef actor : localWorker) {
			actor.tell(message, this.self());
		}

		registeredSystems += 1;
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
			this.log.info("The Data Duplication Factor is set to auto: Factor is set to " + batchCount);
		}
		if (batchCount == 1) {
			this.log.info("The Data Duplication Factor is set to 1. The program therefore cannot distribute the algorithm. This is not that bad, but it slows down the execution time significantly.");
		}

		routingTable = new BatchRoutingTable(batchCount, this.self());
		for (ActorRef remoteDataBouncer : remoteDataBouncer) {
			remoteDataBouncer.tell(new SetupDataBouncerMessage(batchCount), this.self());
		}

		batches = new Batches(batchCount);
		Random random = new Random();
		for (String[] rawRow : table) {
			batches.getBatch(random.nextInt(batchCount)).add(new Row(rawRow));
		}

		for (int i = 0; i < batchCount; i++) {
			this.log.info("Batch {} has {} rows", i, batches.getBatch(i).size());
		}
		List<SingleDifferenceSetTask>[] tasksPerWorker = new List[workerInCluster()];
		for (int i = 0; i < workerInCluster(); i += 1) {
			tasksPerWorker[i] = new ArrayList<>();
		}

		int workerIndex = 0;
		for (int i = 0; i < batchCount; i += 1) {
			for (int k = i; k < batchCount; k += 1) {
				tasksPerWorker[workerIndex % workerInCluster()].add(new SingleDifferenceSetTask(i, k));
				workerIndex += 1;
			}
		}

		for (int i = 0; i < workerInCluster(); i += 1) {
			StringBuilder taskString = new StringBuilder();
			for (SingleDifferenceSetTask t : tasksPerWorker[i])
				taskString.append(" ").append(t.getSetA()).append("|").append(t.getSetB());
			this.log.info("Tasks for worker {}:{}", i, taskString);
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
			worker.tell(new FindDifferenceSetFromBatchMessage(tasksA, tasksB, batchCount, task.isNullEqualsNull()), this.self());
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
		batches = new Batches(message.getBatchCount());
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
			List<Row> batch = batches.getBatch(message.getBatchIdentifier());
			int MAX_ROWS_PER_SPLIT = 100;
			int i = MAX_ROWS_PER_SPLIT * message.getNextSplit();
			List<Row> split = new ArrayList<>();
			for (int k = i; k - i < MAX_ROWS_PER_SPLIT && k < batch.size(); k++) {
				split.add(batch.get(k));
			}
			this.sender().tell(new SendDataBatchMessage(message.getBatchIdentifier(), split,
					((i + 1) / MAX_ROWS_PER_SPLIT) + 1, (int) Math.ceil(1f * batch.size() / MAX_ROWS_PER_SPLIT)), this.self());
			if (((i + 1) / MAX_ROWS_PER_SPLIT) + 1 == split.size()) {
				this.log.info("Send {} splits to remote data bouncer[{} rows]", ((i + 1) / MAX_ROWS_PER_SPLIT) + 1, split.size());
			}
		} else {
			// local worker wants data
			if (batches == null) {
				workerWaitsForBatch.add(new ActorWaitsForBatchModel(this.sender(), message.getBatchIdentifier()));
				return;
			}

			if (batches.hasBatch(message.getBatchIdentifier())) {
				List<Row> batch = batches.getBatch(message.getBatchIdentifier());
				this.sender().tell(new SendDataBatchMessage(message.getBatchIdentifier(), batch, 1, 1), this.self());
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

	private void handle(SendDataBatchMessage message) {
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
					waitFor.getActor().tell(new SendDataBatchMessage(message.getBatchIdentifier(), batches.getBatch(message.getBatchIdentifier()), 1, 1), this.self());
					workerWaitsForBatch.remove(i);
					i -= 1;
				}
			}
		} else {
			// immediately request new data batch
			this.sender().tell(new RequestDataBatchMessage(message.getBatchIdentifier(), message.getCurrentSplit()), this.self());
		}
	}

	private void handle(ReportAndShutdownMessage message) {
		shutdownActorSystem();
	}

	private void shutdownActorSystem() {
		for (ActorRef worker : localWorker) {
			worker.tell(new ReportAndShutdownMessage(), this.self());
		}
		this.getContext().stop(this.self());
		getContext().getSystem().terminate();
	}
}
