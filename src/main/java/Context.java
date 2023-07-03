import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Context {

	private Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> type, T component) {
		providers.put(type, (Provider<T>) () -> component);

	}


	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		Constructor<I> injectConstructor = getInjectConstructor(implementation);
		providers.put(type, (Provider<T>) () -> {
			try {
				Object[] dependencies = Arrays.stream(injectConstructor.getParameters()).map(p -> get(p.getType()).orElseThrow(DependencyNotFoundException::new)).toArray(Object[]::new);
				return (T) injectConstructor.newInstance(dependencies);
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
		if (injectConstructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
			try {
				return implementation.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new IllegalComponentException();
			}
		});
	}


	public <T> Optional<T> get(Class<T> type) {
		return Optional.ofNullable(providers.get(type)).map(c -> (T) c.get());
	}
}
