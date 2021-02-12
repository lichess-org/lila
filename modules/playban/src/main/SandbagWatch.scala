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

  private val onceEvery = lila.memo.OnceEvery(1 hour)

  def apply(game: Game, loser: Color): Fu[Boolean] =
    (game.rated && !game.fromApi) ?? {
      game.userIds
        .map { userId =>
          (records getIfPresent userId, outcomeOf(game, loser, userId)) match {
            case (None, Good)         => funit
            case (Some(record), Good) => updateRecord(userId, record + Good, game)
            case (record, outcome)    => updateRecord(userId, (record | emptyRecord) + outcome, game)
          }
        }
        .sequenceFu
        .void inject isSandbag(game)
    }

  private def sendMessage(userId: User.ID, preset: MsgPreset): Funit =
    onceEvery(userId) ?? {
      userRepo byId userId flatMap {
        _ ?? { u =>
          lila.common.Bus
            .publish(lila.hub.actorApi.mod.AutoWarning(u.id, preset.name), "autoWarning")
          messenger.postPreset(u, preset).void
        }
      }
    }

  private def updateRecord(userId: User.ID, record: Record, game: Game): Funit =
    if (record.immaculate) fuccess(records invalidate userId)
    else {
      records.put(userId, record)
      if (record.latest has Sandbag) {
        if (record.count(Sandbag) == 3) sendMessage(userId, MsgPreset.sandbagAuto)
        else if (record.count(Sandbag) == 4) withWinnerAndLoser(game) { (winner, loser) =>
          fuccess { reporter ! lila.hub.actorApi.report.Sandbagger(winner, loser) }
        }
        else funit
      } else if (record.latest has Boost) {
        if (record.count(Boost) == 3) sendMessage(userId, MsgPreset.boostAuto)
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
      else 6
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
