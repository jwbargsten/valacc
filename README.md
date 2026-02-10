# valacc

Scala 3 accumulative validation lib

## Installation

<!-- include example/project.scala::dependencies -->
```scala
// Scala CLI
//> using dep org.bargsten::valacc:0.2.0
// sbt
// "org.bargsten" %% "valacc" % "0.2.0"
```
<!-- endinclude -->

## Usage

<!-- include example/main.scala::example -->
```scala
import org.bargsten.valacc.*
import org.bargsten.valacc.syntax.*

opaque type PostCode = String

object PostCode:
  private val Pattern = """^(\d{4})\s*([A-Za-z]{2})$""".r

  def parse(v: String): Validated[String, PostCode] = validateWithResult[String, PostCode]:
    val trimmed = v.trim
    demand(trimmed.nonEmpty)("zip code must not be empty")
    val m = demandDefined(Pattern.findFirstMatchIn(trimmed))("zip code must match format '1234AB'")
    val digits = m.group(1).toInt
    ensure(digits >= 1000)(s"digits part must be >= 1000, got $digits")
    val letters = m.group(2).toUpperCase
    ensure(letters != "SA" && letters != "SD" && letters != "SS")(
      s"letter combination '$letters' is not allowed"
    )
    s"$digits $letters"

  def fromUnsafe(v: String): PostCode = v

  extension (zc: PostCode) def unwrap: String = zc

@main
def main() =
  // check some Dutch postcodes
  val examples = List("1234AB", "  2511 dp ", "0999ZZ", "1234SA", "0500SA", "", "bogus", "1234 HX")
  examples.foreach: input =>
    println(s"input: '$input'")
    PostCode.parse(input) match
      case Valid(res)    => println(s"  postcode is $res")
      case Invalid(errs) => errs.toList.foreach(err => println(s"  ERROR: $err"))
```
<!-- endinclude -->

**NOTE**: `Validated` is an alias for the `Validated` from Cats: `type Validated[+E, +A] = cats.data.Validated[NonEmptyList[E], A]`

## The Validation Scope

Validation scopes are code blocks wrapped in:

```
validate:
  ...
```

or

```
validateWithResult:
  ...
```

`validate[E]` just does validation, returns `Valid[Unit]` on success.
`validateWithResult[E, A]` does validation, but also returns a value of type `A`.

Two modes are possible: an accumulative flow using `ensure`-type of functions and a
short-circuiting mode using `demand`-type of functions.

### Accumulating (`ensure`-type)

Adds errors but continues execution.

| Function                  | Returns     | Summary                                         |
| ------------------------- | ----------- | ----------------------------------------------- |
| `ensure(pred)(err)`       | `Unit`      | Adds error if predicate is false                |
| `ensureFail(err)`         | `Unit`      | Always adds error                               |
| `ensureDefined(opt)(err)` | `Option[A]` | Adds error if `None`                            |
| `value.ensure(pred)(err)` | `value`     | Adds error if predicate is false, returns value |

### Short-circuiting (`demand`-type)

Adds error and aborts the validation scope.

| Function                  | Returns   | Summary                                     |
| ------------------------- | --------- | ------------------------------------------- |
| `demand(pred)(err)`       | `Unit`    | Aborts if predicate is false                |
| `demandFail(err)`         | `Nothing` | Always aborts                               |
| `demandDefined(opt)(err)` | `value`   | Aborts if `None`, unwraps value             |
| `value.demand(pred)(err)` | `value`   | Aborts if predicate is false, returns value |

## Nested validations

Nested validations are handled with these functions.

| Function                 | Returns           | Summary                                                              |
| ------------------------ | ----------------- | -------------------------------------------------------------------- |
| `ensureValue(validated)` | `Option[A]`       | Adds errors from `Invalid`                                           |
| `demandValue(validated)` | `value`           | Aborts if `Invalid`, unwraps value                                   |
| `demandValid(validated)` | `Unit`            | Aborts if `Invalid`                                                  |
| `attach(validated)`      | `Validated[E, A]` | Adds errors from `Invalid`, returns validated as-is. Use with `.get` |
| `validated.get`          | `value`           | Aborts if `Invalid`, unwraps value                                   |

