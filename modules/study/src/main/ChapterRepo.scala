package lila.study

import scala.collection.immutable.SeqMap
import akka.stream.scaladsl.*
import chess.format.UciPath
import chess.format.pgn.Tags
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.AsyncColl
import lila.db.dsl.{ *, given }
import lila.tree.{ Branch, Branches, Clock }

import Node.BsonFields as F

final class ChapterRepo(val coll: AsyncColl)(using Executor, akka.stream.Materializer):

  import BSONHandlers.{ writeBranch, given }

  val $sortOrder = $sort.asc("order")
  val $sortOrderDesc = $sort.desc("order")

  def byId(id: StudyChapterId): Fu[Option[Chapter]] = coll(_.byId[Chapter](id))

  def studyIdOf(chapterId: StudyChapterId): Fu[Option[StudyId]] =
    coll(_.primitiveOne[StudyId]($id(chapterId), "studyId"))

  def deleteByStudy(s: Study): Funit = coll(_.delete.one($studyId(s.id))).void

  def deleteByStudyIds(ids: List[StudyId]): Funit = ids.nonEmpty.so:
    coll(_.delete.one($doc("studyId".$in(ids)))).void

  // studyId is useful to ensure that the chapter belongs to the study
  def byIdAndStudy(id: StudyChapterId, studyId: StudyId): Fu[Option[Chapter]] =
    coll(_.one($id(id) ++ $studyId(studyId)))

  def firstByStudy(studyId: StudyId): Fu[Option[Chapter]] =
    coll(_.find($studyId(studyId)).sort($sortOrder).one[Chapter])

  private[study] def lastByStudy(studyId: StudyId): Fu[Option[Chapter]] =
    coll(_.find($studyId(studyId)).sort($sortOrderDesc).one[Chapter])

  def existsByStudy(studyId: StudyId): Fu[Boolean] =
    coll(_.exists($studyId(studyId)))

  def orderedByStudySource(studyId: StudyId): Source[Chapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($studyId(studyId))
          .sort($sortOrder)
          .cursor[Chapter]()
          .documentSource()

  def byIdsSource(ids: Iterable[StudyChapterId]): Source[Chapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($inIds(ids))
          .cursor[Chapter]()
          .documentSource()

  def byStudiesSource(studyIds: Seq[StudyId]): Source[Chapter, ?] =
    Source.futureSource:
      coll.map:
        _.find($doc("studyId".$in(studyIds))).cursor[Chapter]().documentSource()

  // loads all study chapters in memory!
  def orderedByStudyLoadingAllInMemory(studyId: StudyId): Fu[List[Chapter]] =
    coll:
      _.find($studyId(studyId))
        .sort($sortOrder)
        .cursor[Chapter]()
        .list(256)

  def studyIdsByRelayFideId(fideId: chess.FideId): Fu[List[StudyId]] =
    coll(_.distinctEasy[StudyId, List]("studyId", $doc("relay.fideIds" -> fideId)))

  def sort(study: Study, ids: List[StudyChapterId]): Funit =
    coll: c =>
      ids
        .mapWithIndex: (id, index) =>
          c.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
        .parallelVoid

  def nextOrderByStudy(studyId: StudyId): Fu[Chapter.Order] =
    coll(_.primitiveOne[Chapter.Order]($studyId(studyId), $sort.desc("order"), "order")).dmap { ~_ + 1 }

  def setConceal(chapterId: StudyChapterId, conceal: chess.Ply) =
    coll(_.updateField($id(chapterId), "conceal", conceal)).void

  def removeConceal(chapterId: StudyChapterId) =
    coll(_.unsetField($id(chapterId), "conceal")).void

  def setRelayPath(chapterId: StudyChapterId, path: UciPath) =
    coll(_.updateField($id(chapterId) ++ $doc("relay.lastMoveAt".$exists(true)), "relay.path", path)).void

  def setTagsFor(chapter: Chapter) =
    coll(_.updateField($id(chapter.id), "tags", chapter.tags)).void

  def setShapes(shapes: lila.tree.Node.Shapes) =
    setNodeValue(F.shapes, shapes.value.nonEmpty.option(shapes))

  def setComments(comments: lila.tree.Node.Comments) =
    setNodeValue(F.comments, comments.value.nonEmpty.option(comments))

  def setGamebook(gamebook: lila.tree.Node.Gamebook) =
    setNodeValue(F.gamebook, gamebook.nonEmpty.option(gamebook))

  def setGlyphs(glyphs: chess.format.pgn.Glyphs) = setNodeValue(F.glyphs, glyphs.nonEmpty)

  def setClockAndDenorm(
      chapter: Chapter,
      path: UciPath,
      clock: Clock,
      denorm: Option[Chapter.BothClocks]
  ) =
    val updateNode = $doc(pathToField(path, F.clock) -> clock)
    val updateDenorm = denorm.map(clocks => $doc("denorm.clocks" -> clocks))
    coll:
      _.update
        .one(
          $id(chapter.id) ++ $doc(path.toDbField.$exists(true)),
          $set(updateDenorm.foldLeft(updateNode)(_ ++ _))
        )
        .void

  def forceVariation(force: Boolean) = setNodeValue(F.forceVariation, force.option(true))

  def setName(id: StudyChapterId, name: StudyChapterName) = coll(_.updateField($id(id), "name", name)).void

  // insert node and its children
  // and updates chapter denormalization
  private[study] def addSubTree(
      chapter: Chapter,
      subTree: Branch,
      parentPath: UciPath,
      relay: Option[Chapter.Relay]
  ): Funit =
    val set = $doc(subTreeToBsonElements(parentPath, subTree)) ++
      $doc("denorm" -> chapter.denorm) ++
      relay.flatMap(toBdoc).so(r => $doc("relay" -> r))
    coll(_.update.one($id(chapter.id), $set(set))).void

  private def subTreeToBsonElements(parentPath: UciPath, subTree: Branch): List[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES).so:
      val path = parentPath + subTree.id
      subTree.children.nodes
        .flatMap(subTreeToBsonElements(path, _))
        .appended:
          path.toDbField -> writeBranch(subTree)

  // overrides all children sub-nodes in DB! Make the tree merge beforehand.
  def setChildren(children: Branches)(chapter: Chapter, path: UciPath): Funit =
    val set: Bdoc = $doc(childrenTreeToBsonElements(path, children))
    coll(_.update.one($id(chapter.id), $set(set))).void

  private def childrenTreeToBsonElements(
      parentPath: UciPath,
      children: Branches
  ): List[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES).so(children.nodes.flatMap { node =>
      val path = parentPath + node.id
      childrenTreeToBsonElements(path, node.children).appended(path.toDbField -> writeBranch(node))
    })

  private def setNodeValue[A: BSONWriter](
      field: String,
      value: Option[A]
  )(chapter: Chapter, path: UciPath): Funit =
    coll:
      _.updateOrUnsetField(
        $id(chapter.id) ++ $doc(path.toDbField.$exists(true)),
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
        coll:
          _.update
            .one(
              $id(chapter.id) ++ $doc(path.toDbField.$exists(true)),
              $set($doc(sets))
            )
            .void

  // root.path.subField
  private def pathToField(path: UciPath, subField: String): String = s"${path.toDbField}.$subField"

  private[study] def idNamesByStudyIds(
      studyIds: Seq[StudyId],
      nbChaptersPerStudy: Int
  ): Fu[Map[StudyId, Vector[Chapter.IdName]]] =
    studyIds.nonEmpty.so:
      coll:
        _.find(
          $doc("studyId".$in(studyIds)),
          $doc("studyId" -> true, "_id" -> true, "name" -> true).some
        )
          .sort($sortOrder)
          .cursor[Bdoc]()
          .list(nbChaptersPerStudy * studyIds.size)
      .map: docs =>
        docs.foldLeft(Map.empty[StudyId, Vector[Chapter.IdName]]): (hash, doc) =>
          doc.getAsOpt[StudyId]("studyId").fold(hash) { studyId =>
            hash.get(studyId) match
              case Some(chapters) if chapters.sizeIs >= nbChaptersPerStudy => hash
              case maybe =>
                val chapters = ~maybe
                hash + (studyId -> chapterIdNameHandler.readOpt(doc).fold(chapters)(chapters :+ _))
          }

  def idNames(studyId: StudyId): Fu[List[Chapter.IdName]] =
    coll:
      _.find($studyId(studyId), $doc("_id" -> true, "name" -> true).some)
        .sort($sortOrder)
        .cursor[Chapter.IdName]()
        .list(Study.maxChapters.value)

  // ordered like studyIds, then tags with the chapter order field
  def tagsByStudyIds(studyIds: Iterable[StudyId]): Fu[SeqMap[StudyId, SeqMap[StudyChapterId, Tags]]] =
    studyIds.nonEmpty.so:
      coll:
        _.find($doc("studyId".$in(studyIds)), $doc("studyId" -> true, "tags" -> true).some)
          .sort($sortOrder)
          .cursor[Bdoc]()
          .listAll()
          .map: docs =>
            for
              doc <- docs
              chapterId <- doc.getAsOpt[StudyChapterId]("_id")
              studyId <- doc.getAsOpt[StudyId]("studyId")
              tags <- doc.getAsOpt[Tags]("tags")
            yield (studyId, chapterId, tags)
          .map:
            _.groupBy(_._1).view
              .mapValues:
                _.view
                  .map: (_, chapterId, tags) =>
                    chapterId -> tags
                  .to(SeqMap)
              .toMap
          .map: hash =>
            studyIds.view
              .map: id =>
                id -> ~hash.get(id)
              .to(SeqMap)

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

  def insert(s: Chapter): Funit = coll(_.insert.one(s.updateDenorm)).void

  def update(c: Chapter): Funit = coll(_.update.one($id(c.id), c.updateDenorm)).void

  def delete(id: StudyChapterId): Funit = coll(_.delete.one($id(id))).void
  def delete(c: Chapter): Funit = delete(c.id)

  def $studyId(id: StudyId) = $doc("studyId" -> id)
