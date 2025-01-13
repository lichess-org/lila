package lila.plan

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints

import java.util.Currency

case class PlanCheckout(
    email: Option[String],
    money: Money,
    freq: Freq,
    giftTo: Option[UserStr]
):
  def fixFreq = copy(
    freq = if giftTo.isDefined then Freq.Onetime else freq
  )

private object PlanCheckout:

  def amountField(pricing: PlanPricing) = bigDecimal(10, 3)
    .verifying(Constraints.max(pricing.max.amount))
    .verifying(Constraints.min(pricing.min.amount))

final class PlanCheckoutForm(lightUserApi: lila.core.user.LightUserApi):

  private def make(
      currency: Currency
  )(email: Option[String], amount: BigDecimal, freq: String, giftTo: Option[UserStr]) =
    PlanCheckout(
      email,
      Money(amount, currency),
      if freq == "monthly" then Freq.Monthly else Freq.Onetime,
      giftTo = giftTo
    )

  def form(pricing: PlanPricing) = Form[PlanCheckout](
    mapping(
      "email"  -> optional(email),
      "amount" -> PlanCheckout.amountField(pricing),
      "freq"   -> nonEmptyText,
      "gift" -> optional(lila.common.Form.username.historicalField)
        .verifying("Unknown receiver", n => n.forall { blockingFetchUser(_).isDefined })
        .verifying(
          "Receiver is already a Patron",
          n => n.forall { blockingFetchUser(_).fold(true)(!_.isPatron) }
        )
    )(make(pricing.currency))(_ => none)
  )

  private def blockingFetchUser(user: UserStr) =
    lightUserApi.async(user.id).await(1.second, "giftUser")

case class Switch(money: Money)

object Switch:

  def form(pricing: PlanPricing) = Form(
    mapping(
      "amount" -> PlanCheckout.amountField(pricing)
    )(a => Switch(Money(a, pricing.currency)))(_ => none)
  )
