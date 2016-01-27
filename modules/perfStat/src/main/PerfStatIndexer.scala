package lila.perfStat

import akka.actor.ActorRef
import play.api.libs.iteratee._

import lila.db.api._
import lila.db.Implicits._
import lila.game.{ Game, Pov, Query }
import lila.hub.Sequencer
import lila.rating.PerfType
import lila.user.User

final class PerfStatIndexer(storage: PerfStatStorage, sequencer: ActorRef) {

  private implicit val timeout = makeTimeout minutes 2

  def userPerf(user: User, perfType: PerfType): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ! Sequencer.work(compute(user, perfType), p.some)
    p.future
  }

  private def compute(user: User, perfType: PerfType): Funit = {
    import lila.game.tube.gameTube
    import lila.game.BSONHandlers.gameBSONHandler
    pimpQB($query {
      Query.user(user.id) ++
        Query.finished ++
        Query.turnsMoreThan(2) ++
        Query.variant(PerfType variantOf perfType)

    }).sort(Query.sortChronological)
      .cursor[Game]()
      .enumerate(Int.MaxValue, stopOnError = true) |>>>
      Iteratee.fold[Game, PerfStat](PerfStat.init(user.id, perfType)) {
        case (perfStat, game) if game.perfType.contains(perfType) =>
          Pov.ofUserId(game, user.id).fold(perfStat)(perfStat.agg)
        case (perfStat, _) => perfStat
      }
  } flatMap storage.insert

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
