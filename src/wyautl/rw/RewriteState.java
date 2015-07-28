package wyautl.rw;

import wyautl.core.Automaton;

/**
 * Represents an intermediate state reached during rewriting and provides a
 * mechanism for enumerating the possible next steps.
 *
 * @author David J. Pearce
 *
 */
public class RewriteState {
	/**
	 * The automaton which this state represents.
	 */
	private Automaton automaton;

	/**
	 * The array of all possible activations on the given automaton.
	 */
	private Activation[] activations;

	/**
	 * The array of possible steps from this automaton. Each entry matches the
	 * corresponding entry in the activations array. Entries maybe null to
	 * signal steps which have not yet been explored.
	 */
	private RewriteStep[] steps;
}
