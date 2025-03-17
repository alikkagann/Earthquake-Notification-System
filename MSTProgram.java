import java.util.*;
import java.io.*;

/*
  MSTProgram
  -----------
  - Girdi olarak bir test dosyası (ör: test1.txt) alır.
  - İlk kısımda vertex ve edge bilgilerini okuyarak bir Graph oluşturur.
  - Ardından Prim algoritması ile MST (Multiway Tree) kurar.
  - Daha sonra test dosyasındaki komutları ("print-mst", "path", "insert-edge", "decrease-weight", "quit") uygular.
  - MST, multiway tree olarak saklanır. Her vertex'i temsil eden Node'da parent, firstChild, nextSibling, prevSibling
    gibi pointer'lar bulunur.
  - "evert(u)" işlemiyle istenen düğüm kök yapılarak "print-mst" çıktısı alınır.
  - "path u v" komutunda MST üzerinde u->...->v yolunu bulup ekrana basar.
  - insert-edge / decrease-weight komutlarında MST, "minimum spanning tree" özelliğini koruyacak şekilde
    kısmen güncellenir (en kısa yol ekleniyorsa, path üzerindeki en büyük kenar çıkar vb.)
  - "Invalid Operation" kuralları:
    * insert-edge varsa, aynı (u,v) tekrar eklenemez.
    * decrease-weight varsa, graf içinde (u,v) kenarı yoksa geçersizdir.
  - Çıktı formatı ödevde anlatıldığı gibi yapılır.
*/

public class MSTProgram {

    // ---------------------
    // Internal Data Classes
    // ---------------------

    // Vertex in the Graph (adjacency list representation)
    static class Vertex {
        String id;                  // String identifier
        ArrayList<Edge> adj;       // adjacency list (edges)

        // For Prim
        boolean inMST;             // mark if it's included in MST (Prim usage)
        float key;                 // for Prim: best weight to connect
        Vertex pred;               // predecessor in MST (used initially)
        int heapIndex;             // index in the binary heap

        // For referencing the MST multiway tree node
        Node mstNode;             // link to the corresponding node in the multiway tree

        public Vertex(String id) {
            this.id = id;
            this.adj = new ArrayList<>();
            this.inMST = false;
            this.key = Float.MAX_VALUE;
            this.pred = null;
            this.heapIndex = -1;
            this.mstNode = null;
        }
    }

    // Edge in the Graph
    static class Edge {
        Vertex u, v;     // endpoints
        float weight;    // weight

        public Edge(Vertex u, Vertex v, float weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
    }

    // Node in the Multiway Tree
    // Each Node corresponds to exactly one Vertex in the Graph
    static class Node {
        Vertex vertex;
        Node parent;
        Node firstChild;
        Node nextSibling;
        Node prevSibling;

        public Node(Vertex v) {
            this.vertex = v;
            this.parent = null;
            this.firstChild = null;
            this.nextSibling = null;
            this.prevSibling = null;
        }
    }

    // A simple Binary Min-Heap for Prim's algorithm
    // Stores Vertex objects based on vertex.key
    static class BinaryHeap {
        ArrayList<Vertex> heap; // using 1-based indexing is easier, but we can do 0-based

        public BinaryHeap() {
            heap = new ArrayList<>();
        }

        public int size() {
            return heap.size();
        }

        public boolean isEmpty() {
            return heap.isEmpty();
        }

        public void insert(Vertex v) {
            heap.add(v);
            v.heapIndex = heap.size() - 1;
            bubbleUp(heap.size() - 1);
        }

        public Vertex extractMin() {
            if (heap.isEmpty()) return null;
            Vertex min = heap.get(0);
            swap(0, heap.size() - 1);
            heap.remove(heap.size() - 1);
            if (!heap.isEmpty()) {
                bubbleDown(0);
            }
            min.heapIndex = -1;
            return min;
        }

        // decreaseKey means: vertex's key has decreased, so bubble it up
        public void decreaseKey(Vertex v, float newKey) {
            // we assume newKey < v.key
            v.key = newKey;
            bubbleUp(v.heapIndex);
        }

        private void bubbleUp(int idx) {
            while (idx > 0) {
                int parentIdx = (idx - 1) / 2;
                if (heap.get(idx).key < heap.get(parentIdx).key) {
                    swap(idx, parentIdx);
                    idx = parentIdx;
                } else {
                    break;
                }
            }
        }

