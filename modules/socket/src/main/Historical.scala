package lila.socket

import play.api.libs.json._

trait Historical[M <: SocketMember] { self: SocketActor[M] =>

  val history: History

  def notifyVersion[A : Writes](t: String, data: A, troll: Boolean = false) {
    val vmsg = history += History.Message(makeMessage(t, data), troll)
    val send = sendMessage(vmsg) _
    members.values.foreach(send)
  }

  def sendMessage(message: History.Message)(member: M) {
    if (!message.troll || member.troll) member.channel push message.msg
  }
  def sendMessage(member: M)(message: History.Message) {
    sendMessage(message)(member)
  }
}
