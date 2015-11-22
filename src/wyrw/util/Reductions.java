package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyrw.core.Reduction;
import wyrw.core.ReductionRule;
import wyrw.core.Rewrite;

public class Reductions {
	
	
	public static void minimiseAndReduce(Automaton automaton, int maxSteps, ReductionRule... reductions) {
		minimiseAndReduce(automaton,maxSteps,reductions,null);
	}
	
	public static void minimiseAndReduce(Automaton automaton, int maxSteps, ReductionRule[] reductions,
			Comparator<Rewrite.Activation> comparator) {
		automaton.minimise();
		automaton.compact(0);
		reduceOver(automaton, 0, maxSteps, reductions, comparator);
	}
	
	public static void reduceOver(Automaton automaton, int start, int maxSteps, ReductionRule... reductions) {
		reduceOver(automaton,start,maxSteps,reductions,null);
	}
		
	public static void reduce(Automaton automaton, int maxSteps, ReductionRule... reductions) {
		reduceOver(automaton,0,maxSteps,reductions,null);
	}
	
	/**
	 * Simple helper method for reducing an automaton.  
	 * 
	 * @param automaton
	 */
	public static void reduceOver(Automaton automaton, int start, int maxSteps, ReductionRule[] reductions,
			Comparator<Rewrite.Activation> comparator) {
		// Now, attempt to reduce as much as possible
		Incrementaliser inc = new Incrementaliser(automaton);
		
		boolean changed = true;
		while (changed && maxSteps-- > 0) {
			changed = false;
			AbstractActivation[] activations = probe(automaton, start, reductions, comparator);
			int pivot = automaton.nStates();
			for (int i = 0; i != activations.length; ++i) {
				AbstractActivation activation = activations[i];
				int from = activation.target();
				int target = activation.apply(automaton);
				if (target != Automaton.K_VOID && from != target) {
					// Rewrite applied
					inc.rewrite(from, target);
					//
					changed = true;
					break;
				} else {
					automaton.resize(pivot);
				}
			}
		}
	}
	
	private static AbstractActivation[] probe(Automaton automaton, int start, ReductionRule[] reductions,
			Comparator<Rewrite.Activation> comparator) {
		ArrayList<Reduction.Activation> activations = new ArrayList<Reduction.Activation>();
		for (int s = start; s != automaton.nStates(); ++s) {
			// State is reachable from the given root
			Automaton.State state = automaton.get(s);
			// Check whether this state is a term or not; that's because
			// only
			// terms can be roots for rewrite rule applications.
			if (state instanceof Automaton.Term) {
				for (int r = 0; r != reductions.length; ++r) {
					reductions[r].probe(automaton, s, activations);
				}
			}
		}
		AbstractActivation[] array = activations.toArray(new AbstractActivation[activations.size()]);
		if (comparator != null) {
			Arrays.sort(array, comparator);
		}
		return array;
	}
	
	private static final class Incrementaliser {
		private final Automaton automaton;
		private boolean[] reachable;
		private int[] binding;
		
		/**
		 * Initialise the incrementaliser with a given automaton, which is
		 * presumed to be both compacted and minimised.
		 * 
		 * @param automaton
		 */
		public Incrementaliser(Automaton automaton) {
			this.automaton = automaton;
			this.reachable = new boolean[automaton.nStates()*2];
			this.binding = new int[automaton.nStates()*2];
		}
		
		/**
		 * Responsible for updating the data after a successful rewrite has been
		 * applied to the automaton.
		 * 
		 * @param from
		 * @param to
		 */
		public void rewrite(int from, int to) {			
			update();
			compact(0);
		}

		/**
		 * Update the reachability information associated with the automaton
		 * after some change has occurred. This information is currently
		 * recomputed from scratch, though in principle it could be updated
		 * incrementally.
		 */
		private void update() {

			// TODO: update reachability information incrementally

			if (reachable.length < automaton.nStates()) {
				reachable = new boolean[automaton.nStates() * 2];
				binding = new int[automaton.nStates() * 2];
			} else {
				Arrays.fill(reachable, false);
			}
			// first, visit all nodes
			for (int i = 0; i != automaton.nRoots(); ++i) {
				int root = automaton.getRoot(i);
				if (root >= 0) {
					findReachable(automaton, reachable, root);
				}
			}
		}
		

		private void compact(int pivot) {
			int nStates = automaton.nStates();
			int nRoots = automaton.nRoots();
			
			// First, initialise binding for all states upto start state. This
			// ensure that they are subsequently mapped to themselves.
			for (int i = 0; i < nStates; ++i) {
				binding[i] = i;
			}

			// Second, go through and eliminate all unreachable states and compact
			// the automaton down, whilst updating reachable one oneStepUndo
			// information accordingly.
			int j = pivot;

			for (int i = pivot; i < nStates; ++i) {
				if (reachable[i]) {
					Automaton.State ith = automaton.get(i);
					binding[i] = j;
					reachable[i] = false;
					reachable[j] = true;
					automaton.set(j++, ith);
				}
			}

			if (j < nStates) {			
				// Ok, some compaction actually occurred; therefore follow through
				// and update all states accordingly.
				nStates = j;
				automaton.resize(nStates); // will nullify all deleted states

				// Update mapping and oneStepUndo for *all* states
				for (int i = 0; i != nStates; ++i) {
					Automaton.State state = automaton.get(i);
					if(state != null) {
						state.remap(binding);
					}
				}

				// Update mapping for all roots
				for (int i = 0; i != nRoots; ++i) {
					int root = automaton.getRoot(i);
					if (root >= 0) {
						automaton.setRoot(i, binding[root]);
					}
				}
			}
		}
	}
	/**
	 * Visit all states reachable from a given starting state in the given
	 * automaton. In doing this, states which are visited are marked and,
	 * furthermore, those which are "headers" are additionally identified. A
	 * header state is one which is the target of a back-edge in the directed
	 * graph reachable from the start state.
	 *
	 * @param automaton
	 *            --- automaton to traverse.
	 * @param reachable
	 *            --- states marked with false are those which have not been
	 *            visited.
	 * @param index
	 *            --- state to begin traversal from.
	 * @return
	 */
	public static void findReachable(Automaton automaton, boolean[] reachable,
			int index) {
		if (index < 0) {
			return;
		} else if (reachable[index]) {
			// Already visited, so terminate here
			return;
		} else {
			// Not previously visited, so mark now and traverse any children
			reachable[index] = true;
			Automaton.State state = automaton.get(index);
			if (state instanceof Automaton.Term) {
				Automaton.Term term = (Automaton.Term) state;
				if (term.contents != Automaton.K_VOID) {
					findReachable(automaton, reachable, term.contents);
				}
			} else if (state instanceof Automaton.Collection) {
				Automaton.Collection compound = (Automaton.Collection) state;
				for (int i = 0; i != compound.size(); ++i) {
					findReachable(automaton, reachable, compound.get(i));
				}
			}
		}
	}
}
