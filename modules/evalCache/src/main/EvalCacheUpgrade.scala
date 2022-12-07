package lila.evalCache

import play.api.libs.json.{ JsObject, JsString }

import scala.concurrent.duration.*
import chess.format.Fen
import chess.variant.Variant
import lila.socket.Socket
import lila.memo.ExpireCallbackMemo

import scala.collection.mutable
import lila.memo.SettingStore

/* Upgrades the user's eval when a better one becomes available,
 * by remembering the last evalGet of each socket member,
 * and listening to new evals stored.
 */
final private class EvalCacheUpgrade(setting: SettingStore[Boolean], scheduler: akka.actor.Scheduler)(using
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
):
  import EvalCacheUpgrade.*

  private val members       = mutable.AnyRefMap.empty[SriString, WatchingMember]
  private val evals         = mutable.AnyRefMap.empty[SetupId, EvalState]
  private val expirableSris = ExpireCallbackMemo[Socket.Sri](scheduler, 10 minutes, expire)

  private val upgradeMon = lila.mon.evalCache.upgrade

  def register(sri: Socket.Sri, variant: Variant, fen: Fen.Epd, multiPv: Int, path: String)(
      push: Push
  ): Unit =
    if (setting.get())
      members get sri.value foreach { wm =>
        unregisterEval(wm.setupId, sri)
      }
      val setupId = makeSetupId(variant, fen, multiPv)
      members += (sri.value -> WatchingMember(push, setupId, path))
      evals += (setupId     -> evals.get(setupId).fold(EvalState(Set(sri.value), 0))(_ addSri sri))
      expirableSris put sri

  def onEval(input: EvalCacheEntry.Input, sri: Socket.Sri): Unit = if (setting.get())
    (1 to input.eval.multiPv) flatMap { multiPv =>
      val setupId = makeSetupId(input.id.variant, input.fen, multiPv)
      evals get setupId map (setupId -> _)
    } filter {
      _._2.depth < input.eval.depth
    } foreach { case (setupId, eval) =>
      evals += (setupId -> eval.copy(depth = input.eval.depth))
      val wms = eval.sris.withFilter(sri.value !=) flatMap members.get
      if (wms.nonEmpty)
        val json = JsonHandlers.writeEval(input.eval, input.fen)
        wms foreach { wm =>
          wm.push(json + ("path" -> JsString(wm.path)))
        }
        upgradeMon.count.increment(wms.size)
    }

  private def expire(sri: Socket.Sri): Unit =
    members get sri.value foreach { wm =>
      unregisterEval(wm.setupId, sri)
      members -= sri.value
    }

  private def unregisterEval(setupId: SetupId, sri: Socket.Sri): Unit =
    evals get setupId foreach { eval =>
      val newSris = eval.sris - sri.value
      if (newSris.isEmpty) evals -= setupId
      else evals += (setupId -> eval.copy(sris = newSris))
    }

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    upgradeMon.members.update(members.size)
    upgradeMon.evals.update(evals.size)
    upgradeMon.expirable.update(expirableSris.count).unit
  }

private object EvalCacheUpgrade:

  private type SriString = String
  private type SetupId   = String
  private type Push      = JsObject => Unit

  private case class EvalState(sris: Set[SriString], depth: Int):
    def addSri(sri: Socket.Sri) = copy(sris = sris + sri.value)

  private def makeSetupId(variant: Variant, fen: Fen.Epd, multiPv: Int): SetupId =
    s"${variant.id}${SmallFen.make(variant, fen.simple)}^$multiPv"

  private case class WatchingMember(push: Push, setupId: SetupId, path: String)
