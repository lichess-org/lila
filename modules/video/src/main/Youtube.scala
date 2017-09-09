package lila.video

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private[video] final class Youtube(
    url: String,
    apiKey: String,
    max: Int,
    api: VideoApi
) {

  import Youtube._

  private implicit val readSnippet = Json.reads[Snippet]
  private implicit val readStatistics = Json.reads[Statistics]
  private implicit val readContentDetails = Json.reads[ContentDetails]
  private implicit val readEntry = Json.reads[Entry]
  private implicit val readEntries: Reads[Seq[Entry]] =
    (__ \ "items").read(Reads seq readEntry)

  def updateAll: Funit = fetch flatMap { entries =>
    entries.map { entry =>
      api.video.setMetadata(entry.id, Metadata(
        views = ~parseIntOption(entry.statistics.viewCount),
        likes = ~parseIntOption(entry.statistics.likeCount) -
          ~parseIntOption(entry.statistics.dislikeCount),
        description = entry.snippet.description,
        duration = Some(entry.contentDetails.seconds),
        publishedAt = entry.snippet.publishedAt.flatMap { at =>
          scala.util.Try { new DateTime(at) }.toOption
        }
      )).recover {
        case e: Exception => logger.warn("update all youtube", e)
      }
    }.sequenceFu.void
  }

  private def fetch: Fu[List[Entry]] = api.video.allIds flatMap { ids =>
    WS.url(url).withQueryString(
      "id" -> scala.util.Random.shuffle(ids).take(max).mkString(","),
      "part" -> "id,statistics,snippet,contentDetails",
      "key" -> apiKey
    ).get() flatMap {
        case res if res.status == 200 => readEntries reads res.json match {
          case JsError(err) => fufail(err.toString)
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
