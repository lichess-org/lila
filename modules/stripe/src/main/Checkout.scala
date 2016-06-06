package lila.stripe

import play.api.data._
import play.api.data.Forms._

case class Checkout(token: String, cents: Int) {

  def source = Source(token)
}

object Checkout {

  val form = Form(mapping(
    "token" -> nonEmptyText,
    "cents" -> number(min = 500)
  )(Checkout.apply)(Checkout.unapply))
}
