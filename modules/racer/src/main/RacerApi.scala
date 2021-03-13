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
    ec: ExecutionContext,
    system: akka.actor.ActorSystem
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
      val race = RacerRace
        .make(
          owner = player,
          puzzles = puzzles.grouped(2).flatMap(_.headOption).toList
        )
      store.put(race.id, race)
      race
    }

  private val rematchQueue =
    new lila.hub.DuctSequencer(
      maxSize = 32,
      timeout = 20 seconds,
      name = "racer.rematch"
    )

  def rematch(race: RacerRace, player: RacerPlayer.Id): Fu[RacerRace.Id] = race.rematch.flatMap(get) match {
    case Some(found) if found.finished => rematch(found, player)
    case Some(found) =>
      join(found.id, player)
      fuccess(found.id)
    case None =>
      rematchQueue {
        create(player) map { rematch =>
          save(race.copy(rematch = rematch.id.some))
          rematch.id
        }
      }
  }

  def join(id: RacerRace.Id, player: RacerPlayer.Id): Unit =
    get(id).flatMap(_ join player) foreach saveAndPublish

  def registerPlayerMoves(id: RacerRace.Id, player: RacerPlayer.Id, moves: Int): Unit =
    get(id).map(_.registerMoves(player, moves)) foreach saveAndPublish

  def playerEnd(id: RacerRace.Id, player: RacerPlayer.Id): Unit =
    get(id).map(_ end player) foreach saveAndPublish

  private def save(race: RacerRace): Unit =
    store.put(race.id, race)

  private def saveAndPublish(race: RacerRace): Unit = {
    save(race)
    socket.foreach(_ publishState race)
  }

  // work around circular dependency
  private var socket: Option[RacerSocket] = None
  private[racer] def registerSocket(s: RacerSocket) = { socket = s.some }
}
