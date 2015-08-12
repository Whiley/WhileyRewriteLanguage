package wyrw.core;

import java.util.ArrayList;

/**
 * Represents a "proof" of a rewrite. That is, it identifies the sequence of
 * steps taken in rewriting one automaton into another.
 * 
 * @author David J. Pearce
 *
 */
public class RewriteProof {
	private final RewriteStep[] steps;
	
	public RewriteProof(RewriteStep... steps) {
		this.steps = steps;
	}
	
	public int size() {
		return steps.length;
	}
	
	public RewriteStep step(int i) {
		return steps[i];
	}
	
	public RewriteState first() {
		return steps[0].before();
	}
	
	public RewriteState last() {		
		return steps[steps.length-1].after();
	}
}
