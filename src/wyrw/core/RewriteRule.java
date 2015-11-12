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

package wyrw.core;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import wyautl.core.Automaton;
import wyrl.core.Pattern;

public interface RewriteRule {

	/**
	 * Get the pattern object that describes what this rule will match against.
	 * More specifically, any state which matches this pattern is guaranteed to
	 * produce at least one activation from probing. This is useful for creating
	 * dispatch tables for more efficient probing of automaton states.
	 *
	 * @return
	 */
	public Pattern.Term pattern();
	
	/**
	 * Get the annotations associated with this rewrite rule. Annotations are
	 * used-supplied fields that provide some kind of supplementary information.
	 * For example, one could use a "@Name" annotation to give each rule a
	 * unique name.  Similarly, one can provide a description for each rule.
	 * 
	 * @param name
	 * @return
	 */
	public Map<String,Object> annotations();
	
	/**
	 * Get a single annotation associated with this rewrite rule. Annotations are
	 * used-supplied fields that provide some kind of supplementary information.
	 * For example, one could use a "@Name" annotation to give each rule a
	 * unique name.  Similarly, one can provide a description for each rule.
	 * 
	 * @param name
	 * @return
	 */
	public Object annotation(String n);
}