package lila.evalCache

import shogi.variant.Variant
import play.api.libs.json._

import shogi.format.forsyth.Sfen
import lila.socket._
import lila.user.User

final private class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade
)(implicit ec: scala.concurrent.ExecutionContext) {

  def evalGet(
      sri: Socket.Sri,
      d: JsObject,
      push: JsObject => Unit
  ): Unit =
    for {
      sfen <- d str "sfen" map Sfen.apply
      variant = Variant orDefault ~d.str("variant")
      multiPv = (d int "mpv") | 1
      path <- d str "path"
    } {
      def pushData(data: JsObject) = push(Socket.makeMessage("evalHit", data))
      api.getEvalJson(variant, sfen, multiPv) foreach {
        _ foreach { json =>
          pushData(json + ("path" -> JsString(path)))
        }
      }
      if (d.value contains "up") upgrade.register(sri, variant, sfen, multiPv, path)(pushData)
    }

  def untrustedEvalPut(sri: Socket.Sri, userId: User.ID, data: JsObject): Unit =
    truster cachedTrusted userId foreach {
      _ foreach { tu =>
        JsonHandlers.readPutData(tu, data) foreach {
          api.put(tu, _, sri)
        }
      }
    }
}
