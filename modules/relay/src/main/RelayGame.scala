package lila.relay

import chess.format.pgn.{ Tags, Tag, TagType }
import lila.study.{ Chapter, Node, PgnImport }
import lila.study.MultiPgn

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: chess.variant.Variant,
    root: Node.Root,
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

  def resetToSetup = copy(
    root = root.withoutChildren,
    end = None,
    tags = tags.copy(value = tags.value.filter(_.name != Tag.Result))
  )

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

  import lila.common.Iso
  import chess.format.pgn.{ Initial, Pgn }
  val iso: Iso[RelayGames, MultiPgn] =
    import lila.study.PgnDump.WithFlags
    given WithFlags = WithFlags(
      comments = false,
      variations = false,
      clocks = true,
      source = true,
      orientation = false
    )
    Iso[RelayGames, MultiPgn](
      gs =>
        MultiPgn {
          gs.view.map { g =>
            Pgn(
              tags = g.tags,
              turns = lila.study.PgnDump.toTurns(g.root).toList,
              initial = Initial.empty
            ).render
          }.toList
        },
      mul => RelayFetch.multiPgnToGames(mul).get
    )
