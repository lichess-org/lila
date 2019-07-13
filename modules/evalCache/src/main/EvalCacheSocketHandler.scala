package lila.evalCache

import chess.variant.Variant
import play.api.libs.json._

import chess.format.FEN
import lila.socket._
import lila.user.User

final class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade
) {

  import EvalCacheEntry._

  def apply(sri: Socket.Sri, member: SocketMember, user: Option[User]): Handler.Controller =
    makeController(sri, member, user map truster.makeTrusted)

  private def makeController(
    sri: Socket.Sri,
    member: SocketMember,
    trustedUser: Option[TrustedUser]
  ): Handler.Controller = {

    case ("evalPut", o) => trustedUser foreach { tu =>
      JsonHandlers.readPut(tu, o) foreach { api.put(tu, _, sri) }
    }

    case ("evalGet", o) => o obj "d" foreach { evalGet(sri, _, member.push) }
  }

  private[evalCache] def evalGet(
    sri: Socket.Sri,
    d: JsObject,
    push: JsObject => Unit
  ): Unit = for {
    fen <- d str "fen" map FEN.apply
    variant = Variant orDefault ~d.str("variant")
    multiPv = (d int "mpv") | 1
    path <- d str "path"
  } {
    def pushData(data: JsObject) = push(Socket.makeMessage("evalHit", data))
    api.getEvalJson(variant, fen, multiPv) foreach {
      _ foreach { json =>
        pushData(json + ("path" -> JsString(path)))
      }
    }
    if (d.value contains "up") upgrade.register(sri, variant, fen, multiPv, path)(pushData)
  }

  private[evalCache] def untrustedEvalPut(sri: Socket.Sri, userId: User.ID, data: JsObject): Unit =
    truster cachedTrusted userId foreach {
      _ foreach { tu =>
        JsonHandlers.readPutData(tu, data) foreach {
          api.put(tu, _, sri)
        }
      }
    }
}
