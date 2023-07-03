import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {

	private Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> type, T component) {
		providers.put(type, (Provider<T>) () -> component);

	}

	public <T> T get(Class<T> type) {
		return (T) providers.get(type).get();

	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		providers.put(type, (Provider<T>) () -> {
			try {
				return (T) ((Class<?>) implementation).getConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
}
