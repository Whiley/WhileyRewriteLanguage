package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;

import wyautl.core.Automaton;
import wyautl.util.BinaryMatrix;

import static wyautl.core.Automata.*;

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
	private final ArrayList<ParentInfo> parents;
	
	public IncrementalAutomatonMinimiser(Automaton automaton) {
		this.automaton = automaton;
		this.parents = determineParents(automaton);
	}
	
	/**
	 * <p>
	 * Update the automaton after a successful rewrite has occurred. The goal
	 * here is to ensure: 1) the automaton remains in minimised form; 2) that
	 * all unreachable states are nullified; 3) that the parents information is
	 * up-to-date.
	 * </p>
	 * <p>
	 * To ensure the automaton remains in minimised form, we need to collapse
	 * any states which have been rendered equivalent by the rewrite. By
	 * definition, these states must be parents of the two states involved in
	 * the rewrite. There are two cases to consider. If the rewrite is to a
	 * fresh state, then we are guaranteed that no equivalent states are
	 * generated. On the otherhand, if the rewrite is to an existing state, then
	 * there is potential for equivalent states to arise. In both cases, we must
	 * first update the parents of those states involved.
	 * </p>
	 * 
	 * @param from
	 * @param to
	 */
	public void rewrite(int from, int to) {
		// First, update parent information
		ParentInfo fromParents = parents.get(from);
		eliminateUnreachableState(from);
		addAllParents(to,fromParents);
		// Second, collapse any equivalent vertices
		collapseEquivalentParents(from, to, fromParents);
		
		// NOTE: what about fresh states added which were immediately
		// unreachable. For example, they were added to implement a check. We
		// could eliminate these by compacting "above the pivot".
	}
	
	public void substitute(int source, int from, int to) {
		
	}
	
	/**
	 * <p>
	 * The given state has become unreachable. Therefore, we need to recursively
	 * eliminate any children of this state which are also eliminated as a
	 * result. To do this, we remove this state from the parents set of its
	 * children. If any of those children now have an empty set of parents, then
	 * we recursively eliminate them as well
	 * </p>
	 * <p>
	 * <b>NOTE:</b> The major problem with this algorithm is, likely many
	 * garbage collection algorithms, that it does not guarantee to eliminate
	 * all unreachable states. In particular, no state involved in a cycle will
	 * be reclaimed as it will always have at least one parent. <i>At this
	 * stage, it remains to determine how best to resolve this problem. One
	 * solution maybe to record the "dominator" for each state. That way, you
	 * could tell whether a state which was unreachable dominated a child and,
	 * hence, it too was unreachable.</i>
	 * </p>
	 * 
	 * @param index
	 *            Index of the state in question.
	 */
	private void eliminateUnreachableState(int index) {
		
		// FIXME: figure out solution for cycles (see above).
		
		Automaton.State state = automaton.get(index);
		// First, check whether state already removed
		if (state != null) {
			// Second, physically remove the state in question
			automaton.set(index, null);
			parents.set(index, null);
			// Third, update parental information for any children
			switch (state.kind) {
			case Automaton.K_BOOL:
			case Automaton.K_INT:
			case Automaton.K_REAL:
			case Automaton.K_STRING:
				// no children
				return;
			case Automaton.K_LIST:
			case Automaton.K_SET:
			case Automaton.K_BAG: {
				// lots of children :)
				Automaton.Collection c = (Automaton.Collection) state;
				for (int i = 0; i != c.size(); ++i) {
					int child = c.get(i);
					ParentInfo pinfo = parents.get(child);
					pinfo.remove(index);
					if (pinfo.size() == 0) {
						// this state is now unreachable as well
						eliminateUnreachableState(child);
					}
				}
				return;
			}
			default:
				// terms
				Automaton.Term t = (Automaton.Term) state;
				int child = t.contents;
				if(child != Automaton.K_VOID) {
					ParentInfo pinfo = parents.get(child);
					pinfo.remove(index);
					if (pinfo.size() == 0) {
						// this state is now unreachable as well
						eliminateUnreachableState(child);
					}
				}
			}
		}
	}
		
	/**
	 * <p>
	 * Add all parents from another state to a given state (which may
	 * potentially be fresh). For a fresh state, there may be one or more fresh
	 * children who need to have their parent sets initialised as well. In such
	 * case, we recursively traverse them initialising their parent sets.
	 * </p>
	 * 
	 * @param child
	 *            --- state for which we are updating the parent information.
	 * @param parent
	 *            --- single parent for the child in question
	 */
	private void addAllParents(int child, ParentInfo allParents) {
		ParentInfo pinfo = parents.get(child);
		if(pinfo == null) {
			// This is a fresh state
			pinfo = new ParentInfo(allParents.size);	
			parents.set(child, pinfo);
			Automaton.State state = automaton.get(child);
			//
			switch (state.kind) {
			case Automaton.K_BOOL:
			case Automaton.K_INT:
			case Automaton.K_REAL:
			case Automaton.K_STRING:
				// no children
				break;
			case Automaton.K_LIST:
			case Automaton.K_SET:
			case Automaton.K_BAG: {
				// lots of children :)
				Automaton.Collection c = (Automaton.Collection) state;
				for (int i = 0; i != c.size(); ++i) {
					addParent(child,c.get(i));
				}
				break;
			}
			default:
				// terms
				Automaton.Term t = (Automaton.Term) state;
				int grandChild = t.contents;
				if(grandChild != Automaton.K_VOID) {
					addParent(child,grandChild);
				}
			}
		}		
		pinfo.addAll(allParents);
	}
	
	/**
	 * <p>
	 * Add a new parent to a given state (which may potentially be fresh). For a
	 * fresh state, there may be one or more fresh children who need to have
	 * their parent sets initialised as well. In such case, we recursively
	 * traverse them initialising their parent sets.
	 * </p>
	 * 
	 * @param child
	 *            --- state for which we are updating the parent information.
	 * @param parent
	 *            --- single parent for the child in question
	 */

	private void addParent(int child, int parent) {
		ParentInfo pinfo = parents.get(child);
		if(pinfo == null) {
			// This is a fresh state
			pinfo = new ParentInfo(1);	
			parents.set(child, pinfo);
			Automaton.State state = automaton.get(child);
			//
			switch (state.kind) {
			case Automaton.K_BOOL:
			case Automaton.K_INT:
			case Automaton.K_REAL:
			case Automaton.K_STRING:
				// no children
				break;
			case Automaton.K_LIST:
			case Automaton.K_SET:
			case Automaton.K_BAG: {
				// lots of children :)
				Automaton.Collection c = (Automaton.Collection) state;
				for (int i = 0; i != c.size(); ++i) {
					addParent(child,c.get(i));
				}
				break;
			}
			default:
				// terms
				Automaton.Term t = (Automaton.Term) state;
				int grandChild = t.contents;
				if(grandChild != Automaton.K_VOID) {
					addParent(child,grandChild);
				}
			}
		}		
		pinfo.add(parent);
	}
	
	/**
	 * 
	 * @param from
	 * @param to
	 * @param fromParents
	 */
	private void collapseEquivalentParents(int from, int to, ParentInfo fromParents) {
		ParentInfo toParents = parents.get(to);
		// FIXME: the following operations are linear (or worse) in the size of the
		// automaton. Therefore, we want to eliminate this by using a more compact representation.
		IntStack worklist = new IntStack(fromParents.size * toParents.size);
		BinaryMatrix equivs = initialiseEquivalences();
		equivs.set(from, to, true);
				
		// First, determine all the equivalent parents (if any)
		addCandidatesToWorklist(worklist,equivs,fromParents,toParents);
		
		// Second, iterate until all equivalences are determined
		while(worklist.size > 0) {
			to = worklist.pop();
			from = worklist.pop();
			if(!equivs.get(from, to) && equivalent(automaton,equivs,from,to)) {
				equivs.set(from, to, true);
				addCandidatesToWorklist(worklist,equivs,parents.get(from),parents.get(to));
			}			
		}
		
		// Third, collapse equivalent states
		
	}
	
	private BinaryMatrix initialiseEquivalences() {
		BinaryMatrix equivs = new BinaryMatrix(automaton.nStates(),automaton.nStates(),false);
		for(int i=0;i!=automaton.nStates();++i) {
			equivs.set(i, i, true);
		}
		return equivs;
	}
	
	private void addCandidatesToWorklist(IntStack worklist, BinaryMatrix equivs, ParentInfo fromParents, ParentInfo toParents) {
		int[] from_parents = fromParents.parents;
		int[] to_parents = toParents.parents;
		for(int i=0;i!=fromParents.size;++i) {
			int from_parent = from_parents[i];
			Automaton.State from_state = automaton.get(from_parent);
			for(int j=0;j!=toParents.size;++j) {
				int to_parent = to_parents[j];
				Automaton.State to_state = automaton.get(to_parent);
				if (!equivs.get(from_parent, to_parent) && from_state.kind == to_state.kind) {
					// Only add candidates which could actually be the same.
					// This is a simple optimisation which should reduce work
					// considerably.
					worklist.push(from_parent);
					worklist.push(to_parent);
				}
			}	
		}
	}
	
	
	/**
	 * Compute the parents for each state in the automaton from scratch. This is
	 * done using several linear traversals over the states to minimise the
	 * amount of memory churn and ensure linear time.
	 * 
	 * @param automaton
	 * @return
	 */
	private static ArrayList<ParentInfo> determineParents(Automaton automaton) {
		int[] counts = new int[automaton.nStates()];
		// first, visit all nodes
		for (int i = 0; i != automaton.nStates(); ++i) {
			updateParentCounts(automaton.get(i),i,counts);
		}
		//
		ArrayList<ParentInfo> parents = new ArrayList<ParentInfo>();
		for (int i = 0; i != automaton.nStates(); ++i) {
			parents.set(i, new ParentInfo(counts[i]));
		}
		//
		for (int i = 0; i != automaton.nStates(); ++i) {
			updateParents(automaton.get(i),i,parents);
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
			break;
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
	private static void updateParents(Automaton.State state, int parent, ArrayList<ParentInfo> parents) {
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
				parents.get(child).add(parent);
			}
			break;
		}
		default:
			// terms
			Automaton.Term t = (Automaton.Term) state;
			int child = t.contents;
			if(child != Automaton.K_VOID) {
				parents.get(child).add(parent);
			}
		}		
	}
	
	/**
	 * A simple data structure for representing the parent information. This
	 * could be made more interesting, for example, by using a sorted array. Or,
	 * perhaps, a compressed bitset.
	 * 
	 * @author David J. Pearce
	 *
	 */
	private static final class ParentInfo {
		private int[] parents;
		private int size;
		
		public ParentInfo(int capacity) {
			this.parents = new int[capacity];
			this.size = 0;
		}
		
		public int size() {
			return size;
		}
		
		public int get(int index) {
			return parents[index];
		}
		
		public void add(int parent) {
			int index = indexOf(parents,parent);
			if(index == -1) {
				ensureCapacity((size+1)*1);
				parents[size++] = parent;
			}
		}
		
		public void addAll(ParentInfo pinfo) {
			int pinfo_size = pinfo.size;
			ensureCapacity(size+pinfo_size);
			for(int i=0;i!=pinfo_size;++i) {
				int parent = pinfo.parents[i];
				if(indexOf(parents,parent) == -1) {
					parents[size++] = parent;
				}
			}
		}
		
		public void remove(int parent) {
			int index = indexOf(parents,parent);
			if(index != -1) {
				System.arraycopy(parents, index+1, parents, index, size - (index+1));
				size = size - 1;
			}
		}
		
		private void ensureCapacity(int capacity) {
			if(parents.length < capacity) {
				parents = Arrays.copyOf(parents, capacity);
			}
		}
		
		private static int indexOf(int[] array, int element) {
			for(int i=0;i!=array.length;++i) {
				if(array[i] == element) {
					return i;
				}
			}
			return -1;
		}
	}
}
