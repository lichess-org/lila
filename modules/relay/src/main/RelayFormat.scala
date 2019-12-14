package lila.relay

import io.lemonlabs.uri._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.memo.AsyncCache
import lila.study.MultiPgn

final private class RelayFormatApi(
    ws: WSClient,
    asyncCache: AsyncCache.Builder
)(implicit ec: scala.concurrent.ExecutionContext) {

  import RelayFormat._
  import Relay.Sync.UpstreamWithRound

  def get(upstream: UpstreamWithRound): Fu[RelayFormat] = cache get upstream

  def refresh(upstream: UpstreamWithRound): Unit = cache refresh upstream

  private def guessFormat(upstream: UpstreamWithRound): Fu[RelayFormat] = {

    val originalUrl = Url parse upstream.url

    // http://view.livechesscloud.com/ed5fb586-f549-4029-a470-d590f8e30c76
    def guessLcc(url: Url): Fu[Option[RelayFormat]] = url.toString match {
      case Relay.Sync.LccRegex(id) =>
        guessManyFiles(
          Url.parse(
            s"http://1.pool.livechesscloud.com/get/$id/round-${upstream.round | 1}/index.json"
          )
        )
      case _ => fuccess(none)
    }

    def guessSingleFile(url: Url): Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(
          url.some,
          !url.path.parts.contains(mostCommonSingleFileName) option addPart(url, mostCommonSingleFileName)
        ).flatten.distinct
      )(looksLikePgn) dmap2 { (u: Url) =>
        SingleFile(pgnDoc(u))
      }

    def guessManyFiles(url: Url): Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(url) ::: mostCommonIndexNames.filterNot(url.path.parts.contains).map(addPart(url, _))
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
      guessManyFiles(originalUrl) orFail "Cannot find any DGT compatible files"
  } addEffect { format =>
    logger.info(s"guessed format of $upstream: $format")
  } addFailureEffect { err =>
    logger.info(s"can't guess format of $upstream: $err")
  }

  private def httpGet(url: Url): Fu[Option[String]] =
    ws.url(url.toString)
      .withRequestTimeout(4.seconds)
      .get()
      .map {
        case res if res.status == 200 => res.body.some
        case _                        => none
      }

  private def looksLikePgn(body: String): Boolean = MultiPgn.split(body, 1).value.headOption ?? { pgn =>
    lila.study.PgnImport(pgn, Nil).isSuccess
  }
  private def looksLikePgn(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikePgn }

  private def looksLikeJson(body: String): Boolean =
    try {
      Json.parse(body) != JsNull
    } catch {
      case _: Exception => false
    }
  private def looksLikeJson(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikeJson }

  private val cache = asyncCache.multi[UpstreamWithRound, RelayFormat](
    name = "relayFormat",
    f = guessFormat,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )
}

sealed private trait RelayFormat

private object RelayFormat {

  sealed trait DocFormat
  object DocFormat {
    case object Json extends DocFormat
    case object Pgn  extends DocFormat
  }

  case class Doc(url: Url, format: DocFormat)

  def jsonDoc(url: Url) = Doc(url, DocFormat.Json)
  def pgnDoc(url: Url)  = Doc(url, DocFormat.Pgn)

  case class SingleFile(doc: Doc) extends RelayFormat

  type GameNumberToDoc = Int => Doc

  case class ManyFiles(jsonIndex: Url, game: GameNumberToDoc) extends RelayFormat {
    override def toString = s"Manyfiles($jsonIndex, ${game(0)})"
  }

  def addPart(url: Url, part: String) = url.withPath(url.path addPart part)
  def replaceLastPart(url: Url, withPart: String) =
    if (url.path.isEmpty) addPart(url, withPart)
    else
      url.withPath {
        url.path.withParts {
          url.path.parts.init :+ withPart
        }
      }

  val mostCommonSingleFileName = "games.pgn"
  val mostCommonIndexNames     = List("round.json", "index.json")
}
