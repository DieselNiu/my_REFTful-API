import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
	private Constructor<T> constructor;
	private List<Field> injectFields;
	private List<Method> injectMethods;

	public InjectionProvider(Class<T> implementation) {
		if (Modifier.isAbstract(implementation.getModifiers())) throw new IllegalComponentException();
		this.constructor = getInjectConstructor(implementation);
		this.injectFields = getInjectFields(implementation);
		this.injectMethods = getInjectMethods(implementation);

		if (injectFields.stream().anyMatch(x -> Modifier.isFinal(x.getModifiers()))) throw new IllegalComponentException();
		if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) throw new IllegalComponentException();
	}


	@Override
	public T get(Context context) {
		try {
			T instance = constructor.newInstance(toDependencies(context, this.constructor));
			for (Field field : injectFields)
				field.set(instance, toDependency(context, field));
			for (Method method : injectMethods) {
				method.invoke(instance, toDependencies(context, method));
			}
			return instance;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public List<Class<?>> getDependencies() {
		return concat(concat(stream(constructor.getParameterTypes()),
				injectFields.stream().map(field -> field.getType())),
			injectMethods.stream().flatMap(m -> stream(m.getParameterTypes())))
			.toList();
	}

	private static <T> List<Method> getInjectMethods(Class<T> implementation) {
		List<Method> injectMethods = traverse(implementation, ((methods, current) -> injectable(current.getDeclaredMethods())
			.filter(m -> isOverrideByMethod(methods, m))
			.filter(m -> isOverrideByNoInjectMethod(implementation, m)).toList()));
		Collections.reverse(injectMethods);
		return injectMethods;
	}


	private static <T> List<Field> getInjectFields(Class<T> implementation) {
		return traverse(implementation, (fields, current) -> injectable(current.getDeclaredFields()).toList());
	}


	private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
		if (injectConstructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
	}

	private static <T> Constructor<T> defaultConstructor(Class<T> implementation) {
		try {
			return implementation.getDeclaredConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalComponentException();
		}
	}

	private static <T> List<T> traverse(Class<?> implementation, BiFunction<List<T>, Class<?>, List<T>> finder) {
		List<T> members = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			members.addAll(finder.apply(members, current));
			current = current.getSuperclass();
		}
		return members;
	}

	private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
		return stream(declaredFields).filter(c -> c.isAnnotationPresent(Inject.class));
	}

	private static boolean isOverride(Method m, Method o) {
		return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
	}

	private static <T> boolean isOverrideByNoInjectMethod(Class<T> implementation, Method m) {
		return stream(implementation.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> isOverride(m, o));
	}

	private static boolean isOverrideByMethod(List<Method> injectMethods, Method m) {
		return injectMethods.stream().noneMatch(o -> isOverride(m, o));
	}

	private static Object[] toDependencies(Context context, Executable executable) {
		return stream(executable.getParameters()).map(p -> {
			Type type = p.getParameterizedType();
			if (type instanceof ParameterizedType) return context.get((ParameterizedType) type).get();
			return context.get((Class<?>) type).get();
		}).toArray(Object[]::new);
	}


	private static Object toDependency(Context context, Field field) {
		Type type = field.getGenericType();
		if (type instanceof ParameterizedType) return context.get((ParameterizedType) type).get();
		return context.get((Class<?>) type).get();
	}

}
