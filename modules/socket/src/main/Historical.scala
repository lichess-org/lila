package lila.socket

import play.api.libs.iteratee._
import play.api.libs.json._

trait Historical[M <: SocketMember, Metadata] { self: SocketTrouper[M] =>

  protected val history: History[Metadata]

  protected type Message = History.Message[Metadata]

  protected def shouldSkipMessageFor(message: Message, member: M): Boolean

  def notifyVersion[A: Writes](t: String, data: A, metadata: Metadata): Unit = {
    val vmsg = history.+=(makeMessage(t, data), metadata)
    val send = sendMessage(vmsg) _
    members foreachValue send
  }

  def filteredMessage(member: M)(message: Message) =
    if (shouldSkipMessageFor(message, member)) message.skipMsg
    else message.fullMsg

  def sendMessage(message: Message)(member: M): Unit =
    member push filteredMessage(member)(message)

  def sendMessage(member: M)(message: Message): Unit =
    member push filteredMessage(member)(message)

  protected def prependEventsSince(
    since: Option[Socket.SocketVersion],
    enum: Enumerator[JsValue],
    member: M
  ): Enumerator[JsValue] =
    lila.common.Iteratee.prepend(
      since
        .fold(history.getRecent(5).some)(history.since)
        .fold(List(SocketTrouper.resyncMessage))(_ map filteredMessage(member)),
      enum
    )
}
