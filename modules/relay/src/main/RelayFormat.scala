package lidraughts.relay

import io.lemonlabs.uri._
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import lidraughts.memo.AsyncCache
import lidraughts.study.MultiPdn

private final class RelayFormatApi(
    asyncCache: AsyncCache.Builder
) {

  import RelayFormat._

  def get(url: String): Fu[RelayFormat] = cache get Url.parse(url)

  def refresh(url: Url): Unit = cache refresh url

  private def guessFormat(url: Url): Fu[RelayFormat] = {

    def guessSingleJson: Fu[Option[RelayFormat]] =
      lidraughts.common.Future.find(List(url))(looksLikeJson) map2 { (u: Url) =>
        SingleFile(jsonDoc(u))
      }
    def guessSinglePdn: Fu[Option[RelayFormat]] =
      lidraughts.common.Future.find(List(url))(looksLikePdn) map2 { (u: Url) =>
        SingleFile(pdnDoc(u))
      }

    guessSinglePdn orElse guessSingleJson flatten "Cannot find any DGT compatible files"
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
    case object Pdn extends DocFormat
  }

  case class Doc(url: Url, format: DocFormat)

  def jsonDoc(url: Url) = Doc(url, DocFormat.Json)
  def pdnDoc(url: Url) = Doc(url, DocFormat.Pdn)

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

  def looksLikePdn(body: String): Boolean = MultiPdn.split(body.replace("<br>", "\n\n"), 1).value.headOption ?? { pdn =>
    lidraughts.study.PdnImport(pdn, Nil, lidraughts.pref.Pref.default.draughtsResult).isSuccess
  }
  def looksLikePdn(url: Url): Fu[Boolean] = httpGet(url).map { _ exists looksLikePdn }

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

  val mostCommonSingleFileName = "games.pdn"
  val mostCommonIndexNames = List("round.json", "index.json")
}
