package lila.plan

import java.util.Currency
import reactivemongo.api.bson._

import lila.db.dsl._

private object BsonHandlers {

  given BSONHandler[Currency]      = stringAnyValHandler[Currency](_.getCurrencyCode, Currency.getInstance)
  given BSONDocumentHandler[Money] = Macros.handler[Money]
  given BSONHandler[Usd]           = lila.db.dsl.bigDecimalAnyValHandler[Usd](_.value, Usd)

  given BSONHandler[StripeChargeId]   = stringAnyValHandler[StripeChargeId](_.value, StripeChargeId)
  given BSONHandler[StripeCustomerId] = stringAnyValHandler[StripeCustomerId](_.value, StripeCustomerId)

  given BSONHandler[PayPalOrderId] = stringAnyValHandler[PayPalOrderId](_.value, PayPalOrderId)
  given BSONHandler[PayPalPayerId] = stringAnyValHandler[PayPalPayerId](_.value, PayPalPayerId)
  given BSONHandler[PayPalSubscriptionId] =
    stringAnyValHandler[PayPalSubscriptionId](_.value, PayPalSubscriptionId)

  object PatronHandlers {
    import Patron._
    given BSONHandler[PayPalLegacy.Email] =
      stringAnyValHandler[PayPalLegacy.Email](_.value, PayPalLegacy.Email)
    given BSONHandler[PayPalLegacy.SubId] =
      stringAnyValHandler[PayPalLegacy.SubId](_.value, PayPalLegacy.SubId)
    given BSONDocumentHandler[PayPalLegacy]   = Macros.handler[PayPalLegacy]
    given BSONDocumentHandler[PayPalCheckout] = Macros.handler[PayPalCheckout]
    given BSONDocumentHandler[Stripe]         = Macros.handler[Stripe]
    given BSONDocumentHandler[Free]           = Macros.handler[Free]
    given BSONHandler[UserId]                 = stringAnyValHandler[UserId](_.value, UserId)
    given BSONDocumentHandler[Patron]         = Macros.handler[Patron]
  }

  object ChargeHandlers {
    import Charge._
    given BSONDocumentHandler[Stripe]         = Macros.handler[Stripe]
    given BSONDocumentHandler[PayPalLegacy]   = Macros.handler[PayPalLegacy]
    given BSONDocumentHandler[PayPalCheckout] = Macros.handler[PayPalCheckout]
    given BSONDocumentHandler[Charge]         = Macros.handler[Charge]
  }
}
