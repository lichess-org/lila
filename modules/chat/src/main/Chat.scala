package lila.chat

import scala.concurrent.Future

import play.api.libs.json._

import lila.user.User

case class Chat(
    user: User,
    lines: List[Line],
    chans: List[Chan],
    activeChanKeys: Set[String],
    mainChanKey: Option[String]) {

  def chanKeys = chans map (_.key)

  def addChans(cs: List[Chan]) = copy(chans = (chans ::: cs).distinct)
}

case class NamedChat(chat: Chat, namedChans: List[NamedChan]) {

  def toJson = Json.obj(
    "user" -> chat.user.username,
    "lines" -> (chat.lines map (_.toJson)),
    "chans" -> JsObject(namedChans map { c ⇒ c.chan.key -> c.toJson }),
    "activeChans" -> chat.activeChanKeys,
    "mainChan" -> chat.mainChanKey.filter(chat.activeChanKeys.contains))

  def addChans(chans: List[NamedChan]) = copy(namedChans = (namedChans ::: chans).distinct)
}

object Chat {

  val baseChans = List(LichessChan)
  // LichessChan,
  // LobbyChan,
  // TvChan,
  // GameChan("00000000", "Game / thinkabit"),
  // UserChan("claymore", "Claymore"),
  // UserChan("legend", "legend"))
}
