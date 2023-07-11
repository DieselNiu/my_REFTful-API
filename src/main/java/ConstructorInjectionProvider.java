import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
	private final Constructor<T> injectConstructor;
	private final List<Field> injectFields;
	private final List<Method> injectMethods;


	public ConstructorInjectionProvider(Class<T> implementation) {
		if (Modifier.isAbstract(implementation.getModifiers())) throw new IllegalComponentException();
		this.injectConstructor = getInjectConstructor(implementation);
		this.injectFields = getInjectField(implementation);
		this.injectMethods = getInjectMethods(implementation);
		if(injectFields.stream().anyMatch(f->Modifier.isFinal(f.getModifiers()))) throw new IllegalComponentException();
		if(injectMethods.stream().anyMatch(m->m.getTypeParameters().length!=0)) throw new IllegalComponentException();
	}

	private List<Method> getInjectMethods(Class<T> implementation) {
		List<Method> methods = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			methods.addAll(stream(current.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Inject.class))
				.filter(m -> methods.stream().noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
				.filter(getPredicate(implementation))
				.collect(Collectors.toList()));
			current = current.getSuperclass();
		}
		Collections.reverse(methods);
		return methods;
	}

	private Predicate<Method> getPredicate(Class<T> implementation) {
		return m -> stream(implementation.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
			.noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes()));
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
			for (Method method : injectMethods) {
				method.invoke(instance, stream(method.getParameterTypes()).map(c -> context.get(c).get()).toArray());
			}
			return instance;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependency() {
		return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
				injectFields.stream().map(Field::getType)),
			injectMethods.stream().flatMap(c -> stream(c.getParameterTypes()))
		).toList();
	}
}
