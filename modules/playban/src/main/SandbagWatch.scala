package lila.playban

import chess.Color
import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration._

import lila.game.Game
import lila.msg.{ MsgApi, MsgPreset }
import lila.user.{ User, UserRepo }

final private class SandbagWatch(
    userRepo: UserRepo,
    messenger: MsgApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import SandbagWatch._

  private val onceEvery = lila.memo.OnceEvery(1 hour)

  def apply(game: Game, loser: Color): Fu[Boolean] =
    (game.rated && !game.fromApi) ?? {
      game.userIds
        .map { userId =>
          (records getIfPresent userId, isSandbag(game, loser, userId)) match {
            case (None, false)         => funit
            case (Some(record), false) => updateRecord(userId, record + Good)
            case (record, true)        => updateRecord(userId, (record | newRecord) + Sandbag)
          }
        }
        .sequenceFu
        .void inject isSandbag(game)
    }

  private def sendMessage(userId: User.ID): Funit =
    onceEvery(userId) ?? {
      userRepo byId userId flatMap {
        _ ?? { u =>
          lila.common.Bus
            .publish(lila.hub.actorApi.mod.AutoWarning(u.id, MsgPreset.sandbagAuto.name), "autoWarning")
          messenger.postPreset(u, MsgPreset.sandbagAuto).void
        }
      }
    }

  private def updateRecord(userId: User.ID, record: Record) =
    if (record.immaculate) fuccess(records invalidate userId)
    else {
      records.put(userId, record)
      record.alert ?? sendMessage(userId)
    }

  private val records: Cache[User.ID, Record] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(3 hours)
    .build[User.ID, Record]()

  private def isSandbag(game: Game, loser: Color, userId: User.ID): Boolean =
    game.playerByUserId(userId).exists {
      _ == game.player(loser) && isSandbag(game)
    }

  private def isSandbag(game: Game): Boolean =
    game.turns <= {
      if (game.variant == chess.variant.Atomic) 3
      else 6
    }
}

private object SandbagWatch {

  sealed trait Outcome
  case object Good    extends Outcome
  case object Sandbag extends Outcome

  val maxOutcomes = 7

  case class Record(outcomes: List[Outcome]) {

    def +(outcome: Outcome) = copy(outcomes = outcome :: outcomes.take(maxOutcomes - 1))

    def alert = latestIsSandbag && outcomes.count(Sandbag ==) >= 3

    def latestIsSandbag = outcomes.headOption.exists(Sandbag ==)

    def immaculate = outcomes.sizeIs == maxOutcomes && outcomes.forall(Good ==)
  }

  val newRecord = Record(Nil)
}
