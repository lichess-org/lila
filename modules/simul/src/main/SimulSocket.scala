package lila.simul

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.socket.{ protocol as P, * }
import lila.room.RoomSocket.{ Protocol as RP, * }

final private class SimulSocket(
    repo: SimulRepo,
    jsonView: JsonView,
    socketKit: SocketKit,
    socketRequest: SocketRequester,
    chat: lila.core.chat.ChatApi
)(using Executor, lila.core.user.FlairGet):

  def hostIsOn(simulId: SimulId, gameId: GameId): Unit =
    rooms.tell(simulId.into(RoomId), NotifyVersion("hostGame", gameId.value))

  def reload(simulId: SimulId): Unit =
    repo
      .find(simulId)
      .foreach:
        _.foreach: simul =>
          jsonView(simul, simul.conditions.accepted).foreach: obj =>
            rooms.tell(simulId.into(RoomId), NotifyVersion("reload", obj))

  def aborted(simulId: SimulId): Unit =
    rooms.tell(simulId.into(RoomId), NotifyVersion("aborted", Json.obj()))

  def startSimul(simul: Simul, firstGame: Game): Unit =
    firstGame.player(simul.hostId).foreach { player =>
      redirectPlayer(simul, Pov(firstGame, player.color))
    }

  def startGame(simul: Simul, game: Game): Unit =
    game
      .player(simul.hostId)
      .foreach: opponent =>
        redirectPlayer(simul, Pov(game, !opponent.color))

  def filterPresent(simul: Simul, userIds: Set[UserId]): Fu[Seq[UserId]] =
    socketRequest[Seq[UserId]](
      id => send.exec(SimulSocket.Protocol.Out.filterPresent(id, simul.id, userIds)),
      userIds => UserId.from(P.In.commas(userIds).toSeq)
    )

  private def redirectPlayer(simul: Simul, pov: Pov): Unit =
    pov.player.userId.foreach: userId =>
      send.exec(RP.Out.tellRoomUser(simul.id.into(RoomId), userId, makeMessage("redirect", pov.fullId)))

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.simul)

  private lazy val handler: SocketHandler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Simul(roomId.into(SimulId)).some,
      chatBusChan = _.simul,
      localTimeout = Some: (roomId, modId, _) =>
        repo.hostId(roomId.into(SimulId)).map(_.has(modId))
    )

  private lazy val send = socketKit.send("simul-out")

  socketKit
    .subscribe("simul-in", RP.In.reader)(handler.orElse(socketKit.baseHandler))
    .andDo(send.exec(P.Out.boot))

private object SimulSocket:
  object Protocol:
    object Out:
      def filterPresent(reqId: Int, simulId: SimulId, userIds: Set[UserId]) =
        s"room/filter-present $reqId $simulId ${P.Out.commas(userIds)}"
