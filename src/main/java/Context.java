import java.util.Optional;

interface Context {
	<T> Optional<T> get(Class<T> type);
}
