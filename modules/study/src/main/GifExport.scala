package lila.study

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSClient

final class GifExport(
    ws: StandaloneWSClient,
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
          "frames" -> framesRec(chapter.root +: chapter.root.mainline, Json.arr())
        )
      )
      .stream() flatMap {
      case res if res.status != 200 =>
        logger.warn(s"GifExport study ${chapter.studyId}/${chapter._id} ${res.status}")
        fufail(res.statusText)
      case res => fuccess(res.bodyAsSource)
    }

  @annotation.tailrec
  private def framesRec(nodes: Vector[RootOrNode], arr: JsArray): JsArray =
    nodes match {
      case Nil => arr
      case node +: tail =>
        framesRec(
          tail,
          arr :+ Json
            .obj(
              "fen" -> node.fen.value
            )
            .add("check", node.check option true)
            .add("lastMove", node.moveOption.map(_.uci.uci))
            .add("delay", tail.isEmpty option 500) // more delay for last frame
        )
    }
}
