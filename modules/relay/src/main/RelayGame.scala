package lidraughts.relay

import draughts.format.pdn.Tags
import lidraughts.study.{ Node, PdnImport }

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: draughts.variant.Variant,
    root: Node.Root,
    end: Option[PdnImport.End]
) {

  def staticTagsMatch(chapterTags: Tags) = List("white", "black", "round", "event") forall { name =>
    chapterTags(name) == tags(name)
  }

  def started = root.children.nodes.nonEmpty

  def finished = end.isDefined

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty
}
