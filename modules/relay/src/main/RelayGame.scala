package lila.relay

import chess.format.pgn.{ Tags, Tag, TagType }
import lila.study.{ Chapter, PgnImport }
import lila.tree.Root

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    end: Option[PgnImport.End]
):

  def staticTagsMatch(chapterTags: Tags): Boolean =
    import RelayGame.*
    def allSame(tagNames: TagNames) = tagNames.forall { tag => chapterTags(tag) == tags(tag) }
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

  type TagNames = List[Tag.type => TagType]
  val staticTags: TagNames = List(_.Round, _.Event, _.Site)
  val nameTags: TagNames   = List(_.White, _.Black)
  val fideIdTags: TagNames = List(_.WhiteFideId, _.BlackFideId)
