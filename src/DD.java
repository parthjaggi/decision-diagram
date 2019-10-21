import java.lang.reflect.Field;
import java.util.HashMap;

import graph.Graph;
import util.IntPair;
import util.IntTriple;

/**
 * For now this is Binary Decision Diagram.
 */
public class DD {
    public final static boolean GRAPH_USE_COLOR = true;

    HashMap<Integer, ANode> idNodeMap = new HashMap<Integer, ANode>();
    HashMap<ANode, Integer> nodeIdMap = new HashMap<ANode, Integer>();
    HashMap<String, Integer> valueIdMap = new HashMap<String, Integer>();
    int nodeCounter = 0;
    int valueCounter = 0;

    // RC: reduceCache, AC: applyCache
    HashMap<Integer, Integer> tNodeValueIdC = new HashMap<Integer, Integer>();
    HashMap<IntTriple, Integer> iNodeTripletIdC = new HashMap<IntTriple, Integer>();
    HashMap<Integer, Integer> reduceC = new HashMap<Integer, Integer>();
    HashMap<IntPair, Integer> applyC = new HashMap<IntPair, Integer>();

    DD() {
    };

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

    /**
     * prints through the DD and prints the structure.
     */
    public void print(Integer nId) {
        ANode node = idNodeMap.get(nId);
        if (node instanceof TNode) {
            System.out.println("TNode: " + ((TNode) node).value);
            return;
        } else {
            INode inode = (INode) node;
            int nodeId = nodeIdMap.get(inode);
            System.out.println("INode: " + nodeId + " " + inode.value + " " + inode.high + " " + inode.low);
            print(inode.high);
            print(inode.low);
        }
    }

    public Integer reduce(Integer nId) {
        ANode n = idNodeMap.get(nId);
        if (n instanceof TNode) {
            Integer vId = tNodeValueIdC.get(((TNode) n).value);
            if (vId != null) {
                return vId;
            } else {
                tNodeValueIdC.put(((TNode) n).value, nId);
                return nId;
            }
        }
        if (reduceC.containsKey(nId)) {
            return reduceC.get(nId);
        } else {
            INode inode = (INode) n;
            inode.high = reduce(inode.high);
            inode.low = reduce(inode.low);

            Integer rId = getNode(inode.value, inode.high, inode.low);

            reduceC.put(nId, rId);
            return rId;
        }
    }

    /**
     * Helper for reduce and apply.
     */
    public Integer getNode(String value, Integer highId, Integer lowId) {
        if (highId == lowId) {
            return highId;
        }
        Integer vId = valueIdMap.get(value);
        // System.out.println(vId + " " + value);
        // System.out.println(valueIdMap);
        IntTriple triple = new IntTriple(vId, highId, lowId);

        Integer rId = iNodeTripletIdC.get(triple);
        if (rId != null) {
            return rId;
        } else {
            Integer id = addNode(NodeType.INTERNAL.type(), value);
            INode inode = (INode) idNodeMap.get(id);
            inode.high = highId;
            inode.low = lowId;
            iNodeTripletIdC.put(triple, id);
            return id;
        }
    }

