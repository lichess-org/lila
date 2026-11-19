package lila.recap
package ui

import play.api.libs.json.*

import lila.recap.Recap.Availability
import lila.ui.*
import lila.ui.ScalatagsTemplate.*

final class RecapUi(helpers: Helpers):
  import helpers.*

  private def title(user: User)(using ctx: Context) =
    if ctx.is(user) then "Your yearly recap"
    else s"${user.username} yearly recap"

  def home(av: Availability, user: User, checkout: RecapUi.Checkout)(using ctx: Context) =
    val data = av match
      case Availability.Available(data) => data
      case Availability.Queued(data) => data
    Page(title(user))
      .css("recap")
      .js(
        esmInit(
          "recap",
          data ++ Json.obj(
            "checkout" -> checkout.json,
            "imperial" -> lila.common.HTTPRequest.country(using ctx.req).has(lila.core.net.CountryCode("US"))
          )
        )
      )
      .i18n(_.recap, _.variant, _.arena, _.swiss, _.patron, _.preferences, _.`class`)
      .append(checkoutScripts(checkout))
      .csp(_.withPayments):
        main(cls := "recap") // filled by ui/recap

  private def checkoutScripts(checkout: RecapUi.Checkout) = frag(
    checkout.stripe.option(stripeScript),
    checkout.payPal.option(payPalScripts(checkout.payPalClientId, checkout.currency, checkout.payPalLocale))
  )

  def notAvailable(year: Int) =
    Page("Recap not available yet"):
      main(cls := "page-small box box-pad page")(
        h1(cls := "box__top")(s"Your $year Lichess Recap"),
        div(
          p("Your recap will be available at the end of the year. Check back then!")
        )
      )

object RecapUi:
  case class Checkout(
      stripePublicKey: String,
      payPalClientId: String,
      payPalLocale: Option[String],
      currency: String, // ISO code, e.g. EUR
      stripe: Boolean, // whether Stripe and PayPal accept this currency
      payPal: Boolean,
      price: BigDecimal => BigDecimal // minutes of uptime -> what they cost, in currency
  ):
    import Checkout.*
    private def tier(minutes: BigDecimal) = Json.obj("minutes" -> minutes, "amount" -> price(minutes))
    def json = Json.obj(
      "stripePublicKey" -> stripePublicKey,
      "currency" -> currency,
      "stripe" -> stripe,
      "payPal" -> payPal,
      "example" -> tier(exampleMinutes),
      "tiers" -> JsObject(tiers.view.mapValues(ms => JsArray(ms.map(tier))).toSeq)
    )

  object Checkout:
    val uptimeUsdPerMinute = BigDecimal(1.5)
    val exampleMinutes = BigDecimal(5)
    val tiers: Map[String, List[BigDecimal]] = Map(
      "monthly" -> List(2.5, 5, 10),
      "onetime" -> List(30, 60, 120)
    )
    val allMinutes: List[BigDecimal] = (exampleMinutes :: tiers.values.flatten.toList).distinct
