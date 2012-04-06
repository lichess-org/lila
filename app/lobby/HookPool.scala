package lila
package lobby

import memo.HookMemo

import akka.actor._

import play.api.libs.json._
import play.api.libs.iteratee._

final class HookPool(hookMemo: HookMemo) extends Actor {

  private val ownerIds = scala.collection.mutable.Set.empty[String]

  def receive = {

    case HookPool.Register(id)   ⇒ { ownerIds add id }

    case HookPool.Unregister(id) ⇒ { ownerIds remove id }

    case Tick ⇒ {
      (ownerIds.toList map hookMemo.put).sequence.unsafePerformIO
    }
  }
}

object HookPool {

  case class Register(id: String)
  case class Unregister(id: String)
}
