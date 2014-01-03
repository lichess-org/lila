package lila.chat

import akka.actor.ActorRef
import play.api.libs.json.JsValue

import lila.socket.JsChannel

private[chat] final class ChatMember(
    val uid: String,
    val userId: String,
    val troll: Boolean,
    val channel: JsChannel) {

  private[chat] var head: ChatHead = ChatHead(Nil, none, Set.empty, none)
  private[chat] var blocks: Set[String] = Set.empty

  def wants(line: Line) = line.chan match {
    case c: UserChan if c.contains(userId) ⇒ true
    case _ ⇒ (troll || !line.troll) && (head sees line) && !blocks(line.userId)
  }

  def hasActiveChan = head.activeChanKeys contains _

  def tell(msg: JsValue) {
    channel push msg
  }

  def setHead(h: ChatHead) {
    head = h
  }

  def updateHead(f: ChatHead => ChatHead) {
    head = f(head)
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
