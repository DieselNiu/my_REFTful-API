import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

	private Contextconfig contextconfig;


	@BeforeEach
	void setUp() {
		contextconfig = new Contextconfig();
	}

	@Nested
	public class ComponentConstructor {


		@Nested
		public class DependencyCheck {
			@Test
			public void should_throw_exception_if_dependency_not_found() {
				contextconfig.bind(Component.class, ComponentWithInjectConstructor.class);
				DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> contextconfig.getContext());
				assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
				assertEquals(Component.class, dependencyNotFoundException.getComponent());
			}

			@Test
			public void should_throw_exception_when_cyclic_dependency_occur() {
				contextconfig.bind(Component.class, ComponentWithInjectConstructor.class);
				contextconfig.bind(Dependency.class, DependencyDependentOnComponent.class);
				CyclicDependencyException cyclicDependencyException = assertThrows(CyclicDependencyException.class, () -> contextconfig.getContext());
				Set<Class<?>> components = cyclicDependencyException.getComponents();
				assertTrue(components.contains(Component.class));
				assertTrue(components.contains(Dependency.class));
			}
		}

		@Test
		public void should_bind_to_a_specific_instance() {
			Component instance = new Component() {};
			contextconfig.bind(Component.class, instance);
			Context context = contextconfig.getContext();
			assertSame(instance, context.get(Component.class).get());
		}

		@Test
		public void should_return_empty_if_component_not_exist() {
			Context context = contextconfig.getContext();
			Optional<Component> optionalComponent = context.get(Component.class);
			assertTrue(optionalComponent.isEmpty());
		}

	}

	@Nested
	class DependencySelection {

	}

	@Nested
	class LifeCycleManagement {

	}
}

interface Component {

}

interface Dependency {

}


class ComponentWithDefaultConstructor implements Component {
	public ComponentWithDefaultConstructor() {
	}
}


class ComponentWithInjectConstructor implements Component {
	private final Dependency dependency;

	@Inject
	public ComponentWithInjectConstructor(Dependency dependency) {
		this.dependency = dependency;
	}

	public Dependency getDependency() {
		return dependency;
	}
}

class DependencyWithInjectConstructor implements Dependency {
	private final String dependency;

	@Inject
	public DependencyWithInjectConstructor(String dependency) {
		this.dependency = dependency;
	}

	public String getDependency() {
		return dependency;
	}
}

class ComponentWithMultiInjectConstructors implements Component {
	@Inject
	public ComponentWithMultiInjectConstructors(String value) {
	}

	@Inject
	public ComponentWithMultiInjectConstructors(String value, Double d) {
	}
}

class ComponentWithNoDefaultNorInjectConstructor implements Component {

	public ComponentWithNoDefaultNorInjectConstructor(String value, Double d) {
	}
}

class DependencyDependentOnComponent implements Dependency {
	private final Component component;

	@Inject
	public DependencyDependentOnComponent(Component component) {
		this.component = component;
	}

	public Component getComponent() {
		return component;
	}
}
