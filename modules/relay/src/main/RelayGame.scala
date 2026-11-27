package lila.relay

import java.time.{ Instant, OffsetDateTime }
import scala.util.Try

import chess.Outcome
import chess.format.UciPath
import chess.format.pgn.{ Tag, TagType, Tags }

import lila.core.fide.PlayerToken
import lila.study.{ MultiPgn, PgnDump, StudyPgnImport }
import lila.tree.Node.Comments
import lila.tree.{ Clock, Root }

case class RelayGame(
    tags: Tags,
    variant: chess.variant.Variant,
    root: Root,
    points: Option[Outcome.GamePoints]
):
  override def toString =
    s"RelayGame ${root.mainlineNodeList.size} ${tags.outcome} ${tags.names} ${tags.fideIds}"

  def isEmpty = tags.value.isEmpty && root.children.isEmpty

  def isBye = tags.names.exists(_.exists(_.value.toLowerCase == "bye"))

  def hasMoves = root.children.nonEmpty

  def withoutMoves = copy(root = root.withoutChildren)

  def resetToSetup = withoutMoves.copy(
    tags = tags - Tag.Result,
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
            root.setClockAt(Clock(centis, true.some).some, path) | root
      copy(root = newRoot)

  def referenceClockAt(delaySeconds: Int): Option[Instant] =
    if points.isEmpty && root.mainline.nonEmpty && tags.clocks(root.lastMainlineNode.ply.turn).isDefined then
      tags(_.ReferenceTime)
        .flatMap(RelayReferenceTime.parse)
        .flatMap(at => Try(at.plusSeconds(delaySeconds.toLong)).toOption)
        .flatMap(at => Try(Instant.ofEpochMilli(at.toEpochMilli)).toOption)
    else none

  def showResult = Outcome.showPoints(points)

private object RelayGame:

  val lichessDomains = List("lichess.org", "lichess.dev")

  type TagNames = List[Tag.type => TagType]
  val eventTags: TagNames = List(_.Event, _.Site)
  val nameTags: TagNames = List(_.White, _.Black)
  val fideIdTags: TagNames = List(_.WhiteFideId, _.BlackFideId)
  val whiteTags: TagNames = List(_.White, _.WhiteFideId)
  val blackTags: TagNames = List(_.Black, _.BlackFideId)
  val unplayedTag = Tag(_.Termination, "Unplayed")

  def fromChapter(c: lila.study.Chapter) = RelayGame(
    tags = snapshotTags(c, c.tags),
    variant = c.setup.variant,
    root = c.root,
    points = c.tags.points
  )

  // Keep the effective reference and both readings together when relaying a broadcast again.
  def snapshotTags(chapter: lila.study.Chapter, tags: Tags): Tags =
    val clean = tags - Tag.ReferenceTime - Tag.WhiteClock - Tag.BlackClock
    val clockAt = chapter.relay.flatMap(_.clockAt).filter(_ => chapter.tags.points.isEmpty)
    val path = chapter.relay.map(_.path).filterNot(_.isEmpty) | UciPath.fromIds(
      chapter.root.mainline.takeWhile(!_.forceVariation).map(_.id)
    )
    if path.isEmpty || (clockAt.isEmpty && chapter.tags.points.isEmpty) then tags
    else
      val node = chapter.root.nodeAt(path)
      val parentClock = chapter.root.nodeAt(path.parent).flatMap(_.clock)
      val clocks = node.toList.flatMap: last =>
        val (white, black) =
          if last.color.white then (parentClock, last.clock) else (last.clock, parentClock)
        List(
          white.map(c => Tag(_.WhiteClock, c.centis.roundSeconds.value)),
          black.map(c => Tag(_.BlackClock, c.centis.roundSeconds.value))
        ).flatten
      val withClocks = clocks.foldLeft(clean)(_ + _)
      clockAt.fold(withClocks)(at => withClocks + Tag(_.ReferenceTime, at.toString))

  def fromStudyImport(res: StudyPgnImport.Result): RelayGame =
    val fixedTags = cleanOrRemovePlayerNames:
      // remove wrong ongoing result tag if the board has a mate on it
      if res.ending.isDefined && res.tags(_.Result).has("*") then
        res.tags.map(_.filter(_ != Tag(_.Result, "*")))
      // normalize result tag (e.g. 0.5-0 ->  1/2-0)
      else
        res.tags.map(_.map: tag =>
          if tag.name == Tag.Result
          then tag.copy(value = Outcome.showPoints(Outcome.pointsFromResult(tag.value)))
          else tag)
    .pipe(_ - Tag.Date) // trust the chapter date, not the source date
      .pipe(toggleUnplayedTermination(_, res.ending.isDefined && res.root.mainline.sizeIs < 2))
    RelayGame(
      tags = fixedTags,
      variant = res.variant,
      root = res.root.copy(
        comments = Comments.empty,
        children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
      ),
      points = res.ending.map(_.points)
    ).applyTagClocksToLastMoves

  private def cleanOrRemovePlayerNames(tags: Tags) = tags.map:
    _.flatMap: tag =>
      if tag.name != Tag.White && tag.name != Tag.Black then tag.some
      else
        val clean = tag.value.trim
        Option.when(clean.size > 1 && clean.toLowerCase != "unknown"):
          tag.copy(value = clean)

  def toggleUnplayedTermination(tags: Tags, set: Boolean) =
    if set
    then tags + unplayedTag
    else tags.map(_.filter(_ != unplayedTag))

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
      mul => RelayFetch.multiPgnToGames.parseAndCache(mul).fold(e => throw e, identity)
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

  opaque type ReorderNames = String
  object ReorderNames extends OpaqueString[ReorderNames]:
    extension (r: ReorderNames)
      def reorder(games: RelayGames): RelayGames =
        r.linesIterator.map(parseLine).toVector match
          case Vector() => games
          case lines =>
            val gamesByToken = tokenizeGamesByPlayers(games)
            val ordered = lines.flatMap: line =>
              line
                .map(gamesByToken.getOrElse(_, Vector.empty))
                .match
                  case List(one) => one
                  case List(w, b) => w.filter(b.contains)
                  case _ => Vector.empty
                .headOption

            val unordered = games.filterNot(g => ordered.contains(g))
            ordered ++ unordered

    private def parseLine(line: String): List[PlayerToken] = // one or two player tokens
      line.split(";", 2).toList.map(_.trim).filter(_.nonEmpty).map(RelayPlayerLine.tokenize.apply)

    private def tokenizeGamesByPlayers(games: RelayGames): Map[PlayerToken, Vector[RelayGame]] =
      val gamesTokens: Vector[(RelayGame, List[PlayerToken])] =
        games.map: g =>
          g -> g.tags.names
            .mapList(_.map(_.value))
            .flatten
            .map(RelayPlayerLine.tokenize.apply)
            .filter(_.nonEmpty)

      gamesTokens
        .flatMap((game, tokens) => tokens.map(_ -> game))
        .groupBy(_._1)
        .toMap
        .view
        .mapValues(_.map(_._2).distinct)
        .toMap

private[relay] object RelayReferenceTime:

  def parse(value: String): Option[Instant] =
    val timestamp = if value.startsWith("W/") || value.startsWith("B/") then value.drop(2) else value
    Try(OffsetDateTime.parse(timestamp).toInstant).toOption

  def latest(previous: Option[String], current: Option[String]): Option[String] =
    val previousParsed = previous.flatMap(value => parse(value).map(value -> _))
    val currentParsed = current.flatMap(value => parse(value).map(value -> _))
    (previousParsed, currentParsed) match
      case (None, None) => None
      case (Some((value, _)), None) => Some(value)
      case (None, Some((value, _))) => Some(value)
      case (Some((previousValue, previousInstant)), Some((currentValue, currentInstant))) =>
        if currentInstant.isAfter(previousInstant) then Some(currentValue) else Some(previousValue)
