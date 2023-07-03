import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
	ContextConfig contextConfig;

	@BeforeEach
	void setUp() {
		contextConfig = new ContextConfig();
	}

	@Nested
	class ComponentConstruction {


		@Test
		public void should_bind_type_to_a_specific_instance() {
			Component component = new Component() {};
			contextConfig.bind(Component.class, component);
			assertSame(component, contextConfig.getContext().get(Component.class).get());
		}

		@Nested
		class ConstructorInjection {


			@Test
			public void should_return_empty_if_component_is_not_existed() {
				Optional<Component> component = contextConfig.getContext().get(Component.class);
				assertTrue(component.isEmpty());
			}

			@Test
			public void should_bind_type_to_a_class_with_default_constructor() {
				contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component component = contextConfig.getContext().get(Component.class).get();
				assertNotNull(component);
				assertTrue(component instanceof ComponentWithDefaultConstructor);
			}


			@Test
			public void should_bind_type_to_a_class_with_inject_constructor() {
				Dependency dependency = new Dependency() {};
				contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
				contextConfig.bind(Dependency.class, dependency);
				Component component = contextConfig.getContext().get(Component.class).get();
				assertNotNull(component);
				assertSame(dependency, ((ComponentWithInjectConstructor) component).dependency);
			}

			@Test
			public void should_bind_type_to_a_class_with_transitive_dependency() {
				contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
				contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
				contextConfig.bind(String.class, "indirect dependency");

				Component component = contextConfig.getContext().get(Component.class).get();
				assertNotNull(component);
				Dependency dependency = ((ComponentWithInjectConstructor) component).dependency;
				assertNotNull(dependency);
				assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).dependency);
			}

			//TODO: multi inject constructors;
			@Test
			public void should_throw_exception_if_multi_inject_constructors_provided() {
				assertThrows(IllegalComponentException.class, () -> contextConfig.bind(Component.class, ComponentWithMultiInjectConstructor.class));
			}


			//TODO: no inject nor default constructors
			@Test
			public void should_throw_exception_if_no_inject_nor_default_constructors_provided() {
				assertThrows(IllegalComponentException.class, () -> contextConfig.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));
			}

			@Test
			public void should_throw_exception_if_dependency_not_found() {
				contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
				DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());
				assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
				assertThat(dependencyNotFoundException.getComponent()).isEqualTo(Component.class);
			}

			@Test
			public void should_throw_exception_if_cyclic_dependency_occur() {
				contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
				contextConfig.bind(Dependency.class, DependencyDependentOnComponent.class);
				CyclicDependencyException cyclicDependencyException = assertThrows(CyclicDependencyException.class, () -> contextConfig.getContext());
				List<Class<?>> components = cyclicDependencyException.getDependencies();
				assertThat(components.size()).isEqualTo(2);
				assertTrue(components.contains(Component.class));
				assertTrue(components.contains(Dependency.class));
			}


		}

		@Nested
		class FieldInjection {

		}

		@Nested
		class MethodInjection {

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
	Dependency dependency;

	@Inject
	public ComponentWithInjectConstructor(Dependency dependency) {
		this.dependency = dependency;
	}
}

class DependencyDependentOnComponent implements Dependency {
	Component component;

	@Inject
	public DependencyDependentOnComponent(Component component) {
		this.component = component;
	}
}


class DependencyWithInjectConstructor implements Dependency {
	String dependency;

	@Inject
	public DependencyWithInjectConstructor(String dependency) {
		this.dependency = dependency;
	}
}

class ComponentWithMultiInjectConstructor implements Component {
	@Inject
	public ComponentWithMultiInjectConstructor(String value) {
	}

	@Inject
	public ComponentWithMultiInjectConstructor(Double value) {
	}
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {

	public ComponentWithNoInjectNorDefaultConstructor(String value, Double value1) {
	}
}
