package de.hpi.hit_ucc.model;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Queue;

// inspired from https://algs4.cs.princeton.edu/code/edu/princeton/cs/algs4/TrieSET.java.html

public class TrieSet implements Iterable<BitSet> {
	private int R;

	private Node root;      // root of trie
	private int n;          // number of keys in trie

	/**
	 * Initializes an empty set of strings.
	 */
	public TrieSet(int firstLayerWidth) {
		this.R = firstLayerWidth;
	}

	/**
	 * Does the set contain the given key?
	 *
	 * @param key the key
	 * @return {@code true} if the set contains {@code key} and
	 * {@code false} otherwise
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public boolean contains(BitSet key) {
		if (key == null) throw new IllegalArgumentException("argument to contains() is null");
		Node x = get(root, key, 0);
		if (x == null) return false;
		return x.isBitSet;
	}

	private Node get(Node x, BitSet key, int d) {
		if (x == null) return null;
		if (d == key.length()) return x;
		int c = key.nextSetBit(d);
		return get(x.next[c - d], key, c + 1);
	}

	/**
	 * Adds the key to the set if it is not already present.
	 *
	 * @param key the key to add
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void add(BitSet key) {
		if (key == null) throw new IllegalArgumentException("argument to add() is null");
		root = add(root, key, 0);
	}

	private Node add(Node x, BitSet key, int d) {
		if (x == null) x = new Node(R - d);
		if (d == key.length()) {
			if (!x.isBitSet) n++;
			x.isBitSet = true;
		} else {
			int c = key.nextSetBit(d);
			x.next[c - d] = add(x.next[c - d], key, c + 1);
		}
		return x;
	}

	/**
	 * Returns the number of strings in the set.
	 *
	 * @return the number of strings in the set
	 */
	public int size() {
		return n;
	}

	/**
	 * Is the set empty?
	 *
	 * @return {@code true} if the set is empty, and {@code false} otherwise
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	public void clear() {
		root = new Node(R);
	}

	/**
	 * Returns all of the keys in the set, as an iterator.
	 * To iterate over all of the keys in a set named {@code set}, use the
	 * foreach notation: {@code for (Key key : set)}.
	 *
	 * @return an iterator to all of the keys in the set
	 */
	public Iterator<BitSet> iterator() {
		return keysWithPrefix(new BitSet()).iterator();
	}

	/**
	 * Returns all of the keys in the set that start with {@code prefix}.
	 *
	 * @param prefix the prefix
	 * @return all of the keys in the set that start with {@code prefix},
	 * as an iterable
	 */
	public Iterable<BitSet> keysWithPrefix(BitSet prefix) {
		Queue<BitSet> results = new ArrayDeque<>();
		Node x = get(root, prefix, 0);
		collect(x, prefix, results, 0);
		return results;
	}

	private void collect(Node x, BitSet prefix, Queue<BitSet> results, int d) {
		if (x == null) return;
		if (x.isBitSet) results.add(BitSet.valueOf(prefix.toByteArray()));
		for (int c = d; c < R; c++) {
			prefix.set(c);
			collect(x.next[c - d], prefix, results, c + 1);
//			prefix.clear(prefix.length() - 1);
			prefix.clear(c);
		}
	}

	/**
	 * Returns all of the keys in the set that match {@code pattern},
	 * where . symbol is treated as a wildcard character.
	 *
	 * @param pattern the pattern
	 * @return all of the keys in the set that match {@code pattern},
	 * as an iterable, where . is treated as a wildcard character.
	 */
	public Iterable<BitSet> keysThatMatch(BitSet pattern) {
		Queue<BitSet> results = new ArrayDeque<BitSet>();
		BitSet prefix = new BitSet();
		collect(root, prefix, pattern, results);
		return results;
	}

	private void collect(Node x, BitSet prefix, BitSet pattern, Queue<BitSet> results) {
		if (x == null) return;
		int d = prefix.length();
		if (d == pattern.length() && x.isBitSet)
			results.add(prefix);
		if (d == pattern.length())
			return;
		int c = pattern.nextSetBit(d);
//		if (c == '.') {
//			for (char ch = 0; ch < R; ch++) {
//				prefix.append(ch);
//				collect(x.next[ch], prefix, pattern, results);
//				prefix.deleteCharAt(prefix.length() - 1);
//			}
//		} else {
		prefix.set(c);
		collect(x.next[c - d], prefix, pattern, results);
		prefix.clear(prefix.length() - 1);
//		}
	}

	/**
	 * Returns the string in the set that is the longest prefix of {@code query},
	 * or {@code null}, if no such string.
	 *
	 * @param query the query string
	 * @return the string in the set that is the longest prefix of {@code query},
	 * or {@code null} if no such string
	 * @throws IllegalArgumentException if {@code query} is {@code null}
	 */
	public BitSet longestPrefixOf(BitSet query) {
		if (query == null) throw new IllegalArgumentException("argument to longestPrefixOf() is null");
		int length = longestPrefixOf(root, query, 0, -1);
		if (length == -1) return null;


//		return query.substring(0, length);
		for (int i = length; i < query.length(); i++) {
			query.clear(i);
		}
		return query;
	}

	// returns the length of the longest string key in the subtrie
	// rooted at x that is a prefix of the query string,
	// assuming the first d character match and we have already
	// found a prefix match of length length
	private int longestPrefixOf(Node x, BitSet query, int d, int length) {
		if (x == null) return length;
		if (x.isBitSet) length = d;
		if (d == query.length()) return length;
		int c = query.nextSetBit(d);
		return longestPrefixOf(x.next[c - d], query, c + 1, length);
	}

	/**
	 * Removes the key from the set if the key is present.
	 *
	 * @param key the key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void delete(BitSet key) {
		if (key == null) throw new IllegalArgumentException("argument to delete() is null");
		root = delete(root, key, 0);
	}

	private Node delete(Node x, BitSet key, int d) {
		if (x == null) return null;
		if (d == key.length()) {
			if (x.isBitSet) n--;
			x.isBitSet = false;
		} else {
			int c = key.nextSetBit(d);
			x.next[c - d] = delete(x.next[c - d], key, c + 1);
		}

		// remove subtrie rooted at x if it is completely empty
		if (x.isBitSet) return x;
		for (int c = d; c < R; c++)
			if (x.next[c - d] != null)
				return x;
		return null;
	}

	// R-way trie node
	private class Node {
		private Node[] next;
		private boolean isBitSet;

		public Node(int width) {
			next = new Node[width];
		}
	}
}
