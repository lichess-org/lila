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

final class RelayRoundForm {

  import RelayRoundForm._
  import lila.common.Form.ISODateTimeOrTimestamp

  val roundMapping =
    mapping(
      "name" -> cleanText(minLength = 3, maxLength = 80),
      "syncUrl" -> optional {
        cleanText(minLength = 8, maxLength = 600).verifying("Invalid source", validSource _)
      },
      "syncUrlRound" -> optional(number(min = 1, max = 999)),
      "startsAt"     -> optional(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp),
      "throttle"     -> optional(number(min = 2, max = 60))
    )(Data.apply)(Data.unapply)
      .verifying("This source requires a round number. See the new form field below.", !_.roundMissing)

  def create(trs: RelayTour.WithRounds) = Form {
    roundMapping
      .verifying(
        s"Maximum rounds per tournament: ${RelayTour.maxRelays}",
        _ => trs.rounds.sizeIs < RelayTour.maxRelays
      )
  }.fill(Data(name = s"Round ${trs.rounds.size + 1}", syncUrlRound = Some(trs.rounds.size + 1)))

  def edit(r: RelayRound) = Form(roundMapping) fill Data.make(r)
}

object RelayRoundForm {

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
      syncUrl: Option[String] = None,
      syncUrlRound: Option[Int] = None,
      startsAt: Option[DateTime] = None,
      throttle: Option[Int] = None
  ) {

    def requiresRound = syncUrl exists RelayRound.Sync.UpstreamUrl.LccRegex.matches

    def roundMissing = requiresRound && syncUrlRound.isEmpty

    def cleanUrl: Option[String] =
      syncUrl.filter(validUrl).map { u =>
        u stripSuffix "/"
      }

    def gameIds = syncUrl flatMap toGameIds

    def update(relay: RelayRound, user: User) =
      relay.copy(
        name = name,
        sync = makeSync(user) pipe { sync =>
          if (relay.sync.playing) sync.play else sync
        },
        startsAt = startsAt,
        finished = relay.finished && startsAt.fold(true)(_.isBeforeNow)
      )

    private def makeSync(user: User) =
      RelayRound.Sync(
        upstream = cleanUrl.map { u =>
          RelayRound.Sync.UpstreamUrl(s"$u${syncUrlRound.??(" " +)}")
        } orElse gameIds.map { ids =>
          RelayRound.Sync.UpstreamIds(ids.ids)
        },
        until = none,
        nextAt = none,
        delay = throttle ifTrue Granter(_.Relay)(user),
        log = SyncLog.empty
      )

    def make(user: User, tour: RelayTour) =
      RelayRound(
        _id = RelayRound.makeId,
        tourId = tour.id,
        name = name,
        sync = makeSync(user),
        createdAt = DateTime.now,
        finished = false,
        startsAt = startsAt,
        startedAt = none
      )
  }

  object Data {

    def make(relay: RelayRound) =
      Data(
        name = relay.name,
        syncUrl = relay.sync.upstream map {
          case url: RelayRound.Sync.UpstreamUrl => url.withRound.url
          case RelayRound.Sync.UpstreamIds(ids) => ids mkString " "
        },
        syncUrlRound = relay.sync.upstream.flatMap(_.asUrl).flatMap(_.withRound.round),
        startsAt = relay.startsAt,
        throttle = relay.sync.delay
      )
  }
}
