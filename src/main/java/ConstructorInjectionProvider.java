import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
	private Constructor<T> constructor;

	public ConstructorInjectionProvider(Class<T> implementation) {
		this.constructor = getInjectConstructor(implementation);
	}


	private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
		if (injectConstructors.size() > 1) throw new IllegalComponentException();
		return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
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
			Object[] dependencies = Arrays.stream(constructor.getParameters()).map(c -> context.get(c.getType()).get()).toArray(Object[]::new);
			return constructor.newInstance(dependencies);
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getDependencies() {
		return Arrays.stream(constructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
	}
}
