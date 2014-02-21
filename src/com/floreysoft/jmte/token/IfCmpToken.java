package com.floreysoft.jmte.token;

import java.util.List;

import com.floreysoft.jmte.TemplateContext;


public class IfCmpToken extends IfToken {
	private final String operand;

	public IfCmpToken(String expression, String operand, boolean negated, int line, int column) {
		super(expression, negated, line, column);
		this.operand = operand;
	}

	public IfCmpToken(List<String> segments, String expression, String operand, boolean negated, int line, int column) {
		super(segments, expression, negated, line, column);
		this.operand = operand;
	}

	public String getOperand() {
		return operand;
	}

	@Override
	public String getText() {
        return String.format("%s %s='%s'", IF, getExpression(), getOperand());
	}

	@Override
	public Object evaluate(TemplateContext context) {
		Object value = evaluatePlain(context);
        if (value == null) {
            return false;
        }

		boolean condition = getOperand().equals(value.toString());
		return negated ? !condition : condition;
	}

}
