import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

/**
 * @author donaldsa18
 *
 */
public class FATransformer {
	
	//Generates a new NFA with the concatenation of A and B
	public static NFA concat(NFA a, NFA b) {
		HashMap<String,String> aTrans = new HashMap<>();
		HashSet<String> QaUnique = makeUnique(a.getQ(),b.getQ(),"_A",aTrans);
		HashSet<String> FaUnique = makeUnique(a.getF(),aTrans);
		HashMap<String,HashMap<String,ArrayList<String>>> DeltaaUnique = makeUnique(a.getDelta(),aTrans);
		String qStarta = a.getqStart();
		if(aTrans.containsKey(qStarta)) {
			qStarta = aTrans.get(qStarta);
		}
		
		HashMap<String,String> bTrans = new HashMap<>();
		HashSet<String> QbUnique = makeUnique(b.getQ(),a.getQ(),"_B",bTrans);
		//System.out.println("Making B's F unique");
		HashSet<String> FbUnique = makeUnique(b.getF(),bTrans);
		HashMap<String,HashMap<String,ArrayList<String>>> DeltabUnique = makeUnique(b.getDelta(),bTrans);
		String qStartb = b.getqStart();
		if(bTrans.containsKey(qStartb)) {
			qStartb = bTrans.get(qStartb);
		}
		
		HashSet<String> newSigma = new HashSet<String>(a.getSigma());
		newSigma.addAll(b.getSigma());
		HashSet<String> newQ = new HashSet<String>(QaUnique);
		newQ.addAll(QbUnique);
		HashMap<String,HashMap<String,ArrayList<String>>> newDelta = new HashMap<>(DeltaaUnique);
		newDelta.putAll(DeltabUnique);
		Iterator<String> it = FaUnique.iterator();
		while(it.hasNext()) {
			String state = it.next();
			if(newDelta.get(state).containsKey("")) {
				newDelta.get(state).get("").add(qStartb);
			}
			else {
				ArrayList<String> newArr = new ArrayList<>();
				newArr.add(qStartb);
				newDelta.get(state).put("", newArr);
			}
		}
		if(a.getQxOffset() != null && b.getQxOffset() != null) {
			HashMap<String,String> QxOffsetUnique = makeUniqueCoords(a.getQxOffset(),aTrans);
			QxOffsetUnique.putAll(makeUniqueCoords(b.getQxOffset(),bTrans));
			HashMap<String,String> QyOffsetUnique = makeUniqueCoords(a.getQyOffset(),aTrans);
			QyOffsetUnique.putAll(makeUniqueCoords(b.getQyOffset(),bTrans));
			return new NFA(newQ,newSigma,newDelta,qStarta,FbUnique,qStarta,QxOffsetUnique,QyOffsetUnique);
		}
		return new NFA(newQ,newSigma,newDelta,qStarta,FbUnique,qStarta);
	}
	
	private static HashMap<String,HashMap<String,ArrayList<String>>> makeUnique(HashMap<String,HashMap<String,ArrayList<String>>> orig, HashMap<String,String> transitions) {
		HashMap<String,HashMap<String,ArrayList<String>>> unique = new HashMap<>(orig);
		Iterator<Entry<String,String>> it = transitions.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,String> entry = it.next();
			HashMap<String,ArrayList<String>> val = unique.get(entry.getKey());
			unique.remove(entry.getKey());
			unique.put(entry.getValue(),val);
		}
		Iterator<Entry<String,HashMap<String,ArrayList<String>>>> it1 = unique.entrySet().iterator();
		while(it1.hasNext()) {
			HashMap<String, ArrayList<String>> ent = it1.next().getValue();
			if(ent != null) {
				Iterator<Entry<String,ArrayList<String>>> it2 = ent.entrySet().iterator();
				while(it2.hasNext()) {
					Entry<String,ArrayList<String>> entry = it2.next();
					Iterator<String> it3 = entry.getValue().iterator();
					while(it3.hasNext()) {
						String state = it3.next();
						if(transitions.containsKey(state)) {
							entry.getValue().set(entry.getValue().indexOf(state), transitions.get(state));
						}
					}
				}
			}
		}
		return unique;
	}
	
	private static HashSet<String> makeUnique(HashSet<String> orig, HashMap<String,String> transitions) {
		HashSet<String> unique = new HashSet<String>(orig);
		Iterator<Entry<String,String>> it = transitions.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,String> entry = it.next();
			if(orig.contains(entry.getKey())) {
				//System.out.println("Replacing "+entry.getKey()+" with "+entry.getValue());
				unique.remove(entry.getKey());
				unique.add(entry.getValue());
			}
		}
		return unique;
	}
	
	private static HashMap<String,String> makeUniqueCoords(HashMap<String,String> orig, HashMap<String,String> transitions) {
		HashMap<String,String> unique = new HashMap<String,String>(orig);
		Iterator<Entry<String,String>> it = transitions.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,String> entry = it.next();
			if(orig.containsKey(entry.getKey())) {
				String temp = orig.get(entry.getKey());
				unique.remove(entry.getKey());
				unique.put(entry.getValue(), temp);
				//System.out.println("Replacing "+entry.getKey()+" with "+entry.getValue()+" and keeping temp value "+temp);
			}
		}
		return unique;
	}
	
	private static HashSet<String> makeUnique(HashSet<String> orig, HashSet<String> other, String id, HashMap<String,String> transitions) {
		HashSet<String> origIntersect = new HashSet<String>(orig);
		origIntersect.retainAll(other);
		Iterator<String> it = origIntersect.iterator();
		HashSet<String> origUnique = new HashSet<String>(orig);
		Random rand = new Random();
		final String alphabet = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
		while(it.hasNext()) {
			String state = it.next();
			origUnique.remove(state);
			String newstate = state+id;

			while(orig.contains(newstate) || other.contains(newstate)) {
				newstate += alphabet.charAt(rand.nextInt(alphabet.length()));
			}
			origUnique.add(newstate);
			
			transitions.put(state, newstate);
		}
		return origUnique;
	}
}
