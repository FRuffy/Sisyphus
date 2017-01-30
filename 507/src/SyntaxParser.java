import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;


/*
 * The parser class
 * Takes a filename and attempts to parse it
 * Right now we only have a generic file reader
 * 
*/

public class SyntaxParser {
	
	private CompilationUnit cu;
	private ArrayList<MethodDeclaration> methodDeclarationList;
	
	public SyntaxParser(String filename) throws FileNotFoundException{
		FileInputStream in = new FileInputStream(filename);	
		this.cu = JavaParser.parse(in);
		this.methodDeclarationList = this.getMethodDeclaration();
	}
	
	

	/********************************************************************************************/
	/*
	 * Functions imported from the javaparser lib
	 */
	public void parse() {
		new VoidVisitorAdapter<Object>() {
			@Override
			public void visit(MethodCallExpr n, Object arg) {
				super.visit(n, arg);
				System.out.println(" [L " + n.getBegin() + "] " + n);
			}
		}.visit(this.cu, null);
			
	}
	
	/*Returns an arraylist of all MethodCallExpr objects from which
	 * we can gain information about the methods in a file
	 */
	public ArrayList<MethodDeclaration> getMethodDeclaration(){
		ArrayList<MethodDeclaration> methodDecList = new ArrayList<MethodDeclaration>();
		new VoidVisitorAdapter<Object>() {
			@Override
			public void visit(MethodDeclaration n, Object arg) {
				super.visit(n, arg);
				methodDecList.add(n);
				//System.out.println(n.getName());
			}
		}.visit(this.cu, null);
		return methodDecList;
	}
	
	/*
	 * Returns an arraylist of Method objects from parsing information
	 */
	public ArrayList<Method> getMethods(){
		ArrayList<Method> methodList = new ArrayList<Method>();
		for(MethodDeclaration n: this.methodDeclarationList){
			methodList.add(new Method(n));
		}
		return methodList;
	}
	
	/*Returns an arraylist of method names in the class*/
	public ArrayList<String> getMethodNames() {
		ArrayList<Method> methodList = this.getMethods();
		ArrayList<String> methodNames = new ArrayList<String>();
		for(Method call: methodList){
			methodNames.add(call.getMethodName());
		}
		return methodNames;
	}

	/*Returns an arraylist of method return types in the class*/
	public ArrayList<Type> getReturnTypes(){
		ArrayList<Method> methodList = this.getMethods();
		ArrayList<Type> returnTypes = new ArrayList<Type>();
		for(Method call: methodList){
			returnTypes.add(call.getReturnType());
		}
		return returnTypes;
	}	
	
	
}