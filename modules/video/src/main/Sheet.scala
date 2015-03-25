package lila.video

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private[video] final class Sheet(
    url: String,
    api: VideoApi) {

  import Sheet._

  private implicit val readGStr = Json.reads[GStr]
  private implicit val readEntry = Json.reads[Entry]
  private implicit val readEntries: Reads[Seq[Entry]] =
    (__ \ "feed" \ "entry").read(Reads seq readEntry)

  def fetchAll: Funit = fetch flatMap { entries =>
    entries.map { entry =>
      api.video.find(entry.youtubeId).flatMap {
        case Some(video) =>
          val updated = video.copy(
            title = entry.title,
            author = entry.author,
            targets = entry.targets,
            tags = entry.tags,
            lang = entry.lang,
            lichess = entry.lichess,
            ads = entry.ads)
          (video != updated) ?? {
            loginfo(s"[video sheet] update $updated")
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
            lichess = entry.lichess,
            ads = entry.ads,
            metadata = Youtube.empty,
            createdAt = DateTime.now)
          loginfo(s"[video sheet] insert $video")
          api.video.save(video)
        case _ => funit
      }.recover {
        case e: Exception => logerr(s"[video sheet] ${e.getMessage}")
      }
    }.sequenceFu.void >>
      api.video.removeNotIn(entries.map(_.youtubeId)) >>
      api.video.count.clearCache >>
      api.tag.clearCache
  }

  private def fetch: Fu[List[Entry]] = WS.url(url).get() flatMap {
    case res if res.status == 200 => readEntries reads res.json match {
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

  case class Entry(
      `gsx$youtubeid`: GStr,
      `gsx$youtubeauthor`: GStr,
      `gsx$title`: GStr,
      `gsx$target`: GStr,
      `gsx$tags`: GStr,
      `gsx$language`: GStr,
      `gsx$useslichess`: GStr,
      `gsx$ads`: GStr) {
    def youtubeId = `gsx$youtubeid`.toString.trim
    def author = `gsx$youtubeauthor`.toString.trim
    def title = `gsx$title`.toString.trim
    def targets = `gsx$target`.toString.split(';').map(_.trim).toList flatMap parseIntOption
    def tags = `gsx$tags`.toString.split(';').map(_.trim.toLowerCase).toList
    def lang = `gsx$language`.toString.trim
    def lichess = `gsx$useslichess`.toString.trim == "yes"
    def ads = `gsx$ads`.toString.trim == "yes"
  }
}
