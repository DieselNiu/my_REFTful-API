import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
	private final Constructor<T> injectConstructors;
	private final List<Field> injectFields;

	public ConstructorInjectionProvider(Class<T> implementation) {
		this.injectConstructors = getInjectConstructors(implementation);
		this.injectFields = getInjectFields(implementation);
	}

	private static <T> List<Field> getInjectFields(Class<T> implementation) {
		List<Field> injectFields = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			injectFields.addAll(stream(current.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
			current = current.getSuperclass();
		}
		return injectFields;
	}

	private Constructor<T> getInjectConstructors(Class<T> implementation) {
		List<Constructor<?>> constructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
		if (constructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) constructors.stream().findFirst().orElseGet(() -> {
			try {
				return implementation.getDeclaredConstructor();
			} catch (NoSuchMethodException e) {
				throw new IllegalComponentException();
			}
		});
	}


	@Override
	public T get(Context context) {
		try {
			Object[] dependencies = stream(injectConstructors.getParameters()).map(p -> context.get(p.getType()).get()).toArray(Object[]::new);
			T instance = injectConstructors.newInstance(dependencies);
			for (Field field : injectFields) {
				field.set(instance, context.get(field.getType()).get());
			}
			return instance;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependency() {
		return Stream.concat(injectFields.stream().map(Field::getType), stream(injectConstructors.getParameters()).map(Parameter::getType)).toList();
	}
}
