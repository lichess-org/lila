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

final class ChapterRepo(val coll: AsyncColl)(using Executor, akka.stream.Materializer):

  import BSONHandlers.{ writeBranch, given }

  def byId(id: StudyChapterId): Fu[Option[Chapter]] = coll(_.byId[Chapter](id))

  def studyIdOf(chapterId: StudyChapterId): Fu[Option[StudyId]] =
    coll(_.primitiveOne[StudyId]($id(chapterId), "studyId"))

  def deleteByStudy(s: Study): Funit = coll(_.delete.one($studyId(s.id))).void

  def deleteByStudyIds(ids: List[StudyId]): Funit = coll(_.delete.one($doc("studyId" $in ids))).void

  def byIdAndStudy(id: StudyChapterId, studyId: StudyId): Fu[Option[Chapter]] =
    coll(_.one($id(id) ++ $studyId(studyId)))

  def firstByStudy(studyId: StudyId): Fu[Option[Chapter]] =
    coll(_.find($studyId(studyId)).sort($sort asc "order").one[Chapter])

  private[study] def lastByStudy(studyId: StudyId): Fu[Option[Chapter]] =
    coll(_.find($studyId(studyId)).sort($sort desc "order").one[Chapter])

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

  def orderedByStudySource(studyId: StudyId): Source[Chapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($studyId(studyId))
          .sort($sort asc "order")
          .cursor[Chapter]()
          .documentSource()

  def byIdsSource(ids: Iterable[StudyChapterId]): Source[Chapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($inIds(ids))
          .cursor[Chapter]()
          .documentSource()

  // loads all study chapters in memory!
  def orderedByStudy(studyId: StudyId): Fu[List[Chapter]] =
    coll:
      _.find($studyId(studyId))
        .sort($sort asc "order")
        .cursor[Chapter]()
        .list(300)

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

  def setTagsFor(chapter: Chapter) =
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
  def addSubTree(subTree: Branch, newParent: lila.tree.Node, parentPath: UciPath)(chapter: Chapter): Funit =
    val set = $doc(subTreeToBsonElements(parentPath, subTree)) ++ {
      (newParent.children.nodes.sizeIs > 1) so $doc(
        pathToField(parentPath, F.order) -> newParent.children.nodes.map(_.id)
      )
    }
    coll(_.update.one($id(chapter.id), $set(set))).void

  private def subTreeToBsonElements(parentPath: UciPath, subTree: Branch): List[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES) so {
      val path = parentPath + subTree.id
      subTree.children.nodes.flatMap(subTreeToBsonElements(path, _)) appended {
        path.toDbField -> writeBranch(subTree)
      }
    }

  // overrides all children sub-nodes in DB! Make the tree merge beforehand.
  def setChildren(children: Branches)(chapter: Chapter, path: UciPath): Funit =
    val set: Bdoc = {
      (children.nodes.sizeIs > 1) so $doc(
        pathToField(path, F.order) -> children.nodes.map(_.id)
      )
    } ++ $doc(childrenTreeToBsonElements(path, children))

    coll(_.update.one($id(chapter.id), $set(set))).void

  private def childrenTreeToBsonElements(
      parentPath: UciPath,
      children: Branches
  ): List[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES) so
      children.nodes.flatMap { node =>
        val path = parentPath + node.id
        childrenTreeToBsonElements(path, node.children) appended (path.toDbField -> writeBranch(node))
      }

  private def setNodeValue[A: BSONWriter](
      field: String,
      value: Option[A]
  )(chapter: Chapter, path: UciPath): Funit =
    coll:
      _.updateOrUnsetField(
        $id(chapter.id) ++ $doc(path.toDbField $exists true),
        pathToField(path, field),
        value
      ).void

  private[study] def setNodeValues(
      chapter: Chapter,
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

  private[study] def idNamesByStudyIds(
      studyIds: Seq[StudyId],
      nbChaptersPerStudy: Int
  ): Fu[Map[StudyId, Vector[Chapter.IdName]]] =
    studyIds.nonEmpty so coll {
      _.find(
        $doc("studyId" $in studyIds),
        $doc("studyId" -> true, "_id" -> true, "name" -> true).some
      )
        .sort($sort asc "order")
        .cursor[Bdoc]()
        .list(nbChaptersPerStudy * studyIds.size)
    }
      .map { docs =>
        docs.foldLeft(Map.empty[StudyId, Vector[Chapter.IdName]]) { case (hash, doc) =>
          doc.getAsOpt[StudyId]("studyId").fold(hash) { studyId =>
            hash get studyId match
              case Some(chapters) if chapters.sizeIs >= nbChaptersPerStudy => hash
              case maybe =>
                val chapters = ~maybe
                hash + (studyId -> readIdName(doc).fold(chapters)(chapters :+ _))
          }
        }
      }

  def idNames(studyId: StudyId): Fu[List[Chapter.IdName]] =
    coll:
      _.find($studyId(studyId), $doc("_id" -> true, "name" -> true).some)
        .sort($sort asc "order")
        .cursor[Bdoc]()
        .list(Study.maxChapters * 2)
    .dmap(_ flatMap readIdName)

  private def readIdName(doc: Bdoc) =
    for
      id   <- doc.getAsOpt[StudyChapterId]("_id")
      name <- doc.getAsOpt[StudyChapterName]("name")
    yield Chapter.IdName(id, name)

  def tagsByStudyIds(studyIds: Iterable[StudyId]): Fu[List[Tags]] =
    studyIds.nonEmpty so coll { _.primitive[Tags]("studyId" $in studyIds, "tags") }

  def startServerEval(chapter: Chapter) =
    coll:
      _.updateField(
        $id(chapter.id),
        "serverEval",
        Chapter.ServerEval(
          path = chapter.root.mainlinePath,
          done = false
        )
      )
    .void

  def completeServerEval(chapter: Chapter) =
    coll(_.updateField($id(chapter.id), "serverEval.done", true)).void

  def countByStudyId(studyId: StudyId): Fu[Int] =
    coll(_.countSel($studyId(studyId)))

  def insert(s: Chapter): Funit = coll(_.insert one s).void

  def update(c: Chapter): Funit = coll(_.update.one($id(c.id), c)).void

  def delete(id: StudyChapterId): Funit = coll(_.delete.one($id(id))).void
  def delete(c: Chapter): Funit         = delete(c.id)

  private def $studyId(id: StudyId) = $doc("studyId" -> id)
