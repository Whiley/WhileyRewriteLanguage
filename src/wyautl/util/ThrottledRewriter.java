package wyautl.util;

import wyautl.rw.RewriteState;
import wyautl.rw.RewriteStep;
import wyautl.rw.Rewriter;

public class ThrottledRewriter implements Rewriter {
	private final Rewriter rewriter;
	
	private final int maxSteps;
	
	public ThrottledRewriter(Rewriter rewriter, int maxSteps) {
		this.rewriter = rewriter;
		this.maxSteps = maxSteps;
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
		return rewriter.apply(choice);
	}

	@Override
	public RewriteStep apply() {
		int r;
		RewriteState state = rewriter.state();
		int count = 0;
		while (count < maxSteps && (r = AbstractRewriter.selectFirstUnvisited(state)) != -1) {
			RewriteStep step = apply(r);
			state = step.after();
			count = count + 1;
		} 
		// This doesn't make sense
		return new RewriteStep(state,0,state);	
	}
}
