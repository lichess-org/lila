package lila.video

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private[video] final class FetchSheet(
    url: String,
    api: VideoApi) {

  import FetchSheet._

  private implicit val readGStr = Json.reads[GStr]
  private implicit val readEntry = Json.reads[Entry]
  private implicit val readEntries: Reads[Seq[Entry]] =
    (__ \ "feed" \ "entry").read(Reads seq readEntry)

  def apply: Funit = fetch flatMap { entries =>
    entries.map { entry =>
      api.video.find(entry.youtubeId).flatMap {
        case Some(video) => api.video.save(video.copy(
          title = entry.title,
          author = entry.author,
          targets = entry.targets,
          tags = entry.tags,
          lang = entry.lang,
          lichess = entry.lichess,
          ads = entry.ads,
          updatedAt = entry.updatedAt))
        case None => api.video.save(Video(
          _id = entry.youtubeId,
          title = entry.title,
          author = entry.author,
          targets = entry.targets,
          tags = entry.tags,
          lang = entry.lang,
          lichess = entry.lichess,
          ads = entry.ads,
          createdAt = DateTime.now,
          updatedAt = entry.updatedAt))
      }.recover {
        case e: Exception => logerr(s"[video] ${e.getMessage}")
      }
    }.sequenceFu.void
  }

  private def fetch: Fu[List[Entry]] = WS.url(url).get() flatMap {
    case res if res.status == 200 => readEntries reads res.json match {
      case JsError(err)          => fufail(err.toString)
      case JsSuccess(entries, _) => fuccess(entries.toList)
    }
    case res => fufail(s"[video] fetch sheet ${res.status}")
  }
}

object FetchSheet {

  case class GStr(`$t`: String) {
    override def toString = `$t`
  }

  case class Entry(
      `gsx$youtubeid`: GStr,
      `gsx$youtubeauthor`: GStr,
      `gsx$title`: GStr,
      `gsx$target`: GStr,
      `gsx$tags`: GStr,
      `gsx$language`: GStr,
      `gsx$useslichess`: GStr,
      `gsx$ads`: GStr,
      updated: GStr) {
    def youtubeId = `gsx$youtubeid`.toString
    def author = `gsx$youtubeauthor`.toString
    def title = `gsx$title`.toString
    def targets = `gsx$target`.toString.split(';').map(_.trim).toList flatMap parseIntOption
    def tags = `gsx$tags`.toString.split(';').map(_.trim.toLowerCase).toList
    def lang = `gsx$language`.toString
    def lichess = `gsx$useslichess`.toString == "yes"
    def ads = `gsx$ads`.toString == "yes"
    def updatedAt = new DateTime(updated.toString)
  }
}
