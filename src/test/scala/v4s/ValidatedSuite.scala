package v4s

import cats.data.NonEmptyList
import v4s.Validated.*

case class Pokemon(id: Int, name: String, level: Int)

class ValidatedSuite extends munit.FunSuite:

  val validPokemon: Validated[String, Pokemon] = valid(Pokemon(25, "Pikachu", 50))
  val invalidPokemon: Validated[String, Pokemon] = invalidOne("Pokemon fainted")

  // --- Valid / Invalid basics ---

  test("getOrElse returns value when valid, default when invalid"):
    assertEquals(validPokemon.getOrElse(Pokemon(0, "MissingNo", 0)), Pokemon(25, "Pikachu", 50))
    assertEquals(invalidPokemon.getOrElse(Pokemon(132, "Ditto", 10)), Pokemon(132, "Ditto", 10))

  test("isValid / isInvalid"):
    assert(validPokemon.isValid)
    assert(!validPokemon.isInvalid)
    assert(!invalidPokemon.isValid)
    assert(invalidPokemon.isInvalid)

  test("contains"):
    assert(validPokemon.contains(Pokemon(25, "Pikachu", 50)))
    assert(!validPokemon.contains(Pokemon(0, "MissingNo", 0)))
    assert(!invalidPokemon.contains(Pokemon(25, "Pikachu", 50)))

  test("exists"):
    assert(validPokemon.exists(_.level == 50))
    assert(!validPokemon.exists(_.level == 1))
    assert(!invalidPokemon.exists(_ => true))

  test("forall"):
    assert(validPokemon.forall(_.level == 50))
    assert(!validPokemon.forall(_.level == 1))
    assert(invalidPokemon.forall(_ => false))

  test("foreach"):
    var captured: Option[Pokemon] = None
    validPokemon.foreach(p => captured = Some(p))
    assertEquals(captured, Some(Pokemon(25, "Pikachu", 50)))

    var called = false
    invalidPokemon.foreach(_ => called = true)
    assert(!called)

  test("tap executes on valid only"):
    var captured: Option[Pokemon] = None
    validPokemon.tap(p => captured = Some(p))
    assertEquals(captured, Some(Pokemon(25, "Pikachu", 50)))

    var called = false
    invalidPokemon.tap(_ => called = true)
    assert(!called)

  test("tapInvalid executes on invalid only"):
    var captured: Option[NonEmptyList[String]] = None
    invalidPokemon.tapInvalid(es => captured = Some(es))
    assertEquals(captured, Some(NonEmptyList.one("Pokemon fainted")))

    var called = false
    validPokemon.tapInvalid(_ => called = true)
    assert(!called)

  test("orElse returns self when valid, default when invalid"):
    assertEquals(validPokemon.orElse(valid(Pokemon(151, "Mew", 50))), validPokemon)
    assertEquals(invalidPokemon.orElse(valid(Pokemon(151, "Mew", 50))), valid(Pokemon(151, "Mew", 50)))

  test("fold"):
    val res = valid(Pokemon(143, "Snorlax", 30)).fold(_ => "Error", _.name)
    assertEquals(res, "Snorlax")

    val res2 = Invalid(NonEmptyList.of("E1", "E2")).fold(_.toList.mkString(", "), _ => "ok")
    assertEquals(res2, "E1, E2")

  test("toOption"):
    assertEquals(validPokemon.toOption, Some(Pokemon(25, "Pikachu", 50)))
    assertEquals(invalidPokemon.toOption, None)

  test("toEither"):
    assertEquals(validPokemon.toEither, Right(Pokemon(25, "Pikachu", 50)))
    assertEquals(invalidPokemon.toEither, Left(NonEmptyList.one("Pokemon fainted")))

  test("swap valid becomes invalid"):
    assertEquals(valid(42).swap, Invalid(NonEmptyList.one(42)))

  test("swap invalid becomes valid"):
    val errors = NonEmptyList.of("e1", "e2")
    assertEquals(Invalid(errors).swap, Valid(errors))

  // --- map / flatMap / mapErrors / recover ---

  test("map transforms valid value"):
    assertEquals(valid(Pokemon(25, "Pikachu", 50)).map(_.name), valid("Pikachu"))

  test("map on invalid returns invalid"):
    assert(invalidPokemon.map(_.name).isInvalid)

  test("flatMap chains when valid"):
    val result = validPokemon.flatMap(p => valid(Pokemon(5, "Charmeleon", p.level)))
    assertEquals(result, valid(Pokemon(5, "Charmeleon", 50)))

  test("flatMap short-circuits when invalid"):
    val result = validPokemon.flatMap { p =>
      if p.level >= 116 then valid(Pokemon(5, "Charmeleon", p.level))
      else invalidOne("Level too low to evolve")
    }
    assertEquals(result, invalidOne("Level too low to evolve"))

  test("flatMap on invalid returns invalid"):
    assert(invalidPokemon.flatMap(p => valid(p.name)).isInvalid)

  test("filterOrElse keeps valid when predicate passes"):
    assertEquals(validPokemon.filterOrElse(_.level > 10, "too low"), validPokemon)

  test("filterOrElse rejects valid when predicate fails"):
    assertEquals(validPokemon.filterOrElse(_.level > 100, "too low"), invalidOne("too low"))

  test("filterOrElse on invalid returns invalid"):
    assertEquals(invalidPokemon.filterOrElse(_ => true, "irrelevant"), invalidPokemon)

  test("mapErrors transforms errors"):
    val inv: Validated[String, Int] = Invalid(NonEmptyList.of("low hp", "poisoned"))
    val res = inv.mapErrors(_.map(_.toUpperCase))
    assertEquals(res, Invalid(NonEmptyList.of("LOW HP", "POISONED")))

  test("mapErrors on valid returns valid"):
    val v = valid(1)
    assertEquals(v.mapErrors(_.map(_ => 0)), v)

  test("mapEachError"):
    val inv: Validated[String, Int] = Invalid(NonEmptyList.of("aa", "bbb"))
    assertEquals(inv.mapEachError(_.length), Invalid(NonEmptyList.of(2, 3)))

  test("recover on invalid"):
    val inv: Validated[String, Pokemon] = Invalid(NonEmptyList.of("E1", "E2"))
    val res = inv.recover(es => Pokemon(0, s"Recovered from ${es.size} errors", 1))
    assertEquals(res, valid(Pokemon(0, "Recovered from 2 errors", 1)))

  test("recover on valid returns valid"):
    assertEquals(validPokemon.recover(_ => Pokemon(0, "Ditto", 1)), validPokemon)

  // --- Companion ---

  test("unit returns valid unit"):
    assertEquals(Validated.unit, Valid(()))

  test("valid wraps value"):
    assertEquals(Validated.valid(42), Valid(42))

  test("invalidOne creates single-error invalid"):
    assertEquals(Validated.invalidOne("err"), Invalid(NonEmptyList.one("err")))

  test("fromOption"):
    assertEquals(Validated.fromOption(Some(1))("missing"), valid(1))
    assertEquals(Validated.fromOption(None: Option[Int])("missing"), invalidOne("missing"))

  test("fromEither"):
    assertEquals(Validated.fromEither(Right(1)), valid(1))
    assertEquals(Validated.fromEither(Left("err")), invalidOne("err"))

  test("fromTry"):
    assertEquals(Validated.fromTry(scala.util.Success(1)), valid(1))
    val ex = new RuntimeException("boom")
    assertEquals(Validated.fromTry(scala.util.Failure(ex)), invalidOne(ex))

  // --- zip ---

  test("zip two valids with function"):
    val p1 = valid(Pokemon(25, "Pikachu", 50))
    val p2 = valid(Pokemon(26, "Raichu", 60))
    assertEquals(p1.zip(p2)((a, b) => s"${a.name} evolves to ${b.name}"), valid("Pikachu evolves to Raichu"))

  test("zip accumulates errors from both invalids"):
    val i1: Validated[String, Int] = Invalid(NonEmptyList.of("e1", "e2"))
    val i2: Validated[String, Int] = Invalid(NonEmptyList.of("e3"))
    assertEquals(i1.zip(i2)(_ + _), Invalid(NonEmptyList.of("e1", "e2", "e3")))

  test("zip valid with invalid returns invalid"):
    val v = valid(1)
    val i: Validated[String, Int] = invalidOne("err")
    assertEquals(v.zip(i)(_ + _), invalidOne("err"))
    assertEquals(i.zip(v)(_ + _), invalidOne("err"))

  test("zip without function keeps left value"):
    assertEquals(valid(7).zip(valid(9)), valid(7))

  // --- sequence ---

  test("sequence all valids"):
    val list = List(valid(1), valid(2), valid(3))
    assertEquals(list.sequence, valid(List(1, 2, 3)))

  test("sequence with invalids accumulates errors"):
    val list: List[Validated[String, Int]] = List(valid(1), invalidOne("e1"), valid(3), invalidOne("e2"))
    assertEquals(list.sequence, Invalid(NonEmptyList.of("e1", "e2")))

  test("sequence empty list"):
    assertEquals(List.empty[Validated[String, Int]].sequence, valid(List.empty[Int]))

  // --- validateAll ---

  test("validateAll all valid returns unit"):
    assertEquals(Validated.validateAll(valid(1), valid(2), Validated.unit), Validated.unit)

  test("validateAll accumulates errors"):
    val res = Validated.validateAll(valid(1), invalidOne("e1"), Invalid(NonEmptyList.of("e2", "e3")))
    assertEquals(res, Invalid(NonEmptyList.of("e1", "e2", "e3")))
