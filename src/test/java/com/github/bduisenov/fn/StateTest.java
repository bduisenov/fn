package com.github.bduisenov.fn;

import io.vavr.Function1;
import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import static com.github.bduisenov.fn.State.state;
import static io.vavr.API.List;
import static io.vavr.API.Tuple;
import static io.vavr.Function1.identity;
import static org.assertj.core.api.Assertions.assertThat;

class StateTest {

    final State<Integer, Integer, Integer> add1 = state(n -> Tuple(n + 1, n));

    @Test
    void basicStateUsage() {
        assertThat(add1.run(1)).isEqualTo(Tuple(2, 1));
    }

    @Test
    void basicIndexedStateUsage() {
        State<List<Integer>, Option<Integer>, Void> listHead = State.modify(Traversable::headOption);
        State<Option<Integer>, Integer, Void> getOrElse = State.modify(val -> val.getOrElse(0));
        State<Integer, String, Void> toString = State.modify(Object::toString);

        State<List<Integer>, String, String> composite = listHead.flatMap($1 ->
                getOrElse.flatMap($2 ->
                        toString.flatMap($3 -> State.get())));

        assertThat(composite.run(List(1, 2, 3))).isEqualTo(Tuple("1", "1"));
        assertThat(composite.run(List.empty())).isEqualTo(Tuple("0", "0"));
    }

    @Test
    void modifyDoesNotAffectAValue() {
        assertThat(add1.withState(identity()).eval(1)).isEqualTo(add1.eval(1));
        assertThat(add1.withState(x -> x + 1).eval(1)).isEqualTo(add1.eval(1));
    }

    @Test
    void modify_equivalentTo_getThenSet() {
        Function1<Integer, Integer> f = x-> x + 1;
        State<Integer, Integer, Void> s1 = State.<Integer>get().flatMap(l ->
                State.put(f.apply(l)));
        State<Integer, Integer, Void> s2 = State.modify(f);

        assertThat(s1.run(1)).isEqualTo(s2.run(1));
    }

    @Test
    void set_equivalentTo_modify_ignoringFirstParam() {
        String init = "some.init.value";
        String update = "some.update.value";
        State<String, String, Void> s1 = State.modify($_ -> update);
        State<String, String, Void> s2 = State.put(update);

        assertThat(s1.run(init)).isEqualTo(s2.run(init));
    }
}