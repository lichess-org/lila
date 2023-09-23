package lila.relay

import io.mola.galimatias.URL
import play.api.data.*
import play.api.data.Forms.*
import scala.util.Try
import scala.util.chaining.*

import lila.common.Form.{ cleanText, into }
import lila.game.Game
import lila.security.Granter
import lila.study.Study
import lila.user.{ User, Me }
import lila.common.Seconds

final class RelayRoundForm:

  import RelayRoundForm.*
  import lila.common.Form.ISOInstantOrTimestamp

  val roundMapping =
    mapping(
      "name"    -> cleanText(minLength = 3, maxLength = 80).into[RelayRoundName],
      "caption" -> optional(cleanText(minLength = 3, maxLength = 80).into[RelayRound.Caption]),
      "syncUrl" -> optional {
        cleanText(minLength = 8, maxLength = 600)
          .verifying("Invalid source", validSource)
          .verifying("The source URL cannot specify a port", validSourcePort)
      },
      "syncUrlRound" -> optional(number(min = 1, max = 999)),
      "startsAt"     -> optional(ISOInstantOrTimestamp.mapping),
      "finished"     -> optional(boolean),
      "period"       -> optional(number(min = 2, max = 60).into[Seconds]),
      "delay" -> optional(
        number(min = 0, max = RelayDelay.maxSeconds.value).into[Seconds]
      ) // don't increase the max
    )(Data.apply)(unapply)
      .verifying("This source requires a round number. See the new form field below.", !_.roundMissing)

  def create(trs: RelayTour.WithRounds) = Form {
    roundMapping
      .verifying(
        s"Maximum rounds per tournament: ${RelayTour.maxRelays}",
        _ => trs.rounds.sizeIs < RelayTour.maxRelays
      )
  }.fill(
    Data(
      name = RelayRoundName(s"Round ${trs.rounds.size + 1}"),
      caption = none,
      syncUrlRound = Some(trs.rounds.size + 1)
    )
  )

  def edit(r: RelayRound) = Form(roundMapping) fill Data.make(r)

object RelayRoundForm:

  case class GameIds(ids: List[GameId])

  private def toGameIds(ids: String): Option[GameIds] =
    val list = ids.split(' ').view.flatMap(i => GameId from i.trim).toList
    (list.sizeIs > 0 && list.sizeIs <= Study.maxChapters) option GameIds(list)

  private def validSource(source: String): Boolean =
    cleanUrl(source).isDefined || toGameIds(source).isDefined

  private def cleanUrl(source: String): Option[String] =
    for
      url <- Try(URL.parse(source)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      // prevent common mistakes (not for security)
      if !blocklist.exists(subdomain(host, _))
      if !subdomain(host, "chess.com") || url.toString.startsWith("https://api.chess.com/pub")
    yield url.toString.stripSuffix("/")

  private val validPorts = Set(-1, 80, 443, 8080, 8491)
  private def validSourcePort(source: String): Boolean =
    Try(URL.parse(source)).toOption.forall: url =>
      validPorts(url.port)

  private def subdomain(host: String, domain: String) = s".$host".endsWith(s".$domain")

  private val blocklist = List(
    "localhost",
    "127.0.0.1",
    "0.0.0.0",
    "[::1]",
    "[::]",
    "twitch.tv",
    "twitch.com",
    "youtube.com",
    "youtu.be",
    "lichess.org",
    "google.com",
    "vk.com",
    "chess-results.com",
    "chessgames.com",
    "zoom.us",
    "facebook.com",
    "herokuapp.com"
  )

  case class Data(
      name: RelayRoundName,
      caption: Option[RelayRound.Caption],
      syncUrl: Option[String] = None,
      syncUrlRound: Option[Int] = None,
      startsAt: Option[Instant] = None,
      finished: Option[Boolean] = None,
      period: Option[Seconds] = None,
      delay: Option[Seconds] = None
  ):

    def requiresRound = syncUrl exists RelayRound.Sync.UpstreamUrl.LccRegex.matches

    def roundMissing = requiresRound && syncUrlRound.isEmpty

    def gameIds = syncUrl flatMap toGameIds

    def update(relay: RelayRound)(using me: Me) =
      relay.copy(
        name = name,
        caption = caption,
        sync = makeSync(me).pipe: sync =>
          if relay.sync.playing then sync.play else sync,
        startsAt = startsAt,
        finished = ~finished
      )

    private def makeSync(user: User) =
      RelayRound.Sync(
        upstream = syncUrl.flatMap(cleanUrl).map { u =>
          RelayRound.Sync.UpstreamUrl(s"$u${syncUrlRound.so(" " +)}")
        } orElse gameIds.map { ids =>
          RelayRound.Sync.UpstreamIds(ids.ids)
        },
        until = none,
        nextAt = none,
        period = period ifTrue Granter.of(_.Relay)(user),
        delay = delay,
        log = SyncLog.empty
      )

    def make(user: User, tour: RelayTour) =
      RelayRound(
        _id = RelayRound.makeId,
        tourId = tour.id,
        name = name,
        caption = caption,
        sync = makeSync(user),
        createdAt = nowInstant,
        finished = ~finished,
        startsAt = startsAt,
        startedAt = none
      )

  object Data:

    def make(relay: RelayRound) =
      Data(
        name = relay.name,
        caption = relay.caption,
        syncUrl = relay.sync.upstream map {
          case url: RelayRound.Sync.UpstreamUrl => url.withRound.url
          case RelayRound.Sync.UpstreamIds(ids) => ids mkString " "
        },
        syncUrlRound = relay.sync.upstream.flatMap(_.asUrl).flatMap(_.withRound.round),
        startsAt = relay.startsAt,
        finished = relay.finished option true,
        period = relay.sync.period,
        delay = relay.sync.delay
      )
