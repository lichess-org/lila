package lila.relay

import io.mola.galimatias.URL
import play.api.Mode
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter
import scalalib.model.Seconds
import lila.common.Form.{ cleanText, formatter, into, stringIn, LocalDateTimeOrTimestamp }
import lila.core.perm.Granter
import lila.relay.RelayRound.Sync
import lila.relay.RelayRound.Sync.Upstream
import lila.relay.RelayRound.Sync.url.*
import lila.common.Form.PrettyDateTime

final class RelayRoundForm(using mode: Mode):

  import RelayRoundForm.*
  import lila.common.Form.ISOInstantOrTimestamp

  private given (using Me): Formatter[Upstream.Url] =
    formatter.stringTryFormatter(str => validateUpstreamUrl(str).map(Upstream.Url.apply), _.url.toString)

  private given (using Me): Formatter[Upstream.Urls] = formatter.stringTryFormatter(
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
      .filterOrElse(
        _.sizeIs <= RelayFetch.maxChaptersToShow.value,
        s"Max games: ${RelayFetch.maxChaptersToShow}"
      )
      .map(_.distinct)
      .map(Upstream.Ids.apply),
    _.ids.mkString(" ")
  )

  def roundMapping(tour: RelayTour)(using Me): Mapping[Data] =
    mapping(
      "name"                -> cleanText(minLength = 3, maxLength = 80).into[RelayRound.Name],
      "caption"             -> optional(cleanText(minLength = 3, maxLength = 80).into[RelayRound.Caption]),
      "syncSource"          -> optional(stringIn(sourceTypes.map(_._1).toSet)),
      "syncUrl"             -> optional(of[Upstream.Url]),
      "syncUrls"            -> optional(of[Upstream.Urls]),
      "syncIds"             -> optional(of[Upstream.Ids]),
      "startsAt"            -> optional(LocalDateTimeOrTimestamp(tour.info.timeZoneOrDefault).mapping),
      "startsAfterPrevious" -> optional(boolean),
      "finished"            -> optional(boolean),
      "period"              -> optional(number(min = 2, max = 60).into[Seconds]),
      "delay"               -> optional(number(min = 0, max = RelayDelay.maxSeconds.value).into[Seconds]),
      "onlyRound"           -> optional(number(min = 1, max = 999)),
      "slices" -> optional:
        nonEmptyText.transform[List[RelayGame.Slice]](RelayGame.Slices.parse, RelayGame.Slices.show)
    )(Data.apply)(unapply)

  def create(trs: RelayTour.WithRounds)(using Me) = Form(
    roundMapping(trs.tour)
      .verifying(
        s"Maximum rounds per tournament: ${RelayTour.maxRelays}",
        _ => trs.rounds.sizeIs < RelayTour.maxRelays.value
      )
  ).fill(fillFromPrevRounds(trs.rounds))

  def edit(t: RelayTour, r: RelayRound)(using Me) = Form(
    roundMapping(t)
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
      url <- lila.common.url.parse(source).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      // prevent common mistakes (not for security)
      if mode.notProd || !blocklist.exists(subdomain(host, _))
      if !subdomain(host, "chess.com") || url.toString.startsWith("https://api.chess.com/pub")
    yield url

  private def validateUpstreamUrl(s: String)(using Me, Mode): Either[String, URL] = for
    url <- cleanUrl(s).toRight("Invalid source URL")
    url <- if !validSourcePort(url) then Left("The source URL cannot specify a port") else Right(url)
    url <-
      if url.looksLikeLcc && !url.lcc.isDefined
      then Left("LCC URLs must end with /{round-number}, e.g. /5 for round 5")
      else Right(url)
    url <-
      if url.host.toString.endsWith("lichess.org") && !Granter(_.Relay)
      then Left("Invalid source URL")
      else Right(url)
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

    def update(official: Boolean)(relay: RelayRound)(using Me, Mode) =
      val sync = makeSync(relay.sync.some)
      relay.copy(
        name = name,
        caption = if Granter(_.StudyAdmin) then caption else relay.caption,
        sync = if relay.sync.playing then sync.play(official) else sync,
        startsAt = relayStartsAt,
        finishedAt = finished.orZero.option(relay.finishedAt.|(nowInstant))
      )

    private def makeSync(prev: Option[RelayRound.Sync])(using Me): Sync =
      RelayRound.Sync(
        upstream = upstream,
        until = none,
        nextAt = none,
        period = if Granter(_.StudyAdmin) then period else prev.flatMap(_.period),
        delay = delay,
        onlyRound = onlyRound,
        slices = slices,
        log = SyncLog.empty
      )

    def make(tour: RelayTour)(using Me, Mode) =
      RelayRound(
        id = RelayRound.makeId,
        tourId = tour.id,
        name = name,
        caption = Granter(_.StudyAdmin).so(caption),
        sync = makeSync(none),
        createdAt = nowInstant,
        crowd = none,
        finishedAt = (~finished).option(nowInstant),
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
        finished = relay.isFinished.option(true),
        period = relay.sync.period,
        onlyRound = relay.sync.onlyRound,
        slices = relay.sync.slices,
        delay = relay.sync.delay
      )
