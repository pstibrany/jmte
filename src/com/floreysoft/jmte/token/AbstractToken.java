package com.floreysoft.jmte.token;

public abstract class AbstractToken implements Token {

	protected final int line;
	protected final int column;

	public AbstractToken(int line, int column) {
        this.line = line;
        this.column = column;
	}

	public abstract String getText();

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return getText();
	}
}