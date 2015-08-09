package wyautl.util;

import java.util.HashMap;

import wyautl.core.Automaton;
import wyautl.rw.*;

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
	public RewriteState state() {
		return rewriter.state();
	}

	@Override
	public void reset(RewriteState state) {
		rewriter.reset(state);
	}

	@Override
	public RewriteStep apply(int choice) {
		return cacheLookup(rewriter.apply(choice));
	}		
	
	@Override
	public RewriteStep apply() {
		return cacheLookup(rewriter.apply());
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
				// cache state as this will identify activations previously
				// applied.
				step = new RewriteStep(before, step.activation(), after);
				before.update(step.activation(), step);
				rewriter.reset(after);
			} else {
				// Now, this is a completely new state. Therefore, we put it in
				// the cache so that we can check it in the future.
				cache.put(automaton, step.after());
			}
		}
		return step;
	}
}
