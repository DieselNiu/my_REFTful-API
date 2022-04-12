import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class CanaryTest {
    @Test
    void should_be_able_to_run_a_trivial_test_case() {
        assertThat(1 + 2).isEqualTo(3);
    }

    @Test
    void should_be_able_to_use_guava_capabilities() {
        List<Integer> numbers = newArrayList(1, 2, 3, 4, 5);
        List<Integer> doubled = numbers.stream().map(x -> x * 2).collect(toList());
        assertThat(doubled).isEqualTo(newArrayList(2, 4, 6, 8, 10));
    }
}
