package lila.chat

import akka.actor.ActorRef

case class ChatMember(
    uid: String,
    userId: String,
    troll: Boolean,
    channel: lila.socket.JsChannel) {

  private var privateExtraChans = List[String]()
  private var privateActiveChans = Set[String]()

  def wants(line: Line) =
    (troll || !line.troll) && (privateActiveChans.pp contains line.chan.key.pp pp)

  def extraChans = privateExtraChans

  def setExtraChans(chans: List[String]) {
    privateExtraChans = chans
  }

  def setActiveChan(key: String, value: Boolean) {
    privateActiveChans =
      if (value) privateActiveChans + key
      else privateActiveChans - key
  }

  def setActiveChans(keys: Set[String]) {
    privateActiveChans = keys
  }
}
