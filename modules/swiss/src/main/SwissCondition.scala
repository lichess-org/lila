package lila.swiss

import play.api.i18n.Lang

import lila.rating.PerfType
import lila.user.User
import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import alleycats.Zero

object SwissCondition:

  type GetBannedUntil = UserId => Fu[Option[Instant]]

  case object PlayYourGames extends Condition:
    def name(perf: PerfType)(using Lang) = "Play your games"
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
        perf: PerfType,
        getMaxRating: GetMaxRating,
        getBannedUntil: GetBannedUntil
    )(user: User)(using Executor): Fu[WithVerdicts] =
      list.map {
        case PlayYourGames => getBannedUntil(user.id) map PlayYourGames.withBan
        case c: MaxRating  => c(getMaxRating)(user, perf) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user, perf))
      }.parallel dmap WithVerdicts.apply

    def similar(other: All) = sameRatings(other) && titled == other.titled

  object All:
    val empty       = All(none, none, none, none, none, PlayYourGames.some)
    given Zero[All] = Zero(empty)

  final class Verify(historyApi: lila.history.HistoryApi, banApi: SwissBanApi):
    def apply(swiss: Swiss, user: User)(using Executor): Fu[WithVerdicts] =
      val getBan: GetBannedUntil     = banApi.bannedUntil
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      swiss.settings.conditions.withVerdicts(swiss.perfType, getMaxRating, getBan)(user)

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
