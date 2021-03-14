package lila.racer

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import lila.user.User

final class RacerLobby(api: RacerApi)(implicit ec: ExecutionContext, system: akka.actor.ActorSystem) {

  def join(player: RacerPlayer.Id): Fu[RacerRace.Id] = workQueue {
    currentRace flatMap {
      case race if race.players.sizeIs >= RacerRace.maxPlayers => makeNewRaceFor(player)
      case race if race.startsInMillis.exists(_ < 4000)        => makeNewRaceFor(player)
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

  private val fallbackRace = RacerRace.make(RacerPlayer.lichess, Nil)

  private var currentId: Fu[RacerRace.Id] = api create RacerPlayer.lichess

  private def currentRace: Fu[RacerRace] = currentId.map(api.get) dmap { _ | fallbackRace }

  private def makeNewRaceFor(player: RacerPlayer.Id): Fu[RacerRace.Id] = {
    lila.mon.racer.lobbyRace.increment()
    currentId = api create RacerPlayer.lichess
    currentId
  }
}
