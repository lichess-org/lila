package lidraughts.perfStat

import akka.actor.ActorRef

import lidraughts.game.{ Game, GameRepo, Pov, Query }
import lidraughts.hub.Sequencer
import lidraughts.rating.PerfType
import lidraughts.user.User

final class PerfStatIndexer(storage: PerfStatStorage, sequencer: ActorRef) {

  def userPerf(user: User, perfType: PerfType): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ! Sequencer.work(compute(user, perfType), p.some)
    p.future
  }

  private def compute(user: User, perfType: PerfType): Funit = {
    GameRepo.sortedCursor(
      Query.user(user.id) ++
        Query.finished ++
        Query.turnsGt(2) ++
        Query.variant(PerfType variantOf perfType),
      Query.sortChronological
    ).fold(PerfStat.init(user.id, perfType)) {
        case (perfStat, game) if game.perfType.contains(perfType) =>
          Pov.ofUserId(game, user.id).fold(perfStat)(perfStat.agg)
        case (perfStat, _) => perfStat
      }
  } flatMap storage.insert recover lidraughts.db.recoverDuplicateKey(_ => ())

  def addGame(game: Game): Funit = game.players.flatMap { player =>
    player.userId.map { userId =>
      addPov(Pov(game, player), userId)
    }
  }.sequenceFu.void

  private def addPov(pov: Pov, userId: String): Funit = pov.game.perfType ?? { perfType =>
    storage.find(userId, perfType) flatMap {
      _ ?? { perfStat =>
        storage.update(perfStat agg pov)
      }
    }
  }
}
