package lila.mod

import com.github.blemale.scaffeine.Cache
import chess.rating.IntRatingDiff
import chess.IntRating

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
    if game.rated.yes && !game.sourceIs(_.Api)
    userId <- game.userIds
  do
    (records.getIfPresent(userId), outcomeOf(game, loser, userId)) match
      case (None, Good)         =>
      case (Some(record), Good) => setRecord(userId, record + Good, game)
      case (record, outcome)    => setRecord(userId, (record | emptyRecord) + outcome, game)

  private def setRecord(userId: UserId, record: Record, game: Game): Funit =
    if record.immaculate then fuccess(records.invalidate(userId))
    else if game.isTournament && userId.is(game.winnerUserId) then
      // if your opponent always resigns to you in a tournament
      // we'll assume you're not boosting
      funit
    else
      records.put(userId, record)
      for
        nbWarnings <- modLogApi.countRecentRatingManipulationsWarnings(userId)
        sandbagCount       = record.countSandbagWithLatest
        boostCount         = record.samePlayerBoostCount
        sandbagSeriousness = sandbagCount + nbWarnings
        boostSeriousness   = boostCount + nbWarnings
        _ <-
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
      yield ()

  private def sendMessage(userId: UserId, preset: MsgPreset): Funit =
    messageOnceEvery(userId).so:
      lila.common.Bus.pub(lila.core.mod.AutoWarning(userId, preset.name))
      messenger.postPreset(userId, preset).void

  private def withWinnerAndLoser(game: Game)(f: (UserId, UserId) => Funit): Funit =
    (game.winnerUserId, game.loserUserId).tupled.so(f.tupled)

  private val records: Cache[UserId, Record] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(3.hours)
    .build[UserId, Record]()

  private def outcomeOf(game: Game, loser: Color, userId: UserId): Outcome =
    game
      .player(userId)
      .ifTrue(isSandbagOrBoost(game))
      .flatMap: player =>
        if player.color == loser
        then game.winnerUserId.map(Sandbag.apply)
        else game.loserUserId.map(Boost.apply)
      .getOrElse(Good)

  private def isSandbagOrBoost(game: Game): Boolean =

    def loserRatingGt(r: Int) = game.loser.flatMap(_.rating).exists(_ > IntRating(r))

    val baseMinTurns =
      if loserRatingGt(1800) then 20
      else if loserRatingGt(1600) then 12
      else 8

    import chess.variant.*
    val minTurns = game.variant match
      case Atomic                     => baseMinTurns / 4
      case KingOfTheHill | ThreeCheck => baseMinTurns / 2
      case _                          => baseMinTurns

    game.playedPlies <= minTurns && game.winner.exists(_.ratingDiff.exists(_.positive))

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
      text = """Our system noticed that you lost a couple of rated games very quickly. We understand this can happen for many reasons, from a poor connection, to an opponent's clever opening trap, or simply making a mistake and resigning early.

  We're writing because this pattern can also be a sign of "sandbagging," losing games on purpose to lower one's rating. To ensure a fair and enjoyable experience for everyone, our policy requires that players try their best to win every rated game.

  If these quick losses were unintentional, please don't worry. This is just a friendly reminder about our Fair Play policy.

  Thank you for helping keep Lichess fun and fair."""
    )
    lazy val boostAuto = MsgPreset(
      name = "Warning: possible boosting",
      """Our system noticed that you won a couple of rated games very quickly. We understand this can happen for many reasons, perhaps your opponent has a poor connection, fell for a clever opening trap, or simply made a mistake and resigned early.

  We're writing because this pattern can also be a sign of "boosting," where one player benefits from an opponent who is losing on purpose. To ensure a fair and enjoyable experience for everyone, our policy requires that both players try their best to win every rated game.

  If your quick wins were the result of fair play, please don't worry. This is just a friendly reminder about our Fair Play policy.

  Thank you for helping keep Lichess fun and fair."""
    )
