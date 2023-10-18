import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<T> {
	private Type container;
	private ComponentTest component;

	public static <T> ComponentRef<T> of(Class<T> component) {
		return new ComponentRef<>(component,null);
	}


	public static <T> ComponentRef<T> of(Class<T> component, Annotation qualifier) {
		return new ComponentRef<>(component, qualifier);
	}

	public static ComponentRef of(Type type) {
		return new ComponentRef(type, null);
	}


	ComponentRef(Type type, Annotation qualifier) {
		init(type, qualifier);
	}


	protected ComponentRef() {
		Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		init(type, null);
	}

	private void init(Type type, Annotation qualifier) {
		if (type instanceof ParameterizedType container) {
			this.container = container.getRawType();
			this.component = new ComponentTest((Class<?>) container.getActualTypeArguments()[0],qualifier);
		} else {
			this.component = new ComponentTest( (Class<?>) type,qualifier);
		}
	}


	public ComponentTest component() {
		return component;
	}

	public Type getContainer() {
		return container;
	}

	public Class<?> getComponentType() {
		return component.type();
	}

	public boolean isContainer() {
		return container != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ComponentRef<?> that = (ComponentRef<?>) o;
		return Objects.equals(container, that.container) && Objects.equals(component, that.component);
	}

	@Override
	public int hashCode() {
		return Objects.hash(container, component);
	}
}
