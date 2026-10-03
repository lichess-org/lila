package lila.racer

import scalalib.model.Seconds

final class RacerLobby(api: RacerApi)(using Executor)(using scheduler: Scheduler):

  def join(player: RacerPlayer.Id): Fu[RacerRace.Id] = workQueue:
    currentRace
      .flatMap:
        case race if race.players.sizeIs >= RacerRace.maxPlayers => makeNewRace(Seconds(7))
        case race if race.startsInMillis.exists(_ < 3000) => makeNewRace()
        case race => fuccess(race.id)
      .map: raceId =>
        api.join(raceId, player)
        raceId

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(128),
    timeout = 20.seconds,
    name = "racer.lobby",
    lila.mon.asyncActorMonitor.full
  )

  private val fallbackRace = RacerRace.make(RacerPlayer.lichess, Nil, Seconds(10))

  private var currentId: Fu[RacerRace.Id] = api.create(RacerPlayer.lichess)

  private def currentRace: Fu[RacerRace] =
    currentId
      .recoverWith:
        case e: Exception =>
          logger.warn("RacerLobby.currentRace", e)
          makeNewRace()
      .map(api.get)
      .dmap(_ | fallbackRace)

  private def makeNewRace(countdown: Seconds = Seconds(10)): Fu[RacerRace.Id] =
    currentId = api.create(RacerPlayer.lichess, countdown)
    currentId
