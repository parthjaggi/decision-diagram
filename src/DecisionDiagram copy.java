// import java.util.HashMap;

// /**
//  * For now this is Binary Decision Diagram.
//  */
// public class DecisionDiagram {
// 	InternalNode root;
// 	HashMap<Integer, InternalNode> intNodeMap = new HashMap<Integer, InternalNode>();
// 	HashMap<Integer, ExternalNode> extNodeMap = new HashMap<Integer, ExternalNode>();

// 	int intNodeCount = 0;
// 	int extNodeCount = 0;

// 	DecisionDiagram(String value) {
// 		root = new InternalNode(value);
// 		intNodeMap.put(intNodeCount, root);
// 		intNodeCount++;
// 	}

// 	private Object getNode(int nodeType, int nodeIdx) {
// 		if (nodeType == NodeType.INTERNAL.type()) {
// 			return intNodeMap.get(nodeIdx);
// 		} else {
// 			return extNodeMap.get(nodeIdx);
// 		}
// 	}

// 	private int _addNode(int nodeType, Object value) {
// 		if (nodeType == NodeType.INTERNAL.type()) {
// 			intNodeMap.put(intNodeCount, new InternalNode((String) value));
// 			return intNodeCount++;
// 		} else {
// 			extNodeMap.put(extNodeCount, new ExternalNode((Integer) value));
// 			return extNodeCount++;
// 		}
// 	}

// 	// private int findNodeIdx(int value) {
// 	// for (int i = 0; i < uniqueNodeCount; i++) {
// 	// Node node = map.get(i);
// 	// if (node.value == value) {
// 	// return i;
// 	// }
// 	// }
// 	// return -1;
// 	// }

// 	private int findNodeIdxByValue(int nodeType, Object value) {
// 		if (nodeType == NodeType.INTERNAL.type()) {
// 			for (int i = 0; i < intNodeCount; i++) {
// 				InternalNode node = intNodeMap.get(i);
// 				if (node.value == value) {
// 					return i;
// 				}
// 			}
// 			return -1;
// 		} else {
// 			for (int i = 0; i < extNodeCount; i++) {
// 				ExternalNode node = extNodeMap.get(i);
// 				if (node.value == (Integer) value) {
// 					return i;
// 				}
// 			}
// 			return -1;
// 		}
// 	}

// 	public void setHigh(Node node, int value) {
// 		int nodeIdx = findNodeIdx(value);
// 		if (nodeIdx == -1) {
// 			node.high = _addNode(value);
// 		} else {
// 			node.high = nodeIdx;
// 		}
// 	}

// 	private Object putNode(int nodeType, Object value, int atNodeIdx, String linkType) {
// 		if (nodeType == NodeType.INTERNAL.type()) {
// 			// check if value exists in nodeMap.
// 			//
// 			int nodeIdx = findNodeIdxByValue(nodeType, value);
// 			if (nodeIdx == -1) {
// 				getNode(nodeType, nodeIdx).set(linkType, _addNode(nodeType, value));
// 			} else {
// 				node.high = nodeIdx;
// 			}

// 			// return intNodeMap.get(nodeIdx);
// 		} else {
// 			// return extNodeMap.get(nodeIdx);
// 		}

// 	}

// 	// swaps 2 nodes
// 	public void nodes() {

// 	}

// 	public static void main(String[] args) {
// 		DecisionDiagram dd = new DecisionDiagram(12);
// 		dd.setHigh(dd.root, 11);
// 		// dd._addNode(11);
// 		// dd._addNode(10);
// 		// dd._addNode(12);

// 		System.out.println(dd.map);
// 		System.out.println(dd.root.high);
// 	}

// }
