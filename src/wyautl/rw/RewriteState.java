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
	private final Automaton automaton;

	/**
	 * The array of all possible activations on the given automaton.
	 */
	private final Activation[] activations;

	/**
	 * The array of possible steps from this automaton. Each entry matches the
	 * corresponding entry in the activations array. Entries maybe null to
	 * signal steps which have not yet been explored.
	 */
	private final RewriteStep[] steps;
	
	public RewriteState(Automaton automaton, Activation... activations) {
		this.automaton = automaton;
		this.activations = activations;
		this.steps = new RewriteStep[activations.length];
	}
	
	public int size() {
		return activations.length;
	}
	
	public Automaton automaton() {
		return automaton;
	}
	
	public Activation activation(int index) {
		return activations[index];
	}
	
	public RewriteStep step(int index) {
		return steps[index];
	}
	
	public void update(int index, RewriteStep step) {
		this.steps[index] = step;
	}
}