A `ValidationScope` maintains a list of errors. If you want to add the errors of a
separate `Validated` object, you need to "attach" it to the scope. The short-circuit
approach with `demandValue` is clear: add the errors and exit the scope, but for error
accumulation it is different: attach all validations and, if needed, `.get` the values.
This means that in contrast to `demandValue`, `.get` does not add errors to the
validation scope (unless you forgot to call `attach`, see Remarks)

### Example

We start easy: a country code validation. No scope, yet.

<!-- include example/FullExample.scala::countrycode -->
```scala
opaque type CountryCode = String
object CountryCode:
  val Codes: Set[String] = Locale.getISOCountries.toSet
  val NL: CountryCode = CountryCode.fromUnsafe("NL")

  def parse(v: String): Validated[String, CountryCode] =
    val sanitized = v.trim.toUpperCase
    if Codes.contains(sanitized) then valid(sanitized)
    else invalidOne(s"Country code $v is invalid")

  def fromUnsafe(v: String): CountryCode = v

  extension (cc: CountryCode) def unwrap: String = cc
```
<!-- endinclude -->

The postcode is more sophisticated and accumulative, so here it makes sense to use a
scope:

<!-- include example/main.scala::postcode -->
```scala
opaque type PostCode = String

object PostCode:
  private val Pattern = """^(\d{4})\s*([A-Za-z]{2})$""".r

  def parse(v: String): Validated[String, PostCode] = validateWithResult[String, PostCode]:
    val trimmed = v.trim
    demand(trimmed.nonEmpty)("zip code must not be empty")
    val m = demandDefined(Pattern.findFirstMatchIn(trimmed))("zip code must match format '1234AB'")
    val digits = m.group(1).toInt
    ensure(digits >= 1000)(s"digits part must be >= 1000, got $digits")
    val letters = m.group(2).toUpperCase
    ensure(letters != "SA" && letters != "SD" && letters != "SS")(
      s"letter combination '$letters' is not allowed"
    )
    s"$digits $letters"

  def fromUnsafe(v: String): PostCode = v

  extension (zc: PostCode) def unwrap: String = zc
```
<!-- endinclude -->

To build the address we merge the country code and postcode validations into the address
scope:

<!-- include example/FullExample.scala::address -->
```scala
case class AddressRequest(
    postCode: String,
    countryCode: String
)
case class Address(
    postCode: PostCode,
    countryCode: CountryCode
)

def validateAddress(req: AddressRequest): Validated[String, Address] = validateWithResult:
  val countryCode = CountryCode.parse(req.countryCode).attach()
  val postCode = attach(PostCode.parse(req.postCode))

  // you can mix and match with existing ensure and demand concepts
  countryCode.foreach(cc => ensure(cc == CountryCode.NL)("wrong country"))

  Address(
    postCode = postCode.get,
    countryCode = countryCode.get,
  )
```
<!-- endinclude -->

To resolve the address in your endpoint, you can use `.fold` or pattern matching

<!-- include example/FullExample.scala::request -->
```scala
trait Repository:
  def store(address: Address): Unit

case class Response(status: Int, msg: String)

class Routes(repo: Repository):
  def postAddressPatternMatching(req: AddressRequest): Response =
    validateAddress(req) match
      case Invalid(errors) => Response(400, errors.toList.mkString("\n"))
      case Valid(address) =>
        repo.store(address)
        Response(201, "Address stored")

  def postAddressFold(req: AddressRequest): Response =
    validateAddress(req).fold(
      errors => Response(400, errors.toList.mkString("\n")),
      address =>
        repo.store(address)
        Response(201, "Address stored")
    )
```
<!-- endinclude -->

## Remarks

- The validation scope is not thread-safe.
- Calling `.get` in a validation scope will add the errors of the validation object if
  not added previously with `attach`.
- `Validated` is an alias for the `Validated` from Cats: `type Validated[+E, +A] = cats.data.Validated[NonEmptyList[E], A]`

## Build

```bash
sbt compile   # Compile
sbt test      # Run tests
sbt console   # REPL
```

## Release Strategy

We use
[Early SemVer](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy)
