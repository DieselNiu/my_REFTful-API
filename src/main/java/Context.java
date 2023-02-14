import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Context {

	private Map<Class<?>, Provider<?>> providers = new HashMap<>();

	public <T> void bind(Class<T> type, T instance) {
		providers.put(type, (Provider<T>) () -> instance);
	}

	public <T, I extends T> void bind(Class<T> type, Class<I> implementation) {
		Object[] injectConstructors = Arrays.stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toArray(Object[]::new);
		if (injectConstructors.length > 1) throw new IllegalComponentException();
		if(injectConstructors.length == 0 && Arrays.stream(implementation.getConstructors()).filter(c->c.getParameters().length == 0).findFirst().map(c->false).orElse(true)) throw new IllegalComponentException();
		providers.put(type, (Provider<T>) () -> {
			try {
				Constructor<I> constructor = getInjectConstructor(implementation);
				Object[] dependencies = Arrays.stream(constructor.getParameters()).map(c -> get(c.getType())).toArray(Object[]::new);
				return constructor.newInstance(dependencies);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
		return (Constructor<T>) Arrays.stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).findFirst().orElseGet(() -> {
			try {
				return implementation.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public <T> T get(Class<T> type) {
		return (T) providers.get(type).get();
	}


}
