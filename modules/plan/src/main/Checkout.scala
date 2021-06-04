package lila.plan

import java.util.Currency
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import scala.concurrent.duration._

import lila.user.User

case class Checkout(
    email: Option[String],
    money: Money,
    freq: Freq,
    giftTo: Option[String]
) {
  def oneTimeGift = giftTo ifFalse freq.renew
}

private object Checkout {

  def amountField(pricing: PlanPricing) = bigDecimal(10, 3)
    .verifying(Constraints.max(pricing.max.amount))
    .verifying(Constraints.min(pricing.min.amount))
}

final class CheckoutForm(lightUserApi: lila.user.LightUserApi) {

  private def make(
      currency: Currency
  )(email: Option[String], amount: BigDecimal, freq: String, giftTo: Option[String]) =
    Checkout(
      email,
      Money(amount, currency),
      if (freq == "monthly") Freq.Monthly else Freq.Onetime,
      giftTo = giftTo
    )

  def form(pricing: PlanPricing) = Form[Checkout](
    mapping(
      "email"  -> optional(email),
      "amount" -> Checkout.amountField(pricing),
      "freq"   -> nonEmptyText,
      "gift" -> optional(lila.user.UserForm.historicalUsernameField)
        .verifying("Unknown receiver", n => n.fold(true) { blockingFetchUser(_).isDefined })
        .verifying(
          "Receiver is already a Patron",
          n => n.fold(true) { blockingFetchUser(_).fold(true)(!_.isPatron) }
        )
    )(make(pricing.currency) _)(_ => none)
  )

  private def blockingFetchUser(username: String) =
    lightUserApi.async(User normalize username).await(1 second, "giftUser")
}

case class Switch(money: Money)

object Switch {

  def form(pricing: PlanPricing) = Form(
    mapping(
      "amount" -> Checkout.amountField(pricing)
    )(a => Switch(Money(a, pricing.currency)))(_ => none)
  )
}
