package lila.swiss

import play.api.i18n.Lang

import lila.rating.PerfType
import lila.user.{ Me, UserPerfs }
import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import alleycats.Zero
import lila.i18n.{ I18nKeys as trans }
import lila.rating.Perf

object SwissCondition:

  type GetBannedUntil = UserId => Fu[Option[Instant]]

  case object PlayYourGames extends Condition:
    def name(perf: PerfType)(using Lang) = trans.swiss.playYourGames.txt()
    def withBan(bannedUntil: Option[Instant]) = withVerdict:
      bannedUntil.fold[Verdict](Accepted)(RefusedUntil.apply)

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      allowList: Option[AllowList],
      playYourGames: Option[PlayYourGames.type]
  ) extends ConditionList(List(nbRatedGame, maxRating, minRating, titled, allowList, playYourGames)):

    def withVerdicts(
        perfType: PerfType,
        getBannedUntil: GetBannedUntil
    )(using me: Me)(using Perf, Executor, GetMaxRating): Fu[WithVerdicts] =
      list.map {
        case PlayYourGames => getBannedUntil(me.userId) map PlayYourGames.withBan
        case c: MaxRating  => c(perfType) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(perfType))
      }.parallel dmap WithVerdicts.apply

    def similar(other: All) = sameRatings(other) && titled == other.titled

  object All:
    val empty       = All(none, none, none, none, none, PlayYourGames.some)
    given Zero[All] = Zero(empty)

  final class Verify(historyApi: lila.history.HistoryApi, banApi: SwissBanApi):
    def apply(swiss: Swiss)(using me: Me)(using Perf, Executor): Fu[WithVerdicts] =
      val getBan: GetBannedUntil = banApi.bannedUntil
      given GetMaxRating         = historyApi.lastWeekTopRating(me.value, _)
      swiss.settings.conditions.withVerdicts(swiss.perfType, getBan)

  object form:
    import play.api.data.Forms.*
    import lila.gathering.ConditionForm.*

    def all =
      mapping(
        "nbRatedGame" -> nbRatedGame,
        "maxRating"   -> maxRating,
        "minRating"   -> minRating,
        "titled"      -> titled,
        "allowList"   -> allowList,
        "playYourGames" -> optional(boolean)
          .transform(_.contains(true) option PlayYourGames, _.isDefined option true)
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    given BSONHandler[PlayYourGames.type] = lila.db.dsl.ifPresentHandler(PlayYourGames)
    Macros.handler
