package lila.plan

import lila.db.dsl._
import reactivemongo.api.bson._

private[plan] object BsonHandlers {

  implicit val CentsBSONHandler      = intAnyValHandler[Cents](_.value, Cents.apply)
  implicit val ChargeIdBSONHandler   = stringAnyValHandler[ChargeId](_.value, ChargeId.apply)
  implicit val CustomerIdBSONHandler = stringAnyValHandler[CustomerId](_.value, CustomerId.apply)

  object PatronHandlers {
    import Patron._
    implicit val PayPalEmailBSONHandler = stringAnyValHandler[PayPal.Email](_.value, PayPal.Email.apply)
    implicit val PayPalSubIdBSONHandler = stringAnyValHandler[PayPal.SubId](_.value, PayPal.SubId.apply)
    implicit val PayPalBSONHandler      = Macros.handler[PayPal]
    implicit val StripeBSONHandler      = Macros.handler[Stripe]
    implicit val FreeBSONHandler        = Macros.handler[Free]
    implicit val UserIdBSONHandler      = stringAnyValHandler[UserId](_.value, UserId.apply)
    implicit val PatronBSONHandler      = Macros.handler[Patron]
  }

  object ChargeHandlers {
    import Charge._
    implicit val StripeBSONHandler = Macros.handler[Stripe]
    implicit val PayPalBSONHandler = Macros.handler[PayPal]
    implicit val ChargeBSONHandler = Macros.handler[Charge]
  }
}
