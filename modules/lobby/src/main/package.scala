package lila

import lila.socket.WithSocket

package object lobby extends PackageObject with WithPlay with WithSocket {

  private[lobby] def logger = lila.log("lobby")
}
