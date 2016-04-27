package lila.study

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.dsl._

final class ChapterRepo(coll: Coll) {

  import BSONHandlers._

  val maxChapters = 64

  def byId(id: Chapter.ID): Fu[Option[Chapter]] = coll.byId[Chapter](id)

  def byIdAndStudy(id: Chapter.ID, studyId: Study.ID): Fu[Option[Chapter]] =
    coll.byId[Chapter](id).map { _.filter(_.studyId == studyId) }

  def orderedMetadataByStudy(studyId: Study.ID): Fu[List[Chapter.Metadata]] =
    coll.find(
      $studyId(studyId),
      $doc("root" -> false)
    ).sort($sort asc "order").list[Chapter.Metadata](maxChapters)

  def orderedByStudy(studyId: Study.ID): Fu[List[Chapter]] =
    coll.find($studyId(studyId)).sort($sort asc "order").list[Chapter](maxChapters)

  def nextOrderByStudy(studyId: Study.ID): Fu[Int] =
    coll.primitiveOne[Int](
      $studyId(studyId),
      $sort desc "order",
      "order"
    ) map { order => ~order + 1 }

  def countByStudyId(studyId: Study.ID): Fu[Int] =
    coll.countSel($studyId(studyId))

  def insert(s: Chapter): Funit = coll.insert(s).void

  def update(c: Chapter): Funit = coll.update($id(c.id), c).void

  def delete(id: Chapter.ID): Funit = coll.remove($id(id)).void

  private def $studyId(id: Study.ID) = $doc("studyId" -> id)
}
