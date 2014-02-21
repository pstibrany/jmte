package com.floreysoft.jmte.token;

import com.floreysoft.jmte.TemplateContext;


public class ElseToken extends AbstractToken {
	public static final String ELSE = "else";

    public ElseToken(int line, int column) {
        super(line, column);
    }

    @Override
	public String getText() {
        return ELSE;
	}

	@Override
	public Object evaluate(TemplateContext context) {
        return "";
	}
}
