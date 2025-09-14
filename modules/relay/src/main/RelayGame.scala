package lila.relay

import chess.Outcome
import chess.format.UciPath
import chess.format.pgn.{ Tag, TagType, Tags }

import lila.study.{ MultiPgn, PgnDump }
import lila.tree.{ Root, Clock }
import lila.tree.Node.Comments

case class RelayGame(
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    points: Option[Outcome.GamePoints]
):
  override def toString =
    s"RelayGame ${root.mainlineNodeList.size} ${tags.outcome} ${tags.names} ${tags.fideIds}"

  def isEmpty = tags.value.isEmpty && root.children.nodes.isEmpty

  def isBye = tags.names.exists(_.exists(_.value.toLowerCase == "bye"))

  def hasMoves = root.children.nodes.nonEmpty

  def withoutMoves = copy(root = root.withoutChildren)

  def resetToSetup = withoutMoves.copy(
    tags = tags.copy(value = tags.value.filter(_.name != Tag.Result)),
    points = None
  )

  def fideIdsPair: Option[PairOf[Option[chess.FideId]]] =
    tags.fideIds.some.filter(_.exists(_.exists(_.value > 0))).map(_.toPair)

  def hasUnknownPlayer: Boolean =
    List(RelayGame.whiteTags, RelayGame.blackTags).exists:
      _.forall(tag => tags(tag).isEmpty)

  def applyTagClocksToLastMoves: RelayGame =
    val clocks = tags.clocks
    if clocks.forall(_.isEmpty) then this
    else
      val mainlinePath = root.mainlinePath
      val turn = root.lastMainlineNode.ply.turn
      val newRoot = List(
        mainlinePath.nonEmpty.option(mainlinePath.parent) -> turn,
        mainlinePath.some -> !turn
      ).flatMap:
        case (Some(path), color) => clocks(color).map(path -> _)
        case _ => none
      .foldLeft(root):
          case (root, (path, centis)) =>
            if root.nodeAt(path).exists(_.clock.isDefined) then root
            else root.setClockAt(Clock(centis, true.some).some, path) | root
      copy(root = newRoot)

  def showResult = Outcome.showPoints(points)

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  type TagNames = List[Tag.type => TagType]
  val eventTags: TagNames = List(_.Event, _.Site)
  val nameTags: TagNames = List(_.White, _.Black)
  val fideIdTags: TagNames = List(_.WhiteFideId, _.BlackFideId)
  val whiteTags: TagNames = List(_.White, _.WhiteFideId)
  val blackTags: TagNames = List(_.Black, _.BlackFideId)

  def fromChapter(c: lila.study.Chapter) = RelayGame(
    tags = c.tags,
    variant = c.setup.variant,
    root = c.root,
    points = c.tags.points
  )

  def fromStudyImport(res: lila.study.StudyPgnImport.Result): RelayGame =
    val fixedTags = cleanOrRemovePlayerNames:
      removeDateTag:
        Tags:
          // remove wrong ongoing result tag if the board has a mate on it
          if res.ending.isDefined && res.tags(_.Result).has("*") then
            res.tags.value.filter(_ != Tag(_.Result, "*"))
          // normalize result tag (e.g. 0.5-0 ->  1/2-0)
          else
            res.tags.value.map: tag =>
              if tag.name == Tag.Result
              then tag.copy(value = Outcome.showPoints(Outcome.pointsFromResult(tag.value)))
              else tag
    RelayGame(
      tags = fixedTags,
      variant = res.variant,
      root = res.root.copy(
        comments = Comments.empty,
        children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
      ),
      points = res.ending.map(_.points)
    ).applyTagClocksToLastMoves

  private def cleanOrRemovePlayerNames(tags: Tags) = tags.copy(
    value = tags.value.flatMap: tag =>
      if tag.name != Tag.White && tag.name != Tag.Black then tag.some
      else
        val clean = tag.value.trim
        Option.when(clean.size > 1 && clean.toLowerCase != "unknown"):
          tag.copy(value = clean)
  )

  // trust the chapter date, not the source date
  private def removeDateTag(tags: Tags) =
    tags.copy(value = tags.value.filterNot(_.name == Tag.Date))

  import scalalib.Iso
  import chess.format.pgn.InitialComments
  val iso: Iso[RelayGames, MultiPgn] =
    import lila.study.PgnDump.WithFlags
    given WithFlags = WithFlags(
      comments = false,
      variations = false,
      clocks = true,
      orientation = false
    )
    Iso[RelayGames, MultiPgn](
      gs =>
        MultiPgn:
          gs.view
            .map(g => PgnDump.rootToPgn(g.root, g.tags, InitialComments.empty).render)
            .toList
      ,
      mul => RelayFetch.multiPgnToGames.either(mul).fold(e => throw e, identity)
    )

  def filter(onlyRound: Option[Either[String, Int]])(games: RelayGames): RelayGames =
    onlyRound.fold(games):
      case Left(r) => games.filter(_.tags(_.Round).has(r))
      case Right(r) => games.filter(_.tags.roundNumber.has(r))

  // 1-indexed, both inclusive
  case class Slice(from: Int, to: Int)

  object Slices:

    def filterAndOrder(slices: List[Slice])(games: RelayGames): RelayGames =
      if slices.isEmpty then games
      else
        slices
          .foldLeft(Vector.empty[Int]): (acc, slice) =>
            acc ++ (slice.from to slice.to).toVector.filterNot(acc.contains)
          .flatMap(i => games.lift(i - 1))

    // 1-5,12-15,20
    def parse(str: String): List[Slice] = str.trim
      .split(',')
      .toList
      .map(_.trim)
      .flatMap: s =>
        s.split('-').toList.map(_.trim) match
          case Nil => none
          case from :: Nil => from.toIntOption.map(f => Slice(f, f))
          case from :: to :: _ => (from.toIntOption, to.toIntOption).mapN(Slice.apply)

    def show(slices: List[Slice]): String = slices
      .map:
        case Slice(f, t) if f == t => f.toString
        case Slice(f, t) => s"$f-$t"
      .mkString(", ")

    val iso: Iso.StringIso[List[Slice]] = Iso(parse, show)
