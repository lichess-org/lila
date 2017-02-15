package lila.plan

import play.api.data._
import play.api.data.Forms._

case class Checkout(
    token: Source,
    email: Option[String],
    amount: Cents,
    freq: Freq
) {

  def source = token

  def cents = amount

  def toFormData = Some(
    token.value, email, amount.value, freq.toString.toLowerCase
  )
}

object Checkout {

  def make(
    token: String,
    email: Option[String],
    amount: Int,
    freq: String
  ) = Checkout(
    Source(token), email, Cents(amount),
    if (freq == "monthly") Freq.Monthly else Freq.Onetime
  )

  val form = Form[Checkout](mapping(
    "token" -> nonEmptyText,
    "email" -> optional(email),
    "amount" -> number(min = 100, max = 100 * 100000),
    "freq" -> nonEmptyText
  )(Checkout.make)(_.toFormData))
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
