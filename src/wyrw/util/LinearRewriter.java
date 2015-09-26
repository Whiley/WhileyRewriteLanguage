// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyrw.util;

import java.util.*;

import wyautl.core.*;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.Rewrite.State;
import wyrw.core.RewriteRule;
import wyrw.core.Rewriter;

/**
 * <p>
 * Provides a simple implementation of the Rewriter interface which is good for
 * many situations, in particular when the underlying rewrite rules are
 * confluent. This rewriter does not perform backtracking and, hence, it always
 * produces rewrites which form a single line (i.e. they are linear).
 * </p>
 * <p>
 * This rewriter is not particularly efficient as it creates a new state on
 * every rewrite. But, this does mean all intermediate states can be inspected.
 * </p>
 * 
 * @author David J. Pearce
 *
 */
public class LinearRewriter extends AbstractRewriter implements Rewriter {
	/**
	 * The current state being rewritten by this rewriter.
	 */
	protected int HEAD;
	
	/**
	 * The heuristic is responsible for choosing which activation to apply. This
	 * can affect overall performance.
	 */
	protected Heuristic heuristic;

	/**
	 * Provides the activation index into the current state which we're
	 * considering.
	 */
	protected int index;

	public LinearRewriter(Rewrite rewrite) {
		this(rewrite,new UnfairHeuristic());
	}
	
	public LinearRewriter(Rewrite rewrite, Heuristic heuristic) {
		super(rewrite);
	}

	@Override
	public void apply(int maxSteps) {
		int count = 0;
		List<Rewrite.State> states = rewrite.states();
		while (count < maxSteps) {
			Rewrite.State state = states.get(HEAD);
			int next = heuristic.select(state);
			if (next != -1) {
				// Yes, there is at least one activation left to try
				Automaton automaton = new Automaton(state.automaton());
				Activation activation = state.activation(next);
				if (rewrite(automaton, activation)) {
					// An actual step occurred
					HEAD = step(HEAD, automaton, next);
					count = count + 1;
				} else {
					// loop back
					invalidate(HEAD, next);
				}
			} else {
				// There are no activations left to try so we are done.
				break;
			}
		}
	}

	@Override
	public int initialise(Automaton automaton) {
		HEAD = rewrite.add(automaton);
		return HEAD;
	}

	/**
	 * Responsible for choosing the next activation to apply. Since there are
	 * quite a few different approaches which can be taken, this is an interface
	 * which different algorithms can implement.
	 * 
	 * @author David J. Pearce
	 *
	 */
	public static interface Heuristic {
		public int select(Rewrite.State state);
	}

	/**
	 * The most basic of selection heuristics which simply chooses the next
	 * available activation to apply. In this sense, it is "unfair" because it
	 * can lead to certain activations being "starved".
	 * 
	 * @author David J. Pearce
	 *
	 */
	public static class UnfairHeuristic implements Heuristic {

		@Override
		public int select(State state) {
			return state.select();
		}

	}
}
