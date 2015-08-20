package wyrw.util;

import java.util.Comparator;
import java.util.HashMap;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.RewriteRule;

/**
 * <p>
 * Represents a more complex form rewrite which always corresponds to a "graph".
 * To do this, previously seen states are cached so that generated states can be
 * compared to see whether the correspond to those seen before. This strategy is
 * important in systems which are non-confluent to ensure branches which loop
 * will terminate (as they correspond to cycles in the graph).
 * </p>
 * 
 * @author David J. Pearce
 *
 */
public class GraphRewrite extends AbstractRewrite implements Rewrite {
	private final HashMap<Automaton,Integer> cache = new HashMap<Automaton,Integer>();
	
	public GraphRewrite(Schema schema, Comparator<Activation> comparator, RewriteRule[] rules) {
		super(schema, comparator, rules);
	}

	@Override 
	public int add(Automaton automaton) {
		Integer i = cache.get(automaton);
		if(i != null) {
			return i;
		} else {
			int r = super.add(automaton);
			cache.put(automaton,r);
			return r;
		}
	}
}
