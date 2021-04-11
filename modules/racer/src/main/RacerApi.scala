package lila.racer

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.memo.CacheApi
import lila.storm.StormSelector
import lila.user.{ User, UserRepo }
import lila.common.Bus

final class RacerApi(colls: RacerColls, selector: StormSelector, userRepo: UserRepo, cacheApi: CacheApi)(
    implicit
    ec: ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import RacerRace.Id

  private val store = cacheApi.notLoadingSync[RacerRace.Id, RacerRace](2048, "racer.race")(
    _.expireAfterAccess(30 minutes).build()
  )

  def get(id: Id): Option[RacerRace] = store getIfPresent id

  def playerId(sessionId: String, user: Option[User]) = user match {
    case Some(u) => RacerPlayer.Id.User(u.id)
    case None    => RacerPlayer.Id.Anon(sessionId)
  }

  def createAndJoin(player: RacerPlayer.Id): Fu[RacerRace.Id] =
    create(player, 10).map { id =>
      join(id, player)
      id
    }

  def create(player: RacerPlayer.Id, countdownSeconds: Int): Fu[RacerRace.Id] =
    selector.apply map { puzzles =>
      val race = RacerRace
        .make(
          owner = player,
          puzzles = puzzles.grouped(2).flatMap(_.headOption).toList,
          countdownSeconds = 10
        )
      store.put(race.id, race)
      lila.mon.racer.race(lobby = race.isLobby).increment()
      race.id
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
        createAndJoin(player) map { rematchId =>
          save(race.copy(rematch = rematchId.some))
          rematchId
        }
      }
  }

  def join(id: RacerRace.Id, player: RacerPlayer.Id): Option[RacerRace] =
    get(id).flatMap(_ join player) map { r =>
      val race = start(r) | r
      saveAndPublish(race)
      race
    }

  private def start(race: RacerRace): Option[RacerRace] = race.startCountdown.map { starting =>
    system.scheduler.scheduleOnce(RacerRace.duration.seconds + race.countdownSeconds.seconds + 50.millis) {
      finish(race.id)
    }
    starting
  }

  private def finish(id: RacerRace.Id): Unit =
    get(id) foreach { race =>
      lila.mon.racer.players(lobby = race.isLobby).record(race.players.size)
      race.players foreach { player =>
        lila.mon.racer.score(lobby = race.isLobby, auth = player.userId.isDefined).record(player.score)
        player.userId.ifTrue(player.score > 0) foreach { userId =>
          Bus.publish(lila.hub.actorApi.puzzle.RacerRun(userId, player.score), "racerRun")
          userRepo.addRacerRun(userId, player.score)
        }
      }
      publish(race)
    }

  def registerPlayerScore(id: RacerRace.Id, player: RacerPlayer.Id, score: Int): Unit = {
    if (score >= 125) logger.warn(s"$id $player score: $score")
    else get(id).flatMap(_.registerScore(player, score)) foreach saveAndPublish
  }

  private def save(race: RacerRace): Unit =
    store.put(race.id, race)

  private def saveAndPublish(race: RacerRace): Unit = {
    save(race)
    publish(race)
  }
  private def publish(race: RacerRace): Unit = {
    socket.foreach(_ publishState race)
  }

  // work around circular dependency
  private var socket: Option[RacerSocket] = None
  private[racer] def registerSocket(s: RacerSocket) = { socket = s.some }
}