        private void bubbleDown(int idx) {
            int left, right, smallest;
            while (true) {
                left = 2 * idx + 1;
                right = 2 * idx + 2;
                smallest = idx;

                if (left < heap.size() && heap.get(left).key < heap.get(smallest).key) {
                    smallest = left;
                }
                if (right < heap.size() && heap.get(right).key < heap.get(smallest).key) {
                    smallest = right;
                }
                if (smallest != idx) {
                    swap(idx, smallest);
                    idx = smallest;
                } else {
                    break;
                }
            }
        }

        private void swap(int i, int j) {
            Vertex vi = heap.get(i);
            Vertex vj = heap.get(j);
            heap.set(i, vj);
            heap.set(j, vi);
            // update indices
            vi.heapIndex = j;
            vj.heapIndex = i;
        }
    }

    // -------------
    // Graph Object
    // -------------
    static class Graph {
        HashMap<String, Vertex> map; // from id -> Vertex
        ArrayList<Edge> edges;       // keep edges if needed
        ArrayList<Vertex> vertices;  // all vertex objects

        public Graph() {
            map = new HashMap<>();
            edges = new ArrayList<>();
            vertices = new ArrayList<>();
        }

        public void addVertex(String id) {
            Vertex v = new Vertex(id);
            map.put(id, v);
            vertices.add(v);
        }

        public boolean addEdge(String id1, String id2, float w) {
            // check if vertices exist
            Vertex v1 = map.get(id1);
            Vertex v2 = map.get(id2);
            if (v1 == null || v2 == null) return false;

            // check if this edge (v1->v2) or (v2->v1) already exists
            // we do a quick adjacency check
            for (Edge e : v1.adj) {
                if ((e.u == v1 && e.v == v2) || (e.u == v2 && e.v == v1)) {
                    // edge already exists
                    return false;
                }
            }
            // create edge
            Edge e = new Edge(v1, v2, w);
            v1.adj.add(e);
            v2.adj.add(e);
            edges.add(e);
            return true;
        }

        // Return the edge if it exists, else null
        public Edge getEdge(String id1, String id2) {
            Vertex v1 = map.get(id1);
            Vertex v2 = map.get(id2);
            if (v1 == null || v2 == null) return null;
            for (Edge e : v1.adj) {
                if ((e.u == v1 && e.v == v2) || (e.u == v2 && e.v == v1)) {
                    return e;
                }
            }
            return null;
        }

        // For MST building with Prim
        public Vertex getVertex(String id) {
            return map.get(id);
        }
    }

    // -----------
    // MST Manager
    // -----------
    static class MSTManager {
        Graph graph;

        // MST is stored as a multiway tree:
        // Each Vertex has a "mstNode" -> Node with firstChild/nextSibling/etc.
        // We will "link" them via the 'pred' from Prim (initially).

        public MSTManager(Graph g) {
            this.graph = g;
        }

        //---------------------
        // 1) Prim's Algorithm
        //---------------------
        public void buildInitialMST(String rootId) {
            // We'll reset all vertices
            for (Vertex v : graph.vertices) {
                v.inMST = false;
                v.key = Float.MAX_VALUE;
                v.pred = null;
            }

            Vertex root = graph.getVertex(rootId);
            if (root == null) {
                // invalid, but assume input is correct as per instructions
                return;
            }

            root.key = 0;

            BinaryHeap pq = new BinaryHeap();
            // put all in the heap
            for (Vertex v : graph.vertices) {
                pq.insert(v);
            }

            // Prim
            while (!pq.isEmpty()) {
                Vertex u = pq.extractMin();
                u.inMST = true;

                // for each adjacency
                for (Edge e : u.adj) {
                    Vertex w = (e.u == u) ? e.v : e.u;
                    if (!w.inMST && e.weight < w.key) {
                        pq.decreaseKey(w, e.weight);
                        w.pred = u;
                    }
                }
            }

            // now build the multiway tree structure from pred info
            // first, break any existing MST node links if we are rebuilding
            for (Vertex v : graph.vertices) {
                v.mstNode = new Node(v);
            }
            // link them
            for (Vertex v : graph.vertices) {
                if (v.pred != null) {
                    // v's parent is v.pred in MST
                    Node child = v.mstNode;
                    Node parent = v.pred.mstNode;
                    linkAsChild(parent, child);
                }
            }
        }

