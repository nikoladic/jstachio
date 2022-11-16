package io.jstach.jstachio.spi;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import io.jstach.jstache.JStache;
import io.jstach.jstachio.Renderer;
import io.jstach.jstachio.TemplateInfo;

/**
 * An SPI extension point via the {@link ServiceLoader} that is a factory that provides
 * services. All methods are optional (default) so that implementations can decide what
 * particularly plugins/services they want to provide.
 *
 * TODO this still in the works and subject to change greatly!
 *
 * @author agentgt
 *
 */
public interface JStacheServices {

	/**
	 * Provide a filter or not. The final filter is a composite and becomes a filter
	 * chain.
	 * @return filter if this service provider provies one or <code>null</code>
	 */
	default @Nullable JStacheFilter provideFilter() {
		return null;
	}

	/**
	 * Provide a config or not. The final config is a composite of all the found configs.
	 * <p>
	 * Specifically if multiple instances of {@link JStacheServices} are found that return
	 * a nonnull they will be combined by looping through all of them to find a nonnull
	 * value for {@link JStacheConfig#getProperty(String)}. If no configs are provided or
	 * no services found the root {@link JStacheServices} instance will use
	 * {@link System#getProperties()}.
	 *
	 * @apiNote This method is called before {@link #init(JStacheConfig)}
	 * @return config if this service provides one or <code>null</code>
	 */
	default @Nullable JStacheConfig provideConfig() {
		return null;
	}

	/**
	 * Called before the services are used but after {@link #provideConfig()}.
	 * @param config the config never null
	 */
	default void init(JStacheConfig config) {

	}

	/**
	 * Find the root service which is an aggregate of all found implementations.
	 * @return the root service never <code>null</code>.
	 */
	public static JStacheServices find() {
		return JStacheServicesResolver.INSTANCE;
	}

	/**
	 * Find all implementations minus the root aggregate.
	 * @return all custom implementations.
	 */
	public static Stream<JStacheServices> findAll() {
		return JStacheServicesResolver._services();
	}

	/**
	 * Finds a specific implementation using {@link Class#isAssignableFrom(Class)}.
	 * @param <T> the implementation type
	 * @param c the implementation type.
	 * @return an implementation if found
	 */
	public static <T extends JStacheServices> Optional<T> find(Class<T> c) {
		return findAll().filter(s -> c.isAssignableFrom(s.getClass())).map(c::cast).findFirst();
	}

	/**
	 * Resolve template information if possible.
	 * <p>
	 * Implementations are only called if the compiled renderers is missing.
	 * @param contextType models class.
	 * @return resolved template info
	 * @throws Exception if any problem happens creating template information
	 */
	public static TemplateInfo templateInfo(Class<?> contextType) throws Exception {
		return JStacheServicesResolver._templateInfo(contextType);
	}

	/**
	 * Finds a renderer for a model class.
	 * @param <T> the type of model.
	 * @param modelType the class of the model (annotated with {@link JStache})
	 * @return renderer for the specific type.
	 * @throws RuntimeException if the renderer is not found for the type.
	 */
	public static <T> Renderer<T> renderer(Class<T> modelType) {
		return JStacheServicesResolver._renderer(modelType);
	}

}