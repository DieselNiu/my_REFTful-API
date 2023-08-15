import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {

	private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> componentClass, T instance) {
		providers.put(componentClass, (Provider<T>) () -> instance);
	}

	public <T, K extends T> void bind(Class<T> type, Class<K> implementation) {
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
