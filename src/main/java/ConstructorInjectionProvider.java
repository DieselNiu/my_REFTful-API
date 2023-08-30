import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
	private final Constructor<T> injectConstructors;
	private final List<Field> injectFields;
	private final List<Method> injectMethods;

	public ConstructorInjectionProvider(Class<T> implementation) {
		if (Modifier.isAbstract(implementation.getModifiers())) throw new IllegalComponentException();
		this.injectConstructors = getInjectConstructors(implementation);
		this.injectFields = getInjectFields(implementation);
		this.injectMethods = getInjectMethods(implementation);
		if (injectFields.stream().anyMatch(field -> Modifier.isFinal(field.getModifiers()))) throw new IllegalComponentException();
		if (injectMethods.stream().anyMatch(method -> method.getTypeParameters().length > 0)) throw new IllegalComponentException();
	}

	private static <T> List<Method> getInjectMethods(Class<T> implementation) {
		List<Method> injectMethods = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			injectMethods.addAll(stream(current.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Inject.class))
				.filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
				.filter(m -> stream(implementation.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
				.toList());
			current = current.getSuperclass();
		}
		Collections.reverse(injectMethods);
		return injectMethods;
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
			for (Method method : injectMethods) {
				method.invoke(instance, stream(method.getParameterTypes()).map(p -> context.get(p).get()).toArray(Object[]::new));
			}
			return instance;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependency() {
		return Stream.concat(Stream.concat(injectFields.stream().map(Field::getType), stream(injectConstructors.getParameters()).map(Parameter::getType)), injectMethods.stream().flatMap(m -> stream(m.getParameterTypes()))).toList();
	}
}
