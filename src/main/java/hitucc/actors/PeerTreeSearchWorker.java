package hitucc.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hitucc.actors.messages.*;
import hitucc.behaviour.oracle.HittingSetOracle;
import hitucc.model.AlgorithmTimerObject;
import hitucc.model.SerializableBitSet;
import hitucc.model.TreeTask;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeerTreeSearchWorker extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-tree-search-worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());
	private final List<ActorRef> otherWorker = new ArrayList<>();
	private final int columnCount;
	private final int maxLocalTreeDepth = 1000;
	private final SerializableBitSet[] minimalDifferenceSets;
	private final List<SerializableBitSet> discoveredUCCs = new ArrayList<>();
	private final ArrayDeque<TreeTask> backlogWorkStack = new ArrayDeque<>(maxLocalTreeDepth);
	private int finishedActorCount;
	private boolean dirtyAskActorIndex;
	private boolean waitForShutdown;
	private int workerInClusterCount;
	private int localTreeDepth = 0;
	private int askActorIndex = 0;
	private AlgorithmTimerObject timerObject;
	private boolean shouldOutputFile = false;
	private ActorRef initiator;

	public PeerTreeSearchWorker(ActorRef initiator, SerializableBitSet[] minimalDifferenceSets, int columnsInTable, int workerInClusterCount) {
		this.minimalDifferenceSets = minimalDifferenceSets;
		this.columnCount = columnsInTable;
		this.workerInClusterCount = workerInClusterCount;
		this.initiator = initiator;

		initiator.tell(new RegistrationMessage(), this.self());
	}

	public PeerTreeSearchWorker(ActorRef[] clusterWorker, SerializableBitSet[] minimalDifferenceSets, int columnsInTable, AlgorithmTimerObject timerObject) {
		this.minimalDifferenceSets = minimalDifferenceSets;
		this.columnCount = columnsInTable;
		this.workerInClusterCount = clusterWorker.length + 1;
		this.log.info("{}/{} Worker in cluster", 1, workerInClusterCount);

		this.timerObject = timerObject;
		this.shouldOutputFile = true;

		for (ActorRef actor : clusterWorker) {
			actor.tell(new StartTreeSearchMessage(minimalDifferenceSets, workerInClusterCount, columnsInTable), this.self());
		}

		if (workerInClusterCount == 1) {
			startTreeSearch();
		}
	}

	public static Props props(ActorRef initiator, SerializableBitSet[] minimalDifferenceSets, int columnsInTable, int workerInClusterCount) {
		return Props.create(PeerTreeSearchWorker.class, () -> new PeerTreeSearchWorker(initiator, minimalDifferenceSets, columnsInTable, workerInClusterCount));
	}

	public static Props props(ActorRef[] clusterWorker, SerializableBitSet[] minimalDifferenceSets, int columnsInTable, AlgorithmTimerObject timerObject) {
		return Props.create(PeerTreeSearchWorker.class, () -> new PeerTreeSearchWorker(clusterWorker, minimalDifferenceSets, columnsInTable, timerObject));
	}

	@Override
	public void preStart() {
		Reaper.watchWithDefaultReaper(this);
		this.cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegistrationMessage.class, this::handle)
				.match(RegisterClusterMessage.class, this::handle)
				.match(ClusterEvent.MemberUp.class, memberUp -> {
				})
				.match(NeedTreeWorkMessage.class, this::handle)
				.match(TreeWorkMessage.class, this::handle)
				.match(NoTreeWorkMessage.class, this::handle)
				.match(FinishedTreeSearchMessage.class, this::handle)
				.match(PrepareForShutdownMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\" from \"{}\"", object.getClass().getName(), this.sender().path().name()))
				.build();
	}

	private void handle(RegistrationMessage message) {
		otherWorker.add(this.sender());
		this.log.info("{}/{} Worker in cluster", otherWorker.size() + 1, workerInClusterCount);

		if (otherWorker.size() + 1 == workerInClusterCount) {
			this.log.info("start tree search");
			startTreeSearch();
		}
	}

	private void startTreeSearch() {
		ActorRef[] allWorker = new ActorRef[workerInClusterCount];
		allWorker[0] = this.self();
		for (int i = 0; i < otherWorker.size(); i++) {
			allWorker[i + 1] = otherWorker.get(i);
		}

		for (ActorRef worker : otherWorker) {
			worker.tell(new RegisterClusterMessage(allWorker), this.self());
		}

//		for(SerializableBitSet differenceSet : minimalDifferenceSets) {
//			this.log.info(toUCC(differenceSet));
//		}

		SerializableBitSet x = new SerializableBitSet(columnCount);
		SerializableBitSet y = new SerializableBitSet(columnCount);

		backlogWorkStack.add(new TreeTask(x, y, 0, columnCount));

		for (ActorRef worker : otherWorker) {
			worker.tell(new TreeWorkMessage(null), this.self());
		}

		handleNextTask();
	}

	private void handle(RegisterClusterMessage message) {
		for (ActorRef worker : message.getClusterWorker()) {
			if (!this.self().equals(worker)) {
				otherWorker.add(worker);
			}
		}

		Collections.shuffle(otherWorker);
		workerInClusterCount = otherWorker.size() + 1;
	}

	private void handle(PrepareForShutdownMessage message) {
		waitForShutdown = true;
		if (initiator != null) initiator.tell(new FinishedTreeSearchMessage(discoveredUCCs), this.self());
		this.log.info("Finished all work!");
	}

	private void handle(FinishedTreeSearchMessage message) {
		finishedActorCount++;

		discoveredUCCs.addAll(message.getDiscoveredUCCs());

		if (finishedActorCount == otherWorker.size()) {
			for (ActorRef worker : otherWorker) {
				worker.tell(new ReportAndShutdownMessage(), this.self());
			}
			this.self().tell(new ReportAndShutdownMessage(), this.self());
		}
	}

	private void handle(NoTreeWorkMessage message) {
		askActorIndex += 1;

		if (askActorIndex >= otherWorker.size()) {
			if (dirtyAskActorIndex) {
				dirtyAskActorIndex = false;
				askActorIndex = 0;
				otherWorker.get(askActorIndex).tell(new NeedTreeWorkMessage(), this.self());
				return;
			}

			waitForShutdown = true;
			if (initiator != null) initiator.tell(new FinishedTreeSearchMessage(discoveredUCCs), this.self());
			this.log.info("Finished all work!");
		} else {
			otherWorker.get(askActorIndex).tell(new NeedTreeWorkMessage(), this.self());
		}
	}

	private void handle(NeedTreeWorkMessage message) {
		if (backlogWorkStack.isEmpty()) {
			if (waitForShutdown) this.sender().tell(new PrepareForShutdownMessage(), this.self());
			else this.sender().tell(new NoTreeWorkMessage(), this.self());
		} else {
			if (backlogWorkStack.size() > 1) {
				this.sender().tell(new TreeWorkMessage(backlogWorkStack.removeFirst()), this.self());
			} else {
				this.sender().tell(new TreeWorkMessage(null), this.self());
			}
		}
	}

	private void handle(TreeWorkMessage message) {
		if (askActorIndex != 0 && !this.sender().equals(this.self())) {
			dirtyAskActorIndex = true;
		}

		if (message.getTask() != null) backlogWorkStack.add(message.getTask());

		localTreeDepth = 0;
		handleNextTask();
	}

	private void handleNextTask() {
		if (backlogWorkStack.isEmpty()) {
			if (otherWorker.size() > 0) {
				otherWorker.get(askActorIndex).tell(new NeedTreeWorkMessage(), this.self());
			} else {
				this.self().tell(new ReportAndShutdownMessage(), this.self());
			}

			return;
		}

		localTreeDepth += 1;
		if (localTreeDepth >= maxLocalTreeDepth) {
			this.self().tell(new TreeWorkMessage(null), this.self());
			return;
		}

		TreeTask currentTask = backlogWorkStack.removeLast();

		SerializableBitSet x = currentTask.getX();
		SerializableBitSet y = currentTask.getY();
		int length = currentTask.getLength();

		handleLocal(x, y, length);
	}

	private void handleLocal(SerializableBitSet x, SerializableBitSet y, int length) {
		HittingSetOracle.Status result = HittingSetOracle.extendable(x, y, length, minimalDifferenceSets, columnCount);
//		this.log.info("Oracle says {}", result);
		switch (result) {
			case MINIMAL:
				this.report(x);
				break;
			case EXTENDABLE:
				this.split(x, y, length);
				break;
			case NOT_EXTENDABLE:
				// Ignore
				break;
			case FAILED:
				this.log.error("Oracle failed :(");
				break;
		}

		handleNextTask();
	}

	private void report(SerializableBitSet ucc) {
//		this.log.info("SET {}", DifferenceSetDetector.SerializableBitSetToString(ucc, columnCount));
//		this.log.info("UCC: {}", toUCC(ucc));

		discoveredUCCs.add(ucc);
	}

	private void split(SerializableBitSet x, SerializableBitSet y, int next) {
		if (next < columnCount) {
			SerializableBitSet xNew = copySerializableBitSet(x, columnCount);
			xNew.set(next);
			backlogWorkStack.add(new TreeTask(xNew, y, next + 1, columnCount));

			SerializableBitSet yNew = copySerializableBitSet(y, columnCount);
			yNew.set(next);
			backlogWorkStack.add(new TreeTask(x, yNew, next + 1, columnCount));
		} else {
			this.log.info("WHY IS THIS? ################################### This is not an error - just wanted to check if this branch can actually be reached ;)");
		}
	}

	private String beautifyJson(String jsonString) {
		return jsonString.replaceAll(",", ",\n\t").replaceAll(":", ": ").replaceAll("\\{", "{\n\t").replaceAll("}", "\n}");
	}

	private void handle(ReportAndShutdownMessage message) {

		if (shouldOutputFile) {
			timerObject.setFinishTime();


			JSONObject obj = new JSONObject();
			obj.put("Dataset Name", "?");
			obj.put("Encode Data Runtime", timerObject.toSeconds(timerObject.getDictionaryRuntime()));
			obj.put("Build Difference Sets Runtime", timerObject.toSeconds(timerObject.getPhaseOneRuntime()));
			obj.put("Tree Search Runtime", timerObject.toSeconds(timerObject.getPhaseTwoRuntime()));
			obj.put("Algorithm Runtime", timerObject.toSeconds(timerObject.getCompleteRuntime()));
			obj.put("Minimal UCC Count", discoveredUCCs.size());

			this.log.info("Discovered {} UCCs", discoveredUCCs.size());
			this.log.info(beautifyJson(obj.toJSONString()));

//			JSONArray results = new JSONArray();
//			for (SerializableBitSet bitSet : discoveredUCCs) {
//				results.add(toUCC(bitSet));
//			}
//			obj.put("results", results);

			try (FileWriter file = new FileWriter("test-results.json")) {
				file.write(beautifyJson(obj.toJSONString()));
				this.log.info("Successfully Copied JSON Object to File (Path: {})", file);
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.getContext().stop(this.self());
	}

	private SerializableBitSet copySerializableBitSet(SerializableBitSet set, int newLength) {
		SerializableBitSet copy = new SerializableBitSet(newLength);
		for (int i = 0; i < set.logicalLength(); i++) {
			if (set.get(i)) copy.set(i);
		}

		return copy;
	}

	private String toUCC(SerializableBitSet SerializableBitSet) {
		if (SerializableBitSet.logicalLength() == 0) return "";

		StringBuilder output = new StringBuilder();
		for (int i = 0; i < SerializableBitSet.logicalLength() - 1; i++) {
			if (SerializableBitSet.get(i)) {
				output.append(i).append(", ");
			}
		}
		if (SerializableBitSet.get(SerializableBitSet.logicalLength() - 1)) {
			output.append(SerializableBitSet.logicalLength() - 1);
		}
		return output.toString();
	}
}
