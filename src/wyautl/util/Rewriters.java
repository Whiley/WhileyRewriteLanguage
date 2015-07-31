package wyautl.util;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.rw.Activation;
import wyautl.rw.InferenceRule;
import wyautl.rw.ReductionRule;
import wyautl.rw.RewriteRule;
import wyautl.rw.RewriteState;
import wyautl.rw.RewriteStep;
import wyautl.rw.Rewriter;

public class Rewriters {
	
	/**
	 * Fully reduce an automaton in place. This is assuming that the given rules
	 * are confluent, as it makes no effort to avoid infinite rewrite loops.
	 * 
	 * @param automaton
	 * @param rules
	 */
	public static Automaton reduce(Automaton automaton, Schema schema, ReductionRule... rules) {
		Rewriter rewriter = new SingleStepRewriter(automaton,schema,rules);				
		return rewriteAll(rewriter,ReductionRule.class);
	}
		
	public static Automaton infer(Automaton automaton, Schema schema, InferenceRule[] inferences,
			ReductionRule[] reductions, int maxSteps) {
		RewriteRule[] rules = new RewriteRule[inferences.length + reductions.length];
		System.arraycopy(inferences, 0, rules, 0, inferences.length);
		System.arraycopy(reductions, 0, rules, inferences.length, reductions.length);
		Rewriter rewriter = new SingleStepRewriter(automaton, schema, rules);
		rewriter = new CachingRewriter(rewriter);
		automaton = rewriteAll(rewriter, ReductionRule.class);
		int r;
		while (maxSteps > 0 && (r = selectFirstUnvisited(InferenceRule.class, rewriter)) != -1) {
			RewriteStep step = rewriter.apply(r);
			automaton = rewriteAll(rewriter, ReductionRule.class);
			maxSteps = maxSteps - 1;
		}
		if(maxSteps == 0) {
			return null;
		} else {
			return automaton;
		}
	}
	
	private static <T extends RewriteRule> Automaton rewriteAll(Rewriter rewriter, Class<T> kind) {					
		int r;
		Automaton automaton = rewriter.state().automaton();
		while((r = selectFirstUnvisited(kind,rewriter)) != -1) {
			RewriteStep step = rewriter.apply(r);
			automaton = step.after().automaton();
		}
		return automaton;
	}
	
	private static <T extends RewriteRule> int selectFirstUnvisited(Class<T> kind, Rewriter rewriter) {
		RewriteState state = rewriter.state();
		for(int i=0;i!=state.size();++i) {
			if(state.step(i) == null) {
				Activation activation = state.activation(i);
				if(kind.isInstance(activation.rule())) {
					return i;
				}
			}
		}
		return -1;
	}
}
