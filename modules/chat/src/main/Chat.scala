package lila.chat

import lila.user.User

case class Chat(
    user: User,
    lines: List[Line],
    chans: List[(Chan, Boolean)],
    mainChan: Option[Chan]) {
}

object Chat {

  val baseChans = List(
    Chan(lichess, "")
}

sealed abstract class Chan(typ: String) {

  def name: String
}

case object StaticChan {

  val name = typ
}

case class IdChan(id: String) {

  val name = s"$typ:$id"
}

object Chan {
  val lichess = "lichess"
  val lobby = "lobby"
  val game = "game"
  val typs = List(lichess, lobby, game)
  def parse(str: String): Option[Chan] = str.split(':') match {
    case Array(typ, id) if typs.contains(typ) ⇒ IdChan(typ, id).some
    case Array(typ, id) if typs.contains(typ) ⇒ Chan(typ, id).some
    case _                                    ⇒ None
  }
}
