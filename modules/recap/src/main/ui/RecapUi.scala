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

  def home(av: Availability, user: User, checkout: RecapUi.Checkout)(using Context) =
    val data = av match
      case Availability.Available(data) => data
      case Availability.Queued(data) => data
    Page(title(user))
      .css("recap")
      .js(esmInit("recap", data ++ Json.obj("stripePublicKey" -> checkout.stripePublicKey)))
      .i18n(_.recap, _.variant, _.arena, _.swiss, _.patron, _.preferences)
      .append(checkoutScripts(checkout))
      .csp(paymentCsp):
        main(cls := "recap") // filled by ui/recap

  // the donate slide charges in USD, with the same Stripe and PayPal flows as /patron
  private def checkoutScripts(checkout: RecapUi.Checkout) =
    val namespace = attr("data-namespace")
    val payPalSdk = s"https://www.paypal.com/sdk/js?client-id=${checkout.payPalClientId}&currency=USD" +
      checkout.payPalLocale.so(l => s"&locale=$l")
    frag(
      script(src := "https://js.stripe.com/v3/"),
      // one PayPal SDK for one-time orders, another for subscriptions, as PayPal can't do both at once
      script(src := payPalSdk, namespace := "paypalOrder"),
      script(src := s"$payPalSdk&vault=true&intent=subscription", namespace := "paypalSubscription")
    )

  private def paymentCsp: Update[ContentSecurityPolicy] = csp =>
    csp.copy(
      connectSrc = "https://*.stripe.com" :: "https://*.paypal.com" :: csp.connectSrc,
      scriptSrc = "https://*.stripe.com" :: "https://*.paypal.com" :: csp.scriptSrc,
      frameSrc = "https://*.stripe.com" :: "https://*.paypal.com" :: csp.frameSrc
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
  case class Checkout(stripePublicKey: String, payPalClientId: String, payPalLocale: Option[String])
