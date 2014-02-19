package com.floreysoft.jmte.token;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.util.Util;

public class ForEachToken extends ExpressionToken {
	public static final String FOREACH = "foreach";

	private final String varName;
	private final String separator;

	public ForEachToken(String expression, String varName, String separator) {
		super(expression);
		this.varName = varName;
		this.separator = separator != null ? separator : "";
	}

	@Override
	public String getText() {
		if (text == null) {
			text = FOREACH + " " + getExpression() + " " + varName
					+ ((separator == null || separator.isEmpty()) ? "" : " " + separator);
		}
		return text;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ForEachTokenIterator evaluate(TemplateContext context) {
		Object value = evaluatePlain(context);

		final Iterable<Object> iterable;
		if (value == null) {
			iterable = Collections.emptyList();
		} else if (value instanceof Map) {
			iterable = ((Map) value).entrySet();
		} else if (value instanceof Iterable) {
			iterable = ((Iterable) value);
		} else {
			List<Object> arrayAsList = Util.arrayAsList(value);
			if (arrayAsList != null) {
				iterable = arrayAsList;
			} else {
				// we have a single value here and simply wrap it in a List
				iterable = Collections.singletonList(value);
			}
		}

		return new ForEachTokenIterator(iterable.iterator());
	}

	public String getVarName() {
		return varName;
	}

	public String getSeparator() {
		return separator;
	}

    public class ForEachTokenIterator {
        private final Iterator<Object> iterator;
        private int index = -1;

        ForEachTokenIterator(Iterator<Object> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public boolean isLast() {
            return !iterator.hasNext();
        }

        public boolean isFirst() {
            return index == 0;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public Object advance() {
            index++;
            return iterator.next();
        }

    }
}
