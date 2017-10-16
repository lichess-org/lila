package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import lila.user.User

object RelayForm {

  import lila.common.Form.UTCDate._

  val syncTypes = List(
    "dgt-one" -> "DGT (old): all games in a single file",
    "dgt-many" -> "DGT (new): one file per game"
  )

  val form = Form(mapping(
    "name" -> nonEmptyText(minLength = 3, maxLength = 80),
    "description" -> nonEmptyText(minLength = 3, maxLength = 4000),
    "syncType" -> text.verifying(syncTypes.map(_._1).contains _),
    "syncUrl" -> nonEmptyText,
    "startsAt" -> optional(utcDate)
  )(Data.apply)(Data.unapply))

  def create = form

  def edit(r: Relay) = form fill Data.make(r)

  case class Data(
      name: String,
      description: String,
      syncType: String,
      syncUrl: String,
      startsAt: Option[DateTime]
  ) {

    def cleanUrl = {
      val trimmed = syncUrl.trim
      if (trimmed endsWith "/") trimmed.take(trimmed.size - 1)
      else trimmed
    }

    def update(relay: Relay) = relay.copy(
      name = name,
      description = description,
      sync = makeSync,
      startsAt = startsAt
    )

    def makeSync = Relay.Sync(
      upstream = syncType match {
        case "dgt-one" => Relay.Sync.Upstream.DgtOneFile(cleanUrl)
        case _ => Relay.Sync.Upstream.DgtManyFiles(cleanUrl)
      },
      until = none,
      nextAt = none,
      delay = none,
      log = SyncLog(Vector.empty)
    )

    def make(user: User) = Relay(
      _id = Relay.makeId,
      name = name,
      description = description,
      ownerId = user.id,
      sync = makeSync,
      likes = lila.study.Study.Likes(1),
      createdAt = DateTime.now,
      finished = false,
      startsAt = startsAt,
      startedAt = none
    )
  }

  object Data {

    def make(relay: Relay) = Data(
      name = relay.name,
      description = relay.description,
      syncType = relay.sync.upstream.key,
      syncUrl = relay.sync.upstream.url,
      startsAt = relay.startsAt
    )
  }
}
