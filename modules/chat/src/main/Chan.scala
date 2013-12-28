package lila.chat

import play.api.libs.json._

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

  override def toString = key
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
object TvChan extends StaticChan(Chan.typ.tv, "TV")
case class GameChan(i: String, n: String) extends IdChan(Chan.typ.game, i, n)
case class TournamentChan(i: String, n: String) extends IdChan(Chan.typ.game, i, n)
case class UserChan(i: String, n: String) extends IdChan(Chan.typ.game, i, n)

object Chan {

  object typ {
    val lichess = "lichess"
    val lobby = "lobby"
    val tv = "tv"
    val game = "game"
  }

  def apply(typ: String, idOption: Option[String]): Option[Chan] = typ match {
    case Chan.typ.lichess ⇒ LichessChan.some
    case Chan.typ.lobby   ⇒ LobbyChan.some
    case Chan.typ.tv      ⇒ TvChan.some
    case Chan.typ.game    ⇒ idOption map { GameChan(_, "Some game") }
    case _                ⇒ none
  }

  def parse(str: String): Option[Chan] = str.split('_') match {
    case Array(typ, id) ⇒ apply(typ, id.some)
    case Array(typ)     ⇒ apply(typ, none)
    case _              ⇒ None
  }
}