        // link childNode as a child of parentNode in the multiway tree
        private void linkAsChild(Node parent, Node child) {
            child.parent = parent;
            // insert child in sorted order among siblings by vertex id
            // So we find the correct place among parent's children
            if (parent.firstChild == null) {
                parent.firstChild = child;
            } else {
                // we want to insert child so that siblings are sorted by id
                Node cursor = parent.firstChild;
                // if 'child' < cursor => put child as new firstChild
                if (child.vertex.id.compareTo(cursor.vertex.id) < 0) {
                    // insert in front
                    child.nextSibling = cursor;
                    cursor.prevSibling = child;
                    parent.firstChild = child;
                } else {
                    // find place in sibling chain
                    while (cursor.nextSibling != null &&
                            cursor.nextSibling.vertex.id.compareTo(child.vertex.id) < 0) {
                        cursor = cursor.nextSibling;
                    }
                    // now insert child after 'cursor'
                    child.nextSibling = cursor.nextSibling;
                    if (cursor.nextSibling != null) {
                        cursor.nextSibling.prevSibling = child;
                    }
                    cursor.nextSibling = child;
                    child.prevSibling = cursor;
                }
            }
        }

        //--------------------------
        // 2) print-mst <rootVertex>
        //--------------------------
        public void printMST(String rootId) {
            Vertex v = graph.getVertex(rootId);
            if (v == null) return; // assume input valid
            // "Directive-----------------> print-mst rootId"
            System.out.println("Directive-----------------> print-mst " + rootId);

            // We need to "evert" so that v's node is root
            Node newRoot = evert(v.mstNode);

            // Then do a preorder traversal from newRoot
            preorderPrint(newRoot, 0);
        }

        // Preorder traversal, each line prints "." * depth, then vertex id
        private void preorderPrint(Node r, int depth) {
            if (r == null) return;
            // print the current node
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<depth; i++) {
                sb.append(". ");
            }
            System.out.println(sb.toString() + r.vertex.id);

            // traverse children in order
            Node child = r.firstChild;
            while (child != null) {
                preorderPrint(child, depth+1);
                child = child.nextSibling;
            }
        }

        //----------------
        // 3) path u v
        //----------------
        public void path(String idu, String idv) {
            System.out.println("Directive-----------------> path " + idu + " " + idv);
            Vertex u = graph.getVertex(idu);
            Vertex v = graph.getVertex(idv);
            if (u == null || v == null) {
                // assume valid input
                System.out.println(idu + "," + idv);
                return;
            }

            // MST path is unique. We'll find path by temporarily making one of them root
            // then from v up to root, or vice versa. Alternatively, we can do a LCA approach,
            // but simpler: evert(u), then path from u to v is just the parent chain of v.

            Node nu = u.mstNode;
            Node nv = v.mstNode;

            // evert(u) so that u is root
            evert(nu);
            // now, the path from u to v is just the chain from v up to u
            LinkedList<String> pathList = new LinkedList<>();
            Node curr = nv;
            while (curr != null) {
                pathList.addFirst(curr.vertex.id);
                if (curr.vertex == u) break;
                curr = curr.parent;
            }
            // print with commas
            // if the path doesn't actually lead to u (disconnected?), we ignore - but MST is connected.
            StringJoiner sj = new StringJoiner(", ");
            for (String s : pathList) {
                sj.add(s);
            }
            System.out.println(sj.toString());
        }

        //----------------------------
        // 4) insert-edge u v w
        //----------------------------
        public void insertEdge(String idu, String idv, float w) {
            System.out.println("Directive-----------------> insert-edge " + idu + " " + idv + " " + w);

            // 1) Check if edge already exists
            Edge e = graph.getEdge(idu, idv);
            if (e != null) {
                // edge already exists
                System.out.println("Invalid Operation");
                return;
            }
            // 2) Insert into graph adjacency
            boolean ok = graph.addEdge(idu, idv, w);
            if (!ok) {
                // means invalid, maybe vertex not found or such
                // but according to problem, we check that vertices exist, so we do:
                System.out.println("Invalid Operation");
                return;
            }

            // 3) Potentially update MST:
            // The new edge might create a cheaper connection in MST. The rule:
            // - find path between u and v in current MST
            // - if on that path the maximum weight edge is bigger than w, we replace it

            Vertex vu = graph.getVertex(idu);
            Vertex vv = graph.getVertex(idv);

            // evert(u), find path from u to v, track max weight edge
            evert(vu.mstNode);
            // walk from v up to u, collecting edges
            Node cur = vv.mstNode;

            Edge maxEdgeOnPath = null;
            float maxWeight = -1;
            Node parentNode = cur.parent;
            while (parentNode != null) {
                Vertex pVert = parentNode.vertex;
                // find the edge in the graph that connects cur.vertex and pVert
                Edge pathEdge = graph.getEdge(cur.vertex.id, pVert.id);
                if (pathEdge.weight > maxWeight) {
                    maxWeight = pathEdge.weight;
                    maxEdgeOnPath = pathEdge;
                }
                if (pVert == vu) break;
                cur = parentNode;
                parentNode = cur.parent;
            }

            if (maxEdgeOnPath != null && w < maxWeight) {
                // remove maxEdgeOnPath from MST, add new edge
                Vertex x = maxEdgeOnPath.u;
                Vertex y = maxEdgeOnPath.v;
                // remove the child->parent link from MST multiway structure
                removeEdgeFromMST(x, y);
                // now link (u,v) in MST
                // we know the MST link is that v's pred = u (or vice versa)
                // let's set vv.pred = vu (or the other way around) -
                // but we must see which side is parent, up to you
                // for consistency, let's do: v's parent = u
                // but we have to fix the Node structure properly (u as parent)
                linkAsChild(vu.mstNode, vv.mstNode);
            }
        }

