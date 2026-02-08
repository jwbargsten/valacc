package org.bargsten.valacc

import cats.data.NonEmptyList
import org.slf4j.LoggerFactory

import scala.annotation.targetName
import scala.collection.mutable.ListBuffer
import scala.util.control.NoStackTrace

private[valacc] class ValidationException extends Exception, NoStackTrace

class ValidationScope[E]:
  private val logger = LoggerFactory.getLogger(getClass)
  private val _errors = ListBuffer.empty[E]
  private val abort = ValidationException()

  def errors: Option[NonEmptyList[E]] = NonEmptyList.fromList(_errors.toList)

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

  def ensureValue[A](validated: Validated[E, A]): Option[A] =
    validated match
      case Valid(a)        => Some(a)
      case Invalid(errors) => _errors ++= errors.toList; None

  def attach[A](validated: Validated[E, A]): Validated[E, A] =
    validated match
      case inv @ Invalid(errors) => _errors ++= errors.toList; inv
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

  def demandValue[A](validated: Validated[E, A]): A =
    validated match
      case Valid(a) => a
      case Invalid(errors) =>
        _errors ++= errors.toList
        shortcircuit()

  def demandValid(validated: Validated[E, ?]): Unit =
    validated match
      case Valid(_) => ()
      case Invalid(errors) =>
        _errors ++= errors.toList
        shortcircuit()

  // --- extensions available via given scope ---

  extension [A](validated: Validated[E, A])
    def get: A = validated match
      case Valid(a) => a
      case Invalid(errors) =>
        if (_errors.isEmpty)
          _errors ++= errors.toList
          logger.error("get called on an Invalid instance. Did you forget to attach the error in the current scope?")
        shortcircuit()

    def attachV(): Validated[E, A] = attach(validated)

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
  def validate[E](block: ValidationScope[E] ?=> Unit): Validated[E, Unit] =
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
          case None       => Valid(())

  def validated[E, A](block: ValidationScope[E] ?=> A): Validated[E, A] =
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
