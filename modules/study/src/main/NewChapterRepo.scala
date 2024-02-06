package lila.study

import akka.stream.scaladsl.*
import chess.format.pgn.Tags
import chess.format.UciPath
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.AsyncColl
import lila.db.dsl.{ *, given }
import lila.tree.{ Branch, Branches }

import Node.{ BsonFields as F }
import lila.tree.{ NewBranch, NewTree }

final class NewChapterRepo(val coll: AsyncColl)(using Executor, akka.stream.Materializer):

  import BSONHandlers.{ writeNewBranch, given }

  def byId(id: StudyChapterId): Fu[Option[NewChapter]] = coll(_.byId[NewChapter](id))

  def studyIdOf(chapterId: StudyChapterId): Fu[Option[StudyId]] =
    coll(_.primitiveOne[StudyId]($id(chapterId), "studyId"))

  def deleteByStudy(s: Study): Funit = coll(_.delete.one($studyId(s.id))).void

  def deleteByStudyIds(ids: List[StudyId]): Funit = coll(_.delete.one($doc("studyId" $in ids))).void

  def byIdAndStudy(id: StudyChapterId, studyId: StudyId): Fu[Option[NewChapter]] =
    coll(_.one($id(id) ++ $studyId(studyId)))

  def firstByStudy(studyId: StudyId): Fu[Option[NewChapter]] =
    coll(_.find($studyId(studyId)).sort($sort asc "order").one[NewChapter])

  private[study] def lastByStudy(studyId: StudyId): Fu[Option[NewChapter]] =
    coll(_.find($studyId(studyId)).sort($sort desc "order").one[NewChapter])

  def existsByStudy(studyId: StudyId): Fu[Boolean] =
    coll(_ exists $studyId(studyId))

  private val metadataProjection =
    $doc(
      "name"       -> true,
      "setup"      -> true,
      "relay.path" -> true,
      "tags"       -> $doc("$elemMatch" -> $doc("$regex" -> "^Result:"))
    ).some

  def orderedMetadataByStudy(studyId: StudyId): Fu[List[Chapter.Metadata]] =
    coll:
      _.find($studyId(studyId), metadataProjection)
        .sort($sort asc "order")
        .cursor[Chapter.Metadata]()
        .list(300)

  def orderedByStudySource(studyId: StudyId): Source[NewChapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($studyId(studyId))
          .sort($sort asc "order")
          .cursor[NewChapter]()
          .documentSource()

  def byIdsSource(ids: Iterable[StudyChapterId]): Source[NewChapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($inIds(ids))
          .cursor[NewChapter]()
          .documentSource()

  def relaysAndTagsByStudyId(studyId: StudyId): Fu[List[Chapter.RelayAndTags]] =
    coll:
      _.find(
        $studyId(studyId),
        $doc("relay" -> true, "tags" -> true).some
      )
        .cursor[Bdoc]()
        .list(300)
        .map: docs =>
          for
            doc   <- docs
            id    <- doc.getAsOpt[StudyChapterId]("_id")
            relay <- doc.getAsOpt[Chapter.Relay]("relay")
            tags  <- doc.getAsOpt[Tags]("tags")
          yield Chapter.RelayAndTags(id, relay, tags)

  def sort(study: Study, ids: List[StudyChapterId]): Funit =
    coll: c =>
      ids
        .mapWithIndex: (id, index) =>
          c.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
        .parallel
        .void

  def nextOrderByStudy(studyId: StudyId): Fu[Int] =
    coll(_.primitiveOne[Int]($studyId(studyId), $sort desc "order", "order")) dmap { ~_ + 1 }

  def setConceal(chapterId: StudyChapterId, conceal: chess.Ply) =
    coll(_.updateField($id(chapterId), "conceal", conceal)).void

  def removeConceal(chapterId: StudyChapterId) =
    coll(_.unsetField($id(chapterId), "conceal")).void

  def setRelay(chapterId: StudyChapterId, relay: Chapter.Relay) =
    coll(_.updateField($id(chapterId), "relay", relay)).void

  def setRelayPath(chapterId: StudyChapterId, path: UciPath) =
    coll(_.updateField($id(chapterId), "relay.path", path)).void

  def setTagsFor(chapter: NewChapter) =
    coll(_.updateField($id(chapter.id), "tags", chapter.tags)).void

  def setShapes(shapes: lila.tree.Node.Shapes) =
    setNodeValue(F.shapes, shapes.value.nonEmpty option shapes)

  def setComments(comments: lila.tree.Node.Comments) =
    setNodeValue(F.comments, comments.value.nonEmpty option comments)

  def setGamebook(gamebook: lila.tree.Node.Gamebook) =
    setNodeValue(F.gamebook, gamebook.nonEmpty option gamebook)

  def setGlyphs(glyphs: chess.format.pgn.Glyphs) = setNodeValue(F.glyphs, glyphs.nonEmpty)

  def setClock(clock: Option[chess.Centis]) = setNodeValue(F.clock, clock)

  def forceVariation(force: Boolean) = setNodeValue(F.forceVariation, force option true)

  // insert node and its children
  // and sets the parent order field
  def addSubTree(subTree: NewTree, newParent: NewTree, parentPath: UciPath)(chapter: NewChapter): Funit =
    val set = $doc(subTreeToBsonElements(parentPath, subTree))
    coll(_.update.one($id(chapter.id), $set(set))).void

  private def subTreeToBsonElements(parentPath: UciPath, subTree: NewTree): List[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES) so {
      val path = parentPath + subTree.id
      List(path.toDbField -> writeNewBranch(subTree.value))
    }

  private def setNodeValue[A: BSONWriter](
      field: String,
      value: Option[A]
  )(chapter: NewChapter, path: UciPath): Funit =
    coll:
      _.updateOrUnsetField(
        $id(chapter.id) ++ $doc(path.toDbField $exists true),
        pathToField(path, field),
        value
      ).void

  private[study] def setNodeValues(
      chapter: NewChapter,
      path: UciPath,
      values: List[(String, Option[BSONValue])]
  ): Funit =
    values.collect { case (field, Some(v)) =>
      pathToField(path, field) -> v
    } match
      case Nil => funit
      case sets =>
        coll {
          _.update
            .one(
              $id(chapter.id) ++ $doc(path.toDbField $exists true),
              $set($doc(sets))
            )
            .void
        }

  // root.path.subField
  private def pathToField(path: UciPath, subField: String): String = s"${path.toDbField}.$subField"

  private def $studyId(id: StudyId) = $doc("studyId" -> id)
