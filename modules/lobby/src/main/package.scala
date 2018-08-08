package lidraughts

import lidraughts.socket.WithSocket

package object lobby extends PackageObject with WithSocket {

  private[lobby] def logger = lidraughts.log("lobby")
}
