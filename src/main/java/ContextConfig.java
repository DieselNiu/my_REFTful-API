import java.util.*;

import static java.util.Arrays.asList;

public class ContextConfig {
	private final Map<Class<?>, ComponentProvider<?>> componentProviders = new HashMap<>();

	public <T> void bind(Class<T> type, T component) {
		componentProviders.put(type, new ComponentProvider<T>() {
			@Override
			public T get(Context context) {
				return component;
			}

			@Override
			public List<Class<?>> getDependency() {
				return asList();
			}
		});
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		componentProviders.put(type, new ConstructorInjectionProvider<>(implementation));
	}

	public Context getContext() {
		//checkDependencies
		componentProviders.keySet().forEach(c -> checkDependencies(c, new Stack<>()));
		return new Context() {
			@Override
			public <T> Optional<T> get(Class<T> type) {
				return Optional.ofNullable(componentProviders.get(type)).map(c -> (T) c.get(this));
			}
		};

	}


	private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
		for (Class<?> dependency : componentProviders.get(component).getDependency()) {
			if (!componentProviders.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
			if (visiting.contains(dependency)) throw new CyclicDependencyException(visiting);
			visiting.push(dependency);
			checkDependencies(dependency, visiting);
			visiting.pop();
		}

	}

}
