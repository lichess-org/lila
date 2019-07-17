package lila.lobby

import play.api.libs.json._

import lila.socket.RemoteSocket._, Protocol._

final class LobbyRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket
) {

  private val send: (Path, Args*) => Unit = remoteSocketApi.sendTo("lobby-out") _

  private val handler: Handler = {
    case m @ In.ConnectSri(sri, userId) => remoteSocketApi baseHandler m
  }

  remoteSocketApi.subscribe("lobby-in")(handler orElse remoteSocketApi.baseHandler)
}
