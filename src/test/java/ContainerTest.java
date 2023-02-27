import jakarta.inject.Inject;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
	//TODO: instance
	//TODO: interface
	//TODO: abstract class

	ContextConfig config;

	@BeforeEach
	void setUp() {
		config = new ContextConfig();
	}

	@Test
	public void should_bind_type_to_a_specific_instance() {
		Component instance = new Component() {};
		config.bind(Component.class, instance);
		assertSame(instance, config.getContext().get(Component.class).get());

	}

	@Test
	public void should_return_empty_if_component_not_defined() {
		Optional<Component> component = config.getContext().get(Component.class);
		assertTrue(component.isEmpty());
	}


	@Nested
	public class ComponentConstructor {
		@Nested
		public class ConstructorInjection {
			@Test
			public void should_bind_type_to_a_class_with_default_constructor() {
				config.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component instance = config.getContext().get(Component.class).get();
				assertNotNull(instance);
				assertTrue(instance instanceof ComponentWithDefaultConstructor);
			}


			@Test
			public void should_bind_type_to_a_class_with_inject_constructor() {
				config.bind(Component.class, ComponentWithInjectConstructor.class);
				Dependency dependency = new Dependency() {};
				config.bind(Dependency.class, dependency);
				Component instance = config.getContext().get(Component.class).get();
				assertNotNull(instance);
				Dependency dependency1 = ((ComponentWithInjectConstructor) instance).getDependency();
				assertNotNull(dependency1);
				assertSame(dependency, dependency1);
			}

			@Test
			public void should_bind_type_to_a_class_with_transitive_dependency() {
				config.bind(Component.class, ComponentWithInjectConstructor.class);
				config.bind(Dependency.class, DependencyWithInjectConstructor.class);
				config.bind(String.class, "indirect dependency");


				Component instance = config.getContext().get(Component.class).get();
				assertNotNull(instance);
				Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
				assertNotNull(dependency);
				assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
			}

			@Test
			public void should_throw_exception_if_multi_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> config.bind(Component.class, ComponentWithMultiInjectConstructor.class));
			}

			@Test
			public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> config.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));
			}

			@Test
			public void should_throw_exception_if_dependency_not_exist() {
				config.bind(Component.class, ComponentWithInjectConstructor.class);
				DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
				assertThat(exception.getDependency()).isEqualTo(Dependency.class);
			}


			@Test
			public void should_throw_exception_if_transitive_dependency_not_found(){
				config.bind(Component.class, ComponentWithInjectConstructor.class);
				config.bind(Dependency.class, DependencyWithInjectConstructor.class);
				DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
				assertThat(exception.getDependency()).isEqualTo(String.class);
				assertThat(exception.getComponent()).isEqualTo(Dependency.class);
			}


			@Test
			public void should_throw_exception_if_cyclic_dependencies_found() {
				config.bind(Component.class, ComponentWithInjectConstructor.class);
				config.bind(Dependency.class, DependencyDependedOnComponent.class);
				CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
				Set<Class<?>> classes = Sets.newSet(exception.getComponents());
				assertThat(classes.size()).isEqualTo(2);
				assertTrue(classes.contains(Dependency.class));
				assertTrue(classes.contains(Component.class));
			}

			@Test
			public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
				config.bind(Component.class,ComponentWithInjectConstructor.class);
				config.bind(Dependency.class,DependencyDependedOnAnotherDependency.class);
				config.bind(AnotherDependency.class,AnotherDependencyDependedOnComponent.class);
				CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
				List<Class<?>> classes = Lists.newArrayList(exception.getComponents());
				assertThat(classes.size()).isEqualTo(3);
				assertTrue(classes.contains(Dependency.class));
				assertTrue(classes.contains(Component.class));
				assertTrue(classes.contains(AnotherDependency.class));
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

interface AnotherDependency {

}

class DependencyDependedOnAnotherDependency implements Dependency {
	private AnotherDependency anotherDependency;

	@Inject
	public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
		this.anotherDependency = anotherDependency;
	}

	public AnotherDependency getAnotherDependency() {
		return anotherDependency;
	}
}

class AnotherDependencyDependedOnComponent implements AnotherDependency{
	private Component component;

	@Inject
	public AnotherDependencyDependedOnComponent(Component component) {
		this.component = component;
	}


	public Component getComponent() {
		return component;
	}
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

class DependencyDependedOnComponent implements Dependency {
	private Component component;

	@Inject
	public DependencyDependedOnComponent(Component component) {
		this.component = component;
	}
}
