package lila.relay

import chess.format.pgn.{ Tags, Tag, TagType }
import lila.study.{ Chapter, MultiPgn, Node, PgnImport }
import lila.tree.Root

case class RelayGame(
    index: Int,
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    end: Option[PgnImport.End]
):

  def staticTagsMatch(chapterTags: Tags): Boolean =
    allSame(chapterTags, RelayGame.roundTags) && playerTagsMatch(chapterTags)

  def playerTagsMatch(chapterTags: Tags): Boolean =
    if RelayGame.fideIdTags.forall(id => chapterTags.exists(id) && tags.exists(id))
    then allSame(chapterTags, RelayGame.fideIdTags)
    else allSame(chapterTags, RelayGame.nameTags)

  private def allSame(chapterTags: Tags, tagNames: RelayGame.TagNames) = tagNames.forall: tag =>
    chapterTags(tag) == tags(tag)

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty

  def resetToSetup = copy(
    root = root.withoutChildren,
    end = None,
    tags = tags.copy(value = tags.value.filter(_.name != Tag.Result))
  )

  lazy val looksLikeLichess = tags(_.Site).exists: site =>
    RelayGame.lichessDomains.exists: domain =>
      site startsWith s"https://$domain/"

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  type TagNames = List[Tag.type => TagType]
  val roundTags: TagNames  = List(_.Round, _.Event, _.Site)
  val nameTags: TagNames   = List(_.White, _.Black)
  val fideIdTags: TagNames = List(_.WhiteFideId, _.BlackFideId)

  import lila.common.Iso
  import chess.format.pgn.{ InitialComments, Pgn }
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
        MultiPgn:
          gs.view
            .map: g =>
              Pgn(g.tags, InitialComments.empty, lila.study.PgnDump.rootToTree(g.root)).render
            .toList
      ,
      mul => RelayFetch.multiPgnToGames(mul).toOption.get
    )
