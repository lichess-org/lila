package lila.video

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.Future

import lila.common.config._

final private[video] class Youtube(
    ws: StandaloneWSClient,
    url: String,
    apiKey: Secret,
    max: Max,
    api: VideoApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Youtube._

  implicit private val readSnippet        = Json.reads[Snippet]
  implicit private val readStatistics     = Json.reads[Statistics]
  implicit private val readContentDetails = Json.reads[ContentDetails]
  implicit private val readEntry          = Json.reads[Entry]
  implicit private val readEntries: Reads[Seq[Entry]] =
    (__ \ "items").read(Reads seq readEntry)

  def updateAll: Funit =
    fetch flatMap { entries =>
      Future
        .traverse(entries) { entry =>
          api.video
            .setMetadata(
              entry.id,
              Metadata(
                views = ~entry.statistics.viewCount.toIntOption,
                likes = ~entry.statistics.likeCount.toIntOption -
                  ~entry.statistics.dislikeCount.toIntOption,
                description = entry.snippet.description,
                duration = Some(entry.contentDetails.seconds),
                publishedAt = entry.snippet.publishedAt.flatMap { at =>
                  scala.util.Try { new DateTime(at) }.toOption
                }
              )
            )
            .recover {
              case e: Exception => logger.warn("update all youtube", e)
            }
        }
        .void
    }

  private def fetch: Fu[List[Entry]] =
    api.video.allIds flatMap { ids =>
      ws.url(url)
        .withQueryStringParameters(
          "id"   -> lila.common.ThreadLocalRandom.shuffle(ids).take(max.value).mkString(","),
          "part" -> "id,statistics,snippet,contentDetails",
          "key"  -> apiKey.value
        )
        .get() flatMap {
        case res if res.status == 200 =>
          readEntries reads res.body[JsValue] match {
            case JsError(err)          => fufail(err.toString)
            case JsSuccess(entries, _) => fuccess(entries.toList)
          }
        case res =>
          println(res.body)
          fufail(s"[video youtube] fetch ${res.status}")
      }
    }
}

object Youtube {

  def empty = Metadata(0, 0, None, None, None)

  case class Metadata(
      views: Int,
      likes: Int,
      description: Option[String],
      duration: Option[Int], // in seconds
      publishedAt: Option[DateTime]
  )

  private[video] case class Entry(
      id: String,
      snippet: Snippet,
      statistics: Statistics,
      contentDetails: ContentDetails
  )

  private[video] case class Snippet(
      description: Option[String],
      publishedAt: Option[String]
  )

  private[video] case class Statistics(
      viewCount: String,
      likeCount: String,
      dislikeCount: String
  )

  private val iso8601Formatter = org.joda.time.format.ISOPeriodFormat.standard()

  private[video] case class ContentDetails(duration: String) {
    def seconds: Int = iso8601Formatter.parsePeriod(duration).toStandardSeconds.getSeconds
  }
}
