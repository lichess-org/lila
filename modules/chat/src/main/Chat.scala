package lila.chat

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

  def toJson = Json.obj(
    "user" -> user.username,
    "lines" -> (lines map (_.toJson)),
    "chans" -> JsObject(chans map { c ⇒ c.key -> c.toJson }),
    "activeChans" -> activeChanKeys,
    "mainChan" -> mainChanKey.filter(activeChanKeys.contains))
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
