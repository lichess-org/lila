package lila.relay

import scala.util.Try
import io.mola.galimatias.URL
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter
import play.api.Mode
import scalalib.model.Seconds

import lila.common.Form.{ cleanText, into, stringIn, formatter }
import lila.core.perm.Granter

import lila.relay.RelayRound.Sync
import lila.relay.RelayRound.Sync.UpstreamUrl

final class RelayRoundForm(using mode: Mode):

  import RelayRoundForm.*
  import lila.common.Form.ISOInstantOrTimestamp

  private given Formatter[Sync.UpstreamUrl] = formatter.stringTryFormatter(validateUpstreamUrl, _.fetchUrl)
  private given Formatter[Sync.UpstreamUrls] = formatter.stringTryFormatter(
    _.linesIterator.toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .traverse(validateUpstreamUrlOrLcc)
      .map(_.distinct)
      .map(Sync.UpstreamUrls.apply),
    _.urls.map(_.formUrl).mkString("\n")
  )
  private given Formatter[Sync.UpstreamIds] = formatter.stringTryFormatter(
    _.split(' ').toList
      .map(_.trim)
      .traverse: i =>
        GameId.from(i.trim).toRight(s"Invalid game ID: $i")
      .left
      .map(_.mkString(", "))
      .filterOrElse(_.sizeIs <= RelayFetch.maxChapters.value, s"Max games: ${RelayFetch.maxChapters}")
      .map(_.distinct)
      .map(Sync.UpstreamIds.apply),
    _.ids.mkString(" ")
  )

  val lccMapping = mapping(
    "id"    -> cleanText(minLength = 10, maxLength = 40),
    "round" -> number(min = 1, max = 999)
  )(Sync.UpstreamLcc.apply)(unapply)

  val roundMapping =
    mapping(
      "name"       -> cleanText(minLength = 3, maxLength = 80).into[RelayRound.Name],
      "caption"    -> optional(cleanText(minLength = 3, maxLength = 80).into[RelayRound.Caption]),
      "syncSource" -> optional(stringIn(sourceTypes.map(_._1).toSet)),
      "syncUrl"    -> optional(of[Sync.UpstreamUrl]),
      "syncUrls"   -> optional(of[Sync.UpstreamUrls]),
      "syncLcc"    -> optional(lccMapping),
      "syncIds"    -> optional(of[Sync.UpstreamIds]),
      "startsAt"   -> optional(ISOInstantOrTimestamp.mapping),
      "finished"   -> optional(boolean),
      "period"     -> optional(number(min = 2, max = 60).into[Seconds]),
      "delay"      -> optional(number(min = 0, max = RelayDelay.maxSeconds.value).into[Seconds]),
      "onlyRound"  -> optional(number(min = 1, max = 999)),
      "slices" -> optional:
        nonEmptyText
          .transform[List[RelayGame.Slice]](RelayGame.Slices.parse, RelayGame.Slices.show)
    )(Data.apply)(unapply)

  def create(trs: RelayTour.WithRounds) = Form(
    roundMapping
      .verifying(
        s"Maximum rounds per tournament: ${RelayTour.maxRelays}",
        _ => trs.rounds.sizeIs < RelayTour.maxRelays
      )
  ).fill(fillFromPrevRounds(trs.rounds))

  def edit(r: RelayRound) = Form(roundMapping).fill(Data.make(r))

