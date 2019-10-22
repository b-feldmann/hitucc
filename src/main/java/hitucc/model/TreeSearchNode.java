package hitucc.model;

import akka.actor.ActorRef;

public class TreeSearchNode {
	private final static long ROOT_ID = -1L;

	private long parentNodeId;
	private ActorRef parent;
	private long nodeId;
	private int workingChildren;

	public TreeSearchNode(long parentNodeId, ActorRef parent, long nodeId) {
		this.parentNodeId = parentNodeId;
		this.parent = parent;
		this.nodeId = nodeId;
	}

	public TreeSearchNode(ActorRef parent, long nodeId) {
		this.parentNodeId = ROOT_ID;
		this.parent = parent;
		this.nodeId = nodeId;
	}

	public void addChild() {
		workingChildren++;
	}

	public void childFinished(long nodeId) {
		if (this.nodeId == nodeId) workingChildren--;
	}

	public ActorRef getParent() {
		return parent;
	}

	public long getParentNodeId() {
		return parentNodeId;
	}

	public boolean isFulfilled() {
		return workingChildren == 0;
	}

	public boolean isRoot() {
		return parentNodeId == ROOT_ID;
	}
}
