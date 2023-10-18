import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.*;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {
	ContextConfig config;

	@BeforeEach
	public void setup() {
		config = new ContextConfig();
	}

	@Nested
	public class TypeBinding {
		@Test
		public void should_bind_type_to_a_specific_instance() {
			TestComponent instance = new TestComponent() {
			};
			config.bind(TestComponent.class, instance);
			Context context = config.getContext();
			assertSame(instance, context.get(ComponentRef.of(TestComponent.class)).get());
		}

		@ParameterizedTest(name = "supporting {0}")
		@MethodSource
		public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
			Dependency dependency = new Dependency() {};
			config.bind(Dependency.class, dependency);
			config.bind(Component.class, componentType);
			Context context = config.getContext();
			Optional<Component> component = context.get(ComponentRef.of(Component.class));
			assertThat(component.isPresent()).isEqualTo(true);
			assertSame(dependency, component.get().dependency());
		}


		public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
			return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructionInjection.class)),
				Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
				Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)));
		}

		@Test
		public void should_retrieve_empty_for_unbind_type() {
			Context context = config.getContext();
			Optional<Component> component = context.get(ComponentRef.of(Component.class));
			assertTrue(component.isEmpty());
		}


		static class ConstructionInjection implements Component {
			private Dependency dependency;

			@Inject
			public ConstructionInjection(Dependency dependency) {
				this.dependency = dependency;
			}

			@Override
			public Dependency dependency() {
				return dependency;
			}
		}


		static class FieldInjection implements Component {
			@Inject
			Dependency dependency;

			@Override
			public Dependency dependency() {
				return dependency;
			}
		}


		static class MethodInjection implements Component {
			private Dependency dependency;

			@Inject
			void install(Dependency dependency) {
				this.dependency = dependency;
			}

			@Override
			public Dependency dependency() {
				return dependency;
			}
		}


		//Context
		//TODO could get Provider<T> from context

		@Test
		public void should_retrieve_bind_type_as_provider() {
			TestComponent instance = new TestComponent() {};
			config.bind(TestComponent.class, instance);
			Context context = config.getContext();
//			Provider<Component> provider = context.get(Provider<Component>.class); java无法实现这样的api表现形式
//			assertSame(instance, provider.get());
//			ParameterizedType type = new TypeLiteral<Provider<TestComponent>>() {}.getType();
//
//			assertEquals(Provider.class, type.getRawType());
//			assertEquals(TestComponent.class, type.getActualTypeArguments()[0]);

			Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {}).get();
			assertSame(instance, provider.get());
		}

		@Test
		public void should_not_retrieve_bind_type_as_unsupported_container() {
			TestComponent instance = new TestComponent() {};
			config.bind(TestComponent.class, instance);
			Context context = config.getContext();
			assertFalse(context.get(new ComponentRef<List<Component>>() {}).isPresent());
		}


		@Nested
		public class WithQualifier {
			//TODO: binding component with qualifier
			@Test
			public void should_bind_instance_with_qualifier() {
				TestComponent instance = new TestComponent() {};
				config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"));
				Context context = config.getContext();
				TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
				assertSame(instance, chosenOne);
			}

			@Test
			public void should_bind_component_with_qualifier() {
				Dependency dependency = new Dependency() {};
				config.bind(Dependency.class, dependency);
				config.bind(ComponentWithInjectConstructor.class, ComponentWithInjectConstructor.class, new NamedLiteral("ChosenOne"));

				Context context = config.getContext();
				ComponentWithInjectConstructor chosenOne = context.get(ComponentRef.of(ComponentWithInjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
				assertSame(dependency, chosenOne.getDependency());
			}
			//TODO: binding component with multi qualifier

			@Test
			public void     should_bind_instance_with_multi_qualifier() {
				TestComponent instance = new TestComponent() {};
				config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

				Context context = config.getContext();
				TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
				TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();


				assertSame(instance, chosenOne);
				assertSame(instance, skywalker);
			}


			@Test
			public void should_bind_components_with_multi_qualifier() {
				Dependency dependency = new Dependency() {};
				config.bind(Dependency.class, dependency);
				config.bind(ComponentWithInjectConstructor.class, ComponentWithInjectConstructor.class, new NamedLiteral("ChosenOne"),
					new SkywalkerLiteral());

				Context context = config.getContext();
				ComponentWithInjectConstructor chosenOne = context.get(ComponentRef.of(ComponentWithInjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
				ComponentWithInjectConstructor skywalker = context.get(ComponentRef.of(ComponentWithInjectConstructor.class, new SkywalkerLiteral())).get();
				assertSame(dependency, chosenOne.getDependency());
				assertSame(dependency, skywalker.getDependency());

			}
			//TODO: throw illegal component if illegal qualifier

			@Test
			public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
				TestComponent instance = new TestComponent() {};
				assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new TestLiteral()));
			}

			@Test
			public void should_throw_exception_if_illegal_qualifier_given_to_component() {
				assertThrows(IllegalComponentException.class, () -> config.bind(ComponentWithInjectConstructor.class, ComponentWithInjectConstructor.class, new TestLiteral()));
			}
		}


		@Nested
		public class DependencyCheck {
			@ParameterizedTest
			@MethodSource
			public void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
				config.bind(Component.class, component);
				DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
				assertEquals(Dependency.class, exception.getDependency());
				assertEquals(Component.class, exception.getComponent());
			}

			public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
				return Stream.of(Arguments.of(Named.of("Inject Constructor", DependencyCheck.MissingDependencyConstructor.class)),
					Arguments.of(Named.of("Inject Field", DependencyCheck.MissingDependencyField.class)),
					Arguments.of(Named.of("Inject Method", DependencyCheck.MissingDependencyMethod.class)),
					Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
					Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
					Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)));

			}


			static class MissingDependencyConstructor implements TestComponent {
				@Inject
				public MissingDependencyConstructor(Dependency dependency) {
				}
			}

			static class MissingDependencyField implements TestComponent {
				@Inject
				Dependency dependency;
			}

			static class MissingDependencyMethod implements TestComponent {
				@Inject
				void install(Dependency dependency) {

				}
			}

			static class MissingDependencyProviderConstructor implements TestComponent {
				@Inject
				public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {

				}

			}

			static class MissingDependencyProviderField implements TestComponent {
				@Inject Provider<Dependency> dependency;
			}


			static class MissingDependencyProviderMethod implements TestComponent {
				@Inject
				void install(Provider<Dependency> dependency) {
				}

			}


			@ParameterizedTest(name = "cyclic dependency between {0} and {1}")
			@MethodSource
			public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component,
			                                                                Class<? extends Dependency> dependency) {
				config.bind(Component.class, component);
				config.bind(Dependency.class, dependency);
				CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
				Set<Class<?>> classes = Sets.newSet(exception.getComponents());
				assertEquals(2, classes.size());
				assertThat(classes.contains(Component.class)).isEqualTo(true);
				assertThat(classes.contains(Dependency.class)).isEqualTo(true);
			}

			public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
				List<Arguments> arguments = new ArrayList<>();
				for (Named component : List.of(
					Named.of("Inject Constructor", DependencyCheck.CyclicComponentInjectConstructor.class),
					Named.of("Indirect Field", DependencyCheck.CyclicComponentInjectField.class),
					Named.of("Inject Method", DependencyCheck.CyclicComponentInjectMethod.class)))

					for (Named dependency : List.of(
						Named.of("Inject Constructor", DependencyCheck.CyclicDependencyInjectMethod.class),
						Named.of("Inject Field", DependencyCheck.CyclicDependencyInjectField.class),
						Named.of("Inject Method", DependencyCheck.CyclicDependencyInjectMethod.class)))
						arguments.add(Arguments.of(component, dependency));
				return arguments.stream();
			}


			static class CyclicComponentInjectConstructor implements TestComponent {
				@Inject
				public CyclicComponentInjectConstructor(Dependency dependency) {

				}
			}

			static class CyclicComponentInjectField implements TestComponent {
				@Inject
				Dependency dependency;
			}


			static class CyclicComponentInjectMethod implements TestComponent {
				@Inject
				void install(Dependency dependency) {
				}
			}

			static class CyclicDependencyInjectConstructor implements Dependency {
				@Inject
				public CyclicDependencyInjectConstructor(Component component) {
				}
			}

			static class CyclicDependencyInjectField implements TestComponent {
				@Inject
				Component component;
			}

			static class CyclicDependencyInjectMethod implements TestComponent {
				@Inject
				void install(Component component) {

				}
			}


			@ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
			@MethodSource
			public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends Component> component,
			                                                                           Class<? extends Dependency> dependency,
			                                                                           Class<? extends AnotherDependency> anotherDependency) {
				config.bind(Component.class, component);
				config.bind(Dependency.class, dependency);
				config.bind(AnotherDependency.class, anotherDependency);
				CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
				List<Class<?>> components = Arrays.asList(exception.getComponents());
				assertEquals(3, components.size());
				assertTrue(components.contains(Component.class));
				assertTrue(components.contains(Dependency.class));
				assertTrue(components.contains(AnotherDependency.class));
			}


			public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
				List<Arguments> arguments = new ArrayList<>();
				for (Named component : List.of(
					Named.of("Inject Constructor", DependencyCheck.CyclicComponentInjectConstructor.class),
					Named.of("Inject Field", DependencyCheck.CyclicComponentInjectField.class),
					Named.of("Inject Method", DependencyCheck.CyclicComponentInjectMethod.class)))

					for (Named dependency : List.of(
						Named.of("Inject Constructor", DependencyCheck.IndirectCyclicDependencyInjectConstructor.class),
						Named.of("Inject Field", DependencyCheck.IndirectCyclicDependencyInjectField.class),
						Named.of("Inject Method", DependencyCheck.IndirectCyclicDependencyInjectMethod.class)))


						for (Named anotherDependency : List.of(
							Named.of("Inject Constructor", DependencyCheck.IndirectCyclicAnotherDependencyInjectConstructor.class),
							Named.of("Inject Field", DependencyCheck.IndirectCyclicAnotherDependencyInjectField.class),
							Named.of("Inject Method", DependencyCheck.IndirectCyclicAnotherDependencyInjectMethod.class)))
							arguments.add(Arguments.of(component, dependency, anotherDependency));
				return arguments.stream();
			}


			static class IndirectCyclicDependencyInjectConstructor implements Dependency {
				@Inject
				public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
				}
			}

			static class IndirectCyclicDependencyInjectField implements TestComponent {
				@Inject
				AnotherDependency anotherDependency;

			}

			static class IndirectCyclicDependencyInjectMethod implements TestComponent {
				@Inject
				void install(AnotherDependency anotherDependency) {
				}
			}

			static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
				@Inject
				public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {

				}
			}

			static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
				@Inject
				Component component;

			}

			static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
				@Inject
				void install(Component component) {

				}
			}


			static class CyclicDependencyProviderConstructor implements Dependency {
				@Inject
				public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {

				}
			}

			@Test
			public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
				config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
				config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
				Context context = config.getContext();
				assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());

			}


			@Nested
			public class WithQualifier {
				//TODO dependency missing if qualifier not match
				//TODO check cyclic dependencies with qualifier
			}
		}

	}

	record NamedLiteral(String value) implements jakarta.inject.Named {
		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.inject.Named.class;
		}
	}


	@Target({ElementType.ANNOTATION_TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Qualifier
	@interface Skywalker {
	}

	record SkywalkerLiteral() implements Skywalker {

		@Override
		public Class<? extends Annotation> annotationType() {
			return Skywalker.class;
		}
	}


	record TestLiteral() implements Test {

		@Override
		public Class<? extends Annotation> annotationType() {
			return Test.class;
		}
	}
}
