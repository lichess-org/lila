package lila.plan

import play.api.libs.json._

private[plan] object JsonHandlers {

  implicit val StripeSubscriptionId        = Reads.of[String].map(SubscriptionId.apply)
  implicit val StripeClientId              = Reads.of[String].map(ClientId.apply)
  implicit val StripeSessionId             = Reads.of[String].map(SessionId.apply)
  implicit val StripeCustomerId            = Reads.of[String].map(CustomerId.apply)
  implicit val StripeChargeId              = Reads.of[String].map(ChargeId.apply)
  implicit val StripeCents                 = Reads.of[Int].map(Cents.apply)
  implicit val StripePlanReads             = Json.reads[StripePlan]
  implicit val StripeSubscriptionReads     = Json.reads[StripeSubscription]
  implicit val StripeSubscriptionsReads    = Json.reads[StripeSubscriptions]
  implicit val StripeCustomerReads         = Json.reads[StripeCustomer]
  implicit val StripeAddressReads          = Json.reads[StripeCharge.Address]
  implicit val StripeBillingReads          = Json.reads[StripeCharge.BillingDetails]
  implicit val StripeChargeReads           = Json.reads[StripeCharge]
  implicit val StripeInvoiceReads          = Json.reads[StripeInvoice]
  implicit val StripeSessionReads          = Json.reads[StripeSession]
  implicit val StripeSessionCompletedReads = Json.reads[StripeCompletedSession]
}
