package lila.relay

import io.mola.galimatias.URL
import play.api.libs.json._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.study.MultiPgn
import lila.memo.CacheApi
import lila.memo.CacheApi._

final private class RelayFormatApi(ws: StandaloneWSClient, cacheApi: CacheApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import RelayFormat._
  import RelayRound.Sync.UpstreamUrl

  private val cache = cacheApi[UpstreamUrl.WithRound, RelayFormat](8, "relay.format") {
    _.refreshAfterWrite(10 minutes)
      .expireAfterAccess(20 minutes)
      .buildAsyncFuture(guessFormat)
  }

  def get(upstream: UpstreamUrl.WithRound): Fu[RelayFormat] = cache get upstream

  def refresh(upstream: UpstreamUrl.WithRound): Unit = cache invalidate upstream

  private def guessFormat(upstream: UpstreamUrl.WithRound): Fu[RelayFormat] = {

    val originalUrl = URL parse upstream.url

    // http://view.livechesscloud.com/ed5fb586-f549-4029-a470-d590f8e30c76
    def guessLcc(url: URL): Fu[Option[RelayFormat]] =
      url.toString match {
        case UpstreamUrl.LccRegex(id) =>
          guessManyFiles(
            URL.parse(
              s"http://1.pool.livechesscloud.com/get/$id/round-${upstream.round | 1}/index.json"
            )
          )
        case _ => fuccess(none)
      }

    def guessSingleFile(url: URL): Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(
          url.some,
          !url.pathSegments.contains(mostCommonSingleFileName) option addPart(url, mostCommonSingleFileName)
        ).flatten.distinct
      )(looksLikePgn) dmap2 { (u: URL) =>
        SingleFile(pgnDoc(u))
      }

    def guessManyFiles(url: URL): Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(url) ::: mostCommonIndexNames.filterNot(url.pathSegments.contains).map(addPart(url, _))
      )(looksLikeJson) flatMap {
        _ ?? { index =>
          val jsonUrl = (n: Int) => jsonDoc(replaceLastPart(index, s"game-$n.json"))
          val pgnUrl  = (n: Int) => pgnDoc(replaceLastPart(index, s"game-$n.pgn"))
          looksLikeJson(jsonUrl(1).url).map(_ option jsonUrl) orElse
            looksLikePgn(pgnUrl(1).url).map(_ option pgnUrl) dmap2 {
              ManyFiles(index, _)
            }
        }
      }

    guessLcc(originalUrl) orElse
      guessSingleFile(originalUrl) orElse
      guessManyFiles(originalUrl) orFail "No games found, check your source URL"
  } addEffect { format =>
    logger.info(s"guessed format of $upstream: $format")
  }

  private def httpGet(url: URL): Fu[Option[String]] =
    ws.url(url.toString)
      .withRequestTimeout(4.seconds)
      .get()
      .map {
        case res if res.status == 200 => res.body.some
        case _                        => none
      }

  private def looksLikePgn(body: String): Boolean =
    MultiPgn.split(body, 1).value.headOption ?? { pgn =>
      lila.study.PgnImport(pgn, Nil).isValid
    }
  private def looksLikePgn(url: URL): Fu[Boolean] = httpGet(url).map { _ exists looksLikePgn }

  private def looksLikeJson(body: String): Boolean =
    try {
      Json.parse(body) != JsNull
    } catch {
      case _: Exception => false
    }
  private def looksLikeJson(url: URL): Fu[Boolean] = httpGet(url).map { _ exists looksLikeJson }
}

sealed private trait RelayFormat

private object RelayFormat {

  sealed trait DocFormat
  object DocFormat {
    case object Json extends DocFormat
    case object Pgn  extends DocFormat
  }

  case class Doc(url: URL, format: DocFormat)

  def jsonDoc(url: URL) = Doc(url, DocFormat.Json)
  def pgnDoc(url: URL)  = Doc(url, DocFormat.Pgn)

  case class SingleFile(doc: Doc) extends RelayFormat

  type GameNumberToDoc = Int => Doc

  case class ManyFiles(jsonIndex: URL, game: GameNumberToDoc) extends RelayFormat {
    override def toString = s"Manyfiles($jsonIndex, ${game(0)})"
  }

  def addPart(url: URL, part: String)             = url.withPath(s"${url.path}/$part")
  def replaceLastPart(url: URL, withPart: String) = url.withPath(s"${url.path}/../$withPart")

  val mostCommonSingleFileName = "games.pgn"
  val mostCommonIndexNames     = List("round.json", "index.json")
}
