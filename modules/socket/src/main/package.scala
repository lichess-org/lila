package lila.socket

import play.api.libs.json.*
import alleycats.Zero

export lila.Lila.{ *, given }

private val logger = lila.log("socket")

opaque type SocketVersion = Int
object SocketVersion extends OpaqueInt[SocketVersion]:
  extension (o: SocketVersion) def incVersion = SocketVersion(o + 1)
  given Zero[SocketVersion]                   = Zero(0)

opaque type SocketSend = String => Unit
object SocketSend extends FunctionWrapper[SocketSend, String => Unit]

opaque type IsOnline = UserId => Boolean
object IsOnline extends FunctionWrapper[IsOnline, UserId => Boolean]

opaque type OnlineIds = () => Set[UserId]
object OnlineIds extends FunctionWrapper[OnlineIds, () => Set[UserId]]

case class GetVersion(promise: Promise[SocketVersion])
case class SendToFlag(flag: String, message: JsObject)
