import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that handles reading a NFA from a file and writing a NFA to a file in two formats: .nfa and .jff
 * @author donaldsa18
 *
 */
public class FAReaderWriter {
	
	/**
	 * Reads a NFA file to a NFA object
	 * @param file
	 * @param debug
	 * @return
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static NFA read(String file, boolean debug) throws FileNotFoundException {
		Scanner s = new Scanner(new File(file));
		
		//Read each line and parse simple lists
		String[] Q = s.nextLine().split(",");
		String[] sigma = s.nextLine().split(",");
		String delta = s.nextLine();
		String qStart = s.nextLine();
		String[] F = s.nextLine().split(",");
		
		s.close();
		
		//Convert the lists into ArrayLists so they can be easily imported into HashSets/HashMaps
		ArrayList<String> Qarr = new ArrayList<String>(Arrays.asList(Q));
		ArrayList<String> sigmaArr = new ArrayList<String>(Arrays.asList(sigma));
		ArrayList<String> Farr = new ArrayList<String>(Arrays.asList(F));
		Object deltaList = buildList(delta,3).get(0);
		
		if(debug) {
			System.out.println("File: "+file);
			System.out.println("Q: "+toCSVStr(Q));
			System.out.println("Sigma: "+toCSVStr(sigma));
			System.out.println("Delta: "+deltaList);
			System.out.println("qStart: "+qStart);
			System.out.println("F: "+toCSVStr(F)+"\n");
		}
		
		if(deltaList instanceof ArrayList) {
			//TODO:check types better
			ArrayList<ArrayList<ArrayList<String>>> deltaArr = (ArrayList<ArrayList<ArrayList<String>>>)deltaList;
			return new NFA(Qarr,sigmaArr, deltaArr,qStart,Farr);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Reads a JFF file and creates a NFA object describing it
	 * @param file
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static NFA readJFF(String file, boolean debug) throws SAXException, IOException, ParserConfigurationException {
		//Parse JFF file which is an XML file
		File inputFile = new File(file);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();
		
		//Get a list of the states and transitions
		NodeList states = doc.getElementsByTagName("state");
		NodeList transitions = doc.getElementsByTagName("transition");
		
		//Check that the file is a FA file
		Element type = (Element)doc.getElementsByTagName("type").item(0);
		if(!type.getTextContent().equals("fa")) {
			System.err.println("Invalid JFF file, not finite automaton.");
			throw new IOException();
		}
		
		//Initialize NFA tuple variables
		HashMap<String,String> Q = new HashMap<String,String>();
		HashSet<String> sigma = new HashSet<String>();
		HashMap<String,HashMap<String,ArrayList<String>>> delta = new HashMap<String,HashMap<String,ArrayList<String>>>();
		String qStart = "";
		HashSet<String> F = new HashSet<String>();

		//Keep track of X and Y coords for later use
		HashMap<String,String> Qx = new HashMap<String,String>();
		HashMap<String,String> Qy = new HashMap<String,String>();
		
		//Parse through all the states, populating Q, setting the initial state, and populating F.
		for(int i=0;i<states.getLength();i++) {
			Node n = states.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				Q.put(e.getAttribute("id"),e.getAttribute("name"));
				if(e.hasChildNodes()) {
					NodeList prop = e.getChildNodes();
					for(int j=0;j<prop.getLength();j++) {
						Node p = prop.item(j);
						if(p.getNodeName().equals("initial")) {
							qStart = e.getAttribute("name");
						}
						else if(p.getNodeName().equals("final")) {
							F.add(e.getAttribute("name"));
						}
						else if(p.getNodeName().equals("x")) {
							Qx.put(e.getAttribute("name"), e.getElementsByTagName("x").item(0).getTextContent());
						}
						else if(p.getNodeName().equals("y")) {
							Qy.put(e.getAttribute("name"), e.getElementsByTagName("y").item(0).getTextContent());
						}
					}
				}
			}
		}
		
		//Parse through all of the transitions, creating delta as we go
		for(int i=0;i<transitions.getLength();i++) {
			Node n = transitions.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				String from = e.getElementsByTagName("from").item(0).getTextContent();
				String to = e.getElementsByTagName("to").item(0).getTextContent();
				String[] read = e.getElementsByTagName("read").item(0).getTextContent().split(", ");
				if(!delta.containsKey(Q.get(from))) {
					delta.put(Q.get(from), new HashMap<String,ArrayList<String>>());
				}
				if(read != null && read.length > 0) {
					for(String readchar : read) {
						sigma.add(readchar);
						if(!delta.get(Q.get(from)).containsKey(readchar)) {
							delta.get(Q.get(from)).put(readchar, new ArrayList<String>());
						}
						delta.get(Q.get(from)).get(readchar).add(Q.get(to));
					}
				}
				else {
					if(!delta.get(Q.get(from)).containsKey("e")) {
						delta.get(Q.get(from)).put("e", new ArrayList<String>());
					}
					delta.get(Q.get(from)).get("e").add(Q.get(to));
				}
			}
		}
		
		//Fill the empty HashSets and ArrayLists in delta to prevent null pointers
		Iterator<String> itStates = Q.values().iterator();
		while(itStates.hasNext()) {
			Iterator<String> itAlphabet = sigma.iterator();
			String state = itStates.next();
			if(!delta.containsKey(state)) {
				delta.put(state, new HashMap<String,ArrayList<String>>());
			}
			while(itAlphabet.hasNext()) {
				String alphabet = itAlphabet.next();
				if(!delta.get(state).containsKey(alphabet)) {
					delta.get(state).put(alphabet, new ArrayList<String>());
				}
			}
		}
		
		if(debug) {
			System.out.println("File: "+file);
			System.out.println("Q: "+new HashSet<String>(Q.values()));
			System.out.println("Sigma: "+toCSVStr(sigma));
			System.out.println("Delta: "+deltaToString(delta));
			System.out.println("qStart: "+qStart);
			System.out.println("F: "+toCSVStr(F)+"\n");
		}
		
		return new NFA(new HashSet<String>(Q.values()),sigma, delta,qStart,F,qStart, Qx, Qy);
	}
	
	/**
	 * Reads a JFF file to a NFA object and preserves x/y coordinate info
	 * @param fa
	 * @param file
	 * @param debug
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public static void writeJFF(NFA fa, String file, boolean debug) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
		//Top of the XML file
		Document doc = dBuilder.newDocument();
		Element structure = doc.createElement("structure");
		doc.appendChild(structure);
		Element type = doc.createElement("type");
		type.appendChild(doc.createTextNode("fa"));
		structure.appendChild(type);
		Element automaton = doc.createElement("automaton");
		structure.appendChild(automaton);
		Iterator<String> qIt = fa.getQ().iterator();
		
		//If no x, y coords available, distribute states into a grid
		int sqRootNumStates = (int)Math.sqrt(fa.getQ().size());
		int i=0;
		int xCoord = 0;
		int yCoord = 1;
		int multiplier = 100;
		
		//Calculate the x offset for the B NFA
		int bxOffset = 0;
		if(fa.getQxOffset() != null) {
			Iterator<String> offIt = fa.getQxOffset().values().iterator();
			while(offIt.hasNext()) {
				String offset = offIt.next();
				int offsetInt = Math.round(Float.parseFloat(offset));
				//System.out.println("Offset: "+offsetInt);
				if(offsetInt > bxOffset) {
					bxOffset = offsetInt;
				}
			}
			bxOffset += 100;
		}
		
		//Construct JFF file from scratch
		HashMap<String,Integer> stateNums = new HashMap<>();
		while(qIt.hasNext()) {
			String stateStr = qIt.next();
			Element state = doc.createElement("state");
			Attr stateID = doc.createAttribute("id");
			stateID.setValue(""+i);
			state.setAttributeNode(stateID);
			automaton.appendChild(state);
			Attr stateName = doc.createAttribute("name");
			stateName.setValue(stateStr);
			state.setAttributeNode(stateName);
			if(xCoord > sqRootNumStates) {
				xCoord = 0;
				yCoord++;
			}
			
			xCoord++;
			int xPos = xCoord*multiplier;
			int yPos = yCoord*multiplier;
			if(bxOffset != 0 && fa.getQxOffset().containsKey(stateStr) && fa.getQyOffset().containsKey(stateStr)) {
				xPos = Math.round(Float.parseFloat(fa.getQxOffset().get(stateStr)));
				if(stateStr.contains("_B")) {
					xPos += bxOffset;
				}
				yPos = Math.round(Float.parseFloat(fa.getQyOffset().get(stateStr)));
				//System.out.println("X: "+xPos+" Y: "+yPos);
			}
			Element x = doc.createElement("x");
			x.appendChild(doc.createTextNode(xPos+".0"));
			state.appendChild(x);
			Element y = doc.createElement("y");
			y.appendChild(doc.createTextNode(yPos+".0"));
			state.appendChild(y);
			if(fa.getF().contains(stateStr)) {
				Element fin = doc.createElement("final");
				state.appendChild(fin);
			}
			if(fa.getqStart().equals(stateStr)) {
				Element initial = doc.createElement("initial");
				state.appendChild(initial);
			}
			stateNums.put(stateStr, i++);
		}
		
		//Iterate through each transition and add manually
		Iterator<String> itStates = fa.getQ().iterator();
		i=0;
		while(itStates.hasNext()) {
			Iterator<String> itAlphabet = fa.getSigma().iterator();
			String state = itStates.next();
			while(itAlphabet.hasNext()) {
				String alphabet = itAlphabet.next();
				if(fa.getDelta().get(state).containsKey(alphabet)) {
					for(String nextState : fa.getDelta().get(state).get(alphabet)) {
						Element transition = doc.createElement("transition");
						automaton.appendChild(transition);
						Element from = doc.createElement("from");
						transition.appendChild(from);
						from.appendChild(doc.createTextNode(""+i));
						Element to = doc.createElement("to");
						transition.appendChild(to);
						to.appendChild(doc.createTextNode(stateNums.get(nextState)+""));
						Element read = doc.createElement("read");
						transition.appendChild(read);
						read.appendChild(doc.createTextNode(alphabet));
					}
				}
			}
			i++;
		}
		
		//Write to XML file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(file));
		transformer.transform(source, result);
	}
	
	/**
	 * Write a NFA to a .nfa file, which is in the format of a formal description
	 * @param fa
	 * @param file
	 */
	public static void write(NFA fa, String file, boolean debug) {
		String contents = "";
		String[] Q = new String[fa.getQ().size()];
		fa.getQ().toArray(Q);
		contents += toCSVStr(Q)+"\n";
		
		String[] sigma = new String[fa.getSigma().size()];
		fa.getSigma().toArray(sigma);
		contents += toCSVStr(sigma)+"\n";
		
		//Insert transitions
		String delta = deltaToString(fa.getDelta());
		
		contents += delta+"\n";
			//HashMap<String,HashMap<String,ArrayList<String>>>
		contents += fa.getqStart()+"\n";
		
		String[] F = new String[fa.getF().size()];
		fa.getF().toArray(F);
		
		contents += toCSVStr(F);
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(file);
			writer.print(contents);
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(debug) {
			System.out.println("File: "+file);
			System.out.println("Q: "+toCSVStr(Q));
			System.out.println("Sigma: "+toCSVStr(sigma));
			System.out.println("Delta: "+delta);
			System.out.println("qStart: "+fa.getqStart());
			System.out.println("F: "+toCSVStr(F)+"\n");
		}
	}
	
