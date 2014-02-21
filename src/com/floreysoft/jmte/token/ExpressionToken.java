package com.floreysoft.jmte.token;

import java.util.List;

import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.util.Util;

public abstract class ExpressionToken extends AbstractToken {
	private final List<String> segments;
	private final String expression;

	public ExpressionToken(String expression, int line, int column) {
        super(line, column);

		if (expression == null) {
			throw new IllegalArgumentException(
					"Parameter expression must not be null");
		}
        this.segments = Util.MINI_PARSER.split(expression, '.');
        this.expression = Util.MINI_PARSER.unescape(expression);
    }

	protected ExpressionToken(List<String> segments, String expression, int line, int column) {
        super(line, column);

		this.segments = segments;
		this.expression = expression;
	}

	public boolean isEmpty() {
		return getSegments().size() == 0;
	}

	public List<String> getSegments() {
		return segments;
	}

    public String getExpression() {
		return expression;
	}

	@Override
	public abstract Object evaluate(TemplateContext context);

	protected Object evaluatePlain(TemplateContext context) {
		final Object value = context.modelAdaptor.getValue(context, this, getSegments(), getExpression());
		return value;
	}
}
