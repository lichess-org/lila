package lila

import play.api.libs.iteratee._
import play.api.libs.json._

package object socket
  extends PackageObject with WithPlay with socket.WithSocket
