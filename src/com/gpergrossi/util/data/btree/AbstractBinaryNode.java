package com.gpergrossi.util.data.btree;

import java.util.Iterator;

public abstract class AbstractBinaryNode<T extends AbstractBinaryNode<T>> implements IBinaryNode<T>, Iterable<T> {
	
	protected final static <T extends AbstractBinaryNode<T>> void swap(T a, T b) {
		if (a == b) return;
		if (a.hasAncestor(b)) swap (b, a);
		if (a.getRoot() != b.getRoot()) throw new IllegalArgumentException("Nodes must be in same tree");

		// Remember all connections
		T aLeft = a.getLeftChild();
		T aRight = a.getRightChild();
		T bLeft = b.getLeftChild();
		T bRight = b.getRightChild();
		T bParent = b.getParent();
		boolean bSideIsRight = b.isRightChild();
		
		// Remove children from both nodes
		if (bLeft != null) bLeft.removeFromParent();
		if (bRight != null) bRight.removeFromParent();		
		if (aLeft != null) aLeft.removeFromParent();
		if (aRight != null) aRight.removeFromParent();
		
		// Remove b from its parent, replace a with b
		b.removeFromParent();
		a.replaceWith(b); // replaceWith is overridden in Node to handle tree root
		
		// Reconnect b's old children to a
		a.setLeftChild(bLeft);
		a.setRightChild(bRight);
		
		// Reconnect a's old children to b
		// Special cases for when b is child of a
		if (aLeft != b) b.setLeftChild(aLeft);
		if (aRight != b) b.setRightChild(aRight);
		
		// Reconnect a to parent
		// Special case for when b is child of a
		if (bParent == a) bParent = b; 
		if (bSideIsRight) {
			bParent.setRightChild(a);
		} else {
			bParent.setLeftChild(a);
		}
	}
	
	
	
	T parent;
	T leftChild, rightChild;
	
	@Override
	public T getParent() {
		return parent;
	}
	
	@Override
	public void removeFromParent() {
		if (!this.hasParent()) return;
		if (this.isLeftChild()) {
			parent.leftChild = null;
			this.parent = null;
		} else if (this.isRightChild()) {
			parent.rightChild = null;
			this.parent = null;
		} else throw new IllegalStateException("Parent node does not reference this node as a child");
	}
	
	@Override
	public T getLeftChild() {
		return leftChild;
	}
	
	@Override
	public void setLeftChild(T child) throws IllegalStateException {
		if (this.hasLeftChild()) this.getLeftChild().removeFromParent();
		if (child == null) return;
		
		checkChildAllowed(child);

		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		this.leftChild = child;
		child.parent = self;
	}
	
	@Override
	public T getRightChild() {
		return rightChild;
	}
	
	@Override
	public void setRightChild(T child) throws IllegalStateException {		
		if (this.hasRightChild()) this.getRightChild().removeFromParent();
		if (child == null) return;
		
		checkChildAllowed(child);

		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		this.rightChild = child;
		child.parent = self;
	}
	
	
	
	
	
	public boolean hasParent() {
		return getParent() != null;
	}

	public boolean hasLeftChild() {
		return (leftChild != null);
	}
	
	public boolean hasRightChild() {
		return (rightChild != null);
	}
	
	public boolean hasChildren() {
		return IBinaryNode.hasChildren(this);
	}
	
	public boolean isLeftChild() {
		return IBinaryNode.isLeftChild(this);
	}
	
	public boolean isRightChild() {
		return IBinaryNode.isRightChild(this);
	}
	
	public T getSibling() {
		return IBinaryNode.getSibling(this);
	}
	
	
	
	
	
	@Override
	public T getRoot() {
		return IBinaryNode.getRoot(this);
	}
	
	@Override
	public boolean hasAncestor(T potentialAncestor) {
		return IBinaryNode.hasAncestor(this, potentialAncestor);
	}
	
	@Override
	public T getCommonAncestor(T potentialRelative) {
		return IBinaryNode.getCommonAncestor(this, potentialRelative);
	}
	
	@Override
	public T getPredecessor() {
		return IBinaryNode.getPredecessor(this);
	}
	
	@Override
	public T getSuccessor() {
		return IBinaryNode.getSuccessor(this);
	}
	
