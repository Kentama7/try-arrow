package com.example.core

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.andThen
import arrow.core.computations.either
import arrow.core.contains
import arrow.core.extensions.list.apply.tupled
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.leftIfNull
import arrow.core.right
import arrow.core.rightIfNotNull
import arrow.core.rightIfNull
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException

// https://arrow-kt.io/docs/apidocs/arrow-core-data/arrow.core/-either/
suspend fun main() {
    run {
        val throwsSomeStuff: (Int) -> Double = { x -> x.toDouble() }
        val throwsOtherThings: (Double) -> String = { x -> x.toString() }
        val moreThrowing: (String) -> List<String> = { x -> listOf(x) }
        val magic = throwsSomeStuff.andThen(throwsOtherThings).andThen(moreThrowing)
        println("magic = $magic")
        println("magic = ${magic.invoke(1)}")
    }

    run {
        val right: Either<String, Int> = Either.Right(5)
        println(right)

        val left: Either<String, Int> = Either.Left("Something went wrong")
        println(left)
    }

    run {
        val right: Either<String, Int> = Either.Right(5)
        val valueR = right.flatMap { Either.Right(it + 1) }
        println("value = $valueR")

        val left: Either<String, Int> = Either.Left("Something went wrong")
        val valueL = left.flatMap { Either.Right(it + 1) }
        println("value = $valueL")
    }

    // Using exception-throwing code
    run {
        fun parse(s: String): Int =
            if (s.matches(Regex("-?[0-9]+"))) s.toInt()
            else throw NumberFormatException("$s is not a valid integer.")

        fun reciprocal(i: Int): Double =
            if (i == 0) throw IllegalArgumentException("Cannot take reciprocal of 0.")
            else 1.0 / i

        fun stringify(d: Double): String = d.toString()

        fun magic(s: String): String =
            stringify(reciprocal(parse(s)))

        println(magic("1"))
        println(magic("2"))
        println(magic("5"))
        runCatching { magic("0") }.onFailure { println(it) }
        runCatching { magic("a") }.onFailure { println(it) }
    }

    // Either Style
    run {
        fun parse(s: String): Either<NumberFormatException, Int> =
            if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
            else Either.Left(NumberFormatException("$s is not a valid integer."))

        val notANumber = parse("Not a number")
        val number2 = parse("2")
        println("notAnumber = $notANumber")
        println("number2 = $number2")

        fun reciprocal(i: Int): Either<IllegalArgumentException, Double> =
            if (i == 0) Either.Left(IllegalArgumentException("Cannot take reciprocal of 0."))
            else Either.Right(1.0 / i)

        fun stringify(d: Double): String = d.toString()

        fun magic(s: String): Either<Exception, String> =
            parse(s).flatMap { reciprocal(it) }.map { stringify(it) }

        val magic0 = magic("0")
        val magic1 = magic("1")
        val magicNotANumber = magic("Not a number")
        println("magic0 = $magic0")
        println("magic1 = $magic1")
        println("magicNotANumber = $magicNotANumber")

        // pattern-match
        val patternMatch: (String) -> String = {
            when (val x = magic(it)) {
                is Either.Left -> when (x.a) {
                    is NumberFormatException -> "Not a number!"
                    is IllegalArgumentException -> "Cannot take reciprocal of 0!"
                    else -> "Unknown error"
                }
                is Either.Right -> "Got reciprocal: ${x.b}"
            }
        }

        println(patternMatch("2"))
        println(patternMatch("a"))
        println(patternMatch("0"))
    }

    // Either with ADT Style
    run {
        fun parse(s: String): Either<Error, Int> =
            if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
            else Either.Left(Error.NotANumber)

        fun reciprocal(i: Int): Either<Error, Double> =
            if (i == 0) Either.Left(Error.NoZeroReciprocal)
            else Either.Right(1.0 / i)

        fun stringify(d: Double): String = d.toString()

        fun magic(s: String): Either<Error, String> =
            parse(s).flatMap { reciprocal(it) }.map { stringify(it) }

        val patternMatch: (String) -> String = {
            when (val x = magic(it)) {
                is Either.Left -> when (x.a) {
                    is Error.NotANumber -> "Not a number!"
                    is Error.NoZeroReciprocal -> "Can't take reciprocal of 0!"
                }
                is Either.Right -> "Got reciprocal: ${x.b}"
            }
        }

        println(patternMatch("2"))
        println(patternMatch("a"))
        println(patternMatch("0"))
    }

    // map over the left value with mapLeft
    run {
        val r: Either<Int, Int> = Either.Right(7)
        val rightMapLeft = r.mapLeft { it + 1 }
        println("rightMapLeft = $rightMapLeft")
        val l: Either<Int, Int> = Either.Left(7)
        val leftMapLeft = l.mapLeft { it + 1 }
        println("leftMapLeft = $leftMapLeft")
    }

    // swap
    run {
        val r: Either<String, Int> = Either.Right(7)
        val swapped = r.swap()
        println("r = $r")
        println("swapped = $swapped")
    }

    // arbitrary data types
    run {
        val right7 = 7.right()
        println(right7)
        println(right7.contains(7))
        println(right7.contains(8))
        println(right7.getOrElse { 6 })


        val leftHello = "hello".left()
        println(leftHello)
        println(leftHello.getOrElse { 7 })
        println(leftHello.getOrHandle { "$it world!" })
    }

    // conditionally
    run {
        val value1 = Either.conditionally(true, { "Error" }, { 42 })
        println(value1)

        val value2 = Either.conditionally(false, { "Error" }, { 42 })
        println(value2)
    }

    // fold
    run {
        val x: Either<Int, Int> = 7.right()
        val xfold = x.fold({ 1 }, { it + 3 })
        println(xfold)

        val y: Either<Int, Int> = 7.left()
        val yfold = y.fold({ 1 }, { it + 3 })
        println(yfold)
    }

    // getOrHandle
    run {
        val r: Either<Throwable, Int> = Either.Left(NumberFormatException())
        val httpStatusCode = r.getOrHandle {
            when (it) {
                is NumberFormatException -> 400
                else -> 500
            }
        }
        println(httpStatusCode)
    }

    // leftIfNull rightIfNotNull rightIfNull
    run {
        println(Right(12).leftIfNull { -1 })
        println(Right(null).leftIfNull { -1 })
        println(Left(12).leftIfNull { -1 })

        println("value".rightIfNotNull { "left" })
        println(null.rightIfNotNull { "left" })

        println("value".rightIfNull { "left" })
        println(null.rightIfNull { "left" })
    }

    // Functor
    run {
        println(Right(1).map { it + 1 })
    }

    // Applicative
    run {
//        val value = tupled(Either.Right(1), Either.Right("a"), Either.Right(2.0))
    }

    // Monad
    run {
        val value = either<Int, Int> {
            val (a) = Either.Right(1)
            val (b) = Either.Right(1 + a)
            val (c) = Either.Right(1 + b)
            a + b + c
        }
        println(value)
    }
}

sealed class Error {
    object NotANumber : Error()
    object NoZeroReciprocal : Error()
}
