package lila.practice

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.study.Chapter

object JsonView {

  case class JsData(study: JsObject, analysis: JsObject, practice: JsObject)

  implicit val chapterIdWrites: Writes[Chapter.Id] = stringIsoWriter(Chapter.idIso)
  implicit val nbMovesWrites: Writes[PracticeProgress.NbMoves] = intAnyValWriter(_.value)

  def apply(us: UserStudy) = Json.obj(
    "completion" -> JsObject {
      us.practiceStudy.chapters.flatMap { c =>
        us.practice.progress.chapters collectFirst {
          case (id, nbMoves) if id == c.id => id.value -> nbMovesWrites.writes(nbMoves)
        }
      }
    })
}