	@Override
	public T getFirstDescendant() {
		return IBinaryNode.getFirstDescendant(this);
	}
	
	@Override
	public T getLastDescendant() {
		return IBinaryNode.getLastDescendant(this);
	}
	
	
	
	
	
	/**
	 * In order to avoid cycles in the tree, the following conditions must be by children to be attached to a node: <br />
	 * 1. The child to be attached must be a root node (have no parent). <br />
	 * 2. The child to be attached must not be an ancestor of the node to which it will be attached (this owuld cause a cycle). <br />
	 * @param child - the child to be attached to this node
	 * @throws IllegalStateException if either of the above conditions is not met.
	 */
	public void checkChildAllowed(T child) {
		if (child.hasParent()) throw new IllegalStateException("Argument node must be a root node! (I.E. No parent)");
		if (this.hasAncestor(child)) throw new IllegalStateException("Argument node must not be an ancestor of this node.");
	}
	
	/**
	 * Replaces this node with the provided node using setLeftChild or setRightChild as appropriate.
	 * If this node has no parent, no action is taken. The provided child node must be a root node (no parent). 
	 * @param child - a parent-less node to become the left child. 
	 * @throws IllegalStateException when: <br/>
	 * A. the child argument is not a root node. <br/>
	 * B. the child argument is an ancestor of this node (causing a cycle). <br/>
	 */
	public void replaceWith(T child) {
		if (!this.hasParent()) return;
		if (child != null) checkChildAllowed(child);
		
		if (this.isLeftChild()) getParent().setLeftChild(child);
		else if (this.isRightChild()) getParent().setRightChild(child);
		else throw new IllegalStateException("Parent node does not reference this node as a child");
	}
	
	/**
	 * Rotates the subtree with the local root "this" to the right and returns the new local root.
	 * <pre>
	 *     4              2     
	 *    / \            / \    
	 *   2   5   --->   1   4   
	 *  / \                / \  
	 * 1   3              3   5 </pre>
	 * rotateRight() on node 4 results in the new tree on the right and returns 2.<br/><br/>
	 * @return the new local root, AKA the node that took this node's place after rotation.
	 */
	public T rotateRight() {
		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		T newTop = self.getLeftChild();
		if (newTop == null) throw new IllegalStateException("Cannot rotate node because there is no appropriate child!");
		
		newTop.removeFromParent();
		self.replaceWith(newTop);
		
		T oldRight = newTop.getRightChild();
		newTop.setRightChild(self);
		self.setLeftChild(oldRight);
		
		return newTop;
	}

	/**
	 * Rotates the subtree with the local root "this" to the left and returns the new local root.
	 * <pre>
	 *     2              4    
	 *    / \            / \   
	 *   1   4   --->   2   5  
	 *      / \        / \     
	 *     3   5      1   3    </pre>
	 * rotateLeft() on node 2 results in the new tree on the right and returns 4.<br/><br/>
	 * @return the new local root, AKA the node that took this node's place after rotation.
	 */
	public T rotateLeft() {
		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		T newTop = self.getRightChild();
		if (newTop == null) throw new IllegalStateException("Cannot rotate node because there is no appropriate child!");
		
		newTop.removeFromParent();
		self.replaceWith(newTop);
		
		T oldLeft = newTop.getLeftChild();
		newTop.setLeftChild(self);
		self.setRightChild(oldLeft);
		
		return newTop;
	}
	
	/**
	 * Returns the tree as a formatted string. Hard to read, but could be useful anyway.
	 * @return
	 */
	public String treeString() {
		StringBuilder sb = new StringBuilder();

		@SuppressWarnings("unchecked")
		T self = (T) this;
		
		treeString(sb, self, "", "root");
		return sb.toString();
	}

	private void treeString(StringBuilder sb, T node, String indent, String name) {
		if (node == null) return;
		treeString(sb, node.getLeftChild(), indent + "      ", "left");
		sb.append(indent).append(name).append(": ").append(node.toString()).append('\n');
		treeString(sb, node.getRightChild(), indent + "      ", "right");
	}
	
	@Override
	public Iterator<T> iterator() {
		@SuppressWarnings("unchecked")
		T self = (T) this;
		return new TreeNodeIterator<T>(self);
	}
	
}
