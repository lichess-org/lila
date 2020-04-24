package lila.study

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.WSClient
import org.joda.time.format.DateTimeFormat

final class GifExport(
    ws: WSClient,
    url: String
)(implicit ec: scala.concurrent.ExecutionContext) {
  def ofChapter(chapter: Chapter): Fu[Source[ByteString, _]] =
    ws.url(s"$url/game.gif")
      .withMethod("POST")
      .addHttpHeaders("Content-Type" -> "application/json")
      .withBody(
        Json.obj(
          "delay"       -> 80,
          "orientation" -> chapter.setup.orientation.name,
          "white" -> List(
            chapter.tags(_.WhiteTitle),
            chapter.tags(_.White),
            chapter.tags(_.WhiteElo).map(elo => s"($elo)")
          ).flatten.mkString(" "),
          "black" -> List(
            chapter.tags(_.BlackTitle),
            chapter.tags(_.Black),
            chapter.tags(_.BlackElo).map(elo => s"($elo)")
          ).flatten.mkString(" "),
          "frames" -> (chapter.root :: chapter.root.mainline).map { node =>
            Json
              .obj(
                "fen" -> node.fen.value
              )
              .add("check", node.check option true)
              .add("lastMove", node.moveOption.map(_.uci.uci))
          }
        )
      )
      .stream() flatMap {
      case res if res.status != 200 =>
        logger.warn(s"GifExport study ${chapter.studyId}/${chapter._id} ${res.status}")
        fufail(res.statusText)
      case res => fuccess(res.bodyAsSource)
    }
}