    public static Integer operate(ANode n1, ANode n2, DD dd1, DD dd2, DD dd, Eval eval) {
        if (n1 instanceof TNode && n2 instanceof TNode) {
            TNode tN1 = (TNode) n1;
            TNode tN2 = (TNode) n2;
            return dd.addNode(NodeType.EXTERNAL.type(), eval.exec(tN1.value, tN2.value));
        } else if (n1 instanceof TNode) {
            INode iN2 = (INode) n2;
            Integer id = dd.addNode(NodeType.INTERNAL.type(), iN2.value);
            INode s = (INode) dd.idNodeMap.get(id);
            s.high = operate(n1, dd2.idNodeMap.get(iN2.high), dd1, dd2, dd, eval);
            s.low = operate(n1, dd2.idNodeMap.get(iN2.low), dd1, dd2, dd, eval);
            return id;
        } else if (n2 instanceof TNode) {
            INode iN1 = (INode) n1;
            Integer id = dd.addNode(NodeType.INTERNAL.type(), iN1.value);
            INode s = (INode) dd.idNodeMap.get(id);
            s.high = operate(dd1.idNodeMap.get(iN1.high), n2, dd1, dd2, dd, eval);
            s.low = operate(dd1.idNodeMap.get(iN1.low), n2, dd1, dd2, dd, eval);
            return id;
        }

        INode iN1 = (INode) n1;
        INode iN2 = (INode) n2;

        // both n1, n2 have same values
        if (iN1.value.equals(iN2.value)) {
            Integer id1 = dd.addNode(NodeType.INTERNAL.type(), iN1.value);
            INode s1 = (INode) dd.idNodeMap.get(id1);
            s1.high = operate(dd1.idNodeMap.get(iN1.high), dd2.idNodeMap.get(iN2.high), dd1, dd2, dd, eval);
            s1.low = operate(dd1.idNodeMap.get(iN1.low), dd2.idNodeMap.get(iN2.low), dd1, dd2, dd, eval);
            return id1;
        }

        // both n1, n2 are internal and with different values
        Integer id1 = dd.addNode(NodeType.INTERNAL.type(), iN1.value);
        INode s1 = (INode) dd.idNodeMap.get(id1);

        s1.high = operate(dd1.idNodeMap.get(iN1.high), iN2, dd1, dd2, dd, eval);
        s1.low = operate(dd1.idNodeMap.get(iN1.low), iN2, dd1, dd2, dd, eval);
        return id1;
    }

    public Integer apply(Integer n1, Integer n2, Eval eval) {
        Integer result = computeResult(n1, n2, eval);
        if (result != null)
            return result;

        IntPair pair = new IntPair(n1, n2);
        if (applyC.containsKey(pair))
            return applyC.get(pair);

        ANode node1 = idNodeMap.get(n1);
        ANode node2 = idNodeMap.get(n2);
        Integer expandingNode;
        String expandingValue;

        // non terminal nodes. atleast one of them.
        if (!(node1 instanceof TNode)) {
            expandingNode = n1;
            expandingValue = ((INode) node1).value;
        } else {
            expandingNode = n2;
            expandingValue = ((INode) node2).value;
        }

        Integer Fl1;
        Integer Fh1;
        Integer Fl2;
        Integer Fh2;

        if (!(node1 instanceof TNode) && (expandingNode == n1)) {
            Fl1 = ((INode) node1).low;
            Fh1 = ((INode) node1).high;
            Fl2 = n2;
            Fh2 = n2;
        } else {
            Fl1 = n1;
            Fh1 = n1;
            Fl2 = ((INode) node2).low;
            Fh2 = ((INode) node2).high;
        }

        Integer Fl = apply(Fl1, Fl2, eval);
        Integer Fh = apply(Fh1, Fh2, eval);
        Integer Fr = getNode(expandingValue, Fh, Fl);

        applyC.put(pair, Fr);
        return Fr;
    }

    public Integer computeResult(Integer n1, Integer n2, Eval eval) {
        ANode node1 = idNodeMap.get(n1);
        ANode node2 = idNodeMap.get(n2);

        if (node1 instanceof TNode && node2 instanceof TNode) {
            return addNode(NodeType.EXTERNAL.type(), eval.exec(((TNode) node1).value, ((TNode) node2).value));
        }
        if (eval instanceof Operate.Add) {
            if (node1 instanceof TNode && ((TNode) node1).value == 0) {
                return n2;
            }
            if (node2 instanceof TNode && ((TNode) node2).value == 0) {
                return n1;
            }
        } else if (eval instanceof Operate.Multiply) {
            if (node1 instanceof TNode && ((TNode) node1).value == 0
                    || node2 instanceof TNode && ((TNode) node2).value == 0) {
                return addNode(NodeType.EXTERNAL.type(), 0);
            }
            if (node1 instanceof TNode && ((TNode) node1).value == 1) {
                return n2;
            }
            if (node2 instanceof TNode && ((TNode) node2).value == 1) {
                return n1;
            }
        }
        return null;
    }

