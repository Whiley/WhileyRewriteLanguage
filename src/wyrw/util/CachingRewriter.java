package wyrw.util;

import java.util.ArrayList;
import java.util.HashMap;

import wyautl.core.Automaton;
import wyrw.core.RewriteProof;
import wyrw.core.RewriteState;
import wyrw.core.RewriteStep;
import wyrw.core.Rewriter;

public class CachingRewriter implements Rewriter {
	/**
	 * The underlying rewriter on which this backtracking rewriter is based.
	 */
	private final Rewriter rewriter;
	
	/**
	 * The cache of previously seen states. This allows the rewriter to keep
	 * track of which states have been seen before.
	 */
	private final HashMap<Automaton,RewriteState> cache = new HashMap<Automaton,RewriteState>();
	
	public CachingRewriter(Rewriter rewriter) {
		this.rewriter = rewriter;
	}
	
	@Override
	public RewriteState initialise(Automaton automaton) {
		RewriteState state = cache.get(automaton);
		if(state == null) {
			// Now, this is a completely new state. Therefore, we put it in
			// the cache so that we can check it in the future.
			state = rewriter.initialise(automaton);
			cache.put(automaton, state);
			return state;
		}
		return state;
	}
	
	@Override
	public RewriteStep apply(RewriteState state, int choice) {
		return cacheLookup(rewriter.apply(state,choice));
	}		
	
	@Override
	public RewriteProof apply(RewriteState state) {
		ArrayList<RewriteStep> steps = new ArrayList<RewriteStep>();
		int r;
		while ((r = state.select()) != -1) {
			RewriteStep step = cacheLookup(apply(state,r));
			state = step.after();
			steps.add(step);			
		}
		return new RewriteProof(steps.toArray(new RewriteStep[steps.size()]));
	}
	
	private RewriteStep cacheLookup(RewriteStep step) {
		RewriteState before = step.before();
		RewriteState after = step.after();
		if (before != after) {
			// A new state was generated. The next question is whether or not
			// we've seen it before.
			Automaton automaton = after.automaton();
			after = cache.get(automaton);
			if (after != null) {
				// Yes, we have seen this state before. Hence, we return the
				// cached state as this will identify activations previously
				// applied.
				step = new RewriteStep(before, step.activation(), after);
				before.update(step.activation(), step);
			} else {
				// Now, this is a completely new state. Therefore, we put it in
				// the cache so that we can check it in the future.
				cache.put(automaton, step.after());
			}
		}
		return step;
	}
}
