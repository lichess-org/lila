package lila.plan

import play.api.libs.json._
import play.api.libs.functional.syntax._

private[plan] object JsonHandlers {

  implicit val StripeSubscriptionId = Reads.of[String].map(SubscriptionId.apply)
  implicit val StripeClientId       = Reads.of[String].map(ClientId.apply)
  implicit val StripeSessionId      = Reads.of[String].map(SessionId.apply)
  implicit val StripeCustomerId     = Reads.of[String].map(CustomerId.apply)
  implicit val StripeChargeId       = Reads.of[String].map(ChargeId.apply)
  implicit val StripeCents          = Reads.of[Int].map(Cents.apply)
  implicit val StripePriceReads     = Json.reads[StripePrice]
  // require that the items array is not empty.
  implicit val StripeSubscriptionReads: Reads[StripeSubscription] = (
    (__ \ "id").read[String] and
      (__ \ "items" \ "data" \ 0 \ "price").read[StripePrice] and
      (__ \ "customer").read[CustomerId] and
      (__ \ "cancel_at_period_end").read[Boolean] and
      (__ \ "status").read[String]
  )(StripeSubscription.apply _)
  implicit val StripeSubscriptionsReads    = Json.reads[StripeSubscriptions]
  implicit val StripeCustomerReads         = Json.reads[StripeCustomer]
  implicit val StripeAddressReads          = Json.reads[StripeCharge.Address]
  implicit val StripeBillingReads          = Json.reads[StripeCharge.BillingDetails]
  implicit val StripeChargeReads           = Json.reads[StripeCharge]
  implicit val StripeInvoiceReads          = Json.reads[StripeInvoice]
  implicit val StripeSessionReads          = Json.reads[StripeSession]
  implicit val StripeSessionCompletedReads = Json.reads[StripeCompletedSession]
}
