package wyautl.util;

import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.*;
import wyautl.rw.*;

public class RevertingBatchRewriter extends AbstractRewriter implements Rewriter {
	
	/**
	 * The maximum number of rewrite steps to apply in one go.
	 */
	private final int maxSteps;
	
	/**
	 * This is used to maintain information about which states in the current
	 * automaton are reachable. This is necessary to ensure that rewrites are
	 * not applied to multiple states more than once (as this can cause infinite
	 * loops).
	 */
	private boolean[] reachable = new boolean[0];

	/**
	 * The oneStepUndo provides a mapping from new automaton states to their
	 * original states during a reduction. Using this map, every unreachable
	 * state can be returned to its original form in "one step". In particular,
	 * the oneStepUndo function maps reachable states above the pivot to
	 * unreachable states below the pivot.
	 */
	private int[] oneStepUndo = new int[0];
	
	public RevertingBatchRewriter(Schema schema, RewriteRule... rules) {
		this(Integer.MAX_VALUE,schema,Activation.RANK_COMPARATOR,rules);
	}
	
	public RevertingBatchRewriter(Schema schema, Comparator<Activation> comparator, RewriteRule... rules) {
		this(Integer.MAX_VALUE,schema,comparator,rules);
	}
	
	public RevertingBatchRewriter(int maxSteps, Schema schema, Comparator<Activation> comparator, RewriteRule... rules) {
		super(schema, comparator, rules);
		this.maxSteps = maxSteps;
	}
	
	/**
	 * Apply a given activation on this state to potentially produce an updated
	 * state.
	 * 
	 * @param activation
	 * @return
	 */
	@Override
	public RewriteStep apply(RewriteState state, int index) {			
		RewriteState before = state;
		Automaton automaton = new Automaton(state.automaton());
		int pivot = automaton.nStates();
		
		reachable = initReachable(automaton,reachable);
		oneStepUndo = initOneStepUndo(automaton,oneStepUndo);
		
		int r;
		int count = 0;
		while (count < maxSteps && (r = select(state)) != -1) {
			if(inplaceRewrite(state.activation(r), automaton, pivot)) {
				// In this case, something changed so we'd better create our new
				// state.
				state = initialise(automaton);
				count = count + 1;
			} else {
				// This is required to cross out any states which don't actually
				// apply, otherwise we end up in an infinite loop reapplying
				// them here.
				state.update(r, new RewriteStep(state,r,state));
			}
		}
		//
		automaton.compact();
		RewriteStep step = new RewriteStep(before, index, state);
		before.update(index, step);
		return step; 
	}
	
	/**
	 * Apply an activation to the automaton in place. That is, the current
	 * automaton is itself updated.
	 * 
	 * @param index
	 * @param automaton
	 * @return
	 */
	private boolean inplaceRewrite(Activation activation, Automaton automaton, int pivot) {
		int from = activation.root();
		int target = activation.apply(automaton);
		RewriteState nextState;

		if (target != Automaton.K_VOID && from != target) {
			// TODO: can we get rid of this?
//			automaton.minimise();
//			automaton.compact();
			// Update reachability status for nodes affected by this
			// activation. This is because such states could cause
			// an infinite loop of re-activations. More specifically, where
			// we activate on a state and rewrite it, but then it remains
			// and so we repeat.
			reachable = initReachable(automaton, reachable);

			// Revert all states below the pivot which are now unreachable.
			// This is essential to ensuring that the automaton will return
			// to its original state iff it is the unchanged. This must be
			// applied before compaction.
			applyUndo(automaton,activation.root(), target, pivot);
			
			// Compact all states above the pivot to eliminate unreachable
			// states and prevent the automaton from growing continually.
			// This is possible because automton.rewrite() can introduce
			// null states into the automaton.
			compact(automaton, pivot, reachable, oneStepUndo);
			
			return true;
		} else {
			// activation did not apply
			return false;
		}
	}
	
