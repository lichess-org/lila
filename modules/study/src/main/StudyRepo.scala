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

  def update(s: Study): Funit = coll.update($id(s.id), s).void

  def membersById(id: Study.ID): Fu[Option[StudyMembers]] =
    coll.primitiveOne[StudyMembers]($id(id), "members")

  def setPosition(studyId: Study.ID, position: Position.Ref): Funit =
    coll.update(
      $id(studyId),
      $set(
        "position" -> position,
        "shapes" -> List.empty[Shape] // also reset shapes
      )
    ).void

  def getShapes(studyId: Study.ID): Fu[List[Shape]] =
    coll.primitiveOne[List[Shape]]($id(studyId), "shapes") map (~_)

  def setShapes(study: Study, shapes: List[Shape]): Funit =
    coll.update(
      $id(study.id),
      $set("shapes" -> shapes)
    ).void

  def addMember(study: Study, member: StudyMember): Funit =
    coll.update(
      $id(study.id),
      $set(s"members.${member.user.id}" -> member)
    ).void

  def removeMember(study: Study, userId: User.ID): Funit =
    coll.update(
      $id(study.id),
      $unset(s"members.$userId")
    ).void

  def setRole(study: Study, userId: User.ID, role: StudyMember.Role): Funit =
    coll.update(
      $id(study.id),
      $set(s"members.$userId.role" -> role)
    ).void
}
