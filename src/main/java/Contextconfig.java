import java.util.*;

import static java.util.Optional.ofNullable;

public class Contextconfig {
	private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> componentClass, T instance) {
		providers.put(componentClass, new ComponentProvider<T>() {
			@Override
			public T get(Context context) {
				return instance;
			}

			@Override
			public List<Class<?>> getDependency() {
				return List.of();
			}
		});
	}

	public <T, K extends T> void bind(Class<T> type, Class<K> implementation) {
		providers.put(type, new ConstructorInjectionProvider<>(implementation));
	}


	public Context getContext() {
		providers.keySet().forEach(c -> checkDependency(c, new Stack<>()));
		return new Context() {
			@Override
			public <T> Optional<T> get(Class<T> type) {
				return ofNullable(providers.get(type)).map(c -> (T) c.get(this));
			}
		};
	}

	private void checkDependency(Class<?> component, Stack<Class<?>> visiting) {
		for (Class<?> dependency : providers.get(component).getDependency()) {
			if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
			if (visiting.contains(dependency)) throw new CyclicDependencyException(visiting);
			visiting.push(dependency);
			checkDependency(dependency, visiting);
			visiting.pop();
		}
	}
}
