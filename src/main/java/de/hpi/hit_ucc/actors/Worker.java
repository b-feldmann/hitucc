package de.hpi.hit_ucc.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.hpi.hit_ucc.behaviour.differenceSets.AbstractDifferenceSetDetector;
import de.hpi.hit_ucc.HittingSetOracle;
import de.hpi.hit_ucc.HitUCCMaster;
import de.hpi.hit_ucc.actors.Profiler.RegistrationMessage;
import de.hpi.hit_ucc.actors.messages.*;

import java.util.BitSet;

public class Worker extends AbstractActor {

	////////////////////////
	// Actor Construction //
	////////////////////////

	public static final String DEFAULT_NAME = "worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);

	////////////////////
	// Actor Messages //
	////////////////////

	/////////////////
	// Actor WorkerState //
	/////////////////
	private final Cluster cluster = Cluster.get(this.context().system());

	public static Props props() {
		return Props.create(Worker.class);
	}

	/////////////////////
	// Actor Lifecycle //
	/////////////////////

	@Override
	public void preStart() {
		this.cluster.subscribe(this.self(), MemberUp.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	////////////////////
	// Actor Behavior //
	////////////////////

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CurrentClusterState.class, this::handle)
				.match(MemberUp.class, this::handle)
				.match(FindDifferenceSetMessage.class, this::handle)
				.match(TestMinimalDifferenceSetMessage.class, this::handle)
				.match(TreeOracleMessage.class, this::handle)
				.matchAny(object -> this.log.info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void handle(CurrentClusterState message) {
		message.getMembers().forEach(member -> {
			if (member.status().equals(MemberStatus.up()))
				this.register(member);
		});
	}

	private void handle(MemberUp message) {
		this.register(message.member());
	}

	private void register(Member member) {
		if (member.hasRole(HitUCCMaster.MASTER_ROLE))
			this.getContext()
					.actorSelection(member.address() + "/user/" + Profiler.DEFAULT_NAME)
					.tell(new RegistrationMessage(), this.self());
	}

	private void handle(FindDifferenceSetMessage message) {
		BitSet hittingSet = AbstractDifferenceSetDetector.calculateDifferenceSet(message.getRowA(), message.getRowB());

		this.sender().tell(new FoundDifferenceSetMessage(hittingSet), this.self());
	}

	private void handle(TestMinimalDifferenceSetMessage message) {
		int testResult = AbstractDifferenceSetDetector.testStaticMinimalHittingSet(message.getDifferenceSetA(), message.getDifferenceSetB());

		this.sender().tell(new TestedMinimalDifferenceSet(testResult, message.getIndexA(), message.getIndexB()), this.self());
	}

	private void handle(TreeOracleMessage message) {
		BitSet x = message.getX();
		BitSet y = message.getY();
		int length = message.getLength();
		BitSet[] differenceSets = message.getDifferenceSets();
		int numAttributes = message.getNumAttributes();

		HittingSetOracle.Status result = HittingSetOracle.extendable(x, y, length, differenceSets, numAttributes);
		this.sender().tell(new OracleCompletedMessage(result), this.self());
	}

//	private void handle(WorkMessage message) {
//		long y = 0;
//		for (int i = 0; i < 1000000; i++)
//			if (this.isPrime(i))
//				y = y + i;
//
//		this.log.info("done: " + y);
//
//		this.sender().tell(new CompletionMessage(CompletionMessage.status.EXTENDABLE), this.self());
//	}

	private boolean isPrime(long n) {
		if (n == 1)
			return false;

		// Check for the most basic primes
		if (n == 2 || n == 3)
			return true;

		// Check if n is an even number
		if (n % 2 == 0)
			return false;

		// Check the odds
		for (long i = 3; i * i <= n; i += 2)
			if (n % i == 0)
				return false;

		return true;
	}
}