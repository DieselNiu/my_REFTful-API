import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.*;

public class ContextConfig {

	private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
	private Map<Component, ComponentProvider<?>> components = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		providers.put(type, (ComponentProvider<T>) context -> instance);
	}

	public <T> void bind(Class<T> type, T instance, Annotation...  qualifiers) {
		for(Annotation qualifier: qualifiers) {
			components.put(new Component(type, qualifier), context -> instance);
		}
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation,Annotation... qualifiers) {
		for(Annotation qualifier : qualifiers){
			components.put(new Component(type,qualifier),new InjectionProvider<>(implementation));
		}
	}



	record Component(Class<?> type, Annotation qualifiers) {

	}


	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		providers.put(type, new InjectionProvider<>(implementation));
	}





	public Context getContext() {
		providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

		return new Context() {

			@Override
			public <T> Optional<T> get(Ref<T> ref) {
				if(ref.getQualifier() !=null) {
					return Optional.ofNullable(components.get(new Component(ref.getComponent(),ref.getQualifier()))).map(x -> (T) x.get(this));
				}
				if (ref.isContainer()) {
					if (ref.getContainer() != Provider.class) return Optional.empty();

					return (Optional<T>) Optional.ofNullable(providers.get(ref.getComponent())).map(
						provider -> (Provider<Object>) () -> (T) provider.get(this));
				}
				return Optional.ofNullable(providers.get(ref.getComponent())).map(x -> (T) x.get(this));
			}

		};
	}


	private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
		for (Context.Ref dependency : providers.get(component).getDependencies()) {
			if (!providers.containsKey(dependency.getComponent())) throw new DependencyNotFoundException(component, dependency.getComponent());
			if (!dependency.isContainer()) {
				if (visiting.contains(dependency.getComponent())) throw new CyclicDependenciesFoundException(visiting);
				visiting.add(dependency.getComponent());
				checkDependencies(dependency.getComponent(), visiting);
				visiting.pop();
			}
		}

	}


	interface ComponentProvider<T> {
		T get(Context context);

		default List<Context.Ref> getDependencies() {
			return List.of();
		}
	}


}
