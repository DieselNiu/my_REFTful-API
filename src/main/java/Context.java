import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public class Context {

	private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> componentClass, T instance) {
		providers.put(componentClass, (Provider<T>) () -> instance);
	}

	public <T, K extends T> void bind(Class<T> type, Class<K> implementation) {
		Constructor<K> injectConstructors = getInjectConstructors(implementation);
		providers.put(type, new ConstructorInjectionProvider<>(type, injectConstructors));
	}


	class ConstructorInjectionProvider<T> implements Provider<T> {
		private final Class<?> componentType;
		private final Constructor<T> injectConstructors;
		private Boolean constructing = false;

		public ConstructorInjectionProvider(Class<?> componentType, Constructor<T> injectConstructors) {
			this.componentType = componentType;
			this.injectConstructors = injectConstructors;
		}

		@Override
		public T get() {
			if (constructing) throw new CyclicDependencyException(componentType);
			try {
				constructing = true;
				Object[] dependencies = stream(injectConstructors.getParameters()).map(p -> Context.this.get(p.getType()).orElseThrow(() -> new DependencyNotFoundException(componentType, p.getType()))).toArray(Object[]::new);
				return injectConstructors.newInstance(dependencies);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (CyclicDependencyException e) {
				throw new CyclicDependencyException(e.getComponents(), componentType);
			} finally {
				constructing = false;
			}
		}
	}

	private static <T> Constructor<T> getInjectConstructors(Class<T> implementation) {
		List<Constructor<?>> constructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
		if (constructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) constructors.stream().findFirst().orElseGet(() -> {
			try {
				return implementation.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new IllegalComponentException();
			}
		});
	}


	public <T> Optional<T> get(Class<T> type) {
		return ofNullable(providers.get(type)).map(c -> (T) c.get());
	}
}
