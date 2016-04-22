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

  def membersById(id: Study.ID): Fu[Option[StudyMembers]] =
    coll.primitiveOne[StudyMembers]($id(id), "members")

  def memberShapes(studyId: Study.ID, userId: User.ID): Fu[List[Shape]] =
    coll.find($id(studyId), $doc(s"members.$userId.shapes" -> true)).uno[BSONDocument].map { docO =>
      for {
        doc <- docO
        members <- doc.getAs[BSONDocument]("members")
        member <- members.getAs[BSONDocument](userId)
        shapes <- member.getAs[List[Shape]]("shapes")
      } yield shapes
    } map (~_)

  def setChapter(loc: Location) = coll.update(
    $id(loc.study.id),
    $set(s"chapters.${loc.chapterId}" -> loc.chapter)
  ).void

  def setMemberPosition(id: User.ID, ref: Location.Ref, path: Path): Funit =
    coll.update(
      $id(ref.studyId),
      $set(
        s"members.$id.position" -> Position.Ref(ref.chapterId, path),
        s"members.$id.shapes" -> List.empty[Shape] // also reset shapes
      )
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

  def setShapes(study: Study, userId: User.ID, shapes: List[Shape]): Funit =
    coll.update(
      $id(study.id),
      $set(s"members.$userId.shapes" -> shapes)
    ).void
}
