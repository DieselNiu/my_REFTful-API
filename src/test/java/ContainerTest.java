import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

public class ContainerTest {
	ContextConfig config;

	@BeforeEach
	void setUp() {
		config = new ContextConfig();
	}




	@Nested
	public class DependenciesSelection {
		@Nested
		public class ProviderType{
			//Context
			//TODO could get Provider<T> from context
			//InjectionProvider
			//TODO  support inject constructor
			//TODO support inject field
			//TODO support inject method
		}

	}

	@Nested
	public class LifecycleManagement {

	}

}

interface Component {

	Dependency dependency();
}

interface Dependency {

}

interface AnotherDependency {

}

interface TestComponent {

}




