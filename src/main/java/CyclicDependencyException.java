import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyException extends RuntimeException {
	private final Set<Class<?>> components = new HashSet<>();

	public CyclicDependencyException(Class<?> component) {
		components.add(component);
	}

	public CyclicDependencyException(Set<Class<?>> componentset, Class<?> componentType) {
		components.add(componentType);
		components.addAll(componentset);
	}

	public Set<Class<?>> getComponents() {
		return components;
	}
}
