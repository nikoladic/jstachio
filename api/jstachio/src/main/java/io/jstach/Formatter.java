package io.jstach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import io.jstach.context.ContextNode;

/**
 * Formats and then sends the results to the downstream appender.
 *
 * Implementations should be singleton like and should not contain state.
 *
 * @apiNote Although the formatter has access to the raw {@link Appendable} the formatter
 * should never use it directly and simply pass it on to the downstream appender.
 * @author agentgt
 *
 */
public interface Formatter extends Function<Object, String> {

	@Override
	default String apply(Object t) {
		StringBuilder sb = new StringBuilder();
		try {
			format(Appender.StringAppender.INSTANCE, sb, "", Object.class, t);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return sb.toString();
	}

	/**
	 * Formats the object and then sends the results to the downstream appender.
	 *
	 *
	 * @apiNote Although the formatter has access to the raw {@link Appendable} the
	 * formatter should never use it directly and simply pass it on to the downstream
	 * appender.
	 * @param <A> the appendable type
	 * @param <APPENDER> the downstream appender type
	 * @param downstream the downstream appender to be used instead of the appendable
	 * directly
	 * @param a the appendable to be passed to the appender
	 * @param path the dotted mustache like path
	 * @param c the object class
	 * @param o the object maybe null.
	 * @throws IOException
	 */
	<A extends Appendable, APPENDER extends Appender<A>> //
	void format(APPENDER downstream, A a, String path, Class<?> c, @Nullable Object o) throws IOException;

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			char c) throws IOException {
		downstream.append(a, c);
	}

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			short s) throws IOException {
		downstream.append(a, s);
	}

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			int i) throws IOException {
		downstream.append(a, i);
	}

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			long l) throws IOException {
		downstream.append(a, l);
	}

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			double d) throws IOException {
		downstream.append(a, d);
	}

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			boolean b) throws IOException {
		downstream.append(a, b);
	}

	default <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a, String path,
			String s) throws IOException {
		downstream.append(a, s);
	}

	/**
	 * Default formatters.
	 *
	 * @author agentgt
	 */
	enum DefaultFormatter implements Formatter {

		/**
		 * Default formatter.
		 *
		 * Unlike the mustache spec it will throw a NPE trying to form at null objects
		 *
		 *
		 */
		DEFAULT_FORMATTER {
			@Override
			public <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a,
					String path, Class<?> c, @Nullable Object o) throws IOException {
				if (o == null) {
					throw new NullPointerException("null at: " + path);
				}
				else if (o instanceof ContextNode m) {
					downstream.append(a, m.renderString());
				}
				else {
					downstream.append(a, String.valueOf(o));
				}
			}
		},
		/**
		 * Formatter that follows the spec rules that if a variable is a missing it will
		 * be an empty string (ie NOOP).
		 */
		SPEC_FORMATTER {
			@Override
			public <A extends Appendable, APPENDER extends Appender<A>> void format(APPENDER downstream, A a,
					String path, Class<?> c, @Nullable Object o) throws IOException {
				if (o instanceof ContextNode m) {
					downstream.append(a, m.renderString());
				}
				else if (o != null) {
					downstream.append(a, String.valueOf(o));
				}

			}
		}

	}

}
