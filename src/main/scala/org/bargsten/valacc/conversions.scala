package org.bargsten.valacc

import cats.data.Validated.Valid
import cats.data.{NonEmptyList, Validated}

import scala.util.{Failure, Success, Try}

extension [A](t: Try[A])
  def toValidated: Validated[NonEmptyList[Throwable], A] = t match
    case Success(a) => Valid(a)
    case Failure(e) => ValAcc.invalidOne(e)

  def toValidated[E](f: Throwable => E): Validated[NonEmptyList[E], A] = t match
    case Success(a) => Valid(a)
    case Failure(e) => ValAcc.invalidOne(f(e))
