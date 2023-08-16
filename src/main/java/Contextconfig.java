import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public class Contextconfig {
	private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
	private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

	public <T> void bind(Class<T> componentClass, T instance) {
		providers.put(componentClass, context -> instance);
		dependencies.put(componentClass, List.of());
	}

	public <T, K extends T> void bind(Class<T> type, Class<K> implementation) {
		providers.put(type, new ConstructorInjectionProvider<>(type, implementation));
		dependencies.put(type, stream(getInjectConstructors(implementation).getParameters()).map(Parameter::getType).collect(Collectors.toList()));

	}

	interface ComponentProvider<T> {
		T get(Context context);
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

	class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
		private final Class<?> componentType;
		private final Constructor<T> injectConstructors;
		private Boolean constructing = false;

		public ConstructorInjectionProvider(Class<?> componentType, Class<T> implementation) {
			this.componentType = componentType;
			this.injectConstructors = getInjectConstructors(implementation);
		}


		@Override
		public T get(Context context) {
			if (constructing) throw new CyclicDependencyException(componentType);
			try {
				constructing = true;
				Object[] dependencies = stream(injectConstructors.getParameters()).map(p -> context.get(p.getType()).get()).toArray(Object[]::new);
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


	public Context getContext() {
		for (Class<?> component : dependencies.keySet()) {
			for (Class<?> dependency : dependencies.get(component)) {
				if (!dependencies.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
			}

		}
		return new Context() {
			@Override
			public <T> Optional<T> get(Class<T> type) {
				return ofNullable(providers.get(type)).map(c -> (T) c.get(this));
			}
		};
	}


}
