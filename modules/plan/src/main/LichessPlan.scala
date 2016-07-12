package lila.plan

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

  def byStripePlan(plan: StripePlan) = findUnder(plan.cents)

  def byId(id: String) = all.find(_.id == id)

  def exists(id: String) = all.exists(_.id == id)
}
