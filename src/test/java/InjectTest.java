import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@Nested
public class InjectTest {
	private Dependency dependency = mock(Dependency.class);
	private Context context = mock(Context.class);


	@BeforeEach
	void setUp() {
		Mockito.when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
	}


	@Nested
	class ConstructorInjection {


		@Nested
		class Injection {
			@Test
			public void should_call_default_constructor_if_no_inject_constructor() {
				ComponentWithDefaultConstructor component = new ConstructorInjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
				assertNotNull(component);
			}

			@Test
			public void should_inject_dependency_via_inject_constructor() {
				ComponentWithInjectConstructor component = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class).get(context);
				assertNotNull(component);
				assertSame(dependency, component.dependency);
			}
		}


		@Nested
		class IllegalInjectConstructor {

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
			public void should_throw_exception_if_multi_inject_constructors_provided() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructor.class));
			}


			@Test
			public void should_throw_exception_if_no_inject_nor_default_constructors_provided() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithNoInjectNorDefaultConstructor.class));
			}

		}


	}

	@Nested
	class FieldInjection {


		@Nested
		class Injection {

			@Test
			public void should_inject_dependency_via_field() {
				ComponentWithFieldInjection fieldInjection = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class).get(context);
				assertSame(dependency, fieldInjection.dependency);
			}


			static class SuperClassWithFieldInjection {
				@Inject
				Dependency dependency;
			}

			static class SubClassWithFieldInjection extends SuperClassWithFieldInjection {

			}

			@Test
			public void should_inject_dependency_via_super_class_inject_field() {
				SubClassWithFieldInjection fieldInjection = new ConstructorInjectionProvider<>(SubClassWithFieldInjection.class).get(context);
				assertSame(dependency, fieldInjection.dependency);
			}


			static class ComponentWithFieldInjection {
				@Inject
				Dependency dependency;
			}

			@Test
			public void should_include_dependency_from_field_dependency() {
				ConstructorInjectionProvider<ComponentWithFieldInjection> injectionProvider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
				assertArrayEquals(new Class[]{Dependency.class}, injectionProvider.getDependency().toArray());
			}
		}


		@Nested
		class IllegalInjectFields {
			static class FinalInjectField {
				@Inject final Dependency dependency = null;
			}

			@Test
			public void should_throw_exception_if_inject_field_is_final() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
			}

		}

	}

	@Nested
	class MethodInjection {


		@Nested
		class Injection {
			static class InjectMethodWithNoDependency {
				boolean called = false;

				@Inject
				public void install() {
					called = true;
				}
			}

			@Test
			public void should_call_inject_method_even_if_no_dependency_declared() {
				InjectMethodWithNoDependency withNoDependency = new ConstructorInjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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
				InjectMethodWithDependency methodWithDependency = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class).get(context);
				assertSame(dependency, methodWithDependency.dependency);

			}

			@Test
			public void should_include_dependencies_from_inject_method() {
				ConstructorInjectionProvider<InjectMethodWithDependency> injectionProvider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
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

			static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
				int subCalled = 0;

				@Inject
				void installAnother() {
					subCalled = superCalled + 1;
				}
			}


			@Test
			public void should_inject_dependencies_via_inject_method_from_superclass() {
				SubClassWithInjectMethod subClassWithInjectMethod = new ConstructorInjectionProvider<>(SubClassWithInjectMethod.class).get(context);
				assertEquals(2, subClassWithInjectMethod.subCalled);
			}


			static class SubclassOverrideSuperWithInject extends SuperClassWithInjectMethod {
				@Inject
				void install() {
					super.install();
				}
			}

			@Test
			public void should_only_call_once_if_sub_class_override_inject_method_with_inject() {
				SubclassOverrideSuperWithInject superWithInject = new ConstructorInjectionProvider<>(SubclassOverrideSuperWithInject.class).get(context);
				assertEquals(1, superWithInject.superCalled);


			}


			static class SubclassOverrideSuperWithNoInject extends SuperClassWithInjectMethod {
				void install() {
					super.install();
				}
			}

			@Test
			public void should_not_call_inject_method_if_override_with_no_inject() {
				SubclassOverrideSuperWithNoInject superWithInject = new ConstructorInjectionProvider<>(SubclassOverrideSuperWithNoInject.class).get(context);
				assertEquals(0, superWithInject.superCalled);
			}

		}

		@Nested
		class IllegalInjectMethods {
			static class InjectMethodWithTypeParameter {
				@Inject
				<T> void install() {

				}
			}

			@Test
			public void should_throw_exception_if_inject_method_has_type_parameter() {
				assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
			}
		}



	}
}
