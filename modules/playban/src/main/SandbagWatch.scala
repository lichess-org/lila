package lila.playban

import chess.Color
import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration._

import lila.game.Game
import lila.msg.{ MsgApi, MsgPreset }
import lila.user.{ User, UserRepo }

final private class SandbagWatch(
    userRepo: UserRepo,
    messenger: MsgApi,
    reporter: lila.hub.actors.Report
)(implicit ec: scala.concurrent.ExecutionContext) {

  import SandbagWatch._

  private val messageOnceEvery = lila.memo.OnceEvery(1 hour)

  // returns true if one of the players has 2 or more sandbag outcomes
  def apply(game: Game, loser: Color): Fu[Boolean] =
    (game.rated && !game.fromApi) ?? {
      game.userIds
        .map { userId =>
          (records getIfPresent userId, outcomeOf(game, loser, userId).pp(userId)) match {
            case (None, Good)         => fuccess(0)
            case (Some(record), Good) => updateRecord(userId, record + Good, game) inject 0
            case (record, outcome) =>
              val newRecord = (record | emptyRecord) + outcome
              updateRecord(userId, newRecord, game) inject {
                (outcome == Sandbag) ?? newRecord.count(Sandbag)
              }
          }
        }
        .sequenceFu
        .thenPp
        .dmap { _.exists(_ > 0) }
    }

  private def sendMessage(userId: User.ID, preset: MsgPreset): Funit =
    messageOnceEvery(userId) ?? {
      lila.common.Bus.publish(lila.hub.actorApi.mod.AutoWarning(userId, preset.name), "autoWarning")
      messenger.postPreset(userId, preset).void
    }

  private def updateRecord(userId: User.ID, record: Record, game: Game): Funit =
    if (record.pp(userId).immaculate) fuccess(records invalidate userId)
    else {
      records.put(userId, record)
      if (record.latest has Sandbag) {
        if (record.count(Sandbag).pp("sandbag") == 3) sendMessage(userId, MsgPreset.sandbagAuto)
        else if (record.count(Sandbag) == 4) withWinnerAndLoser(game) { (winner, loser) =>
          fuccess { reporter ! lila.hub.actorApi.report.Sandbagger(winner, loser) }
        }
        else funit
      } else if (record.latest has Boost) {
        if (record.count(Boost).pp("boost") == 3) sendMessage(userId, MsgPreset.boostAuto)
        else if (record.count(Boost) == 4) withWinnerAndLoser(game) { (winner, loser) =>
          fuccess { reporter ! lila.hub.actorApi.report.Booster(winner, loser) }
        }
        else funit
      } else funit
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
        if (player.color == loser) Sandbag else Boost
      }

  private def isSandbag(game: Game): Boolean =
    game.playedTurns <= {
      if (game.variant == chess.variant.Atomic) 3
      else 8
    }
}

private object SandbagWatch {

  sealed trait Outcome
  case object Good    extends Outcome
  case object Sandbag extends Outcome
  case object Boost   extends Outcome

  val maxOutcomes = 7

  case class Record(outcomes: List[Outcome]) {

    def +(outcome: Outcome) = copy(outcomes = outcome :: outcomes.take(maxOutcomes - 1))

    def count(outcome: Outcome) = outcomes.count(outcome ==)

    def latest = outcomes.headOption

    def immaculate = outcomes.sizeIs == maxOutcomes && outcomes.forall(Good ==)
  }

  val emptyRecord = Record(Nil)
}
