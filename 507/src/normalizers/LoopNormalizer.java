package normalizers;

import java.util.Optional;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class LoopNormalizer extends Normalizer {

	/*
	private static BlockStmt mergeBlocks(Statement s1, Statement s2){
		NodeList<Statement> newStmts = new NodeList<Statement>();

		if (s1 instanceof BlockStmt && s2 instanceof BlockStmt){
			BlockStmt b1 = (BlockStmt) s1;
			BlockStmt b2 = (BlockStmt) s2;
			newStmts.addAll(b1.getStatements());
			newStmts.addAll(b2.getStatements());

		} else if (s1 instanceof BlockStmt){
			BlockStmt b1 = (BlockStmt) s1;
			newStmts.addAll(b1.getStatements());
			newStmts.add(s2);
		} else if (s2 instanceof BlockStmt){
			BlockStmt b2 = (BlockStmt) s2;
			newStmts.add(s1);
			newStmts.addAll(b2.getStatements());
		} else {
			newStmts.add(s1);
			newStmts.add(s2);

		}
		if (newStmts.isEmpty()){
			System.err.println("EMPTY MERGE OF " + s1 + " AND " + s2);
		}
		return new BlockStmt(newStmts);
	}*/

	private static BlockStmt exprBlock(NodeList<Expression> l){
		NodeList<Statement> newList = new NodeList<Statement>();
		for (Expression e : l){
			newList.add(new ExpressionStmt(e));
		}
		return new BlockStmt(newList);
	}

	private class FixLoopsVisitor extends ModifierVisitor<Object>{

		public FixLoopsVisitor(){

		}

		protected <T extends Node> T modifyNode(T _node, Object _arg) {
			if (_node == null) {
				return null;
			}
			Node r = (Node) _node.accept(this, _arg);
			if (r == null) {
				return null;
			}
			return (T) r;
		}

		@Override
		public Visitable visit(ForStmt n, Object arg) {
			//Clone this like before
			Statement body = modifyNode(n.getBody(), arg);
			Expression compare;
			if (n.getCompare().isPresent()){
				compare = modifyNode(n.getCompare().get(), arg);	
			} else {
				compare = new BooleanLiteralExpr(true);
			}
			NodeList<Expression> initialization = modifyList(n.getInitialization(), arg);
			NodeList<Expression> update = modifyList(n.getUpdate(), arg);
			//Comment comment = modifyNode(n.getComment(), arg);

			//While loop body is old body, plus the update at the end
			Statement newBody;
			if (update != null){
				BlockStmt bs = new BlockStmt();
				bs.addStatement(body);
				bs.addStatement(exprBlock(update));
				newBody = bs;
			}
			else {
				newBody = body;
			}
			
			if (newBody.getChildNodes().isEmpty()){
				System.err.println("Empty block");
			}

			Optional<Range> or = n.getRange();
			WhileStmt loop;
			if (or.isPresent()){
				loop = new WhileStmt(or.get(), compare, newBody);
			} else {
				loop = new WhileStmt(compare, newBody);
			}
			
			


			//System.err.println("COND:" + loop.getCondition());



			//Finally, return a block with the initialization appended to the loop

			BlockStmt r = new BlockStmt();
			r.addStatement(exprBlock(initialization));
			r.addStatement(loop);
			//r.setComment(comment);

			//System.err.println("Changed loop:\n" + n);
			//System.err.println("to:\n" + r);

			return r;
		}		
		@Override
		public Visitable visit(DoStmt n, Object arg) {
			Statement body = modifyNode(n.getBody(), arg);
			Expression condition = modifyNode(n.getCondition(), arg);
			//Comment comment = modifyNode(n.getComment(), arg);

			//Make a while loop from our Do-While loop i.e. same cond and body
			WhileStmt loop = new WhileStmt(condition.clone(), body.clone());

			//Finally, return a block with an initial body run appended to the loop

			BlockStmt r = new BlockStmt();
			r.addStatement(body);
			r.addStatement(loop);

			return r;
		}


		//http://stackoverflow.com/questions/85190/how-does-the-java-for-each-loop-work
		@Override
		public Visitable visit(ForeachStmt n, Object arg) {
			Statement body = modifyNode(n.getBody(), arg);
			Expression iterable = modifyNode(n.getIterable(), arg);
			VariableDeclarationExpr variable = modifyNode(n.getVariable(), arg);
			//Comment comment = modifyNode(n.getComment(), arg);

			Type iterType = 
					new TypeParameter("Iterable", 
							new NodeList<ClassOrInterfaceType>(variable.getElementType()));

			VariableDeclarationExpr iterDecl = new VariableDeclarationExpr(iterType, "__iter");
			AssignExpr iterAssign = new AssignExpr(new NameExpr("__iter"), iterable, AssignExpr.Operator.ASSIGN);

			//New body: Create variable with iter.next() assigned to it
			Expression elemValue = new MethodCallExpr(new NameExpr("__iter"), "next");
			Expression elemDecl = 
					new AssignExpr(variable, elemValue, AssignExpr.Operator.ASSIGN); 
			//Then do the normal loop body
			BlockStmt newBody = new BlockStmt();
			newBody.addStatement(new ExpressionStmt(elemDecl)); 
			newBody.addStatement(new ExpressionStmt(iterAssign));
			newBody.addStatement(body);

			//End condition, check if iter has next
			Expression endCond = new MethodCallExpr(new NameExpr("__iter"), "hasNext");

			//Make the for loop doing each step of the for-each
			ForStmt loop = 
					new ForStmt(new NodeList<Expression>(iterDecl),
							endCond, new NodeList<Expression>(), newBody);

			//Finally, turn our for-loop into a while loop and return
			return visit(loop, arg);
		}

		private <N extends Node> NodeList<N> modifyList(NodeList<N> list, Object arg) {
			if (list == null) {
				return null;
			}
			return (NodeList<N>) list.accept(this, arg);
		}


	}


	@Override
	public Node result() {
		this.startBlock.accept(new FixLoopsVisitor(), null);
		return this.startBlock;
	}

}
