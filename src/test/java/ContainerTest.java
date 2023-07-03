import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
	Context context;

	@BeforeEach
	void setUp() {
		context = new Context();
	}

	@Nested
	class ComponentConstruction {


		@Test
		public void should_bind_type_to_a_specific_instance() {
			Component component = new Component() {};
			context.bind(Component.class, component);
			assertSame(component, context.get(Component.class).orElseThrow(DependencyNotFoundException::new));
		}

		@Nested
		class ConstructorInjection {


			@Test
			public void should_return_empty_if_component_is_not_existed() {
				Optional<Component> component = context.get(Component.class);
				assertTrue(component.isEmpty());
			}

			@Test
			public void should_bind_type_to_a_class_with_default_constructor() {
				context.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component component = context.get(Component.class).orElseThrow(DependencyNotFoundException::new);
				assertNotNull(component);
				assertTrue(component instanceof ComponentWithDefaultConstructor);
			}


			@Test
			public void should_bind_type_to_a_class_with_inject_constructor() {
				Dependency dependency = new Dependency() {};
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				context.bind(Dependency.class, dependency);
				Component component = context.get(Component.class).orElseThrow(DependencyNotFoundException::new);
				assertNotNull(component);
				assertSame(dependency, ((ComponentWithInjectConstructor) component).dependency);
			}

			@Test
			public void should_bind_type_to_a_class_with_transitive_dependency() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				context.bind(Dependency.class, DependencyWithInjectConstructor.class);
				context.bind(String.class, "indirect dependency");

				Component component = context.get(Component.class).orElseThrow(DependencyNotFoundException::new);
				assertNotNull(component);
				Dependency dependency = ((ComponentWithInjectConstructor) component).dependency;
				assertNotNull(dependency);
				assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).dependency);
			}

			//TODO: multi inject constructors;
			@Test
			public void should_throw_exception_if_multi_inject_constructors_provided() {
				assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithMultiInjectConstructor.class));
			}


			//TODO: no inject nor default constructors
			@Test
			public void should_throw_exception_if_no_inject_nor_default_constructors_provided() {
				assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));
			}

			@Test
			public void should_throw_exception_if_dependency_not_found() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class).orElseThrow(DependencyNotFoundException::new));
			}

			@Test
			@Disabled
			public void should_throw_exception_if_cyclic_dependency_occur() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				context.bind(Dependency.class, DependencyDependentOnComponent.class);
				assertThrows(CyclicDependencyException.class, () -> context.get(Component.class));
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
