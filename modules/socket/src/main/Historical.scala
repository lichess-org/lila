package lila.socket

import play.api.libs.json._

trait Historical[M <: SocketMember] { self: SocketActor[M] =>

  val history: History

  def notifyVersion[A: Writes](t: String, data: A, troll: Boolean = false) {
    val vmsg = history.+=(makeMessage(t, data), troll)
    val send = sendMessage(vmsg) _
    members.values.foreach(send)
  }

  def sendMessage(message: History.Message)(member: M) {
    member.channel push {
      if (message.troll && !member.troll) message.skipMsg
      else message.fullMsg
    }
  }
  def sendMessage(member: M)(message: History.Message) {
    sendMessage(message)(member)
  }
}
