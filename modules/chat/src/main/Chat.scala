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

  def prependChan(c: Chan) = if (chans contains c) this else copy(chans = c :: chans)
  def appendChan(c: Chan) = if (chans contains c) this else copy(chans = chans :+ c)

  def withPageChan(c: Chan) = copy(
    chans = (chans :+ c).distinct,
    pageChanKey = c.key.some,
    activeChanKeys = c.autoActive.fold(activeChanKeys + c.key, activeChanKeys),
    mainChanKey = c.autoActive.fold(c.key.some, mainChanKey))

  def setActiveChanKey(key: String, value: Boolean) =
    copy(activeChanKeys = if (value) activeChanKeys + key else activeChanKeys - key)
  def setMainChanKey(key: Option[String]) = copy(mainChanKey = key)

  def join(c: Chan) = appendChan(c).setActiveChanKey(c.key, true).setMainChanKey(c.key.some)

  def updatePref(pref: ChatPref) = ChatPref(
    on = pref.on,
    chans = chans map (_.key),
    activeChans = activeChanKeys filterNot Chan.autoActive,
    mainChan = (mainChanKey filterNot Chan.autoActive) orElse pref.mainChan)
}

object ChatHead {

  def apply(pref: ChatPref): ChatHead = ChatHead(
    chans = pref.chans.map(Chan.parse).flatten,
    pageChanKey = none,
    activeChanKeys = pref.activeChans,
    mainChanKey = pref.mainChan)
}

case class Chat(head: ChatHead, namedChans: List[NamedChan], lines: List[Line]) {

  def toJson = Json.obj(
    "lines" -> lines.map(_.toJson),
    "chans" -> JsObject(namedChans map { c ⇒ c.chan.key -> c.toJson }),
    "pageChan" -> head.pageChanKey.fold[JsValue](JsNull)(JsString.apply),
    "activeChans" -> head.activeChanKeys,
    "mainChan" -> head.mainChanKey.filter(head.activeChanKeys.contains))
}

object Chat {

  val maxChans = 7

  val systemUsername = "Lichess"
}
