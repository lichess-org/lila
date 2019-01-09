package lila.evalCache

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

  def apply(uid: Socket.Uid, member: SocketMember, user: Option[User]): Handler.Controller =
    makeController(uid, member, user map truster.makeTrusted)

  private def makeController(
    uid: Socket.Uid,
    member: SocketMember,
    trustedUser: Option[TrustedUser]
  ): Handler.Controller = {

    case ("evalPut", o) => trustedUser foreach { tu =>
      JsonHandlers.readPut(tu, o) foreach { api.put(tu, _, uid) }
    }

    case ("evalGet", o) => for {
      d <- o obj "d"
      variant = chess.variant.Variant orDefault ~d.str("variant")
      fen <- d str "fen" map FEN.apply
      multiPv = (d int "mpv") | 1
      path <- d str "path"
    } {
      api.getEvalJson(variant, fen, multiPv) foreach {
        _ foreach { json =>
          member push Socket.makeMessage("evalHit", json + ("path" -> JsString(path)))
        }
      }
      if (d.value contains "up")
        upgrade.register(uid, member, variant, fen, multiPv, path)
    }
  }
}
