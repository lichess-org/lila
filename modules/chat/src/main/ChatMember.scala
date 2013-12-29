package lila.chat

import akka.actor.ActorRef
import play.api.libs.json.JsValue

import lila.socket.JsChannel

private[chat] sealed trait ChatMember[Message] {

  val uid: String
  val userId: String

  def wants(line: Line): Boolean
  def tell(msg: Message): Unit

  private[chat] var head: ChatHead = ChatHead(Nil, none, Set.empty, none)
  private[chat] var blocks: Set[String] = Set.empty

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

  def setBlocks(bs: Set[String]) {
    blocks = bs
  }

  def block(u: String) {
    blocks = blocks + u
  }

  def unBlock(u: String) {
    blocks = blocks - u
  }
}

private[chat] final class JsChatMember(
    val uid: String,
    val userId: String,
    val troll: Boolean,
    val channel: JsChannel) extends ChatMember[JsValue] {

  def wants(line: Line) =
    (troll || !line.troll) &&
      (head.activeChanKeys contains line.chan.key) &&
      !blocks(line.userId) &&
      line.to.fold(true)(_ == userId)

  def tell(msg: JsValue) {
    channel push msg
  }
}

private[chat] final class BotChatMember(
    val uid: String,
    actor: ActorRef) extends ChatMember[Line] {

  val userId = uid
  val troll = false

  def wants(line: Line) = line.to == Some(userId)

  def tell(msg: Line) { actor ! msg }
}
