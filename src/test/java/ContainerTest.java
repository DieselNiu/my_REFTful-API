import jakarta.inject.Inject;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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


			abstract class AbstractComponent implements Component {
				@Inject
				public AbstractComponent() {

				}
			}

			@Test
			public void should_throw_exception_if_component_is_abstract() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
			}

			@Test
			public void should_throw_exception_if_component_is_interface() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
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
			public void should_throw_exception_if_transitive_dependency_not_found() {
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
				config.bind(Component.class, ComponentWithInjectConstructor.class);
				config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
				config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
				CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
				List<Class<?>> classes = Lists.newArrayList(exception.getComponents());
				assertThat(classes.size()).isEqualTo(3);
				assertTrue(classes.contains(Dependency.class));
				assertTrue(classes.contains(Component.class));
				assertTrue(classes.contains(AnotherDependency.class));
			}

		}


		@Nested
		public class FieldInjection {
			static class FinalInjectField {
				@Inject
				final Dependency dependency = null;
			}

			@Test
			public void should_throw_exception_if_inject_field_is_final() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
			}


			static class ComponentWithFieldInjection {
				@Inject
				Dependency dependency;
			}

			static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
			}

			@Test
			public void should_inject_dependency_via_field() {
				Dependency dependency = new Dependency() {};
				config.bind(Dependency.class, dependency);
				config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
				ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
				assertSame(dependency, component.dependency);
			}


			@Test
			public void should_inject_dependency_via_superclass_inject_fields() {
				Dependency dependency = new Dependency() {};
				config.bind(Dependency.class, dependency);
				config.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);
				ComponentWithFieldInjection component = config.getContext().get(SubclassWithFieldInjection.class).get();
				assertSame(dependency, component.dependency);
			}


			@Test
			public void should_throw_exception_when_field_dependency_missing() {
				ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
				assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
			}


		}

		@Nested
		public class MethodInjection {
			static class InjectMethodWithNoDependency {
				boolean called = false;

				@Inject
				void install() {
					this.called = true;
				}
			}

			@Test
			public void should_call_inject_method_even_if_no_dependency_declared() {
				config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
				InjectMethodWithNoDependency dependency = config.getContext().get(InjectMethodWithNoDependency.class).get();
				assertTrue(dependency.called);
			}

			static class InjectMethodWithDependency {
				Dependency dependency;

				@Inject
				void install(Dependency dependency) {
					this.dependency = dependency;
				}
			}

			@Test
			public void should_inject_dependency_via_inject_method() {
				Dependency dependency = new Dependency() {};
				config.bind(Dependency.class, dependency);
				config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
				InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class).get();
				assertSame(dependency, component.dependency);
			}


			static class SuperClassWithInjectMethod {
				int superCalled = 0;

				@Inject
				void install() {
					superCalled++;
				}
			}

			static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
				int subCalled = 0;

				@Inject
				void installAnother() {
					subCalled = superCalled + 1;
				}
			}

			@Test
			public void should_inject_dependencies_via_inject_method_from_superclass() {
				config.bind(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);
				SubclassWithInjectMethod injectMethod = config.getContext().get(SubclassWithInjectMethod.class).get();
				assertEquals(1, injectMethod.superCalled);
				assertEquals(2, injectMethod.subCalled);
			}

			static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
				@Inject
				void install() {
					super.install();
				}
			}


			@Test
			public void should_only_call_once_if_subclass_inject_method_with_inject() {
				config.bind(SubclassOverrideSuperClassWithInject.class, SubclassOverrideSuperClassWithInject.class);
				SubclassOverrideSuperClassWithInject component = config.getContext().get(SubclassOverrideSuperClassWithInject.class).get();
				assertEquals(1, component.superCalled);

			}



			static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
				void install() {
					super.install();
				}
			}

			@Test
			public void should_not_call_inject_method_if_override_with_no_inject() {
				config.bind(SubclassOverrideSuperClassWithNoInject.class, SubclassOverrideSuperClassWithNoInject.class);
				SubclassOverrideSuperClassWithNoInject component = config.getContext().get(SubclassOverrideSuperClassWithNoInject.class).get();
				assertEquals(0, component.superCalled);

			}


			@Test
			public void should_include_dependencies_from_inject_method() {
				ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
				assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));

			}
			static class InjectMethodWithTypeParameter {
				@Inject
				<T> void install() {
					
				}
			}
			
			@Test
			public void should_throw_exception_if_inject_method_has_type_parameter(){
			  assertThrows(IllegalComponentException.class,()->new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
			}



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

class AnotherDependencyDependedOnComponent implements AnotherDependency {
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
