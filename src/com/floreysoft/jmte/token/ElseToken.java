package com.floreysoft.jmte.token;

import com.floreysoft.jmte.TemplateContext;


public class ElseToken extends AbstractToken {
	public static final String ELSE = "else";

	@Override
	public String getText() {
		if (text == null) {
			text = ELSE;
		}
		return text;
	}

	@Override
	public Object evaluate(TemplateContext context) {
        return "";
	}

	@Override
	public String emit() {
		return ELSE;
	}
}
