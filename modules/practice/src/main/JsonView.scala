package lila.practice

import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.core.i18n.Translate

object JsonView:

  case class JsData(study: JsObject, analysis: JsObject, practice: JsObject)

  given Writes[PracticeProgress.NbMoves] = writeAs(_.value)
  given (using Translate): Writes[PracticeStudy] = OWrites: ps =>
    Json.obj(
      "id" -> ps.id,
      "name" -> ps.name.txt(),
      "desc" -> ps.desc.txt()
    )
  import PracticeGoal.*
  given Writes[PracticeGoal] = OWrites:
    case Mate => Json.obj("result" -> "mate")
    case MateIn(moves) => Json.obj("result" -> "mateIn", "moves" -> moves)
    case DrawIn(moves) => Json.obj("result" -> "drawIn", "moves" -> moves)
    case EqualIn(moves) => Json.obj("result" -> "equalIn", "moves" -> moves)
    case EvalIn(cp, moves) => Json.obj("result" -> "evalIn", "cp" -> cp, "moves" -> moves)
    case Promotion(cp) => Json.obj("result" -> "promotion", "cp" -> cp)

  private given (using Translate): Writes[PracticeSection] = OWrites: sec =>
    Json.obj(
      "id" -> sec.id,
      "name" -> sec.name.txt(),
      "studies" -> sec.studies.map: stu =>
        Json.obj(
          "id" -> stu.id,
          "slug" -> stu.slug,
          "name" -> stu.name.txt()
        )
    )

  def apply(us: UserStudy)(using Translate) =
    Json.obj(
      "study" -> us.practiceStudy,
      "url" -> us.url,
      "completion" -> JsObject:
        us.practiceStudy.chapters.flatMap: c =>
          us.practice.progress.chapters.collectFirst:
            case (id, nbMoves) if id == c.id => id.value -> Json.toJson(nbMoves),
      "structure" -> us.practice.structure.sections
    )

  def api(us: UserPractice)(using Translate) = Json.obj(
    "sections" -> us.structure.sections,
    "progress" -> us.progress.chapters
  )
