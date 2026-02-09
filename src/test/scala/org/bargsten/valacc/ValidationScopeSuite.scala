package org.bargsten.valacc

import cats.data.NonEmptyList
import org.bargsten.valacc.syntax.*

class ValidationScopeSuite extends munit.FunSuite:

  // --- ensure ---

  test("ensure with true adds no error"):
    val result = validate[String] { ensure(true) { "nope" } }
    assertEquals(result, Validated.unit)

  test("ensure with false adds error"):
    val result = validate[String] { ensure(false) { "bad" } }
    assertEquals(result, invalidOne("bad"))

  test("ensure with false adds no duplicate errors (demand)"):
    val result = validate[String] {
      val v = invalidOne("bad")
      attach(v)
      demandValue(v)
    }
    assertEquals(result, invalidOne("bad"))

  test("ensure with false adds no duplicate errors (get)"):
    val result = validate[String] {
      val v = invalidOne("bad")
      attach(v)
      v.get
    }
    assertEquals(result, invalidOne("bad"))

  test("multiple ensures accumulate"):
    val result = validate[String]:
      ensure(false) { "e1" }
      ensure(true) { "skip" }
      ensure(false) { "e2" }
    assertEquals(result, Invalid(NonEmptyList.of("e1", "e2")))

  test("ensureFail always adds error"):
    val result = validate[String] { ensureFail { "always" } }
    assertEquals(result, invalidOne("always"))

  // --- demand ---

  test("demand with true continues"):
    val result = validated[String, String]:
      demand(true) { "nope" }
      "ok"
    assertEquals(result, valid("ok"))

  test("demand with false short-circuits"):
    var reached = false
    val result = validated[String, String]:
      demand(false) { "stop" }
      reached = true
      "unreachable"
    assertEquals(result, invalidOne("stop"))
    assert(!reached)

  test("demandFail always short-circuits"):
    var reached = false
    val result = validated[String, String]:
      demandFail { "boom" }
      reached = true
      "unreachable"
    assertEquals(result, invalidOne("boom"))
    assert(!reached)

  test("ensure then demand accumulates both"):
    val result = validate[String]:
      ensure(false) { "e-ensure" }
      demand(false) { "e-demand" }
    assertEquals(result, Invalid(NonEmptyList.of("e-ensure", "e-demand")))

  // --- ensureDefined / demandDefined ---

  test("ensureDefined with Some returns Some"):
    val result = validated[String, Option[Int]]:
      ensureDefined(Some(42)) { "missing" }
    assertEquals(result, valid(Some(42)))

  test("ensureDefined with None adds error and returns None"):
    val result = validated[String, Option[Int]]:
      ensureDefined(None: Option[Int]) { "missing" }
    assertEquals(result, invalidOne("missing"))

  test("demandDefined with Some returns value"):
    val result = validated[String, Int]:
      demandDefined(Some(42)) { "missing" }
    assertEquals(result, valid(42))

  test("demandDefined with None short-circuits"):
    var reached = false
    val result = validated[String, Int]:
      val v = demandDefined(None: Option[Int]) { "missing" }
      reached = true
      v
    assertEquals(result, invalidOne("missing"))
    assert(!reached)

  // --- ensureValue / demandValue / demandValid ---

  test("ensureValue with valid returns Some"):
    val result = validated[String, Option[Int]]:
      ensureValue(valid(42))
    assertEquals(result, valid(Some(42)))

  test("ensureValue with invalid adds errors, returns None"):
    val result = validated[String, Option[Int]]:
      ensureValue(Invalid(NonEmptyList.of("e1", "e2")))
    assertEquals(result, Invalid(NonEmptyList.of("e1", "e2")))

  test("demandValue with valid returns value"):
    val result = validated[String, Int]:
      demandValue(valid(42))
    assertEquals(result, valid(42))

  test("demandValue with invalid short-circuits"):
    var reached = false
    val result = validated[String, Int]:
      val v = demandValue[String, Int](invalidOne("bad"))
      reached = true
      v
    assertEquals(result, invalidOne("bad"))
    assert(!reached)

  test("demandValid with valid continues"):
    val result = validate[String]:
      demandValid(valid(42))
    assertEquals(result, Validated.unit)

  test("get without attach"):
    val result = validated[String, String]:
      val inv = invalidOne("error")
      inv.get
    assertEquals(result, invalidOne("error"))

  test("demandValid with invalid short-circuits"):
    var reached = false
    val result = validate[String]:
      demandValid(invalidOne("bad"))
      reached = true
    assertEquals(result, invalidOne("bad"))
    assert(!reached)

  // --- attach ---

  test("attach accumulates errors from invalid"):
    val result = validate[String]:
      attach(invalidOne("existing"))
    assertEquals(result, invalidOne("existing"))

  test("attach passes through valid"):
    val result = validated[String, Int]:
      val v = attach(valid(42))
      v match
        case Valid(a) => a
        case _        => 0
    assertEquals(result, valid(42))

  // --- extension methods on scope ---

  test("Validated.get extracts value"):
    val result = validated[String, Int]:
      valid(42).get
    assertEquals(result, valid(42))

  test("Validated.get short-circuits on invalid (attach then get pattern)"):
    var reached = false
    val result = validated[String, Int]:
      val v = attach(invalidOne[String]("bad"))
      v.get
      reached = true
      0
    assertEquals(result, invalidOne("bad"))
    assert(!reached)

  test("fluent .attach()"):
    val result = validate[String]:
      invalidOne[String]("err").attach()
      ()
    assertEquals(result, invalidOne("err"))

  test("value.ensure with passing predicate"):
    val result = validated[String, Pokemon]:
      Pokemon(25, "Pikachu", 50).ensure(_.level > 10) { "too low" }
    assertEquals(result, valid(Pokemon(25, "Pikachu", 50)))

  test("value.ensure with failing predicate"):
    val result = validated[String, Pokemon]:
      Pokemon(25, "Pikachu", 5).ensure(_.level > 10) { "too low" }
    assertEquals(result, invalidOne("too low"))

  test("value.demand with passing predicate"):
    val result = validated[String, Pokemon]:
      Pokemon(25, "Pikachu", 50).demand(_.level >= 50) { "not ready" }
    assertEquals(result, valid(Pokemon(25, "Pikachu", 50)))

  test("value.demand with failing predicate short-circuits"):
    var reached = false
    val result = validated[String, Pokemon]:
      Pokemon(25, "Pikachu", 10).demand(_.level >= 50) { "not ready" }
      reached = true
      Pokemon(25, "Pikachu", 10)
    assertEquals(result, invalidOne("not ready"))
    assert(!reached)

  // --- validate / validated builders ---

  test("validate with no errors returns unit"):
    val result = validate[String] { ensure(true) { "x" } }
    assertEquals(result, Validated.unit)

  test("validated returns wrapped value on success"):
    val result = validated[String, Int]:
      ensure(true) { "x" }
      42
    assertEquals(result, valid(42))

  test("validated returns errors on failure"):
    val result = validated[String, Int]:
      ensure(false) { "e1" }
      ensure(false) { "e2" }
      42
    assertEquals(result, Invalid(NonEmptyList.of("e1", "e2")))

  test("validated short-circuits on demand failure with accumulated errors"):
    val result = validated[String, Int]:
      ensure(false) { "first" }
      demand(false) { "stops here" }
      ensure(false) { "never reached" }
      42
    assertEquals(result, Invalid(NonEmptyList.of("first", "stops here")))

  test("full workflow: ensure + attach + get"):
    val result = validated[String, Int]:
      ensure(false) { "e1" }
      val other = attach(Invalid(NonEmptyList.of("e2", "e3")))
      other.get
    assertEquals(result, Invalid(NonEmptyList.of("e1", "e2", "e3")))
end ValidationScopeSuite
