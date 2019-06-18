package de.hpi.hit_ucc.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.hpi.hit_ucc.DifferenceSetDetector;
import de.hpi.hit_ucc.actors.messages.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

public class Profiler extends AbstractActor {

	public static final String DEFAULT_NAME = "profiler";
	private final static boolean USE_ONLY_MINIMAL_DIFFERENCE_SETS = true;
	private final static boolean CUT_DUPLICATES_IN_STEP_TWO = true;
	////////////////////////
	// Actor Construction //
	////////////////////////
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Queue<IWorkMessage> unassignedWork = new LinkedList<>();
	private final Queue<ActorRef> idleWorkers = new LinkedList<>();
	private final Map<ActorRef, IWorkMessage> busyWorkers = new HashMap<>();

	private TaskMessage task;

	private List<BitSet> differenceSets = new ArrayList<>();
	// 1: minimal, -1: not minimal, 0: unassigned
	private int[] minimalState;

	private BitSet[] minimalDifferenceSets;
	private List<BitSet> foundUCCs = new ArrayList<>();

	private long startTime = 0;
	private long endTime = 0;

	/////////////////
	// Actor WorkerState //
	/////////////////

	public static Props props() {
		return Props.create(Profiler.class);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegistrationMessage.class, this::handle)
				.match(Terminated.class, this::handle)
				.match(TaskMessage.class, this::handle)
				.match(FoundDifferenceSetMessage.class, this::handle)
				.match(TestedMinimalDifferenceSet.class, this::handle)
				.match(OracleCompletedMessage.class, this::handle)
				.matchAny(object -> this.log.info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void handle(RegistrationMessage message) {
		this.context().watch(this.sender());

		this.assign(this.sender());
		this.log.info("Registered {}", this.sender());
	}

	////////////////////
	// Actor Behavior //
	////////////////////

	private void prepareStepOne(String[][] table) {
		log.info("Start find difference sets:");

		for (int indexA = 0; indexA < table.length; indexA++) {
			for (int indexB = indexA + 1; indexB < table.length; indexB++) {
				String[] rowA = table[indexA];
				String[] rowB = table[indexB];

				assign(new FindDifferenceSetMessage(rowA, rowB));
			}
		}
	}

	private void prepareStepTwo() {
		if (!USE_ONLY_MINIMAL_DIFFERENCE_SETS) {
			log.info("Skip Step Two");

			if (CUT_DUPLICATES_IN_STEP_TWO) {
				differenceSets = new ArrayList<>(new HashSet<>(differenceSets));
				log.info("Removed duplicate difference sets and cut count to {} sets.", differenceSets.size());
			}

			minimalDifferenceSets = new BitSet[differenceSets.size()];
			differenceSets.toArray(minimalDifferenceSets);

			prepareStepThree();
			return;
		}

		log.info("Found {} difference sets.", differenceSets.size());
		if (CUT_DUPLICATES_IN_STEP_TWO) {
			differenceSets = new ArrayList<>(new HashSet<>(differenceSets));
			log.info("Removed duplicate difference sets and cut count to {} sets.", differenceSets.size());
		}

		log.info("Start find minimal difference sets:");

		minimalState = new int[differenceSets.size()];

		for (int i = 0; i < differenceSets.size(); i++) {
			for (int k = i + 1; k < differenceSets.size(); k++) {
				assign(new TestMinimalDifferenceSetMessage(differenceSets.get(i), i, differenceSets.get(k), k));
			}
		}
	}

	private void endStepTwo() {
		List<BitSet> tempList = new ArrayList<>();

		for (int i = 0; i < minimalState.length; i++) {
			if (minimalState[i] == 0) {
				tempList.add(differenceSets.get(i));
			}
		}

		minimalDifferenceSets = new BitSet[tempList.size()];
		tempList.toArray(minimalDifferenceSets);

		log.info("Found following minimal difference sets:");
		for (BitSet differenceSet : minimalDifferenceSets) {
			log.info(DifferenceSetDetector.bitSetToString(differenceSet, task.getAttributes()));
		}

		differenceSets.clear();
		minimalState = new int[0];
	}

	private void prepareStepThree() {
		log.info("Start find hitting sets from {} difference sets:", minimalDifferenceSets.length);

		BitSet x = new BitSet(task.getAttributes());
		BitSet y = new BitSet(task.getAttributes());
		this.assign(new TreeOracleMessage(x, y, 0, minimalDifferenceSets, task.getAttributes()));
	}

	private void handle(TaskMessage task) {
		startTime = System.currentTimeMillis();

		this.task = task;

		String[][] table = task.getInputFile();

		prepareStepOne(table);
	}

	private void handle(FoundDifferenceSetMessage message) {
		ActorRef worker = this.sender();
		this.busyWorkers.remove(worker);
		assign(worker);

		differenceSets.add(message.getHittingSet());

		if (busyWorkers.isEmpty()) {
			prepareStepTwo();
		}
	}

	private void handle(TestedMinimalDifferenceSet message) {
		ActorRef worker = this.sender();
		this.busyWorkers.remove(worker);
		assign(worker);

		if (message.getResult() == DifferenceSetDetector.FIRST_MINIMAL) {
			minimalState[message.getIndexB()] = -1;
		}
		if (message.getResult() == DifferenceSetDetector.SECOND_MINIMAL) {
			minimalState[message.getIndexA()] = -1;
		}

		if (busyWorkers.isEmpty()) {
			endStepTwo();
			prepareStepThree();
		}
	}

	private void handle(Terminated message) {
		this.context().unwatch(message.getActor());
//
//		if (!this.idleWorkers.remove(message.getActor())) {
//			WorkMessage work = this.busyWorkers.remove(message.getActor());
//			if (work != null) {
//				this.assign(work);
//			}
//		}
		this.log.info("Unregistered {}", message.getActor());
	}

	private void handle(OracleCompletedMessage message) {
		ActorRef worker = this.sender();
		TreeOracleMessage work = (TreeOracleMessage) this.busyWorkers.remove(worker);

//		this.log.info("Completed: [{} | {}] => {}", DifferenceSetDetector.bitSetToString(work.getX()), DifferenceSetDetector.bitSetToString(work.getY()), message.getResult());

		switch (message.getResult()) {
			case MINIMAL:
				this.report(work);
				break;
			case EXTENDABLE:
				this.split(work);
				break;
			case NOT_EXTENDABLE:
				// Ignore
				break;
			case FAILED:
				this.assign(work);
				break;
		}

		this.assign(worker);

		if (busyWorkers.isEmpty() && unassignedWork.isEmpty()) {
			endTime = System.currentTimeMillis();
			log.info("FINISHED UCC DETECTION in {} seconds", (endTime - startTime) / 1000f);

			for (BitSet ucc : foundUCCs) {
				this.log.info("UCC: {}", toUCC(ucc));
			}
		}
	}

	private void report(TreeOracleMessage work) {
		this.log.info("UCC: {}", DifferenceSetDetector.bitSetToString(work.getX(), work.getNumAttributes()));
		foundUCCs.add(work.getX());
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

	private void split(TreeOracleMessage work) {
		BitSet x = work.getX();
		BitSet y = work.getY();
		BitSet[] minimalDifferenceSets = work.getDifferenceSets();

		int next = work.getLength();

		if (next < this.task.getAttributes()) {
			BitSet xNew = copyBitSet(x, next);
			xNew.set(next);
			this.assign(new TreeOracleMessage(xNew, y, next + 1, minimalDifferenceSets, task.getAttributes()));

			BitSet yNew = copyBitSet(y, next);
			yNew.set(next);
			this.assign(new TreeOracleMessage(x, yNew, next + 1, minimalDifferenceSets, task.getAttributes()));
		}
	}

	private BitSet copyBitSet(BitSet set, int newLength) {
		BitSet copy = new BitSet(newLength);
		for (int i = 0; i < set.length(); i++) {
			if (set.get(i)) copy.set(i);
		}

		return copy;
	}

	private void assign(IWorkMessage work) {
		ActorRef worker = this.idleWorkers.poll();

		if (worker == null) {
			this.unassignedWork.add(work);
			return;
		}

		this.busyWorkers.put(worker, work);
		worker.tell(work, this.self());
	}

	private void assign(ActorRef worker) {
		IWorkMessage work = this.unassignedWork.poll();

		if (work == null) {
			this.idleWorkers.add(worker);
			return;
		}

		this.busyWorkers.put(worker, work);
		worker.tell(work, this.self());
	}

	@Data
	@AllArgsConstructor
	public static class RegistrationMessage implements Serializable {
		private static final long serialVersionUID = 4545299661052078209L;
	}
}