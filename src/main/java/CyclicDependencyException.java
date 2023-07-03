import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependencyException extends RuntimeException {
	Set<Class<?>> dependencies = new HashSet<>();

	public CyclicDependencyException(List<Class<?>> visiting) {
		dependencies.addAll(visiting);
	}


	public List<Class<?>> getDependencies() {
		return dependencies.stream().toList();
	}
}
