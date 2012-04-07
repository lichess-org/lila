package lila
package socket

import memo.SocketMemo

import akka.actor._

final class Pool(socketMemo: SocketMemo) extends Actor {

  private val uids = scala.collection.mutable.Set.empty[String]

  def receive = {

    case Pool.Register(uid)   ⇒ {
      uids add uid
      (socketMemo put uid).unsafePerformIO
    }

    case Pool.Unregister(uid) ⇒ {
      uids remove uid
      (socketMemo remove uid).unsafePerformIO
    }

    case Tick ⇒ {
      (uids.toList map socketMemo.put).sequence.unsafePerformIO
    }
  }
}

object Pool {
  case class Register(id: String)
  case class Unregister(id: String)
}

