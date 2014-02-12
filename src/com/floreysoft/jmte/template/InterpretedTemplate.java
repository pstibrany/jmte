package com.floreysoft.jmte.template;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.floreysoft.jmte.DefaultModelAdaptor;
import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.ModelAdaptor;
import com.floreysoft.jmte.ProcessListener;
import com.floreysoft.jmte.ProcessListener.Action;
import com.floreysoft.jmte.ScopedMap;
import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.token.ElseIfToken;
import com.floreysoft.jmte.token.ElseToken;
import com.floreysoft.jmte.token.EndToken;
import com.floreysoft.jmte.token.ExpressionToken;
import com.floreysoft.jmte.token.ForEachToken;
import com.floreysoft.jmte.token.IfToken;
import com.floreysoft.jmte.token.InvalidToken;
import com.floreysoft.jmte.token.PlainTextToken;
import com.floreysoft.jmte.token.StringToken;
import com.floreysoft.jmte.token.Token;
import com.floreysoft.jmte.token.TokenStream;

public class InterpretedTemplate implements Template {

    public static final String SPECIAL_ITERATOR_VARIABLE = "_it";
    public static final String ODD_PREFIX = "odd_";
    public static final String EVEN_PREFIX = "even_";
    public static final String LAST_PREFIX = "last_";
    public static final String FIRST_PREFIX = "first_";

    protected final Engine engine;
    protected final String template;
    protected final String sourceName;

    protected final List<Token> tokens;
    protected transient StringBuilder output;
    protected Set<String> usedVariables;

