import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectTest {

	ContextConfig contextConfig;

	@BeforeEach
	void setUp() {
		contextConfig = new ContextConfig();
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

		//TODO: interface

		//TODO: multi inject constructors;
		@Test
		public void should_throw_exception_if_multi_inject_constructors_provided() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructor.class));
		}


		//TODO: no inject nor default constructors
		@Test
		public void should_throw_exception_if_no_inject_nor_default_constructors_provided() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithNoInjectNorDefaultConstructor.class));
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
			contextConfig.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
			contextConfig.bind(Dependency.class, dependency);
			FieldInjection.ComponentWithFieldInjection fieldInjection = contextConfig.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();
			assertSame(dependency, fieldInjection.dependency);
		}

		@Test
		public void should_include_dependency_in_dependencies() {
			ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> injectionProvider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
			assertArrayEquals(new Class[]{Dependency.class}, injectionProvider.getDependency().toArray());
		}

		@Test
		@Disabled
		public void should_create_component_with_injection_field() {
			Context context = mock(Context.class);
			Dependency dependency = new Dependency() {};
			when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
			ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> injectionProvider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
			FieldInjection.ComponentWithFieldInjection fieldInjection = injectionProvider.get(context);
			assertSame(dependency, fieldInjection.dependency);
		}


		@Test
		@Disabled
		public void should_throw_exception_when_field_dependency_missing() {
			contextConfig.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
			assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());
		}

		static class SubClassWithFieldInjection extends FieldInjection.SuperClassWithFieldInjection {

		}

		static class SuperClassWithFieldInjection {
			@Inject
			Dependency dependency;
		}

		@Test
		public void should_inject_dependency_via_super_class_inject_field() {
			Dependency dependency = new Dependency() {};
			contextConfig.bind(FieldInjection.SubClassWithFieldInjection.class, FieldInjection.SubClassWithFieldInjection.class);
			contextConfig.bind(Dependency.class, dependency);
			FieldInjection.SubClassWithFieldInjection fieldInjection = contextConfig.getContext().get(FieldInjection.SubClassWithFieldInjection.class).get();
			assertSame(dependency, fieldInjection.dependency);
		}


		//TODO throw exception if field is final
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
			public void install() {
				called = true;
			}
		}

		@Test
		public void should_call_inject_method_even_if_no_dependency_declared() {
			contextConfig.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);
			MethodInjection.InjectMethodWithNoDependency withNoDependency = contextConfig.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();
			assertTrue(withNoDependency.called);
		}


		static class InjectMethodWithDependency {
			Dependency dependency;

			@Inject
			public void install(Dependency dependency) {
				this.dependency = dependency;
			}
		}


		@Test
		public void should_inject_dependency_with_inject_method() {
			Dependency dependency = new Dependency() {};
			contextConfig.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);
			contextConfig.bind(Dependency.class, dependency);

			MethodInjection.InjectMethodWithDependency methodWithDependency = contextConfig.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();
			assertSame(dependency, methodWithDependency.dependency);

		}

		@Test
		public void should_include_dependencies_from_inject_method() {
			ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> injectionProvider = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
			List<Class<?>> dependency = injectionProvider.getDependency();
			assertArrayEquals(new Class<?>[]{Dependency.class}, dependency.stream().toArray());
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
				subCalled = superCalled + 1;
			}
		}


		@Test
		public void should_inject_dependencies_via_inject_method_from_superclass() {
			contextConfig.bind(MethodInjection.SuperClassWithInjectMethod.class, MethodInjection.SuperClassWithInjectMethod.class);
			contextConfig.bind(MethodInjection.SubClassWithInjectMethod.class, MethodInjection.SubClassWithInjectMethod.class);
			MethodInjection.SubClassWithInjectMethod subClassWithInjectMethod = contextConfig.getContext().get(MethodInjection.SubClassWithInjectMethod.class).get();
			assertEquals(2, subClassWithInjectMethod.subCalled);
		}


		static class SubclassOverrideSuperWithInject extends MethodInjection.SuperClassWithInjectMethod {
			@Inject
			void install() {
				super.install();
			}
		}

		@Test
		public void should_only_call_once_if_sub_class_override_inject_method_with_inject() {
			contextConfig.bind(MethodInjection.SubclassOverrideSuperWithInject.class, MethodInjection.SubclassOverrideSuperWithInject.class);
			MethodInjection.SubclassOverrideSuperWithInject superWithInject = contextConfig.getContext().get(MethodInjection.SubclassOverrideSuperWithInject.class).get();
			assertEquals(1, superWithInject.superCalled);


		}


		static class SubclassOverrideSuperWithNoInject extends MethodInjection.SuperClassWithInjectMethod {
			void install() {
				super.install();
			}
		}

		@Test
		public void should_not_call_inject_method_if_override_with_no_inject() {
			contextConfig.bind(MethodInjection.SubclassOverrideSuperWithNoInject.class, MethodInjection.SubclassOverrideSuperWithNoInject.class);
			MethodInjection.SubclassOverrideSuperWithNoInject superWithInject = contextConfig.getContext().get(MethodInjection.SubclassOverrideSuperWithNoInject.class).get();
			assertEquals(0, superWithInject.superCalled);
		}


		static class InjectMethodWithTypeParameter {
			@Inject
			<T> void install() {

			}
		}

		@Test
		public void should_throw_exception_if_inject_method_has_type_parameter() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
		}


	}
}
