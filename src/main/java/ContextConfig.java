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
			public <T> Optional<T> get(Class<T> type) {
				return Optional.ofNullable(providers.get(type)).map(x -> (T) x.get(this));
			}
		};
	}

	interface ComponentProvider<T> {
		T get(Context context);

		default List<Class<?>> getDependencies(){
			return of();
		};
	}

	private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
		for (Class<?> dependency : providers.get(component).getDependencies()) {
			if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
			if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
			visiting.add(dependency);
			checkDependencies(dependency, visiting);
			visiting.pop();
		}

	}


}
