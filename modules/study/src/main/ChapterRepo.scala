package lila.study

import chess.format.pgn.Tags
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final class ChapterRepo(coll: Coll) {

  import BSONHandlers._

  val noRootProjection = $doc("root" -> false)

  def byId(id: Chapter.Id): Fu[Option[Chapter]] = coll.byId[Chapter, Chapter.Id](id)

  def studyIdOf(chapterId: Chapter.Id): Fu[Option[Study.Id]] =
    coll.primitiveOne[Study.Id]($id(chapterId), "studyId")

  // def metadataById(id: Chapter.Id): Fu[Option[Chapter.Metadata]] =
  // coll.find($id(id), noRootProjection).one[Chapter.Metadata]

  def deleteByStudy(s: Study): Funit = coll.remove($studyId(s.id)).void

  def deleteByStudyIds(ids: List[Study.Id]): Funit = coll.remove($doc("studyId" $in ids)).void

  def byIdAndStudy(id: Chapter.Id, studyId: Study.Id): Fu[Option[Chapter]] =
    coll.byId[Chapter, Chapter.Id](id).map { _.filter(_.studyId == studyId) }

  def firstByStudy(studyId: Study.Id): Fu[Option[Chapter]] =
    coll.find($studyId(studyId)).sort($sort asc "order").one[Chapter]

  def existsByStudy(studyId: Study.Id): Fu[Boolean] =
    coll exists $studyId(studyId)

  def orderedMetadataByStudy(studyId: Study.Id): Fu[List[Chapter.Metadata]] =
    coll.find(
      $studyId(studyId),
      noRootProjection
    ).sort($sort asc "order").list[Chapter.Metadata]()

  // loads all study chapters in memory! only used for search indexing and cloning
  def orderedByStudy(studyId: Study.Id): Fu[List[Chapter]] =
    coll.find($studyId(studyId))
      .sort($sort asc "order")
      .list[Chapter](none, readPreference = ReadPreference.secondaryPreferred)

  def relaysAndTagsByStudyId(studyId: Study.Id): Fu[List[Chapter.RelayAndTags]] =
    coll.find($doc("studyId" -> studyId), $doc("relay" -> true, "tags" -> true)).list[Bdoc]() map { docs =>
      for {
        doc <- docs
        id <- doc.getAs[Chapter.Id]("_id")
        relay <- doc.getAs[Chapter.Relay]("relay")
        tags <- doc.getAs[Tags]("tags")
      } yield Chapter.RelayAndTags(id, relay, tags)
    }

  def sort(study: Study, ids: List[Chapter.Id]): Funit = ids.zipWithIndex.map {
    case (id, index) =>
      coll.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
  }.sequenceFu.void

  def nextOrderByStudy(studyId: Study.Id): Fu[Int] =
    coll.primitiveOne[Int](
      $studyId(studyId),
      $sort desc "order",
      "order"
    ) map { order => ~order + 1 }

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

  def setComments(comments: lila.tree.Node.Comments) = setNodeValue("co", comments.value.nonEmpty option comments) _

  def setGamebook(gamebook: lila.tree.Node.Gamebook) = setNodeValue("ga", gamebook.nonEmpty option gamebook) _

  def setGlyphs(glyphs: chess.format.pgn.Glyphs) = setNodeValue("g", glyphs.nonEmpty) _

  def setClock(clock: Option[chess.Centis]) = setNodeValue("l", clock) _

  def forceVariation(force: Boolean) = setNodeValue("fv", force option true) _

  def setScore(score: Option[lila.tree.Eval.Score]) = setNodeValue("e", score) _

  def setChildren(children: Node.Children) = setNodeValue("n", children.some) _

  private def setNodeValue[A: BSONValueWriter](field: String, value: Option[A])(chapter: Chapter, path: Path): Funit =
    pathToField(chapter, path, field) match {
      case None =>
        logger.warn(s"Can't setNodeValue ${chapter.id} $path $field")
        funit
      case Some(field) => (value match {
        case None => coll.unsetField($id(chapter.id), field)
        case Some(v) => coll.updateField($id(chapter.id), field, v)
      }) void
    }

  // root.n.0.n.0.n.1.n.0.n.2.subField
  private def pathToField(chapter: Chapter, path: Path, subField: String): Option[String] =
    if (path.isEmpty) s"root.$subField".some
    else chapter.root.children.pathToIndexes(path) map { indexes =>
      s"root.n.${indexes.mkString(".n.")}.$subField"
    }

  private[study] def setChild(chapter: Chapter, path: Path, child: Node): Funit =
    pathToField(chapter, path, "n") ?? { parentChildrenPath =>
      coll.update(
        $id(chapter.id) ++ $doc(s"$parentChildrenPath.i" -> child.id),
        $set(s"$parentChildrenPath.$$" -> child)
      ) flatMap { res =>
          (res.n == 0) ?? coll.update($id(chapter.id), $push(parentChildrenPath -> child)).void
        }
    }

  private[study] def idNamesByStudyIds(studyIds: Seq[Study.Id], nbChaptersPerStudy: Int): Fu[Map[Study.Id, Vector[Chapter.IdName]]] =
    coll.find(
      $doc("studyId" $in studyIds),
      $doc("studyId" -> true, "_id" -> true, "name" -> true)
    ).sort($sort asc "order").list[Bdoc](nbChaptersPerStudy * studyIds.size).map { docs =>
        docs.foldLeft(Map.empty[Study.Id, Vector[Chapter.IdName]]) {
          case (hash, doc) =>
            doc.getAs[Study.Id]("studyId").fold(hash) { studyId =>
              hash get studyId match {
                case Some(chapters) if chapters.size >= nbChaptersPerStudy => hash
                case maybe =>
                  val chapters = ~maybe
                  hash + (studyId -> readIdName(doc).fold(chapters)(chapters :+ _))
              }
            }
        }
      }

  def idNames(studyId: Study.Id): Fu[List[Chapter.IdName]] =
    coll.find(
      $doc("studyId" -> studyId),
      $doc("_id" -> true, "name" -> true)
    ).sort($sort asc "order")
      .list[Bdoc](Study.maxChapters * 2, ReadPreference.secondaryPreferred)
      .map { _ flatMap readIdName }

  private def readIdName(doc: Bdoc) = for {
    id <- doc.getAs[Chapter.Id]("_id")
    name <- doc.getAs[Chapter.Name]("name")
  } yield Chapter.IdName(id, name)

  def startServerEval(chapter: Chapter) =
    coll.updateField($id(chapter.id), "serverEval", Chapter.ServerEval(
      path = chapter.root.mainlinePath,
      done = false
    )).void

  def completeServerEval(chapter: Chapter) =
    coll.updateField($id(chapter.id), "serverEval.done", true).void

  def countByStudyId(studyId: Study.Id): Fu[Int] =
    coll.countSel($studyId(studyId))

  def insert(s: Chapter): Funit = coll.insert(s).void

  def update(c: Chapter): Funit = coll.update($id(c.id), c).void

  def delete(id: Chapter.Id): Funit = coll.remove($id(id)).void
  def delete(c: Chapter): Funit = delete(c.id)

  private def $studyId(id: Study.Id) = $doc("studyId" -> id)
}
