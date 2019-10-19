import java.lang.reflect.Field;
import java.rmi.server.Operation;
import java.util.HashMap;

import util.IntTriple;

/**
 * For now this is Binary Decision Diagram.
 */
public class DecisionDiagram {
	ANode root;
	HashMap<Integer, ANode> idNodeMap = new HashMap<Integer, ANode>();
	HashMap<ANode, Integer> nodeIdMap = new HashMap<ANode, Integer>();
	HashMap<String, Integer> valueIdMap = new HashMap<String, Integer>();
	int nodeCounter = 0;
	int valueCounter = 0;

	// RC: reduceCache, AC: applyCache
	HashMap<Integer, Integer> tNodeValueIdC = new HashMap<Integer, Integer>();
	HashMap<IntTriple, Integer> iNodeTripletIdC = new HashMap<IntTriple, Integer>();

	DecisionDiagram() {
	};

	DecisionDiagram(DecisionDiagram another) {
		this.root = another.root;
		this.idNodeMap = another.idNodeMap;
		this.nodeIdMap = another.nodeIdMap;
		this.valueIdMap = another.valueIdMap;
		this.nodeCounter = another.nodeCounter;
		this.valueCounter = another.valueCounter;
	};

	DecisionDiagram(String value) {
		addNode(NodeType.INTERNAL.type(), value);
		root = (INode) idNodeMap.get(0);
	}

	public void addRoot(String value) {
		addNode(NodeType.INTERNAL.type(), value);
		root = (INode) idNodeMap.get(0);
	}

	public void addRoot(Integer nodeId) {
		root = (INode) idNodeMap.get(nodeId);
	}

	public int addNode(int nodeType, Object value) {
		if (nodeType == NodeType.INTERNAL.type()) {
			INode node = new INode((String) value);
			idNodeMap.put(nodeCounter, node);
			nodeIdMap.put(node, nodeCounter);
			addValueToMap(node);
			return nodeCounter++;
		} else {
			TNode node = new TNode((Integer) value);
			idNodeMap.put(nodeCounter, node);
			nodeIdMap.put(node, nodeCounter);
			return nodeCounter++;
		}
	}

	private void addValueToMap(INode node) {
		if (!valueIdMap.containsKey(node.value)) {
			valueIdMap.put(node.value, valueCounter);
			valueCounter++;
		}
	}

	public void addNodeLink(Integer atNodeIdx, Integer newNodeType, String linkType, Object value) {
		idNodeMap.get(atNodeIdx).setField(linkType, addNode(newNodeType, value));
	}

	public void addLink(Integer atNodeIdx, Integer toNodeIdx, String linkType) {
		idNodeMap.get(atNodeIdx).setField(linkType, toNodeIdx);
	}

	// removes duplicate terminal and internal nodes
	public void removeDuplicateNodes(ANode node, INode parent) {
		if (node instanceof TNode) {
			TNode tnode = (TNode) node;
			if (!tNodeValueIdC.containsKey(tnode.value)) {
				tNodeValueIdC.put(tnode.value, nodeIdMap.get(tnode));
			}
			return;
		} else {
			INode inode = (INode) node;
			int nodeId = nodeIdMap.get(node);
			int valueId = valueIdMap.get(inode.value);

			// delink duplicate terminal nodes
			uniquifyTNodes(inode);

			// delink duplicate internal nodes
			IntTriple triplet = new IntTriple(valueId, inode.high, inode.low);
			if (iNodeTripletIdC.containsKey(triplet)) {
				if (parent.high == nodeId) {
					parent.high = iNodeTripletIdC.get(triplet);
				} else {
					parent.low = iNodeTripletIdC.get(triplet);
				}
			} else {
				iNodeTripletIdC.put(triplet, nodeId);
			}

			removeDuplicateNodes(idNodeMap.get(inode.high), inode);
			removeDuplicateNodes(idNodeMap.get(inode.low), inode);
		}
	}

	// removes nodes having the same lowId and highId
	public void removeRedundantNodes(ANode node, INode parent) {
		if (node instanceof TNode) {
			return;
		} else {
			INode inode = (INode) node;
			int nodeId = nodeIdMap.get(node);
			if (inode.high == inode.low) {
				if (parent == null) {
					root = (INode) idNodeMap.get(inode.high);
				} else {
					if (parent.high == nodeId) {
						parent.high = inode.high;
					} else {
						parent.low = inode.high;
					}
				}
			}
			removeRedundantNodes(idNodeMap.get(inode.high), inode);
			removeRedundantNodes(idNodeMap.get(inode.low), inode);
		}
	}

