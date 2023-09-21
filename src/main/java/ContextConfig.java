import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {

	private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		providers.put(type, (ComponentProvider<T>) context -> instance);
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		providers.put(type, new InjectionProvider<>(implementation));
	}


	public Context getContext() {
		providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

		return new Context() {

			@Override
			public Optional get(Type type) {
				if (isContainerType(type)) return getContainerType((ParameterizedType) type);
				return getComponent((Class<?>) type);
			}

			private <T> Optional<T> getComponent(Class<T> type) {
				return Optional.ofNullable(providers.get(type)).map(x -> (T) x.get(this));
			}

			private Optional getContainerType(ParameterizedType type) {
				if (type.getRawType() != Provider.class) return Optional.empty();
				return Optional.ofNullable(providers.get(getComponentType(type))).map(
					provider -> (Provider<Object>) () -> provider.get(this));
			}
		};
	}

	private static Class<?> getComponentType(Type type) {
		return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
	}

	private static boolean isContainerType(Type type) {
		return type instanceof ParameterizedType;
	}

	interface ComponentProvider<T> {
		T get(Context context);

		default List<Type> getDependency() {
			return of();
		}
	}

	private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
		for (Type dependency : providers.get(component).getDependency()) {
			if (isContainerType(dependency)) checkContainerDependency(component, dependency);
			else checkComponentDependency(component, visiting, (Class<?>) dependency);
		}

	}

	private void checkContainerDependency(Class<?> component, Type dependency) {
		if (!providers.containsKey(getComponentType(dependency))) throw new DependencyNotFoundException(component, getComponentType(dependency));
	}

	private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
		if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
		if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
		visiting.add(dependency);
		checkDependencies(dependency, visiting);
		visiting.pop();
	}


}
