package com.gpergrossi.util.data.btree;

import com.gpergrossi.util.data.OrderedPair;

/**
 * @param <T> a higher level class that implements this interface
 */
public interface IBinaryNode<T extends IBinaryNode<T>> {
	
	/**
	 * Returns the root of the provided node's tree. May be the node itself.
	 * @param node - the node from which the root is to be found
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the root of the tree
	 */
	public static <K extends IBinaryNode<K>> K getRoot(IBinaryNode<K> node) {
		@SuppressWarnings("unchecked")
		K result = (K) node;
		while (result.getParent() != null) result = result.getParent();
		return result;
	}
	
	/**
	 * Returns true if the provided node has either a left child, right child, or both.
	 * @param node - the node for which children should be checked
	 * @return true if the provided node has one or more children
	 */
	public static boolean hasChildren(IBinaryNode<?> node) {
		return (node.getLeftChild() != null) || (node.getRightChild() != null);
	}
	
	/**
	 * Returns true if the provided node has a parent and is the left child of that parent.
	 * @param node - the node for which the 'left child' status should be checked
	 * @return true if the provided node is a left child
	 */
	public static boolean isLeftChild(IBinaryNode<?> node) {
		return (node.getParent() != null) && (node.getParent().getLeftChild() == node);
	}
	
	/**
	 * Returns true if the provided node has a parent and is the right child of that parent.
	 * @param node - the node for which the 'right child' status should be checked
	 * @return true if the provided node is a right child
	 */
	public static boolean isRightChild(IBinaryNode<?> node) {
		return (node.getParent() != null) && (node.getParent().getRightChild() == node);
	}
	
	/**
	 * Returns the sibling of the provided node or null if there isn't one.
	 * @param node - the node for which the sibling should be found
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the sibling of the provided node or null
	 */
	public static <K extends IBinaryNode<K>> K getSibling(IBinaryNode<K> node) {
		if (isLeftChild(node)) return node.getParent().getRightChild();
		if (isRightChild(node)) return node.getParent().getLeftChild();
		if (node.getParent() != null) throw new IllegalStateException("Tree inconsistency detected! Node's parent does not acknowledge node as child.");
		return null;
	}
	
	/**
	 * Returns true if the provided potentialAncestor is an ancestor of the potentialDescendant.
	 * @param potentialDescendant - the node that may potentially be a descendant of the provided ancestor
	 * @param potentialAncestor - the node that may potentially be an ancestor of the provided node
	 * @return true if the potentialAncestor is an ancestor of the potentialDescendant.
	 */
	public static boolean hasAncestor(IBinaryNode<?> potentialDescendant, IBinaryNode<?> potentialAncestor) {
		IBinaryNode<?> node = potentialDescendant;
		while (node != null) {
			if (node == potentialAncestor) return true;
			node = node.getParent();
		}
		return false;
	}
	
