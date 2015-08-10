package wyautl.util;

import java.util.ArrayList;

import wyautl.rw.RewriteProof;
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
	public RewriteProof apply() {
		int r;
		ArrayList<RewriteStep> steps = new ArrayList<RewriteStep>();
		RewriteState state = rewriter.state();
		int count = 0;
		while (count < maxSteps && (r = AbstractRewriter.selectFirstUnvisited(state)) != -1) {
			RewriteStep step = apply(r);
			steps.add(step);
			state = step.after();
			count = count + 1;
		}
		return new RewriteProof(steps.toArray(new RewriteStep[steps.size()]));
	}
}
