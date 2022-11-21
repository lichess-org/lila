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

final class IsOnline(f: (String => Boolean)) extends (String => Boolean):
  def apply(id: String) = f(id)

final class OnlineIds(f: () => Set[String]) extends (() => Set[String]):
  def apply() = f()

case class GetVersion(promise: Promise[SocketVersion])

case class SendToFlag(flag: String, message: JsObject)
