package core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;

import datastructures.NodeWrapper;
import datastructures.PDGGraphViz;
import dfg.DataDependencyGraphFinder;
import jgrapht.DOTExporter;
import jgrapht.DirectedGraph;
import jgrapht.experimental.dag.DirectedAcyclicGraph;
import jgrapht.graph.DefaultEdge;
import jgrapht.graph.DirectedPseudograph;
import normalizers.Normalizer;
import normalizers.StandardForm;
import parsers.ControlDependencyParser;
import parsers.ControlFlowParser;
import visitors.ASTUtil;

/*
 * Method class that holds information about a particular method
 * - the name of the method, the return type of the method.
 */

public class Method {
	private String methodName;
	private Type returnType;
	private List<Parameter> parameters;
	private BlockStmt body;
	private MethodDeclaration originalDecl;
	private DirectedAcyclicGraph<NodeWrapper, DefaultEdge> cdg;
	private DirectedPseudograph<NodeWrapper, DefaultEdge> ddg;
	private DirectedPseudograph<Node, DefaultEdge>  pdg;
	private NodeFeature nodeFeature;
	private Method unNormalized;

	public Method(MethodDeclaration methodDeclaration) {
		this.originalDecl = methodDeclaration;
		this.methodName = methodDeclaration.getNameAsString();
		this.parameters = methodDeclaration.getParameters();
		this.returnType = methodDeclaration.getType();
		this.body = methodDeclaration.getBody().get();
		this.pdg = this.constructPDG();
		//this.nodeFeature = this.constructMethodFeature();
		this.trimBody();
	}
	
	public void printComparison(){
		System.out.println("Method before normalizing:");
		System.out.println(unNormalized.originalDecl);
		System.out.println("Method after normalizing:");
		System.out.println(this.originalDecl);
	}
	
	public DirectedAcyclicGraph<NodeWrapper, DefaultEdge> getCdg(){
		return cdg;
	}
	public DirectedPseudograph<NodeWrapper, DefaultEdge> getDdg(){
		return ddg;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public Type getReturnType() {
		return this.returnType;
	}

	public List<Parameter> getMethodParameters() {
		return this.parameters;
	}

	public BlockStmt getBody() {
		return this.body;
	}

	public BlockStmt getFilteredBody() {
		BlockStmt filteredBody = (BlockStmt) this.body.clone();
		for (Comment co : filteredBody.getAllContainedComments()) {
			co.remove();
		}

		return filteredBody;
	}

	public void trimBody() {
		BlockStmt filteredBody = (BlockStmt) this.body.clone();
		for (Comment co : filteredBody.getAllContainedComments()) {
			co.remove();
		}
	}

	public MethodDeclaration getFilteredMethod() {
		MethodDeclaration methodDeclaration = this.originalDecl;
		for (Comment co : methodDeclaration.getAllContainedComments()) {
			co.remove();
		}

		return methodDeclaration;
	}

	/*
	 * Do a traversal of the nodes of the method body without comments and
	 * return the list
	 */
	public List<Node> getMethodNodes() {
		List<Node> methodNodes = new ArrayList<Node>();
		List<Node> queueNodes = new ArrayList<Node>();
		queueNodes.add(this.getFilteredMethod());
		while (!queueNodes.isEmpty()) {
			Node current = queueNodes.remove(0);
			// System.out.println("current: "+current+", class:
			// "+current.getClass());
			if (!(current instanceof Comment)) {
				methodNodes.add(current);
			}
			List<Node> currentChildren = current.getChildNodes();
			for (Node child : currentChildren) {
				queueNodes.add(child);
			}
		}
		return methodNodes;

	}

	/*
	 * Combine the NodeFeatures of all Nodes into one NodeFeature at the root
	 * This will be the characteristic feature of the whole method (from the
	 * Deckard paper)
	 */
	private NodeFeature getMethodFeature(Node current) {
		NodeFeature nodeFeature = new NodeFeature();
		if(current.getClass().toString().equals("com.github.javaparser.ast.expr.MethodCallExpr")){
			nodeFeature.addNode(current.toString());
		}
		else{
			nodeFeature.addNode(current.getClass().toString());
		}
		if (current.getChildNodes().size() == 0) {
			return nodeFeature;
		}
		List<Node> currentChildren = current.getChildNodes();
		for (Node child : currentChildren) {
			//System.out.println("parent class "+current.getClass().toString());
			//System.out.println("child class "+child.getClass().toString());
			NodeFeature childMethodFeature = getMethodFeature(child);
			nodeFeature.combineNodeFeatures(childMethodFeature);
		}
		return nodeFeature;

	}

	public NodeFeature getMethodFeature() {
		//System.out.println("considering method name "+this.getMethodName());
		BlockStmt root = this.getFilteredBody();
		NodeFeature methodFeature = getMethodFeature(root);
		return methodFeature;
	}
	
	/*public NodeFeature getMethodFeature(){
		return this.nodeFeature;
	}*/

	/**
	 * Return a new method that is equivalent to this method, but normalized by
	 * the given normalizer
	 */
	public Method normalize() {
		Method ret = new Method((MethodDeclaration)StandardForm.toStandardForm(this.originalDecl));
		ret.unNormalized = this;
		return ret;
	}
	
	public boolean isRecursive(){
		return containsCallTo(this.methodName);
	}
	
	public boolean containsCallTo(String function){
		return ASTUtil.occursFree(this.body, function);
	}
	
	public DirectedPseudograph<Node, DefaultEdge> constructPDG(){
		//System.out.println("Building pdg for method: "+this.getMethodName());
		ControlFlowParser cfp = new ControlFlowParser(this);
		DirectedPseudograph<NodeWrapper, DefaultEdge> cfg = cfp.getCFG();
		ControlDependencyParser cdp = new ControlDependencyParser(cfg);
		cdg = cdp.getCDG();
		DataDependencyGraphFinder ddgf = new DataDependencyGraphFinder(cfg, this, cfp.getInitialNode());
		ddg = ddgf.findReachingDefs();
		
		//combine cdg and ddg to pdg with Nodes as vertices rather
		//than NodeWrappers
		DirectedPseudograph<Node, DefaultEdge> pdgNode = new DirectedPseudograph<>(DefaultEdge.class);
		for(NodeWrapper n: cdg.vertexSet()){
			pdgNode.addVertex(n.NODE);
		}
		for(NodeWrapper n: ddg.vertexSet()){
			if (!pdgNode.containsVertex(n.NODE)){
				pdgNode.addVertex(n.NODE);
			}
			
		}
		for(DefaultEdge e: cdg.edgeSet()){
			pdgNode.addEdge(cdg.getEdgeSource(e).NODE, cdg.getEdgeTarget(e).NODE);
		}
		for(DefaultEdge e: ddg.edgeSet()){
			pdgNode.addEdge(ddg.getEdgeSource(e).NODE, ddg.getEdgeTarget(e).NODE);
		}
		
		
		PDGGraphViz.writeDot(cdg, "cdg.dot");
		PDGGraphViz.writeDot(ddg, "ddg.dot");
		PDGGraphViz.writeDotNode(pdgNode, "pdg.dot");
		
		return pdgNode;
	
	}
	
	public DirectedPseudograph<Node, DefaultEdge> getPDG(){
		return this.pdg;
	}
}
