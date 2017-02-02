package lila.evalCache

import lila.socket._
import lila.user.User

final class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster) {

  import EvalCacheEntry._

  def apply(member: SocketMember, user: Option[User]): Handler.Controller =
    makeController(member, user map truster.makeTrusted)

  private def makeController(member: SocketMember, trustedUser: Option[TrustedUser]): Handler.Controller = {
    case ("evalPut", o) => trustedUser foreach { tu =>
      JsonHandlers.readPut(tu.user, o) foreach { api.put(tu, _) }
    }
    // case ("evalGet", o) => JsonHandlers.readGet(o) foreach { put(trustedUser, _) }
  }
}
