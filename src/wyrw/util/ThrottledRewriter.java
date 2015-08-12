package wyrw.util;

import java.util.ArrayList;

import wyautl.core.Automaton;
import wyrw.core.RewriteProof;
import wyrw.core.RewriteState;
import wyrw.core.RewriteStep;
import wyrw.core.Rewriter;

public class ThrottledRewriter implements Rewriter {
	private final Rewriter rewriter;
	
	private final int maxSteps;
	
	public ThrottledRewriter(Rewriter rewriter, int maxSteps) {
		this.rewriter = rewriter;
		this.maxSteps = maxSteps;
	}

	@Override
	public RewriteStep apply(RewriteState state, int choice) {
		return rewriter.apply(state, choice);
	}

	@Override
	public RewriteProof apply(RewriteState state) {
		int r;
		ArrayList<RewriteStep> steps = new ArrayList<RewriteStep>();
		int count = 0;
		while (count < maxSteps && (r = state.select()) != -1) {
			RewriteStep step = apply(state, r);
			steps.add(step);
			state = step.after();
			count = count + 1;
		}
		return new RewriteProof(steps.toArray(new RewriteStep[steps.size()]));
	}

	@Override
	public RewriteState initialise(Automaton automaton) {
		return rewriter.initialise(automaton);
	}
}
