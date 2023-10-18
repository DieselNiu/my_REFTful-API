import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.*;

public class ContextConfig {

	private Map<ComponentTest, ComponentProvider<?>> components = new HashMap<>();

//	public <T> void bind(Class<T> type, T instance) {
//		components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
//	}

	public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
		if(Arrays.stream(qualifiers).anyMatch(q-> !q.annotationType().isAnnotationPresent(Qualifier.class)))
			throw new IllegalComponentException();
		if (qualifiers.length == 0)
			components.put(new ComponentTest(type, null), (ComponentProvider<T>) context -> instance);
		for (Annotation qualifier : qualifiers) {
			components.put(new ComponentTest(type, qualifier), context -> instance);
		}
	}


	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		components.put(new ComponentTest(type, null), new InjectionProvider<>(implementation));
	}


	public <T, I extends T> void bind(Class<T> type, Class<I> implementation, Annotation... qualifiers) {
		if(Arrays.stream(qualifiers).anyMatch(q->!q.annotationType().isAnnotationPresent(Qualifier.class)))
			throw new IllegalComponentException();
		for (Annotation qualifier : qualifiers) {
			components.put(new ComponentTest(type, qualifier), new InjectionProvider<>(implementation));
		}
	}



	public Context getContext() {
		components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

		return new Context() {

			@Override
			public <T> Optional<T> get(ComponentRef<T> ref) {
				if (ref.isContainer()) {
					if (ref.getContainer() != Provider.class) return Optional.empty();

					return (Optional<T>) Optional.ofNullable(getProvider(ref)).map(
						provider -> (Provider<Object>) () -> (T) provider.get(this));
				}
				return Optional.ofNullable(getProvider(ref)).map(x -> (T) x.get(this));
			}

		};
	}

	private <T> ComponentProvider<?> getProvider(ComponentRef<T> ref) {
		return components.get(ref.component());
	}


	private void checkDependencies(ComponentTest component, Stack<Class<?>> visiting) {
		for (ComponentRef dependency : components.get(component).getDependencies()) {
			if (!components.containsKey(dependency.component())) throw new DependencyNotFoundException(component.type(), dependency.getComponentType());
			if (!dependency.isContainer()) {
				if (visiting.contains(dependency.getComponentType())) throw new CyclicDependenciesFoundException(visiting);
				visiting.add(dependency.getComponentType());
				checkDependencies(dependency.component(), visiting);
				visiting.pop();
			}
		}

	}


	interface ComponentProvider<T> {
		T get(Context context);

		default List<ComponentRef> getDependencies() {
			return List.of();
		}
	}


}