        //-------------------------------
        // 5) decrease-weight u v w
        //-------------------------------
        public void decreaseWeight(String idu, String idv, float dw) {
            System.out.println("Directive-----------------> decrease-weight " + idu + " " + idv + " " + dw);

            // check edge existence
            Edge e = graph.getEdge(idu, idv);
            if (e == null) {
                // invalid
                System.out.println("Invalid Operation");
                return;
            }
            // new weight = e.weight - dw
            float newWeight = e.weight - dw;
            if (newWeight < 0) {
                // problem statement: we assume all edges are positive weights, but
                // not specified what if it becomes negative. We'll accept it or you might clamp it.
                // We'll just accept it here:
                newWeight = 0;
            }
            e.weight = newWeight;

            // same MST update logic: new edge weight might become beneficial
            Vertex vu = e.u;
            Vertex vv = e.v;

            // evert(u), find max edge on path u->v
            evert(vu.mstNode);
            float maxWeight = -1;
            Edge maxEdgeOnPath = null;
            Node cur = vv.mstNode;
            Node parentNode = cur.parent;
            while (parentNode != null) {
                Vertex pVert = parentNode.vertex;
                Edge pathEdge = graph.getEdge(cur.vertex.id, pVert.id);
                if (pathEdge.weight > maxWeight) {
                    maxWeight = pathEdge.weight;
                    maxEdgeOnPath = pathEdge;
                }
                if (pVert == vu) break;
                cur = parentNode;
                parentNode = cur.parent;
            }

            if (maxEdgeOnPath != null && e.weight < maxWeight) {
                // remove the heavier edge from MST, add this one if not already in MST
                removeEdgeFromMST(maxEdgeOnPath.u, maxEdgeOnPath.v);
                // link (u,v) into MST
                linkAsChild(vu.mstNode, vv.mstNode);
            }
        }

        // -------------------------------------
        // "Evert" operation to make 'node' root
        // -------------------------------------
        // Implementation approach:
        // We'll do a standard multiway-tree evert:
        //   - Climb up from node, reversing parent->child relations.
        //   - The node ends up as root.
        // For simplicity, we'll do a standard top-down approach.
        // The end result is node is root, parent pointers reversed, siblings reversed, etc.

        private Node evert(Node node) {
            if (node == null) return null;

            Node cur = node;
            Node prev = null;
            Node next = null;

            // We'll climb up the parent chain, reversing it
            while (cur != null) {
                next = cur.parent;
                cur.parent = prev;  // reverse parent
                // also handle sibling links carefully:

                // if we are reversing direction: the parent's relationship to cur
                // must be cut as a child, because we'll re-link in the next iteration
                // In a classic evert for a "tree with parent pointers", we wouldn't have to
                // handle siblings so intricately. But let's do it carefully:

                // We will treat each step as: cut(cur) from its parent, then link it as we go
                // A simpler approach is "Link-Cut" style. But let's do something simpler.

                // We'll set cur as root of partial. The sibling structure among
                // cur's children remains the same. We'll break from the parent's child list if needed.

                // detach cur from possible parent's child list
                if (next != null) {
                    // next is old parent. We remove 'cur' from next's children
                    if (next.firstChild == cur) {
                        next.firstChild = null; // might lose siblings, but let's do a thorough approach
                    } else {
                        // if it wasn't the firstChild, we find cur in siblings
                        Node s = next.firstChild;
                        while (s != null) {
                            if (s == cur) {
                                // remove from sibling chain
                                if (s.prevSibling != null) {
                                    s.prevSibling.nextSibling = s.nextSibling;
                                }
                                if (s.nextSibling != null) {
                                    s.nextSibling.prevSibling = s.prevSibling;
                                }
                                break;
                            }
                            s = s.nextSibling;
                        }
                    }
                }

                // now we also remove sibling pointers from cur to avoid confusion
                cur.prevSibling = null;
                cur.nextSibling = null;

                prev = cur;
                cur = next;
            }
            // now node is root of the reversed structure
            return node;
        }

