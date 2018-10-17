package lidraughts.relay

import draughts.format.pdn.Tags
import lidraughts.study.{ Chapter, Node, PdnImport }

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: draughts.variant.Variant,
    root: Node.Root,
    end: Option[PdnImport.End]
) {

  def staticTagsMatch(chapterTags: Tags): Boolean = RelayGame.staticTags forall { name =>
    chapterTags(name) == tags(name)
  }
  def staticTagsMatch(chapter: Chapter): Boolean = staticTagsMatch(chapter.tags)

  def started = root.children.nodes.nonEmpty

  def finished = end.isDefined

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty
}

private object RelayGame {

  val staticTags = List("white", "black", "round", "event")
}
