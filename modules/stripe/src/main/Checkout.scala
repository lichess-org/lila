package lila.stripe

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
