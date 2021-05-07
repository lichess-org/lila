package lila.mod

import chess.Color
import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration._

import lila.game.Game
import lila.msg.{ MsgApi, MsgPreset }
import lila.report.ReportApi
import lila.user.User

final private class SandbagWatch(
    messenger: MsgApi,
    reportApi: ReportApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import SandbagWatch._

  private val messageOnceEvery = lila.memo.OnceEvery(1 hour)

  def apply(game: Game): Unit = for {
    loser <- game.loser.map(_.color)
    if game.rated && !game.fromApi
    userId <- game.userIds
  } {
    (records getIfPresent userId, outcomeOf(game, loser, userId)) match {
      case (None, Good)         =>
      case (Some(record), Good) => setRecord(userId, record + Good, game)
      case (record, outcome)    => setRecord(userId, (record | emptyRecord) + outcome, game)
    }
  }

  private def setRecord(userId: User.ID, record: Record, game: Game): Funit =
    if (record.immaculate) fuccess {
      records invalidate userId
    }
    else {
      records.put(userId, record)
      val sandbagCount = record.countSandbagWithLatest
      val boostCount   = record.samePlayerBoostCount
      if (sandbagCount == 3) sendMessage(userId, MsgPreset.sandbagAuto)
      else if (sandbagCount == 4) game.loserUserId ?? { loser =>
        reportApi.autoSandbagReport(record.sandbagOpponents, loser)
      }
      else if (boostCount == 3) sendMessage(userId, MsgPreset.boostAuto)
      else if (boostCount == 4) withWinnerAndLoser(game)(reportApi.autoBoostReport)
      else funit
    }

  private def sendMessage(userId: User.ID, preset: MsgPreset): Funit =
    messageOnceEvery(userId) ?? {
      lila.common.Bus.publish(lila.hub.actorApi.mod.AutoWarning(userId, preset.name), "autoWarning")
      messenger.postPreset(userId, preset).void
    }

  private def withWinnerAndLoser(game: Game)(f: (User.ID, User.ID) => Funit): Funit =
    game.winnerUserId ?? { winner =>
      game.loserUserId ?? {
        f(winner, _)
      }
    }

  private val records: Cache[User.ID, Record] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(3 hours)
    .build[User.ID, Record]()

  private def outcomeOf(game: Game, loser: Color, userId: User.ID): Outcome =
    game
      .playerByUserId(userId)
      .ifTrue(isSandbag(game))
      .fold[Outcome](Good) { player =>
        if (player.color == loser) game.winnerUserId.fold[Outcome](Good)(Sandbag.apply)
        else game.loserUserId.fold[Outcome](Good)(Boost.apply)
      }

  private def isSandbag(game: Game): Boolean =
    game.playedTurns <= {
      if (game.variant == chess.variant.Atomic) 3
      else 8
    } && game.winner ?? (~_.ratingDiff > 0)
}

private object SandbagWatch {

  sealed trait Outcome
  case object Good                      extends Outcome
  case class Sandbag(opponent: User.ID) extends Outcome
  case class Boost(opponent: User.ID)   extends Outcome

  val maxOutcomes = 7

  case class Record(outcomes: List[Outcome]) {

    def +(outcome: Outcome) = copy(outcomes = outcome :: outcomes.take(maxOutcomes - 1))

    def count(outcome: Outcome) = outcomes.count(outcome ==)

    def latest = outcomes.headOption

    def immaculate = outcomes.sizeIs == maxOutcomes && outcomes.forall(Good ==)

    def latestIsSandbag = latest exists {
      case Sandbag(_) => true
      case _          => false
    }

    def countSandbagWithLatest: Int = latestIsSandbag ?? outcomes.count {
      case Sandbag(_) => true
      case _          => false
    }

    def sandbagOpponents = outcomes.collect { case Sandbag(opponent) =>
      opponent
    }.distinct

    def samePlayerBoostCount = latest ?? {
      case Boost(opponent) =>
        outcomes.count {
          case Boost(o) if o == opponent => true
          case _                         => false
        }
      case _ => 0
    }
  }

  val emptyRecord = Record(Nil)
}
