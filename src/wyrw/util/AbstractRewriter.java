package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.RewriteRule;
import wyrw.core.RewriteState;
import wyrw.core.RewriteStep;
import wyrw.core.Rewriter;

public abstract class AbstractRewriter implements Rewriter {
	
	/**
	 * The underlying rewrite to which this rewriter is being applied.
	 */
	protected final Rewrite rewrite;
	
	/**
	 * The schema used by automata being reduced. This is primarily useful for
	 * debugging purposes.
	 */
	protected final Schema schema;
	
	/**
	 * The list of rewrite rules which the rewriter can apply.
	 */
	protected final RewriteRule[] rules;
			
	public AbstractRewriter(Rewrite rewrite, Schema schema, RewriteRule... rules) {
		this.schema = schema;
		this.rules = rules;		
		this.rewrite = rewrite;
	}
	
	@Override
	public void apply(int maxSteps) {
		
	}
		
	@Override
	public void initialise(Automaton automaton) {
		
	}
}
