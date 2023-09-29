package lila.relay

import io.mola.galimatias.URL
import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import chess.format.pgn.PgnStr

import lila.study.MultiPgn
import lila.memo.CacheApi
import lila.memo.CacheApi.*

final private class RelayFormatApi(ws: StandaloneWSClient, cacheApi: CacheApi)(using Executor):

  import RelayFormat.*
  import RelayRound.Sync.UpstreamUrl

  private val cache = cacheApi[UpstreamUrl.WithRound, RelayFormat](8, "relay.format"):
    _.refreshAfterWrite(10 minutes).expireAfterAccess(20 minutes).buildAsyncFuture(guessFormat)

  def get(upstream: UpstreamUrl.WithRound): Fu[RelayFormat] = cache get upstream

  def refresh(upstream: UpstreamUrl.WithRound): Unit = cache invalidate upstream

  private def guessFormat(upstream: UpstreamUrl.WithRound): Fu[RelayFormat] = {

    val originalUrl = URL parse upstream.url

    // http://view.livechesscloud.com/ed5fb586-f549-4029-a470-d590f8e30c76
    def guessLcc(url: URL): Fu[Option[RelayFormat]] =
      url.toString match
        case UpstreamUrl.LccRegex(id) =>
          guessManyFiles:
            URL.parse:
              s"http://1.pool.livechesscloud.com/get/$id/round-${upstream.round | 1}/index.json"
        case _ => fuccess(none)

    def guessSingleFile(url: URL): Fu[Option[RelayFormat]] =
      List(
        url.some,
        !url.pathSegments.contains(mostCommonSingleFileName) option addPart(url, mostCommonSingleFileName)
      ).flatten.distinct.findM(looksLikePgn) dmap2 { (u: URL) =>
        SingleFile(pgnDoc(u))
      }

    def guessManyFiles(url: URL): Fu[Option[RelayFormat]] =
      (List(url) ::: mostCommonIndexNames
        .filterNot(url.pathSegments.contains)
        .map(addPart(url, _)))
        .findM(looksLikeJson)
        .flatMapz: index =>
          val jsonUrl = (n: Int) => jsonDoc(replaceLastPart(index, s"game-$n.json"))
          val pgnUrl  = (n: Int) => pgnDoc(replaceLastPart(index, s"game-$n.pgn"))
          looksLikeJson(jsonUrl(1).url).map(_ option jsonUrl) orElse
            looksLikePgn(pgnUrl(1).url).map(_ option pgnUrl) dmap2 {
              ManyFiles(index, _)
            }

    guessLcc(originalUrl) orElse
      guessSingleFile(originalUrl) orElse
      guessManyFiles(originalUrl) orFail "No games found, check your source URL"
  } addEffect { format =>
    logger.info(s"guessed format of $upstream: $format")
  }

  private[relay] def httpGet(url: URL): Fu[String] =
    ws.url(url.toString)
      .withRequestTimeout(4.seconds)
      .withFollowRedirects(false)
      .get()
      .flatMap: res =>
        if res.status == 200 then fuccess(res.body)
        else fufail(s"[${res.status}] $url")
      .monSuccess(_.relay.httpGet(url.host.toString))

  private def looksLikePgn(body: String): Boolean =
    MultiPgn.split(PgnStr(body), 1).value.headOption so { pgn =>
      lila.study.PgnImport(pgn, Nil).isRight
    }
  private def looksLikePgn(url: URL): Fu[Boolean] = httpGet(url) map looksLikePgn

  private def looksLikeJson(body: String): Boolean =
    try Json.parse(body) != JsNull
    catch case _: Exception => false
  private def looksLikeJson(url: URL): Fu[Boolean] = httpGet(url) map looksLikeJson

sealed private trait RelayFormat

private object RelayFormat:

  enum DocFormat:
    case Json, Pgn

  case class Doc(url: URL, format: DocFormat)

  def jsonDoc(url: URL) = Doc(url, DocFormat.Json)
  def pgnDoc(url: URL)  = Doc(url, DocFormat.Pgn)

  case class SingleFile(doc: Doc) extends RelayFormat

  type GameNumberToDoc = Int => Doc

  case class ManyFiles(jsonIndex: URL, game: GameNumberToDoc) extends RelayFormat:
    override def toString = s"Manyfiles($jsonIndex, ${game(0)})"

  def addPart(url: URL, part: String)             = url.withPath(s"${url.path}/$part")
  def replaceLastPart(url: URL, withPart: String) = url.withPath(s"${url.path}/../$withPart")

  val mostCommonSingleFileName = "games.pgn"
  val mostCommonIndexNames     = List("round.json", "index.json")
