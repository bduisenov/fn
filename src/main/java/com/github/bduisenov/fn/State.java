package com.github.bduisenov.fn;

import io.vavr.Function1;
import io.vavr.Function2;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;

import static io.vavr.API.Tuple;
import static io.vavr.Function1.identity;

/**
 * The State Monad represents a computation that threads a piece of state
 * through each step.
 *
 * @param <SA>
 * @param <A>
 */
public class State<SA, SB, A> {

    // type
    public static class StateT<S, A> extends State<S, S, A> {
        public StateT(Function1<S, Tuple2<S, A>> runState) {
            super(runState);
        }
    }

    private final Function1<SA, Tuple2<SB, A>> runState;

    /**
     * Creates a new State Monad given a function from a piece of state to a
     * value and an updated state.
     *
     * @param runState
     */
    public State(Function1<SA, Tuple2<SB, A>> runState) {
        this.runState = runState;
    }

    /**
     * Evaluates the computation given an initial state then returns a final
     * value after running each step.
     *
     * @param s
     * @return
     */
    public A eval(SA s) {
        return run(s)._2;
    }

    /**
     * Evaluates the computation given an initial state then returns the final
     * state after running each step.
     *
     * @param s
     * @return
     */
    public SB exec(SA s) {
        return run(s)._1;
    }

    // Run with the provided initial state value
    public Tuple2<SB, A> run(SA initial) {
        return runState.apply(initial);
    }

    // Executes an action that can modify the inner state.
    public <SC> State<SA, SC, A> withState(Function1<SB, SC> f) {
        return transform((s, a) -> Tuple(f.apply(s), a));
    }

    // Like {@link #map}, but also allows the state (`S`) value to be modified.
    public <B, SC> State<SA, SC, B> transform(Function2<SB, A, Tuple2<SC, B>> f) {
        return new State<>(runState.andThen(f.tupled()));
    }

    // Functor
    public <B> State<SA, SB, B> map(Function1<A, B> f) {
        return transform((s, a) -> Tuple(s, f.apply(a)));
    }

    public <SC, B> State<SA, SC, B> bimap(Function1<SB, SC> f, Function1<A, B> g) {
        return transform((s, a) -> Tuple(f.apply(s), g.apply(a)));
    }

    // Monad
    public <B, SC> State<SA, SC, B> flatMap(Function1<A, State<SB, SC, B>> fas) {
        return new State<>(runState.andThen(sba -> fas.apply(sba._2).run(sba._1)));
    }

    // Applicative
    @SuppressWarnings("unchecked")
    public <B> State<SA, SB, B> ap(State<SA, SB, Function1<A, B>> sasbfab) {
        return sasbfab.flatMap(fab ->
                (State<SB, SB, B>) this.flatMap(a ->
                        pure(fab.apply(a))));
    }

    // Cartesian
    public <B> State<SA, SB, Tuple2<A, B>> product(State<SA, SB, B> sasbb) {
        return new State<>(s -> {
            Tuple2<SB, B> sbb = sasbb.runState.apply(s);
            Tuple2<SB, A> sba = runState.apply(s);
            return Tuple(sba._1, Tuple(sba._2, sbb._2));
        });
    }

    public <B, C> State<SA, SB, Tuple3<A, B, C>> product(State<SA, SB, B> sasbb, State<SA, SB, C> sasbc) {
        return new State<>(s -> {
            Tuple2<SB, C> sbc = sasbc.runState.apply(s);
            Tuple2<SB, B> sbb = sasbb.runState.apply(s);
            Tuple2<SB, A> sba = runState.apply(s);
            return Tuple(sba._1, Tuple(sba._2, sbb._2, sbc._2));
        });
    }

    public <B, C, D> State<SA, SB, Tuple4<A, B, C, D>> product(State<SA, SB, B> sasbb, State<SA, SB, C> sasbc, State<SA, SB, D> sasbd) {
        return new State<>(c -> {
            Tuple2<SB, D> sbd = sasbd.runState.apply(c);
            Tuple2<SB, C> dbc = sasbc.runState.apply(c);
            Tuple2<SB, B> sbb = sasbb.runState.apply(c);
            Tuple2<SB, A> sba = runState.apply(c);
            return Tuple(sba._1, Tuple(sba._2, sbb._2, dbc._2, sbd._2));
        });
    }

    // Fetches the current value of the state.
    public static <S> State<S, S, S> get() {
        return gets(identity());
    }

    // Sets the state.
    public static <S> State<S, S, Void> put(S s) {
        return new State<>($_ -> Tuple(s, null));
    }

    // Gets a specific component of the state using a projection function.
    public static <S, T> State<S, S, T> gets(Function1<S, T> f) {
        return new State<>(s -> Tuple(s, f.apply(s)));
    }

    // Updates the state with the result of executing the given function.
    public static <S> State<S, S, Void> modify(Function1<S, S> f) {
        return new State<>(s -> Tuple(f.apply(s), null));
    }

    @SuppressWarnings("unchecked")
    public static <SA, SB, A> State<SA, SB, A> pure(A a) {
        return new State<>(s -> (Tuple2<SB, A>)Tuple(s, a));
    }

    public static <S, A> State<S, S, A> state(Function1<S, Tuple2<S, A>> runState) {
        return new State<>(runState);
    }

    // ApplicativeOps
    public static <SA, SB, A, B> Function1<State<SA, SB, A >, State<SA, SB, B>> liftA(Function1<A, B> f) {
        return sasba -> sasba.ap(pure(f));
    }

    public static <SA, SB, A, B, C> Function1<State<SA, SB, A>, Function1<State<SA, SB, B>, State<SA, SB, C>>> liftA2(Function1<A, Function1<B, C>> f) {
        return a -> b -> b.ap(a.map(f));
    }

    public static <SA, SB, A, B, C, D> Function1<State<SA, SB, A>, Function1<State<SA, SB, B>, Function1<State<SA, SB, C>, State<SA, SB, D>>>> liftA3(
            Function1<A, Function1<B, Function1<C, D>>> f) {
        return a -> b -> c -> c.ap(b.ap(a.map(f)));
    }

    // MonadOps
    public static <SA, SB, A, B> Function1<State<SA, SB, A>, State<SA, SB, B>> liftM(Function1<A, B> f) {
        return m1 -> m1.flatMap(x1 -> pure(f.apply(x1)));
    }

    @SuppressWarnings("unchecked")
    public static <SA, SB, A, B, C> Function1<State<SA, SB, A>, Function1<State<SA, SB, B>, State<SA, SB, C>>> liftM2(Function1<A, Function1<B, C>> f) {
        return m1 -> m2 -> m1.flatMap(x1 -> (State<SB, SB, C>)m2.flatMap(x2 -> pure(f.apply(x1).apply(x2))));
    }

    @SuppressWarnings("unchecked")
    public static <SA, SB, A, B, C, D> Function1<State<SA, SB, A>, Function1<State<SA, SB, B>, Function1<State<SA, SB, C>, State<SA, SB, D>>>> liftM3(
            Function1<A, Function1<B, Function1<C, D>>> f) {
        return m1 -> m2 -> m3 -> m1.flatMap(x1 -> (State<SB, SB, D>)m2.flatMap(x2 -> (State<SB, SB, D>)m3.flatMap(x3 -> pure(f.apply(x1).apply(x2).apply(x3)))));
    }
}
