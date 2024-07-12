package lila.study

import chess.format.pgn.{ Glyph, Tags }
import chess.format.{ Fen, Uci, UciPath }
import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant
import chess.{ ByColor, Centis, Color, Outcome, Ply }
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.tree.Node.{ Comment, Gamebook, Shapes }
import lila.tree.{ Branch, Root }

case class Chapter(
    @Key("_id") id: StudyChapterId,
    studyId: StudyId,
    name: StudyChapterName,
    setup: Chapter.Setup,
    root: Root,
    tags: Tags,
    order: Chapter.Order,
    ownerId: UserId,
    conceal: Option[Ply] = None,
    practice: Option[Boolean] = None,
    gamebook: Option[Boolean] = None,
    description: Option[String] = None,
    relay: Option[Chapter.Relay] = None,
    serverEval: Option[Chapter.ServerEval] = None,
    denorm: Option[Chapter.LastPosDenorm] = None,
    createdAt: Instant
) extends Chapter.Like:

  import Chapter.BothClocks

  def updateDenorm: Chapter =
    val looksLikeGame = tags.names.exists(_.isDefined) || tags.outcome.isDefined
    val newDenorm = looksLikeGame.option:
      val node = relay.map(_.path).filterNot(_.isEmpty).flatMap(root.nodeAt) | root.lastMainlineNode
      val clocks = relay.so: r =>
        val path       = r.path
        val parentPath = path.parent.some.filter(_ != path)
        val parentNode = parentPath.flatMap(root.nodeAt)
        val clockSwap  = ByColor(node.clock, parentNode.flatMap(_.clock).orElse(node.clock))
        if node.color.black then clockSwap else clockSwap.swap
      val uci = node.moveOption.map(_.uci)
      val check = node.moveOption
        .flatMap(_.san.value.lastOption)
        .collect:
          case '+' => Chapter.Check.Check
          case '#' => Chapter.Check.Mate
      Chapter.LastPosDenorm(node.fen, uci, check, clocks)
    copy(denorm = newDenorm)

  def updateRoot(f: Root => Option[Root]) =
    f(root).map: newRoot =>
      copy(root = newRoot)

  def addNode(node: Branch, path: UciPath, newRelay: Option[Chapter.Relay] = None): Option[Chapter] =
    updateRoot:
      _.withChildren(_.addNodeAt(node, path))
    .map:
      _.copy(relay = newRelay.orElse(relay)).updateDenorm

  def setShapes(shapes: Shapes, path: UciPath): Option[Chapter] =
    updateRoot(_.setShapesAt(shapes, path))

  def setComment(comment: Comment, path: UciPath): Option[Chapter] =
    updateRoot(_.setCommentAt(comment, path))

  def setGamebook(gamebook: Gamebook, path: UciPath): Option[Chapter] =
    updateRoot(_.setGamebookAt(gamebook, path))

  def deleteComment(commentId: Comment.Id, path: UciPath): Option[Chapter] =
    updateRoot(_.deleteCommentAt(commentId, path))

  def toggleGlyph(glyph: Glyph, path: UciPath): Option[Chapter] =
    updateRoot(_.toggleGlyphAt(glyph, path))

  def setClock(
      clock: Option[Centis],
      path: UciPath
  ): Option[(Chapter, Option[BothClocks])] =
    updateRoot(_.setClockAt(clock, path))
      .map(_.updateDenorm)
      .map: chapter =>
        chapter -> chapter.denorm.map(_.clocks).filter(chapter.denorm != _)

  def forceVariation(force: Boolean, path: UciPath): Option[Chapter] =
    updateRoot(_.forceVariationAt(force, path))

  def opening: Option[Opening] =
    Variant.list
      .openingSensibleVariants(setup.variant)
      .so(OpeningDb.searchInFens(root.mainline.map(_.fen.opening)))

  def isEmptyInitial = order == 1 && root.children.isEmpty && tags.value.isEmpty

  def cloneFor(study: Study) =
    copy(
      id = Chapter.makeId,
      studyId = study.id,
      ownerId = study.ownerId,
      createdAt = nowInstant
    )

  def preview = ChapterPreview(
    id = id,
    name = name,
    players = ChapterPreview.players(denorm.so(_.clocks))(tags),
    orientation = setup.orientation,
    fen = denorm.fold(Fen.initial)(_.fen),
    lastMove = denorm.flatMap(_.uci),
    lastMoveAt = relay.map(_.lastMoveAt),
    check = denorm.flatMap(_.check),
    result = tags.outcome.isDefined.option(tags.outcome)
  )

  def isPractice = ~practice
  def isGamebook = ~gamebook
  def isConceal  = conceal.isDefined

  def withoutChildren = copy(root = root.withoutChildren)

  def withoutChildrenIfPractice = if isPractice then copy(root = root.withoutChildren) else this

  def isOverweight = root.children.countRecursive >= Chapter.maxNodes

object Chapter:

  type Order = Int

  // I've seen chapters with 35,000 nodes on prod.
  // It works but could be used for DoS.
  val maxNodes = 3000

  trait Like:
    val id: StudyChapterId
    val name: StudyChapterName
    def initialPosition = Position.Ref(id, UciPath.root)

  case class Setup(
      gameId: Option[GameId],
      variant: Variant,
      orientation: Color,
      fromFen: Option[Boolean] = None
  ):
    def isFromFen = ~fromFen

  case class Relay(
      path: UciPath,
      lastMoveAt: Instant,
      fideIds: Option[PairOf[Option[chess.FideId]]]
  ):
    def secondsSinceLastMove: Int = (nowSeconds - lastMoveAt.toSeconds).toInt

  case class ServerEval(path: UciPath, done: Boolean)

  type BothClocks = ByColor[Option[Centis]]

  enum Check:
    case Check, Mate

  /* Last position of the main line.
   * Used for chapter previews. */
  case class LastPosDenorm(fen: Fen.Full, uci: Option[Uci], check: Option[Check], clocks: BothClocks)

  case class IdName(@Key("_id") id: StudyChapterId, name: StudyChapterName)

  def defaultName(order: Order) = StudyChapterName(s"Chapter $order")

  private val defaultNameRegex           = """Chapter \d+""".r
  def isDefaultName(n: StudyChapterName) = n.value.isEmpty || defaultNameRegex.matches(n.value)

  def fixName(n: StudyChapterName) = StudyChapterName(lila.common.String.softCleanUp(n.value).take(80))

  def makeId = StudyChapterId(scalalib.ThreadLocalRandom.nextString(8))

  def make(
      studyId: StudyId,
      name: StudyChapterName,
      setup: Setup,
      root: Root,
      tags: Tags,
      order: Int,
      ownerId: UserId,
      practice: Boolean,
      gamebook: Boolean,
      conceal: Option[Ply],
      relay: Option[Relay] = None
  ) =
    Chapter(
      id = makeId,
      studyId = studyId,
      name = fixName(name),
      setup = setup,
      root = root,
      tags = tags,
      order = order,
      ownerId = ownerId,
      practice = practice.option(true),
      gamebook = gamebook.option(true),
      conceal = conceal,
      relay = relay,
      createdAt = nowInstant
    )
