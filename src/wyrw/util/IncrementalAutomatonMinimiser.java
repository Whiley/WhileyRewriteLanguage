package wyrw.util;

import java.util.ArrayList;

import wyautl.core.Automaton;

/**
 * <p>
 * Responsible for efficiently ensuring an automaton remains properly minimised
 * and compacted after a rewrite operation has occurred. Rewrites can result in
 * the presence of multiple equivalent states, though not always. For example,
 * consider this automaton:
 * </p>
 * 
 * <pre>
 *     Or
 *    /  \
 *  And  And
 *   |    |
 *  "X"  "Y"
 * </pre>
 * 
 * <p>
 * Now, suppose we rewrite state "X" to "Y", then their parent nodes become
 * equivalent and should be merged. The purpose of this algorithm is to do this
 * efficiently without traversing the entire automaton.
 * </p>
 * 
 * <p>
 * The algorithm works by firstly maintaining the set of "parents" for each
 * state. That is the set of states for which this state is a direct child.
 * Since automata are directed graphs states can multiple parents (i.e. unlike
 * trees, where each node has exactly one parent). Since we expect relatively
 * few parents for each state, an array is used for this purpose.
 * </p>
 * <p>
 * Given knowledge of the parents for each state, the algorithm can now
 * efficiently determine the "cone" of states which could be equivalent after a
 * rewrite. Specifically, the parents of the states involved in the rewrite are
 * candidates for equivalence. Furthermore, if they are equivalent then their
 * parents are candidates for being equivalent and so on, until we fail to find
 * an equivalent pair of parents or we reach a root state.
 * </p>
 * <p>
 * <b>NOTE</b>: this algorithm should eventually be properly integrated with the
 * Automaton data structure. At this stage, I have avoided doing this simply
 * because of the work involved in doing it.
 * </p>
 * 
 * @author David J. Pearce
 *
 */
public class IncrementalAutomatonMinimiser {
	/**
	 * The automaton for which the incremental information is being maintained.
	 */
	private final Automaton automaton;
	
	/**
	 * This represents the set of parents for each state in the automaton. This
	 * is a list because we will need to expand it as the automaton increases in
	 * size (which it likely will as rewrites occur).
	 */
	private final ArrayList<int[]> parents;
	
	public IncrementalAutomatonMinimiser(Automaton automaton) {
		this.automaton = automaton;
		this.parents = determineParents(automaton);
	}
	
	public void rewrite(int from, int to) {
		
	}
	
	public void substitute(int source, int from, int to) {
		
	}
	
	/**
	 * Compute the parents for each state in the automaton from scratch. This is
	 * done using several linear traversals over the states to minimise the
	 * amount of memory churn and ensure linear time.
	 * 
	 * @param automaton
	 * @return
	 */
	private static ArrayList<int[]> determineParents(Automaton automaton) {
		int[] counts = new int[automaton.nStates()];
		// first, visit all nodes
		for (int i = 0; i != automaton.nStates(); ++i) {
			updateParentCounts(automaton.get(i),i,counts);
		}
		//
		ArrayList<int[]> parents = new ArrayList<int[]>();
		for (int i = 0; i != automaton.nStates(); ++i) {
			parents.set(i, new int[counts[i]]);
		}
		//
		for (int i = 0; i != automaton.nStates(); ++i) {
			updateParents(automaton.get(i),i,counts,parents);
		}
		//
		return parents;
	}
	
	/**
	 * Update the parent count for each child (if any) of the given state.
	 * 
	 * @param state
	 *            --- The state whose children we are interested in.
	 * @param parent
	 *            --- The index of the state whose children we are interested
	 *            in.
	 * @param counts
	 *            --- The array of parent counts for each state.
	 */
	private static void updateParentCounts(Automaton.State state, int parent, int[] counts) {
		switch(state.kind) {
		case Automaton.K_BOOL:
		case Automaton.K_INT:
		case Automaton.K_REAL:
		case Automaton.K_STRING:
			return;		
		case Automaton.K_LIST:
		case Automaton.K_SET:
		case Automaton.K_BAG: {
			Automaton.Collection c = (Automaton.Collection) state;
			for(int i=0;i!=c.size();++i) {
				int child = c.get(i);
				counts[child]++;				
			}
		}
		default:
			// terms
			Automaton.Term t = (Automaton.Term) state;
			int child = t.contents;
			if(child != Automaton.K_VOID) {
				counts[child]++;
			}
		}	
	}
	
	/**
	 * Update the parents for each child (if any) of the given state.
	 * 
	 * @param state
	 *            --- The state whose children we are interested in.
	 * @param parent
	 *            --- The index of the state whose children we are interested
	 *            in.
	 * @param counts
	 *            --- The array of parent counts for each state.
	 * @param parents
	 *            --- The list of parents for each state.
	 */
	private static void updateParents(Automaton.State state, int parent, int[] counts, ArrayList<int[]> parents) {
		switch(state.kind) {
		case Automaton.K_BOOL:
		case Automaton.K_INT:
		case Automaton.K_REAL:
		case Automaton.K_STRING:
			return;		
		case Automaton.K_LIST:
		case Automaton.K_SET:
		case Automaton.K_BAG: {
			Automaton.Collection c = (Automaton.Collection) state;
			for(int i=0;i!=c.size();++i) {
				updateParents(parent,c.get(i),counts,parents);
			}
		}
		default:
			// terms
			Automaton.Term t = (Automaton.Term) state;
			int child = t.contents;
			if(child != Automaton.K_VOID) {
				updateParents(parent,child,counts,parents);
			}
		}		
	}
	
	private static void updateParents(int parent, int child, int[] counts,
			ArrayList<int[]> parents) {
		int index = counts[child] - 1;
		counts[child] = index;
		int[] childParents = parents.get(child);
		childParents[index] = parent;
	}	
}
