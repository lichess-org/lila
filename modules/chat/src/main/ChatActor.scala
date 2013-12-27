package lila.chat

import akka.actor._

import lila.common.Bus
import lila.common.PimpedJson._
import lila.hub.actorApi.chat._

private[chat] final class ChatActor(api: Api, bus: Bus) extends Actor {

  bus.subscribe(self, 'chatInput)

  override def postStop() {
    bus.unsubscribe(self)
  }

  def receive = {

    case Tell(userId, o) ⇒ for {
      d ← o obj "d"
      chan ← d str "chan"
      text ← d str "text"
    } api.write(chan, userId, text) foreach {
      _ foreach { line ⇒ bus.publish(line, 'chatOutput) }
    }
  }
}
