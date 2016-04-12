package lila

import lila.socket.WithSocket

package object study extends PackageObject with WithPlay with WithSocket {

  private[study] val logger = lila.log("study")
}
