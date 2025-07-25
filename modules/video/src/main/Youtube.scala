package lila.video

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.ThreadLocalRandom

import lila.core.config.*

final private[video] class Youtube(
    ws: StandaloneWSClient,
    url: String,
    apiKey: Secret,
    max: Max,
    api: VideoApi
)(using Executor):

  import Youtube.*

  private given Reads[Snippet] = Json.reads
  private given Reads[Statistics] = Json.reads
  private given Reads[ContentDetails] = Json.reads
  private val readEntry: Reads[Entry] = Json.reads
  private val readEntries: Reads[Seq[Entry]] = (__ \ "items").read(using Reads.seq(using readEntry))

  def updateAll: Funit =
    fetch.flatMap { entries =>
      Future
        .traverse(entries) { entry =>
          api.video
            .setMetadata(
              entry.id,
              Metadata(
                views = ~entry.statistics.viewCount.toIntOption,
                likes = ~entry.statistics.likeCount.toIntOption,
                description = entry.snippet.description,
                duration = Some(entry.contentDetails.seconds),
                publishedAt = entry.snippet.publishedAt.flatMap { at =>
                  scala.util.Try { java.time.Instant.parse(at) }.toOption
                }
              )
            )
            .recover { case e: Exception =>
              logger.warn("update all youtube", e)
            }
        }
        .void
    }

  private def fetch: Fu[List[Entry]] =
    api.video.allIds.flatMap { ids =>
      ws.url(url)
        .withQueryStringParameters(
          "id" -> ThreadLocalRandom.shuffle(ids).take(max.value).mkString(","),
          "part" -> "id,statistics,snippet,contentDetails",
          "key" -> apiKey.value
        )
        .get()
        .flatMap:
          case res if res.status == 200 =>
            readEntries.reads(res.body[JsValue]) match
              case JsError(err) => fufail(err.toString)
              case JsSuccess(entries, _) => fuccess(entries.toList)
          case res =>
            fufail(s"[video youtube] fetch ${res.status}")
    }

object Youtube:

  def empty = Metadata(0, 0, None, None, None)

  case class Metadata(
      views: Int,
      likes: Int,
      description: Option[String],
      duration: Option[Int], // in seconds
      publishedAt: Option[Instant]
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
      likeCount: String
  )

  private[video] case class ContentDetails(duration: String):
    def seconds: Int = java.time.Duration.parse(duration).getSeconds().toInt
