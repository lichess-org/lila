package lila.plan

import play.api.data._
import play.api.data.Forms._

case class Checkout(
    token: String,
    email: Option[String],
    amount: Int,
    freq: String) {

  def source = Source(token)

  def cents = Cents(amount)

  def isMonthly = freq == "monthly"
}

object Checkout {

  val form = Form(mapping(
    "token" -> nonEmptyText,
    "email" -> optional(email),
    "amount" -> number(min = 100, max = 100 * 100000),
    "freq" -> nonEmptyText
  )(Checkout.apply)(Checkout.unapply))
}

case class Switch(usd: BigDecimal) {

  def cents = Usd(usd).cents
}

object Switch {

  val form = Form(mapping(
    "usd" -> bigDecimal(precision = 10, scale = 2)
      .verifying(_ >= 1)
      .verifying(_ <= 100000)
  )(Switch.apply)(Switch.unapply))
}
