package lila.relay

import chess.format.pgn.Tags
import lila.study.{ Chapter, Node, PgnImport }

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: chess.variant.Variant,
    root: Node.Root,
    end: Option[PgnImport.End]
):

  def staticTagsMatch(chapterTags: Tags): Boolean =
    import RelayGame.*
    def allSame(tagNames: List[String]) = tagNames.forall { tag => chapterTags(tag) == tags(tag) }
    println(tags)
    allSame(staticTags) && {
      if fideIdTags.forall(id => chapterTags.exists(id) && tags.exists(id))
      then allSame(fideIdTags)
      else allSame(nameTags)
    }
  def staticTagsMatch(chapter: Chapter): Boolean = staticTagsMatch(chapter.tags)

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty

  lazy val looksLikeLichess = tags(_.Site) exists { site =>
    RelayGame.lichessDomains exists { domain =>
      site startsWith s"https://$domain/"
    }
  }

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  val staticTags = List("round", "event", "site")
  val nameTags   = List("white", "black")
  val fideIdTags = List("WhiteFideId", "BlackFideId")
