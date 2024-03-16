package lila.plan

import reactivemongo.api.bson.*

import java.util.Currency

import lila.db.dsl.{ *, given }

private object BsonHandlers:

  given BSONHandler[Currency]      = stringAnyValHandler[Currency](_.getCurrencyCode, Currency.getInstance)
  given BSONDocumentHandler[Money] = Macros.handler

  given BSONHandler[PayPalOrderId] = stringAnyValHandler[PayPalOrderId](_.value, PayPalOrderId.apply)
  given BSONHandler[PayPalPayerId] = stringAnyValHandler[PayPalPayerId](_.value, PayPalPayerId.apply)
  given BSONHandler[PayPalSubscriptionId] =
    stringAnyValHandler[PayPalSubscriptionId](_.value, PayPalSubscriptionId.apply)

  object PatronHandlers:
    import Patron.*
    given BSONDocumentHandler[PayPalLegacy]                   = Macros.handler
    given payPalCheckout: BSONDocumentHandler[PayPalCheckout] = Macros.handler
    given BSONDocumentHandler[Stripe]                         = Macros.handler
    given BSONDocumentHandler[Free]                           = Macros.handler
    given BSONDocumentHandler[Patron]                         = Macros.handler

  object ChargeHandlers:
    import Charge.*
    import PatronHandlers.payPalCheckout
    given BSONDocumentHandler[Stripe]       = Macros.handler
    given BSONDocumentHandler[PayPalLegacy] = Macros.handler
    given BSONDocumentHandler[Charge]       = Macros.handler
