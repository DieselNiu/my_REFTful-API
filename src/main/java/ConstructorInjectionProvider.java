import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
	private Constructor<T> constructor;
	private List<Field> injectFields;
	private List<Method> injectMethods;

	public ConstructorInjectionProvider(Class<T> implementation) {
		this.constructor = getInjectConstructor(implementation);
		this.injectFields = getInjectFields(implementation);
		this.injectMethods = getInjectMethods(implementation);
	}


	@Override
	public T get(Context context) {
		try {
			Object[] dependencies = stream(constructor.getParameters()).map(c -> context.get(c.getType()).get()).toArray(Object[]::new);
			T t = constructor.newInstance(dependencies);
			for (Field field : injectFields)
				field.set(t, context.get(field.getType()).get());
			for (Method method : injectMethods)
				method.invoke(t, stream(method.getParameterTypes()).map(m -> context.get(m).get()).toArray(Object[]::new));
			return t;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependencies() {
		return
			concat(
				concat(stream(constructor.getParameters()).map(Parameter::getType),
					injectFields.stream().map(field -> field.getType())),
				injectMethods.stream().flatMap(m -> stream(m.getParameterTypes())))
				.toList();
	}

	private static <T> List<Method> getInjectMethods(Class<T> implementation) {
		List<Method> injectMethods = new ArrayList<>();
		Class<?> current = implementation;
		while (current != Object.class) {
			injectMethods.addAll(stream(current.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Inject.class))
				.filter(m->injectMethods.stream().noneMatch(o->o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(),m.getParameterTypes())))
				.filter(m-> stream(implementation.getDeclaredMethods()).filter(m1->!m1.isAnnotationPresent(Inject.class))
					.noneMatch(o->o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(),m.getParameterTypes()))).toList());
			current = current.getSuperclass();
		}
		 Collections.reverse(injectMethods);
		return injectMethods;
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
