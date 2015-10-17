package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyrw.core.Reduction;
import wyrw.core.ReductionRule;

public class Reductions {
	
	
	public static void minimiseAndReduce(Automaton automaton, int maxSteps, ReductionRule... reductions) {
		minimiseAndReduce(automaton,maxSteps,reductions,AbstractActivation.RANK_COMPARATOR);
	}
	
	public static void minimiseAndReduce(Automaton automaton, int maxSteps, ReductionRule[] reductions,
			Comparator<AbstractActivation> comparator) {
		automaton.minimise();
		automaton.compact(0);
		reduceOver(automaton, 0,maxSteps, reductions, comparator);
	}
	
	public static void reduceOver(Automaton automaton, int start, int maxSteps, ReductionRule... reductions) {
		reduceOver(automaton,start,maxSteps,reductions,AbstractActivation.RANK_COMPARATOR);
	}
		
	public static void reduce(Automaton automaton, int maxSteps, ReductionRule... reductions) {
		reduceOver(automaton,0,maxSteps,reductions,AbstractActivation.RANK_COMPARATOR);
	}
	
	/**
	 * Simple helper method for reducing an automaton.
	 * 
	 * @param automaton
	 */
	public static void reduceOver(Automaton automaton, int start, int maxSteps, ReductionRule[] reductions,
			Comparator<AbstractActivation> comparator) {
		// Now, attempt to reduce as much as possible
		boolean changed = true;
		nProbes = 0;
		int nApplied = 0;
		while (changed && maxSteps-- > 0) {
			changed = false;
			AbstractActivation[] activations = probe(automaton, start, reductions, comparator);
			int pivot = automaton.nStates();
			for (int i = 0; i != activations.length; ++i) {
				AbstractActivation activation = activations[i];
				int from = activation.target();
				int target = activation.apply(automaton);
				if (target != Automaton.K_VOID && from != target) {
					// Rewrite applied
					//automaton.minimise();					
					automaton.compact(0);
					nApplied++;
					changed = true;
					break;
				} else {
					automaton.resize(pivot);
				}
			}
		}
		//System.out.println("REDUCTION ACTIVATIONS = " + nApplied + " / " + nProbes);		
	}
	
	static int nProbes;
	
	private static AbstractActivation[] probe(Automaton automaton, int start, ReductionRule[] reductions,
			Comparator<AbstractActivation> comparator) {
		ArrayList<Reduction.Activation> activations = new ArrayList<Reduction.Activation>();
		for (int s = start; s != automaton.nStates(); ++s) {
			// State is reachable from the given root
			Automaton.State state = automaton.get(s);
			// Check whether this state is a term or not; that's because
			// only
			// terms can be roots for rewrite rule applications.
			if (state instanceof Automaton.Term) {
				for (int r = 0; r != reductions.length; ++r) {
					reductions[r].probe(automaton, s, activations);
					nProbes++;
				}
			}
		}
		AbstractActivation[] array = activations.toArray(new AbstractActivation[activations.size()]);
		if (comparator != null) {
			Arrays.sort(array, comparator);
		}
		return array;
	}
}
