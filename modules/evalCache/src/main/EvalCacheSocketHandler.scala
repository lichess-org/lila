package lila.evalCache

import play.api.libs.json.*

import chess.variant.Variant
import chess.format.Fen
import lila.socket.*
import lila.user.User
import lila.common.Json.given

final private class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade
)(using scala.concurrent.ExecutionContext):

  def evalGet(
      sri: Socket.Sri,
      d: JsObject,
      push: JsObject => Unit
  ): Unit =
    for {
      fen <- d.get[Fen.Epd]("fen")
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
      multiPv = (d int "mpv") | 1
      path <- d str "path"
    } yield {
      def pushData(data: JsObject) = push(Socket.makeMessage("evalHit", data))
      api.getEvalJson(variant, fen, multiPv) foreach {
        _ foreach { json =>
          pushData(json + ("path" -> JsString(path)))
        }
      }
      if (d.value contains "up") upgrade.register(sri, variant, fen, multiPv, path)(pushData)
    }

  def untrustedEvalPut(sri: Socket.Sri, userId: UserId, data: JsObject): Unit =
    truster cachedTrusted userId foreach {
      _ foreach { tu =>
        JsonHandlers.readPutData(tu, data) foreach {
          api.put(tu, _, sri)
        }
      }
    }
