import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
	//TODO: instance
	//TODO: interface
	//TODO: abstract class
	interface Component {

	}


	static class ComponentWithDefaultConstructor implements Component {
		public ComponentWithDefaultConstructor() {

		}
	}

	@Test
	public void should_bind_type_to_a_specific_instance() {
		Context context = new Context();
		Component instance = new Component() {};
		context.bind(Component.class, instance);
		assertSame(instance, context.get(Component.class));

	}


	@Nested
	public class ComponentConstructor {
		@Nested
		public class ConstructorInjection {
			@Test
			public void should_bind_type_to_a_class_with_default_constructor() {
				Context context = new Context();
				context.bind(Component.class, ComponentWithDefaultConstructor.class);
				Component instance = context.get(Component.class);
				assertNotNull(instance);
				assertTrue(instance instanceof ComponentWithDefaultConstructor);
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
