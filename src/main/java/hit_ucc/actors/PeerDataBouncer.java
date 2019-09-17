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
import hit_ucc.model.Batches;
import hit_ucc.model.Row;
import hit_ucc.model.SingleDifferenceSetTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PeerDataBouncer extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-data-bouncer";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());

	private final boolean useDictionaryEncoding = true;
	private List<ActorRef> doormans = new ArrayList<>();
	private List<ActorRef> localWorker = new ArrayList<>();

	private Batches batches;

	public static Props props() {
		return Props.create(PeerDataBouncer.class);
	}

	@Override
	public void preStart() {
		this.cluster.subscribe(this.self(), MemberUp.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CurrentClusterState.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(TaskMessage.class, this::handle)
				.match(RequestDataBatchMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\"", object.getClass().getName()))
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
		if (doormans.contains(this.sender())) return;
		if (localWorker.contains(this.sender())) return;

		this.context().watch(this.sender());

		if (this.sender().path().name().equals(PeerDataBouncer.DEFAULT_NAME)) {
			doormans.add(this.sender());
			this.log.info("Registered {}; {} registered doorman", this.sender().path().name(), doormans.size());
		} else {
			localWorker.add(this.sender());
			this.log.info("Registered {}; {} registered local worker", this.sender().path().name(), localWorker.size());
		}

		this.sender().tell(new RegistrationMessage(), this.self());
	}

	private void handle(TaskMessage task) {
		this.log.info("Received Task Message with table of size [row: {}, columns: {}]", task.getInputFile().length, task.getInputFile()[0].length);

		String[][] table = task.getInputFile();
		int batchCount = task.getDataDuplicationFactor();

		if (batchCount < 1) {
			this.log.info("Connected to " + localWorker.size() + " worker.");
			batchCount
					= 0;
			int triangleCount = 0;
			while (triangleCount < localWorker.size() + 1) {
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

		int triangleCount = 0;
		for (int i = 0; i < batchCount; i++) {
			triangleCount += i + 1;
		}

		batches = new Batches(batchCount);
		Random random = new Random();
		for (String[] rawRow : table) {
			batches.getBatch(random.nextInt(batchCount)).add(new Row(rawRow));
		}

		for (int i = 0; i < batchCount; i++) {
			this.log.info("Batch {} has {} rows", i, batches.getBatch(i).size());
		}
		List<SingleDifferenceSetTask>[] tasksPerWorker = new List[localWorker.size()];
		for (int i = 0; i < localWorker.size(); i += 1) {
			tasksPerWorker[i] = new ArrayList<>();
		}

		int workerIndex = 0;
		for (int i = 0; i < batchCount; i += 1) {
			for (int k = i; k < batchCount; k += 1) {
				tasksPerWorker[workerIndex % localWorker.size()].add(new SingleDifferenceSetTask(i, k));
				workerIndex += 1;
			}
		}

		for (int i = 0; i < localWorker.size(); i += 1) {
			StringBuilder taskString = new StringBuilder();
			for(SingleDifferenceSetTask t : tasksPerWorker[i]) taskString.append(" ").append(t.getSetA()).append("|").append(t.getSetB());
			this.log.info("Tasks for worker {}:{}", i, taskString);
			localWorker.get(i).tell(new FindDifferenceSetFromBatchMessage(tasksPerWorker[i], batchCount, task.isNullEqualsNull()), this.self());
		}

		this.log.info("Dispatched {} rows. (duplication factor of {})", batchCount * table.length, batchCount);

//		endEverything();
	}

	private void handle(RequestDataBatchMessage message) {
		this.sender().tell(new SendDataBatchMessage(message.getBatchIdentifier(), batches.getBatch(message.getBatchIdentifier())), this.self());
	}

	private void handle(ReportAndShutdownMessage message) {
		endEverything();
	}

	private void endEverything() {
		for (ActorRef worker : localWorker) {
			worker.tell(new ReportAndShutdownMessage(), this.self());
		}
		this.getContext().stop(this.self());
		getContext().getSystem().terminate();
	}
}
