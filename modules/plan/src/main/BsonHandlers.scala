package lila.plan

import reactivemongo.api.bson._

import lila.db.dsl._

private[plan] object BsonHandlers {

  implicit val CentsBSONHandler: BSONHandler[Cents] = intAnyValHandler[Cents](_.value, Cents.apply)
  implicit val ChargeIdBSONHandler: BSONHandler[ChargeId] =
    stringAnyValHandler[ChargeId](_.value, ChargeId.apply)
  implicit val CustomerIdBSONHandler: BSONHandler[CustomerId] =
    stringAnyValHandler[CustomerId](_.value, CustomerId.apply)

  object PatronHandlers {
    import Patron._
    implicit val PayPalEmailBSONHandler: BSONHandler[PayPal.Email] =
      stringAnyValHandler[PayPal.Email](_.value, PayPal.Email.apply)
    implicit val PayPalSubIdBSONHandler: BSONHandler[PayPal.SubId] =
      stringAnyValHandler[PayPal.SubId](_.value, PayPal.SubId.apply)
    implicit val PayPalBSONHandler: BSONDocumentHandler[PayPal] = Macros.handler[PayPal]
    implicit val StripeBSONHandler: BSONDocumentHandler[Stripe] = Macros.handler[Stripe]
    implicit val FreeBSONHandler: BSONDocumentHandler[Free]     = Macros.handler[Free]
    implicit val UserIdBSONHandler: BSONHandler[UserId] =
      stringAnyValHandler[UserId](_.value, UserId.apply)
    implicit val PatronBSONHandler: BSONDocumentHandler[Patron] = Macros.handler[Patron]
  }

  object ChargeHandlers {
    import Charge._
    implicit val StripeBSONHandler: BSONDocumentHandler[Stripe] = Macros.handler[Stripe]
    implicit val PayPalBSONHandler: BSONDocumentHandler[PayPal] = Macros.handler[PayPal]
    implicit val ChargeBSONHandler: BSONDocumentHandler[Charge] = Macros.handler[Charge]
  }
}
