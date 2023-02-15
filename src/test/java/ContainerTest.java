import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
	//TODO: instance
	//TODO: interface
	//TODO: abstract class

	Context context;

	@BeforeEach
	void setUp() {
		context = new Context();
	}

	@Test
	public void should_bind_type_to_a_specific_instance() {
		Component instance = new Component() {};
		context.bind(Component.class, instance);
		assertSame(instance, context.get(Component.class).orElseThrow(DependencyNotFoundException::new));

	}


	@Nested
	public class ComponentConstructor {
		@Nested
		public class ConstructorInjection {
			@Test
			public void should_bind_type_to_a_class_with_default_constructor() {
				context.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component instance = context.get(Component.class).get();
				assertNotNull(instance);
				assertTrue(instance instanceof ComponentWithDefaultConstructor);
			}


			@Test
			public void should_bind_type_to_a_class_with_inject_constructor() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				Dependency dependency = new Dependency() {};
				context.bind(Dependency.class, dependency);
				Component instance = context.get(Component.class).get();
				assertNotNull(instance);
				Dependency dependency1 = ((ComponentWithInjectConstructor) instance).getDependency();
				assertNotNull(dependency1);
				assertSame(dependency, dependency1);
			}

			@Test
			public void should_bind_type_to_a_class_with_transitive_dependency() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				context.bind(Dependency.class, DependencyWithInjectConstructor.class);
				context.bind(String.class, "indirect dependency");


				Component instance = context.get(Component.class).orElseThrow(DependencyNotFoundException::new);
				assertNotNull(instance);
				Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
				assertNotNull(dependency);
				assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
			}

			@Test
			public void should_throw_exception_if_multi_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithMultiInjectConstructor.class));
			}

			@Test
			public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));
			}

			@Test
			public void should_throw_exception_if_dependency_not_exist() {
				context.bind(Component.class, ComponentWithInjectConstructor.class);
				assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class).get());
			}

			@Test
			public void should_() {
				Optional<Component> component = context.get(Component.class);
				assertTrue(component.isEmpty());
			}

		}


		@Nested
		public class MethodInjection {

		}

		@Nested
		public class FieldInjection {

		}
	}

	@Nested
	public class DependenciesSelection {

	}

	@Nested
	public class LifecycleManagement {

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
	private Dependency dependency;

	@Inject
	public ComponentWithInjectConstructor(Dependency dependency) {
		this.dependency = dependency;
	}

	public Dependency getDependency() {
		return dependency;
	}

}


class DependencyWithInjectConstructor implements Dependency {
	private String dependency;

	@Inject
	public DependencyWithInjectConstructor(String dependency) {
		this.dependency = dependency;
	}


	public String getDependency() {
		return dependency;
	}
}


class ComponentWithMultiInjectConstructor implements Component {

	@Inject
	public ComponentWithMultiInjectConstructor(String name, Double value) {
	}

	@Inject
	public ComponentWithMultiInjectConstructor(String name) {
	}
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {
	public ComponentWithNoInjectNorDefaultConstructor(String name) {
	}
}

