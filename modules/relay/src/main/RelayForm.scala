package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import io.lemonlabs.uri.AbsoluteUrl

import lila.security.Granter
import lila.user.User
import lila.common.Form.clean

final class RelayForm {

  import RelayForm._
  import lila.common.Form.ISODateTimeOrTimestamp

  val form = Form(
    mapping(
      "name"        -> clean(text(minLength = 3, maxLength = 80)),
      "description" -> clean(text(minLength = 3, maxLength = 400)),
      "markup"      -> optional(text(maxLength = 20000)),
      "official"    -> optional(boolean),
      "syncUrl" -> optional {
        nonEmptyText.verifying("Invalid source", validSource _)
      },
      "syncUrlRound" -> optional(number(min = 1, max = 999)),
      "credit"       -> optional(clean(nonEmptyText)),
      "startsAt"     -> optional(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp),
      "throttle"     -> optional(number(min = 2, max = 60))
    )(Data.apply)(Data.unapply)
      .verifying("This source requires a round number. See the new form field below.", !_.roundMissing)
  )

  def create = form

  def edit(r: Relay) = form fill Data.make(r)
}

object RelayForm {

  private def validSource(url: String) =
    try {
      AbsoluteUrl
        .parse(url)
        .hostOption
        .exists { host =>
          host.apexDomain.fold(true) { apex =>
            !blocklist.contains(apex) && (
              // only allow public API, not arbitrary URLs
              apex != "chess.com" || url.startsWith("https://api.chess.com/pub")
            )
          }
        }
    } catch {
      case _: io.lemonlabs.uri.parsing.UriParsingException => false
    }

  private val blocklist = List(
    "twitch.tv",
    "twitch.com",
    "youtube.com",
    "youtu.be",
    "lichess.org",
    "google.com",
    "chess.com",
    "vk.com",
    "localhost",
    "chess-results.com",
    "chessgames.com"
  )

  case class Data(
      name: String,
      description: String,
      markup: Option[String],
      official: Option[Boolean],
      syncUrl: Option[String],
      syncUrlRound: Option[Int],
      credit: Option[String],
      startsAt: Option[DateTime],
      throttle: Option[Int]
  ) {

    def requiresRound = syncUrl exists Relay.Sync.LccRegex.matches

    def roundMissing = requiresRound && syncUrlRound.isEmpty

    def cleanUrl: Option[String] =
      syncUrl.map { u =>
        val trimmed = u.trim
        if (trimmed endsWith "/") trimmed.take(trimmed.length - 1)
        else trimmed
      }

    def update(relay: Relay, user: User) =
      relay.copy(
        name = name,
        description = description,
        markup = markup,
        official = ~official && Granter(_.Relay)(user),
        sync = makeSync(user),
        credit = credit,
        startsAt = startsAt,
        finished = relay.finished && startsAt.fold(true)(_.isBeforeNow)
      )

    def makeSync(user: User) =
      Relay.Sync(
        upstream = cleanUrl map { u =>
          Relay.Sync.Upstream(s"$u${syncUrlRound.??(" " +)}")
        },
        until = none,
        nextAt = none,
        delay = throttle ifTrue Granter(_.Relay)(user),
        log = SyncLog.empty
      )

    def make(user: User) =
      Relay(
        _id = Relay.makeId,
        name = name,
        description = description,
        markup = markup,
        ownerId = user.id,
        sync = makeSync(user),
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

    def make(relay: Relay) =
      Data(
        name = relay.name,
        description = relay.description,
        markup = relay.markup,
        official = relay.official option true,
        syncUrl = relay.sync.upstream.map(_.withRound.url),
        syncUrlRound = relay.sync.upstream.flatMap(_.withRound.round),
        credit = relay.credit,
        startsAt = relay.startsAt,
        throttle = relay.sync.delay
      )
  }
}