	public void traverse(ANode node) {
		if (node instanceof TNode) {
			System.out.println("TNode: " + ((TNode) node).value);
			return;
		} else {
			INode inode = (INode) node;
			int nodeId = nodeIdMap.get(inode);
			System.out.println("INode: " + nodeId + " " + inode.value + " " + inode.high + " " + inode.low);
			traverse(idNodeMap.get(inode.high));
			traverse(idNodeMap.get(inode.low));
		}
	}

	public void reduce(ANode node, INode parent) {
		removeDuplicateNodes(node, parent);
		removeRedundantNodes(node, parent);
	}

	public void uniquifyTNodes(INode node) {
		ANode hnode = idNodeMap.get(node.high);
		ANode lnode = idNodeMap.get(node.low);

		if (hnode instanceof TNode) {
			TNode htnode = (TNode) hnode;
			if (tNodeValueIdC.containsKey(htnode.value)) {
				node.high = tNodeValueIdC.get(htnode.value);
			}
		}
		if (lnode instanceof TNode) {
			TNode ltnode = (TNode) lnode;
			if (tNodeValueIdC.containsKey(ltnode.value)) {
				node.low = tNodeValueIdC.get(ltnode.value);
			}
		}
	}

	public void toString(ANode node) {
		// try implementing using toString method of nodes.

		ANode hNode = idNodeMap.get(((INode) node).high);
		ANode lNode = idNodeMap.get(((INode) node).low);

		// System.out.print(node.value + ", H: " + hNode.value + ", L: " + lNode.value);
		System.out.print(node.value);

		if (hNode instanceof TNode) {
			System.out.print(", H: " + ((TNode) hNode).value);
		} else {
			System.out.print(", H: " + ((INode) hNode).value);
		}

		if (lNode instanceof TNode) {
			System.out.print(hNode.value);
			return;
		}

		if (hNode.value != null) {
			toString(hNode);
		}
		if (lNode.value != null) {
			toString(lNode);
		}
	}

