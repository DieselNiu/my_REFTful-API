import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class InjectionTest {

	ContextConfig config;

	@BeforeEach
	void setUp() {
		config = new ContextConfig();
	}

	@Nested
	public class ConstructorInjection {


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


		@Test
		public void should_throw_exception_if_multi_inject_constructor_provided() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructor.class));
		}

		@Test
		public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithNoInjectNorDefaultConstructor.class));
		}


		@Test
		public void should_include_dependency_from_inject_constructor() {
			ConstructorInjectionProvider<ComponentWithInjectConstructor> injectionProvider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
			assertArrayEquals(new Class<?>[]{Dependency.class}, injectionProvider.getDependencies().toArray(Class<?>[]::new));
		}


	}


	@Nested
	public class FieldInjection {
		static class FinalInjectField {
			@Inject final Dependency dependency = null;
		}

		@Test
		public void should_throw_exception_if_inject_field_is_final() {
			assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
		}


		static class ComponentWithFieldInjection {
			@Inject
			Dependency dependency;
		}

		static class SubclassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {
		}

		@Test
		public void should_inject_dependency_via_field() {
			Dependency dependency = new Dependency() {};
			config.bind(Dependency.class, dependency);
			config.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
			FieldInjection.ComponentWithFieldInjection component = config.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();
			assertSame(dependency, component.dependency);
		}


		@Test
		public void should_inject_dependency_via_superclass_inject_fields() {
			Dependency dependency = new Dependency() {};
			config.bind(Dependency.class, dependency);
			config.bind(FieldInjection.SubclassWithFieldInjection.class, FieldInjection.SubclassWithFieldInjection.class);
			FieldInjection.ComponentWithFieldInjection component = config.getContext().get(FieldInjection.SubclassWithFieldInjection.class).get();
			assertSame(dependency, component.dependency);
		}


		@Test
		public void should_throw_exception_when_field_dependency_missing() {
			ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
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
			config.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);
			MethodInjection.InjectMethodWithNoDependency dependency = config.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();
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
			config.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);
			MethodInjection.InjectMethodWithDependency component = config.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();
			assertSame(dependency, component.dependency);
		}


		static class SuperClassWithInjectMethod {
			int superCalled = 0;

			@Inject
			void install() {
				superCalled++;
			}
		}

		static class SubclassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
			int subCalled = 0;

			@Inject
			void installAnother() {
				subCalled = superCalled + 1;
			}
		}

		@Test
		public void should_inject_dependencies_via_inject_method_from_superclass() {
			config.bind(MethodInjection.SubclassWithInjectMethod.class, MethodInjection.SubclassWithInjectMethod.class);
			MethodInjection.SubclassWithInjectMethod injectMethod = config.getContext().get(MethodInjection.SubclassWithInjectMethod.class).get();
			assertEquals(1, injectMethod.superCalled);
			assertEquals(2, injectMethod.subCalled);
		}

		static class SubclassOverrideSuperClassWithInject extends MethodInjection.SuperClassWithInjectMethod {
			@Inject
			void install() {
				super.install();
			}
		}


		@Test
		public void should_only_call_once_if_subclass_inject_method_with_inject() {
			config.bind(MethodInjection.SubclassOverrideSuperClassWithInject.class, MethodInjection.SubclassOverrideSuperClassWithInject.class);
			MethodInjection.SubclassOverrideSuperClassWithInject component = config.getContext().get(MethodInjection.SubclassOverrideSuperClassWithInject.class).get();
			assertEquals(1, component.superCalled);

		}


		static class SubclassOverrideSuperClassWithNoInject extends MethodInjection.SuperClassWithInjectMethod {
			void install() {
				super.install();
			}
		}

		@Test
		public void should_not_call_inject_method_if_override_with_no_inject() {
			config.bind(MethodInjection.SubclassOverrideSuperClassWithNoInject.class, MethodInjection.SubclassOverrideSuperClassWithNoInject.class);
			MethodInjection.SubclassOverrideSuperClassWithNoInject component = config.getContext().get(MethodInjection.SubclassOverrideSuperClassWithNoInject.class).get();
			assertEquals(0, component.superCalled);

		}


		@Test
		public void should_include_dependencies_from_inject_method() {
			ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
			assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));

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
