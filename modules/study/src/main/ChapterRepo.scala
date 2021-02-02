package lila.study

import akka.stream.scaladsl._
import chess.format.pgn.Tags
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.db.dsl._

final class ChapterRepo(val coll: Coll)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import BSONHandlers._

  val noRootProjection = $doc("root" -> false)

  def byId(id: Chapter.Id): Fu[Option[Chapter]] = coll.byId[Chapter, Chapter.Id](id)

  def studyIdOf(chapterId: Chapter.Id): Fu[Option[Study.Id]] =
    coll.primitiveOne[Study.Id]($id(chapterId), "studyId")

  // def metadataById(id: Chapter.Id): Fu[Option[Chapter.Metadata]] =
  // coll.find($id(id), noRootProjection).one[Chapter.Metadata]

  def deleteByStudy(s: Study): Funit = coll.delete.one($studyId(s.id)).void

  def deleteByStudyIds(ids: List[Study.Id]): Funit = coll.delete.one($doc("studyId" $in ids)).void

  def byIdAndStudy(id: Chapter.Id, studyId: Study.Id): Fu[Option[Chapter]] =
    coll.byId[Chapter, Chapter.Id](id).dmap { _.filter(_.studyId == studyId) }

  def firstByStudy(studyId: Study.Id): Fu[Option[Chapter]] =
    coll.find($studyId(studyId)).sort($sort asc "order").one[Chapter]

  def existsByStudy(studyId: Study.Id): Fu[Boolean] =
    coll exists $studyId(studyId)

  def orderedMetadataByStudy(studyId: Study.Id): Fu[List[Chapter.Metadata]] =
    coll
      .find(
        $studyId(studyId),
        noRootProjection.some
      )
      .sort($sort asc "order")
      .cursor[Chapter.Metadata]()
      .list()

  def orderedByStudySource(studyId: Study.Id): Source[Chapter, _] =
    coll
      .find($studyId(studyId))
      .sort($sort asc "order")
      .cursor[Chapter](readPreference = ReadPreference.secondaryPreferred)
      .documentSource()

  // loads all study chapters in memory!
  def orderedByStudy(studyId: Study.Id): Fu[List[Chapter]] =
    coll
      .find($studyId(studyId))
      .sort($sort asc "order")
      .cursor[Chapter]()
      .list()

  def relaysAndTagsByStudyId(studyId: Study.Id): Fu[List[Chapter.RelayAndTags]] =
    coll
      .find(
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

  def sort(study: Study, ids: List[Chapter.Id]): Funit =
    ids.zipWithIndex
      .map { case (id, index) =>
        coll.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
      }
      .sequenceFu
      .void

  def nextOrderByStudy(studyId: Study.Id): Fu[Int] =
    coll.primitiveOne[Int](
      $studyId(studyId),
      $sort desc "order",
      "order"
    ) dmap { order =>
      ~order + 1
    }

  def setConceal(chapterId: Chapter.Id, conceal: Chapter.Ply) =
    coll.updateField($id(chapterId), "conceal", conceal).void

  def removeConceal(chapterId: Chapter.Id) =
    coll.unsetField($id(chapterId), "conceal").void

  def setRelay(chapterId: Chapter.Id, relay: Chapter.Relay) =
    coll.updateField($id(chapterId), "relay", relay).void

  def setRelayPath(chapterId: Chapter.Id, path: Path) =
    coll.updateField($id(chapterId), "relay.path", path).void

  def setTagsFor(chapter: Chapter) =
    coll.updateField($id(chapter.id), "tags", chapter.tags).void

  def setShapes(shapes: lila.tree.Node.Shapes) = setNodeValue("h", shapes.value.nonEmpty option shapes) _

  def setComments(comments: lila.tree.Node.Comments) =
    setNodeValue("co", comments.value.nonEmpty option comments) _

  def setGamebook(gamebook: lila.tree.Node.Gamebook) = setNodeValue("ga", gamebook.nonEmpty option gamebook) _

  def setGlyphs(glyphs: chess.format.pgn.Glyphs) = setNodeValue("g", glyphs.nonEmpty) _

  def setClock(clock: Option[chess.Centis]) = setNodeValue("l", clock) _

  def forceVariation(force: Boolean) = setNodeValue("fv", force option true) _

  def setScore(score: Option[lila.tree.Eval.Score]) = setNodeValue("e", score) _

  // overrides all children sub-nodes in DB! Make the tree merge beforehand.
  def setChildren(children: Node.Children)(chapter: Chapter, path: Path): Funit = {
    val parentOrderField = pathToField(path, Node.BsonFields.order)
    val (set: Bdoc, unset: Option[String]) =
      if (children.nodes.sizeIs > 1) ($doc(parentOrderField -> children.nodes.map(_.id)), None)
      else ($empty, parentOrderField.some)
    val allSet = set ++ $doc(childrenTreeToBsonElements(path, children))

    coll.update
      .one($id(chapter.id), $set(allSet) ++ unset.??(u => $unset(u)))
      .void
  }

  private def childrenTreeToBsonElements(parentPath: Path, children: Node.Children): Vector[(String, Bdoc)] =
    children.nodes.flatMap { node =>
      val path = parentPath + node
      childrenTreeToBsonElements(path, node.children) appended (path.toDbField -> writeNode(node))
    }

  private def setNodeValue[A: BSONWriter](
      field: String,
      value: Option[A]
  )(chapter: Chapter, path: Path): Funit =
    coll
      .updateOrUnsetField($id(chapter.id), pathToField(path, field), value)
      .addEffect {
        case 0 => logger.warn(s"Can't setNodeValue ${chapter.id} $path $field, no node matched!")
        case _ =>
      }
      .void

  // root.path.subField
  private def pathToField(path: Path, subField: String): String = s"${path.toDbField}.$subField"

  private[study] def idNamesByStudyIds(
      studyIds: Seq[Study.Id],
      nbChaptersPerStudy: Int
  ): Fu[Map[Study.Id, Vector[Chapter.IdName]]] =
    studyIds.nonEmpty ?? coll
      .find(
        $doc("studyId" $in studyIds),
        $doc("studyId" -> true, "_id" -> true, "name" -> true).some
      )
      .sort($sort asc "order")
      .cursor[Bdoc]()
      .list(nbChaptersPerStudy * studyIds.size)
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
    coll
      .find(
        $studyId(studyId),
        $doc("_id" -> true, "name" -> true).some
      )
      .sort($sort asc "order")
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .list(Study.maxChapters * 2)
      .dmap { _ flatMap readIdName }

  private def readIdName(doc: Bdoc) =
    for {
      id   <- doc.getAsOpt[Chapter.Id]("_id")
      name <- doc.getAsOpt[Chapter.Name]("name")
    } yield Chapter.IdName(id, name)

  def startServerEval(chapter: Chapter) =
    coll
      .updateField(
        $id(chapter.id),
        "serverEval",
        Chapter.ServerEval(
          path = chapter.root.mainlinePath,
          done = false
        )
      )
      .void

  def completeServerEval(chapter: Chapter) =
    coll.updateField($id(chapter.id), "serverEval.done", true).void

  def countByStudyId(studyId: Study.Id): Fu[Int] =
    coll.countSel($studyId(studyId))

  def insert(s: Chapter): Funit = coll.insert.one(s).void

  def update(c: Chapter): Funit = coll.update.one($id(c.id), c).void

  def delete(id: Chapter.Id): Funit = coll.delete.one($id(id)).void
  def delete(c: Chapter): Funit     = delete(c.id)

  private def $studyId(id: Study.Id) = $doc("studyId" -> id)
}