	/**
	 * Select the first unvisited state, or return -1 if none exists.
	 * 
	 * @return
	 */
	public int select(RewriteState state) {
		for (int i = 0; i != state.size(); ++i) {
			if (reachable[i] && state.step(i) == null) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Initialise the oneStepUndo map by assigning every state to itself, and
	 * ensuring that enough space was allocated.
	 */
	private static int[] initOneStepUndo(Automaton automaton, int[] oneStepUndo) {
		int nStates = automaton.nStates();

		// Ensure capacity for undo and binding space
		if(oneStepUndo.length < nStates) {
			oneStepUndo = new int[nStates * 2];
		}

		// Initialise undo information
		for (int i = 0; i != oneStepUndo.length; ++i) {
			oneStepUndo[i] = i;
		}
		
		return oneStepUndo;
	}
	
	/**
	 * The purpose of this method is to ensure that states below the pivot which
	 * are now unreachable (if any) are reverted to their original state. This
	 * is import to ensure that the automaton will always return to its original
	 * state iff it is equivalent.
	 *
	 * @param activation
	 * @param pivot
	 */
	private void applyUndo(Automaton automaton, int from, int to, int pivot) {
		// Update the oneStepUndo map with the new information.
		int nStates = automaton.nStates();
		int oStates = oneStepUndo.length;

		// First, ensure enough memory allocated for undo function.
		if (oStates < nStates) {
			// First, copy and update undo information
			int[] tmpUndo = new int[nStates * 2];
			System.arraycopy(oneStepUndo, 0, tmpUndo, 0, oStates);
			for (int i = oStates; i != tmpUndo.length; ++i) {
				tmpUndo[i] = i;
			}
			oneStepUndo = tmpUndo;
		}

		// Second, apply the oneStepUndo map to all unreachable vertices
		boolean changed = false;
		for (int i = 0; i != pivot; ++i) {
			if (!reachable[i]) {
				Automaton.State state = automaton.get(i);
				if (state != null) {
					changed |= state.remap(oneStepUndo);
				}
			}
		}

		// Third, minimise automaton (if applicable)
		if(changed) {
			// At this point, the automaton is not necessarily minimised and,
			// hence, we must minimise it.
			automaton.minimise();
			reachable = initReachable(automaton, reachable);
		}

		// Finally, update the oneStepUndo information. This has to be done last
		// since unreachable states to utilise the previous oneStepUndo
		// information.  See #382.
		if (to >= pivot) {
			if(from < pivot) {
				// In this case, we need to initialise the oneStepUndo
				// information.
				oneStepUndo[to] = from;
			} else if (from != oneStepUndo[from]){
				// In this case, we need to transfer the oneStepUndo
				// information.
				oneStepUndo[to] = oneStepUndo[from];
				// Reset undo information.
				oneStepUndo[from] = from;
			}
		}
	}
	
	/**
	 * Update the reachability information associated with the automaton after
	 * some change has occurred. This information is currently recomputed from
	 * scratch, though in principle it could be updated incrementally.
	 */
	private static boolean[] initReachable(Automaton automaton,
			boolean[] reachable) {

		// TODO: update reachability information incrementally

		if (reachable.length < automaton.nStates()) {
			reachable = new boolean[automaton.nStates() * 2];
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

		return reachable;
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
	
	private static void compact(Automaton automaton, int pivot,
			boolean[] reachable, int[] oneStepUndo) {
		int nStates = automaton.nStates();
		int nRoots = automaton.nRoots();
		int[] binding = new int[nStates];

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
				oneStepUndo[j] = oneStepUndo[i];
				automaton.set(j++, ith);
			}
		}

		if (j < nStates) {
			// Update the oneStepUndo relation to ensure it remains
			// sound. The invariant it maintains is that all states above the
			// pivot map to themselves or to a state below the pivot.
			for(int i=j;i<nStates;++i) {
				oneStepUndo[i] = i;
			}

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
				oneStepUndo[i] = binding[oneStepUndo[i]];
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
