package lila

import lila.socket.WithSocket

package object pool extends PackageObject with WithPlay with WithSocket {

  private[pool] type ID = String
}
