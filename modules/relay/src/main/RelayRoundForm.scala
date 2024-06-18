package lila.relay

import io.mola.galimatias.URL
import play.api.data.*
import play.api.data.Forms.*
import play.api.Mode

import scala.util.Try
import scala.util.chaining.*

import lila.common.Form.{ cleanText, into }
import scalalib.model.Seconds
import lila.core.perm.Granter

import lila.relay.RelayRound.Sync

final class RelayRoundForm(using mode: Mode):

  import RelayRoundForm.*
  import lila.common.Form.ISOInstantOrTimestamp

  val roundMapping =
    mapping(
      "name"    -> cleanText(minLength = 3, maxLength = 80).into[RelayRound.Name],
      "caption" -> optional(cleanText(minLength = 3, maxLength = 80).into[RelayRound.Caption]),
      "syncUrl" -> optional {
        cleanText(minLength = 8, maxLength = 600)
          .verifying("Invalid source", validSource)
          .verifying("The source URL cannot specify a port", url => mode.notProd || validSourcePort(url))
      },
      "syncUrlRound" -> optional(number(min = 1, max = 999)),
      "startsAt"     -> optional(ISOInstantOrTimestamp.mapping),
      "finished"     -> optional(boolean),
      "period"       -> optional(number(min = 2, max = 60).into[Seconds]),
      "delay"        -> optional(number(min = 0, max = RelayDelay.maxSeconds.value).into[Seconds]),
      "onlyRound"    -> optional(number(min = 1, max = 999)),
      "slices" -> optional:
        nonEmptyText
          .transform[List[RelayGame.Slice]](RelayGame.Slices.parse, RelayGame.Slices.show)
    )(Data.apply)(unapply)
      .verifying("This source requires a round number. See the new form field below.", !_.roundMissing)

  def create(trs: RelayTour.WithRounds) = Form(
    roundMapping
      .verifying(
        s"Maximum rounds per tournament: ${RelayTour.maxRelays}",
        _ => trs.rounds.sizeIs < RelayTour.maxRelays
      )
  ).fill(fillFromPrevRounds(trs.rounds))

  def edit(r: RelayRound) = Form(roundMapping).fill(Data.make(r))

object RelayRoundForm:

  def fillFromPrevRounds(rounds: List[RelayRound]): Data =
    val prevs: Option[(RelayRound, RelayRound)] = rounds.reverse match
      case a :: b :: _ => (a, b).some
      case _           => none
    val prev: Option[RelayRound] = rounds.lastOption
    val roundNumberRegex         = """([^\d]*)(\d{1,2})([^\d]*)""".r
    val roundNumberIn: String => Option[Int] =
      case roundNumberRegex(_, n, _) => n.toIntOption
      case _                         => none
    def replaceRoundNumber(s: String, n: Int): String =
      roundNumberRegex.replaceAllIn(s, m => s"${m.group(1)}${n}${m.group(3)}")
    val prevNumber: Option[Int] = prev.flatMap(p => roundNumberIn(p.name.value))
    val nextNumber              = (prevNumber | rounds.size) + 1
    val guessName = for
      n <- prevNumber
      if prevs
        .map(_._2)
        .fold(true): old =>
          roundNumberIn(old.name.value).contains(n - 1)
      p <- prev
    yield replaceRoundNumber(p.name.value, nextNumber)
    val nextUrl =
      prev.flatMap(_.sync.upstream).flatMap(_.asUrl).map(_.withRound).filter(_.round.isDefined).map(_.url)
    val guessDate = for
      (prev, old) <- prevs
      prevDate    <- prev.startsAt
      oldDate     <- old.startsAt
      delta = prevDate.toEpochMilli - oldDate.toEpochMilli
    yield prevDate.plusMillis(delta)
    Data(
      name = RelayRound.Name(guessName | s"Round ${nextNumber}"),
      caption = prev.flatMap(_.caption),
      syncUrl = nextUrl,
      syncUrlRound = (prevs.isEmpty || nextUrl.isDefined).option(nextNumber),
      startsAt = guessDate,
      period = prev.flatMap(_.sync.period),
      delay = prev.flatMap(_.sync.delay),
      onlyRound = prev.flatMap(_.sync.onlyRound).map(_ + 1),
      slices = prev.flatMap(_.sync.slices)
    )

  case class GameIds(ids: List[GameId])

  private def toGameIds(ids: String): Option[GameIds] =
    val list = ids.split(' ').view.flatMap(i => GameId.from(i.trim)).toList
    (list.sizeIs > 0 && list.sizeIs <= RelayFetch.maxChapters.value).option(GameIds(list))

  private def validSource(source: String)(using mode: Mode): Boolean =
    cleanUrl(source).isDefined || toGameIds(source).isDefined

  private def cleanUrl(source: String)(using mode: Mode): Option[String] =
    for
      url <- Try(URL.parse(source)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      // prevent common mistakes (not for security)
      if mode.notProd || !blocklist.exists(subdomain(host, _))
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
      name: RelayRound.Name,
      caption: Option[RelayRound.Caption],
      syncUrl: Option[String] = None,
      syncUrlRound: Option[Int] = None,
      startsAt: Option[Instant] = None,
      finished: Option[Boolean] = None,
      period: Option[Seconds] = None,
      delay: Option[Seconds] = None,
      onlyRound: Option[Int] = None,
      slices: Option[List[RelayGame.Slice]] = None
  ):

    def requiresRound = syncUrl.exists(RelayRound.Sync.UpstreamUrl.LccRegex.matches)

    def roundMissing = requiresRound && syncUrlRound.isEmpty

    def gameIds = syncUrl.flatMap(toGameIds)

    def update(official: Boolean)(relay: RelayRound)(using me: Me)(using mode: Mode) =
      relay.copy(
        name = name,
        caption = caption,
        sync = makeSync(me).pipe: sync =>
          if relay.sync.playing then sync.play(official) else sync,
        startsAt = startsAt,
        finished = ~finished
      )

    private def makeSync(user: User)(using mode: Mode): Sync =
      RelayRound.Sync(
        upstream = syncUrl
          .flatMap(cleanUrl)
          .map { u =>
            RelayRound.Sync.UpstreamUrl(s"$u${syncUrlRound.so(" " +)}")
          }
          .orElse(gameIds.map { ids =>
            RelayRound.Sync.UpstreamIds(ids.ids)
          }),
        until = none,
        nextAt = none,
        period = period.ifTrue(Granter.ofUser(_.StudyAdmin)(user)),
        delay = delay,
        onlyRound = onlyRound,
        slices = slices,
        log = SyncLog.empty
      )

    def make(user: User, tour: RelayTour)(using mode: Mode) =
      RelayRound(
        id = RelayRound.makeId,
        tourId = tour.id,
        name = name,
        caption = caption,
        sync = makeSync(user),
        createdAt = nowInstant,
        crowd = none,
        finished = ~finished,
        startsAt = startsAt,
        startedAt = none
      )

  object Data:

    def make(relay: RelayRound) =
      Data(
        name = relay.name,
        caption = relay.caption,
        syncUrl = relay.sync.upstream.map {
          case url: RelayRound.Sync.UpstreamUrl => url.withRound.url
          case RelayRound.Sync.UpstreamIds(ids) => ids.mkString(" ")
        },
        syncUrlRound = relay.sync.upstream.flatMap(_.asUrl).flatMap(_.withRound.round),
        startsAt = relay.startsAt,
        finished = relay.finished.option(true),
        period = relay.sync.period,
        onlyRound = relay.sync.onlyRound,
        slices = relay.sync.slices,
        delay = relay.sync.delay
      )
