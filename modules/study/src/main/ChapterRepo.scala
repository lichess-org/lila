package lila.study

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.dsl._

final class ChapterRepo(coll: Coll) {

  import BSONHandlers._

  def byId(id: Chapter.ID): Fu[Option[Chapter]] = coll.byId[Chapter](id)

  def byIdAndStudy(id: Chapter.ID, studyId: Study.ID): Fu[Option[Chapter]] =
    coll.byId[Chapter](id).map { _.filter(_.studyId == studyId) }

  def orderedMetadataByStudy(studyId: Study.ID): Fu[List[Chapter.Metadata]] =
    coll.find(
      $doc("studyId" -> studyId),
      $doc("root" -> false)
    ).sort($sort asc "order").list[Chapter.Metadata](64)

  def nextOrderByStudy(studyId: Study.ID): Fu[Int] =
    coll.primitiveOne[Int](
      $doc("studyId" -> studyId),
      $sort desc "order",
      "order"
    ) map { order => ~order + 1 }

  def insert(s: Chapter): Funit = coll.insert(s).void

  def update(c: Chapter): Funit = coll.update($id(c.id), c).void
}
