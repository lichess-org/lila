package lila.relay

import lila.study.{ Node }
import chess.format.pgn.Tags

case class RelayGame(
    tags: Tags,
    root: Node.Root,
    whiteName: RelayGame.PlayerName,
    blackName: RelayGame.PlayerName
) {

  def id = s"$whiteName - $blackName, ${tags(_.Event) | "?"}"

  override def toString = id
}

object RelayGame {

  case class PlayerName(value: String) extends AnyVal with StringValue
}
