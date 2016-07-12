package lila.plan

import play.api.libs.json._

private[plan] object JsonHandlers {

  implicit val StripeCustomerId = Reads.of[String].map(CustomerId.apply)
  implicit val StripeChargeId = Reads.of[String].map(ChargeId.apply)
  implicit val StripeCents = Reads.of[Int].map(Cents.apply)
  implicit val StripePlanReads = Json.reads[StripePlan]
  implicit val StripeSubscriptionReads = Json.reads[StripeSubscription]
  implicit val StripeSubscriptionsReads = Json.reads[StripeSubscriptions]
  implicit val StripeCustomerReads = Json.reads[StripeCustomer]
  implicit val StripeChargeReads = Json.reads[StripeCharge]
  implicit val StripeInvoiceReads = Json.reads[StripeInvoice]
}
