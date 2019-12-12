package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.validation.Constraints._

import lila.security.Granter
import lila.user.User

object RelayForm {

  import lila.common.Form.UTCDate._

  val form = Form(
    mapping(
      "name" -> text(minLength = 3, maxLength = 80),
      "description" -> text(minLength = 3, maxLength = 400),
      "markup" -> optional(text(maxLength = 20000)),
      "official" -> optional(boolean),
      "syncUrl" -> nonEmptyText
        .verifying("Lichess tournaments can't be used as broadcast source", u => !isTournamentApi(u)),
      "syncUrlRound" -> optional(number(min = 1, max = 999)),
      "credit" -> optional(nonEmptyText),
      "startsAt" -> optional(utcDate),
      "throttle" -> optional(number(min = 2, max = 60))
    )(Data.apply)(Data.unapply)
      .verifying("This source requires a round number. See the new form field below.", !_.roundMissing)
  )

  private def isTournamentApi(url: String) =
    """/api/tournament/\w{8}/games""".r.find(url)

  def create = form

  def edit(r: Relay) = form fill Data.make(r)

  case class Data(
      name: String,
      description: String,
      markup: Option[String],
      official: Option[Boolean],
      syncUrl: String,
      syncUrlRound: Option[Int],
      credit: Option[String],
      startsAt: Option[DateTime],
      throttle: Option[Int]
  ) {

    def requiresRound = Relay.Sync.LccRegex matches syncUrl

    def roundMissing = requiresRound && syncUrlRound.isEmpty

    def cleanUrl = {
      val trimmed = syncUrl.trim
      if (trimmed endsWith "/") trimmed.take(trimmed.size - 1)
      else trimmed
    }

    def update(relay: Relay, user: User) = relay.copy(
      name = name,
      description = description,
      markup = markup,
      official = ~official && Granter(_.Relay)(user),
      sync = makeSync,
      credit = credit,
      startsAt = startsAt,
      finished = relay.finished && startsAt.fold(true)(_.isBefore(DateTime.now))
    )

    def makeSync = Relay.Sync(
      upstream = Relay.Sync.Upstream(s"$cleanUrl${syncUrlRound.??(" " +)}"),
      until = none,
      nextAt = none,
      delay = throttle,
      log = SyncLog.empty
    )

    def make(user: User) = Relay(
      _id = Relay.makeId,
      name = name,
      description = description,
      markup = markup,
      ownerId = user.id,
      sync = makeSync,
      credit = credit,
      likes = lila.study.Study.Likes(1),
      createdAt = DateTime.now,
      finished = false,
      official = ~official && Granter(_.Relay)(user),
      startsAt = startsAt,
      startedAt = none
    )
  }

  object Data {

    def make(relay: Relay) = Data(
      name = relay.name,
      description = relay.description,
      markup = relay.markup,
      official = relay.official option true,
      syncUrl = relay.sync.upstream.withRound.url,
      syncUrlRound = relay.sync.upstream.withRound.round,
      credit = relay.credit,
      startsAt = relay.startsAt,
      throttle = relay.sync.delay
    )
  }
}
