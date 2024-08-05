package lila.relay

import io.mola.galimatias.URL
import play.api.Mode
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter
import scalalib.model.Seconds

import scala.util.Try

import lila.common.Form.{ cleanText, formatter, into, stringIn }
import lila.core.perm.Granter
import lila.relay.RelayRound.Sync
import lila.relay.RelayRound.Sync.Upstream

final class RelayRoundForm(using mode: Mode):

  import RelayRoundForm.*
  import lila.common.Form.ISOInstantOrTimestamp

  private given Formatter[Upstream.Url] =
    formatter.stringTryFormatter(str => validateUpstreamUrl(str).map(Upstream.Url.apply), _.url.toString)
  private given Formatter[Upstream.Urls] = formatter.stringTryFormatter(
    _.linesIterator.toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .traverse(validateUpstreamUrl)
      .map(_.distinct)
      .map(Upstream.Urls.apply),
    _.urls.mkString("\n")
  )
  private given Formatter[Upstream.Ids] = formatter.stringTryFormatter(
    _.split(' ').toList
      .map(_.trim)
      .traverse: i =>
        GameId.from(i.trim).toRight(s"Invalid game ID: $i")
      .left
      .map(_.mkString(", "))
      .filterOrElse(_.sizeIs <= RelayFetch.maxChapters.value, s"Max games: ${RelayFetch.maxChapters}")
      .map(_.distinct)
      .map(Upstream.Ids.apply),
    _.ids.mkString(" ")
  )

  private def lccIsComplete(url: Upstream.Url) =
    url.lcc.isDefined || !url.url.host.toString.endsWith("livechesscloud.com")

  def roundMapping(using Me) =
    mapping(
      "name"       -> cleanText(minLength = 3, maxLength = 80).into[RelayRound.Name],
      "caption"    -> optional(cleanText(minLength = 3, maxLength = 80).into[RelayRound.Caption]),
      "syncSource" -> optional(stringIn(sourceTypes.map(_._1).toSet)),
      "syncUrl" -> optional(
        of[Upstream.Url]
          .verifying("LCC URLs must end with /{round-number}, e.g. /5 for round 5", lccIsComplete)
          .verifying(
            "Invalid source URL",
            u => !u.url.host.toString.endsWith("lichess.org") || Granter(_.Relay)
          )
      ),
      "syncUrls"            -> optional(of[Upstream.Urls]),
      "syncIds"             -> optional(of[Upstream.Ids]),
      "startsAt"            -> optional(ISOInstantOrTimestamp.mapping),
      "startsAfterPrevious" -> optional(boolean),
      "finished"            -> optional(boolean),
      "period"              -> optional(number(min = 2, max = 60).into[Seconds]),
      "delay"               -> optional(number(min = 0, max = RelayDelay.maxSeconds.value).into[Seconds]),
      "onlyRound"           -> optional(number(min = 1, max = 999)),
      "slices" -> optional:
        nonEmptyText
          .transform[List[RelayGame.Slice]](RelayGame.Slices.parse, RelayGame.Slices.show)
    )(Data.apply)(unapply)

  def create(trs: RelayTour.WithRounds)(using Me) = Form(
    roundMapping
      .verifying(
        s"Maximum rounds per tournament: ${RelayTour.maxRelays}",
        _ => trs.rounds.sizeIs < RelayTour.maxRelays
      )
  ).fill(fillFromPrevRounds(trs.rounds))

  def edit(r: RelayRound)(using Me) = Form(
    roundMapping
      .verifying(
        "The round source cannot be itself",
        d => d.syncSource.forall(_ != "url") || d.syncUrl.forall(_.roundId.forall(_ != r.id))
      )
  ).fill(Data.make(r))

