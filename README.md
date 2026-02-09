# valacc

Scala 3 accumulative validation lib

## Installation

<!-- include example/project.scala::dependencies -->
```scala
// Scala CLI
//> using dep org.bargsten::valacc:0.1.0
// sbt
// "org.bargsten" %% "valacc" % "0.1.0"
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

  def parse(v: String): Validated[String, PostCode] = validated[String, PostCode]:
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

## Build

```bash
sbt compile   # Compile
sbt test      # Run tests
sbt console   # REPL
```

## Release Strategy

We use [Early SemVer](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy)
