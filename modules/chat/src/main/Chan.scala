package lila.chat

import play.api.libs.json._

sealed trait Chan {
  def typ: String
  def key: String
  def idOption: Option[String]

  override def toString = key
}

sealed abstract class StaticChan(val typ: String, val name: String) extends Chan {
  val key = typ
  val idOption = none
}

sealed abstract class IdChan(val typ: String, val id: String) extends Chan {
  val key = s"${typ}_$id"
  val idOption = id.some
}

object LichessChan extends StaticChan(Chan.typ.lichess, "Lichess")
object LobbyChan extends StaticChan(Chan.typ.lobby, "Lobby")
object TvChan extends StaticChan(Chan.typ.tv, "TV")
case class GameWatcherChan(i: String) extends IdChan(Chan.typ.gameWatcher, i)
case class GamePlayerChan(i: String) extends IdChan(Chan.typ.gamePlayer, i)
object TournamentLobbyChan extends StaticChan(Chan.typ.tournamentLobby, "Tournament Lobby")
case class TournamentChan(i: String) extends IdChan(Chan.typ.tournament, i)

case class NamedChan(chan: Chan, name: String) {

  def toJson = Json.obj(
    "key" -> chan.key,
    "name" -> name)
}

object Chan {

  object typ {
    val lichess = "lichess"
    val lobby = "lobby"
    val tv = "tv"
    val gameWatcher = "gameWatcher"
    val gamePlayer = "gamePlayer"
    val tournamentLobby = "tournamentLobby"
    val tournament = "tournament"
  }

  def apply(typ: String, idOption: Option[String]): Option[Chan] = typ match {
    case Chan.typ.lichess         ⇒ LichessChan.some
    case Chan.typ.lobby           ⇒ LobbyChan.some
    case Chan.typ.tv              ⇒ TvChan.some
    case Chan.typ.gameWatcher     ⇒ idOption map GameWatcherChan
    case Chan.typ.gamePlayer      ⇒ idOption map GamePlayerChan
    case Chan.typ.tournamentLobby ⇒ TournamentLobbyChan.some
    case Chan.typ.tournament      ⇒ idOption map TournamentChan
    case _                        ⇒ none
  }

  def parse(str: String): Option[Chan] = str.split('_') match {
    case Array(typ, id) ⇒ apply(typ, id.some)
    case Array(typ)     ⇒ apply(typ, none)
    case _              ⇒ None
  }
}

