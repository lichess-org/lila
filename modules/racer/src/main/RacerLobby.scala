package lila.racer

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import lila.user.User

final class RacerLobby(api: RacerApi)(implicit ec: ExecutionContext, system: akka.actor.ActorSystem) {

  def join(player: RacerPlayer.Id): Fu[RacerRace.Id] = workQueue {
    currentRace flatMap {
      case race if race.players.sizeIs >= RacerRace.maxPlayers => makeNewRace(7)
      case race if race.startsInMillis.exists(_ < 3000)        => makeNewRace(10)
      case race                                                => fuccess(race.id)
    } map { raceId =>
      api.join(raceId, player)
      raceId
    }
  }

  private val workQueue =
    new lila.hub.DuctSequencer(
      maxSize = 128,
      timeout = 20 seconds,
      name = "racer.lobby"
    )

  private val fallbackRace = RacerRace.make(RacerPlayer.lichess, Nil, 10)

  private var currentId: Fu[RacerRace.Id] = api.create(RacerPlayer.lichess, 10)

  private def currentRace: Fu[RacerRace] = currentId.map(api.get) dmap { _ | fallbackRace }

  private def makeNewRace(countdownSeconds: Int): Fu[RacerRace.Id] = {
    currentId = api.create(RacerPlayer.lichess, countdownSeconds)
    currentId
  }
}
