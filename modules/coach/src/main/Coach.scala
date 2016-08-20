package lila.coach

import org.joda.time.DateTime

case class Coach(
  _id: Coach.Id, // user ID
  createdAt: DateTime,
  updatedAt: DateTime)

object Coach {

  case class Id(value: String) extends AnyVal with StringValue
}
