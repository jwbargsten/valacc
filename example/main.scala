// :snx example
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
  // :xns