	/**
	 * A method for easily printing the delta data structure for debugging or logging
	 * @param d
	 * @return
	 */
	private static String deltaToString(HashMap<String,HashMap<String,ArrayList<String>>> d) {
		String delta = "";
		delta += "[";
		for(String state : d.keySet()) {
			HashMap<String,ArrayList<String>> trans = d.get(state);
			delta += "[";
			for(String letter : trans.keySet()) {
				delta += "["+toCSVStr(trans.get(letter))+"]";
			}
			delta += "]";
		}
		delta += "]";
		return delta;
	}
	
	/**
	 * Convert a String array into a csv
	 * @param arr
	 * @return
	 */
	private static String toCSVStr(String[] arr) {
		String rtn = "";
		for(int i=0;i<arr.length-1;i++) {
			rtn += arr[i]+",";
		}
		rtn += arr[arr.length-1];
		return rtn;
	}
	
	/**
	 * Convert an ArrayList of Strings array into a csv
	 * @param arr
	 * @return
	 */
	private static String toCSVStr(ArrayList<String> arr) {
		if(arr.size() == 0) {
			return "";
		}
		else if(arr.size() == 1) {
			return arr.get(0);
		}
		else
		{
			String rtn = "";
			for(int i=0;i<arr.size()-1;i++) {
				rtn += arr.get(i)+",";
			}
			rtn += arr.get(arr.size()-1);
			return rtn;
		}
	}
	
