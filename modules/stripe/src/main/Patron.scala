package lila.stripe

import org.joda.time.DateTime

case class Patron(
    _id: Patron.UserId,
    stripe: Option[Patron.Stripe] = none,
    payPal: Option[Patron.PayPal] = none,
    lastLevelUp: DateTime) {

  def id = _id

  def userId = _id

  def canLevelUp = lastLevelUp isBefore DateTime.now.minusDays(25)
}

object Patron {

  case class UserId(value: String) extends AnyVal

  case class Stripe(customerId: CustomerId)
  object Stripe {
    case class CustomerId(value: String) extends AnyVal
  }
  case class PayPal(email: Option[PayPal.Email], subId: Option[PayPal.SubId])
  object PayPal {
    case class Email(value: String) extends AnyVal
    case class SubId(value: String) extends AnyVal
  }

  private[stripe] object BSONHandlers {
    import reactivemongo.bson._
    import lila.db.dsl._
    implicit val StripeCustomerIdBSONHandler = stringAnyValHandler[CustomerId](_.value, CustomerId.apply)
    implicit val StripeBSONHandler = Macros.handler[Stripe]
    implicit val PayPalEmailBSONHandler = stringAnyValHandler[PayPal.Email](_.value, PayPal.Email.apply)
    implicit val PayPalSubIdBSONHandler = stringAnyValHandler[PayPal.SubId](_.value, PayPal.SubId.apply)
    implicit val PayPalBSONHandler = Macros.handler[PayPal]
    implicit val UserIdBSONHandler = stringAnyValHandler[UserId](_.value, UserId.apply)
    implicit val PatronBSONHandler = Macros.handler[Patron]
  }
}
