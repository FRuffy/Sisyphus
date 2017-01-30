import java.io.IOException;
import java.util.*;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;

public class CodeHelper {
	public static void main(String[] args) {
		
		//String libName = "Math.class";
		// String functionCode = args[0];
		//String srcName = "src/TestCode.java";
		String libName = "lib.txt";
		String srcName = "code.txt";
		ArrayList<String> libMethodNames = new ArrayList<String>(); 
		ArrayList<String> srcMethodNames = new ArrayList<String>();
		ArrayList<Type> libMethodReturnTypes = new ArrayList<Type>(); 
		ArrayList<Type> srcMethodReturnTypes = new ArrayList<Type>();
		List<Parameter> srcMethodParameter = new ArrayList<Parameter> ();
		BlockStmt srcMethodBody = new BlockStmt();
		ArrayList<Method> libMethods = new ArrayList<Method>();
		ArrayList<Method> srcMethods = new ArrayList<Method>();
		//initialize java parser for both library and source code.
		try{
			SyntaxParser libparser = new SyntaxParser(libName);
			SyntaxParser srcparser = new SyntaxParser(srcName);
			libMethodNames = libparser.getMethodNames();
			srcMethodNames = srcparser.getMethodNames();
			libMethodReturnTypes = libparser.getReturnTypes();
			srcMethodReturnTypes = srcparser.getReturnTypes();
			libMethods = libparser.getMethods();
			srcMethods = srcparser.getMethods();
			srcMethodParameter = srcMethods.get(3).getMethodParameters();
			srcMethodBody = srcMethods.get(3).getBody();
		}catch (IOException e) {
			e.printStackTrace();
			new RuntimeException(e);
		}
		
		
		// Debug
		//System.out.println("Working Directory = " + System.getProperty("user.dir"));
		
		// Test the results of javaparser functions
		System.out.println("library method names");
		System.out.println(libMethodNames);
		System.out.println("library method return types");
		System.out.println(libMethodReturnTypes);
		System.out.println("source code method names");
		System.out.println(srcMethodNames);
		System.out.println("source method return types");
		System.out.println(srcMethodReturnTypes);
		System.out.println("Parameter type of main method");
		System.out.println(srcMethodParameter.get(0).getType());
		System.out.println("Body of main method");
		System.out.println(srcMethodBody.toString());
		
		// ugly stuff
		CloneDetector cloneDetect = new CloneDetector();
		System.out.println("Testing match method");
		Method libMethod = libMethods.get(1);
		Method srcMethod = srcMethods.get(1);
		System.out.println("Method1: "+libMethod.getMethodName()+" Method2: "+srcMethod.getMethodName());
		System.out.println(cloneDetect.matchMethods(libMethod, srcMethod));
		libMethod = libMethods.get(2);
		srcMethod = srcMethods.get(2);
		System.out.println("Method1: "+libMethod.getMethodName()+" Method2: "+srcMethod.getMethodName());
		System.out.println(cloneDetect.matchMethods(libMethod, srcMethod));
	}
}
