import org.bargsten.valacc.*
import org.bargsten.valacc.syntax.*

import java.util.Locale

// :snx countrycode
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
// :xns

// :snx address
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
// :xns

// :snx request
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
// :xns
