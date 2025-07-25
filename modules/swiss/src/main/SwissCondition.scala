package lila.swiss

import alleycats.Zero

import lila.core.LightUser
import lila.core.i18n.{ I18nKey, Translate }
import lila.core.user.UserApi
import lila.gathering.Condition.*
import lila.gathering.{ Condition, ConditionList }
import lila.rating.PerfType

object SwissCondition:

  type GetBannedUntil = UserId => Fu[Option[Instant]]

  case object PlayYourGames extends Condition:
    def name(perf: PerfType)(using Translate) = I18nKey.swiss.playYourGames.txt()
    def withBan(bannedUntil: Option[Instant]) = withVerdict:
      bannedUntil.fold[Verdict](Accepted)(RefusedUntil.apply)

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      accountAge: Option[AccountAge],
      allowList: Option[AllowList],
      playYourGames: Option[PlayYourGames.type]
  ) extends ConditionList(
        List(nbRatedGame, maxRating, minRating, titled, accountAge, allowList, playYourGames)
      ):

    def withVerdicts(
        perfType: PerfType,
        getBannedUntil: GetBannedUntil
    )(using me: LightUser.Me)(using Perf, Executor, GetMaxRating, GetAge): Fu[WithVerdicts] =
      list
        .parallel:
          case PlayYourGames => getBannedUntil(me.userId).map(PlayYourGames.withBan)
          case c: MaxRating => c(perfType).map(c.withVerdict)
          case c: FlatCond => fuccess(c.withVerdict(c(perfType)))
          case c: AccountAge => c.apply.map { c.withVerdict(_) }
        .dmap(WithVerdicts.apply)

    def similar(other: All) = sameRatings(other) && titled == other.titled

    def isDefault = this == All.empty

  object All:
    val empty = All(none, none, none, none, none, none, PlayYourGames.some)
    given Zero[All] = Zero(empty)

  final class Verify(historyApi: lila.core.history.HistoryApi, banApi: SwissBanApi, userApi: UserApi)(using
      Executor
  ):
    def apply(swiss: Swiss)(using me: LightUser.Me)(using Perf): Fu[WithVerdicts] =
      val getBan: GetBannedUntil = banApi.bannedUntil
      given GetMaxRating = historyApi.lastWeekTopRating(me.userId, _)
      given GetAge = me => userApi.accountAge(me.userId)
      swiss.settings.conditions.withVerdicts(swiss.perfType, getBan)

  object form:
    import play.api.data.Forms.*
    import lila.gathering.ConditionForm.*

    def all =
      mapping(
        "nbRatedGame" -> nbRatedGame,
        "maxRating" -> maxRating,
        "minRating" -> minRating,
        "titled" -> titled,
        "accountAge" -> accountAge,
        "allowList" -> allowList,
        "playYourGames" -> optional(boolean)
          .transform(_.contains(true).option(PlayYourGames), _.isDefined.option(true))
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    given BSONHandler[PlayYourGames.type] = lila.db.dsl.ifPresentHandler(PlayYourGames)
    Macros.handler
