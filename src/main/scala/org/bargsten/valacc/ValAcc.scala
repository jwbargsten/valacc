package org.bargsten.valacc

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}

/*
sealed trait Validated[+E, +A]:
  def isValid: Boolean
  def isInvalid: Boolean = !isValid

  def contains[A2 >: A](elem: A2): Boolean = this match
    case Valid(a) => a == elem
    case _        => false

  def exists(p: A => Boolean): Boolean = this match
    case Valid(a) => p(a)
    case _        => false

  def forall(p: A => Boolean): Boolean = this match
    case Valid(a) => p(a)
    case _        => true

  def foreach(f: A => Unit): Unit = this match
    case Valid(a) => f(a)
    case _        =>

  def fold[B](fe: NonEmptyList[E] => B, fa: A => B): B = this match
    case Valid(a)        => fa(a)
    case Invalid(errors) => fe(errors)

  def map[B](f: A => B): Validated[E, B] = this match
    case Valid(a)        => Valid(f(a))
    case inv: Invalid[E] => inv

  def flatMap[E2 >: E, B](f: A => Validated[E2, B]): Validated[E2, B] = this match
    case Valid(a)        => f(a)
    case inv: Invalid[E] => inv

  def filterOrElse[E2 >: E](p: A => Boolean, error: => E2): Validated[E2, A] = this match
    case v @ Valid(a)    => if p(a) then v else Invalid(NonEmptyList.one(error))
    case inv: Invalid[E] => inv

  def mapErrors[F](f: NonEmptyList[E] => NonEmptyList[F]): Validated[F, A] = this match
    case v: Valid[A]     => v
    case Invalid(errors) => Invalid(f(errors))

  def mapEachError[F](f: E => F): Validated[F, A] = this match
    case v: Valid[A]     => v
    case Invalid(errors) => Invalid(errors.map(f))

  def recover[A2 >: A](f: NonEmptyList[E] => A2): Validated[Nothing, A2] = this match
    case v: Valid[A]     => v
    case Invalid(errors) => Valid(f(errors))

  def getOrElse[A2 >: A](default: => A2): A2 = this match
    case Valid(a) => a
    case _        => default

  def orElse[E2, A2 >: A](default: => Validated[E2, A2]): Validated[E2, A2] = this match
    case v: Valid[A] => v
    case _           => default

  def toOption: Option[A] = this match
    case Valid(a) => Some(a)
    case _        => None

  def toEither: Either[NonEmptyList[E], A] = this match
    case Valid(a)        => Right(a)
    case Invalid(errors) => Left(errors)

  def swap: Validated[A, NonEmptyList[E]] = this match
    case Valid(a)        => Invalid(NonEmptyList.one(a))
    case Invalid(errors) => Valid(errors)

  def tap(f: A => Unit): Validated[E, A] =
    this match
      case Valid(a) => f(a)
      case _        =>
    this

  def tapInvalid(f: NonEmptyList[E] => Unit): Validated[E, A] =
    this match
      case Invalid(errors) => f(errors)
      case _               =>
    this

  def zip[E2 >: E, B, C](other: Validated[E2, B])(f: (A, B) => C): Validated[E2, C] =
    (this, other) match
      case (Valid(a), Valid(b))       => Valid(f(a, b))
      case (Invalid(e1), Invalid(e2)) => Invalid(e1 ::: e2)
      case (inv: Invalid[E2], _)      => inv
      case (_, inv: Invalid[E2])      => inv

  def zip[E2 >: E](other: Validated[E2, ?]): Validated[E2, A] =
    (this, other) match
      case (Valid(a), Valid(_))       => Valid(a)
      case (Invalid(e1), Invalid(e2)) => Invalid(e1 ::: e2)
      case (inv: Invalid[E2], _)      => inv
      case (_, Invalid(e2))           => Invalid(e2)

  def toCats: cats.data.Validated[NonEmptyList[E], A] =
    this match
      case Valid(v)      => cats.data.Validated.Valid(v)
      case Invalid(errs) => cats.data.Validated.Invalid(errs)

end Validated

final case class Valid[+A](value: A) extends Validated[Nothing, A]:
  def isValid = true

final case class Invalid[+E](errors: NonEmptyList[E]) extends Validated[E, Nothing]:
  def isValid = false

 */

object ValAcc:
  def valid[A](a: A): Validated[Nothing, A] = Valid(a)
  def invalid[E](errors: NonEmptyList[E]): Validated[NonEmptyList[E], Nothing] = Invalid(errors)
  def invalidOne[E](e: E): Validated[NonEmptyList[E], Nothing] = Invalid(NonEmptyList.one(e))
  val unit: Validated[Nothing, Unit] = Valid(())

  def fromOption[E, A](opt: Option[A])(error: => E): Validated[NonEmptyList[E], A] = opt match
    case Some(a) => Valid(a)
    case None    => invalidOne(error)

  def fromEither[E, A](either: Either[E, A]): Validated[NonEmptyList[E], A] = either match
    case Right(a) => Valid(a)
    case Left(e)  => invalidOne(e)

  def fromTry[A](t: scala.util.Try[A]): Validated[NonEmptyList[Throwable], A] = t match
    case scala.util.Success(a) => Valid(a)
    case scala.util.Failure(e) => invalidOne(e)

  def cond[E, A](test: Boolean, a: => A, error: => E): Validated[NonEmptyList[E], A] =
    if test then Valid(a) else invalidOne(error)

  def validateAll[E](validations: Validated[NonEmptyList[E], ?]*): Validated[NonEmptyList[E], Unit] =
    val errors = validations.collect { case Invalid(es) => es }
    if errors.isEmpty then Valid(())
    else Invalid(errors.reduce(_ ::: _))

  extension [E,A](v: Validated[NonEmptyList[E], A])
    def flatMap[B, EE >: E](f: A => Validated[NonEmptyList[EE], B]): Validated[NonEmptyList[EE], B] = v.andThen(f)

  extension [E, A](list: List[Validated[NonEmptyList[E], A]])
    def sequence: Validated[NonEmptyList[E], List[A]] =
      val values = List.newBuilder[A]
      val errors = List.newBuilder[E]
      list.foreach:
        case Valid(a)    => values += a
        case Invalid(es) => errors ++= es.toList
      val errs = errors.result()
      NonEmptyList.fromList(errs) match
        case Some(nel) => Invalid(nel)
        case None      => Valid(values.result())
