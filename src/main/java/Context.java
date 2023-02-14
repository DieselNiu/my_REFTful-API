import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
	private Map<Class<?>, Class<?>> implementations = new HashMap<>();

	private Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		providers.put(type, (Provider<T>) () -> instance);
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

	public <T> T get(Class<T> type) {
		return (T) providers.get(type).get();
	}


}
