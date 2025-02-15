package lila.gathering
import java.time.format.{ DateTimeFormatter, FormatStyle }

import lila.core.i18n.Translate
import lila.gathering.Condition.*
import lila.rating.PerfType

object ConditionHandlers:

  object BSONHandlers:
    import reactivemongo.api.bson.*
    import lila.db.dsl.{ *, given }

    given BSONDocumentHandler[NbRatedGame] = Macros.handler
    given BSONDocumentHandler[MaxRating]   = Macros.handler
    given BSONDocumentHandler[MinRating]   = Macros.handler
    given BSONHandler[Titled.type]         = ifPresentHandler(Titled)
    given BSONHandler[AccountAge]          = Macros.handler
    given BSONDocumentHandler[TeamMember]  = Macros.handler
    given BSONDocumentHandler[AllowList]   = Macros.handler
    given BSONHandler[Bots] =
      quickHandler[Bots]({ case BSONBoolean(v) => Bots(v) }, bots => BSONBoolean(bots.allowed))

  object JSONHandlers:
    import lila.common.Json.given
    import play.api.libs.json.*

    def verdictsFor(verdicts: WithVerdicts, pt: PerfType)(using translate: Translate) =
      Json.obj(
        "list" -> verdicts.list.map { case WithVerdict(cond, verd) =>
          Json.obj(
            "condition" -> cond.name(pt),
            "verdict" -> verd.match
              case Accepted        => JsString("ok")
              case Refused(reason) => reason(translate)
              case RefusedUntil(until) =>
                val date = DateTimeFormatter
                  .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                  .withLocale(translate.lang.toLocale)
                s"Because you missed your last Swiss game, you cannot enter a new Swiss tournament until $date"
          )
        },
        "accepted" -> verdicts.accepted
      )

    given OWrites[Condition.RatingCondition] = OWrites { r =>
      Json.obj("rating" -> r.rating)
    }

    given OWrites[Condition.NbRatedGame] = OWrites { r =>
      Json.obj("nb" -> r.nb)
    }
