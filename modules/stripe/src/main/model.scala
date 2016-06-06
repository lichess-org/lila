package lila.stripe

case class Source(value: String) extends AnyVal
case class CustomerId(value: String) extends AnyVal

sealed abstract class Plan(val id: String, val usd: Int) {

  def cents = usd * 100
}

object Plan {
  case object Monthly5 extends Plan("monthly_5", 5)
  case object Monthly10 extends Plan("monthly_10", 10)
  case object Monthly20 extends Plan("monthly_20", 20)
  case object Monthly50 extends Plan("monthly_50", 50)
  case object Monthly100 extends Plan("monthly_100", 100)

  val all = List(Monthly5, Monthly10, Monthly20, Monthly50, Monthly100)
}
