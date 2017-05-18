package com.github.bduisenov.fn;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Lens offers a way of focusing on a particular part of a data structure (possibly nested).
 * Using a lens we can access the view part and read, write or modify it.
 *
 * @param <A>
 * @param <B>
 */
public class Lens<A, B> {

    private final Function<A, B> get;

    private final BiFunction<A, B, A> set;

    public Lens(Function<A, B> get, BiFunction<A, B, A> set) {
        this.get = get;
        this.set = set;
    }

    /**
     * Function extracts the view from a source.
     *
     * @param a represents the source type.
     * @return the view type
     */
    public B get(A a) {
        return get.apply(a);
    }

    /**
     * Sets a view value in a source.
     *
     * @param a represents the source type.
     * @param b represents the view type.
     * @return new object with refreshed data
     */
    public A set(A a, B b) {
        return set.apply(a, b);
    }

    /**
     * Function that gets {@code B} from argument {@code A} and applies {@code f} (modifier) to it.
     * f.ex. {@code Person.Name.mod(greatPerson, Function.identity())} does nothing.
     *
     * @param a argument
     * @param f modifier
     * @return A
     */
    public A mod(A a, Function<B, B> f) {
        return set(a, f.apply(get(a)));
    }

    public <C> Lens<C, B> compose(Lens<C, A> that) {
        return new Lens<>(
                c -> get(that.get(c)),
                (c, b) -> that.mod(c, a -> set(a, b)));
    }

    /**
     * Function which is used for composition of lenses for updating nested objects.
     * f.ex. {@code Person.Address.andThen(Address.ZipCode).set(greatPerson, 1234)} will
     * return new {@code Person} with updated new {@code Address} containing new {@code zipcode} 1234.
     * or in other words
     * {@code Lens<A, B>}  composed with {@code Lens<B, C>} results into {@code Lens<A, C>}
     *
     * @param that next lens
     * @param <C>  object
     * @return updated object
     */
    public <C> Lens<A, C> andThen(Lens<B, C> that) {
        return that.compose(this);
    }
}
