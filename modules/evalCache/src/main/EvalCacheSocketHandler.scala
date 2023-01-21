package lila.evalCache

import play.api.libs.json.*

import chess.variant.Variant
import chess.format.Fen
import lila.socket.*
import lila.user.User
import lila.common.Json.given
import lila.socket.Socket.Sri
import lila.common.Bus
import lila.hub.actorApi.socket.remote.{ TellSriOut, TellSrisOut }

final private class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade
)(using scala.concurrent.ExecutionContext):

  def evalGet(sri: Socket.Sri, d: JsObject): Unit =
    for
      fen <- d.get[Fen.Epd]("fen")
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
      multiPv = (d int "mpv") | 1
      path <- d str "path"
    yield
      api.getEvalJson(variant, fen, multiPv) foreach {
        _ foreach { json =>
          EvalCacheSocketHandler.pushHit(sri, json + ("path" -> JsString(path)))
        }
      }
      if (d.value contains "up") upgrade.register(sri, variant, fen, multiPv, path)

  def untrustedEvalPut(sri: Socket.Sri, userId: UserId, data: JsObject): Unit =
    truster cachedTrusted userId foreach {
      _ foreach { tu =>
        JsonHandlers.readPutData(tu, data) foreach {
          api.put(tu, _, sri)
        }
      }
    }

private object EvalCacheSocketHandler:

  def pushHit(sri: Sri, data: JsObject): Unit =
    Bus.publish(TellSriOut(sri.value, Socket.makeMessage("evalHit", data)), "remoteSocketOut")

  def pushHits(sris: Iterable[Sri], data: JsObject): Unit =
    Bus.publish(TellSrisOut(Sri raw sris, Socket.makeMessage("evalHit", data)), "remoteSocketOut")
