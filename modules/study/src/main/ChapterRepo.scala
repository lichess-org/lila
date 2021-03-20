package lila.study

import akka.stream.scaladsl._
import chess.format.pgn.Tags
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.db.AsyncColl
import lila.db.dsl._

final class ChapterRepo(val coll: AsyncColl)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import BSONHandlers._

  def byId(id: Chapter.Id): Fu[Option[Chapter]] = coll(_.byId[Chapter, Chapter.Id](id))

  def studyIdOf(chapterId: Chapter.Id): Fu[Option[Study.Id]] =
    coll(_.primitiveOne[Study.Id]($id(chapterId), "studyId"))

  def deleteByStudy(s: Study): Funit = coll(_.delete.one($studyId(s.id))).void

  def deleteByStudyIds(ids: List[Study.Id]): Funit = coll(_.delete.one($doc("studyId" $in ids))).void

  def byIdAndStudy(id: Chapter.Id, studyId: Study.Id): Fu[Option[Chapter]] =
    coll(_.byId[Chapter, Chapter.Id](id)).dmap { _.filter(_.studyId == studyId) }

  def firstByStudy(studyId: Study.Id): Fu[Option[Chapter]] =
    coll(_.find($studyId(studyId)).sort($sort asc "order").one[Chapter])

  def existsByStudy(studyId: Study.Id): Fu[Boolean] =
    coll(_ exists $studyId(studyId))

  private val metadataProjection =
    $doc(
      "name"       -> true,
      "setup"      -> true,
      "relay.path" -> true,
      "tags"       -> $doc("$elemMatch" -> $doc("$regex" -> "^Result:"))
    ).some

  def orderedMetadataByStudy(studyId: Study.Id): Fu[List[Chapter.Metadata]] =
    coll {
      _.find($studyId(studyId), metadataProjection)
        .sort($sort asc "order")
        .cursor[Chapter.Metadata]()
        .list()
    }

  def orderedByStudySource(studyId: Study.Id): Source[Chapter, _] =
    Source futureSource {
      coll map {
        _.find($studyId(studyId))
          .sort($sort asc "order")
          .cursor[Chapter](readPreference = readPref)
          .documentSource()
      }
    }

  // loads all study chapters in memory!
  def orderedByStudy(studyId: Study.Id): Fu[List[Chapter]] =
    coll {
      _.find($studyId(studyId))
        .sort($sort asc "order")
        .cursor[Chapter]()
        .list()
    }

  def relaysAndTagsByStudyId(studyId: Study.Id): Fu[List[Chapter.RelayAndTags]] =
    coll {
      _.find(
        $studyId(studyId),
        $doc("relay" -> true, "tags" -> true).some
      )
        .cursor[Bdoc]()
        .list() map { docs =>
        for {
          doc   <- docs
          id    <- doc.getAsOpt[Chapter.Id]("_id")
          relay <- doc.getAsOpt[Chapter.Relay]("relay")
          tags  <- doc.getAsOpt[Tags]("tags")
        } yield Chapter.RelayAndTags(id, relay, tags)
      }
    }

  def sort(study: Study, ids: List[Chapter.Id]): Funit =
    coll { c =>
      ids.zipWithIndex
        .map { case (id, index) =>
          c.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
        }
        .sequenceFu
        .void
    }

  def nextOrderByStudy(studyId: Study.Id): Fu[Int] =
    coll(
      _.primitiveOne[Int](
        $studyId(studyId),
        $sort desc "order",
        "order"
      )
    ) dmap { order =>
      ~order + 1
    }

  def setConceal(chapterId: Chapter.Id, conceal: Chapter.Ply) =
    coll(_.updateField($id(chapterId), "conceal", conceal)).void

  def removeConceal(chapterId: Chapter.Id) =
    coll(_.unsetField($id(chapterId), "conceal")).void

  def setRelay(chapterId: Chapter.Id, relay: Chapter.Relay) =
    coll(_.updateField($id(chapterId), "relay", relay)).void

  def setRelayPath(chapterId: Chapter.Id, path: Path) =
    coll(_.updateField($id(chapterId), "relay.path", path)).void

  def setTagsFor(chapter: Chapter) =
    coll(_.updateField($id(chapter.id), "tags", chapter.tags)).void

  def setShapes(shapes: lila.tree.Node.Shapes) =
    setNodeValue(Node.BsonFields.shapes, shapes.value.nonEmpty option shapes) _

  def setComments(comments: lila.tree.Node.Comments) =
    setNodeValue(Node.BsonFields.comments, comments.value.nonEmpty option comments) _

  def setGamebook(gamebook: lila.tree.Node.Gamebook) =
    setNodeValue(Node.BsonFields.gamebook, gamebook.nonEmpty option gamebook) _

  def setGlyphs(glyphs: chess.format.pgn.Glyphs) = setNodeValue(Node.BsonFields.glyphs, glyphs.nonEmpty) _

  def setClock(clock: Option[chess.Centis]) = setNodeValue(Node.BsonFields.clock, clock) _

  def forceVariation(force: Boolean) = setNodeValue(Node.BsonFields.forceVariation, force option true) _

  // insert node and its children
  // and sets the parent order field
  def addSubTree(subTree: Node, newParent: RootOrNode, parentPath: Path)(chapter: Chapter): Funit = {
    val set = $doc(subTreeToBsonElements(parentPath, subTree)) ++ {
      (newParent.children.nodes.sizeIs > 1) ?? $doc(
        pathToField(parentPath, Node.BsonFields.order) -> newParent.children.nodes.map(_.id)
      )
    }
    coll(_.update.one($id(chapter.id), $set(set))).void
  }

  private def subTreeToBsonElements(parentPath: Path, subTree: Node): Vector[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES) ?? {
      val path = parentPath + subTree
      subTree.children.nodes.flatMap(subTreeToBsonElements(path, _)) appended {
        path.toDbField -> writeNode(subTree)
      }
    }

  // overrides all children sub-nodes in DB! Make the tree merge beforehand.
  def setChildren(children: Node.Children)(chapter: Chapter, path: Path): Funit = {

    val set: Bdoc = {
      (children.nodes.sizeIs > 1) ?? $doc(
        pathToField(path, Node.BsonFields.order) -> children.nodes.map(_.id)
      )
    } ++ $doc(childrenTreeToBsonElements(path, children))

    coll(_.update.one($id(chapter.id), $set(set))).void
  }

  private def childrenTreeToBsonElements(parentPath: Path, children: Node.Children): Vector[(String, Bdoc)] =
    (parentPath.depth < Node.MAX_PLIES) ??
      children.nodes.flatMap { node =>
        val path = parentPath + node
        childrenTreeToBsonElements(path, node.children) appended (path.toDbField -> writeNode(node))
      }

  private def setNodeValue[A: BSONWriter](
      field: String,
      value: Option[A]
  )(chapter: Chapter, path: Path): Funit =
    coll {
      _.updateOrUnsetField(
        $id(chapter.id) ++ $doc(path.toDbField $exists true),
        pathToField(path, field),
        value
      ).void
    }

  private[study] def setNodeValues(
      chapter: Chapter,
      path: Path,
      values: List[(String, Option[BSONValue])]
  ): Funit =
    values.collect { case (field, Some(v)) =>
      pathToField(path, field) -> v
    } match {
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
    }

  // root.path.subField
  private def pathToField(path: Path, subField: String): String = s"${path.toDbField}.$subField"

  private[study] def idNamesByStudyIds(
      studyIds: Seq[Study.Id],
      nbChaptersPerStudy: Int
  ): Fu[Map[Study.Id, Vector[Chapter.IdName]]] =
    studyIds.nonEmpty ?? coll(
      _.find(
        $doc("studyId" $in studyIds),
        $doc("studyId" -> true, "_id" -> true, "name" -> true).some
      )
        .sort($sort asc "order")
        .cursor[Bdoc]()
        .list(nbChaptersPerStudy * studyIds.size)
    )
      .map { docs =>
        docs.foldLeft(Map.empty[Study.Id, Vector[Chapter.IdName]]) { case (hash, doc) =>
          doc.getAsOpt[Study.Id]("studyId").fold(hash) { studyId =>
            hash get studyId match {
              case Some(chapters) if chapters.sizeIs >= nbChaptersPerStudy => hash
              case maybe =>
                val chapters = ~maybe
                hash + (studyId -> readIdName(doc).fold(chapters)(chapters :+ _))
            }
          }
        }
      }

  def idNames(studyId: Study.Id): Fu[List[Chapter.IdName]] =
    coll {
      _.find(
        $studyId(studyId),
        $doc("_id" -> true, "name" -> true).some
      )
        .sort($sort asc "order")
        .cursor[Bdoc](readPref)
        .list(Study.maxChapters * 2)
    }
      .dmap { _ flatMap readIdName }

  private def readIdName(doc: Bdoc) =
    for {
      id   <- doc.getAsOpt[Chapter.Id]("_id")
      name <- doc.getAsOpt[Chapter.Name]("name")
    } yield Chapter.IdName(id, name)

  def startServerEval(chapter: Chapter) =
    coll {
      _.updateField(
        $id(chapter.id),
        "serverEval",
        Chapter.ServerEval(
          path = chapter.root.mainlinePath,
          done = false
        )
      )
    }.void

  def completeServerEval(chapter: Chapter) =
    coll(_.updateField($id(chapter.id), "serverEval.done", true)).void

  def countByStudyId(studyId: Study.Id): Fu[Int] =
    coll(_.countSel($studyId(studyId)))

  def insert(s: Chapter): Funit = coll(_.insert one s).void

  def update(c: Chapter): Funit = coll(_.update.one($id(c.id), c)).void

  def delete(id: Chapter.Id): Funit = coll(_.delete.one($id(id))).void
  def delete(c: Chapter): Funit     = delete(c.id)

  private def $studyId(id: Study.Id) = $doc("studyId" -> id)
}
