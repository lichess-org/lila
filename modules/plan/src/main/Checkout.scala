package lila.plan

import play.api.data._
import play.api.data.Forms._

case class Checkout(token: String, amount: Int) {

  def source = Source(token)

  def cents = Cents(amount)
}

object Checkout {

  val form = Form(mapping(
    "token" -> nonEmptyText,
    "amount" -> number(min = 500)
  )(Checkout.apply)(Checkout.unapply))
}

case class Switch(plan: Option[String], cancel: Option[Int])

object Switch {

  val form = Form(mapping(
    "plan" -> optional(text.verifying("Invalid plan", LichessPlan.exists _)),
    "cancel" -> optional(number)
  )(Switch.apply)(Switch.unapply))
}
