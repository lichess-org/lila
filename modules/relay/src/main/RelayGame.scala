package lila.relay

import chess.format.pgn.Tags
import lila.study.{ Chapter, Node, PgnImport }

case class RelayGame(
    index: Int,
    tags: Tags,
    root: Node.Root,
    end: Option[PgnImport.End]
) {

  def is(c: Chapter): Boolean = c.relay ?? is
  def is(r: Chapter.Relay): Boolean = r.index == index

  def started = root.children.nodes.nonEmpty

  def finished = end.isDefined
}
