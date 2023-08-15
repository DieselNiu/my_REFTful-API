import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

	private Context context;


	@BeforeEach
	void setUp() {
		context = new Context();
	}

	@Nested
	class ComponentConstructor {

		@Test
		public void should_bind_to_a_specific_instance() {
			Component instance = new Component() {};
			context.bind(Component.class, instance);
			assertSame(instance, context.get(Component.class).get());
		}

		@Test
		public void should_return_empty_if_component_not_exist() {
			Optional<Component> optionalComponent = context.get(Component.class);
			assertTrue(optionalComponent.isEmpty());
		}


		@Nested
		class ConstructorInjection {
			@Test
			public void should_bind_a_class_with_default_constructor() {
				context.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component component = context.get(Component.class).get();
				assertNotNull(component);
				assertTrue(component instanceof ComponentWithDefaultConstructor);
			}

			@Test
			public void should_bind_to_a_class_with_inject_constructor() {
				Dependency dependency = new Dependency() {};
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				context.bind(Dependency.class, dependency);
				Component component = context.get(Component.class).get();
				assertNotNull(component);
				assertSame(dependency, ((ComponentWithInjectConstructor) component).getDependency());
			}

			@Test
			public void should_bind_to_a_class_with_transitive_dependency() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				context.bind(Dependency.class, DependencyWithInjectConstructor.class);
				context.bind(String.class, "indirect dependency");
				Component component = context.get(Component.class).get();
				assertNotNull(component);
				Dependency dependency = ((ComponentWithInjectConstructor) component).getDependency();
				assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
			}

			@Test
			public void should_throw_exception_if_multi_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithMultiInjectConstructors.class));
			}

			@Test
			public void should_throw_exception_if_no_default_nor_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithNoDefaultNorInjectConstructor.class));
			}

			@Test
			public void should_throw_exception_if_dependency_not_found() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				assertThrows(DependencyNotFoundException.class, () -> context.get(Dependency.class).orElseThrow(DependencyNotFoundException::new));
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
