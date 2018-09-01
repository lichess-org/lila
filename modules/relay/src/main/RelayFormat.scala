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

  def get(url: String): Fu[Option[RelayFormat]] = cache get Url.parse(url.pp).pp

  private def guessFormat(url: Url): Fu[Option[RelayFormat]] = {

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
      )(looksLikeJson) map2 { (index: Url) =>
          ManyFiles(index, i => pgnDoc(replaceLastPart(index, s"game-$i.pgn")))
        }

    guessSingleFile orElse guessManyFiles
  } thenPp

  private val cache = asyncCache.multi[Url, Option[RelayFormat]](
    name = "relayFormat",
    f = guessFormat,
    expireAfter = _.ExpireAfterAccess(1 hour)
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

  case class ManyFiles(jsonIndex: Url, game: Int => Doc) extends RelayFormat

  def httpGet(url: Url): Fu[Option[String]] =
    WS.url(url.toString.pp("httpGet")).withRequestTimeout(4.seconds.toMillis).get().map {
      case res if res.status == 200 => res.body.some
      case _ => none
    }

  def looksLikePgn(body: String): Boolean = MultiPgn.split(body, 1).value.headOption ?? { pgn =>
    lila.study.PgnImport(pgn, Nil).isSuccess
  }
  def looksLikePgn(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikePgn }

  def looksLikeJson(body: String): Boolean = Json.parse(body) != JsNull
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
