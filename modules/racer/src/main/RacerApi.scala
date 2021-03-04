package lila.racer

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.puzzle.Puzzle
import lila.socket.Socket
import lila.storm.StormPuzzle
import lila.storm.StormSelector
import lila.user.User

final class RacerApi(colls: RacerColls, selector: StormSelector, cacheApi: CacheApi)(implicit
    ec: ExecutionContext
) {

  import RacerRace.Id
  import RacerBsonHandlers._

  private val store = cacheApi.notLoadingSync[RacerRace.Id, RacerRace](512, "racer.race")(
    _.expireAfterWrite(1 hour).build()
  )

  def get(id: Id): Option[RacerRace] = store getIfPresent id

  def playerId(sessionId: String, user: Option[User]) = user match {
    case Some(u) => RacerPlayer.Id.User(u.id)
    case None    => RacerPlayer.Id.Anon(sessionId)
  }

  def create(player: RacerPlayer.Id): Fu[RacerRace] =
    selector.apply map { puzzles =>
      val race = RacerRace.make(
        owner = player,
        puzzles = puzzles.grouped(2).flatMap(_.headOption).toList
      )
      store.put(race.id, race)
      race
    }

  def join(id: RacerRace.Id, player: RacerPlayer.Id): Option[RacerRace] =
    get(id) map { race =>
      race.join(player) match {
        case Some(joined) =>
          store.put(joined.id, joined)
          joined
        case None => race
      }
    }
}
