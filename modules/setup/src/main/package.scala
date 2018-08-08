package lidraughts

import lidraughts.socket.WithSocket

package object setup extends PackageObject with WithSocket {

  private[setup] def logger = lidraughts.log("setup")
}
