package org.bargsten.valacc

import cats.data.Validated.Valid

import scala.util.{Failure, Success, Try}

extension [A](t: Try[A])
  def toValidated: Validated[Throwable, A] = t match
    case Success(a) => Valid(a)
    case Failure(e) => Validated.invalidOne(e)

  def toValidated[E](f: Throwable => E): Validated[E, A] = t match
    case Success(a) => Valid(a)
    case Failure(e) => Validated.invalidOne(f(e))