	/**
	 * Returns the node farthest from root that is an ancestor of both the provided node and
	 * the provided potentialRelative. May return null if there is no common ancestor.
	 * @param node - a node that may be a relative of {@code potentialRelative}
	 * @param potentialRelative - a node that may be a relative of {@code node}
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the closest common ancestor of the provided node and the potentialRelative (local root of the smallest subtree containing both)
	 */
	public static <K extends IBinaryNode<K>> K getCommonAncestor(IBinaryNode<K> node, IBinaryNode<K> potentialRelative) {
		@SuppressWarnings("unchecked")
		K knode = (K) node;
		while (knode != null) {
			if (potentialRelative.hasAncestor(knode)) return knode;
			knode = knode.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the in-order previous node or null if none exists within the tree.
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node for which the predecessor should be returned
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the predecessor of the provided node or null
	 */
	public static <K extends IBinaryNode<K>> K getPredecessor(IBinaryNode<K> node) {
		if (node.getLeftChild() != null) {
			return getLastDescendant(node.getLeftChild());
		} else {
			while (node != null && !isRightChild(node)) node = node.getParent();
			if (node != null) return node.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the in-order next node or null if none exists within the tree. 
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node for which the successor should be returned
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the successor of the provided node or null
	 */
	public static <K extends IBinaryNode<K>> K getSuccessor(IBinaryNode<K> node) {
		if (node.getRightChild() != null) {
			return getFirstDescendant(node.getRightChild());
		} else {
			while (node != null && !isLeftChild(node)) node = node.getParent();
			if (node != null) return node.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the first in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node from which the first descendant should be located
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return first in order descendant of the provided node or the node itself
	 */
	public static <K extends IBinaryNode<K>> K getFirstDescendant(IBinaryNode<K> node) {
		while (node.getLeftChild() != null) node = node.getLeftChild();
		@SuppressWarnings("unchecked")
		K output = (K) node;
		return output;
	}
	
	/**
	 * Returns the last in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node from which the last descendant should be located
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return last in order descendant of the provided node or the node itself
	 */
	public static <K extends IBinaryNode<K>> K getLastDescendant(IBinaryNode<K> node) {
		while (node.getRightChild() != null) node = node.getRightChild();
		@SuppressWarnings("unchecked")
		K output = (K) node;
		return output;
	}
	
	/**
	 * Returns the breadth and depth of the tree for which the provided node is the root.
	 * @param node - the node to be treated as root
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return an OrderedPair of Integers describing the breath and depth.
	 */
	public static <K extends IBinaryNode<K>> OrderedPair<Integer> getTreeBreadthAndDepth(IBinaryNode<K> node) {
		OrderedPair<Integer> left;
		if (node.getLeftChild() != null) {
			left = getTreeBreadthAndDepth(node.getLeftChild());
		} else {
			left = new OrderedPair<>(0, 0);
		}
		
		OrderedPair<Integer> right;
		if (node.getRightChild() != null) {
			right = getTreeBreadthAndDepth(node.getRightChild());
		} else {
			right = new OrderedPair<>(0, 0);
		}
		
		int depth = Math.max(left.second, right.second)+1;
		int breadth = left.first + right.first + 1;
		
		return new OrderedPair<>(breadth, depth);
	}
	
	
	
	
	
	/**
	 * Get the parent of this node. May be null.
	 * @return parent node or null
	 */
	public T getParent();
	
	/**
	 * Disconnects this node from its parent. If this node has no parent, no changes are made.
	 */
	public void removeFromParent();
	
	/**
	 * Get the left child of this node. May be null.
	 * @return left child or null
	 */
	public T getLeftChild();
	
	/**
	 * <p>Set the left child of this node, removing the old left child if there was one.
	 * If the child argument is non-null, then the provided child node must be a root node (no parent)
	 * and must not be an ancestor of this node (because that would cause a cycle).</p>
	 * 
	 * <p>The child argument is allowed to have children, in which case an entire subtree will be
	 * assigned as the child of this node.</p>
	 * 
	 * @param child - a parent-less node to become the left child, or null. 
	 * @throws IllegalStateException when the child argument is non-null and one of the following is true: <br/>
	 * A. the child argument is not a root node. <br/>
	 * B. the child argument is an ancestor of this node (causing a cycle). <br/>
	 */
	public void setLeftChild(T child) throws IllegalStateException;
	
	/**
	 * Get the right child of this node. May be null.
	 * @return right child or null
	 */
	public T getRightChild();
	
	/**
	 * <p>Set the right child of this node, removing the old right child if there was one.
	 * If the child argument is non-null, then the provided child node must be a root node (no parent)
	 * and must not be an ancestor of this node (because that would cause a cycle).</p>
	 * 
	 * <p>The child argument is allowed to have children, in which case an entire subtree will be
	 * assigned as the child of this node.</p>
	 * 
	 * @param child - a parent-less node to become the right child, or null. 
	 * @throws IllegalStateException when the child argument is non-null and one of the following is true: <br/>
	 * A. the child argument is not a root node. <br/>
	 * B. the child argument is an ancestor of this node (causing a cycle). <br/>
	 */
	public void setRightChild(T child) throws IllegalStateException;
	
	
	
	
	
	/**
	 * Get the root of the tree to which this node belongs.
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @return root node (may be self)
	 */
	public T getRoot();
	
	/**
	 * Return true if the provided potentialAncestor is an ancestor of this node.
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @param potentialAncestor - the node that may potentially be an ancestor of this node
	 * @return true if the potentialAncestor is an ancestor of this node.
	 */
	public boolean hasAncestor(T potentialAncestor);
	
	/**
	 * Return the node farthest from root that is an ancestor of both this node and
	 * the provided potentialRelative. May return null if there is no common ancestor.
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @param potentialRelative - a node that may be a relative of this node
	 * @return the closest common ancestor of this node and the potentialRelative (smallest subtree containing both)
	 */
	public T getCommonAncestor(T potentialRelative);
	
	/**
	 * Return the in-order previous node or null if none exists within the tree.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.</p>
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @return the predecessor of this node or null
	 */
	public T getPredecessor();
	
	/**
	 * Return the in-order next node or null if none exists within the tree.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.</p>
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @return the successor of this node or null
	 */
	public T getSuccessor();
	
	/**
	 * Return the first in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.</p>
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @return first in order descendant or self
	 */
	public T getFirstDescendant();
	
	/**
	 * Return the last in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.</p>
	 * 
	 * <p><b>Utility Method</b><br/>
	 * This method is considered to be a utility method and is included
	 * in the interface only so that it can be overridden by subclasses whose
	 * implementation details may cause this method's implementation to be trivial.
	 * For convenience, the {@link AbstractBinaryNode} class implements these utility methods.
	 * Additionally, all utility methods in this interface have a static method 
	 * implementation available for when extension of an abstract class is inconvenient.</p>
	 * 
	 * @return last in order descendant or self
	 */
	public T getLastDescendant();
	
}
