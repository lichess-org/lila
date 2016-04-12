package lila

import lila.socket.WithSocket

package object setup extends PackageObject with WithPlay with WithSocket {

  private[setup] def logger = lila.log("setup")
}
