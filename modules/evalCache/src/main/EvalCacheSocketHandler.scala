package lidraughts.evalCache

import play.api.libs.json._

import draughts.format.FEN
import lidraughts.socket._
import lidraughts.user.User

final class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster
) {

  import EvalCacheEntry._

  def apply(member: SocketMember, user: Option[User]): Handler.Controller =
    makeController(member, user map truster.makeTrusted)

  private def makeController(member: SocketMember, trustedUser: Option[TrustedUser]): Handler.Controller = {

    case ("evalPut", o) => trustedUser foreach { tu =>
      JsonHandlers.readPut(tu, o) foreach { api.put(tu, _) }
    }

    case ("evalGet", o) => for {
      d <- o obj "d"
      variant = draughts.variant.Variant orDefault ~d.str("variant")
      fen <- d str "fen"
      multiPv = (d int "mpv") | 1
      path <- d str "path"
    } api.getEvalJson(variant, FEN(fen), multiPv) foreach {
      _ foreach { json =>
        member push Socket.makeMessage("evalHit", json + ("path" -> JsString(path)))
      }
    }
  }
}
