package com.floreysoft.jmte;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.floreysoft.jmte.ProcessListener.Action;
import com.floreysoft.jmte.encoder.Encoder;
import com.floreysoft.jmte.token.Token;

/**
 * Holds the combined current state of a template during evaluation.
 * 
 * @author olli
 * 
 */
public class TemplateContext {

	public final ScopedMap model;
	/**
	 * Stack like hierarchy of structure giving tokens (if and foreach)
	 */
	public final List<Token> scopes;
	public final String template;
	public final Engine engine;
	public final String sourceName;
	public final ModelAdaptor modelAdaptor;
	public final Locale locale;
	public final ErrorHandler errorHandler;
	final ProcessListener processListener;

	public TemplateContext(String template, Locale locale, String sourceName, ScopedMap model,
			ModelAdaptor modelAdaptor, Engine engine, ErrorHandler errorHandler, ProcessListener processListener) {
		this.model = model;
		this.template = template;
		this.locale = locale;
		this.engine = engine;
		this.scopes = new ArrayList<Token>();
		this.sourceName = sourceName;
		this.modelAdaptor = modelAdaptor;
		this.errorHandler = errorHandler;
		this.processListener = processListener;
	}

	/**
	 * Pushes a token on the scope stack.
	 */
	public void push(Token token) {
		scopes.add(token);
	}

	/**
	 * Pops a token from the scope stack.
	 */
	public Token pop() {
		if (scopes.isEmpty()) {
			return null;
		} else {
			Token token = scopes.remove(scopes.size() - 1);
			return token;
		}
	}

	/**
	 * Allows you to send additional notifications of executed processing steps.
	 * 
	 * @param token
	 *            the token that is handled
	 * @param action
	 *            the action that is executed on the action
	 */
	public void notifyProcessListener(Token token, Action action) {
		if (processListener != null) {
			processListener.log(this, token, action);
		}
	}

	public <C> Renderer<C> resolveRendererForClass(Class<C> clazz) {
		return engine.getRendererRegistry().resolveRendererForClass(clazz);
	}

	public NamedRenderer resolveNamedRenderer(String rendererName) {
		return engine.getRendererRegistry().resolveNamedRenderer(rendererName);
	}
	
	public Encoder getEncoder() {
		return engine.getEncoder();
	}

}
