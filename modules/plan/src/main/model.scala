package lila.plan

import org.joda.time.DateTime

case class PayPalIpnKey(value: String) extends AnyVal

case class CustomerId(value: String) extends AnyVal
case class ChargeId(value: String) extends AnyVal

case class Source(value: String) extends AnyVal
case class Usd(value: BigDecimal) extends AnyVal with Ordered[Usd] {
  def compare(other: Usd) = value compare other.value
  def cents = Cents((value * 100).toInt)
  def toFloat = value.toFloat
  override def toString = s"$$$value"
}
object Usd {
  def apply(value: Double): Usd = Usd(BigDecimal(value))
  def apply(value: Int): Usd = Usd(BigDecimal(value))
}
case class Cents(value: Int) extends AnyVal with Ordered[Cents] {
  def compare(other: Cents) = value compare other.value
  def usd = Usd(value / 100d)
  override def toString = usd.toString
}

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripePlan(id: String, name: String, amount: Cents) {
  def cents = amount
  def usd = cents.usd
}
object StripePlan {
  def make(cents: Cents): StripePlan = StripePlan(
    id = s"monthly_${cents.value}",
    name = s"Patron ${cents.usd}",
    amount = cents)

  val defaults = List(5, 10, 20, 50).map(Usd.apply).map(_.cents).map(StripePlan.make)
}

case class StripeSubscription(
    id: String,
    plan: StripePlan,
    customer: CustomerId,
    cancel_at_period_end: Boolean) {

  def renew = !cancel_at_period_end
}

case class StripeCustomer(
    id: CustomerId,
    email: Option[String],
    subscriptions: StripeSubscriptions) {

  def firstSubscription = subscriptions.data.headOption

  def plan = firstSubscription.map(_.plan)
}

case class StripeCharge(id: ChargeId, amount: Cents, customer: CustomerId)

case class StripeInvoice(
    id: Option[String],
    amount_due: Int,
    date: Long,
    paid: Boolean) {
  def cents = Cents(amount_due)
  def usd = cents.usd
  def dateTime = new DateTime(date * 1000)
}
