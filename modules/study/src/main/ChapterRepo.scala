package lila.study

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.dsl._

final class ChapterRepo(coll: Coll) {

  import BSONHandlers._

  val maxChapters = 64

  def byId(id: Chapter.ID): Fu[Option[Chapter]] = coll.byId[Chapter](id)

  def deleteByStudy(s: Study): Funit = coll.remove($studyId(s.id)).void

  def byIdAndStudy(id: Chapter.ID, studyId: Study.ID): Fu[Option[Chapter]] =
    coll.byId[Chapter](id).map { _.filter(_.studyId == studyId) }

  def firstByStudy(studyId: Study.ID): Fu[Option[Chapter]] =
    coll.find($studyId(studyId)).sort($sort asc "order").one[Chapter]

  def orderedMetadataByStudy(studyId: Study.ID): Fu[List[Chapter.Metadata]] =
    coll.find(
      $studyId(studyId),
      $doc("root" -> false)
    ).sort($sort asc "order").list[Chapter.Metadata](maxChapters)

  def orderedByStudy(studyId: Study.ID): Fu[List[Chapter]] =
    coll.find($studyId(studyId)).sort($sort asc "order").list[Chapter](maxChapters)

  def sort(study: Study, ids: List[Chapter.ID]): Funit = ids.zipWithIndex.map {
    case (id, index) =>
      coll.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
  }.sequenceFu.void

  def nextOrderByStudy(studyId: Study.ID): Fu[Int] =
    coll.primitiveOne[Int](
      $studyId(studyId),
      $sort desc "order",
      "order"
    ) map { order => ~order + 1 }

  def setConceal(chapterId: Chapter.ID, conceal: Chapter.Ply) =
    coll.updateField($id(chapterId), "conceal", conceal.value).void

  def removeConceal(chapterId: Chapter.ID) =
    coll.unsetField($id(chapterId), "conceal").void

  def namesByStudyIds(studyIds: Seq[Study.ID]): Fu[Map[Study.ID, Vector[String]]] =
    coll.find(
      $doc("studyId" -> $doc("$in" -> studyIds)),
      $doc("studyId" -> true, "name" -> true)
    ).sort($sort asc "order").list[Bdoc]().map { docs =>
        docs.foldLeft(Map.empty[Study.ID, Vector[String]]) {
          case (hash, doc) => {
            for {
              studyId <- doc.getAs[String]("studyId")
              name <- doc.getAs[String]("name")
            } yield hash + (studyId -> (hash.get(studyId) match {
              case None        => Vector(name)
              case Some(names) => names :+ name
            }))
          } | hash
        }
      }

  def countByStudyId(studyId: Study.ID): Fu[Int] =
    coll.countSel($studyId(studyId))

  def insert(s: Chapter): Funit = coll.insert(s).void

  def update(c: Chapter): Funit = coll.update($id(c.id), c).void

  def delete(id: Chapter.ID): Funit = coll.remove($id(id)).void
  def delete(c: Chapter): Funit = delete(c.id)

  private def $studyId(id: Study.ID) = $doc("studyId" -> id)
}
