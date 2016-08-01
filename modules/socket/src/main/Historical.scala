package lila.socket

import play.api.libs.json._

trait Historical[M <: SocketMember, Metadata] { self: SocketActor[M] =>

  val history: History[Metadata]

  protected type Message = History.Message[Metadata]

  protected def shouldSkipMessageFor(message: Message, member: M): Boolean

  def notifyVersion[A: Writes](t: String, data: A, metadata: Metadata): Unit = {
    val vmsg = history.+=(makeMessage(t, data), metadata)
    val send = sendMessage(vmsg) _
    members.values foreach send
  }

  def sendMessage(message: Message)(member: M): Unit =
    member push {
      if (shouldSkipMessageFor(message, member)) message.skipMsg
      else message.fullMsg
    }

  def sendMessage(member: M)(message: Message): Unit =
    sendMessage(message)(member)
}
