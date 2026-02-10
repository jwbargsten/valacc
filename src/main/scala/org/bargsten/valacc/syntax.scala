package org.bargsten.valacc

import cats.data.{NonEmptyList, Validated}
import org.bargsten.valacc.{ValAcc, ValidationScope}

object syntax:
  def ensure[E](predicate: Boolean)(error: => E)(using s: ValidationScope[E]): Unit =
    s.ensure(predicate)(error)

  def ensureFail[E](error: => E)(using s: ValidationScope[E]): Unit =
    s.ensureFail(error)

  def ensureDefined[E, A](option: Option[A])(error: => E)(using s: ValidationScope[E]): Option[A] =
    s.ensureDefined(option)(error)

  def ensureValue[E, A](validated: Validated[NonEmptyList[E], A])(using s: ValidationScope[E]): Option[A] =
    s.ensureValue(validated)

  def demand[E](predicate: Boolean)(error: => E)(using s: ValidationScope[E]): Unit =
    s.demand(predicate)(error)

  def demandFail[E](error: => E)(using s: ValidationScope[E]): Nothing =
    s.demandFail(error)

  def demandDefined[E, A](option: Option[A])(error: => E)(using s: ValidationScope[E]): A =
    s.demandDefined(option)(error)

  def demandValue[E, A](validated: Validated[NonEmptyList[E], A])(using s: ValidationScope[E]): A =
    s.demandValue(validated)

  def demandValid[E](validated: Validated[NonEmptyList[E], ?])(using s: ValidationScope[E]): Unit =
    s.demandValid(validated)

  def attach[E, A](validated: Validated[NonEmptyList[E], A])(using s: ValidationScope[E]): Validated[NonEmptyList[E], A] =
    s.attach(validated)

  export ValAcc.*
  export ValidationScope.*
