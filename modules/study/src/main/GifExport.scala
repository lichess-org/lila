package lila.study

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.WSClient

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
          "delay"       -> 100,
          "orientation" -> chapter.setup.orientation.engName,
          "black" -> List(
            chapter.tags(_.SenteTitle),
            chapter.tags(_.Sente),
            chapter.tags(_.SenteElo).map(elo => s"($elo)")
          ).flatten.mkString(" "),
          "white" -> List(
            chapter.tags(_.GoteTitle),
            chapter.tags(_.Gote),
            chapter.tags(_.GoteElo).map(elo => s"($elo)")
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
      case node +: tail =>
        framesRec(
          tail,
          arr :+ Json
            .obj(
              "sfen" -> node.sfen.value
            )
            .add("check", node.check option true)
            .add("lastMove", node.usiOption.map(_.usi))
            .add("delay", tail.isEmpty option 500) // more delay for last frame
        )
      case _ => arr
    }
}
