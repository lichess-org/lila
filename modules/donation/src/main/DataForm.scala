package lila.donation

import play.api.data._
import play.api.data.Forms._

object DataForm {

  private val txnTypes = Set("express_checkout", "web_accept", "recurring_payment")

  val ipn = Form(mapping(
    "txn_id" -> nonEmptyText,
    "txn_type" -> nonEmptyText.verifying("Invalid txn type", txnTypes contains _),
    "mc_gross" -> bigDecimal,
    "custom" -> optional(text),
    "payer_email" -> optional(text),
    "first_name" -> optional(text),
    "last_name" -> optional(text)
  )(Ipn.apply)(Ipn.unapply))

  case class Ipn(
      txnId: String,
      txnType: String,
      amount: BigDecimal,
      userId: Option[String],
      email: Option[String],
      firstName: Option[String],
      lastName: Option[String]) {

    def name = (firstName |@| lastName) apply { _ + " " + _ }

    def cents = (amount * 100).toInt
  }
}
