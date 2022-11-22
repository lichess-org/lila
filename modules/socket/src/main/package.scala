package lila.socket

import scala.concurrent.Promise
import play.api.libs.json.*
import alleycats.Zero

export lila.Lila.{ *, given }

opaque type SocketVersion = Int
object SocketVersion extends OpaqueInt[SocketVersion]:
  given Zero[SocketVersion]                   = Zero(SocketVersion(0))
  extension (o: SocketVersion) def incVersion = SocketVersion(o + 1)

opaque type SocketSend = String => Unit
object SocketSend extends TotalWrapper[SocketSend, String => Unit]

opaque type IsOnline = String => Boolean
object IsOnline extends TotalWrapper[IsOnline, String => Boolean]

opaque type OnlineIds = () => Set[String]
object OnlineIds extends TotalWrapper[OnlineIds, () => Set[String]]

case class GetVersion(promise: Promise[SocketVersion])
case class SendToFlag(flag: String, message: JsObject)
