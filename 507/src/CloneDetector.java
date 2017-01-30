import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;

/*
 * The detector class
 * Takes the parser output and tries to find similarities
 * Right now we only have a simple line by line comparison of a full file
 * 
*/

public class CloneDetector {
	
	
	public CloneDetector() {
		
	}

	// The algorithm
	public boolean detectClone(BufferedReader bufferedLibReader, BufferedReader bufferedFunctionReader)
			throws IOException {
		String funLine;
		String libLine;
		
		while (true) {

			libLine = bufferedLibReader.readLine();
			funLine = bufferedFunctionReader.readLine();

			if (libLine == null || funLine == null) {
				break;
			}
			if (!(libLine.equals(funLine))) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * This method should do some kind of comparison between Method 1 and Method 2 and return
	 * true if they are exact/near matches. Right now they just return true if they 
	 * are exact matches
	 */
	public boolean matchMethods(Method method1, Method method2){
		if(method1.getMethodName().compareToIgnoreCase(method2.getMethodName())!=0){
			return false;
			
		}
		if(method1.getReturnType().toString().compareTo(method2.getReturnType().toString())!=0){
			return false;
		}
		List<Parameter> parameters1 = method1.getMethodParameters();
		List<Parameter> parameters2 = method2.getMethodParameters();
		if(parameters1.size()!=parameters2.size()){
			return false;
		}
		boolean[] param2Matched = new boolean[parameters2.size()];
		for(int i = 0; i<parameters1.size(); i++){
			boolean foundMatch = false;
			for(int j = 0; j<parameters2.size(); j++){
				if(parameters1.get(i).getType().toString().compareTo(parameters2.get(j).toString())==0){
					if(!param2Matched[j]){
						param2Matched[j] = true;
						foundMatch = true;
					}
					
				}
			}
			if(!foundMatch){
				return false;
			}
		}
		BlockStmt body1 = method1.getBody();
		BlockStmt body2 = method2.getBody();
		if(body1.toString().compareToIgnoreCase(body2.toString())!=0){
			return false;
		}
		return true;
		
	}
	
}
