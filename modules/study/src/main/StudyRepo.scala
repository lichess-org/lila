package lila.study

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import lila.user.User

private final class StudyRepo(coll: Coll) {

  import BSONHandlers._

  def byId(id: Study.ID) = coll.find(selectId(id)).one[Study]

  def exists(id: Study.ID) = coll.count(selectId(id).some).map(0<)

  def insert(s: Study): Funit = coll.insert(s).void

  def setOwnerPath(ref: Location.Ref, path: Path): Funit =
    coll.update(
      selectId(ref.studyId),
      BSONDocument("$set" -> BSONDocument(s"chapters.${ref.chapterId}.path" -> path))
    ).void

  private def selectId(id: Study.ID) = BSONDocument("_id" -> id)
}
