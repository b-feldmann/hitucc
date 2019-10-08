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
import hit_ucc.behaviour.oracle.HittingSetOracle;
import hit_ucc.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreeSearchPeerWorker extends AbstractActor {
	public static final String DEFAULT_NAME = "tree-search-peer-worker";
	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());
	long treeSearchStart = 0;
	private Random random = new Random();
	private List<ActorRef> colleagues = new ArrayList<>();
	private ActorRef dataBouncer = null;
	private List<WorkerState> colleaguesStates = new ArrayList<>();
	private WorkerState selfState = WorkerState.NOT_STARTED;
	private int columnCount = 0;
	private int maxLocalTreeDepth = 10;
	private int currentLocalTreeDepth = 0;
	private SerializableBitSet[] minimalDifferenceSets = new SerializableBitSet[0];
	private List<SerializableBitSet> discoveredUCCs = new ArrayList<>();
	private List<TreeSearchNode> treeSearchNodes = new ArrayList<>();
	private TreeSearchNode currentTreeNode;
	private long currentTreeNodeId = 0;

	public TreeSearchPeerWorker(SerializableBitSet[] minimalDifferenceSets) {
		this.minimalDifferenceSets = minimalDifferenceSets;
	}

	public static Props props(SerializableBitSet[] minimalDifferenceSets) {
		return Props.create(TreeSearchPeerWorker.class, (new TreeSearchPeerWorker(minimalDifferenceSets)));
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
		if (this.self().toString().equals(other.toString())) {
			this.log.error("Actor String is not unique >.<");
		}

		return this.self().toString().compareTo(other.toString()) < 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CurrentClusterState.class, this::handle)
				.match(RegistrationMessage.class, this::handle)
				.match(WorkerStateChangedMessage.class, this::handle)
				.match(TreeNodeWorkMessage.class, this::handle)
				.match(TreeNodeFulfilledMessage.class, this::handle)
				.match(UCCDiscoveredMessage.class, this::handle)
				.match(ReportAndShutdownMessage.class, this::handle)
				.matchAny(object -> this.log.info("Meh.. Received unknown message: \"{}\"", object.toString()))
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
		if (colleagues.contains(this.sender())) return;

		this.context().watch(this.sender());

		if (this.sender().path().name().equals(PeerDataBouncer.DEFAULT_NAME)) {
			dataBouncer = this.sender();
			this.log.info("Registered {} DataBouncer", this.sender().path().name());
		} else {
			colleagues.add(this.sender());
			colleaguesStates.add(WorkerState.NOT_STARTED);
			this.log.info("Registered {}; {} registered colleagues", this.sender().path().name(), colleagues.size());
		}

		this.sender().tell(new RegistrationMessage(), this.self());
	}

	private void handle(WorkerStateChangedMessage message) {
//		this.log.info("Received New State Message {}", message.state);

		for (int i = 0; i < colleaguesStates.size(); i++) {
			if (colleagues.get(i).equals(this.sender())) {
				colleaguesStates.set(i, message.getState());
			}
		}

		if (this.self().equals(this.sender())) {
			if (selfState != WorkerState.TREE_TRAVERSAL || message.getState() != WorkerState.READY_TO_MERGE) {
				selfState = message.getState();
			}
		}

		if (selfState == WorkerState.DONE && message.getState() == WorkerState.DONE) {
			boolean finished = true;
			for (WorkerState colleaguesState : colleaguesStates) {
				if (colleaguesState != WorkerState.DONE) {
					finished = false;
				}
			}
			if (finished) {
				this.log.info("Finished Collecting {} UCCs from table!", discoveredUCCs.size());
			}
		}
	}

	private void addRootTreeSearchNode() {
		currentTreeNode = new TreeSearchNode(this.sender(), ++currentTreeNodeId);
		treeSearchNodes.add(currentTreeNode);
	}

	private void addNewTreeSearchNode(long nodeId) {
		currentTreeNode = new TreeSearchNode(nodeId, this.sender(), ++currentTreeNodeId);
		treeSearchNodes.add(currentTreeNode);
	}

	private void addChildToTreeSearchNode() {
		currentTreeNode.addChild();
	}

	private TreeSearchNode childFulfilledTreeSearchNode(long nodeId) {
		for (TreeSearchNode node : treeSearchNodes) {
			node.childFinished(nodeId);
			if (node.isFulfilled()) return node;
		}

		return null;
	}

	private void fulfillCurrentTreeNode() {
		if (this.currentTreeNode.isFulfilled()) {
			treeSearchNodes.remove(currentTreeNode);

			currentTreeNode.getParent().tell(new TreeNodeFulfilledMessage(currentTreeNode.getParentNodeId()), this.self());
		}
	}

	private void handle(TreeNodeFulfilledMessage message) {
		currentTreeNode = childFulfilledTreeSearchNode(message.nodeId);

		if (currentTreeNode == null) return;
		if (currentTreeNode.isRoot()) {
			this.log.info("Finished UCC discovery!");

			dataBouncer.tell(new ReportAndShutdownMessage(), this.self());
//			for (ActorRef actorRef : colleagues) {
//				actorRef.tell(new ReportAndShutdownMessage(), this.self());
//			}
//			this.self().tell(new ReportAndShutdownMessage(), this.self());
//			getContext().getSystem().terminate();
		} else {
			fulfillCurrentTreeNode();
		}

	}

	private void handle(TreeNodeWorkMessage message) {
		if (selfState != WorkerState.TREE_TRAVERSAL) broadcastAndSetState(WorkerState.TREE_TRAVERSAL);

		currentLocalTreeDepth = 1;
		addNewTreeSearchNode(message.nodeId);

		SerializableBitSet x = message.getX();
		SerializableBitSet y = message.getY();
		int length = message.getLength();
		SerializableBitSet[] differenceSets = message.getDifferenceSets();

		handleLocal(x, y, length, differenceSets);
	}

	private void handleLocal(SerializableBitSet x, SerializableBitSet y, int length, SerializableBitSet[] differenceSets) {
		currentLocalTreeDepth += 1;

		HittingSetOracle.Status result = HittingSetOracle.extendable(x, y, length, differenceSets, columnCount);
		switch (result) {
			case MINIMAL:
				this.report(x);
				fulfillCurrentTreeNode();
				break;
			case EXTENDABLE:
				this.split(x, y, length, differenceSets);
				break;
			case NOT_EXTENDABLE:
				fulfillCurrentTreeNode();
				// Ignore
				break;
			case FAILED:
				fulfillCurrentTreeNode();
				this.log.error("Oracle failed :(");
				break;
		}
	}

	private void report(SerializableBitSet ucc) {
//		this.log.info("SET {}", DifferenceSetDetector.SerializableBitSetToString(ucc, columnCount));
//		this.log.info("UCC: {}", toUCC(ucc));

		discoveredUCCs.add(ucc);
		for (ActorRef worker : colleagues) {
			worker.tell(new UCCDiscoveredMessage(ucc), this.self());
		}
	}

	private void split(SerializableBitSet x, SerializableBitSet y, int next, SerializableBitSet[] differenceSets) {
		if (next < columnCount) {
			SerializableBitSet xNew = copySerializableBitSet(x, next);
			xNew.set(next);
			ActorRef randomRef = getRandomColleague();
			addChildToTreeSearchNode();
			randomRef.tell(new TreeNodeWorkMessage(xNew, y, next + 1, minimalDifferenceSets, columnCount, currentTreeNodeId), this.self());

			SerializableBitSet yNew = copySerializableBitSet(y, next);
			yNew.set(next);
			if (currentLocalTreeDepth >= maxLocalTreeDepth) {
				randomRef = getRandomColleague();
				addChildToTreeSearchNode();
				randomRef.tell(new TreeNodeWorkMessage(x, yNew, next + 1, minimalDifferenceSets, columnCount, currentTreeNodeId), this.self());
			} else {
				handleLocal(x, yNew, next + 1, minimalDifferenceSets);
			}
		} else {
			this.log.info("WHY IS THIS? ################################### This is not an error - just wanted to check if this branch can actually be reached ;)");
			fulfillCurrentTreeNode();
		}
	}

	private ActorRef getRandomColleague() {
		if (colleagues.size() > 0) return colleagues.get(random.nextInt(colleagues.size()));

		return this.self();
	}

	private void handle(UCCDiscoveredMessage message) {
		discoveredUCCs.add(message.ucc);
	}

	private void handle(ReportAndShutdownMessage message) {
		if (treeSearchStart != 0) {
			this.log.info("Tree Search Cost: {}", System.currentTimeMillis() - treeSearchStart);
		}

		for (SerializableBitSet ucc : discoveredUCCs) {
//			this.log.info("UCC: {}", toUCC(ucc));
		}
		this.log.info("Discovered {} UCCs", discoveredUCCs.size());

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

		String output = "";
		for (int i = 0; i < SerializableBitSet.logicalLength() - 1; i++) {
			if (SerializableBitSet.get(i)) {
				output += i + ", ";
			}
		}
		if (SerializableBitSet.get(SerializableBitSet.logicalLength() - 1)) {
			output += (SerializableBitSet.logicalLength() - 1) + ", ";
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

	@Data
	@AllArgsConstructor
	private static class UCCDiscoveredMessage implements Serializable {
		private static final long serialVersionUID = 997981649989901337L;
		private SerializableBitSet ucc;

		private UCCDiscoveredMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private class TreeNodeWorkMessage implements Serializable {
		private static final long serialVersionUID = 2360129506196901337L;
		private SerializableBitSet x;
		private SerializableBitSet y;
		private int length;
		private SerializableBitSet[] differenceSets;
		private int numAttributes;

		private long nodeId;

		private TreeNodeWorkMessage() {
		}
	}

	@Data
	@AllArgsConstructor
	private class TreeNodeFulfilledMessage implements Serializable {
		private static final long serialVersionUID = 1085642539643901337L;

		private long nodeId;

		private TreeNodeFulfilledMessage() {
		}
	}
}
