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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import wyautl.core.Automaton;
import wyrw.core.Rewrite;
import wyrw.core.Rewriter;

public class BreadthFirstRewriter extends AbstractRewriter implements Rewriter {
	private final HashSet<Integer> visited = new HashSet<Integer>();
	private ArrayList<Integer> frontier = new ArrayList<Integer>();
	private int index;
	
	public BreadthFirstRewriter(Rewrite rewrite) {
		super(rewrite);
	}

	@Override
	public void apply(int count) {
		while(count > 0 && step()) {
			count = count - 1;
		}
	}

	@Override
	public int initialise(Automaton automaton) {
		int state = rewrite.add(automaton);		
		frontier.add(state);
		visited.add(state);
		index = 0;
		return state;
	}	
	
	private boolean step() {
		List<Rewrite.State> states = rewrite.states();
		while (frontier.size() > 0) {			
			while (index < frontier.size()) {
				int before = frontier.get(index);
				Rewrite.State state = states.get(before);
				int activation = state.select();
				if (activation != -1) {
					Automaton automaton = new Automaton(state.automaton());
					if (rewrite(automaton, state.activation(activation))) {
						step(before, automaton, activation);
						return true;
					} else {
						invalidate(before, activation);
					}					
				} else {
					index = index + 1;
				}
			}
			extendFrontier();
			index = 0;
		}
		
		return false;
	}
	
	/**
	 * Got through out current frontier and extend every state by one step to
	 * produce a new frontier. Any steps previously visited are ignored.
	 */
	private void extendFrontier() {
		ArrayList<Integer> nFrontier = new ArrayList<Integer>();
		List<Rewrite.State> states = rewrite.states();		
		for (int i = 0; i != frontier.size(); ++i) {			
			Rewrite.State state = states.get(frontier.get(i));
			for (int j = 0; j != state.size(); ++j) {
				Rewrite.Step step = state.step(j);
				int next = step.after();
				if (!visited.contains(next)) {
					visited.add(next);
					nFrontier.add(next);
				}
			}

		}		
		frontier = nFrontier;	
	}
}