    // evaluates DD to integer. assignMap holds all vars.
    public static Integer eval(Integer nodeId, HashMap<String, Integer> assignMap, DD dd) {
        ANode n = dd.idNodeMap.get(nodeId);
        if (n instanceof TNode) {
            return ((TNode) n).value;
        }
        INode inode = (INode) n;

        String linkType = LinkType.byNum(assignMap.get(inode.value));
        Integer nextNodeId = (Integer) inode.getField(linkType);
        return eval(nextNodeId, assignMap, dd);
    }

    // evaluates some terms in DD. assignMap does not hold all vars.
    // public static void partialEval(Integer nodeId, Integer parentId, String
    // linkType,
    // HashMap<String, Integer> assignMap, DD dd) {
    // ANode n = dd.idNodeMap.get(nodeId);
    // if (n instanceof TNode)
    // return;

    // INode inode = (INode) n;
    // if (assignMap.containsKey(inode.value)) {
    // String nextLinkType = LinkType.byNum(assignMap.get(inode.value));
    // Integer nextNodeId = (Integer) inode.getField(nextLinkType);

    // if (parentId == null) {
    // dd.root = dd.idNodeMap.get(nextNodeId);
    // partialEval(nextNodeId, null, null, assignMap, dd);
    // } else {
    // ANode p = dd.idNodeMap.get(parentId);
    // INode pnode = (INode) p;
    // pnode.setField(linkType, nextNodeId);
    // partialEval(nextNodeId, parentId, linkType, assignMap, dd);
    // }
    // } else {
    // partialEval(inode.high, parentId, linkType, assignMap, dd);
    // partialEval(inode.low, parentId, linkType, assignMap, dd);
    // }
    // }

    public Graph getGraph(int id) {
        Graph g = new Graph(true /* directed */, false /* bottom-to-top */, false /* left-to-right */,
                true /* multi-links */);
        // XADDNode root = getExistNode(id);
        ANode root = idNodeMap.get(id);
        root.toGraph(g, id);
        return g;
    }

    public void showGraph(int node, String label) {
        Graph g2 = getGraph(node);
        g2.addNode("_temp_");
        g2.addNodeLabel("_temp_", label);
        g2.addNodeShape("_temp_", "box");
        g2.addNodeStyle("_temp_", "filled");
        g2.addNodeColor("_temp_", "gold1");
        g2.launchViewer(1300, 770);
    }

    /**
     * NODES
     */
    public static abstract class ANode {
        public String value;

        public abstract void toGraph(Graph g, int id);

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

        public void toGraph(Graph g, int id) {
            // todo
        }
    }

    public static class TNode extends ANode {
        int value;
        public Object _annotate;

        TNode(int value) {
            this.value = value;
        }

        public void toGraph(Graph g, int id) {
            String this_node = Integer.toString(id);
            g.addNode(this_node);
            if (_annotate == null)
                g.addNodeLabel(this_node, Integer.toString(value));
            else
                g.addNodeLabel(this_node, Integer.toString(value) + "\n[ " + _annotate + " ]");

            if (GRAPH_USE_COLOR)
                g.addNodeColor(this_node, "lightsalmon"); // red, darkred, lightsalmon
            g.addNodeShape(this_node, "box");
            g.addNodeStyle(this_node, "filled");
        }
    }

