package lila.study

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

private final class StudyRepo(coll: Coll) {

  import BSONHandlers._

  def byId(id: Study.ID) = coll.byId[Study](id)

  def exists(id: Study.ID) = coll.exists($id(id))

  def insert(s: Study): Funit = coll.insert(s).void

  def membersById(id: Study.ID): Fu[Option[MemberMap]] =
    coll.primitiveOne[MemberMap]($id(id), "members")

  def setChapter(loc: Location) = coll.update(
    $id(loc.study.id),
    $set(s"chapters.${loc.chapterId}" -> loc.chapter)
  ).void

  def setMemberPosition(id: User.ID, ref: Location.Ref, path: Path): Funit =
    coll.update(
      $id(ref.studyId),
      $set(s"members.$id.position" -> Position.Ref(ref.chapterId, path))
    ).void

  def setRole(study: Study, id: User.ID, role: StudyMember.Role): Funit =
    coll.update(
      $id(study.id),
      $set(s"members.$id.role" -> role)
    ).void
}
