package com.floreysoft.jmte.token;

import com.floreysoft.jmte.ModelBuilder;
import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.Renderer;
import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.encoder.Encoder;
import com.floreysoft.jmte.renderer.RawRenderer;

public class StringToken extends ExpressionToken {
	// ${<h1>,address(NIX),</h1>;long(full)}
    private final String rawText;

	private final String defaultValue; // NIX
	private final String prefix; // <h1>
	private final String suffix; // </h1>
	private final String rendererName; // long
	private final String parameters; // full

	public StringToken(int line, int column) {
		this("", "", null, null, null, null, null, line, column);
	}

	public StringToken(String rawText, String variableName, String defaultValue,
			String prefix, String suffix, String rendererName, String parameters, int line, int column)
    {
		super(variableName, line, column);
        this.rawText = rawText;
		this.defaultValue = defaultValue;
		this.prefix = prefix;
		this.suffix = suffix;
		this.rendererName = rendererName;
		this.parameters = parameters;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object evaluate(TemplateContext context) {
		boolean rawRendering = false;
		final Object value = evaluatePlain(context);

		final String renderedResult;
		if (value == null || value.equals("")) {
			renderedResult = defaultValue != null ? defaultValue : "";
		} else {
			String namedRendererResult = null;
			if (rendererName != null && !rendererName.equals("")) {
				final NamedRenderer rendererForName = context
						.resolveNamedRenderer(rendererName);
				if (rendererForName != null) {
					if (rendererForName instanceof RawRenderer) {
						rawRendering = true;
					}
					namedRendererResult = rendererForName.render(value, parameters, context.locale);
				}
			}
			if (namedRendererResult != null) {
				renderedResult = namedRendererResult;
			} else {
				final Renderer<Object> rendererForClass = (Renderer<Object>) context
						.resolveRendererForClass(value.getClass());
				if (rendererForClass != null) {
					if (rendererForClass instanceof RawRenderer) {
						rawRendering = true;
					}
					renderedResult = rendererForClass.render(value, context.locale);
				} else {
                    context.errorHandler.error("no-renderer-for-class", this, new ModelBuilder("clazz", value.getClass().getName()).build());
                    renderedResult = null;
				}
			}
		}

		if (renderedResult == null || renderedResult.equals("")) {
			return renderedResult;
		} else {
			final String prefixedRenderedResult = (prefix != null ? prefix : "") + renderedResult + (suffix != null ? suffix : "");
			Encoder encoder = context.getEncoder();
			if (!rawRendering && encoder != null) {
				final String encodedPrefixedRenderedResult = encoder.encode(prefixedRenderedResult);
				return encodedPrefixedRenderedResult;
			} else {
				return prefixedRenderedResult;
			}
		}
	}

    @Override
    public String getText() {
        return rawText;
    }
}
