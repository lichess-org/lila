package lila.relay

import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import scala.util.chaining._

import lila.common.Form.{ cleanNonEmptyText, cleanText }
import lila.game.Game
import lila.security.Granter
import lila.study.Study
import lila.user.User

final class RelayForm {

  import RelayForm._
  import lila.common.Form.ISODateTimeOrTimestamp

  val form = Form(
    mapping(
      "name"        -> cleanText(minLength = 3, maxLength = 80),
      "description" -> cleanText(minLength = 3, maxLength = 400),
      "markup"      -> optional(cleanText(maxLength = 20000)),
      "official"    -> optional(boolean),
      "syncUrl" -> optional {
        cleanText(minLength = 8, maxLength = 600).verifying("Invalid source", validSource _)
      },
      "syncUrlRound" -> optional(number(min = 1, max = 999)),
      "credit"       -> optional(cleanNonEmptyText),
      "startsAt"     -> optional(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp),
      "throttle"     -> optional(number(min = 2, max = 60))
    )(Data.apply)(Data.unapply)
      .verifying("This source requires a round number. See the new form field below.", !_.roundMissing)
  )

  def create = form

  def edit(r: Relay) = form fill Data.make(r)
}

object RelayForm {

  case class GameIds(ids: List[Game.ID])

  private def toGameIds(ids: String): Option[GameIds] = {
    val list = ids.split(' ').view.map(_.trim take Game.gameIdSize).filter(Game.validId).toList
    (list.sizeIs > 0 && list.sizeIs <= Study.maxChapters) option GameIds(list)
  }

  private def validSource(source: String): Boolean =
    validUrl(source) || toGameIds(source).isDefined

  private def validUrl(source: String): Boolean =
    AbsoluteUrl.parseOption(source).exists { url =>
      url.hostOption
        .exists { host =>
          host.toString != "localhost" &&
          host.toString != "127.0.0.1" &&
          host.apexDomain.fold(true) { apex =>
            !blocklist.contains(apex) && (
              // only allow public API, not arbitrary URLs
              apex != "chess.com" || url.toString.startsWith("https://api.chess.com/pub")
            )
          }
        }
    }

  private val blocklist = List(
    "twitch.tv",
    "twitch.com",
    "youtube.com",
    "youtu.be",
    "lichess.org",
    "google.com",
    "vk.com",
    "localhost",
    "chess-results.com",
    "chessgames.com",
    "zoom.us",
    "facebook.com",
    "herokuapp.com"
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

    def requiresRound = syncUrl exists Relay.Sync.UpstreamUrl.LccRegex.matches

    def roundMissing = requiresRound && syncUrlRound.isEmpty

    def cleanUrl: Option[String] =
      syncUrl.filter(validUrl).map { u =>
        u stripSuffix "/"
      }

    def gameIds = syncUrl flatMap toGameIds

    def update(relay: Relay, user: User) =
      relay.copy(
        name = name,
        description = description,
        markup = markup,
        official = ~official && Granter(_.Relay)(user),
        sync = makeSync(user) pipe { sync =>
          if (relay.sync.playing) sync.play else sync
        },
        credit = credit,
        startsAt = startsAt,
        finished = relay.finished && startsAt.fold(true)(_.isBeforeNow)
      )

    private def makeSync(user: User) =
      Relay.Sync(
        upstream = cleanUrl.map { u =>
          Relay.Sync.UpstreamUrl(s"$u${syncUrlRound.??(" " +)}")
        } orElse gameIds.map { ids =>
          Relay.Sync.UpstreamIds(ids.ids)
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
        syncUrl = relay.sync.upstream map {
          case url: Relay.Sync.UpstreamUrl => url.withRound.url
          case Relay.Sync.UpstreamIds(ids) => ids mkString " "
        },
        syncUrlRound = relay.sync.upstream.flatMap(_.asUrl).flatMap(_.withRound.round),
        credit = relay.credit,
        startsAt = relay.startsAt,
        throttle = relay.sync.delay
      )
  }
}