object RelayRoundForm:

  val sourceTypes = List(
    "url"  -> "Single PGN URL",
    "urls" -> "Combine several PGN URLs",
    "lcc"  -> "LiveChessCloud page",
    "ids"  -> "Lichess game IDs",
    "push" -> "Push local games"
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
        .fold(true): old =>
          roundNumberIn(old.name.value).contains(n - 1)
      p <- prev
    yield replaceRoundNumber(p.name.value, nextNumber)
    val nextLcc: Option[Sync.UpstreamLcc] = prev
      .flatMap(_.sync.upstream)
      .flatMap:
        case lcc: Sync.UpstreamLcc => lcc.copy(round = nextNumber).some
        case _                     => none
    val guessDate = for
      (prev, old) <- prevs
      prevDate    <- prev.startsAt
      oldDate     <- old.startsAt
      delta = prevDate.toEpochMilli - oldDate.toEpochMilli
    yield prevDate.plusMillis(delta)
    Data(
      name = RelayRound.Name(guessName | s"Round ${nextNumber}"),
      caption = prev.flatMap(_.caption),
      syncSource = prev.map(Data.make).flatMap(_.syncSource),
      syncLcc = nextLcc,
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

  private def cleanUrl(source: String)(using mode: Mode): Option[String] =
    for
      url <- Try(URL.parse(source)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      // prevent common mistakes (not for security)
      if mode.notProd || !blocklist.exists(subdomain(host, _))
      if !subdomain(host, "chess.com") || url.toString.startsWith("https://api.chess.com/pub")
    yield url.toString.stripSuffix("/")

  private def validateUpstreamUrlOrLcc(s: String)(using Mode): Either[String, Sync.FetchableUpstream] =
    Sync.UpstreamLcc.find(s) match
      case Some(lcc) => Right(lcc)
      case None      => validateUpstreamUrl(s)

  private def validateUpstreamUrl(s: String)(using Mode): Either[String, Sync.UpstreamUrl] = for
    url <- cleanUrl(s).toRight("Invalid source URL")
    url <- if !validSourcePort(url) then Left("The source URL cannot specify a port") else Right(url)
  yield Sync.UpstreamUrl(url)

  private def cleanUrls(source: String)(using mode: Mode): Option[List[String]] =
    source.linesIterator.toList.flatMap(cleanUrl).some.filter(_.nonEmpty)

  private val validPorts = Set(-1, 80, 443, 8080, 8491)
  private def validSourcePort(source: String)(using mode: Mode): Boolean =
    mode.notProd ||
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
      syncSource: Option[String],
      syncUrl: Option[Sync.UpstreamUrl] = None,
      syncUrls: Option[Sync.UpstreamUrls] = None,
      syncLcc: Option[Sync.UpstreamLcc] = None,
      syncIds: Option[Sync.UpstreamIds] = None,
      startsAt: Option[Instant] = None,
      finished: Option[Boolean] = None,
      period: Option[Seconds] = None,
      delay: Option[Seconds] = None,
      onlyRound: Option[Int] = None,
      slices: Option[List[RelayGame.Slice]] = None
  ):
    def upstream: Option[Sync.Upstream] = syncSource
      .match
        case None         => syncUrl.orElse(syncUrls).orElse(syncIds)
        case Some("url")  => syncUrl
        case Some("urls") => syncUrls
        case Some("lcc")  => syncLcc
        case Some("ids")  => syncIds
        case _            => None
      .map:
        case url: Sync.UpstreamUrl =>
          val foundLcc = for
            lccId <- Sync.UpstreamLcc.findId(url)
            round <- roundNumberIn(name.value)
          yield Sync.UpstreamLcc(lccId, round)
          foundLcc | url
        case up => up

    def update(official: Boolean)(relay: RelayRound)(using me: Me)(using mode: Mode) =
      val sync = makeSync(me)
      relay.copy(
        name = name,
        caption = caption,
        sync = if relay.sync.playing then sync.play(official) else sync,
        startsAt = startsAt,
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
        startsAt = startsAt,
        startedAt = none
      )

  object Data:

    def make(relay: RelayRound) =
      Data(
        name = relay.name,
        caption = relay.caption,
        syncSource = relay.sync.upstream
          .fold("push"):
            case _: Sync.UpstreamUrl  => "url"
            case _: Sync.UpstreamUrls => "urls"
            case _: Sync.UpstreamLcc  => "lcc"
            case _: Sync.UpstreamIds  => "ids"
          .some,
        syncUrl = relay.sync.upstream.collect:
          case url: Sync.UpstreamUrl => url,
        syncUrls = relay.sync.upstream.collect:
          case url: Sync.UpstreamUrl   => Sync.UpstreamUrls(List(url))
          case urls: Sync.UpstreamUrls => urls,
        syncLcc = relay.sync.upstream.collect:
          case lcc: Sync.UpstreamLcc => lcc,
        syncIds = relay.sync.upstream.collect:
          case ids: Sync.UpstreamIds => ids,
        startsAt = relay.startsAt,
        finished = relay.finished.option(true),
        period = relay.sync.period,
        onlyRound = relay.sync.onlyRound,
        slices = relay.sync.slices,
        delay = relay.sync.delay
      )
