package lila.practice

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.study.JsonView._

object JsonView {

  case class JsData(study: JsObject, analysis: JsObject, practice: JsObject)

  implicit val nbMovesWrites: Writes[PracticeProgress.NbMoves] = intAnyValWriter(_.value)
  implicit val practiceStudyWrites: Writes[PracticeStudy] = OWrites { ps =>
    Json.obj(
      "id" -> ps.id,
      "name" -> ps.name,
      "desc" -> ps.desc)
  }

  def apply(us: UserStudy) = Json.obj(
    "study" -> us.practiceStudy,
    "prev" -> us.practice.structure.prev(us.practiceStudy.id),
    "next" -> us.practice.structure.next(us.practiceStudy.id),
    "completion" -> JsObject {
      us.practiceStudy.chapters.flatMap { c =>
        us.practice.progress.chapters collectFirst {
          case (id, nbMoves) if id == c.id => id.value -> nbMovesWrites.writes(nbMoves)
        }
      }
    },
    "structure" -> us.practice.structure.sections.map { sec =>
      Json.obj(
        "id" -> sec.id,
        "name" -> sec.name,
        "studies" -> sec.studies.map { stu =>
          Json.obj(
            "id" -> stu.id,
            "slug" -> stu.slug,
            "name" -> stu.name)
        })
    })
}
