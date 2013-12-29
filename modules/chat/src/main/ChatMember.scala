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

  def wants(line: Line) =
    (troll || !line.troll) &&
      (head.activeChanKeys contains line.chan.key) &&
      !blocks(line.userId)

  def tell(msg: JsValue) {
    channel push msg
  }

  def setHead(h: ChatHead) {
    head = h
  }

  def addChan(chan: Chan) {
    head = head addChan chan
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
