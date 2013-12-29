package lila.chat

import scala.concurrent.Future

import play.api.libs.json._

import lila.user.User

case class ChatHead(
    chans: List[Chan],
    pageChanKey: Option[String],
    activeChanKeys: Set[String],
    mainChanKey: Option[String]) {

  def chanKeys = chans map (_.key)

  def withPageChan(c: Chan) = copy(
    chans = (chans :+ c).distinct,
    pageChanKey = c.key.some,
    activeChanKeys = c.autoActive.fold(activeChanKeys + c.key, activeChanKeys),
    mainChanKey = c.autoActive.fold(c.key.some, mainChanKey))
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

  val baseChans = List(LichessChan)
}
