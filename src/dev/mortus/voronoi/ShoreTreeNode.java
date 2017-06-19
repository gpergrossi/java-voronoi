package dev.mortus.voronoi;

import dev.mortus.util.data.LinkedBinaryNode;

/**
 * This class mostly recasts the LinkedBinaryNode class. I tried to find a way around this by
 * using LinkedBinaryNode<T extends LinkedBinaryNode<?>>, but this was not possible to do because
 * the methods that need to remain private depend on the type argument.
 * 
 * @author Gregary Pergrossi
 */
public abstract class ShoreTreeNode extends LinkedBinaryNode {
	
	public ShoreTreeNode() {
		super();
	}
	
	public ShoreTreeNode(ShoreTree rootParent) {
		super(rootParent);
	}
	
	public ShoreTreeNode getLeftChild() {
		return (ShoreTreeNode) super.getLeftChild();
	}

	public ShoreTreeNode getRightChild() {
		return (ShoreTreeNode) super.getRightChild();
	}
	
	public ShoreTreeNode getParent() {
		return (ShoreTreeNode) super.getParent();
	}

	public ShoreTreeNode getPredecessor() {
		return (ShoreTreeNode) super.getPredecessor();
	}

	public ShoreTreeNode getSuccessor() {
		return (ShoreTreeNode) super.getSuccessor();
	}
	
	public ShoreTreeNode getRoot() {
		return (ShoreTreeNode) super.getRoot();
	}
	
	public ShoreTreeNode getFirstDescendant() {
		return (ShoreTreeNode) super.getFirstDescendant();
	}
	
	public ShoreTreeNode getLastDescendant() {
		return (ShoreTreeNode) super.getLastDescendant();
	}
	
	public ShoreTreeNode getSibling() {
		return (ShoreTreeNode) super.getSibling();
	}

	/**
	 * Returns an iterator over this node's subtree.
	 */
	@Override
	public Iterator<ShoreTreeNode> subtreeIterator() {
		return super.<ShoreTreeNode>castedSubtreeIterator();
	}
	
	public abstract ShoreArc getArc(final BuildState state, double siteX);
	
}
