package com.github.bduisenov.fn;

import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.github.bduisenov.fn.Those.That;
import static com.github.bduisenov.fn.Those.These;
import static com.github.bduisenov.fn.Those.This;
import static io.vavr.API.For;
import static io.vavr.API.None;
import static io.vavr.API.Option;
import static io.vavr.API.Some;
import static io.vavr.API.Tuple;
import static org.assertj.core.api.Assertions.assertThat;

class ThoseTest {

    static Stream<Those<String, Integer>> thoseProvider() {
        return Stream.of(That("some.value"), This(123), These("some.value", 123));
    }

    static Stream<Tuple2<Option<String>, Option<Integer>>> optionsProvider() {
        return Stream.of(
                Tuple(Option("some.value"), Option(123)),
                Tuple(None(), Option(123)),
                Tuple(Option("some.value"), None()),
                Tuple(None(), None()));
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void leftOption_isDefined_leftAndBoth(Those<String, Integer> arg) {
        assertThat(arg.isLeft() || arg.isBoth()).isEqualTo(arg.left().isDefined());
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void rightOption_isDefined_forRightAndBoth(Those<String, Integer> arg) {
        assertThat(arg.isRight() || arg.isBoth()).isEqualTo(arg.right().isDefined());
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void onlyLeftOrRight(Those<String, Integer> arg) {
        final Option<Either<String, Integer>> onlyLeftEitherOpt = arg.onlyLeft().map(API::Left);
        final Option<Either<String, Integer>> onlyRightEitherOpt = arg.onlyRight().map(API::Right);

        assertThat(onlyLeftEitherOpt.orElse(onlyRightEitherOpt)).isEqualTo(arg.onlyLeftOrRight());
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void onlyBoth_consistentWith_leftAndRight(Those<String, Integer> arg) {
        assertThat(arg.onlyBoth()).isEqualTo(For(arg.left(), arg.right()).yield(API::Tuple));
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void isLeft_consistentWith_toOption(Those<String, Integer> arg) {
        assertThat(arg.isLeft()).isEqualTo(arg.toOption().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("optionsProvider")
    void fromOptions_leftOrRight_consistentWithInputOptions(Tuple2<Option<String>, Option<Integer>> arg) {
        Option<Those<String, Integer>> x = Those.fromOptions(arg._1, arg._2);

        assertThat(x.flatMap(Those::left)).isEqualTo(arg._1);
        assertThat(x.flatMap(Those::right)).isEqualTo(arg._2);
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void option_roundtrip(Those<String, Integer> arg) {
        Option<Those<String, Integer>> thoseMaybe = Those.fromOptions(arg.left(), arg.right());
        assertThat(thoseMaybe).isEqualTo(Some(arg));
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void toEither_consistentWithRight(Those<String, Integer> arg) {
        assertThat(arg.toEither().toOption()).isEqualTo(arg.right());
    }

    @ParameterizedTest
    @MethodSource("thoseProvider")
    void getOrElse_consistentWith_optionGetOrElse(Those<String, Integer> arg) {
        int other = 456;

        assertThat(arg.getOrElse(other)).isEqualTo(arg.toOption().getOrElse(other));
    }
}