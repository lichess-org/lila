package lila.chat

import scala.concurrent.Future

import play.api.libs.json._

import lila.pref.Pref.ChatPref
import lila.user.User

case class ChatHead(
    chans: List[Chan],
    pageChanKey: Option[String],
    activeChanKeys: Set[String],
    mainChanKey: Option[String]) {

  def chanKeys = chans map (_.key)

  def setChan(c: Chan, value: Boolean) = if (value) {
    if (chans contains c) this else copy(chans = c :: chans).sorted
  }
  else {
    if (chans contains c) copy(chans = chans filterNot (c==)) else this
  }

  def withPageChan(c: Chan) = setChan(c, true).copy(
    pageChanKey = c.key.some,
    activeChanKeys = c.autoActive.fold(activeChanKeys + c.key, activeChanKeys),
    mainChanKey = c.autoActive.fold(c.key.some, mainChanKey))

  def setActiveChanKey(key: String, value: Boolean) =
    copy(activeChanKeys = if (value) activeChanKeys + key else activeChanKeys - key)

  def setMainChanKey(key: Option[String]) = copy(mainChanKey = key)

  def join(c: Chan) = setChan(c, true).setActiveChanKey(c.key, true).setMainChanKey(c.key.some)

  def updatePref(pref: ChatPref) = ChatPref(
    on = pref.on,
    chans = chans filterNot (_.contextual) map (_.key),
    activeChans = activeChanKeys filterNot Chan.autoActive,
    mainChan = (mainChanKey filterNot Chan.autoActive) orElse pref.mainChan)

  def sorted = copy(chans = chans.sorted)
}

object ChatHead {

  def apply(pref: ChatPref): ChatHead = ChatHead(
    chans = pref.chans.map(Chan.parse).flatten,
    pageChanKey = none,
    activeChanKeys = pref.activeChans,
    mainChanKey = pref.mainChan).sorted
}

case class Chat(head: ChatHead, namedChans: List[NamedChan], lines: List[Line]) {

  def toJson = Json.obj(
    "lines" -> lines.map(_.toJson),
    "head" -> Json.obj(
      "chans" -> JsObject(namedChans map { c ⇒ c.chan.key -> c.toJson }),
      "pageChan" -> head.pageChanKey.fold[JsValue](JsNull)(JsString.apply),
      "activeChans" -> head.activeChanKeys,
      "mainChan" -> head.mainChanKey.filter(head.activeChanKeys.contains)))
}

object Chat {

  val maxChans = 7

  val systemUsername = "Lichess"
}
