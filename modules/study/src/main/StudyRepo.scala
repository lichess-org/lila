package lila.study

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

final class StudyRepo(private[study] val coll: Coll) {

  import BSONHandlers._

  def byId(id: Study.ID) = coll.byId[Study](id)

  def exists(id: Study.ID) = coll.exists($id(id))

  private[study] def selectOwnerId(ownerId: User.ID) = $doc("ownerId" -> ownerId)
  private[study] def selectMemberId(memberId: User.ID) = $doc("uids" -> memberId)
  private[study] val selectPublic = $doc("visibility" -> VisibilityHandler.write(Study.Visibility.Public))
  private[study] val selectPrivate = $doc("visibility" -> VisibilityHandler.write(Study.Visibility.Private))

  def countByOwner(ownerId: User.ID) = coll.countSel(selectOwnerId(ownerId))

  def insert(s: Study): Funit = coll.insert {
    StudyBSONHandler.write(s) ++ $doc("uids" -> s.members.ids)
  }.void

  def updateSomeFields(s: Study): Funit = coll.update($id(s.id), $set(
    "position" -> s.position,
    "name" -> s.name,
    "settings" -> s.settings,
    "visibility" -> s.visibility
  )).void

  def delete(s: Study): Funit = coll.remove($id(s.id)).void

  def membersById(id: Study.ID): Fu[Option[StudyMembers]] =
    coll.primitiveOne[StudyMembers]($id(id), "members")

  def setPosition(studyId: Study.ID, position: Position.Ref): Funit =
    coll.update($id(studyId), $set("position" -> position)).void

  def incViews(study: Study) =
    coll.incFieldUnchecked($id(study.id), "views")

  def addMember(study: Study, member: StudyMember): Funit =
    coll.update(
      $id(study.id),
      $set(s"members.${member.id}" -> member) ++ $addToSet("uids" -> member.id)
    ).void

  def removeMember(study: Study, userId: User.ID): Funit =
    coll.update(
      $id(study.id),
      $unset(s"members.$userId") ++ $pull("uids" -> userId)
    ).void

  def setRole(study: Study, userId: User.ID, role: StudyMember.Role): Funit =
    coll.update(
      $id(study.id),
      $set(s"members.$userId.role" -> role)
    ).void

  def uids(studyId: Study.ID): Fu[Set[User.ID]] =
    coll.primitiveOne[Set[User.ID]]($id(studyId), "uids") map (~_)
}
