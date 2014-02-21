package com.floreysoft.jmte.token;

import com.floreysoft.jmte.TemplateContext;


public class EndToken extends AbstractToken {
	public static final String END = "end";

    public EndToken(int line, int column) {
        super(line, column);
    }

    @Override
	public String getText() {
        return END;
	}

	@Override
	public Object evaluate(TemplateContext context) {
		return "";
	}
}
