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
import hitucc.model.SerializableBitSet;
import hitucc.model.TreeTask;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class PeerTreeSearchWorker extends AbstractActor {
	public static final String DEFAULT_NAME = "peer-tree-search-worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());
	private long treeSearchStart = 0;
	private int finishedActorCount;
	private boolean dirtyAskActorIndex;
	private boolean waitForShutdown;
	private int waitForUccCount = -1;

	private int workerInClusterCount;

	private List<ActorRef> otherWorker = new ArrayList<>();
	private int columnCount;
	private int localTreeDepth = 0;
	private int maxLocalTreeDepth = 1000;
	private int askActorIndex = 0;
	private SerializableBitSet[] minimalDifferenceSets;
	private List<SerializableBitSet> discoveredUCCs = new ArrayList<>();
	private ArrayDeque<TreeTask> backlogWorkStack = new ArrayDeque<>(maxLocalTreeDepth);

	private boolean shouldOutputFile = false;

	public PeerTreeSearchWorker(ActorRef initiator, SerializableBitSet[] minimalDifferenceSets, int columnsInTable, int workerInClusterCount) {
		this.minimalDifferenceSets = minimalDifferenceSets;
		this.columnCount = columnsInTable;
		this.workerInClusterCount = workerInClusterCount;

		initiator.tell(new RegistrationMessage(), this.self());
	}

	public PeerTreeSearchWorker(ActorRef[] clusterWorker, SerializableBitSet[] minimalDifferenceSets, int columnsInTable) {
		this.minimalDifferenceSets = minimalDifferenceSets;
		this.columnCount = columnsInTable;
		this.workerInClusterCount = clusterWorker.length + 1;
		this.log.info("{}/{} Worker in cluster", 1, workerInClusterCount);

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

	public static Props props(ActorRef[] clusterWorker, SerializableBitSet[] minimalDifferenceSets, int columnsInTable) {
		return Props.create(PeerTreeSearchWorker.class, () -> new PeerTreeSearchWorker(clusterWorker, minimalDifferenceSets, columnsInTable));
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

	private boolean priority(ActorRef A, ActorRef B) {
		if (A.path().name().equals(B.path().name())) {
			this.log.error("Actor String is not unique >.<");
		}

		return A.path().name().compareTo(B.path().name()) < 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegistrationMessage.class, this::handle)
				.match(RegisterClusterMessage.class, this::handle)
				.match(ClusterEvent.MemberUp.class, memberUp -> {
				})
				.match(NeedTreeWorkMessage.class, this::handle)
				.match(NeedTreeWorkOrFinishMessage.class, this::handle)
				.match(TreeWorkMessage.class, this::handle)
				.match(NoTreeWorkMessage.class, this::handle)
				.match(NoTreeWorkAndFinishMessage.class, this::handle)
				.match(UCCDiscoveredMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\" from \"{}\"", object.getClass().getName(), this.sender().path().name()))
				.build();
	}

	private void handle(RegistrationMessage message) {
		otherWorker.add(this.sender());
		this.log.info("{}/{} Worker in cluster", otherWorker.size() + 1, workerInClusterCount);

		if (otherWorker.size() + 1 == workerInClusterCount) {
			otherWorker.sort((o1, o2) -> priority(o1, o2) ? -1 : 1);
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

		treeSearchStart = System.currentTimeMillis();
		SerializableBitSet x = new SerializableBitSet(columnCount);
		SerializableBitSet y = new SerializableBitSet(columnCount);

		backlogWorkStack.add(new TreeTask(x, y, 0, columnCount));

		for (ActorRef worker : otherWorker) {
			worker.tell(new TreeWorkMessage(new ArrayDeque<>()), this.self());
		}

		handleNextTask();
	}

	private void handle(RegisterClusterMessage message) {
		for (ActorRef worker : message.getClusterWorker()) {
			if (!this.self().equals(worker)) {
				otherWorker.add(worker);
			}
		}
		workerInClusterCount = otherWorker.size() + 1;

		otherWorker.sort((o1, o2) -> priority(o1, o2) ? -1 : 1);
	}

	private void handle(NoTreeWorkAndFinishMessage message) {
		if (askActorIndex < otherWorker.size() - 2) {
			askActorIndex = otherWorker.size() - 2;
			dirtyAskActorIndex = false;
		}

		this.log.info("No Tree Work and Finish Message");
		handle(new NoTreeWorkMessage());
	}

	private void handle(NoTreeWorkMessage message) {
		askActorIndex += 1;

		if (askActorIndex >= otherWorker.size()) {
			if (dirtyAskActorIndex) {
				askActorIndex = 0;
				otherWorker.get(askActorIndex).tell(new NeedTreeWorkMessage(), this.self());
				return;
			}

			waitForShutdown = true;
			this.log.info("Finished all work and can't get any other");
		} else {
			if (askActorIndex == otherWorker.size() - 1) {
				otherWorker.get(askActorIndex).tell(new NeedTreeWorkOrFinishMessage(), this.self());
			} else otherWorker.get(askActorIndex).tell(new NeedTreeWorkMessage(), this.self());
		}
	}

	private void handle(NeedTreeWorkOrFinishMessage message) {
		handle(new NeedTreeWorkMessage());

		if (backlogWorkStack.isEmpty()) {
			finishedActorCount += 1;

			if (finishedActorCount == otherWorker.size()) {
				for (ActorRef worker : otherWorker) {
					worker.tell(new ReportAndShutdownMessage(discoveredUCCs.size()), this.self());
				}
				this.self().tell(new ReportAndShutdownMessage(discoveredUCCs.size()), this.self());
			}
		}
	}

	private void handle(NeedTreeWorkMessage message) {
//		this.log.info("Receive Need Work Message from {}", this.sender().path().name());
		if (backlogWorkStack.isEmpty()) {
//			this.log.info("Send no work");
			if (waitForShutdown) this.sender().tell(new NoTreeWorkAndFinishMessage(), this.self());
			else this.sender().tell(new NoTreeWorkMessage(), this.self());
		} else {
//			this.log.info("Redistribute {}/{} tasks to other worker {}", backlogWorkStack.size() == 1 ? 0 : 1, backlogWorkStack.size(), this.sender().path().name());
			ArrayDeque<TreeTask> newStack = new ArrayDeque<>();
			if (backlogWorkStack.size() > 1) {
				newStack.add(backlogWorkStack.removeFirst());
			}
			this.sender().tell(new TreeWorkMessage(newStack), this.self());
		}
	}

	private void handle(TreeWorkMessage message) {
		dirtyAskActorIndex = askActorIndex != 0;

		backlogWorkStack.addAll(message.getTaskQueue());
//		if (!this.sender().equals(this.self())) {
//			this.log.info("Received Work from {}. Queue has size of {}", this.sender().path().name(), backlogWorkStack.size());
//		}

		localTreeDepth = 0;
		handleNextTask();
	}

	private void handleNextTask() {
		if (backlogWorkStack.isEmpty()) {
//			this.log.error("Backlog is empty!");

			askActorIndex = 0;
			if(otherWorker.size() > 0) {
				otherWorker.get(askActorIndex).tell(new NeedTreeWorkMessage(), this.self());
			} else {
				this.self().tell(new ReportAndShutdownMessage(), this.self());
			}

			return;
		}

		localTreeDepth += 1;
		if (localTreeDepth >= maxLocalTreeDepth) {
			this.self().tell(new TreeWorkMessage(new ArrayDeque<>()), this.self());
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
		for (ActorRef worker : otherWorker) {
			worker.tell(new UCCDiscoveredMessage(ucc), this.self());
		}
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

	private void handle(UCCDiscoveredMessage message) {
		discoveredUCCs.add(message.getUcc());

		if (discoveredUCCs.size() == waitForUccCount) {
			handle(new ReportAndShutdownMessage(waitForUccCount));
		}
	}

	private String beautifyJson(String jsonString) {
		return jsonString.replaceAll(",",",\n\t").replaceAll(":",": ").replaceAll("\\{","{\n\t").replaceAll("}","\n}");
	}

	private void handle(ReportAndShutdownMessage message) {
		if (discoveredUCCs.size() < message.getUccCount()) {
			waitForUccCount = message.getUccCount();
			return;
		}
		if (treeSearchStart != 0) {
			this.log.info("Tree Search Cost: {}", System.currentTimeMillis() - treeSearchStart);
		}

		this.log.info("Discovered {} UCCs", discoveredUCCs.size());

		if (shouldOutputFile) {

			JSONObject obj = new JSONObject();
			obj.put("Dataset Name", "?");
			obj.put("Encode Data Runtime", 0);
			obj.put("Build Difference Sets Runtime", 0);
			obj.put("Tree Search Runtime", System.currentTimeMillis() - treeSearchStart);
			obj.put("Algorithm Runtime", 0);
			obj.put("Minimal UCC Count", discoveredUCCs.size());

//			JSONArray results = new JSONArray();
//			for (SerializableBitSet bitSet : discoveredUCCs) {
//				results.add(toUCC(bitSet));
//			}
//			obj.put("results", results);

			try (FileWriter file = new FileWriter("test-results.json")) {
				file.write(beautifyJson(obj.toJSONString()));
				this.log.info("Successfully Copied JSON Object to File (Path: {})", file);
				file.flush();
				file.close();
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
