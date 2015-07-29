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
		RewriteStep step = rewriter.apply(choice);
		// At this point, we check whether or not we have previously encountered
		// this state.
		Automaton automaton = step.after().automaton();
		RewriteState after = cache.get(automaton);
		if (after != null) {
			// indicates a state we have previously encountered
			RewriteState before = step.before();
			step = new RewriteStep(before, step.activation(), after);
			before.update(step.activation(), step);
		} else {
			// this is a new state
			cache.put(automaton, after);
		}
		return step;
	}
}
