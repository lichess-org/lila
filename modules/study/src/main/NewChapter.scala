package lila.study

import monocle.syntax.all.*
import chess.format.pgn.{ Glyph, Tags }
import chess.format.UciPath
import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant
import chess.{ Ply, Centis, Color, Outcome }
import ornicar.scalalib.ThreadLocalRandom

import lila.tree.{ NewTree, NewRoot, NewBranch }
import lila.tree.Node.{ Comment, Gamebook, Shapes }

// Convert NewChapter <=> Chapter
// User NewChapter in parallel with Chapter => verify
case class NewChapter(
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
    this.focus(_.root).modifyF(f)

  def addBranch(node: NewBranch, path: UciPath, newRelay: Option[Chapter.Relay] = None): Option[NewChapter] =
    updateRoot: root =>
      root.addBranchAt(path, node)
    .map(_.copy(relay = newRelay orElse relay))

  def addNode(node: NewTree, path: UciPath, newRelay: Option[Chapter.Relay] = None): Option[NewChapter] =
    updateRoot:
      _.addNodeAt(path, node)
    .map(_.copy(relay = newRelay orElse relay))

  def setShapes(shapes: Shapes, path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyAt(path, _.copy(shapes = shapes)))

  def setComment(comment: Comment, path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyAt(path, _.setComment(comment)))

  def setGamebook(gamebook: Gamebook, path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyAt(path, _.copy(gamebook = gamebook.some)))

  def deleteComment(commentId: Comment.Id, path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyAt(path, _.deleteComment(commentId)))

  def toggleGlyph(glyph: Glyph, path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyAt(path, _.toggleGlyph(glyph)))

  def setClock(clock: Option[Centis], path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyAt(path, _.copy(clock = clock)))

  def forceVariation(force: Boolean, path: UciPath): Option[NewChapter] =
    updateRoot(_.modifyWithParentPath(path, _.copy(forceVariation = force)))

  def opening: Option[Opening] =
    Variant.list
      .openingSensibleVariants(setup.variant)
      .so(OpeningDb.searchInFens(root.mainlineValues.map(_.metas.fen.opening)))

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
    teams = tags(_.WhiteTeam) zip tags(_.BlackTeam),
    hasRelayPath = relay.exists(!_.path.isEmpty)
  )

  def isPractice = ~practice
  def isGamebook = ~gamebook
  def isConceal  = conceal.isDefined

  def withoutChildren = copy(root = root.withoutChildren)

  def withoutChildrenIfPractice = if isPractice then copy(root = root.withoutChildren) else this

  def relayAndTags = relay map { Chapter.RelayAndTags(id, _, tags) }

  def isOverweight = root.size >= Chapter.maxNodes

object NewChapter:

  def makeId = StudyChapterId(ThreadLocalRandom nextString 8)

  def make(
      studyId: StudyId,
      name: StudyChapterName,
      setup: Chapter.Setup,
      root: NewRoot,
      tags: Tags,
      order: Int,
      ownerId: UserId,
      practice: Boolean,
      gamebook: Boolean,
      conceal: Option[Ply],
      relay: Option[Chapter.Relay] = None
  ) =
    NewChapter(
      _id = makeId,
      studyId = studyId,
      name = Chapter.fixName(name),
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
