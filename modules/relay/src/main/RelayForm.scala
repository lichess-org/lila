package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import lila.user.User

object RelayForm {

  import lila.common.Form.UTCDate._

  val form = Form(mapping(
    "name" -> nonEmptyText(minLength = 3, maxLength = 80),
    "description" -> nonEmptyText(minLength = 3, maxLength = 4000),
    "pgnUrl" -> nonEmptyText,
    "startsAt" -> optional(utcDate)
  )(Data.apply)(Data.unapply))

  def create = form

  case class Data(
      name: String,
      description: String,
      pgnUrl: String,
      startsAt: Option[DateTime]
  ) {

    def update(relay: Relay) = relay.copy(
      name = name,
      description = description,
      pgnUrl = pgnUrl,
      startsAt = startsAt
    )

    def make(user: User) = Relay(
      _id = Relay.makeId,
      name = name,
      description = description,
      pgnUrl = pgnUrl,
      ownerId = user.id,
      createdAt = DateTime.now,
      startsAt = startsAt,
      syncLog = SyncLog(Nil),
      syncUntil = none
    )
  }

  object Data {

    def make(relay: Relay) = Data(
      name = relay.name,
      description = relay.description,
      pgnUrl = relay.pgnUrl,
      startsAt = relay.startsAt
    )
  }
}
