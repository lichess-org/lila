package lila.relay

import chess.format.pgn.Tags
import lila.study.{ Node, PgnImport }

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: chess.variant.Variant,
    root: Node.Root,
    end: Option[PgnImport.End]
) {

  def staticTagsMatch(chapterTags: Tags) = List("white", "black", "round", "event") forall { name =>
    chapterTags(name) == tags(name)
  }

  def started = root.children.nodes.nonEmpty

  def finished = end.isDefined

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty
}
