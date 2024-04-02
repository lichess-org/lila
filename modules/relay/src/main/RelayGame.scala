package lila.relay

import chess.format.pgn.{ Tag, TagType, Tags }

import lila.study.{ MultiPgn, PgnImport }
import lila.tree.Root

case class RelayGame(
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    ending: Option[PgnImport.End],
    index: Option[Int] = none
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
  def isPush  = index.isEmpty

  def resetToSetup = copy(
    root = root.withoutChildren,
    ending = None,
    tags = tags.copy(value = tags.value.filter(_.name != Tag.Result))
  )

  def fideIdsPair: Option[PairOf[Option[chess.FideId]]] =
    tags.fideIds.some.filter(_.forall(_.isDefined)).map(_.toPair)

  lazy val looksLikeLichess = tags(_.Site).exists: site =>
    RelayGame.lichessDomains.exists: domain =>
      site.startsWith(s"https://$domain/")

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  type TagNames = List[Tag.type => TagType]
  val roundTags: TagNames  = List(_.Round, _.Event, _.Site)
  val nameTags: TagNames   = List(_.White, _.Black)
  val fideIdTags: TagNames = List(_.WhiteFideId, _.BlackFideId)

  import scalalib.Iso
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
      mul => RelayFetch.multiPgnToGames(mul).fold(e => throw e, identity)
    )
