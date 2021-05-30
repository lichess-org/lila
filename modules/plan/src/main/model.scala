package lila.plan

import org.joda.time.DateTime

case class ChargeId(value: String)       extends AnyVal
case class ClientId(value: String)       extends AnyVal
case class CustomerId(value: String)     extends AnyVal
case class SessionId(value: String)      extends AnyVal
case class SubscriptionId(value: String) extends AnyVal

case class Source(value: String) extends AnyVal

sealed abstract class Freq(val renew: Boolean)
object Freq {
  case object Monthly extends Freq(renew = true)
  case object Onetime extends Freq(renew = false)
}

case class Usd(value: BigDecimal) extends AnyVal with Ordered[Usd] {
  def compare(other: Usd) = value compare other.value
  def cents               = Cents((value * 100).toInt)
  override def toString   = s"$$$value"
}
object Usd {
  def apply(value: Int): Usd = Usd(BigDecimal(value))
}
case class Cents(value: Int) extends AnyVal with Ordered[Cents] {
  def compare(other: Cents) = Integer.compare(value, other.value)
  def usd                   = Usd(BigDecimal(value, 2))
  override def toString     = usd.toString
}

object Cents {
  val lifetime = Cents(25000)
}

case class Country(code: String) extends AnyVal

case class StripeSubscriptions(data: List[StripeSubscription])

object StripeProduct {
  object dev {
    val monthlyId = "prod_JZswNwe0eLPJIU"
    val onetimeId = "prod_JZuNrVAZSUieAd"
  }
  val monthlyId = dev.monthlyId
  val onetimeId = dev.onetimeId
}

case class StripePrice(product: String, unit_amount: Cents) {
  def cents = unit_amount
  def usd   = cents.usd
}
object StripePrice {
  def make(cents: Cents, freq: Freq): StripePrice =
    freq match {
      case Freq.Monthly =>
        StripePrice(
          product = StripeProduct.monthlyId,
          unit_amount = cents
        )
      case Freq.Onetime =>
        StripePrice(
          product = StripeProduct.onetimeId,
          unit_amount = cents
        )
    }

  val defaultAmounts = List(5, 10, 20, 50).map(Usd.apply).map(_.cents)
}

case class StripeSession(id: SessionId)
case class CreateStripeSession(
    success_url: String,
    cancel_url: String,
    customer_id: CustomerId,
    checkout: Checkout
)

case class StripeSubscription(
    id: String,
    price: StripePrice,
    customer: CustomerId,
    cancel_at_period_end: Boolean,
    status: String
) {
  def renew    = !cancel_at_period_end
  def isActive = status == "active"
}

case class StripeCustomer(
    id: CustomerId,
    email: Option[String],
    subscriptions: StripeSubscriptions
) {

  def firstSubscription = subscriptions.data.headOption
  def renew             = firstSubscription.??(_.renew)
}

case class StripeCharge(
    id: ChargeId,
    amount: Cents,
    customer: CustomerId,
    billing_details: Option[StripeCharge.BillingDetails]
) {
  def lifetimeWorthy = amount >= Cents.lifetime
  def country        = billing_details.flatMap(_.address).flatMap(_.country).map(Country)
}

object StripeCharge {
  case class Address(country: Option[String])
  case class BillingDetails(address: Option[Address])
}

case class StripeInvoice(
    id: Option[String],
    amount_due: Int,
    created: Long,
    paid: Boolean
) {
  def cents    = Cents(amount_due)
  def usd      = cents.usd
  def dateTime = new DateTime(created * 1000)
}

case class StripeCompletedSession(
    customer: CustomerId,
    mode: String,
    subscription: Option[SubscriptionId]
)
