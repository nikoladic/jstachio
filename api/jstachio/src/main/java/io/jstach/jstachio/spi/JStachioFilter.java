package io.jstach.jstachio.spi;

import java.io.IOException;
import java.util.List;

import io.jstach.jstachio.Template;
import io.jstach.jstachio.TemplateInfo;

/**
 * Advises or filters a previously applied template.
 *
 * @apiNote Implementations should be threadsafe!
 * @author agentgt
 *
 */
public interface JStachioFilter {

	/**
	 * A fully composed chain that renders a model by applying filtering.
	 *
	 * @apiNote The filter chain should be stateless and threadsafe as there is no
	 * guarantee that a filter chain will be recreated for the same {@link TemplateInfo}.
	 * @author agentgt
	 *
	 */
	public interface FilterChain {

		/**
		 * Renders the passed in model.
		 * @param model a model assumed never to be <code>null</code>.
		 * @param appendable the appendable to write to.
		 * @throws IOException if there is an error writing to the appendable
		 */
		public void process(Object model, Appendable appendable) throws IOException;

		/**
		 * A marker method that the filter is broken and should not be used. This mainly
		 * for the filter pipeline to determine if filter should be called.
		 * @param model the model that would be rendered
		 * @return by default false
		 */
		default boolean isBroken(Object model) {
			return false;
		}

	}

	/**
	 * Advises or filters a previously created filter.
	 * @param template info about the template
	 * @param previous the function returned early in the chain.
	 * @return an advised render function or often the previous render function if no
	 * advise is needed.
	 */
	FilterChain filter( //
			TemplateInfo template, //
			FilterChain previous);

	/**
	 * Applies filter with previous filter broken unless the parameter template is a
	 * {@link FilterChain} or is a{@link Template} which generated renderers usually are.
	 * @param template info about the template
	 * @return an advised render function or often the previous render function if no
	 * advise is needed.
	 */
	@SuppressWarnings("unchecked")
	default FilterChain filter( //
			TemplateInfo template) {
		FilterChain previous = BrokenFilter.INSTANCE;
		if (template instanceof FilterChain c) {
			previous = c;
		}
		else if (template instanceof @SuppressWarnings("rawtypes") Template t) {
			/*
			 * This is sort of abusing that filter chains happen to be a functional
			 * interface
			 */
			previous = (model, appendable) -> {
				t.execute(model, appendable);
			};
		}
		return filter(template, previous);
	}

}

enum BrokenFilter implements io.jstach.jstachio.spi.JStachioFilter.FilterChain {

	INSTANCE;

	@Override
	public void process(Object model, Appendable appendable) throws IOException {
		throw new IllegalStateException();
	}

	@Override
	public boolean isBroken(Object model) {
		return true;
	}

}

class CompositeFilterChain implements JStachioFilter {

	private final List<JStachioFilter> filters;

	public CompositeFilterChain(List<JStachioFilter> filters) {
		super();
		this.filters = filters;
	}

	@Override
	public FilterChain filter(TemplateInfo template, FilterChain previous) {
		var current = previous;
		for (var f : filters) {
			current = f.filter(template, current);
		}
		return current;
	}

}
