package lila.stripe

import org.joda.time.DateTime

case class Customer(
    _id: Customer.Id, // stripe customer ID
    userId: Customer.UserId, // unique index
    lastLevelUp: DateTime) {

  def id = _id

  def canLevelUp = lastLevelUp isBefore DateTime.now.minusDays(25)
}

object Customer {

  case class Id(value: String) extends AnyVal
  case class UserId(value: String) extends AnyVal

  private[stripe] object BSONHandlers {
    import reactivemongo.bson._
    import lila.db.dsl._

    implicit val CustomerIdBSONHandler = stringAnyValHandler[Id](_.value, Id.apply)
    implicit val CustomerUserIdBSONHandler = stringAnyValHandler[UserId](_.value, UserId.apply)
    implicit val CustomerBSONHandler = Macros.handler[Customer]
  }
}
