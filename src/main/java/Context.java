import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Context {

	private Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		providers.put(type, (Provider<T>) () -> instance);
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		Constructor<I> constructor = getInjectConstructor(implementation);
		providers.put(type, new ConstructorInjection<>(type,constructor));
	}


	private <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
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
		return Optional.ofNullable(providers.get(type)).map(x -> (T) x.get());
	}

	class ConstructorInjection<T> implements Provider<T> {
		private Class<?> componentType;
		private Constructor<T> constructor;
		private boolean constructing = false;


		public ConstructorInjection(Class<?> component, Constructor<T> constructor) {
			this.componentType = component;
			this.constructor = constructor;
		}

		@Override
		public T get() {
			if (constructing) throw new CyclicDependenciesFoundException(componentType);
			try {
				constructing = true;
				Object[] dependencies = Arrays.stream(constructor.getParameters()).map(c -> Context.this.get(c.getType()).orElseThrow(() -> new DependencyNotFoundException(componentType, c.getType()))).toArray(Object[]::new);
				return constructor.newInstance(dependencies);
			}catch (CyclicDependenciesFoundException e) {
				throw new CyclicDependenciesFoundException(componentType,e);
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			} finally {
				constructing = false;
			}
		}
	}
}
