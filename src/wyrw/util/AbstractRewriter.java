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

import wyautl.core.Automaton;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.Rewriter;

public abstract class AbstractRewriter implements Rewriter {
	
	/**
	 * The underlying rewrite to which this rewriter is being applied. The
	 * general assumption is that there is at most one rewriter assigned to any
	 * given rewrite.
	 */
	protected final Rewrite rewrite;
	
	/**
	 * The normaliser provides a generic hook for different approaches to
	 * normalising an automaton after a successful rule application.
	 */
	protected final Normaliser normaliser;
				
	public AbstractRewriter(Rewrite rewrite, Normaliser normaliser) {
		this.rewrite = rewrite;
		this.normaliser = normaliser;
	}
	
	@Override
	abstract public void apply(int maxSteps);
		
	@Override
	abstract public int initialise(Automaton automaton);
	
	protected void invalidate(int state, int activation) {
		rewrite.add(new AbstractRewrite.Step(state, state, activation));
	}
	
	/**
	 * Record that a particular step has been taken in the rewrite.
	 * 
	 * @param state
	 *            State before rewrite occurred
	 * @param automaton
	 *            Automaton after rewrite occurred
	 * @param activation
	 *            Activation index which caused rewrite
	 * @return
	 */
	protected int step(int state, Automaton automaton, int activation) {
		int after = rewrite.add(automaton);
		rewrite.add(new AbstractRewrite.Step(state, after, activation));
		return after;
	}
	
	/**
	 * Apply an activation to the automaton in place. That is, the current
	 * automaton is itself updated.
	 * 
	 * @param index
	 * @param automaton
	 * @return
	 */
	protected boolean rewrite(Automaton automaton,Activation activation) {
		int size = automaton.nStates();
		int from = activation.root();
		int target = activation.apply(automaton);

		if (target != Automaton.K_VOID && from != target) {
			normaliser.apply(automaton);
			return true;
		} else {
			// activation did not apply
			automaton.resize(size);
			return false;
		}
	}
}
