import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
	private Constructor<T> constructor;
	private List<Field> injectFields;

	public ConstructorInjectionProvider(Class<T> implementation) {
		this.constructor = getInjectConstructor(implementation);
		this.injectFields = getInjectFields(implementation);
	}


	@Override
	public T get(Context context) {
		try {
			Object[] dependencies = stream(constructor.getParameters()).map(c -> context.get(c.getType()).get()).toArray(Object[]::new);
			T t = constructor.newInstance(dependencies);
			for (Field field : injectFields) {
				field.set(t, context.get(field.getType()).get());
			}
			return t;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependencies() {
		return Stream.concat(stream(constructor.getParameters()).map(Parameter::getType),
			injectFields.stream().map(field -> field.getType())).toList();
	}

	private static <T> List<Field> getInjectFields(Class<T> implementation) {
		List<Field> injectFields = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			injectFields.addAll(stream(current.getDeclaredFields()).filter(c -> c.isAnnotationPresent(Inject.class))
				.toList());
			current = current.getSuperclass();
		}
		return injectFields;
	}


	private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
		if (injectConstructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
			try {
				return implementation.getDeclaredConstructor();
			} catch (NoSuchMethodException e) {
				throw new IllegalComponentException();
			}
		});
	}

}
