import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependencyException extends RuntimeException {
	Set<Class<?>> dependencies = new HashSet<>();

	public CyclicDependencyException(Class<?> component) {
		dependencies.add(component);
	}

	public CyclicDependencyException(Class<?> type, Set<Class<?>> dependency) {
		dependencies.add(type);
		dependencies.addAll(dependency);
	}


	public List<Class<?>> getDependencies() {
		return dependencies.stream().toList();
	}
}
