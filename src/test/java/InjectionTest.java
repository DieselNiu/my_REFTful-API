import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {

	private Dependency dependency = mock(Dependency.class);
	private Provider<Dependency> dependencyProvider = mock(Provider.class);
	private Context context = mock(Context.class);
	private ParameterizedType dependencyType;

	@BeforeEach
	void setUp() throws NoSuchFieldException {
		dependencyType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
		when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
		when(context.get(eq(dependencyType))).thenReturn(Optional.of(dependencyProvider));
	}

	@Nested
	public class ConstructorInjection {

		@Nested
		class Injection {

			@Test
			public void should_call_default_constructor_if_no_inject_constructor() {
				ComponentWithDefaultConstructor instance = new InjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
				assertNotNull(instance);
			}


			@Test
			public void should_inject_dependency_via_inject_constructor() {
				ComponentWithInjectConstructor instance = new InjectionProvider<>(ComponentWithInjectConstructor.class).get(context);
				assertNotNull(instance);
				assertSame(dependency, instance.getDependency());
			}

			@Test
			public void should_include_dependency_from_inject_constructor() {
				InjectionProvider<ComponentWithInjectConstructor> injectionProvider = new InjectionProvider<>(ComponentWithInjectConstructor.class);
				assertArrayEquals(new Class<?>[]{Dependency.class}, injectionProvider.getDependencies().toArray(Class<?>[]::new));
			}

			//TODO: include dependency type from inject constructor
			@Test
			public void should_include_provider_type_from_inject_constructor() {
				InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
				assertArrayEquals(new Type[]{dependencyType}, provider.getDependencyTypes().toArray(Type[]::new));
			}




			//InjectionProvider
			//TODO  support inject constructor

			static class ProviderInjectConstructor {
				Provider<Dependency> dependency;

				@Inject
				public ProviderInjectConstructor(Provider<Dependency> dependency) {
					this.dependency = dependency;
				}
			}

			@Test
			public void should_inject_provider_via_inject_constructor() {
				ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);
				assertSame(dependencyProvider, instance.dependency);

			}


		}

		@Nested
		class IllegalInjectConstructor {
			@Test
			public void should_throw_exception_if_component_is_abstract() {
				assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ConstructorInjection.AbstractComponent.class));
			}

			@Test
			public void should_throw_exception_if_component_is_interface() {
				assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(Component.class));
			}


			@Test
			public void should_throw_exception_if_multi_inject_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithMultiInjectConstructor.class));
			}

			@Test
			public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
				assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithNoInjectNorDefaultConstructor.class));
			}

		}


		abstract class AbstractComponent implements Component {
			@Inject
			public AbstractComponent() {

			}
		}


	}

	@Nested
	public class FieldInjection {

		@Nested
		class Injection {

			static class ComponentWithFieldInjection {
				@Inject
				Dependency dependency;
			}

			static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
			}

			@Test
			public void should_inject_dependency_via_field() {
				ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);
				assertSame(dependency, component.dependency);
			}


			@Test
			public void should_inject_dependency_via_superclass_inject_fields() {
				ComponentWithFieldInjection component = new InjectionProvider<>(SubclassWithFieldInjection.class).get(context);
				assertSame(dependency, component.dependency);
			}


			@Test
			public void should_throw_exception_when_field_dependency_missing() {
				InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
				assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
			}

			//TODO: include dependency type  from inject field


			@Test
			public void should_include_provider_type_from_inject_field() {
				InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
				assertArrayEquals(new Type[]{dependencyType}, provider.getDependencyTypes().toArray(Type[]::new));
			}


			static class ProviderInjectField {
				@Inject
				Provider<Dependency> dependency;
			}

			@Test
			public void should_inject_provider_via_inject_method() {
				ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
				assertSame(dependencyProvider, instance.dependency);

			}


		}

		//TODO support inject field


		@Nested
		class IllegalInjectFields {
			static class FinalInjectField {
				@Inject final Dependency dependency = null;
			}

			@Test
			public void should_throw_exception_if_inject_field_is_final() {
				assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectField.class));
			}
		}

	}

	@Nested
	public class MethodInjection {


		@Nested
		class Injection {
			static class InjectMethodWithNoDependency {
				boolean called = false;

				@Inject
				void install() {
					this.called = true;
				}
			}

			@Test
			public void should_call_inject_method_even_if_no_dependency_declared() {
				InjectMethodWithNoDependency dependency = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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
				InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
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
				SubclassWithInjectMethod injectMethod = new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);
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
				SubclassOverrideSuperClassWithInject component = new InjectionProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);
				assertEquals(1, component.superCalled);

			}


			static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
				void install() {
					super.install();
				}
			}

			@Test
			public void should_not_call_inject_method_if_override_with_no_inject() {
				SubclassOverrideSuperClassWithNoInject component = new InjectionProvider<>(SubclassOverrideSuperClassWithNoInject.class).get(context);
				assertEquals(0, component.superCalled);
			}


			@Test
			public void should_include_dependencies_from_inject_method() {
				InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
				assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));

			}

			//TODO: include dependency type  from inject method
			@Test
			public void should_include_provider_type_from_inject_method() {
				InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
				assertArrayEquals(new Type[]{dependencyType}, provider.getDependencyTypes().toArray(Type[]::new));
			}



			//TODO support inject method
			static class ProviderInjectMethod {
				Provider<Dependency> dependency;

				@Inject
				void install(Provider<Dependency> dependency) {
					this.dependency = dependency;
				}
			}

			@Test
			public void should_inject_provider_via_inject_method() {
				ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
				assertSame(dependencyProvider, instance.dependency);

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
				assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
			}


		}

	}
}

interface Components {

}

class ComponentWithDefaultConstructor implements Components {
	public ComponentWithDefaultConstructor() {

	}

}

class ComponentWithInjectConstructor implements Components {
	private Dependency dependency;

	@Inject
	public ComponentWithInjectConstructor(Dependency dependency) {
		this.dependency = dependency;
	}

	public Dependency getDependency() {
		return dependency;
	}

}


class ComponentWithMultiInjectConstructor implements Components {

	@Inject
	public ComponentWithMultiInjectConstructor(String name, Double value) {
	}

	@Inject
	public ComponentWithMultiInjectConstructor(String name) {
	}
}

class ComponentWithNoInjectNorDefaultConstructor implements Components {
	public ComponentWithNoInjectNorDefaultConstructor(String name) {
	}
}
