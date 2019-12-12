package lila.evalCache

import play.api.libs.json.{ JsString, JsObject }
import scala.collection.mutable.AnyRefMap
import scala.concurrent.duration._

import chess.format.FEN
import chess.variant.Variant
import lila.socket.Socket
import lila.memo.ExpireCallbackMemo

/* Upgrades the user's eval when a better one becomes available,
 * by remembering the last evalGet of each socket member,
 * and listening to new evals stored.
 */
private final class EvalCacheUpgrade {
  import EvalCacheUpgrade._

  private val members = AnyRefMap.empty[SriString, WatchingMember]
  private val evals = AnyRefMap.empty[SetupId, Set[SriString]]
  private val expirableSris = new ExpireCallbackMemo(20 minutes, sri => unregister(Socket.Sri(sri)))

  def register(sri: Socket.Sri, variant: Variant, fen: FEN, multiPv: Int, path: String)(push: Push): Unit = {
    members get sri.value foreach { wm =>
      unregisterEval(wm.setupId, sri)
    }
    val setupId = makeSetupId(variant, fen, multiPv)
    members += (sri.value -> WatchingMember(push, setupId, path))
    evals += (setupId -> (~evals.get(setupId) + sri.value))
    expirableSris put sri.value
  }

  def onEval(input: EvalCacheEntry.Input, sri: Socket.Sri): Unit = {
    (1 to input.eval.multiPv) flatMap { multiPv =>
      evals get makeSetupId(input.id.variant, input.fen, multiPv)
    } foreach { sris =>
      val wms = sris.filter(sri.value !=) flatMap members.get
      if (wms.nonEmpty) {
        val json = JsonHandlers.writeEval(input.eval, input.fen)
        wms foreach { wm =>
          wm.push(json + ("path" -> JsString(wm.path)))
        }
        lila.mon.evalCache.upgrade.hit(wms.size)
        lila.mon.evalCache.upgrade.members(members.size)
        lila.mon.evalCache.upgrade.evals(evals.size)
        lila.mon.evalCache.upgrade.expirable(expirableSris.count)
      }
    }
  }

  def unregister(sri: Socket.Sri): Unit = members get sri.value foreach { wm =>
    unregisterEval(wm.setupId, sri)
    members -= sri.value
    expirableSris remove sri.value
  }

  private def unregisterEval(setupId: SetupId, sri: Socket.Sri): Unit =
    evals get setupId foreach { sris =>
      val newSris = sris - sri.value
      if (newSris.isEmpty) evals -= setupId
      else evals += (setupId -> newSris)
    }

}

private object EvalCacheUpgrade {

  private type SriString = String
  private type SetupId = String
  private type Push = JsObject => Unit

  private def makeSetupId(variant: Variant, fen: FEN, multiPv: Int): SetupId =
    s"${variant.id}${EvalCacheEntry.SmallFen.make(variant, fen).value}^$multiPv"

  private case class WatchingMember(push: Push, setupId: SetupId, path: String)
}
