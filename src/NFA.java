import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A class containing NFA 5-tuple information, a current state, and optionally x/y coordinates for each state.
 * @author donaldsa18
 *
 */
public class NFA {
	private HashSet<String> Q;
	private HashSet<String> sigma;
	private HashMap<String,HashMap<String,ArrayList<String>>> delta;
	private String qStart;
	private HashSet<String> F;
	private String state;
	private boolean accepted;
	private HashMap<String,String> QxOffset;
	private HashMap<String,String> QyOffset;
	
	/**
	 * A constructor for NFAs accepting ArrayLists
	 * @param states
	 * @param alphabet
	 * @param transitions
	 * @param start
	 * @param acceptingStates
	 */
	public NFA(ArrayList<String> states,ArrayList<String> alphabet, ArrayList<ArrayList<ArrayList<String>>> transitions, String start, ArrayList<String> acceptingStates) {
		Q = new HashSet<String>(states);
		sigma = new HashSet<String>(alphabet);
		delta = new HashMap<String,HashMap<String,ArrayList<String>>>();
		
		//Iterate through the states x sigma to construct the Hashtable
		for(int i=0;i<states.size();i++) {
			ArrayList<ArrayList<String>> transition = transitions.get(i);
			HashMap<String,ArrayList<String>> transHash = new HashMap<>();
			for(int j=0;j<alphabet.size();j++) {
				if(transition != null && i < transition.size()) {
					transHash.put(alphabet.get(i),transition.get(i));
				}
			}
			delta.put(states.get(i), transHash);
		}
		qStart = start;
		F = new HashSet<String>(acceptingStates);
		state = start;
		if(F.contains(state)) {
			accepted = true;
		}
		QxOffset = null;
		QyOffset = null;
	}
	
	/**
	 * Constructor for duplicating a NFA without state coordinates.
	 * @param states
	 * @param alphabet
	 * @param transitions
	 * @param start
	 * @param acceptingStates
	 * @param currState
	 */
	public NFA(HashSet<String> states, HashSet<String> alphabet, HashMap<String,HashMap<String,ArrayList<String>>> transitions, String start, HashSet<String> acceptingStates, String currState) {
		Q = states;
		sigma = alphabet;
		delta = transitions;
		qStart = start;
		F = acceptingStates;
		state = currState;
		if(F.contains(state)) {
			accepted = true;
		}
		QxOffset = null;
		QyOffset = null;
	}
	
	/**
	 * Constructor for duplicating a NFA with state coordinates.
	 * @param states
	 * @param alphabet
	 * @param transitions
	 * @param start
	 * @param acceptingStates
	 * @param currState
	 * @param Qx
	 * @param Qy
	 */
	public NFA(HashSet<String> states, HashSet<String> alphabet, HashMap<String,HashMap<String,ArrayList<String>>> transitions, String start, HashSet<String> acceptingStates, String currState, HashMap<String,String> Qx, HashMap<String,String> Qy) {
		Q = states;
		sigma = alphabet;
		delta = transitions;
		qStart = start;
		F = acceptingStates;
		state = currState;
		if(F.contains(state)) {
			accepted = true;
		}
		QxOffset = Qx;
		QyOffset = Qy;
	}
	
	/**
	 * Reads an input character and does the transition. If there are multiple transitions, it returns a list
	 * of new NFAs. If there are no transitions, it the state is set to null, which means it rejects.
	 * @param input character
	 * @return ArrayList<NFA> containing all possible states
	 */
	public ArrayList<NFA> read(String input) {
		ArrayList<NFA> possNFAs = new ArrayList<>();
		for(int i=0;i<2;i++) {
			if(!accepted) {
				ArrayList<String> nextStates = delta.get(state).get(input);
				if(nextStates.size() == 0) {
					possNFAs.add(new NFA(Q,sigma,delta,qStart,F,null,QxOffset,QyOffset));
				}
				else if(nextStates.size() == 1) {
					possNFAs.add(new NFA(Q,sigma,delta,qStart,F,nextStates.get(0),QxOffset,QyOffset));
				}
				else {
					for(int j=0;j<nextStates.size();j++) {
						possNFAs.add(new NFA(Q,sigma,delta,qStart,F,nextStates.get(j),QxOffset,QyOffset));
					}
				}
			}
			else {
				possNFAs.add(this);
			}
			input = "";//Check epsilon transitions
		}
		System.out.println("At "+state+" and going to: ");
		for(NFA poss: possNFAs) {
			System.out.print(poss.state+",");
		}
		return possNFAs;
	}
	
	//Various getters for the properties of a NFA and the x/y coordinates of the states if applicable
	public HashSet<String> getQ() {
		return Q;
	}
	public HashMap<String, HashMap<String, ArrayList<String>>> getDelta() {
		return delta;
	}
	public HashSet<String> getF() {
		return F;
	}
	public HashSet<String> getSigma() {
		return sigma;
	}
	public String getqStart() {
		return qStart;
	}
	public void reset() {
		state = qStart;
	}
	public boolean isAccepted() {
		return accepted;
	}
	public HashMap<String, String> getQxOffset() {
		return QxOffset;
	}
	public HashMap<String, String> getQyOffset() {
		return QyOffset;
	}
}
