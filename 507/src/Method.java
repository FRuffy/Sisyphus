import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;

import normalizers.Normalizer;

import java.util.*;

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

	public Method(MethodDeclaration methodDeclaration) {
		this.originalDecl = methodDeclaration;
		this.methodName = methodDeclaration.getNameAsString();
		this.parameters = methodDeclaration.getParameters();
		this.returnType = methodDeclaration.getType();
		this.body = methodDeclaration.getBody().get();

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
	
	/**
	 * Return a new method that is equivalent to this method,
	 * but normalized by the given normalizer
	 */
	public Method normalize(Normalizer norm){
		norm.initialize(this.originalDecl);
		return new Method((MethodDeclaration)norm.result());
	}
}