	public static Integer operate(ANode n1, ANode n2, DecisionDiagram dd1, DecisionDiagram dd2, DecisionDiagram dd,
			Eval eval) {
		System.out.println();
		System.out.println("operate called.");
		if (n1 instanceof TNode && n2 instanceof TNode) {
			TNode tN1 = (TNode) n1;
			TNode tN2 = (TNode) n2;
			System.out.println("tN1 and tN2: " + tN1.value + " " + tN2.value);
			// return dd.addNode(NodeType.EXTERNAL.type(), tN1.value + tN2.value);
			return dd.addNode(NodeType.EXTERNAL.type(), eval.exec(tN1.value, tN2.value));
		} else if (n1 instanceof TNode) {
			INode iN2 = (INode) n2;
			System.out.println("iN2: " + iN2.value);
			Integer id = dd.addNode(NodeType.INTERNAL.type(), iN2.value);
			INode s = (INode) dd.idNodeMap.get(id);
			// s.high = operate(n1, dd2.idNodeMap.get(iN2.high), dd1, dd2, dd);
			// s.low = operate(n1, dd2.idNodeMap.get(iN2.low), dd1, dd2, dd);
			s.high = operate(n1, dd2.idNodeMap.get(iN2.high), dd1, dd2, dd, eval);
			s.low = operate(n1, dd2.idNodeMap.get(iN2.low), dd1, dd2, dd, eval);
			return id;
		} else if (n2 instanceof TNode) {
			INode iN1 = (INode) n1;
			System.out.println("iN1: " + iN1.value);
			Integer id = dd.addNode(NodeType.INTERNAL.type(), iN1.value);
			INode s = (INode) dd.idNodeMap.get(id);
			// s.high = operate(dd1.idNodeMap.get(iN1.high), n2, dd1, dd2, dd);
			// s.low = operate(dd1.idNodeMap.get(iN1.low), n2, dd1, dd2, dd);
			s.high = operate(dd1.idNodeMap.get(iN1.high), n2, dd1, dd2, dd, eval);
			s.low = operate(dd1.idNodeMap.get(iN1.low), n2, dd1, dd2, dd, eval);
			return id;
		}

		INode iN1 = (INode) n1;
		INode iN2 = (INode) n2;

		System.out.println("iN1 and iN2: " + iN1.value + " " + iN2.value);

		// both n1, n2 have same values
		if (iN1.value == iN2.value) {
			Integer id1 = dd.addNode(NodeType.INTERNAL.type(), iN1.value);
			INode s1 = (INode) dd.idNodeMap.get(id1);
			// s1.high = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.high),
			// dd1, dd2, dd);
			// s1.low = operate(dd1.idNodeMap.get(iN1.low), dd2.idNodeMap.get(iN2.low), dd1,
			// dd2, dd);
			s1.high = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.high), dd1, dd2, dd, eval);
			s1.low = operate(dd1.idNodeMap.get(iN1.low), dd2.idNodeMap.get(iN2.low), dd1, dd2, dd, eval);
			return id1;
		}

		// both n1, n2 are internal and with different values
		Integer id1 = dd.addNode(NodeType.INTERNAL.type(), iN1.value);
		Integer id2 = dd.addNode(NodeType.INTERNAL.type(), iN2.value);
		Integer id3 = dd.addNode(NodeType.INTERNAL.type(), iN2.value);
		INode s1 = (INode) dd.idNodeMap.get(id1);
		INode s2 = (INode) dd.idNodeMap.get(id2);
		INode s3 = (INode) dd.idNodeMap.get(id3);

		s1.high = id2;
		// s2.high = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.high),
		// dd1, dd2, dd);
		// s2.low = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.low),
		// dd1, dd2, dd);
		s2.high = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.high), dd1, dd2, dd, eval);
		s2.low = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.low), dd1, dd2, dd, eval);

		s1.low = id3;
		s3.high = operate(dd1.idNodeMap.get(iN1.low), dd2.idNodeMap.get(iN2.high), dd1, dd2, dd, eval);
		s3.low = operate(dd1.idNodeMap.get(iN1.low), dd2.idNodeMap.get(iN2.low), dd1, dd2, dd, eval);
		return id1;
	}

	// public static Object eval(ANode n1, DecisionDiagram dd1, HashMap<String,
	// Boolean> assignMap)
	// throws CloneNotSupportedException {
	// DecisionDiagram dd = (DecisionDiagram) dd1.clone();

	// }

	// evaluates DD to integer. assignMap holds all vars.
	public static Integer eval(Integer nodeId, HashMap<String, Integer> assignMap, DecisionDiagram dd) {
		ANode n = dd.idNodeMap.get(nodeId);
		if (n instanceof TNode) {
			return ((TNode) n).value;
		}
		INode inode = (INode) n;

		String linkType = LinkType.byNum(assignMap.get(inode.value));
		Integer nextNodeId = (Integer) inode.getField(linkType);
		return eval(nextNodeId, assignMap, dd);
	}

	// evaluates DD to another DD. assignMap does not hold all vars.
	public static void partialEval(Integer nodeId, Integer parentId, String linkType,
			HashMap<String, Integer> assignMap, DecisionDiagram dd) {
		ANode n = dd.idNodeMap.get(nodeId);
		if (n instanceof TNode)
			return;

		INode inode = (INode) n;
		if (assignMap.containsKey(inode.value)) {
			String nextLinkType = LinkType.byNum(assignMap.get(inode.value));
			Integer nextNodeId = (Integer) inode.getField(nextLinkType);

			if (parentId == null) {
				dd.root = dd.idNodeMap.get(nextNodeId);
				partialEval(nextNodeId, null, null, assignMap, dd);
			} else {
				ANode p = dd.idNodeMap.get(parentId);
				INode pnode = (INode) p;
				pnode.setField(linkType, nextNodeId);
				partialEval(nextNodeId, parentId, linkType, assignMap, dd);
			}
		} else {
			partialEval(inode.high, parentId, linkType, assignMap, dd);
			partialEval(inode.low, parentId, linkType, assignMap, dd);
		}
	}

	/**
	 * NODES
	 */
	public static abstract class ANode {
		public String value;

		// public abstract void toString();

		// stackoverflow.com/questions/13128194/java-how-can-i-dynamically-reference-an-objects-property
		public void setField(String fieldName, Object value) {
			Field field;
			try {
				field = getClass().getDeclaredField(fieldName);
				field.set(this, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Object getField(String fieldName) {
			Field field;
			try {
				field = getClass().getDeclaredField(fieldName);
				return field.get(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return new Object();
		}
	}

	public static class INode extends ANode {
		int high;
		int low;
		String value;

		INode(String value) {
			this.value = value;
		}
	}

	public static class TNode extends ANode {
		int value;

		TNode(int value) {
			this.value = value;
		}
	}

	public static void main(String[] args) {
		// initialize unreduced but ordered decision diagram
		// DecisionDiagram dd = new DecisionDiagram("x2");
		// dd.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "x1");
		// dd.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.LOW.type(), "x1");
		// dd.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		// dd.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		// dd.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		// dd.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		// dd.reduce(dd.root, null);

		// initialize 2 dds, and perform add operation on them
		// DecisionDiagram dd1 = new DecisionDiagram("a");
		// dd1.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "c");
		// dd1.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.LOW.type(), "b");
		// dd1.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		// dd1.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		// dd1.addLink(2, 1, LinkType.HIGH.type());
		// dd1.addLink(2, 4, LinkType.LOW.type());
		// dd1.traverse(dd1.root);
		// System.out.println();

		// DecisionDiagram dd2 = new DecisionDiagram("a");
		// dd2.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 0);
		// dd2.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.LOW.type(), "c");
		// dd2.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 2);
		// dd2.addLink(2, 1, LinkType.LOW.type());
		// dd2.traverse(dd2.root);
		// System.out.println();

		// DecisionDiagram dd = new DecisionDiagram();
		// dd.addRoot(DecisionDiagram.operate(dd1.root, dd2.root, dd1, dd2, dd, new
		// Add()));
		// dd.traverse(dd.root);

		// System.out.println();
		// dd.reduce(dd.root, null);
		// dd.traverse(dd.root);

		// initialize 2 dds, multiply them, summation over variable
		// probability of being in x4 in future, by taking 'right' action
		DecisionDiagram dd1 = new DecisionDiagram("x4'");
		dd1.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "x2");
		dd1.addNodeLink(1, NodeType.INTERNAL.type(), LinkType.LOW.type(), "x4");
		dd1.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		dd1.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		dd1.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		dd1.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		dd1.traverse(dd1.root);

		// reward DD
		DecisionDiagram dd2 = new DecisionDiagram("x4'");
		dd2.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		dd2.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		dd2.traverse(dd2.root);

		DecisionDiagram dd2w = new DecisionDiagram("x4");
		dd2w.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
		dd2w.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
		dd2w.traverse(dd2w.root);

		DecisionDiagram dd = new DecisionDiagram();
		dd.addRoot(DecisionDiagram.operate(dd1.root, dd2.root, dd1, dd2, dd, new Operate.Multiply()));
		dd.traverse(dd.root);

		DecisionDiagram tdd1 = new DecisionDiagram(dd);
		DecisionDiagram tdd2 = new DecisionDiagram(dd);
		HashMap<String, Integer> am = new HashMap<String, Integer>();
		am.put("x4'", 0);
		HashMap<String, Integer> am2 = new HashMap<String, Integer>();
		am2.put("x4'", 1);
		DecisionDiagram.partialEval(0, null, null, am, tdd1);
		DecisionDiagram.partialEval(0, null, null, am2, tdd2);

		System.out.println();
		tdd1.traverse(tdd1.root);
		System.out.println();
		tdd2.traverse(tdd2.root);
		System.out.println();

		DecisionDiagram tdd3 = new DecisionDiagram();
		tdd3.addRoot(DecisionDiagram.operate(tdd1.root, tdd2.root, tdd1, tdd2, tdd3, new Operate.Add()));
		tdd3.traverse(tdd3.root);
		System.out.println();

		DecisionDiagram tdd4 = new DecisionDiagram();
		tdd4.addRoot(DecisionDiagram.operate(tdd3.root, dd2w.root, tdd3, dd2w, tdd4, new Operate.Add()));
		tdd4.traverse(tdd4.root);

		HashMap<String, Integer> am3 = new HashMap<String, Integer>();
		am3.put("x4", 0);
		am3.put("x2", 1);
		System.out.println(DecisionDiagram.eval(tdd4.nodeIdMap.get(tdd4.root), am3, tdd4));
	}
}
