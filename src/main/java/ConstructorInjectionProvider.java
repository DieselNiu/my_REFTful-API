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
	private final Constructor<T> injectConstructor;
	private final List<Field> injectFields;


	public ConstructorInjectionProvider(Class<T> implementation) {
		this.injectConstructor = getInjectConstructor(implementation);
		this.injectFields = getInjectField(implementation);
	}

	private List<Field> getInjectField(Class<T> implementation) {
		List<Field> fields = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			fields.addAll(stream(current.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Inject.class)).collect(Collectors.toList()));
			current = current.getSuperclass();
		}
		return fields;
	}

	private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		List<Constructor<?>> injectConstructors = stream(implementation.getDeclaredConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
		if (injectConstructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
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
			Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> context.get(p.getType()).get()).toArray(Object[]::new);
			T instance = injectConstructor.newInstance(dependencies);
			for (Field field : injectFields) {
				field.set(instance, context.get(field.getType()).get());
			}
			return instance;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependency() {
		return Stream.concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
			injectFields.stream().map(Field::getType)).toList();
	}
}