        // Remove edge (x,y) from the MST multiway tree
        // That means we find which node is child of which, then "cut" that child from parent's child list
        private void removeEdgeFromMST(Vertex x, Vertex y) {
            // one must be the ancestor of the other in the MST.
            // We can see from their node structure who is parent.
            Node nx = x.mstNode;
            Node ny = y.mstNode;
            // We'll evert(x) so that x is root, so we can see if y is child
            evert(nx);
            // if y is direct or indirect child, then y.parent is x
            // so the MST edge is (x,y). Let's see if indeed y.parent == x
            if (ny.parent == nx) {
                // cut ny from nx
                cutChild(nx, ny);
            } else if (nx.parent == ny) {
                // cut nx from ny
                cutChild(ny, nx);
            }
        }

        private void cutChild(Node parent, Node child) {
            // remove 'child' from parent's children
            if (parent.firstChild == child) {
                parent.firstChild = child.nextSibling;
                if (child.nextSibling != null) {
                    child.nextSibling.prevSibling = null;
                }
            } else {
                // search siblings
                Node s = parent.firstChild;
                while (s != null) {
                    if (s == child) {
                        if (s.prevSibling != null) {
                            s.prevSibling.nextSibling = s.nextSibling;
                        }
                        if (s.nextSibling != null) {
                            s.nextSibling.prevSibling = s.prevSibling;
                        }
                        break;
                    }
                    s = s.nextSibling;
                }
            }
            child.parent = null;
            child.prevSibling = null;
            child.nextSibling = null;
        }
    }

    // ------------------
    // Main Program Flow
    // ------------------
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java MSTProgram <inputfile>");
            return;
        }

        String filename = args[0];

        try {
            Scanner sc = new Scanner(new File(filename));
            Graph graph = new Graph();

            // 1) Read number of vertices
            int n = Integer.parseInt(sc.nextLine().trim());

            // 2) Read vertex identifiers
            ArrayList<String> vertexIds = new ArrayList<>();
            for (int i=0; i<n; i++) {
                String id = sc.nextLine().trim();
                graph.addVertex(id);
                vertexIds.add(id);
            }

            // 3) Read number of edges
            int e = Integer.parseInt(sc.nextLine().trim());
            // 4) Read edges
            for (int i=0; i<e; i++) {
                String line = sc.nextLine().trim();
                String[] parts = line.split("\\s+");
                // format: vertex1 vertex2 weight
                String v1 = parts[0];
                String v2 = parts[1];
                float w = Float.parseFloat(parts[2]);
                graph.addEdge(v1, v2, w);
            }

            // 5) Build MST using Prim, root is the first vertex read
            MSTManager mst = new MSTManager(graph);
            String firstVertexId = vertexIds.get(0);
            mst.buildInitialMST(firstVertexId);

            // 6) Process directives
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.equals("")) continue;  // skip empty

                // parse directive
                String[] tokens = line.split("\\s+");
                if (tokens[0].equals("print-mst")) {
                    // format: print-mst u
                    String rootId = tokens[1];
                    mst.printMST(rootId);
                }
                else if (tokens[0].equals("path")) {
                    // format: path u v
                    String u = tokens[1];
                    String v = tokens[2];
                    mst.path(u, v);
                }
                else if (tokens[0].equals("insert-edge")) {
                    // format: insert-edge u v w
                    String u = tokens[1];
                    String v = tokens[2];
                    float w = Float.parseFloat(tokens[3]);
                    mst.insertEdge(u, v, w);
                }
                else if (tokens[0].equals("decrease-weight")) {
                    // format: decrease-weight u v w
                    String u = tokens[1];
                    String v = tokens[2];
                    float dw = Float.parseFloat(tokens[3]);
                    mst.decreaseWeight(u, v, dw);
                }
                else if (tokens[0].equals("quit")) {
                    System.out.println("Directive-----------------> quit");
                    break;
                }
                else {
                    // unknown directive
                    // not specified by problem, we can ignore or do something
                }
            }

            sc.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
