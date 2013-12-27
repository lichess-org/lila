package lila.chat

import play.api.libs.json._

import lila.user.User

case class Chat(
    user: User,
    lines: List[Line],
    chans: List[(Chan, Boolean)],
    mainChan: Option[Chan]) {

  def toJson = Json.obj(
    "lines" -> (lines map { line ⇒
      Json.obj(
        "chan" -> line.chan.key,
        "user" -> line.username,
        "troll" -> line.troll,
        "date" -> line.date.getSeconds,
        "text" -> line.html.toString)
    }),
    "chans" -> JsObject(chans map {
      case (chan, _) ⇒ chan.key -> chan.toJson
    }),
    "activeChans" -> (chans collect { case (chan, true) ⇒ chan.key }),
    "mainChan" -> mainChan.map(_.key))
}

object Chat {

  val baseChans = List(
    LichessChan,
    LobbyChan,
    GameChan("00000000", "Game / thinkabit"),
    UserChan("claymore", "Claymore"),
    UserChan("legend", "legend"))
}

sealed trait Chan {
  def typ: String
  def key: String
  def name: String
  def idOption: Option[String]

  def toJson = Json.obj(
    "typ" -> typ,
    "id" -> idOption,
    "key" -> key,
    "name" -> name)
}

sealed abstract class StaticChan(val typ: String, val name: String) extends Chan {
  val key = typ
  val idOption = none
}

sealed abstract class IdChan(val typ: String, val id: String, val name: String) extends Chan {
  val key = s"${typ}_$id"
  val idOption = id.some
}

object LichessChan extends StaticChan(Chan.typ.lichess, "Lichess")
object LobbyChan extends StaticChan(Chan.typ.lobby, "Lobby")
case class GameChan(i: String, n: String) extends IdChan(Chan.typ.game, i, n)
case class TournamentChan(i: String, n: String) extends IdChan(Chan.typ.game, i, n)
case class UserChan(i: String, n: String) extends IdChan(Chan.typ.game, i, n)

object Chan {

  object typ {
    val lichess = "lichess"
    val lobby = "lobby"
    val game = "game"
  }

  def apply(typ: String, idOption: Option[String]): Option[Chan] = typ match {
    case Chan.typ.lichess ⇒ LichessChan.some
    case Chan.typ.lobby   ⇒ LobbyChan.some
    case Chan.typ.game    ⇒ idOption map { GameChan(_, "Some game") }
    case _                ⇒ none
  }

  def parse(str: String): Option[Chan] = str.split('_') match {
    case Array(typ, id) ⇒ apply(typ, id.some)
    case Array(typ)     ⇒ apply(typ, none)
    case _              ⇒ None
  }
}
