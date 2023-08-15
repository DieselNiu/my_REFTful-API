import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class Context {

	private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> componentClass, T instance) {
		providers.put(componentClass, (Provider<T>) () -> instance);
	}

	public <T, K extends T> void bind(Class<T> type, Class<K> implementation) {
		List<Constructor<?>> constructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
		if (constructors.size() > 1) throw new IllegalComponentException();
		if (constructors.isEmpty() && stream(implementation.getConstructors()).noneMatch(c -> c.getParameters().length == 0))
			throw new IllegalComponentException();
		providers.put(type, (Provider<T>) () -> {
			try {
				Constructor<K> injectConstructors = getInjectConstructors(implementation);
				Object[] dependencies = stream(injectConstructors.getParameters()).map(p -> get(p.getType())).toArray(Object[]::new);
				return injectConstructors.newInstance(dependencies);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static <T> Constructor<T> getInjectConstructors(Class<T> implementation) {
		return (Constructor<T>) stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).findFirst().orElseGet(() -> {
			try {
				return implementation.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public <T> T get(Class<T> type) {
		return (T) providers.get(type).get();
	}


}
