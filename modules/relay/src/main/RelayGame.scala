package lila.relay

import chess.format.pgn.{ Tag, TagType, Tags }

import lila.study.{ MultiPgn, StudyPgnImport, PgnDump }
import lila.tree.Root

case class RelayGame(
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    ending: Option[StudyPgnImport.End]
):

  // We don't use tags.boardNumber.
  // Organizers change it at any time while reordering the boards.
  def isSameGame(otherTags: Tags): Boolean =
    allSame(otherTags, RelayGame.eventTags) &&
      otherTags.roundNumber == tags.roundNumber &&
      playerTagsMatch(otherTags)

  private def playerTagsMatch(otherTags: Tags): Boolean =
    if RelayGame.fideIdTags.forall(id => otherTags.exists(id) && tags.exists(id))
    then allSame(otherTags, RelayGame.fideIdTags)
    else allSame(otherTags, RelayGame.nameTags)

  private def allSame(otherTags: Tags, tagNames: RelayGame.TagNames) = tagNames.forall: tag =>
    otherTags(tag) == tags(tag)

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty

  def resetToSetup = copy(
    root = root.withoutChildren,
    ending = None,
    tags = tags.copy(value = tags.value.filter(_.name != Tag.Result))
  )

  def fideIdsPair: Option[PairOf[Option[chess.FideId]]] =
    tags.fideIds.some.filter(_.forall(_.isDefined)).map(_.toPair)

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  type TagNames = List[Tag.type => TagType]
  val eventTags: TagNames  = List(_.Event, _.Site)
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
            .map(g => PgnDump.rootToPgn(g.root, g.tags, InitialComments.empty).render)
            .toList
      ,
      mul => RelayFetch.multiPgnToGames(mul).fold(e => throw e, identity)
    )
