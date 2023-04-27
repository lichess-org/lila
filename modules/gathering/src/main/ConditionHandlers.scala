package lila.gathering

import lila.gathering.Condition.*
import play.api.i18n.Lang

object ConditionHandlers:

  object BSONHandlers:
    import reactivemongo.api.bson.*
    import lila.db.dsl.{ *, given }
    import lila.rating.BSONHandlers.perfTypeKeyHandler

    given BSONDocumentHandler[NbRatedGame] = Macros.handler
    given BSONDocumentHandler[MaxRating]   = Macros.handler
    given BSONDocumentHandler[MinRating]   = Macros.handler
    given BSONHandler[Titled.type] = quickHandler(
      { case _: BSONValue => Titled },
      _ => BSONBoolean(true)
    )
    given BSONDocumentHandler[TeamMember] = Macros.handler
    given BSONDocumentHandler[AllowList]  = Macros.handler

  object JSONHandlers:
    import lila.common.Json.given
    import play.api.libs.json.*

    def verdictsFor(verdicts: WithVerdicts, lang: Lang) =
      Json.obj(
        "list" -> verdicts.list.map { case WithVerdict(cond, verd) =>
          Json.obj(
            "condition" -> cond.name(using lang),
            "verdict" -> (verd match
              case Refused(reason) => reason(lang)
              case Accepted        => JsString("ok")
            )
          )
        },
        "accepted" -> verdicts.accepted
      )

    given OWrites[Condition.RatingCondition] = OWrites { r =>
      Json.obj(
        "perf"   -> r.perf.key,
        "rating" -> r.rating
      )
    }

    given OWrites[Condition.NbRatedGame] = OWrites { r =>
      Json
        .obj("nb" -> r.nb)
        .add("perf" -> r.perf.map(_.key))
    }
