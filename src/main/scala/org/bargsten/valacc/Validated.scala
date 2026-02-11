package org.bargsten.valacc

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, Validated as CatsValidated}

type Validated[+E, +A] = cats.data.Validated[NonEmptyChain[E], A]

object Validated:
  def valid[A](a: A): Validated[Nothing, A] = Valid(a)
  def invalid[E](errors: NonEmptyChain[E]): Validated[E, Nothing] = Invalid(errors)
  def invalidOne[E](e: E): Validated[E, Nothing] = Invalid(NonEmptyChain.one(e))
  val unit: Validated[Nothing, Unit] = Valid(())

  def fromOption[E, A](opt: Option[A])(error: => E): Validated[E, A] = opt match
    case Some(a) => Valid(a)
    case None    => invalidOne(error)

  def fromEither[E, A](either: Either[E, A]): Validated[E, A] = either match
    case Right(a) => Valid(a)
    case Left(e)  => invalidOne(e)

  def fromTry[A](t: scala.util.Try[A]): Validated[Throwable, A] = t match
    case scala.util.Success(a) => Valid(a)
    case scala.util.Failure(e) => invalidOne(e)

  def cond[E, A](test: Boolean, a: => A, error: => E): Validated[E, A] =
    if test then Valid(a) else invalidOne(error)

  def validateAll[E](validations: Validated[E, ?]*): Validated[E, Unit] =
    val errors = validations.collect { case Invalid(es) => es }
    if errors.isEmpty then Valid(())
    else Invalid(errors.reduce(_ ++ _))

  extension [E, A](v: Validated[E, A])
    def flatMap[B, EE >: E](f: A => Validated[EE, B]): Validated[EE, B] = v.andThen(f)

    def contains[A2 >: A](elem: A2): Boolean = v match
      case Valid(a) => a == elem
      case _        => false

    def filterOrElse(p: A => Boolean, error: => E): Validated[E, A] = v match
      case Valid(a) => if p(a) then v else Invalid(NonEmptyChain.one(error))
      case _        => v

    def tap(f: A => Unit): Validated[E, A] =
      v.foreach(f)
      v

    def tapInvalid(f: NonEmptyChain[E] => Unit): Validated[E, A] =
      v match
        case Invalid(errors) => f(errors)
        case _               =>
      v

    def mapErrors[F](f: NonEmptyChain[E] => NonEmptyChain[F]): Validated[F, A] = v.leftMap(f)

    def mapEachError[F](f: E => F): Validated[F, A] = v.leftMap(_.map(f))

    def recover[A2 >: A](f: NonEmptyChain[E] => A2): Validated[Nothing, A2] = v match
      case Valid(a)        => Valid(a)
      case Invalid(errors) => Valid(f(errors))

  extension [E, A](list: List[Validated[E, A]])
    def sequence: Validated[E, List[A]] =
      val values = List.newBuilder[A]
      val errors = List.newBuilder[E]
      list.foreach:
        case Valid(a)    => values += a
        case Invalid(es) => errors ++= es.iterator
      val errs = errors.result()
      NonEmptyChain.fromSeq(errs) match
        case Some(nec) => Invalid(nec)
        case None      => Valid(values.result())
end Validated