object RelayRoundForm:

  val sourceTypes = List(
    "push" -> "Broadcaster App",
    "url"  -> "Single PGN URL",
    "urls" -> "Combine several PGN URLs",
    "ids"  -> "Lichess game IDs"
  )

  private val roundNumberRegex = """([^\d]*)(\d{1,2})([^\d]*)""".r
  val roundNumberIn: String => Option[Int] =
    case roundNumberRegex(_, n, _) => n.toIntOption
    case _                         => none

  def fillFromPrevRounds(rounds: List[RelayRound]): Data =
    val prevs: Option[(RelayRound, RelayRound)] = rounds.reverse match
      case a :: b :: _ => (a, b).some
      case _           => none
    val prev: Option[RelayRound] = rounds.lastOption
    def replaceRoundNumber(s: String, n: Int): String =
      roundNumberRegex.replaceAllIn(s, m => s"${m.group(1)}${n}${m.group(3)}")
    val prevNumber: Option[Int] = prev.flatMap(p => roundNumberIn(p.name.value))
    val nextNumber              = (prevNumber | rounds.size) + 1
    val guessName = for
      n <- prevNumber
      if prevs
        .map(_._2)
        .forall: old =>
          roundNumberIn(old.name.value).contains(n - 1)
      p <- prev
    yield replaceRoundNumber(p.name.value, nextNumber)
    val guessStartsAtTime = for
      (prev, old) <- prevs
      prevDate    <- prev.startsAtTime
      oldDate     <- old.startsAtTime
      delta = prevDate.toEpochMilli - oldDate.toEpochMilli
    yield prevDate.plusMillis(delta)
    val nextUrl: Option[URL] = for
      p  <- prev
      up <- p.sync.upstream
      lcc     = up.lcc.filter(lcc => prevNumber.contains(lcc.round))
      nextLcc = lcc.map(_.copy(round = nextNumber).pageUrl)
      next <- nextLcc.orElse(up.asUrl)
    yield next
    Data(
      name = RelayRound.Name(guessName | s"Round ${nextNumber}"),
      caption = prev.flatMap(_.caption),
      syncSource = prev.map(Data.make).flatMap(_.syncSource),
      syncUrl = nextUrl.map(Upstream.Url.apply),
      startsAt = guessStartsAtTime,
      startsAfterPrevious = prev.exists(_.startsAfterPrevious).option(true),
      period = prev.flatMap(_.sync.period),
      delay = prev.flatMap(_.sync.delay),
      onlyRound = prev.flatMap(_.sync.onlyRound).map(_ + 1),
      slices = prev.flatMap(_.sync.slices)
    )

  case class GameIds(ids: List[GameId])

  private def cleanUrl(source: String)(using mode: Mode): Option[URL] =
    for
      url <- Try(URL.parse(source)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      // prevent common mistakes (not for security)
      if mode.notProd || !blocklist.exists(subdomain(host, _))
      if !subdomain(host, "chess.com") || url.toString.startsWith("https://api.chess.com/pub")
    yield url

  private def validateUpstreamUrl(s: String)(using Mode): Either[String, URL] = for
    url <- cleanUrl(s).toRight("Invalid source URL")
    url <- if !validSourcePort(url) then Left("The source URL cannot specify a port") else Right(url)
  yield url

  private val validPorts                                           = Set(-1, 80, 443, 8080, 8491)
  private def validSourcePort(url: URL)(using mode: Mode): Boolean = mode.notProd || validPorts(url.port)

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
      syncSource: Option[String],
      syncUrl: Option[Upstream.Url] = None,
      syncUrls: Option[Upstream.Urls] = None,
      syncIds: Option[Upstream.Ids] = None,
      startsAt: Option[Instant] = None,
      startsAfterPrevious: Option[Boolean] = None,
      finished: Option[Boolean] = None,
      period: Option[Seconds] = None,
      delay: Option[Seconds] = None,
      onlyRound: Option[Int] = None,
      slices: Option[List[RelayGame.Slice]] = None
  ):
    def upstream: Option[Upstream] = syncSource.match
      case None         => syncUrl.orElse(syncUrls).orElse(syncIds)
      case Some("url")  => syncUrl
      case Some("urls") => syncUrls
      case Some("ids")  => syncIds
      case _            => None

    private def relayStartsAt: Option[RelayRound.Starts] = startsAt
      .map(RelayRound.Starts.At(_))
      .orElse((~startsAfterPrevious).option(RelayRound.Starts.AfterPrevious))

    def update(official: Boolean)(relay: RelayRound)(using me: Me)(using mode: Mode) =
      val sync = makeSync(me)
      relay.copy(
        name = name,
        caption = caption,
        sync = if relay.sync.playing then sync.play(official) else sync,
        startsAt = relayStartsAt,
        finished = ~finished
      )

    private def makeSync(user: User)(using mode: Mode): Sync =
      RelayRound.Sync(
        upstream = upstream,
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
        startsAt = relayStartsAt,
        startedAt = none
      )

  object Data:

    def make(relay: RelayRound) =
      Data(
        name = relay.name,
        caption = relay.caption,
        syncSource = relay.sync.upstream
          .fold("push"):
            case _: Upstream.Url  => "url"
            case _: Upstream.Urls => "urls"
            case _: Upstream.Ids  => "ids"
          .some,
        syncUrl = relay.sync.upstream.collect:
          case url: Upstream.Url => url,
        syncUrls = relay.sync.upstream.collect:
          case url: Upstream.Url   => Upstream.Urls(List(url.url))
          case urls: Upstream.Urls => urls,
        syncIds = relay.sync.upstream.collect:
          case ids: Upstream.Ids => ids,
        startsAt = relay.startsAtTime,
        startsAfterPrevious = relay.startsAfterPrevious.option(true),
        finished = relay.finished.option(true),
        period = relay.sync.period,
        onlyRound = relay.sync.onlyRound,
        slices = relay.sync.slices,
        delay = relay.sync.delay
      )
