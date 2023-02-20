import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ContextConfig {

	private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
	private Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		providers.put(type, (ComponentProvider<T>) context -> instance);
		dependencies.put(type,asList());
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		Constructor<I> constructor = getInjectConstructor(implementation);
		providers.put(type, new ConstructorInjection<>(type, constructor));
		dependencies.put(type, Arrays.stream(constructor.getParameters()).map(Parameter::getType).collect(Collectors.toList()));

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

	public Context getContext() {

          for (Class<?> component : dependencies.keySet()) {
			  for (Class<?> dependency: dependencies.get(component)) {
				  if(!dependencies.containsKey(dependency)) throw new DependencyNotFoundException(component,dependency);
			  }
          }


		return new Context() {
			@Override
			public <T> Optional<T> get(Class<T> type) {
				return Optional.ofNullable(providers.get(type)).map(x -> (T) x.get(this));
			}
		};
	}

	interface ComponentProvider<T> {
		T get(Context context);
	}


	class ConstructorInjection<T> implements ComponentProvider<T> {
		private Class<?> componentType;
		private Constructor<T> constructor;
		private boolean constructing = false;


		public ConstructorInjection(Class<?> component, Constructor<T> constructor) {
			this.componentType = component;
			this.constructor = constructor;
		}


		@Override
		public T get(Context context) {
			if (constructing) throw new CyclicDependenciesFoundException(componentType);
			try {
				constructing = true;
				Object[] dependencies = Arrays.stream(constructor.getParameters()).map(c -> context.get(c.getType()).orElseThrow(() -> new DependencyNotFoundException(componentType, c.getType()))).toArray(Object[]::new);
				return constructor.newInstance(dependencies);
			} catch (CyclicDependenciesFoundException e) {
				throw new CyclicDependenciesFoundException(componentType, e);
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			} finally {
				constructing = false;
			}
		}
	}
}