    public static void main(String[] args) {
        /* TESTING REDUCE OPERATION */
        // initialize unreduced but ordered decision diagram
        // DD dd = new DD();
        // dd.addNode(NodeType.INTERNAL.type(), "x2");
        // dd.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "x1");
        // dd.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.LOW.type(), "x1");
        // dd.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // dd.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // dd.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // dd.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // Integer id = dd.reduce(0);
        // dd.print(id);

        /* TESTING ADD OPERATION */
        // initialize 2 dds, and perform add operation on them
        DD dd = new DD();
        Integer r1 = dd.addNode(NodeType.INTERNAL.type(), "a");
        dd.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "c");
        dd.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.LOW.type(), "b");
        dd.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        dd.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        dd.addLink(2, 1, LinkType.HIGH.type());
        dd.addLink(2, 4, LinkType.LOW.type());
        dd.print(r1);
        System.out.println();

        Integer r2 = dd.addNode(NodeType.INTERNAL.type(), "a");
        dd.addNodeLink(r2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 0);
        dd.addNodeLink(r2, NodeType.INTERNAL.type(), LinkType.LOW.type(), "c");
        dd.addNodeLink(r2 + 2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 2);
        dd.addLink(r2 + 2, r2 + 1, LinkType.LOW.type());
        dd.print(r2);
        System.out.println();

        Integer result = dd.apply(r1, r2, new Operate.Add());
        dd.print(result);
        System.out.println();

        // System.out.println();
        // dd.reduce(dd.root, null);
        // dd.print(dd.root);

        /* TESTING SPUDD */
        // | x2 | x4 |
        // | x1 | x3 |
        // actions: 0, 1, 2, 3 (top, right, bottom, left). reward at x4 is 1
        // get utility at x2
        // HashMap<Integer, Integer> actionValueMap = new HashMap<Integer, Integer>();
        // HashMap<Integer, String> actionFStateMap = new HashMap<Integer, String>(); //
        // future state
        // actionFStateMap.put(1, "x4");
        // actionFStateMap.put(2, "x1");

        // // utility at t=0, V0prime (primed)
        // DecisionDiagram V0prime = new DecisionDiagram("x4'");
        // V0prime.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // V0prime.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // // V0prime.print(V0prime.root);

        // Integer bestAction = null;
        // Integer bestActionEval = 0;

        // for (Map.Entry<Integer, String> actionFStateEntry :
        // actionFStateMap.entrySet()) {
        // // create probability DD based on future state.
        // DecisionDiagram Q = new DecisionDiagram(actionFStateEntry.getValue() + "'");
        // Q.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "x2");
        // Q.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.LOW.type(), "x2");
        // Q.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // Q.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // Q.addLink(2, 3, LinkType.LOW.type()); // to value 1
        // Q.addLink(2, 4, LinkType.HIGH.type()); // to value 0
        // // Q.print(Q.root);

        // // multiply with V0prime
        // DecisionDiagram F = new DecisionDiagram();
        // DecisionDiagram F2 = new DecisionDiagram();
        // F.addRoot(DecisionDiagram.operate(Q.root, V0prime.root, Q, V0prime, F, new
        // Operate.Multiply()));
        // F2.addRoot(DecisionDiagram.operate(Q.root, V0prime.root, Q, V0prime, F2, new
        // Operate.Multiply()));
        // F.reduce(F.root, null);
        // F2.reduce(F2.root, null);
        // // F.print(F.root);

        // // summation over primed variables in Q.
        // HashMap<String, Integer> assignMap = new HashMap<String, Integer>();
        // assignMap.put(actionFStateEntry.getValue() + "'", 0);
        // DecisionDiagram.partialEval(0, null, null, assignMap, F);
        // assignMap.put(actionFStateEntry.getValue() + "'", 1);
        // DecisionDiagram.partialEval(0, null, null, assignMap, F2);
        // DecisionDiagram G = new DecisionDiagram();
        // G.addRoot(DecisionDiagram.operate(F.root, F2.root, F, F2, G, new
        // Operate.Add()));
        // // G.print(G.root);
        // // System.out.println();

        // // evaluate
        // HashMap<String, Integer> evalAssignMap = new HashMap<String, Integer>();
        // evalAssignMap.put("x4'", 0); // todo: unprime all variables in G. eval
        // non-present vars to 0.
        // evalAssignMap.put("x2", 1);
        // Integer eval = DecisionDiagram.eval(G.nodeIdMap.get(G.root), evalAssignMap,
        // G);
        // actionValueMap.put(actionFStateEntry.getKey(), eval);

        // if (eval > bestActionEval) {
        // bestActionEval = eval;
        // bestAction = actionFStateEntry.getKey();
        // }
        // }

        // // future reward
        // DecisionDiagram FR = new DecisionDiagram(bestActionEval);

        // DecisionDiagram V0 = new DecisionDiagram("x4");
        // V0.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // V0.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);

        // DecisionDiagram V1 = new DecisionDiagram();
        // V1.addRoot(DecisionDiagram.operate(V0.root, FR.root, V0, FR, V1, new
        // Operate.Add()));
        // V1.print(V1.root);

        /* TESTING SPUDD - HARDCODED */
        // initialize 2 dds, multiply them, summation over variable
        // probability of being in x4 in future, by taking 'right' action
        // DecisionDiagram dd1 = new DecisionDiagram("x4'");
        // dd1.addNodeLink(0, NodeType.INTERNAL.type(), LinkType.HIGH.type(), "x2");
        // dd1.addNodeLink(1, NodeType.INTERNAL.type(), LinkType.LOW.type(), "x4");
        // dd1.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // dd1.addNodeLink(1, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // dd1.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // dd1.addNodeLink(2, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // dd1.print(dd1.root);

        // // reward DD
        // DecisionDiagram dd2 = new DecisionDiagram("x4'");
        // dd2.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // dd2.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // dd2.print(dd2.root);

        // DecisionDiagram dd2w = new DecisionDiagram("x4");
        // dd2w.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.HIGH.type(), 1);
        // dd2w.addNodeLink(0, NodeType.EXTERNAL.type(), LinkType.LOW.type(), 0);
        // dd2w.print(dd2w.root);

        // DecisionDiagram dd = new DecisionDiagram();
        // dd.addRoot(DecisionDiagram.operate(dd1.root, dd2.root, dd1, dd2, dd, new
        // Operate.Multiply()));
        // dd.print(dd.root);

        // DecisionDiagram tdd1 = new DecisionDiagram(dd);
        // DecisionDiagram tdd2 = new DecisionDiagram(dd);
        // HashMap<String, Integer> am = new HashMap<String, Integer>();
        // am.put("x4'", 0);
        // HashMap<String, Integer> am2 = new HashMap<String, Integer>();
        // am2.put("x4'", 1);
        // DecisionDiagram.partialEval(0, null, null, am, tdd1);
        // DecisionDiagram.partialEval(0, null, null, am2, tdd2);

        // System.out.println();
        // tdd1.print(tdd1.root);
        // System.out.println();
        // tdd2.print(tdd2.root);
        // System.out.println();

        // DecisionDiagram tdd3 = new DecisionDiagram();
        // tdd3.addRoot(DecisionDiagram.operate(tdd1.root, tdd2.root, tdd1, tdd2, tdd3,
        // new Operate.Add()));
        // tdd3.print(tdd3.root);
        // System.out.println();

        // DecisionDiagram tdd4 = new DecisionDiagram();
        // tdd4.addRoot(DecisionDiagram.operate(tdd3.root, dd2w.root, tdd3, dd2w, tdd4,
        // new Operate.Add()));
        // tdd4.print(tdd4.root);

        // HashMap<String, Integer> am3 = new HashMap<String, Integer>();
        // am3.put("x4", 0);
        // am3.put("x2", 1);
        // System.out.println(DecisionDiagram.eval(tdd4.nodeIdMap.get(tdd4.root), am3,
        // tdd4));
    }
}
