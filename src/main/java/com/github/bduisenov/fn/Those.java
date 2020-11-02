package com.github.bduisenov.fn;

import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.vavr.API.Left;
import static io.vavr.API.None;
import static io.vavr.API.Right;
import static io.vavr.API.Some;
import static io.vavr.API.Tuple;
import static java.util.function.Function.identity;

/*
 * Represents a right-biased disjunction with three possibilities: a left value, a right value, or
 * both a left and right value (This, That, and These respectively).
 *
 * `Those<A, B>` is similar to `Either<A, B>`, except that it can represent the simultaneous presence of
 * an `A` and a `B`. It is right-biased so methods such as `map` operate on the
 * `B` value.
 *
 * @param <A>
 * @param <B>
 */
public abstract class Those<A, B> {

    private Those() {
        // NOOP
    }

    public static <A, B> Those<A, B> That(A left) {
        return new That<>(left);
    }

    public static <A, B> Those<A, B> This(B right) {
        return new This<>(right);
    }

    public static <A, B> Those<A, B> These(A left, B right) {
        return new These<>(left, right);
    }

    public <C> C fold(Function<A, C> leftMapper, Function<B, C> rightMapper, BiFunction<A, B, C> bothMapper) {
        if (this instanceof That) {
            final That<A, B> that = (That<A, B>) this;
            return leftMapper.apply(that.left);
        }

        if (this instanceof This) {
            final This<A, B> aThis = (This<A, B>) this;
            return rightMapper.apply(aThis.right);
        }

        if (this instanceof These) {
            These<A, B> these = (These<A, B>) this;
            return bothMapper.apply(these.left, these.right);
        }

        throw new UnsupportedOperationException("Unknown instance of Those");
    }

    /**
     * @return whether the receiver contains a left value.
     */
    public boolean isLeft() {
        return fold($_ -> true, $_ -> false, ($1, $2) -> false);
    }

    /**
     * @return whether the receiver contains a right value.
     */
    public boolean isRight() {
        return fold($_ -> false, $_ -> true, ($1, $2) -> false);
    }

    /**
     * @return whether the receiver contains both a left and right value.
     */
    public boolean isBoth() {
        return fold($_ -> false, $_ -> false, ($1, $2) -> true);
    }

    public Option<A> left() {
        return fold(API::Some, $_ -> None(), (left, $_) -> Some(left));
    }

    public Option<B> right() {
        return fold($_ -> None(), API::Some, ($_, right) -> Some(right));
    }

    public Option<A> onlyLeft() {
        return fold(API::Some, $_ -> None(), ($1, $2) -> None());
    }

    public Option<B> onlyRight() {
        return fold($_ -> None(), API::Some, ($1, $2) -> None());
    }

    public Option<Either<A, B>> onlyLeftOrRight() {
        return fold(left -> Some(Left(left)), right -> Some(Right(right)), ($1, $2) -> None());
    }

    public Option<Tuple2<A, B>> onlyBoth() {
        return fold($_ -> None(), $_ -> None(), (left, right) -> Some(Tuple(left, right)));
    }

    public Either<A, B> toEither() {
        return fold(API::Left, API::Right, ($_, right) -> Right(right));
    }

    public Option<B> toOption() {
        return right();
    }

    public B getOrElse(B other) {
        return right().getOrElse(other);
    }

    public <C, D> Those<C, D> bimap(Function<A, C> thatMapper, Function<B, D> thisMapper) {
        return fold(
                left -> That(thatMapper.apply(left)),
                right -> This(thisMapper.apply(right)),
                (left, right) -> These(thatMapper.apply(left), thisMapper.apply(right)));
    }

    public <D> Those<A, D> map(Function<B, D> thisMapper) {
        return bimap(identity(), thisMapper);
    }

    public <C> Those<C, B> mapLeft(Function<A, C> thatMapper) {
        return bimap(thatMapper, identity());
    }

    static <A, B> Option<Those<A, B>> fromOptions(Option<A> oa, Option<B> ob) {
        if (oa.isDefined()) {
            if (ob.isDefined()) {
                return Some(These(oa.get(), ob.get()));
            } else {
                return Some(That(oa.get()));
            }
        } else {
            if (ob.isDefined()) {
                return Some(This(ob.get()));
            } else {
                return None();
            }
        }
    }

    static <A, B> Those<A, B> fromEither(Either<A, B> eab) {
        return eab.fold(Those::That, Those::This);
    }

    private static final class That<L, R> extends Those<L, R> {

        private final L left;

        public That(L left) {
            this.left = left;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            That<?, ?> that = (That<?, ?>) o;
            return Objects.equals(left, that.left);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left);
        }

        @Override
        public String toString() {
            return "That(" + left + ")";
        }
    }

    private static final class This<L, R> extends Those<L, R> {

        private final R right;

        public This(R right) {
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            This<?, ?> aThis = (This<?, ?>) o;
            return Objects.equals(right, aThis.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(right);
        }

        @Override
        public String toString() {
            return "This(" + right + ")";
        }
    }

    private static final class These<L, R> extends Those<L, R> {

        private final L left;

        private final R right;

        public These(L left, R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            These<?, ?> these = (These<?, ?>) o;
            return Objects.equals(left, these.left) &&
                    Objects.equals(right, these.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }

        @Override
        public String toString() {
            return "These(" + left + ", " + right + ")";
        }
    }
}
