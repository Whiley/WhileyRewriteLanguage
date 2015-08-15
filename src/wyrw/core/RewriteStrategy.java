package wyrw.core;

/**
 * Responsible for controlling the overall way in which the rewrite space is
 * searched, as necessary for non-confluent rewrite systems.
 * 
 * @author David J. Pearce
 *
 */
public interface RewriteStrategy {
	
	/**
	 * Apply the strategy to a given state to rewrite as much as possible. This
	 * produces a rewrite "proof" which identifies all steps taken during
	 * rewriting.
	 * 
	 * @return
	 */
	public RewriteProof apply(RewriteState state);
}
