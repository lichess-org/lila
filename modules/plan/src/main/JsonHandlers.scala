package lila.plan

import play.api.libs.json._

private[plan] object JsonHandlers {

  implicit val StripeSubscriptionId: Reads[SubscriptionId] =
    Reads.of[String].map(SubscriptionId.apply)
  implicit val StripeClientId: Reads[ClientId]     = Reads.of[String].map(ClientId.apply)
  implicit val StripeSessionId: Reads[SessionId]   = Reads.of[String].map(SessionId.apply)
  implicit val StripeCustomerId: Reads[CustomerId] = Reads.of[String].map(CustomerId.apply)
  implicit val StripeChargeId: Reads[ChargeId]     = Reads.of[String].map(ChargeId.apply)
  implicit val StripeCents: Reads[Cents]           = Reads.of[Int].map(Cents.apply)
  implicit val StripePlanReads: Reads[StripePlan]  = Json.reads[StripePlan]
  implicit val StripeSubscriptionReads: Reads[StripeSubscription] = Json.reads[StripeSubscription]
  implicit val StripeSubscriptionsReads: Reads[StripeSubscriptions] =
    Json.reads[StripeSubscriptions]
  implicit val StripeCustomerReads: Reads[StripeCustomer] = Json.reads[StripeCustomer]
  implicit val StripeChargeReads: Reads[StripeCharge]     = Json.reads[StripeCharge]
  implicit val StripeInvoiceReads: Reads[StripeInvoice]   = Json.reads[StripeInvoice]
  implicit val StripeSessionReads: Reads[StripeSession]   = Json.reads[StripeSession]
  implicit val StripeSessionCompletedReads: Reads[StripeCompletedSession] =
    Json.reads[StripeCompletedSession]
}
