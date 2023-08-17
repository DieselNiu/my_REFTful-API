import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContainerTest {

	private Contextconfig contextconfig;


	@BeforeEach
	void setUp() {
		contextconfig = new Contextconfig();
	}

	@Nested
	class ComponentConstructor {

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


		@Nested
		class ConstructorInjection {
			@Test
			public void should_bind_a_class_with_default_constructor() {
				contextconfig.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component component = contextconfig.getContext().get(Component.class).get();
				assertNotNull(component);
				assertTrue(component instanceof ComponentWithDefaultConstructor);
			}

			@Test
			public void should_bind_to_a_class_with_inject_constructor() {
				Dependency dependency = new Dependency() {};
				contextconfig.bind(Component.class, ComponentWithInjectConstructor.class);
				contextconfig.bind(Dependency.class, dependency);
				Component component = contextconfig.getContext().get(Component.class).get();
				assertNotNull(component);
				assertSame(dependency, ((ComponentWithInjectConstructor) component).getDependency());
			}

			@Test
			public void should_bind_to_a_class_with_transitive_dependency() {
				contextconfig.bind(Component.class, ComponentWithInjectConstructor.class);
				contextconfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
				contextconfig.bind(String.class, "indirect dependency");
				Component component = contextconfig.getContext().get(Component.class).get();
				assertNotNull(component);
				Dependency dependency = ((ComponentWithInjectConstructor) component).getDependency();
				assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
			}

			@Test
			public void should_throw_exception_if_multi_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> contextconfig.bind(Component.class, ComponentWithMultiInjectConstructors.class));
			}

			@Test
			public void should_throw_exception_if_no_default_nor_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> contextconfig.bind(Component.class, ComponentWithNoDefaultNorInjectConstructor.class));
			}

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


			@Test
			public void should_throw_exception_if_transitive_cyclic_dependency_occur() {

			}


		}

		@Nested
		class FieldInjection {
			static class ComponentWithFieldInjection {
				@Inject
				Dependency dependency;
			}

			@Test
			public void should_inject_dependency_via_field() {
				Dependency dependency = new Dependency() {};
				contextconfig.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
				contextconfig.bind(Dependency.class, dependency);
				ComponentWithFieldInjection injection = contextconfig.getContext().get(ComponentWithFieldInjection.class).get();
				assertSame(dependency, injection.dependency);
			}

			@Test
			public void should_create_component_with_inject_field() {
				Context context = mock(Context.class);
				Dependency dependency = mock(Dependency.class);
				when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
				ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
				assertSame(dependency, provider.get(context).dependency);
			}

			@Test
			public void should_throw_exception_when_field_dependency_missing() {
				contextconfig.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
				assertThrows(DependencyNotFoundException.class, () -> contextconfig.getContext());
			}

			@Test
			public void should_include_dependency_in_dependencies() {
				ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
				Class<?>[] classes = provider.getDependency().toArray(Class<?>[]::new);
				assertArrayEquals(new Class<?>[]{Dependency.class}, classes);
			}

			static class DependencyWithInjectField implements Dependency {
				@Inject
				Component component;
			}


			@Test
			public void should_throw_exception_when_field_has_cyclic_dependencies() {
				ConstructorInjectionProvider<DependencyWithInjectField> provider = new ConstructorInjectionProvider<>(DependencyWithInjectField.class);
				Class<?>[] classes = provider.getDependency().toArray(Class<?>[]::new);
				assertArrayEquals(new Class<?>[]{Component.class}, classes);
			}


			static class SuperClassWithFieldInjection extends ComponentWithFieldInjection {

			}

			@Test
			public void should_inject_dependency_via_super_class_inject_field() {
				Dependency dependency = new Dependency() {};
				contextconfig.bind(SuperClassWithFieldInjection.class, SuperClassWithFieldInjection.class);
				contextconfig.bind(Dependency.class, dependency);

				SuperClassWithFieldInjection fieldInjection = contextconfig.getContext().get(SuperClassWithFieldInjection.class).get();
				assertSame(dependency, fieldInjection.dependency);

			}

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
