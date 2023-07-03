import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
	@Nested
	class ComponentConstruction {

		interface Component {

		}


		static class ComponentWithDefaultConstructor implements Component {
			public ComponentWithDefaultConstructor() {
			}
		}

		@Test
		public void should_bind_type_to_a_specific_instance() {
			Context context = new Context();
			Component component = new Component() {};
			context.bind(Component.class, component);
			assertSame(component, context.get(Component.class));
		}

		@Nested
		class ConstructorInjection {

			@Test
			public void should_bind_type_to_a_class_with_default_constructor() {
				Context context = new Context();
				context.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component component = context.get(Component.class);
				assertNotNull(component);
				assertTrue(component instanceof ComponentWithDefaultConstructor);

			}

		}

		@Nested
		class FieldInjection {

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
