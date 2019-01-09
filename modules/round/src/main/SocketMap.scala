package lila.round

import scala.concurrent.duration._

import lila.game.Game
import lila.hub.actorApi.map.{ Tell, TellIfExists, Exists }

private object SocketMap {

  def make(
    makeHistory: (String, Boolean) => History,
    dependencies: RoundSocket.Dependencies,
    socketTimeout: FiniteDuration
  ): SocketMap = {

    var historyPersistenceEnabled = false

    import dependencies._
    val bus = system.lilaBus

    lazy val socketMap: SocketMap = lila.socket.SocketMap[RoundSocket](
      system = system,
      mkTrouper = (id: Game.ID) => new RoundSocket(
        dependencies = dependencies,
        gameId = id,
        history = makeHistory(id, historyPersistenceEnabled),
        keepMeAlive = () => socketMap touch id
      ),
      accessTimeout = socketTimeout,
      monitoringName = "round.socketMap",
      broomFrequency = 4001 millis
    )
    bus.subscribeFuns(
      'startGame -> {
        case msg: lila.game.actorApi.StartGame => socketMap.tellIfPresent(msg.game.id, msg)
      },
      'roundSocket -> {
        case TellIfExists(id, msg) => socketMap.tellIfPresent(id, msg)
        case Tell(id, msg) => socketMap.tell(id, msg)
        case Exists(id, promise) => promise success socketMap.exists(id)
      },
      'deploy -> {
        case m: lila.hub.actorApi.Deploy =>
          socketMap tellAll m
          logger.warn("Enable history persistence")
          historyPersistenceEnabled = true
          // if the deploy didn't go through, cancel persistence
          system.scheduler.scheduleOnce(10.minutes) {
            logger.warn("Disabling round history persistence!")
            historyPersistenceEnabled = false
          }
      }
    )
    socketMap
  }
}
