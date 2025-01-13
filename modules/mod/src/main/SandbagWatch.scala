package lila.mod

import com.github.blemale.scaffeine.Cache
import chess.rating.IntRatingDiff

import lila.core.msg.{ MsgApi, MsgPreset }
import lila.report.ReportApi

final private class SandbagWatch(
    messenger: MsgApi,
    reportApi: ReportApi,
    modLogApi: ModlogApi
)(using Executor):

  import SandbagWatch.*
  import Outcome.*

  private val messageOnceEvery = scalalib.cache.OnceEvery[UserId](1.hour)

  def apply(game: Game): Unit = for
    loser <- game.loser.map(_.color)
    if game.rated && !game.sourceIs(_.Api)
    userId <- game.userIds
  do
    (records.getIfPresent(userId), outcomeOf(game, loser, userId)) match
      case (None, Good)         =>
      case (Some(record), Good) => setRecord(userId, record + Good, game)
      case (record, outcome)    => setRecord(userId, (record | emptyRecord) + outcome, game)

  private def setRecord(userId: UserId, record: Record, game: Game): Funit =
    if record.immaculate then
      fuccess:
        records.invalidate(userId)
    else if game.isTournament && userId.is(game.winnerUserId) then
      // if your opponent always resigns to you in a tournament
      // we'll assume you're not boosting
      funit
    else
      records.put(userId, record)
      modLogApi
        .countRecentRatingManipulationsWarnings(userId)
        .flatMap: nbWarnings =>
          val sandbagCount       = record.countSandbagWithLatest
          val boostCount         = record.samePlayerBoostCount
          val sandbagSeriousness = sandbagCount + nbWarnings
          val boostSeriousness   = boostCount + nbWarnings
          if sandbagCount == 3
          then sendMessage(userId, msgPreset.sandbagAuto)
          else if sandbagCount == 4 then
            game.loserUserId.so:
              reportApi.autoSandbagReport(record.sandbagOpponents, _, sandbagSeriousness)
          else if boostCount == 3
          then sendMessage(userId, msgPreset.boostAuto)
          else if boostCount == 4
          then withWinnerAndLoser(game)((u1, u2) => reportApi.autoBoostReport(u1, u2, boostSeriousness))
          else funit

  private def sendMessage(userId: UserId, preset: MsgPreset): Funit =
    messageOnceEvery(userId).so:
      lila.common.Bus.publish(lila.core.mod.AutoWarning(userId, preset.name), "autoWarning")
      messenger.postPreset(userId, preset).void

  private def withWinnerAndLoser(game: Game)(f: (UserId, UserId) => Funit): Funit =
    game.winnerUserId.so: winner =>
      game.loserUserId.so:
        f(winner, _)

  private val records: Cache[UserId, Record] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(3.hours)
    .build[UserId, Record]()

  private def outcomeOf(game: Game, loser: Color, userId: UserId): Outcome =
    game
      .player(userId)
      .ifTrue(isSandbag(game))
      .fold[Outcome](Good): player =>
        if player.color == loser then game.winnerUserId.fold[Outcome](Good)(Sandbag.apply)
        else game.loserUserId.fold[Outcome](Good)(Boost.apply)

  private def isSandbag(game: Game): Boolean =
    game.playedTurns <= {
      if game.variant == chess.variant.Atomic then 3
      else 8
    } && game.winner.exists(_.ratingDiff.exists(_.positive))

private object SandbagWatch:

  enum Outcome:
    case Good
    case Sandbag(opponent: UserId)
    case Boost(opponent: UserId)

  val maxOutcomes = 7

  case class Record(outcomes: List[Outcome]):

    def +(outcome: Outcome) = copy(outcomes = outcome :: outcomes.take(maxOutcomes - 1))

    def count(outcome: Outcome) = outcomes.count(outcome ==)

    def latest = outcomes.headOption

    def immaculate = outcomes.sizeIs == maxOutcomes && outcomes.forall(Outcome.Good ==)

    def latestIsSandbag = latest.exists:
      case Outcome.Sandbag(_) => true
      case _                  => false

    def countSandbagWithLatest: Int = latestIsSandbag.so(outcomes.count:
      case Outcome.Sandbag(_) => true
      case _                  => false)

    def sandbagOpponents = outcomes.collect { case Outcome.Sandbag(opponent) => opponent }.distinct

    def samePlayerBoostCount = latest.so:
      case Outcome.Boost(opponent) =>
        outcomes.count:
          case Outcome.Boost(o) if o == opponent => true
          case _                                 => false
      case _ => 0

  val emptyRecord = Record(Nil)

  object msgPreset:

    lazy val sandbagAuto = MsgPreset(
      name = "Warning: possible sandbagging",
      text =
        """You have lost a couple games after a few moves. Please note that you MUST try to win every rated game.
  Losing rated games on purpose is called "sandbagging" and is not allowed on Lichess.

  Thank you for your understanding."""
    )
    lazy val boostAuto = MsgPreset(
      name = "Warning: possible boosting",
      """You have won a couple of games after a few moves. Please note that both players MUST try to win every game.
  Taking advantage of opponents losing rated games on purpose is called "boosting" and is not allowed on Lichess.

  Thank you for your understanding."""
    )