	public InterpretedTemplate(String template, String sourceName, Engine engine) {
        this.template = template;
        this.sourceName = sourceName;
        this.engine = engine;

        tokens = TokenStream.parseTokens(sourceName, template, engine.getExprStartToken(), engine.getExprEndToken());
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized Set<String> getUsedVariables() {
		if (this.usedVariables != null) {
			return this.usedVariables;
		}

		this.usedVariables = new TreeSet<String>();
		final Engine engine = new Engine();
		final ScopedMap scopedMap = new ScopedMap(Collections.EMPTY_MAP);

		ProcessListener processListener = new ProcessListener() {

			@Override
			public void log(TemplateContext context, Token token, Action action) {
				if (token instanceof ExpressionToken) {
					String variable = ((ExpressionToken) token).getExpression();
					if (!isLocal(context, variable)) {
						usedVariables.add(variable);
					}
				}
			}

			// a variable is local if any enclosing foreach has it as a step
			// variable
			private boolean isLocal(TemplateContext context, String variable) {
				for (Token token : context.scopes) {
					if (token instanceof ForEachToken) {
						String foreachVarName = ((ForEachToken) token)
								.getVarName();
						if (foreachVarName.equals(variable)) {
							return true;
						}
					}
				}
				return false;

			}

		};
		final Locale locale = Locale.getDefault();
		TemplateContext context = new TemplateContext(template, locale, sourceName, scopedMap,
				new DefaultModelAdaptor(), engine, engine.getErrorHandler(), processListener);

        TokenStream tokenStream = new TokenStream(tokens);

		transformPure(tokenStream, context);
		return usedVariables;
	}

    /**
         * Transforms a template into an expanded output using the given model.
         *
         * @param model
         *            the model used to evaluate expressions inside the template
         * @param modelAdaptor
         *            adaptor used for this transformation to look up values from
         *            model
         * @return the expanded output
         */
    @Override
	public synchronized String transform(Map<String, Object> model, Locale locale,
			ModelAdaptor modelAdaptor, ProcessListener processListener) {
		try {
			TemplateContext context = new TemplateContext(template, locale, sourceName, new ScopedMap(
					model), modelAdaptor, engine, engine.getErrorHandler(), processListener);

            TokenStream tokenStream = new TokenStream(tokens);

            return transformPure(tokenStream, context);
		} finally {
			output = null;
		}
	}

	protected String transformPure(TokenStream tokenStream, TemplateContext context) {
		output = new StringBuilder(
				(int) (context.template.length() * context.engine
						.getExpansionSizeFactor()));
		tokenStream.nextToken();
		while (tokenStream.currentToken() != null) {
			content(context, tokenStream, false);
		}
		return output.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void foreach(TemplateContext context, TokenStream tokenStream, boolean inheritedSkip) {
		ForEachToken feToken = (ForEachToken) tokenStream.currentToken();
		Iterable iterable = (Iterable) feToken.evaluate(context);
		feToken.setIterator(iterable.iterator());
		tokenStream.consume();

		context.model.enterScope();
		context.push(feToken);
		try {

			// in case we do not want to evaluate the body, we just do a quick
			// scan until the matching end
			if (inheritedSkip || !feToken.iterator().hasNext()) {
				Token contentToken;
				while ((contentToken = tokenStream.currentToken()) != null
						&& !(contentToken instanceof EndToken)) {
					content(context, tokenStream, true);
				}
				if (contentToken == null) {
					engine.getErrorHandler().error("missing-end", feToken);
				} else {
					tokenStream.consume();
					context.notifyProcessListener(contentToken, Action.END);
				}
			} else {

				while (feToken.iterator().hasNext()) {

					context.model.put(feToken.getVarName(), feToken.advance());
					addSpecialVariables(feToken, context.model);

					// for each iteration we need to rewind to the beginning
					// of the for loop
					tokenStream.rewind(feToken);
					Token contentToken;
					while ((contentToken = tokenStream.currentToken()) != null
							&& !(contentToken instanceof EndToken)) {
						content(context, tokenStream, false);
					}
					if (contentToken == null) {
						engine.getErrorHandler().error("missing-end", feToken);
					} else {
						tokenStream.consume();
						context.notifyProcessListener(contentToken, Action.END);
					}
					if (!feToken.isLast()) {
						output.append(feToken.getSeparator());
					}
				}
			}

		} finally {
			context.model.exitScope();
			context.pop();
		}
	}
	
	private boolean elseIfCondition(TemplateContext context, TokenStream tokenStream, boolean inheritedSkip) {
		ElseIfToken elseIfToken = (ElseIfToken) tokenStream.currentToken();
		tokenStream.consume();
		
		context.push(elseIfToken);
		
		boolean localSkip;
		try {
			if (inheritedSkip) {
				localSkip = true;
			} else {
				localSkip = !(Boolean) elseIfToken.evaluate(context);
			}
				
			Token contentToken;
			while((contentToken = tokenStream.currentToken()) != null
					&& !(contentToken instanceof EndToken)
					&& !(contentToken instanceof ElseToken)
					&& !(contentToken instanceof ElseIfToken)) {
				content(context, tokenStream, localSkip);
			}
		} finally {
			context.pop();
		}
		
		return localSkip;
	}

	private void condition(TemplateContext context, TokenStream tokenStream, boolean inheritedSkip) {
		IfToken ifToken = (IfToken) tokenStream.currentToken();
		tokenStream.consume();
		
		context.push(ifToken);
		try {
			boolean localSkip;
			if (inheritedSkip) {
				localSkip = true;
			} else {
				localSkip = !(Boolean) ifToken.evaluate(context);
			}
						
			Token contentToken;
			while ((contentToken = tokenStream.currentToken()) != null
					&& !(contentToken instanceof EndToken)
					&& !(contentToken instanceof ElseToken)
					&& !(contentToken instanceof ElseIfToken)) {
				content(context, tokenStream, localSkip);
			}
			
			boolean elseIfSkip = !localSkip;
			boolean inheritedElseIfSkip = elseIfSkip;
			while ((contentToken = tokenStream.currentToken()) != null
					&& contentToken instanceof ElseIfToken) {
				inheritedElseIfSkip = elseIfCondition(context, tokenStream, elseIfSkip);
				
				if (!inheritedElseIfSkip) {
					elseIfSkip = true;
				}
			}
			
			if (contentToken instanceof ElseToken) {
				tokenStream.consume();
				// toggle for else branch
				if (!inheritedSkip) {
					localSkip = !localSkip || elseIfSkip;
				}
				context.notifyProcessListener(contentToken,
						inheritedSkip ? Action.SKIP : Action.EVAL);

				while ((contentToken = tokenStream.currentToken()) != null
						&& !(contentToken instanceof EndToken)) {
					content(context, tokenStream, localSkip);
				}

			}

			if (contentToken == null) {
				engine.getErrorHandler().error("missing-end", ifToken);
			} else {
				tokenStream.consume();
				context.notifyProcessListener(contentToken, Action.END);
			}
		} finally {
			context.pop();
		}
	}

	private void content(TemplateContext context, TokenStream tokenStream, boolean skip) {
		Token token = tokenStream.currentToken();
		context.notifyProcessListener(token, skip ? Action.SKIP : Action.EVAL);
		if (token instanceof PlainTextToken) {
			tokenStream.consume();
			if (!skip) {
				output.append(token.getText());
			}
		} else if (token instanceof StringToken) {
			tokenStream.consume();
			if (!skip) {
				String expanded = (String) token.evaluate(context);
				output.append(expanded);
			}
		} else if (token instanceof ForEachToken) {
			foreach(context, tokenStream, skip);
		} else if (token instanceof ElseIfToken) {
			elseIfCondition(context, tokenStream, skip);
		} else if (token instanceof IfToken) {
			condition(context, tokenStream, skip);
		} else if (token instanceof ElseToken) {
			tokenStream.consume();
			engine.getErrorHandler().error("else-out-of-scope", token);
		} else if (token instanceof EndToken) {
			tokenStream.consume();
			engine.getErrorHandler().error("unmatched-end", token);
		} else if (token instanceof InvalidToken) {
			tokenStream.consume();
			engine.getErrorHandler().error("invalid-expression", token);
		} else {
			tokenStream.consume();
			// what ever else there may be, we just evaluate it
			String evaluated = (String) token.evaluate(context);
			output.append(evaluated);
		}

	}

	@Override
	public String toString() {
		return template;
	}

    protected void addSpecialVariables(ForEachToken feToken, Map<String, Object> model) {
		String suffix = feToken.getVarName();
		addSpecialVariables(feToken, model, suffix);

		// special _it variable as an alias for run variable in inner loop
		model.put(SPECIAL_ITERATOR_VARIABLE, model.get(feToken.getVarName()));
		addSpecialVariables(feToken, model, SPECIAL_ITERATOR_VARIABLE);
	}

    private void addSpecialVariables(ForEachToken feToken, Map<String, Object> model, String suffix) {
        model.put(FIRST_PREFIX + suffix, feToken.isFirst());
        model.put(LAST_PREFIX + suffix, feToken.isLast());
        model.put(EVEN_PREFIX + suffix, feToken.getIndex() % 2 == 0);
        model.put(ODD_PREFIX + suffix, feToken.getIndex() % 2 == 1);
    }

    public String transform(Map<String, Object> model, Locale locale, ProcessListener processListener) {
        return transform(model, locale, engine.getModelAdaptor(), processListener);
    }

    public String transform(Map<String, Object> model, Locale locale) {
        return transform(model, locale, engine.getModelAdaptor(), null);
    }
}
