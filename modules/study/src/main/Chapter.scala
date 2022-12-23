package lila.study

import chess.format.pgn.{ Glyph, Tags }
import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant
import chess.{ Ply, Centis, Color, Outcome }
import org.joda.time.DateTime
import ornicar.scalalib.ThreadLocalRandom

import lila.tree.Node.{ Comment, Gamebook, Shapes }

case class Chapter(
    _id: StudyChapterId,
    studyId: StudyId,
    name: StudyChapterName,
    setup: Chapter.Setup,
    root: Node.Root,
    tags: Tags,
    order: Int,
    ownerId: UserId,
    conceal: Option[Ply] = None,
    practice: Option[Boolean] = None,
    gamebook: Option[Boolean] = None,
    description: Option[String] = None,
    relay: Option[Chapter.Relay] = None,
    serverEval: Option[Chapter.ServerEval] = None,
    createdAt: DateTime
) extends Chapter.Like:

  def updateRoot(f: Node.Root => Option[Node.Root]) =
    f(root) map { newRoot =>
      copy(root = newRoot)
    }

  def addNode(node: Node, path: Path, newRelay: Option[Chapter.Relay] = None): Option[Chapter] =
    updateRoot {
      _.withChildren(_.addNodeAt(node, path))
    } map {
      _.copy(relay = newRelay orElse relay)
    }

  def setShapes(shapes: Shapes, path: Path): Option[Chapter] =
    updateRoot(_.setShapesAt(shapes, path))

  def setComment(comment: Comment, path: Path): Option[Chapter] =
    updateRoot(_.setCommentAt(comment, path))

  def setGamebook(gamebook: Gamebook, path: Path): Option[Chapter] =
    updateRoot(_.setGamebookAt(gamebook, path))

  def deleteComment(commentId: Comment.Id, path: Path): Option[Chapter] =
    updateRoot(_.deleteCommentAt(commentId, path))

  def toggleGlyph(glyph: Glyph, path: Path): Option[Chapter] =
    updateRoot(_.toggleGlyphAt(glyph, path))

  def setClock(clock: Option[Centis], path: Path): Option[Chapter] =
    updateRoot(_.setClockAt(clock, path))

  def forceVariation(force: Boolean, path: Path): Option[Chapter] =
    updateRoot(_.forceVariationAt(force, path))

  def opening: Option[Opening] =
    Variant.list.openingSensibleVariants(setup.variant) ??
      OpeningDb.searchInFens(root.mainline.map(_.fen.opening))

  def isEmptyInitial = order == 1 && root.children.nodes.isEmpty

  def cloneFor(study: Study) =
    copy(
      _id = Chapter.makeId,
      studyId = study.id,
      ownerId = study.ownerId,
      createdAt = DateTime.now
    )

  def metadata = Chapter.Metadata(
    _id = _id,
    name = name,
    setup = setup,
    outcome = tags.outcome.isDefined option tags.outcome,
    hasRelayPath = relay.exists(!_.path.isEmpty)
  )

  def isPractice = ~practice
  def isGamebook = ~gamebook
  def isConceal  = conceal.isDefined

  def withoutChildren = copy(root = root.withoutChildren)

  def withoutChildrenIfPractice = if (isPractice) copy(root = root.withoutChildren) else this

  def relayAndTags = relay map { Chapter.RelayAndTags(id, _, tags) }

  def isOverweight = root.children.countRecursive >= Chapter.maxNodes

object Chapter:

  // I've seen chapters with 35,000 nodes on prod.
  // It works but could be used for DoS.
  val maxNodes = 3000

  sealed trait Like:
    val _id: StudyChapterId
    val name: StudyChapterName
    val setup: Chapter.Setup
    inline def id = _id

    def initialPosition = Position.Ref(id, Path.root)

  case class Setup(
      gameId: Option[GameId],
      variant: Variant,
      orientation: Color,
      fromFen: Option[Boolean] = None
  ):
    def isFromFen = ~fromFen

  case class Relay(
      index: Int, // game index in the source URL
      path: Path,
      lastMoveAt: DateTime
  ):
    def secondsSinceLastMove: Int = (nowSeconds - lastMoveAt.getSeconds).toInt

  case class ServerEval(path: Path, done: Boolean)

  case class RelayAndTags(id: StudyChapterId, relay: Relay, tags: Tags):

    def looksAlive =
      tags.outcome.isEmpty &&
        relay.lastMoveAt.isAfter {
          DateTime.now.minusMinutes {
            tags.clockConfig.fold(40)(_.limitInMinutes.toInt / 2 atLeast 15 atMost 60)
          }
        }

    def looksOver = !looksAlive

  case class Metadata(
      _id: StudyChapterId,
      name: StudyChapterName,
      setup: Setup,
      outcome: Option[Option[Outcome]],
      hasRelayPath: Boolean
  ) extends Like:

    def looksOngoing = outcome.exists(_.isEmpty) && hasRelayPath

    def resultStr: Option[String] = outcome.map(o => Outcome.showResult(o).replace("1/2", "½"))

  case class IdName(id: StudyChapterId, name: StudyChapterName)

  def defaultName(order: Int) = StudyChapterName(s"Chapter $order")

  private val defaultNameRegex           = """Chapter \d+""".r
  def isDefaultName(n: StudyChapterName) = n.value.isEmpty || defaultNameRegex.matches(n.value)

  def fixName(n: StudyChapterName) = StudyChapterName(lila.common.String.softCleanUp(n.value) take 80)

  val idSize = 8
  def makeId = StudyChapterId(ThreadLocalRandom nextString idSize)

  def make(
      studyId: StudyId,
      name: StudyChapterName,
      setup: Setup,
      root: Node.Root,
      tags: Tags,
      order: Int,
      ownerId: UserId,
      practice: Boolean,
      gamebook: Boolean,
      conceal: Option[Ply],
      relay: Option[Relay] = None
  ) =
    Chapter(
      _id = makeId,
      studyId = studyId,
      name = fixName(name),
      setup = setup,
      root = root,
      tags = tags,
      order = order,
      ownerId = ownerId,
      practice = practice option true,
      gamebook = gamebook option true,
      conceal = conceal,
      relay = relay,
      createdAt = DateTime.now
    )
