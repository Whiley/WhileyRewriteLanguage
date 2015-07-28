package wyautl.rw;

/**
 * Represents a single rewriting step. This may or may not correspond directly
 * to single application of an underlying rewrite rule. For example, it might
 * correspond to a single "inference step".
 *
 * @author David J. Pearce
 *
 */
public class RewriteStep {
	private RewriteState before;
	private RewriteState after;
	private RewriteRule rule;
}
