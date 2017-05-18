package com.github.bduisenov.fn;

import io.vavr.Function1;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;

import static io.vavr.API.Tuple;

/**
 * The State Monad represents a computation that threads a piece of state
 * through each step.
 *
 * @param <S>
 * @param <A>
 */
public class State<S, A> {

    private Function1<S, Tuple2<S, A>> runState;

    /**
     * Creates a new State Monad given a function from a piece of state to a
     * value and an updated state.
     *
     * @param runState
     */
    public State(Function1<S, Tuple2<S, A>> runState) {
        this.runState = runState;
    }

    /**
     * Evaluates the computation given an initial state then returns a final
     * value after running each step.
     *
     * @param s
     * @return
     */
    public A eval(S s) {
        return runState.apply(s)._2;
    }

    /**
     * Evaluates the computation given an initial state then returns the final
     * state after running each step.
     *
     * @param s
     * @return
     */
    public S exec(S s) {
        return runState.apply(s)._1;
    }

    public Tuple2<S, A> run(S s) {
        return runState.apply(s);
    }

    // Executes an action that can modify the inner state.
    public State<S, A> withState(Function1<S, S> f) {
        return new State<>(f.andThen(runState));
    }

    // Functor
    public <B> State<S, B> map(Function1<? super A, ? extends B> f) {
        return new State<>(s -> {
            Tuple2<S, A> val = runState.apply(s);
            return Tuple(val._1, f.apply(val._2));
        });
    }

    // Monad
    public <B> State<S, B> flatMap(Function1<A, State<S, B>> f) {
        return new State<>(s -> {
            Tuple2<S, A> val = runState.apply(s);
            return f.apply(val._2).runState.apply(val._1);
        });
    }

    // Applicative
    public <B> State<S, B> ap(State<S, Function1<A, B>> stfn) {
        return stfn.flatMap(f ->
                this.flatMap(a ->
                        pure(f.apply(a))));
    }

    // Cartesian
    public <B> State<S, Tuple2<A, B>> product(State<S, B> r) {
        return new State<>(c -> {
            Tuple2<S, B> d_ = r.runState.apply(c);
            Tuple2<S, A> ef = runState.apply(c);
            return Tuple(ef._1, Tuple(ef._2, d_._2));
        });
    }

    public <B, C> State<S, Tuple3<A, B, C>> product(State<S, B> r, State<S, C> s) {
        return new State<>(c -> {
            Tuple2<S, C> d_ = s.runState.apply(c);
            Tuple2<S, B> e_ = r.runState.apply(c);
            Tuple2<S, A> fg = runState.apply(c);
            return Tuple(fg._1, Tuple(fg._2, e_._2, d_._2));
        });
    }

    public <B, C, D> State<S, Tuple4<A, B, C, D>> product(State<S, B> r, State<S, C> s, State<S, D> t) {
        return new State<>(c -> {
            Tuple2<S, D> d_ = t.runState.apply(c);
            Tuple2<S, C> e_ = s.runState.apply(c);
            Tuple2<S, B> f_ = r.runState.apply(c);
            Tuple2<S, A> gh = runState.apply(c);
            return Tuple(gh._1, Tuple(gh._2, f_._2, e_._2, d_._2));
        });
    }

    // Fetches the current value of the state.
    public static <S> State<S, S> get() {
        return new State<>(s -> Tuple(s, s));
    }

    // Sets the state.
    public static <S> State<S, Void> put(S s) {
        return new State<>($_ -> Tuple(s, null));
    }

    // Gets a specific component of the state using a projection function.
    public static <S, A> State<S, A> gets(Function1<S, A> f) {
        return new State<>(s -> Tuple(s, f.apply(s)));
    }

    // Updates the state with the result of executing the given function.
    public static <S> State<S, Void> modify(Function1<S, S> f) {
        return new State<>(s -> Tuple(f.apply(s), null));
    }

    public static <S, A> State<S, A> pure(A a) {
        return new State<>(s -> Tuple(s, a));
    }

    public static <S, A> State<S, A> state(Function1<S, Tuple2<S, A>> runState) {
        return new State<>(runState);
    }

    // ApplicativeOps
    public static <S, A, B> Function1<State<S, A>, State<S, B>> liftA(Function1<A, B> f) {
        return a -> a.ap(pure(f));
    }

    public static <S, A, B, C> Function1<State<S, A>, Function1<State<S, B>, State<S, C>>> liftA2(Function1<A, Function1<B, C>> f) {
        return a -> b -> b.ap(a.map(f));
    }

    public static <S, A, B, C, D> Function1<State<S, A>, Function1<State<S, B>, Function1<State<S, C>, State<S, D>>>> liftA3(
            Function1<A, Function1<B, Function1<C, D>>> f) {
        return a -> b -> c -> c.ap(b.ap(a.map(f)));
    }

    // MonadOps
    public static <S, A, B> Function1<State<S, A>, State<S, B>> liftM(Function1<A, B> f) {
        return m1 -> m1.flatMap(x1 -> pure(f.apply(x1)));
    }

    public static <S, A, B, C> Function1<State<S, A>, Function1<State<S, B>, State<S, C>>> liftM2(Function1<A, Function1<B, C>> f) {
        return m1 -> m2 -> m1.flatMap(x1 -> m2.flatMap(x2 -> pure(f.apply(x1).apply(x2))));
    }

    public static <S, A, B, C, D> Function1<State<S, A>, Function1<State<S, B>, Function1<State<S, C>, State<S, D>>>> liftM3(
            Function1<A, Function1<B, Function1<C, D>>> f) {
        return m1 -> m2 -> m3 -> m1.flatMap(x1 -> m2.flatMap(x2 -> m3.flatMap(x3 -> pure(f.apply(x1).apply(x2).apply(x3)))));
    }
}
