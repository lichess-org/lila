package lila.plan

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

import java.util.Currency
import scala.util.Try

import lila.common.Json.given
import lila.core.net.IpAddress

object StripeJson:
  def toClient(session: StripeSession) =
    Json.obj("session" -> Json.obj("id" -> session.id, "clientSecret" -> session.clientSecret))

private object JsonHandlers:

  given Reads[Currency] = lila.common.Json.tryRead(code => Try(Currency.getInstance(code.toUpperCase)))

  object stripe:
    given Reads[StripePrice] = Json.reads
    given Reads[StripeItem]  = Json.reads
    // require that the items array is not empty.
    given Reads[StripeSubscription] = (
      (__ \ "id")
        .read[String]
        .and((__ \ "items" \ "data" \ 0).read[StripeItem])
        .and((__ \ "customer").read[StripeCustomerId])
        .and((__ \ "cancel_at_period_end").read[Boolean])
        .and((__ \ "status").read[String])
        .and((__ \ "default_payment_method").readNullable[String])
        .and((__ \ "ipAddress").readNullable[String].map(_.flatMap(IpAddress.from)))
    )(StripeSubscription.apply)
    given Reads[StripeSubscriptions]         = Json.reads
    given Reads[StripeCustomer]              = Json.reads
    given Reads[StripeCharge.Address]        = Json.reads
    given Reads[StripeCharge.BillingDetails] = Json.reads
    given Reads[StripeCharge]                = Json.reads
    given Reads[StripeInvoice]               = Json.reads
    given Reads[StripePaymentIntent]         = Json.reads
    given Reads[StripeSession]               = Json.reads
    given Reads[StripeCompletedSession]      = Json.reads
    given Reads[StripeCard]                  = Json.reads
    given Reads[StripePaymentMethod]         = Json.reads
    given Reads[StripeSetupIntent]           = Json.reads
    given Reads[StripeSessionWithIntent]     = Json.reads

  object payPal:
    given Reads[PayPalPayerId]             = Reads.of[String].map(PayPalPayerId.apply)
    given Reads[PayPalOrderId]             = Reads.of[String].map(PayPalOrderId.apply)
    given Reads[PayPalSubscriptionId]      = Reads.of[String].map(PayPalSubscriptionId.apply)
    given Reads[PayPalEventId]             = Reads.of[String].map(PayPalEventId.apply)
    given Reads[PayPalPlanId]              = Reads.of[String].map(PayPalPlanId.apply)
    given Reads[PayPalTransactionId]       = Reads.of[String].map(PayPalTransactionId.apply)
    given Reads[PayPalOrderCreated]        = Json.reads
    given Reads[PayPalSubscriptionCreated] = Json.reads
    given Reads[PayPalAmount]              = Json.reads
    given Reads[PayPalPurchaseUnit]        = Json.reads
    given Reads[PayPalAddress]             = Json.reads
    given Reads[PayPalPayer]               = Json.reads
    given Reads[PayPalOrder]               = Json.reads
    given Reads[PayPalPayment]             = Json.reads
    given Reads[PayPalBillingInfo]         = Json.reads
    given Reads[PayPalSubscription]        = Json.reads
    given Reads[PayPalEvent]               = Json.reads
    given Reads[PayPalPlan]                = Json.reads
    given Reads[PayPalCapture]             = Json.reads
    given Reads[PayPalSaleAmount]          = Json.reads
    given Reads[PayPalSale]                = Json.reads
