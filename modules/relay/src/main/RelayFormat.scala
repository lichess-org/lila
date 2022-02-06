package lila.relay

import io.lemonlabs.uri._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.study.MultiNotation
import lila.memo.CacheApi
import lila.memo.CacheApi._

// todo
final private class RelayFormatApi(ws: WSClient, cacheApi: CacheApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import RelayFormat._
  import Relay.Sync.Upstream

  private val cache = cacheApi[Upstream, RelayFormat](8, "relay.format") {
    _.refreshAfterWrite(10 minutes)
      .expireAfterAccess(20 minutes)
      .buildAsyncFuture(guessFormat)
  }

  def get(upstream: Upstream): Fu[RelayFormat] = cache get upstream

  def refresh(upstream: Upstream): Unit = cache invalidate upstream

  private def guessFormat(upstream: Upstream): Fu[RelayFormat] = {

    val originalUrl = Url parse upstream.url

    def guessSingleFile(url: Url): Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(
          url.some
        ).flatten.distinct
      )(looksLikeNotation) dmap2 { (u: Url) =>
        SingleFile(notationDoc(u))
      }

    def guessManyFiles(url: Url): Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(url)
      )(looksLikeJson) flatMap {
        _ ?? { index =>
          val jsonUrl     = (n: Int) => jsonDoc(replaceLastPart(index, s"game-$n.json"))
          val notationUrl = (n: Int) => notationDoc(replaceLastPart(index, s"game-$n.kif"))
          looksLikeJson(jsonUrl(1).url).map(_ option jsonUrl) orElse
            looksLikeNotation(notationUrl(1).url).map(_ option notationUrl) dmap2 {
              ManyFiles(index, _)
            }
        }
      }

    guessSingleFile(originalUrl) orElse
      guessManyFiles(originalUrl) orFail "No games found, check your source URL"
  } addEffect { format =>
    logger.info(s"guessed format of $upstream: $format")
  }

  private def httpGet(url: Url): Fu[Option[String]] =
    ws.url(url.toString)
      .withRequestTimeout(4.seconds)
      .get()
      .map {
        case res if res.status == 200 => res.body.some
        case _                        => none
      }

  private def looksLikeNotation(body: String): Boolean =
    MultiNotation.split(body, 1).value.headOption ?? { notation =>
      lila.study.NotationImport(notation, Nil).isValid
    }
  private def looksLikeNotation(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikeNotation }

  private def looksLikeJson(body: String): Boolean =
    try {
      Json.parse(body) != JsNull
    } catch {
      case _: Exception => false
    }
  private def looksLikeJson(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikeJson }
}

sealed private trait RelayFormat

private object RelayFormat {

  sealed trait DocFormat
  object DocFormat {
    case object Json     extends DocFormat
    case object Notation extends DocFormat
  }

  case class Doc(url: Url, format: DocFormat)

  def jsonDoc(url: Url)     = Doc(url, DocFormat.Json)
  def notationDoc(url: Url) = Doc(url, DocFormat.Notation)

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
}
