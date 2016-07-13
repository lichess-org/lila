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
    "amount" -> number(min = 100)
  )(Checkout.apply)(Checkout.unapply))
}

case class Switch(usd: Int) {

  def cents = Usd(usd).cents
}

object Switch {

  val form = Form(mapping(
    "usd" -> number(min = 1)
  )(Switch.apply)(Switch.unapply))
}
