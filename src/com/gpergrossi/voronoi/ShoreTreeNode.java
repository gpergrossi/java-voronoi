package com.gpergrossi.voronoi;

import com.gpergrossi.util.data.btree.AbstractBinaryNode;

/**
 * This class mostly recasts the LinkedBinaryNode class. I tried to find a way around this by
 * using LinkedBinaryNode<T extends LinkedBinaryNode<?>>, but this was not possible to do because
 * the methods that need to remain private depend on the type argument.
 * 
 * @author Gregary Pergrossi
 */
public abstract class ShoreTreeNode extends AbstractBinaryNode<ShoreTreeNode> {
	
	public static int IDCounter = 0;

	public final int ID;
	String debugName;
	
	ShoreTree rootParent;
	
	public ShoreTreeNode() {
		super();
		this.ID = (IDCounter++);
	}
	
	public ShoreTreeNode(ShoreTree rootParent) {
		this();
		this.rootParent = rootParent;
	}
	
	public abstract ShoreArc getArc(final BuildState state, double siteX);
	
	public boolean isRoot() {
		if (this.rootParent == null) return false;
		if (this.rootParent.root != this) throw new IllegalStateException("ShoreTreeNode's rootParent does not acknowledge node as root!");
		return true;
	}
	
	@Override
	public void removeFromParent() {
		super.removeFromParent();
		if (this.isRoot()) rootParent.setRoot(null);
	}
	
	@Override
	public void replaceWith(ShoreTreeNode child) {
		super.replaceWith(child);
		if (this.isRoot()) rootParent.setRoot(child);
	}
	
}
