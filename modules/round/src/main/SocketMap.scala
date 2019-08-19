package lidraughts.round

import scala.concurrent.duration._

import lidraughts.game.Game
import lidraughts.hub.actorApi.map.{ Tell, TellIfExists, Exists }
import lidraughts.hub.actorApi.Deploy

private object SocketMap {

  def make(
    makeHistory: Game.ID => History,
    dependencies: RoundSocket.Dependencies,
    socketTimeout: FiniteDuration
  ): SocketMap = {

    import dependencies._

    lazy val socketMap: SocketMap = lidraughts.socket.SocketMap[RoundSocket](
      system = system,
      mkTrouper = (id: Game.ID) => new RoundSocket(
        dependencies = dependencies,
        gameId = id,
        history = makeHistory(id),
        keepMeAlive = () => socketMap touch id
      ),
      accessTimeout = socketTimeout,
      monitoringName = "round.socketMap",
      broomFrequency = 4001 millis
    )
    system.lidraughtsBus.subscribeFuns(
      'startGame -> {
        case msg: lidraughts.game.actorApi.StartGame => socketMap.tellIfPresent(msg.game.id, msg)
      },
      'roundSocket -> {
        case TellIfExists(id, msg) => socketMap.tellIfPresent(id, msg)
        case Tell(id, msg) => socketMap.tell(id, msg)
        case Exists(id, promise) => promise success socketMap.exists(id)
      },
      'deploy -> {
        case m: Deploy => socketMap tellAll m
      }
    )
    socketMap
  }
}
