import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependenciesFoundException extends RuntimeException {
	private Set<Class<?>> components = new HashSet<>();

	public CyclicDependenciesFoundException(Class<?> componentType, CyclicDependenciesFoundException e) {
		components.add(componentType);
		components.addAll(e.components);
	}

	public CyclicDependenciesFoundException(List<Class<?>> visiting) {
		components.addAll(visiting);
	}

	public Class<?>[] getComponents() {
		return components.toArray(Class<?>[]::new);
	}
}
