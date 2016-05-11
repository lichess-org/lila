package lila.study

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class StudyRepo(coll: Coll) {

  import BSONHandlers._

  def byId(id: Study.ID) = coll.byId[Study](id)

  def exists(id: Study.ID) = coll.exists($id(id))

  private val sortRecent = $sort desc "createdAt"

  def whereUidsContain(userId: User.ID, page: Int): Fu[Paginator[Study]] = Paginator(
    adapter = new Adapter[Study](
      collection = coll,
      selector = $doc("uids" -> userId),
      projection = $empty,
      sort = sortRecent),
    currentPage = page,
    maxPerPage = 10)

  def byOwner(userId: User.ID, page: Int): Fu[Paginator[Study]] = Paginator(
    adapter = new Adapter[Study](
      collection = coll,
      selector = $doc("ownerId" -> userId),
      projection = $empty,
      sort = sortRecent),
    currentPage = page,
    maxPerPage = 10)

  def insert(s: Study): Funit = coll.insert {
    StudyBSONHandler.write(s) ++ $doc("uids" -> s.members.ids)
  }.void

  def update(s: Study): Funit = coll.update($id(s.id), s).void

  def membersById(id: Study.ID): Fu[Option[StudyMembers]] =
    coll.primitiveOne[StudyMembers]($id(id), "members")

  def setPosition(studyId: Study.ID, position: Position.Ref): Funit =
    coll.update($id(studyId), $set("position" -> position)).void

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
}
