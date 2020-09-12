package lila.video

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyReadables._
import scala.annotation.nowarn
import scala.concurrent.Future

final private[video] class Sheet(
    ws: StandaloneWSClient,
    url: String,
    api: VideoApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Sheet._

  implicit private val readGStr  = Json.reads[GStr]
  implicit private val readEntry = Json.reads[Entry]
  implicit private val readEntries: Reads[Seq[Entry]] =
    (__ \ "feed" \ "entry").read(Reads seq readEntry)

  def select(entry: Entry) =
    entry.include && entry.lang == "en"

  def fetchAll: Funit =
    fetch dmap (_ filter select) flatMap { entries =>
      Future
        .traverse(entries) { entry =>
          api.video
            .find(entry.youtubeId)
            .flatMap {
              case Some(video) =>
                val updated = video.copy(
                  title = entry.title,
                  author = entry.author,
                  targets = entry.targets,
                  tags = entry.tags,
                  lang = entry.lang,
                  ads = entry.ads,
                  startTime = entry.startTime
                )
                (video != updated) ?? {
                  logger.info(s"sheet update $updated")
                  api.video.save(updated)
                }
              case None =>
                val video = Video(
                  _id = entry.youtubeId,
                  title = entry.title,
                  author = entry.author,
                  targets = entry.targets,
                  tags = entry.tags,
                  lang = entry.lang,
                  ads = entry.ads,
                  startTime = entry.startTime,
                  metadata = Youtube.empty,
                  createdAt = DateTime.now
                )
                logger.info(s"sheet insert $video")
                api.video.save(video)
              case _ => funit
            }
            .recover {
              case e: Exception => logger.warn("sheet update", e)
            }
        }
        .void >>
        api.video.removeNotIn(entries.map(_.youtubeId))
    }

  private def fetch: Fu[List[Entry]] =
    ws.url(url).get() flatMap {
      case res if res.status == 200 =>
        readEntries reads res.body[JsValue] match {
          case JsError(err)          => fufail(err.toString)
          case JsSuccess(entries, _) => fuccess(entries.toList)
        }
      case res => fufail(s"[video sheet] fetch ${res.status}")
    }
}

object Sheet {

  case class GStr(`$t`: String) {
    override def toString = `$t`
  }

  @nowarn case class Entry(
      `gsx$youtubeid`: GStr,
      `gsx$youtubeauthor`: GStr,
      `gsx$title`: GStr,
      `gsx$target`: GStr,
      `gsx$tags`: GStr,
      `gsx$language`: GStr,
      `gsx$include`: GStr,
      `gsx$starttimeinseconds`: GStr,
      `gsx$ads`: GStr
  ) {
    def youtubeId = `gsx$youtubeid`.toString.trim
    def author    = `gsx$youtubeauthor`.toString.trim
    def title     = `gsx$title`.toString.trim
    def targets   = `gsx$target`.toString.split(';').map(_.trim).toList flatMap (_.toIntOption)
    def tags =
      `gsx$tags`.toString.split(';').map(_.trim.toLowerCase).toList.filter(_.nonEmpty) ::: {
        if (targets contains 1) List("beginner")
        else if (targets contains 3) List("advanced")
        else Nil
      }
    def lang      = `gsx$language`.toString.trim
    def ads       = `gsx$ads`.toString.trim == "yes"
    def include   = `gsx$include`.toString.trim == "yes"
    def startTime = ~`gsx$starttimeinseconds`.toString.trim.toIntOption
  }
}
