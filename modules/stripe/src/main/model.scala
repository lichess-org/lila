package lila.stripe

import org.joda.time.DateTime

case class CustomerId(value: String) extends AnyVal

case class Source(value: String) extends AnyVal
case class Usd(value: Int) extends AnyVal with Ordered[Usd] {
  def compare(other: Usd) = value compare other.value
  def cents = Cents(value * 100)
  override def toString = s"$$$value"
}
case class Cents(value: Int) extends AnyVal with Ordered[Cents] {
  def compare(other: Cents) = value compare other.value
  def usd = Usd(value / 100)
  override def toString = usd.toString
}

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripePlan(id: String, amount: Int) {
  def cents = Cents(amount)
  def usd = cents.usd
}

case class StripeSubscription(id: String, plan: StripePlan, customer: CustomerId)

case class StripeCustomer(id: CustomerId, subscriptions: StripeSubscriptions) {

  def firstSubscription = subscriptions.data.headOption

  def plan = firstSubscription.map(_.plan)
}

case class StripeCharge(amount: Int, customer: CustomerId) {
  def cents = Cents(amount)
}

case class StripeInvoice(
    id: Option[String],
    amount_due: Int,
    date: Long,
    paid: Boolean) {
  def cents = Cents(amount_due)
  def usd = cents.usd
  def dateTime = new DateTime(date * 1000)
}
