package de.hpi.hit_ucc.model;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BitSetPrefixTreeNode implements Iterable<BitSet>{

	private int level;
	private int columnNumber;
	private BitSet bitSet;
	private BitSetPrefixTreeNode[] children;
	private BitSetPrefixTreeNode parent;

	public BitSetPrefixTreeNode(int level, int columnNumber) {
		this(null, level, columnNumber);
	}

	public BitSetPrefixTreeNode(BitSetPrefixTreeNode parent, int level, int columnNumber) {
		this.parent = parent;
		this.level = level;
		this.columnNumber = columnNumber;
		this.children = new BitSetPrefixTreeNode[columnNumber];
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	public BitSetPrefixTreeNode[] getChildren() {
		return children;
	}

	public void setChildren(BitSetPrefixTreeNode[] children) {
		this.children = children;
	}

	public BitSet getBitSet() {
		return bitSet;
	}

	public void setBitSet(BitSet bitSet) {
		this.bitSet = bitSet;
	}

	public BitSetPrefixTreeNode getParent() {
		return parent;
	}

	public void setParent(BitSetPrefixTreeNode parent) {
		this.parent = parent;
	}

	public void addBitSet(BitSet bitSet) {
		if (bitSet.length() >= level) {
			setBitSet(bitSet);
		} else {
			if (children[bitSet.get(level) ? 0 : 1] == null) {
				children[bitSet.get(level) ? 0 : 1] = new BitSetPrefixTreeNode(this, level + 1, columnNumber);
			}
			BitSetPrefixTreeNode child = children[bitSet.get(level) ? 0 : 1];
			child.addBitSet(bitSet);
		}
	}

	public void addBitSets(List<BitSet> bitSets) {
		for (BitSet bitset : bitSets) {
			addBitSet(bitset);
		}
	}

	public boolean containsBitSet(BitSet bitset) {
		return getContainingNode(bitset) != null;
	}

	private BitSetPrefixTreeNode getContainingNode(BitSet other) {
		if (other == null) {
			return null;
		} else if (other.size() <= level && other.equals(bitSet)) {
			return this;
		} else {
			BitSetPrefixTreeNode child = children[bitSet.get(level) ? 0 : 1];
			return child != null ? child.getContainingNode(other) : null;
		}
	}

	@Override
	public Iterator<BitSet> iterator() {
		return null;
	}
}
