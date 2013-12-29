package lila.chat

import akka.actor.ActorRef

private[chat] final class ChatMember(
    val uid: String,
    val userId: String,
    val troll: Boolean,
    val channel: lila.socket.JsChannel) {

  private[chat] var head: ChatHead = ChatHead(Nil, none, Set.empty, none)

  def wants(line: Line) =
    (troll || !line.troll) && (head.activeChanKeys contains line.chan.key)

  def setHead(h: ChatHead) {
    head = h
  }

  def setMainChan(chan: Option[String]) {
    head = head.copy(mainChanKey = chan)
  }

  def setActiveChan(key: String, value: Boolean) {
    head = head.copy(
      activeChanKeys = if (value) head.activeChanKeys + key else head.activeChanKeys - key
    )
  }
}
