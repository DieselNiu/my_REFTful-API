import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {
	private Contextconfig contextconfig;


	@BeforeEach
	void setUp() {
		contextconfig = new Contextconfig();
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
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class));
		}

		@Test
		public void should_throw_exception_if_no_default_nor_inject_constructor_provided() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithNoDefaultNorInjectConstructor.class));
		}

		@Test
		public void should_include_dependency_from_inject_constructor() {
			ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
			List<Class<?>> dependency = provider.getDependency();
			assertTrue(dependency.contains(Dependency.class));
		}


		abstract class AbstractComponent implements Component {
			@Inject
			public AbstractComponent() {
			}
		}

		@Test
		public void should_throw_exception_if_component_is_abstract() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ConstructorInjection.AbstractComponent.class));
		}

		@Test
		public void should_throw_exception_if_component_is_interface() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
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
			contextconfig.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
			contextconfig.bind(Dependency.class, dependency);
			FieldInjection.ComponentWithFieldInjection injection = contextconfig.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();
			assertSame(dependency, injection.dependency);
		}

		@Test
		public void should_create_component_with_inject_field() {
			Context context = mock(Context.class);
			Dependency dependency = mock(Dependency.class);
			when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
			ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
			assertSame(dependency, provider.get(context).dependency);
		}

		@Test
		public void should_throw_exception_when_field_dependency_missing() {
			contextconfig.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
			assertThrows(DependencyNotFoundException.class, () -> contextconfig.getContext());
		}

		@Test
		public void should_include_dependency_in_dependencies() {
			ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
			Class<?>[] classes = provider.getDependency().toArray(Class<?>[]::new);
			assertArrayEquals(new Class<?>[]{Dependency.class}, classes);
		}

		static class DependencyWithInjectField implements Dependency {
			@Inject
			Component component;
		}


		@Test
		public void should_throw_exception_when_field_has_cyclic_dependencies() {
			ConstructorInjectionProvider<FieldInjection.DependencyWithInjectField> provider = new ConstructorInjectionProvider<>(FieldInjection.DependencyWithInjectField.class);
			Class<?>[] classes = provider.getDependency().toArray(Class<?>[]::new);
			assertArrayEquals(new Class<?>[]{Component.class}, classes);
		}


		static class SuperClassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {

		}

		@Test
		public void should_inject_dependency_via_super_class_inject_field() {
			Dependency dependency = new Dependency() {};
			contextconfig.bind(FieldInjection.SuperClassWithFieldInjection.class, FieldInjection.SuperClassWithFieldInjection.class);
			contextconfig.bind(Dependency.class, dependency);

			FieldInjection.SuperClassWithFieldInjection fieldInjection = contextconfig.getContext().get(FieldInjection.SuperClassWithFieldInjection.class).get();
			assertSame(dependency, fieldInjection.dependency);

		}

		static class FinalInjectField {
			@Inject final Dependency dependency = null;
		}

		@Test
		public void should_throw_exception_if_inject_field_is_final() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
		}

	}

	@Nested
	class MethodInjection {
		static class InjectMethodWithNoDependency {
			boolean called = false;


			@Inject
			void install() {
				called = true;
			}

		}

		@Test
		public void should_call_inject_method_even_if_no_dependency() {
			contextconfig.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);
			MethodInjection.InjectMethodWithNoDependency withNoDependency = contextconfig.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();
			assertTrue(withNoDependency.called);
		}

		static class InjectMethodWithDependency {
			Dependency dependency;

			@Inject
			void install(Dependency dependency) {
				this.dependency = dependency;
			}
		}

		@Test
		public void should_inject_dependency_with_inject_method() {
			Dependency dependency = new Dependency() {};
			contextconfig.bind(Dependency.class, dependency);
			contextconfig.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);
			MethodInjection.InjectMethodWithDependency methodWithDependency = contextconfig.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();
			assertSame(dependency, methodWithDependency.dependency);
		}

		@Test
		public void should_include_dependency_in_dependencies() {
			ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> injectionProvider = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
			List<Class<?>> dependencies = injectionProvider.getDependency();
			assertTrue(dependencies.contains(Dependency.class));
		}

		static class SuperClassWithInjectMethod {
			int superCalled = 0;

			@Inject
			void install() {
				superCalled++;
			}
		}


		static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
			int subCalled = 0;

			@Inject
			void installAnother() {
				subCalled = 1 + superCalled;
			}
		}


		@Test
		public void should_inject_dependency_via_inject_method_from_superclass() {
			contextconfig.bind(MethodInjection.SubClassWithInjectMethod.class, MethodInjection.SubClassWithInjectMethod.class);
			MethodInjection.SubClassWithInjectMethod subClassWithInjectMethod = contextconfig.getContext().get(MethodInjection.SubClassWithInjectMethod.class).get();
			assertEquals(1, subClassWithInjectMethod.superCalled);
			assertEquals(2, subClassWithInjectMethod.subCalled);
		}

		static class SubClassOverrideSuperClass extends MethodInjection.SuperClassWithInjectMethod {

			@Inject
			void install() {
				super.install();
			}
		}


		@Test
		public void should_only_call_once_if_subclass_override_superclass() {
			contextconfig.bind(MethodInjection.SubClassOverrideSuperClass.class, MethodInjection.SubClassOverrideSuperClass.class);
			MethodInjection.SubClassOverrideSuperClass overrideSuperClass = contextconfig.getContext().get(MethodInjection.SubClassOverrideSuperClass.class).get();
			assertEquals(1, overrideSuperClass.superCalled);
		}


		static class SubclassOverrideSuperclassWithNoInject extends MethodInjection.SuperClassWithInjectMethod {

			void install() {
				super.install();
			}
		}

		@Test
		public void should_not_call_inject_method_if_override_with_no_inject() {
			contextconfig.bind(MethodInjection.SubclassOverrideSuperclassWithNoInject.class, MethodInjection.SubclassOverrideSuperclassWithNoInject.class);
			MethodInjection.SubclassOverrideSuperclassWithNoInject superclassWithNoInject = contextconfig.getContext().get(MethodInjection.SubclassOverrideSuperclassWithNoInject.class).get();
			assertEquals(0, superclassWithNoInject.superCalled);
		}

		static class InjectMethodWithTypeParameter {
			@Inject
			<T> void install() {

			}
		}

		@Test
		public void should_throw_exception_if_method_has_type_parameter() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
		}
	}

}
