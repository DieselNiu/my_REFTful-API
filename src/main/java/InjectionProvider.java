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

class InjectionProvider<T> implements ComponentProvider<T> {
	private final Constructor<T> injectConstructor;
	private final List<Field> injectFields;
	private final List<Method> injectMethods;


	public InjectionProvider(Class<T> implementation) {
		if (Modifier.isAbstract(implementation.getModifiers())) throw new IllegalComponentException();
		this.injectConstructor = getInjectConstructor(implementation);
		this.injectFields = getInjectField(implementation);
		this.injectMethods = getInjectMethods(implementation);
		if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) throw new IllegalComponentException();
		if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) throw new IllegalComponentException();
	}

	@Override
	public T get(Context context) {
		try {
			T instance = injectConstructor.newInstance(toDependency(context, injectConstructor));
			for (Field field : injectFields) {
				field.set(instance, toDependency(context, field));
			}
			for (Method method : injectMethods) {
				method.invoke(instance, toDependency(context, method));
			}
			return instance;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public List<Class<?>> getDependency() {
		return concat(concat(stream(injectConstructor.getParameterTypes()),
				injectFields.stream().map(Field::getType)),
			injectMethods.stream().flatMap(c -> stream(c.getParameterTypes()))
		).toList();
	}

	private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		List<Constructor<?>> injectConstructors = injectable(implementation.getDeclaredConstructors()).toList();
		if (injectConstructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
	}


	private List<Method> getInjectMethods(Class<T> implementation) {
		List<Method> injectMethods = traverse(implementation, (methods, current) -> injectable(current.getDeclaredMethods())
					.filter(m -> isOverrideByInjectMethod(methods, m))
					.filter(m -> isOverrideByNoInjectMethod(implementation, m)).toList());
		Collections.reverse(injectMethods);
		return injectMethods;
	}



	private List<Field> getInjectField(Class<T> implementation) {
		return traverse(implementation, (fields, current) -> injectable(current.getDeclaredFields()).toList());
	}


	private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
		return stream(declaredFields).filter(field -> field.isAnnotationPresent(Inject.class));
	}

	private boolean isOverride(Method m, Method o) {
		return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
	}


	private boolean isOverrideByNoInjectMethod(Class<T> implementation, Method m11) {
		return stream(implementation.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
			.noneMatch(o1 -> isOverride(m11, o1));
	}

	private boolean isOverrideByInjectMethod(List<Method> methods, Method m) {
		return methods.stream().noneMatch(o -> isOverride(m, o));
	}


	private static Object[] toDependency(Context context, Executable executable) {
		return stream(executable.getParameterTypes()).map(p -> context.get(p).get()).toArray(Object[]::new);
	}

	private static Object toDependency(Context context, Field field) {
		return context.get(field.getType()).get();
	}

	private static <T> Constructor<T> defaultConstructor(Class<T> implementation) {
		try {
			return implementation.getDeclaredConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalComponentException();
		}
	}

	private static <T>  List<T> traverse(Class<?> implementation, BiFunction<List<T>, Class<?>, List<T>> finder) {
		List<T> members = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			members.addAll(finder.apply(members, current));
			current = current.getSuperclass();
		}
		return members;
	}


}
