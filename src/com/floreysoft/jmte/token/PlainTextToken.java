package com.floreysoft.jmte.token;

import com.floreysoft.jmte.TemplateContext;

public class PlainTextToken extends AbstractToken {
    private final String text;

	public PlainTextToken(String text, int line, int column) {
        super(line, column);
        this.text = text;
	}

    @Override
    public String getText() {
        return text;
    }

    @Override
	public Object evaluate(TemplateContext context) {
		return getText();
	}
}
