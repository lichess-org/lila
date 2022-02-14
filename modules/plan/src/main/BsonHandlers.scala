package lila.plan

import java.util.Currency
import reactivemongo.api.bson._

import lila.db.dsl._

private[plan] object BsonHandlers {

  implicit val CurrencyBSONHandler = stringAnyValHandler[Currency](_.getCurrencyCode, Currency.getInstance)
  implicit val MoneyBSONHandler    = Macros.handler[Money]
  implicit val UsdBSONHandler      = lila.db.dsl.bigDecimalAnyValHandler[Usd](_.value, Usd)

  implicit val StripeChargeIdBSONHandler = stringAnyValHandler[StripeChargeId](_.value, StripeChargeId.apply)
  implicit val StripeCustomerIdBSONHandler =
    stringAnyValHandler[StripeCustomerId](_.value, StripeCustomerId.apply)

  implicit val PayPalOrderIdBSONHandler = stringAnyValHandler[PayPalOrderId](_.value, PayPalOrderId.apply)
  implicit val PayPalPayerIdBSONHandler = stringAnyValHandler[PayPalPayerId](_.value, PayPalPayerId.apply)

  object PatronHandlers {
    import Patron._
    implicit val PayPalEmailBSONHandler =
      stringAnyValHandler[PayPalLegacy.Email](_.value, PayPalLegacy.Email.apply)
    implicit val PayPalSubIdBSONHandler =
      stringAnyValHandler[PayPalLegacy.SubId](_.value, PayPalLegacy.SubId.apply)
    implicit val PayPalLegacyBSONHandler   = Macros.handler[PayPalLegacy]
    implicit val PayPalCheckoutBSONHandler = Macros.handler[PayPalCheckout]
    implicit val StripeBSONHandler         = Macros.handler[Stripe]
    implicit val FreeBSONHandler           = Macros.handler[Free]
    implicit val UserIdBSONHandler         = stringAnyValHandler[UserId](_.value, UserId.apply)
    implicit val PatronBSONHandler         = Macros.handler[Patron]
  }

  object ChargeHandlers {
    import Charge._
    implicit val StripeBSONHandler         = Macros.handler[Stripe]
    implicit val PayPalLegacyBSONHandler   = Macros.handler[PayPalLegacy]
    implicit val PayPalCheckoutBSONHandler = Macros.handler[PayPalCheckout]
    implicit val ChargeBSONHandler         = Macros.handler[Charge]
  }
}
