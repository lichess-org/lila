package lila.relay

import chess.format.pgn.Tags
import lila.study.{ Chapter, Node }

case class RelayGame(
    tags: Tags,
    root: Node.Root,
    whiteName: RelayGame.PlayerName,
    blackName: RelayGame.PlayerName
) {

  lazy val id = RelayGame.makeId(whiteName.value, blackName.value, tags(_.Event))

  def is(c: Chapter) = id == RelayGame.makeId(~c.tags(_.White), ~c.tags(_.Black), c.tags(_.Event))

  override def toString = id
}

object RelayGame {

  def makeId(white: String, black: String, event: Option[String]) =
    s"$white - $black, ${event | "?"}"

  case class PlayerName(value: String) extends AnyVal with StringValue
}
