package lila.study

import chess.format.pgn.{ Glyph, Tags }
import chess.format.UciPath
import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant
import chess.{ Ply, Centis, Color, Outcome }
import ornicar.scalalib.ThreadLocalRandom

import lila.tree.{ Root, Branch, Branches, NewBranch, NewRoot, NewTree }
import lila.tree.Node.{ Comment, Gamebook, Shapes }

case class Chapter(
    _id: StudyChapterId,
    studyId: StudyId,
    name: StudyChapterName,
    setup: Chapter.Setup,
    root: NewRoot,
    tags: Tags,
    order: Int,
    ownerId: UserId,
    conceal: Option[Ply] = None,
    practice: Option[Boolean] = None,
    gamebook: Option[Boolean] = None,
    description: Option[String] = None,
    relay: Option[Chapter.Relay] = None,
    serverEval: Option[Chapter.ServerEval] = None,
    createdAt: Instant
) extends Chapter.Like:

  def updateRoot(f: NewRoot => Option[NewRoot]) =
    f(root) map { newRoot =>
      copy(root = newRoot)
    }

  def addNode(node: NewTree, path: UciPath, newRelay: Option[Chapter.Relay] = None): Option[Chapter] =
    updateRoot(_.addNodeAt(path, node)) map {
      _.copy(relay = newRelay orElse relay)
    }

  def setShapes(shapes: Shapes, path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPathMetas(path, _.copy(shapes = shapes)))

  def setComment(comment: Comment, path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPathMetas(path, _.setComment(comment)))

  def setGamebook(gamebook: Gamebook, path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPathMetas(path, _.copy(gamebook = gamebook.some)))

  def deleteComment(commentId: Comment.Id, path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPathMetas(path, _.deleteComment(commentId)))

  def toggleGlyph(glyph: Glyph, path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPathMetas(path, _.toggleGlyph(glyph)))

  def setClock(clock: Option[Centis], path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPathMetas(path, _.copy(clock = clock)))

  def forceVariation(force: Boolean, path: UciPath): Option[Chapter] =
    updateRoot(_.modifyWithParentPath(path, _.copy(forceVariation = force)))

  def opening: Option[Opening] =
    Variant.list.openingSensibleVariants(setup.variant) ??
      OpeningDb.searchInFens(root.mainlineValues.map(_.metas.fen.opening))

  def isEmptyInitial = order == 1 && root.isEmpty

  def cloneFor(study: Study) =
    copy(
      _id = Chapter.makeId,
      studyId = study.id,
      ownerId = study.ownerId,
      createdAt = nowInstant
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

  def withoutChildrenIfPractice = if isPractice then copy(root = root.withoutChildren) else this

  def relayAndTags = relay map { Chapter.RelayAndTags(id, _, tags) }

  def isOverweight = root.size >= Chapter.maxNodes

object Chapter:

  // I've seen chapters with 35,000 nodes on prod.
  // It works but could be used for DoS.
  val maxNodes = 3000

  trait Like:
    val _id: StudyChapterId
    val name: StudyChapterName
    val setup: Chapter.Setup
    inline def id = _id

    def initialPosition = Position.Ref(id, UciPath.root)

  case class Setup(
      gameId: Option[GameId],
      variant: Variant,
      orientation: Color,
      fromFen: Option[Boolean] = None
  ):
    def isFromFen = ~fromFen

  case class Relay(
      index: Int, // game index in the source URL
      path: UciPath,
      lastMoveAt: Instant
  ):
    def secondsSinceLastMove: Int = (nowSeconds - lastMoveAt.toSeconds).toInt

  case class ServerEval(path: UciPath, done: Boolean)

  case class RelayAndTags(id: StudyChapterId, relay: Relay, tags: Tags):

    def looksAlive =
      tags.outcome.isEmpty &&
        relay.lastMoveAt.isAfter {
          nowInstant.minusMinutes {
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

  def makeId = StudyChapterId(ThreadLocalRandom nextString 8)

  def make(
      studyId: StudyId,
      name: StudyChapterName,
      setup: Setup,
      root: NewRoot,
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
      createdAt = nowInstant
    )
