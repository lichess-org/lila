package lila.plan

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Currency
import scala.util.Try

private[plan] object JsonHandlers {

  implicit val CurrencyReads = Reads.of[String].flatMapResult { code =>
    Try(Currency getInstance code.toUpperCase).fold(err => JsError(err.getMessage), cur => JsSuccess(cur))
  }
  implicit val CountryReads = Reads.of[String].map(Country.apply)

  object stripe {
    implicit val SubscriptionIdReads = Reads.of[String].map(StripeSubscriptionId.apply)
    implicit val SessionIdReads      = Reads.of[String].map(StripeSessionId.apply)
    implicit val CustomerIdReads     = Reads.of[String].map(StripeCustomerId.apply)
    implicit val ChargeIdReads       = Reads.of[String].map(StripeChargeId.apply)
    implicit val AmountReads         = Reads.of[Int].map(StripeAmount.apply)
    implicit val PriceReads          = Json.reads[StripePrice]
    implicit val ItemReads           = Json.reads[StripeItem]
    // require that the items array is not empty.
    implicit val SubscriptionReads: Reads[StripeSubscription] = (
      (__ \ "id").read[String] and
        (__ \ "items" \ "data" \ 0).read[StripeItem] and
        (__ \ "customer").read[StripeCustomerId] and
        (__ \ "cancel_at_period_end").read[Boolean] and
        (__ \ "status").read[String] and
        (__ \ "default_payment_method").readNullable[String]
    )(StripeSubscription.apply _)
    implicit val SubscriptionsReads     = Json.reads[StripeSubscriptions]
    implicit val CustomerReads          = Json.reads[StripeCustomer]
    implicit val AddressReads           = Json.reads[StripeCharge.Address]
    implicit val BillingReads           = Json.reads[StripeCharge.BillingDetails]
    implicit val ChargeReads            = Json.reads[StripeCharge]
    implicit val InvoiceReads           = Json.reads[StripeInvoice]
    implicit val SessionReads           = Json.reads[StripeSession]
    implicit val SessionCompletedReads  = Json.reads[StripeCompletedSession]
    implicit val CardReads              = Json.reads[StripeCard]
    implicit val PaymentMethodReads     = Json.reads[StripePaymentMethod]
    implicit val SetupIntentReads       = Json.reads[StripeSetupIntent]
    implicit val SessionWithIntentReads = Json.reads[StripeSessionWithIntent]
  }

  object payPal {
    import play.api.libs.json.JodaReads._
    implicit val PayerIdReads             = Reads.of[String].map(PayPalPayerId.apply)
    implicit val OrderIdReads             = Reads.of[String].map(PayPalOrderId.apply)
    implicit val SubscriptionIdReads      = Reads.of[String].map(PayPalSubscriptionId.apply)
    implicit val OrderCreatedReads        = Json.reads[PayPalOrderCreated]
    implicit val SubscriptionCreatedReads = Json.reads[PayPalSubscriptionCreated]
    implicit val AmountReads              = Json.reads[PayPalAmount]
    implicit val PurchaseUnitReads        = Json.reads[PayPalPurchaseUnit]
    implicit val AddressReads             = Json.reads[PayPalAddress]
    implicit val PayerReads               = Json.reads[PayPalPayer]
    implicit val OrderReads               = Json.reads[PayPalOrder]
    implicit val PaymentReads             = Json.reads[PayPalPayment]
    implicit val BillingInfoReads         = Json.reads[PayPalBillingInfo]
    implicit val SubscriptionReads        = Json.reads[PayPalSubscription]
  }
}