	/**
	 * Convert a HashSet into a csv
	 * @param set
	 * @return
	 */
	private static String toCSVStr(HashSet<String> set) {
		String[] newArr = set.toArray(new String[set.size()]);
		return toCSVStr(newArr);
	}

	/**
	 * A method that builds a nested list recursively with a limit defined by the layer size
	 * @param nestedList
	 * @param layer
	 * @return
	 */
	private static ArrayList<Object> buildList(String nestedList,int layer) {
		ArrayList<Object> list = new ArrayList<>();
		//Check for empty list
		if(layer < 0 || nestedList == null || nestedList.equals("")) {
			return list;
		}
		
		//Look for closing brackets if opening brackets are seen
		if(nestedList.charAt(0) == '[') {
			int openBr = 1;
			int openBrInd = nestedList.indexOf("[");
			int i=openBrInd+1;
			while(openBr != 0 && i<nestedList.length()) {
				if(nestedList.charAt(i) == '[') {
					openBr++;
				}
				if(nestedList.charAt(i) == ']') {
					openBr--;
				}
				i++;
			}
			
			if(openBr == 0) {
				//System.out.println("nested list: "+nestedList+"\nopen bracket ind: "+openBrInd+"\nclose bracket ind: "+i+"\nnext list: "+nestedList.substring(openBrInd+1,i-1));
				if(openBrInd+1 == i-1) {
					list.add(new ArrayList<>());
				}
				else {
					ArrayList<Object> nestedArrayList = buildList(nestedList.substring(openBrInd+1,i-1),layer-1);
					list.add(nestedArrayList);
				}
				if(nestedList.length() > i) {
					//System.out.println("rest: "+nestedList.substring(i+1));
					ArrayList<Object> rest = buildList(nestedList.substring(i+1),layer);
					list.addAll(rest);
				}
			}
		}
		
		//If there are no brackets, this is a simple list
		if(!nestedList.contains("[")) {
			list.addAll(Arrays.asList(nestedList.split(",")));
		}
		return list;
	}
}
