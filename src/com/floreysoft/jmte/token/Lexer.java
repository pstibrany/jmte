package com.floreysoft.jmte.token;

import static com.floreysoft.jmte.util.NestedParser.*;

import java.util.List;

import com.floreysoft.jmte.util.Util;

public class Lexer {

	public AbstractToken nextToken(final char[] template, final int start,
			final int end) {
		String text = new String(template, start, end - start);
		if (text.startsWith("--")) {
			// comment
			return null;
		}

        int line = getLine(template, start, end);
        int column = getColumn(template, start, end);

		AbstractToken token = innerNextToken(text, line, column);
		return token;
	}

    static int getLine(char[] buffer, int start, int end) {
        int line = 1;
        for (int i = 0; i < start; i++) {
            if (buffer[i] == '\n') {
                line++;
            }
        }
        return line;
    }

    static int getColumn(char[] buffer, int start, int end) {
        int column = 0;
        if (buffer.length != 0) {
            for (int i = start; i >= 0; i--) {
                if (buffer[i] == '\n') {
                    break;
                } else {
                    column++;
                }
            }
        }
        return column;
    }

	private String unescapeAccess(List<? extends Object> arr,int index){
		String val = access(arr,index);
		if (val!=null && val.trim().length()>0){
			val = Util.NO_QUOTE_MINI_PARSER.unescape(val); 
		}
		return val;
	}
	
	private AbstractToken innerNextToken(final String untrimmedInput, int line, int column) {
		final String input = Util.trimFront(untrimmedInput);

		final List<String> split = Util.RAW_MINI_PARSER.splitOnWhitespace(input);

		// LENGTH 0
		if (split.size() == 0) {
			// empty expression like ${}
			return new StringToken(line, column);
		}

		if (split.size() >= 2) {
			// LENGTH 2..n

			final String cmd = split.get(0);
			final String objectExpression = split.get(1);

			boolean isIf = cmd.equalsIgnoreCase(IfToken.IF);
			boolean isElseIf = cmd.equalsIgnoreCase(ElseIfToken.ELSE_IF);
			if (isIf || isElseIf) {
				final boolean negated;
				final String ifExpression;
				// TODO: Both '!' and '=' work only if there are no white space
				// separators
				if (objectExpression.startsWith("!")) {
					negated = true;
					ifExpression = objectExpression.substring(1);
				} else {
					negated = false;
					ifExpression = objectExpression;
				}
				if (!ifExpression.contains("=")) {
					if (isIf) {
						return new IfToken(ifExpression, negated, line, column);
					} else {
						return new ElseIfToken(ifExpression, negated, line, column);
					}
				} else {
					final String[] ifSplit = ifExpression.split("=");
					final String variable = ifSplit[0];
					String operand = ifSplit[1];
					// remove optional quotations
					if (operand.startsWith("'") || operand.startsWith("\"")) {
						operand = operand.substring(1, operand.length() - 1);
					}
					if (isIf) {
						return new IfCmpToken(variable, operand, negated, line, column);
					} else {
						return new ElseIfCmpToken(variable, operand, negated, line, column);
					}
				}
			}
			if (cmd.equalsIgnoreCase(ForEachToken.FOREACH)) {
				final String varName = split.get(2);
				// we might also have
				// separator
				// data
				// but as the separator itself can contain
				// spaces
				// and the number of spaces between the previous
				// parts is unknown, we need to do this smarter
				int gapCount = 0;
				int separatorBegin = 0;
				while (separatorBegin < input.length()) {
					char c = input.charAt(separatorBegin);
					separatorBegin++;
					if (Character.isWhitespace(c)) {
						gapCount++;
						if (gapCount == 3) {
							break;
						} else {
							while (Character.isWhitespace(c = input
									.charAt(separatorBegin)))
								separatorBegin++;
						}
					}
				}

				String separator = input.substring(separatorBegin);
				if (separator !=null){
					separator = Util.NO_QUOTE_MINI_PARSER.unescape(separator);
				}
				return new ForEachToken(objectExpression, varName, separator
						.length() != 0 ? separator : null, line, column);
			}
		}

		final String objectExpression = split.get(0);
		// ${
		// } which might be used for silent line breaks
		if (objectExpression.equals("")) {
			return new StringToken(line, column);
		}
		final String cmd = objectExpression;
		if (cmd.equalsIgnoreCase(ElseToken.ELSE)) {
			return new ElseToken(line, column);
		}
		if (cmd.equalsIgnoreCase(EndToken.END)) {
			return new EndToken(line, column);
		}

		// ${<h1>,address(NIX),</h1>;long(full)}
		String variableName = null; // address
		String defaultValue = null; // NIX
		String prefix = null; // <h1>
		String suffix = null; // </h1>
		String rendererName = null; // long
		String parameters = null; // full

		// be sure to use the raw input as we might have to preserve
		// whitespace for prefix and postfix
		// only innermost parsers are allowed to unescape
		final List<String> strings = Util.RAW_OUTPUT_MINI_PARSER.split(
				untrimmedInput, ';', 2);
		// <h1>,address(NIX),</h1>
		final String complexVariable = strings.get(0);
		// only innermost parsers are allowed to unescape
		final List<String> wrappedStrings = Util.RAW_OUTPUT_MINI_PARSER.split(
				complexVariable, ',', 3);
		// <h1>
		prefix = wrappedStrings.size() == 3 ? unescapeAccess(wrappedStrings, 0) : null;
		// </h1>
		suffix = wrappedStrings.size() == 3 ? unescapeAccess(wrappedStrings, 2) : null;

		// address(NIX)
		final String completeDefaultString = (wrappedStrings.size() == 3 ? unescapeAccess(
				wrappedStrings, 1)
				: complexVariable).trim();
		final List<String> defaultStrings = Util.MINI_PARSER.greedyScan(
				completeDefaultString, "(", ")");
		// address
		variableName = unescapeAccess(defaultStrings, 0);
		// NIX
		defaultValue = unescapeAccess(defaultStrings, 1);

		// long(full)
		final String format = access(strings, 1);
		final List<String> scannedFormat = Util.MINI_PARSER.greedyScan(format,
				"(", ")");
		// long
		rendererName = access(scannedFormat, 0);
		// full
		parameters = access(scannedFormat, 1);

		// this is not a well formed variable name
		if (variableName.contains(" ")) {
			return new InvalidToken(untrimmedInput, line, column);
		}

		final StringToken stringToken = new StringToken(untrimmedInput,
				variableName, defaultValue, prefix, suffix, rendererName,
				parameters, line, column);
		return stringToken;

	}

}
