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
 * This rewriter is <i>fair</i> in that it makes an effort to ensure rules are
 * given equal opportunity to fire. Intuitively, this is to ensure rules are not
 * "starved" as this can prevent certain rewrites of interest from happening.
 * </p>
 * <p>
 * This rewriter is not particularly efficient as it creates a new state on
 * every rewrite. But, this does mean all intermediate states can be inspected.
 * </p>
 * 
 * @author David J. Pearce
 *
 */
public class FairLinearRewriter extends AbstractRewriter implements Rewriter {
	/**
	 * The current state being rewritten by this rewriter.
	 */
	protected int current;
	
	/**
	 * Provides the activation index into the current state which we're
	 * considering.
	 */
	protected int index;
	
	public FairLinearRewriter(Rewrite rewrite) {
		super(rewrite);
	}

	@Override
	public void apply(int maxSteps) {
		int count = 0;
		List<Rewrite.State> states = rewrite.states();
		while (count < maxSteps) {
			Rewrite.State state = states.get(current);
			int next = select(state);
			if (next != -1) {
				// Yes, there is at least one activation left to try
				Automaton automaton = new Automaton(state.automaton());
				Activation activation = state.activation(next);
				if(rewrite(automaton,activation)) {
					// An actual step occurred
					current = step(current, automaton, next);
					count = count + 1;
				} else {
					// loop back
					invalidate(current, next);
				}
			} else {
				// There are no activations left to try so we are done.
				break;
			}
		}
	}

	@Override
	public int initialise(Automaton automaton) {
		current = rewrite.add(automaton);
		return current;
	}		
	
	private int select(Rewrite.State state) {
		if(state.rank() == 0) {
			return -1;
		} else {
			// Following loop guaranteed to terminate because rank is non-zero.
			while (true) {
				index = (index + 1) % state.size();
				if (state.step(index) == null) {
					return index;
				}				
			}
			
		}
	}
	
}
