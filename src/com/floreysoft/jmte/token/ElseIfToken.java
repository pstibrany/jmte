package com.floreysoft.jmte.token;

import java.util.List;

public class ElseIfToken extends IfToken {

	public static final String ELSE_IF = "elseif";
	
	public ElseIfToken(List<String> segments, String expression, boolean negated, int line, int column) {
		super(segments, expression, negated, line, column);
	}
	
	public ElseIfToken(String ifExpression, boolean negated, int line, int column) {
		super(ifExpression, negated, line, column);
	}

	@Override
	public String getText() {
	    return ELSE_IF + " " + getExpression();
	}
}
