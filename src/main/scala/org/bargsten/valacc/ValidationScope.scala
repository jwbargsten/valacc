package org.bargsten.valacc

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, Validated}

import scala.annotation.targetName
import scala.collection.mutable.ListBuffer
import scala.util.control.NoStackTrace

private[valacc] class ValidationException extends Exception, NoStackTrace

class ValidationScope[E]:
  private val _errors = ListBuffer.empty[E]
  private val abort = ValidationException()

  def errors: Option[NonEmptyChain[E]] = NonEmptyChain.fromSeq(_errors.toList)

  private[valacc] def shortcircuit(): Nothing = throw abort
  private[valacc] def isOurException(t: Throwable): Boolean = t eq abort

  // --- accumulating ---

  def ensure(predicate: Boolean)(error: => E): Unit =
    if !predicate then _errors += error

  def ensureFail(error: => E): Unit =
    _errors += error

  def ensureDefined[A](option: Option[A])(error: => E): Option[A] =
    option match
      case some @ Some(_) => some
      case None           => _errors += error; None

  def ensureValue[A](validated: Validated[NonEmptyChain[E], A]): Option[A] =
    validated match
      case Valid(a)        => Some(a)
      case Invalid(errors) => _errors ++= errors.iterator; None

  def attach[A](validated: Validated[NonEmptyChain[E], A]): Validated[NonEmptyChain[E], A] =
    validated match
      case inv @ Invalid(errors) => _errors ++= errors.iterator; inv
      case v                     => v

  // --- short-circuiting ---

  def demand(predicate: Boolean)(error: => E): Unit =
    if !predicate then
      _errors += error
      shortcircuit()

  def demandFail(error: => E): Nothing =
    _errors += error
    shortcircuit()

  def demandDefined[A](option: Option[A])(error: => E): A =
    option match
      case Some(a) => a
      case None    => _errors += error; shortcircuit()

  def demandValue[A](validated: Validated[NonEmptyChain[E], A]): A =
    validated match
      case Valid(a) => a
      case Invalid(errors) =>
        _errors ++= errors.iterator
        shortcircuit()

  def demandValid(validated: Validated[NonEmptyChain[E], ?]): Unit =
    validated match
      case Valid(_) => ()
      case Invalid(errors) =>
        _errors ++= errors.iterator
        shortcircuit()

  // --- extensions available via given scope ---

  private def addMissingErrors(xs: NonEmptyChain[E]) =
    if _errors.isEmpty then _errors ++= xs.iterator
    else
      val seen = new java.util.IdentityHashMap[E, Boolean]()
      _errors.foreach(e => seen.put(e, true))
      _errors ++= xs.filter(x => !seen.containsKey(x)).iterator

  extension [A](validated: Validated[NonEmptyChain[E], A])
    def get: A = validated match
      case Valid(a) => a
      case Invalid(errors) =>
        addMissingErrors(errors)
        shortcircuit()

    @targetName("attachFluent")
    def attach(): Validated[NonEmptyChain[E], A] = this.attach(validated)

  extension [A](value: A)
    def ensure(predicate: A => Boolean)(error: => E): A =
      if !predicate(value) then _errors += error
      value

    def demand(predicate: A => Boolean)(error: => E): A =
      if !predicate(value) then
        _errors += error
        shortcircuit()
      value
end ValidationScope

object ValidationScope:
  def validate[E](block: ValidationScope[E] ?=> Unit): Validated[NonEmptyChain[E], Unit] =
    val scope = new ValidationScope[E]
    try
      block(using scope)
      scope.errors match
        case Some(errs) => Invalid(errs)
        case None       => Valid(())
    catch
      case t: ValidationException if scope.isOurException(t) =>
        scope.errors match
          case Some(errs) => Invalid(errs)
          case None       => throw AssertionError("assertion failed: caught ValidationException, but no errors")

  def validateWithResult[E, A](block: ValidationScope[E] ?=> A): Validated[NonEmptyChain[E], A] =
    val scope = new ValidationScope[E]
    try
      val result = block(using scope)
      scope.errors match
        case Some(errs) => Invalid(errs)
        case None       => Valid(result)
    catch
      case t: ValidationException if scope.isOurException(t) =>
        scope.errors match
          case Some(errs) => Invalid(errs)
          case None       => throw AssertionError("assertion failed: caught ValidationException, but no errors")
