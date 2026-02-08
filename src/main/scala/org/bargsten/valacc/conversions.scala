package org.bargsten.valacc

import scala.util.{Try, Success, Failure}

extension [A](t: Try[A])
  def toValidated: Validated[Throwable, A] = t match
    case Success(a) => Valid(a)
    case Failure(e) => Validated.invalidOne(e)

  def toValidated[E](f: Throwable => E): Validated[E, A] = t match
    case Success(a) => Valid(a)
    case Failure(e) => Validated.invalidOne(f(e))
