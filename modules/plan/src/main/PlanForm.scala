package lila.plan

import cats.implicits._
import play.api.data._
import play.api.data.Forms._

object PlanForm {

  private val txnTypes = Set("express_checkout", "web_accept", "recurring_payment", "subscr_payment")
  // ignored types = subscr_cancel, ...

  val ipn = Form(
    mapping(
      "txn_id"            -> optional(nonEmptyText),
      "subscr_id"         -> optional(nonEmptyText),
      "txn_type"          -> text.verifying("Invalid txn type", txnTypes contains _),
      "mc_currency"       -> nonEmptyText,
      "mc_gross"          -> bigDecimal,
      "mc_fee"            -> bigDecimal,
      "custom"            -> optional(text),
      "payer_email"       -> optional(nonEmptyText),
      "first_name"        -> optional(text),
      "last_name"         -> optional(text),
      "residence_country" -> optional(text)
    )(Ipn.apply)(Ipn.unapply)
  )

  case class Ipn(
      txnId: Option[String],
      subId: Option[String],
      txnType: String,
      currencyCode: String,
      gross: BigDecimal,
      fee: BigDecimal,
      userId: Option[String],
      email: Option[String],
      firstName: Option[String],
      lastName: Option[String],
      countryCode: Option[String]
  ) {

    def name = (firstName, lastName) mapN { _ + " " + _ }

    def country = countryCode map Country

    def money = for
  }
}
