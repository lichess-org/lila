package lila.stripe

case class Source(value: String) extends AnyVal
case class Usd(value: Int) extends AnyVal with Ordered[Usd] {
  def compare(other: Usd) = value compare other.value
  def cents = Cents(value * 100)
  override def toString = s"$value"
}
case class Cents(value: Int) extends AnyVal with Ordered[Cents] {
  def compare(other: Cents) = value compare other.value
  def usd = Usd(value / 100)
  override def toString = usd.toString
}

sealed abstract class LichessPlan(val id: String, val usd: Usd) {
  def cents = usd.cents
  def stripePlan = StripePlan(id, cents.value)
}

object LichessPlan {
  case object Monthly5 extends LichessPlan("monthly_5", Usd(5))
  case object Monthly10 extends LichessPlan("monthly_10", Usd(10))
  case object Monthly20 extends LichessPlan("monthly_20", Usd(20))
  case object Monthly50 extends LichessPlan("monthly_50", Usd(50))
  case object Monthly100 extends LichessPlan("monthly_100", Usd(100))

  val all = List(Monthly5, Monthly10, Monthly20, Monthly50, Monthly100)
  val min = Monthly5

  def findUnder(cents: Cents): Option[LichessPlan] = all.reverse.find(_.cents <= cents)
}

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripePlan(id: String, amount: Int) {
  def cents = Cents(amount)
  def usd = cents.usd
}

case class StripeSubscription(id: String, plan: StripePlan, customer: String)

case class StripeCustomer(id: String, subscriptions: StripeSubscriptions) {

  def firstSubscription = subscriptions.data.headOption
}

case class StripeCharge(amount: Int, customer: String)
