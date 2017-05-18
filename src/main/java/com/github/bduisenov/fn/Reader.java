package com.github.bduisenov.fn;

import io.vavr.Function1;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;

import static io.vavr.API.Tuple;
import static io.vavr.Function1.constant;
import static io.vavr.Function1.identity;

/**
 * A `Reader` monad with `R` for environment and `A` to represent the modified
 * environment.
 *
 * @param <R>
 * @param <A>
 */
public class Reader<R, A> {

    // The function that modifies the environment
    private final Function1<R, A> reader;

    public Reader(Function1<R, A> reader) {
        this.reader = reader;
    }

    // Runs the reader and extracts the final value from it
    public A runReader(R environment) {
        return reader.apply(environment);
    }

    // Executes a computation in a modified environment
    public Reader<R, A> local(Function1<R, R> f) {
        return new Reader<>(f.andThen(reader));
    }

    // Functor
    public <B> Reader<R, B> map(Function1<A, B> f) {
        return new Reader<>(e ->
                f.apply(reader.apply(e)));
    }

    // Monad
    public <B> Reader<R, B> flatMap(Function1<A, Reader<R, B>> f) {
        return new Reader<>(e ->
                f.apply(reader.apply(e)).reader.apply(e));
    }

    // Cartesian
    public <B> Reader<R, Tuple2<A, B>> product(Reader<R, B> r) {
        return new Reader<>(c -> Tuple(this.runReader(c), r.runReader(c)));
    }

    public <B, C> Reader<R, Tuple3<A, B, C>> product(Reader<R, B> r, Reader<R, C> s) {
        return new Reader<>(c -> Tuple(this.runReader(c), r.runReader(c), s.runReader(c)));
    }

    public <B, C, D> Reader<R, Tuple4<A, B, C, D>> product(Reader<R, B> r, Reader<R, C> s, Reader<R, D> t) {
        return new Reader<>(c -> Tuple(this.runReader(c), r.runReader(c), s.runReader(c), t.runReader(c)));
    }

    public static <R, A> Function1<R, A> runReader(Reader<R, A> reader) {
        return reader::runReader;
    }

    // Runs the reader and extracts the final value from it. This provides a global
    // function for running a reader.
    public static <R, A> Reader<R, A> reader(Function1<R, A> f) {
        return new Reader<>(f);
    }

    // Retrieves the monad environment
    public static <R> Reader<R, R> ask() {
        return new Reader<>(identity());
    }

    // Retrieves a function of the current environment
    public static <R, A> Reader<R, A> asks(Function1<R, A> f) {
        return new Reader<>(f);
    }

    // Pointed
    public static <R, A> Reader<R, A> pure(A a) {
        return new Reader<>(constant(a));
    }

    // MonadOps
    public static <R, A, B> Function1<Reader<R, A>, Reader<R, B>> liftM(Function1<A, B> f) {
        return m1 -> m1.flatMap(x1 -> pure(f.apply(x1)));
    }

    public static <R, A, B, C> Function1<Reader<R, A>, Function1<Reader<R, B>, Reader<R, C>>> liftM2(Function1<A, Function1<B, C>> f) {
        return m1 -> m2 -> m1.flatMap(x1 -> m2.flatMap(x2 -> pure(f.apply(x1).apply(x2))));
    }

    public static <R, A, B, C, D> Function1<Reader<R, A>, Function1<Reader<R, B>, Function1<Reader<R, C>, Reader<R, D>>>> liftM3(
            Function1<A, Function1<B, Function1<C, D>>> f) {
        return m1 -> m2 -> m3 -> m1.flatMap(x1 -> m2.flatMap(x2 -> m3.flatMap(x3 -> pure(f.apply(x1).apply(x2).apply(x3)))));
    }
}
