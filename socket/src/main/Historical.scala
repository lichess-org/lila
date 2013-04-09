package lila.socket

import play.api.libs.json._

trait Historical[M <: SocketMember] { self: SocketActor[M] ⇒

  val history: History

  def notifyVersion[A : Writes](t: String, data: A) {
    val vmsg = history += makeMessage(t, data)
    members.values.foreach(_.channel push vmsg)
  }
}
