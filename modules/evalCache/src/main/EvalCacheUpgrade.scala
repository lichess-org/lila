package lila.evalCache

import play.api.libs.json.{ JsObject, JsString }
import scala.collection.mutable.AnyRefMap
import scala.concurrent.duration._

import shogi.format.forsyth.Sfen
import shogi.variant.Variant
import lila.socket.Socket
import lila.memo.ExpireCallbackMemo

/* Upgrades the user's eval when a better one becomes available,
 * by remembering the last evalGet of each socket member,
 * and listening to new evals stored.
 */
final private class EvalCacheUpgrade(scheduler: akka.actor.Scheduler)(implicit
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
) {
  import EvalCacheUpgrade._

  private val members       = AnyRefMap.empty[SriString, WatchingMember]
  private val evals         = AnyRefMap.empty[SetupId, Set[SriString]]
  private val expirableSris = new ExpireCallbackMemo(20 minutes, sri => unregister(Socket.Sri(sri)))

  private val upgradeMon = lila.mon.evalCache.upgrade

  def register(sri: Socket.Sri, variant: Variant, sfen: Sfen, multiPv: Int, path: String)(
      push: Push
  ): Unit = {
    members get sri.value foreach { wm =>
      unregisterEval(wm.setupId, sri)
    }
    val setupId = makeSetupId(variant, sfen, multiPv)
    members += (sri.value -> WatchingMember(push, setupId, path))
    evals += (setupId     -> (~evals.get(setupId) + sri.value))
    expirableSris put sri.value
  }

  def onEval(input: EvalCacheEntry.Input, sri: Option[Socket.Sri]): Unit = {
    (1 to input.eval.multiPv) flatMap { multiPv =>
      evals get makeSetupId(input.id.variant, input.sfen, multiPv)
    } foreach { sris =>
      val wms = sris.filter { s => sri.map(s != _.value).getOrElse(true) } flatMap members.get
      if (wms.nonEmpty) {
        val json = JsonHandlers.writeEval(input.eval, input.sfen)
        wms foreach { wm =>
          wm.push(json + ("path" -> JsString(wm.path)))
        }
        upgradeMon.count.increment(wms.size)
      }
    }
  }

  def unregister(sri: Socket.Sri): Unit =
    members get sri.value foreach { wm =>
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

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    upgradeMon.members.update(members.size)
    upgradeMon.evals.update(evals.size)
    upgradeMon.expirable.update(expirableSris.count).unit
  }
}

private object EvalCacheUpgrade {

  private type SriString = String
  private type SetupId   = String
  private type Push      = JsObject => Unit

  private def makeSetupId(variant: Variant, sfen: Sfen, multiPv: Int): SetupId =
    s"${variant.id}${EvalCacheEntry.SmallSfen.make(variant, sfen).value}^$multiPv"

  private case class WatchingMember(push: Push, setupId: SetupId, path: String)
}
