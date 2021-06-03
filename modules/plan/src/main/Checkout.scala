package lila.plan

import java.util.Currency
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

case class Checkout(
    email: Option[String],
    money: Money,
    freq: Freq
) {}

object Checkout {

  def make(currency: Currency)(email: Option[String], amount: BigDecimal, freq: String) =
    Checkout(
      email,
      Money(amount, currency),
      if (freq == "monthly") Freq.Monthly else Freq.Onetime
    )

  def amountField(pricing: PlanPricing) = bigDecimal(10, 3)
    .verifying(Constraints.max(pricing.max.amount))
    .verifying(Constraints.min(pricing.min.amount))

  def form(pricing: PlanPricing) = Form[Checkout](
    mapping(
      "email"  -> optional(email),
      "amount" -> amountField(pricing),
      "freq"   -> nonEmptyText
    )(make(pricing.currency) _)(_ => none)
  )
}

case class Switch(money: Money)

object Switch {

  def form(pricing: PlanPricing) = Form(
    mapping(
      "amount" -> Checkout.amountField(pricing)
    )(a => Switch(Money(a, pricing.currency)))(_ => none)
  )
}
