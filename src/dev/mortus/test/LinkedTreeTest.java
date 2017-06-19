package dev.mortus.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import dev.mortus.util.data.LinkedBinaryNode;

public class LinkedTreeTest {

	public static class TestNode extends LinkedBinaryNode {
		
		int value;
		
		protected TestNode(int value) {
			super();
			this.value = value;
		}
		
		@Override
		public TestNode getLeftChild() {
			return (TestNode) super.getLeftChild();
		}

		@Override
		public TestNode getRightChild() {
			return (TestNode) super.getRightChild();
		}
		
		protected void setLeftChild(TestNode left) {
			super.setLeftChild(left);
		}

		protected void setRightChild(TestNode right) {
			super.setRightChild(right);
		}
		
		public void insert(int value) {
			if (value < this.value) {
				if (this.hasLeftChild()) {
					this.getLeftChild().insert(value);
				} else {
					this.setLeftChild(new TestNode(value));
				}
			} else {
				if (this.hasRightChild()) {
					this.getRightChild().insert(value);
				} else {
					this.setRightChild(new TestNode(value));
				}
			}
		}
		
		public TestNode find(int value) {
			if (this.value == value) return this;
			if (value < this.value) {
				if (this.hasLeftChild()) return this.getLeftChild().find(value);
			} else {
				if (this.hasRightChild()) return this.getRightChild().find(value);
			}
			return null;
		}
		
		@Override
		public Iterator<TestNode> subtreeIterator() {
			return super.<TestNode>castedSubtreeIterator();
		}
		
		@Override
		public String toString() {
			return "TestNode[ID="+id+" value="+value+"]";
		}
		
	}
	
	public static class TestTree implements LinkedBinaryNode.Tree<TestNode> {

		TestNode root;
		
		public TestTree(int value) {
			this.root = new TestNode(value);
		}
		
		public void insert(int value) {
			this.root.insert(value);
		}
		
		public TestNode find(int value) {
			return this.root.find(value);
		}
		
		@Override
		public String toString() {
			return toString(root, "", "root");
		}

		private String toString(TestNode node, String indent, String name) {
			if (node == null) return "";
			return toString(node.getLeftChild(), indent + "      ", "left") + indent + name + ": " + node.toString() + "\n" + toString(node.getRightChild(), indent + "      ", "right"); 
		}

		@Override
		public LinkedBinaryNode getRoot() {
			return root;
		}

		@Override
		public void setRoot(LinkedBinaryNode node) {
			if (!(node instanceof TestNode)) throw new RuntimeException("TestTree can only reference TestNode roots.");
			this.root = (TestNode) node;			
		}

		@Override
		public Iterator<TestNode> iterator() {
			return this.root.subtreeIterator();
		}
		
	}
		
	public static void main(String[] args) {

		TestTree tree = new TestTree(51);
		
		Random random = new Random(513131698877542L);
		
		for (int i = 0; i < 10; i++) {
			tree.insert(random.nextInt(500));
		}
		
		{
			ArrayList<LinkedBinaryNode> leaves = new ArrayList<LinkedBinaryNode>();
			for (int i = 0; i < 10; i++) {
				LinkedBinaryNode node = tree.root.getFirstDescendant();
				while (node != null) {
					if (!node.hasChildren()) leaves.add(node);
					node = node.getSuccessor();
				}
				int index = random.nextInt(leaves.size());
				node = leaves.get(index);
				leaves.clear();
				if (node == null) break;
				
				int val = ((TestNode)node).value;
				TestNode floating = null;
				
				if (val != -1) {
					floating = new TestNode(val);
					floating.setLeftChild(new TestNode(-1));
					floating.setRightChild(new TestNode(-1));
				}
				
				node.replaceWith(floating);
			}
		}
		
		System.out.print(tree);
		
		int index = 0;
		LinkedBinaryNode node = tree.root.getFirstDescendant();
		while (node != null) {
			System.out.println("index["+(index++)+"]: "+node);

			if (node.getParent() != null) {
				if (node.getParent().getLeftChild() != node && node.getParent().getRightChild() != node) {
					System.out.println("node is neither child of parent");
				}
			}
			
			if (node.getPredecessor() != node.debugFindPredecessor()) {
				System.out.println("bad predecessor: ");
				System.out.println("predecessor: "+node.getPredecessor());
				System.out.println("should be: "+node.debugFindPredecessor());
			}
			if (node.getPredecessor() != null && node != node.getPredecessor().getSuccessor()) {
				System.out.println("bad connection to predecessor");
			}
			
			if (node.getSuccessor() != node.debugFindSuccessor()) {
				System.out.println("bad successor: ");
				System.out.println("successor: "+node.getSuccessor());
				System.out.println("should be: "+node.debugFindSuccessor());
			}	
			if (node.getSuccessor() != null && node != node.getSuccessor().getPredecessor()) {
				System.out.println("bad connection to successor");
			}
			
			if (node.hasLeftChild() && node.getLeftChild().getParent() != node) {
				System.out.println("bad connection to left child");
			}
			if (node.hasRightChild() && node.getRightChild().getParent() != node) {
				System.out.println("bad connection to right child");
			}
			
			node = node.getSuccessor();
		}
		
		
		
	}

}
