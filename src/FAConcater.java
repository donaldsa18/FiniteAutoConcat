import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

/**
 * @author donaldsa18
 *
 */
public class FAConcater {
	private static boolean debug = false;
	/**
	 * Run the program with the -d 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			int numSuccess = 0;
			if(args.length == 4 || args.length == 6  || args.length == 8) {
				HashMap<String,String> options = new HashMap<>();
				for(int i=0;i+1<args.length;i+=2) {
					options.put(args[i], args[i+1]);
				}
				if(options.containsKey("-d")) {
					if(options.get("-d").toLowerCase().equals("t") || options.get("-d").toLowerCase().equals("true")) {
						debug = true;
					}
				}
				if(options.containsKey("-rj")) {
					numSuccess += concatDirJFF(options.get("-t"), options.get("-rj"));
				}
				if(options.containsKey("-r")) {
					numSuccess += concatDirAll(options.get("-t"), options.get("-r"));
				}
			}
			if(numSuccess == 0) {
				System.out.println("Options:\n-t\t\tThe path of the folder to scan for JFF and NFA files\n-r\t\tThe path of the folder to save concatenated NFA files\n-rt\t\tThe path of the folder to save concatenated JFF files\n-d\t\ttrue or t to enable debug logging");
				numSuccess += concatDirJFF("tests", "resultsJFLAP");
				numSuccess += concatDirAll("tests", "results");
			}
			System.out.println(numSuccess + " successful NFA concatenations");
		}
		catch (NullPointerException e) {
			System.err.println("Null pointer, something went wrong.");
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println("Something is broken, halp.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Concatenates all JFLAP files in a folder and saves it to a JFLAP file
	 * @param dirPath
	 * @param savePath
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private static int concatDirJFF(String dirPath, String savePath) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		File dir = new File(dirPath);
		File save = new File(savePath);
        File[] files = dir.listFiles(new FilenameFilter() { 
                 public boolean accept(File dir, String filename)
                      { return filename.endsWith(".jff"); }
        } );
        int rtn = 0;
        for(int i=0;i<files.length;i++) {
        	for(int j=i+1;j<files.length;j++) {
        		if(debug) {
        			System.out.println("Trying to concat file "+":"+files[i].getName()+" with file "+":"+files[j].getName());
        		}
        		NFA A = FAReaderWriter.readJFF(files[i].getPath(),debug);
        		NFA B = FAReaderWriter.readJFF(files[j].getPath(),debug);
        		NFA AB = FATransformer.concat(A, B);
        		String newPath = save.getAbsolutePath()+"\\"+files[i].getName().replaceFirst("[.][^.]+$", "")+files[j].getName();
        		rtn++;
        		if(debug) {
        			System.out.println("Saving to "+newPath);
        		}
    			FAReaderWriter.writeJFF(AB, newPath,debug);
        	}
        }
        return rtn;
	}
	
	/**
	 * Concatenates all JFLAP/NFA files in a folder and saves it to a NFA file
	 * @param dirPath
	 * @param savePath
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private static int concatDirAll(String dirPath, String savePath) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		File dir = new File(dirPath);
		File save = new File(savePath);
        File[] filesNFA = dir.listFiles(new FilenameFilter() { 
                 public boolean accept(File dir, String filename)
                      { return filename.endsWith(".nfa"); }
        } );
        
        File[] filesJFF = dir.listFiles(new FilenameFilter() { 
            public boolean accept(File dir, String filename)
                 { return filename.endsWith(".jff"); }
        } );
        File[] files = new File[filesNFA.length+filesJFF.length];
        NFA[] NFAs = new NFA[filesNFA.length+filesJFF.length];
        for(int i=0;i<filesNFA.length;i++) {
        	NFAs[i] = FAReaderWriter.read(filesNFA[i].getPath(),debug);
        	files[i] = filesNFA[i];
        }
        for(int i=0;i<filesJFF.length;i++) {
        	NFAs[filesNFA.length+i] = FAReaderWriter.readJFF(filesJFF[i].getPath(),debug);
        	files[filesNFA.length+i] = filesJFF[i];
        }
        int rtn = 0;
        for(int i=0;i<NFAs.length;i++) {
        	for(int j=i+1;j<NFAs.length;j++) {
        		if(debug) {
        			System.out.println("Trying to concat file "+":"+files[i].getName()+" with file "+":"+files[j].getName());
        		}
        		NFA AB = FATransformer.concat(NFAs[i], NFAs[j]);
        		String newPath = save.getAbsolutePath()+"\\"+files[i].getName().replaceFirst("[.][^.]+$", "");
        		newPath += files[j].getName().replaceFirst("[.][^.]+$", "")+".nfa";
        		rtn++;
        		if(debug) {
        			System.out.println("Saving to "+newPath);
        		}
    			FAReaderWriter.write(AB, newPath,debug);
        	}
        }
        return rtn;
	}
	
	/**
	 * Tests that AB behaves as it should using accepting strings of length limit from A and B
	 * @param A
	 * @param B
	 * @param AB
	 * @param limit
	 * @return
	 */
	public static boolean testAB(NFA A, NFA B, NFA AB, int limit) {
		
		//Generate and test all strings of length equal to the limit
		HashMap<String,Boolean> strA = findAcceptingStrs(A,limit);
		HashMap<String,Boolean> strB = findAcceptingStrs(B,limit);
		HashMap<String,Boolean> strAB = findAcceptingStrs(AB,limit*2);
		
		System.out.println("A has "+strA.size()+" accepting strings of length "+limit+" and below.");
		System.out.println("B has "+strB.size()+" accepting strings of length "+limit+" and below.");
		System.out.println("AB has "+strAB.size()+" accepting strings of length "+limit*2+" and below.");
		
		//Iterate over every string tested with A
		Iterator<Entry<String, Boolean>> aIt = strA.entrySet().iterator();
		while(aIt.hasNext()) {
			Entry<String, Boolean> aEntry = aIt.next();
			
			//Iterate over every string tested with B
			Iterator<Entry<String, Boolean>> bIt = strB.entrySet().iterator();
			while(bIt.hasNext()) {
				Entry<String, Boolean> bEntry = bIt.next();
				
				//If the string1 is in A and string2 is in B, string1+string2 must be in AB
				if(aEntry.getValue().booleanValue() && bEntry.getValue().booleanValue()) {
					if(!strAB.containsKey(aEntry.getKey()+","+bEntry.getKey())) {
						System.err.println("Error: The string "+aEntry.getKey()+" in A and the string "+bEntry.getKey()+" in B is not in AB");
						return false;
					}
					else {
						if(debug) {
							System.out.println("Tested A="+aEntry.getKey()+" B="+bEntry.getKey());
						}
					}
				}//If not, string1+string2 must not be in AB
				else {
					if(strAB.containsKey(aEntry.getKey()+","+bEntry.getKey())) {
						String inA = " in A";
						String inB = " in B";
						if(!aEntry.getValue().booleanValue()) {
							inA = " is not"+inA;
						}
						if(!bEntry.getValue().booleanValue()) {
							inB = " is not"+inB;
						}
						System.err.println("Error: The string "+aEntry.getKey()+inA+" and the string "+bEntry.getKey()+inB+" but the string "+aEntry.getKey()+","+bEntry.getKey()+" is in AB");
						return false;
					}
					else {
						if(debug) {
							System.out.println("Tested A="+aEntry.getKey()+" B="+bEntry.getKey());
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Test the FA with all possible string combinations in sigma equal to the size specified
	 * @param fa
	 * @param limit
	 * @return
	 */
	public static HashMap<String,Boolean> findAcceptingStrs(NFA fa, int limit) {
		String[] sigma = new String[fa.getSigma().size()];
		fa.getSigma().toArray(sigma);
		ArrayList<String> possStrs = getNextStr(sigma, limit, "");
		HashMap<String,Boolean> strs = new HashMap<>();
		for(String test : possStrs) {
			fa.reset();
			ArrayList<NFA> possNFAs = new ArrayList<NFA>();
			if(debug) {
				System.out.println("Testing sequence "+test);
			}
			String[] sequence = test.split(",");
			for(int i=0;i<sequence.length;i++) {
				ArrayList<NFA> nextPossNFAs = new ArrayList<NFA>();
				for(NFA possNFA : possNFAs) {
					if(possNFA.isAccepted()) {
						strs.put(test, true);
						break;
					}
					nextPossNFAs.addAll(possNFA.read(sequence[i]));
				}
				possNFAs = nextPossNFAs;
			}
			if(!fa.isAccepted()) {
				strs.put(test,false);
			}
		}
		return strs;
	}
	
	/**
	 * Generates the regex [sigma]{size} for testing NFAs
	 * @param sigma
	 * @param size
	 * @param currStr
	 * @return
	 */
	private static ArrayList<String> getNextStr(String[] sigma, int size, String currStr) {
		ArrayList<String> strs = new ArrayList<>();
		if(size == 0) {
			strs.add(currStr);
		}
		if(!currStr.isEmpty()) {
			currStr += ",";
		}
		for(int i=0;i<sigma.length && size > 0;i++) {
			ArrayList<String> tests = getNextStr(sigma,size-1,currStr+sigma[i]);
			strs.addAll(tests);
		}
		return strs;
	}
}
