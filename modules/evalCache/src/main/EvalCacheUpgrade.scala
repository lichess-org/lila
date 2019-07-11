package lila.evalCache

import play.api.libs.json.{ JsString, JsObject }
import scala.collection.mutable.AnyRefMap

import chess.format.FEN
import chess.variant.Variant
import lila.socket.{ Socket, SocketMember }

/* Upgrades the user's eval when a better one becomes available,
 * by remembering the last evalGet of each socket member,
 * and listening to new evals stored.
 */
private final class EvalCacheUpgrade(asyncCache: lila.memo.AsyncCache.Builder) {
  import EvalCacheUpgrade._

  private val members = AnyRefMap.empty[UidString, WatchingMember]
  private val evals = AnyRefMap.empty[SetupId, Set[UidString]]

  def register(uid: Socket.Uid, variant: Variant, fen: FEN, multiPv: Int, path: String)(push: Push): Unit = {
    members get uid.value foreach { wm =>
      unregisterEval(wm.setupId, uid)
    }
    val setupId = makeSetupId(variant, fen, multiPv)
    members += (uid.value -> WatchingMember(push, setupId, path))
    evals += (setupId -> (~evals.get(setupId) + uid.value))
  }

  def onEval(input: EvalCacheEntry.Input, uid: Socket.Uid): Unit = {
    (1 to input.eval.multiPv) flatMap { multiPv =>
      evals get makeSetupId(input.id.variant, input.fen, multiPv)
    } foreach { uids =>
      val wms = uids.filter(uid.value !=) flatMap members.get
      if (wms.nonEmpty) {
        val json = JsonHandlers.writeEval(input.eval, input.fen)
        wms foreach { wm =>
          wm push Socket.makeMessage("evalHit", json + ("path" -> JsString(wm.path)))
        }
        lila.mon.evalCache.upgrade.hit(wms.size)
        lila.mon.evalCache.upgrade.members(members.size)
        lila.mon.evalCache.upgrade.evals(evals.size)
      }
    }
  }

  def unregister(uid: Socket.Uid): Unit = members get uid.value foreach { wm =>
    unregisterEval(wm.setupId, uid)
    members -= uid.value
  }

  private def unregisterEval(setupId: SetupId, uid: Socket.Uid): Unit =
    evals get setupId foreach { uids =>
      val newUids = uids - uid.value
      if (newUids.isEmpty) evals -= setupId
      else evals += (setupId -> newUids)
    }

}

private object EvalCacheUpgrade {

  private type UidString = String
  private type SetupId = String
  private type Push = JsObject => Unit

  private def makeSetupId(variant: Variant, fen: FEN, multiPv: Int): SetupId =
    s"${variant.id}${EvalCacheEntry.SmallFen.make(variant, fen).value}^$multiPv"

  private case class WatchingMember(push: Push, setupId: SetupId, path: String)
}
