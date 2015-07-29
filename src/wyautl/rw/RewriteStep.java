package wyautl.rw;

/**
 * Represents a single rewriting step. This may or may not correspond directly
 * to single application of an underlying rewrite rule. For example, it might
 * correspond to a single "inference step".
 *
 * @author David J. Pearce
 *
 */
public class RewriteStep {
	/**
	 * State which held before this rewrite step
	 */
	private final RewriteState before;
	
	/**
	 * State which held after this rewrite step
	 */
	private final RewriteState after;
	
	/**
	 * The activation applied to the before state which led to the after state.
	 * This identifies the rewrite rule, but also the actual binding for that
	 * rule against states in the automaton.
	 */
	private final Activation activation;
	
	public RewriteStep(RewriteState before, Activation activation, RewriteState after) {
		this.before = before;
		this.after = after;
		this.activation = activation;
	}
}
