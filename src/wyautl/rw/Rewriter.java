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

package wyautl.rw;


/**
 * Represents the (abstract) mechanism for controlling the rewriting of a given
 * automaton under a given set of rules. Different implementation of this
 * interface are possible, and will have different performance characteristics.
 *
 * @author David J. Pearce
 *
 */
public interface Rewriter {

	/**
	 * Access the working state of the rewriter. This state can then be
	 * inspected to see choose the next activation.
	 * 
	 * @return
	 */
	public RewriteState state();
	
	/**
	 * Reset the working state to a previous state. This allows the rewrite to
	 * reset any internal data structures as well.
	 * 
	 * @param state
	 */
	public void reset(RewriteState state);
	
	/**
	 * Apply the rewriter to a given state by taking one of the available
	 * choices.  The choice taken must be valid for the state.
	 *
	 * @param state
	 * @param choice
	 * @return
	 */
	public RewriteStep apply(int choice);	
}
