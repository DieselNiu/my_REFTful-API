import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
	private final Constructor<T> injectConstructors;

	public ConstructorInjectionProvider(Class<T> implementation) {
		this.injectConstructors = getInjectConstructors(implementation);
	}

	private Constructor<T> getInjectConstructors(Class<T> implementation) {
		List<Constructor<?>> constructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
		if (constructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) constructors.stream().findFirst().orElseGet(() -> {
			try {
				return implementation.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new IllegalComponentException();
			}
		});
	}


	@Override
	public T get(Context context) {
		try {
			Object[] dependencies = stream(injectConstructors.getParameters()).map(p -> context.get(p.getType()).get()).toArray(Object[]::new);
			return injectConstructors.newInstance(dependencies);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependency() {
		return stream(injectConstructors.getParameters()).map(Parameter::getType).collect(Collectors.toList());
	}
}
