package lila.relay

import chess.Outcome
import chess.format.pgn.{ Tag, TagType, Tags }

import lila.study.{ MultiPgn, PgnDump }
import lila.tree.Root

case class RelayGame(
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    outcome: Option[Outcome]
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
    tags = tags.copy(value = tags.value.filter(_.name != Tag.Result)),
    outcome = None
  )

  def fideIdsPair: Option[PairOf[Option[chess.FideId]]] =
    tags.fideIds.some.filter(_.forall(_.exists(_ > 0))).map(_.toPair)

  def hasUnknownPlayer: Boolean =
    List(RelayGame.whiteTags, RelayGame.blackTags).exists:
      _.forall(tag => tags(tag).isEmpty)

  def showResult = Outcome.showResult(outcome)

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  type TagNames = List[Tag.type => TagType]
  val eventTags: TagNames  = List(_.Event, _.Site)
  val nameTags: TagNames   = List(_.White, _.Black)
  val fideIdTags: TagNames = List(_.WhiteFideId, _.BlackFideId)
  val whiteTags: TagNames  = List(_.White, _.WhiteFideId)
  val blackTags: TagNames  = List(_.Black, _.BlackFideId)

  def fromChapter(c: lila.study.Chapter) = RelayGame(
    tags = c.tags,
    variant = c.setup.variant,
    root = c.root,
    outcome = c.tags.outcome
  )

  import scalalib.Iso
  import chess.format.pgn.InitialComments
  val iso: Iso[RelayGames, MultiPgn] =
    import lila.study.PgnDump.WithFlags
    given WithFlags = WithFlags(
      comments = false,
      variations = false,
      clocks = true,
      source = true,
      orientation = false,
      site = none
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

  def filter(onlyRound: Option[Int])(games: RelayGames): RelayGames =
    onlyRound.fold(games): round =>
      games.filter(_.tags.roundNumber.has(round))

  // 1-indexed, both inclusive
  case class Slice(from: Int, to: Int):
    override def toString = s"$from-$to"

  object Slices:
    def filter(slices: List[Slice])(games: RelayGames): RelayGames =
      if slices.isEmpty then games
      else
        games.view.zipWithIndex
          .filter: (g, i) =>
            val n = i + 1
            slices.exists: s =>
              n >= s.from && n <= s.to
          .map(_._1)
          .toVector

    // 1-5,12-15,20
    def parse(str: String): List[Slice] = str.trim
      .split(',')
      .toList
      .map(_.trim)
      .flatMap: s =>
        s.split('-').toList.map(_.trim) match
          case Nil             => none
          case from :: Nil     => from.toIntOption.map(f => Slice(f, f))
          case from :: to :: _ => (from.toIntOption, to.toIntOption).mapN(Slice.apply)

    def show(slices: List[Slice]): String = slices
      .map:
        case Slice(f, t) if f == t => f.toString
        case slice                 => slice.toString
      .mkString(",")

    val iso: Iso.StringIso[List[Slice]] = Iso(parse, show)
