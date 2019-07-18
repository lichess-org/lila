package lila.site

import play.api.libs.json._

import lila.socket.RemoteSocket._

final class SiteRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket
) {

  remoteSocketApi.subscribe("site-in", Protocol.In.baseReader)(remoteSocketApi.baseHandler)
}
