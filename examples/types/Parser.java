import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import wyautl.core.Automaton;

public class Parser {
	private String input;
	private int index;
	private int dummyID = Integer.MAX_VALUE;
	public Parser(String input) {
		this.input = input;
		this.index = 0;
	}

	public int parse(Automaton automaton) {
		HashMap<String, Integer> environment = new HashMap<String, Integer>();
		return parse(automaton,environment);
	}

	public int parse(Automaton automaton, Map<String, Integer> environment) {

		int lhs = parseAndOr(automaton,environment);
		skipWhiteSpace();

		if (index < input.length() && input.charAt(index) == ',') {
			ArrayList<Integer> elements = new ArrayList<Integer>();
			elements.add(lhs);
			while (index < input.length() && input.charAt(index) == ',') {
				match(",");
				elements.add(parseAndOr(automaton,environment));
				skipWhiteSpace();
			}
			int[] es = new int[elements.size()];
			for (int i = 0; i != es.length; ++i) {
				es[i] = elements.get(i);
			}
			lhs = Types.Tuple(automaton, es);
		}

		return lhs;
	}

	public int parseAndOr(Automaton automaton, Map<String, Integer> environment) {

		int lhs = parseTerm(automaton,environment);
		skipWhiteSpace();

		if(index < input.length()) {
			char lookahead = input.charAt(index);

			if(lookahead == '&') {
				match("&");
				int rhs = parseAndOr(automaton,environment);
				lhs = Types.Intersect(automaton, lhs, rhs);
			} else if(lookahead == '|') {
				match("|");
				int rhs = parseAndOr(automaton,environment);
				lhs = Types.Union(automaton, lhs, rhs);
			}
		}

		return lhs;
	}

	public int parseTerm(Automaton automaton, Map<String, Integer> environment) {
		skipWhiteSpace();
		char lookahead = input.charAt(index);

		if (lookahead == '(') {
			return parseBracketed(automaton, environment);
		} else if (lookahead == '!') {
			match("!");
			return Types.Not(automaton, parseTerm(automaton, environment));
		} else if(lookahead == '\\') {
			return parseRecursive(automaton,environment);			
		} else {	
			String word = readWord();
			if (word.equals("int")) {
				return automaton.add(Types.Int);
			} else if (word.equals("any")) {
				return automaton.add(Types.Any);
			} else if (environment.containsKey(word)) {
				return environment.get(word);
			} else {
				throw new RuntimeException("unknown keyword or variable: "
						+ word);
			}
		}
	}

	private int parseBracketed(Automaton automaton, Map<String, Integer> environment) {
		match("(");
		int root = parse(automaton,environment);
		match(")");
		return root;
	}

	public int parseRecursive(Automaton automaton, Map<String, Integer> environment) {
		match("\\");
		String name = readWord();
		match(".");
		int temp = dummyID--;
		environment.put(name, temp);
		int actual = parse(automaton, environment);
		remap(automaton,temp,actual);
		return actual;
	}
	
	private String readWord() {
		int start = index;
		while (index < input.length()
				&& Character.isLetter(input.charAt(index))) {
			index++;
		}
		return input.substring(start, index);
	}

	private int readNumber() {
		int start = index;
		while (index < input.length() && Character.isDigit(input.charAt(index))) {
			index = index + 1;
		}
		return Integer.parseInt(input.substring(start, index));
	}

	private void match(String text) {
		skipWhiteSpace();
		if(input.startsWith(text,index)) {
			index += text.length();
		} else {
			error();
		}
	}

	private void skipWhiteSpace() {
		while (index < input.length()
				&& (input.charAt(index) == ' ' || input.charAt(index) == '\n')) {
			index = index + 1;
		}
	}

	private void remap(Automaton automaton, int from, int to) {
		for(int i=0;i!=automaton.nStates();++i) {
			automaton.get(i).remap(from,to);
		}
	}

	private void error() {
		final String msg = "Cannot parse character '"
			+ input.charAt(index)
		    + "' at position " + index + " of input '" + input + "'\n";
		throw new RuntimeException(msg);
	}
}
