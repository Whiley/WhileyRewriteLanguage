package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.RewriteRule;

public class StackedRewrite implements Rewrite {
	/**
	 * The schema used by automata being reduced. This is primarily useful for
	 * debugging purposes.
	 */
	protected final Schema schema;
	
	/**
	 * The list of rewrite rules which the rewriter can apply.
	 */
	protected final RewriteRule[] rules;
	
	/**
	 * Used to sort activations generated for a given state. This allows for
	 * some heuristics which reduce the amount of rewriting required.
	 */
	protected final Comparator<Activation> comparator;
	
	/**
	 * The top-level rewrite
	 */
	private final Rewrite rewrite;
	
	public StackedRewrite(Rewrite rewrite, Schema schema, RewriteRule... rules) {
		this(rewrite,schema,Activation.RANK_COMPARATOR,rules);
	}
	
	public StackedRewrite(Rewrite rewrite, Schema schema, Comparator<Activation> comparator, RewriteRule... rules) {
		this.rewrite = rewrite;
		this.schema = schema;
		this.comparator = comparator;
		this.rules = rules;
	}

	@Override
	public int add(Automaton automaton) {		
		return rewrite.add(rewrite(automaton));
	}

	@Override
	public int add(Step step) {
		return rewrite.add(step);
	}

	@Override
	public List<Step> steps() {
		return rewrite.steps();
	}

	@Override
	public List<State> states() {
		return rewrite.states();
	}
	
	private Automaton rewrite(Automaton automaton) {
		automaton = new Automaton(automaton);
		while(inplaceRewrite(automaton,initialise(automaton))) {
			// what to do here?
		}
		return automaton;
	}	
	
	private boolean inplaceRewrite(Automaton automaton, Activation[] activations) {
		for (int i = 0; i != activations.length; ++i) {
			Activation activation = activations[i];
			int from = activation.root();
			int target = activation.apply(automaton);
			if (target != Automaton.K_VOID && from != target) {
				// Rewrite applied
				automaton.compact();
				automaton.minimise();
				return true;
			}
		}
		return false;
	}

	private Activation[] initialise(Automaton automaton) {
		ArrayList<Activation> activations = new ArrayList<Activation>();
		for (int s = 0; s != automaton.nStates(); ++s) {
			Automaton.State state = automaton.get(s);
			// Check whether this state is a term or not; that's because only
			// terms can be roots for rewrite rule applications.
			if (state instanceof Automaton.Term) {
				for (int r = 0; r != rules.length; ++r) {
					rules[r].probe(automaton, s, activations);
				}
			}
		}
		Activation[] array = activations.toArray(new Activation[activations.size()]);
		Arrays.sort(array, comparator);
		return array;
	}
}
