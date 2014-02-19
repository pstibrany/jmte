package com.floreysoft.jmte.token;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.floreysoft.jmte.util.StartEndPair;
import com.floreysoft.jmte.util.Util;

public class TokenStream {
	private final List<Token> tokens;
	private int currentTokenIndex = 0;
	private Token currentToken = null;

	public TokenStream(List<Token> tokens) {
        this.tokens = tokens;
	}

	public static List<Token> parseTokens(String sourceName, String input, String splitStart, String splitEnd) {
        Lexer lexer = new Lexer();
        List<StartEndPair> scan = Util.scan(input, splitStart, splitEnd, true);
		List<Token> tokens = new ArrayList<Token>();
		final char[] inputChars = input.toCharArray();
		int offset = 0;
		for (StartEndPair startEndPair : scan) {
			int plainTextLengthBeforeNextToken = startEndPair.start
					- splitStart.length() - offset;
			if (plainTextLengthBeforeNextToken != 0) {
				AbstractToken token = new PlainTextToken(Util.NO_QUOTE_MINI_PARSER.unescape(new String(inputChars,
						offset, plainTextLengthBeforeNextToken)));
				tokens.add(token);
			}
			offset = startEndPair.end + splitEnd.length();

			AbstractToken token = lexer.nextToken(inputChars,
					startEndPair.start, startEndPair.end);
			// == null means this is a comment or other skipable stuff
			if (token == null) {
				continue;
			}
			token.setSourceName(sourceName);
			tokens.add(token);
		}

		// do not forget to add the final chunk of pure text (might be the
		// only
		// chunk indeed)
		int remainingChars = input.length() - offset;
		if (remainingChars != 0) {
			AbstractToken token = new PlainTextToken(Util.NO_QUOTE_MINI_PARSER.unescape(new String(inputChars,
					offset, remainingChars)));
			tokens.add(token);
		}

        return tokens;
	}

	public Token nextToken() {
		if (currentTokenIndex < tokens.size()) {
			currentToken = tokens.get(currentTokenIndex++);
		} else {
			currentToken = null;
		}
		return currentToken;
	}

	public void consume() {
		nextToken();
	}

	public Token currentToken() {
		return currentToken;
	}

    private int getTokenIndex(Token token) {
        int index = 0;
        for (Token t: tokens) {
            if (t == token) {
                return index;
            }
            index ++;
        }
        return -1;
    }

	public void rewind(Token tokenToRewindTo) {
		this.currentTokenIndex = getTokenIndex(tokenToRewindTo) + 1;
		consume();
	}
}
