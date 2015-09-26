package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.RewriteRule;
import wyrw.core.Rewriter;
import wyrw.core.Rewriter.Normaliser;

/**
 * Responsible for normalising an automaton using a predefined set of
 * <code>RewriteRule</code>.
 * 
 * @author David J. Pearce
 *
 */
public class RewritingNormaliser implements AbstractRewriter.Normaliser {
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
	 * The normaliser provides a generic hook for different approaches to
	 * normalising an automaton after a successful rule application.
	 */
	protected final Rewriter.Normaliser normaliser;
	
	/**
	 * Used to sort activations generated for a given state. This allows for
	 * some heuristics which reduce the amount of rewriting required.
	 */
	protected final Comparator<Activation> comparator;
	
	public RewritingNormaliser(Schema schema, Rewriter.Normaliser normaliser, RewriteRule... rules) {
		this(schema,normaliser, Activation.RANK_COMPARATOR,rules);
	}
	
	public RewritingNormaliser(Schema schema, Rewriter.Normaliser normaliser, Comparator<Activation> comparator, RewriteRule... rules) {
		this.schema = schema;
		this.normaliser = normaliser;
		this.comparator = comparator;
		this.rules = rules;
	}

	@Override
	public void apply(Automaton automaton) {
		automaton = new Automaton(automaton);
		while(inplaceRewrite(automaton,initialise(automaton))) {
			// do nout
		}
	}	
	
	private boolean inplaceRewrite(Automaton automaton, Activation[] activations) {
		int size = automaton.nStates();
		for (int i = 0; i != activations.length; ++i) {
			Activation activation = activations[i];
			int from = activation.root();
			int target = activation.apply(automaton);
			if (target != Automaton.K_VOID && from != target) {
				// Rewrite applied
				normaliser.apply(automaton);
				return true;
			} else {
				automaton.resize(size);
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
