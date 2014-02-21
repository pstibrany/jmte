package com.floreysoft.jmte.token;

import com.floreysoft.jmte.TemplateContext;

public class InvalidToken extends AbstractToken {
    private final String text;

    public InvalidToken(String text, int line, int column) {
        super(line, column);
        this.text = text;
    }

    public Object evaluate(TemplateContext context) {
		context.engine.getErrorHandler().error("invalid-expression", this);
		return "";
	}

    @Override
    public String getText() {
        return text;
    }
}
