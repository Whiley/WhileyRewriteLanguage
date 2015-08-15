package wyrw.core;

import java.util.ArrayList;

/**
 * Represents a "proof" of a rewrite. That is, it identifies the sequence of
 * steps taken in rewriting one automaton into another. Observe that a rewrite
 * proof does not necessarily correspond to a linear sequence of rewrites, as
 * backtracking and other concerns may arise.
 * 
 * @author David J. Pearce
 *
 */
public interface RewriteProof {
	
	/**
	 * Return the number of steps in the proof
	 * 
	 * @return
	 */
	public int size();

	/**
	 * Returns a given step in the proof
	 * @param i
	 * @return
	 */
	public RewriteStep step(int i);
	
	public RewriteState first() {
		return steps[0].before();
	}
	
	public RewriteState last() {		
		return steps[steps.length-1].after();
	}
}
