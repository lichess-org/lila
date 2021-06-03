package lila.plan

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
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

case class Money(amount: BigDecimal, locale: Locale) {
  val currency = Currency getInstance locale
  def display  = NumberFormat.getCurrencyInstance(locale).format(amount)
}

case class Country(code: String) extends AnyVal

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripeProducts(monthly: String, onetime: String)

case class StripeItem(id: String, price: StripePrice)

case class StripePrice(product: String, unit_amount: Cents) {
  def cents = unit_amount
  def usd   = cents.usd
}

case class NextUrls(cancel: String, success: String)

case class StripeSession(id: SessionId)
case class CreateStripeSession(customerId: CustomerId, checkout: Checkout, urls: NextUrls)

case class StripeSubscription(
    id: String,
    item: StripeItem,
    customer: CustomerId,
    cancel_at_period_end: Boolean,
    status: String,
    default_payment_method: Option[String]
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

case class StripePaymentMethod(card: Option[StripeCard])

case class StripeCard(brand: String, last4: String, exp_year: Int, exp_month: Int)

case class StripeCompletedSession(customer: CustomerId, mode: String) {
  def freq = if (mode == "subscription") Freq.Monthly else Freq.Onetime
}

case class StripeSetupIntent(payment_method: String)

case class StripeSessionWithIntent(setup_intent: StripeSetupIntent)
