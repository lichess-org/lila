package lila.relay

import io.lemonlabs.uri._
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import lila.memo.AsyncCache
import lila.study.MultiPgn

private final class RelayFormatApi(
    asyncCache: AsyncCache.Builder
) {

  import RelayFormat._

  def get(url: String): Fu[RelayFormat] = cache get Url.parse(url)

  def refresh(url: Url): Unit = cache refresh url

  private def guessFormat(url: Url): Fu[RelayFormat] = {

    def guessSingleFile: Fu[Option[RelayFormat]] =
      lila.common.Future.find(List(
        url.some,
        !url.path.parts.contains(mostCommonSingleFileName) option addPart(url, mostCommonSingleFileName)
      ).flatten.distinct)(looksLikePgn) map2 { (u: Url) =>
        SingleFile(pgnDoc(u))
      }

    def guessManyFiles: Fu[Option[RelayFormat]] =
      lila.common.Future.find(
        List(url) ::: mostCommonIndexNames.filterNot(url.path.parts.contains).map(addPart(url, _))
      )(looksLikeJson) flatMap {
          _ ?? { index =>
            val jsonUrl = (n: Int) => jsonDoc(replaceLastPart(index, s"game-$n.json"))
            val pgnUrl = (n: Int) => pgnDoc(replaceLastPart(index, s"game-$n.pgn"))
            looksLikeJson(jsonUrl(1).url).map(_ option jsonUrl) orElse
              looksLikePgn(pgnUrl(1).url).map(_ option pgnUrl) map2 { (gameUrl: GameNumberToDoc) =>
                ManyFiles(index, gameUrl)
              }
          }
        }

    guessSingleFile orElse guessManyFiles flatten "Cannot find any DGT compatible files"
  } addEffect { format =>
    logger.info(s"guessed format of $url: $format")
  } addFailureEffect { err =>
    logger.info(s"can't guess format of $url: $err")
  }

  private val cache = asyncCache.multi[Url, RelayFormat](
    name = "relayFormat",
    f = guessFormat,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )
}

private sealed trait RelayFormat

private object RelayFormat {

  sealed trait DocFormat
  object DocFormat {
    case object Json extends DocFormat
    case object Pgn extends DocFormat
  }

  case class Doc(url: Url, format: DocFormat)

  def jsonDoc(url: Url) = Doc(url, DocFormat.Json)
  def pgnDoc(url: Url) = Doc(url, DocFormat.Pgn)

  case class SingleFile(doc: Doc) extends RelayFormat

  type GameNumberToDoc = Int => Doc

  case class ManyFiles(jsonIndex: Url, game: GameNumberToDoc) extends RelayFormat {
    override def toString = s"Manyfiles($jsonIndex, ${game(0)})"
  }

  def httpGet(url: Url): Fu[Option[String]] =
    WS.url(url.toString).withRequestTimeout(4.seconds.toMillis).get().map {
      case res if res.status == 200 => res.body.some
      case _ => none
    }

  def looksLikePgn(body: String): Boolean = MultiPgn.split(body, 1).value.headOption ?? { pgn =>
    lila.study.PgnImport(pgn, Nil).isSuccess
  }
  def looksLikePgn(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikePgn }

  def looksLikeJson(body: String): Boolean = try {
    Json.parse(body) != JsNull
  } catch {
    case _: Exception => false
  }
  def looksLikeJson(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikeJson }

  def addPart(url: Url, part: String) = url.withPath(url.path addPart part)
  def replaceLastPart(url: Url, withPart: String) =
    if (url.path.isEmpty) addPart(url, withPart)
    else url.withPath {
      url.path.withParts {
        url.path.parts.init :+ withPart
      }
    }

  val mostCommonSingleFileName = "games.pgn"
  val mostCommonIndexNames = List("round.json", "index.json")
}
