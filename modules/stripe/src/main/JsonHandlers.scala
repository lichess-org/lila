package lila.stripe

import play.api.libs.json._

object JsonHandlers {

  implicit val StripePlanReads = Json.reads[StripePlan]
  implicit val StripeSubscriptionReads = Json.reads[StripeSubscription]
  implicit val StripeSubscriptionsReads = Json.reads[StripeSubscriptions]
  implicit val StripeCustomerReads = Json.reads[StripeCustomer]
  implicit val StripeChargeReads = Json.reads[StripeCharge]
  implicit val StripeInvoiceReads = Json.reads[StripeInvoice]
}
