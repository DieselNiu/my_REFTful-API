import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

public class ContextConfig {

		 private Map<Component, ComponentProvider<?>> components = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
	}

	public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
		for (Annotation qualifier : qualifiers) {
			components.put(new Component(type, qualifier), context -> instance);
		}
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation, Annotation... qualifiers) {
		for (Annotation qualifier : qualifiers) {
			components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
		}
	}


	record Component(Class<?> type, Annotation qualifiers) {

	}


	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		components.put(new Component(type, null), new InjectionProvider<>(implementation));
	}


	public Context getContext() {
		components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

		return new Context() {

			@Override
			public <T> Optional<T> get(Ref<T> ref) {
				if (ref.isContainer()) {
					if (ref.getContainer() != Provider.class) return Optional.empty();

					return (Optional<T>) Optional.ofNullable(getProvider(ref)).map(
						provider -> (Provider<Object>) () -> (T) provider.get(this));
				}
				return Optional.ofNullable(getProvider(ref)).map(x -> (T) x.get(this));
			}

		};
	}

	private <T> ComponentProvider<?> getProvider(Context.Ref<T> ref) {
		return components.get(new Component(ref.getComponent(), ref.getQualifier()));
	}


	private void checkDependencies(Component component, Stack<Class<?>> visiting) {
		for (Context.Ref dependency : components.get(component).getDependencies()) {
			if (!components.containsKey(new Component(dependency.getComponent(),dependency.getQualifier()))) throw new DependencyNotFoundException(component.type(), dependency.getComponent());
			if (!dependency.isContainer()) {
				if (visiting.contains(dependency.getComponent())) throw new CyclicDependenciesFoundException(visiting);
				visiting.add(dependency.getComponent());
				checkDependencies(new Component(dependency.getComponent(),dependency.getQualifier()), visiting);
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
