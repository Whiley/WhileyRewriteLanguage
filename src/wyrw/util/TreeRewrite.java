package wyrw.util;

import java.util.Comparator;

import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.RewriteRule;

/**
 * <p>
 * Represents the simplest form of a rewrite which always corresponds to a
 * "tree" (or, for multi-rooted rewrites, a "forest"). This means caching of
 * previously seen states is not performed and, thus, every state generated is
 * "new".
 * </p>
 * <p>
 * The intention is that this provides a lightweight and efficient form of
 * rewrite for cases where we just want to rewrite as quickly as possible.
 * Typically, we're assuming the underlying rewrite system is confluent for this
 * to make sense.
 * </p>
 * 
 * @author David J. Pearce
 *
 */
public class TreeRewrite extends AbstractRewrite implements Rewrite {

	public TreeRewrite(Schema schema, Comparator<Activation> comparator, RewriteRule[] rules) {
		super(schema, comparator, rules);
	}

}
