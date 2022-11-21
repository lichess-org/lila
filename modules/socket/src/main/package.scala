package lila.socket

import scala.concurrent.Promise
import play.api.libs.json.*
import alleycats.Zero

export lila.Lila.{ *, given }

opaque type SocketVersion <: Int = Int
object SocketVersion:
  def apply(v: Int): SocketVersion = v
  given Format[SocketVersion]      = lila.common.Json.intFormat
  given Zero[SocketVersion]        = Zero(SocketVersion(0))
extension (o: SocketVersion)
  def socketVersion: Int = o
  def incVersion         = SocketVersion(o + 1)

private type IsOnlineType            = String => Boolean
opaque type IsOnline <: IsOnlineType = IsOnlineType
object IsOnline:
  def apply(f: IsOnlineType): IsOnline = f

opaque type OnlineIds <: () => Set[String] = () => Set[String]
object OnlineIds:
  def apply(f: () => Set[String]): OnlineIds = f

case class GetVersion(promise: Promise[SocketVersion])

case class SendToFlag(flag: String, message: JsObject)
