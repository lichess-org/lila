package lila.study

import org.joda.time.DateTime
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Project, Match }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

final class StudyRepo(private[study] val coll: Coll) {

  import BSONHandlers._

  private[study] val projection = $doc(
    "uids" -> false,
    "likers" -> false,
    "views" -> false,
    "rank" -> false)

  def byId(id: Study.ID) = coll.find($id(id), projection).uno[Study]

  def byOrderedIds(ids: Seq[String]) = coll.byOrderedIds[Study](ids)(_.id)

  def cursor(selector: Bdoc) = coll.find(selector).cursor[Study]()

  def nameById(id: Study.ID) = coll.primitiveOne[String]($id(id), "name")

  def exists(id: Study.ID) = coll.exists($id(id))

  private[study] def selectOwnerId(ownerId: User.ID) = $doc("ownerId" -> ownerId)
  private[study] def selectMemberId(memberId: User.ID) = $doc("uids" -> memberId)
  private[study] val selectPublic = $doc("visibility" -> VisibilityHandler.write(Study.Visibility.Public))
  private[study] val selectPrivate = $doc("visibility" -> VisibilityHandler.write(Study.Visibility.Private))
  private[study] def selectLiker(userId: User.ID) = $doc("likers" -> userId)

  def countByOwner(ownerId: User.ID) = coll.countSel(selectOwnerId(ownerId))

  def insert(s: Study): Funit = coll.insert {
    StudyBSONHandler.write(s) ++ $doc(
      "updatedAt" -> DateTime.now,
      "uids" -> s.members.ids,
      "likers" -> List(s.ownerId),
      "rank" -> Study.Rank.compute(s.likes, s.createdAt))
  }.void

  def updateSomeFields(s: Study): Funit = coll.update($id(s.id), $set(
    "position" -> s.position,
    "name" -> s.name,
    "settings" -> s.settings,
    "visibility" -> s.visibility,
    "updatedAt" -> DateTime.now
  )).void

  def delete(s: Study): Funit = coll.remove($id(s.id)).void

  def membersById(id: Study.ID): Fu[Option[StudyMembers]] =
    coll.primitiveOne[StudyMembers]($id(id), "members")

  def setPosition(studyId: Study.ID, position: Position.Ref): Funit =
    coll.update(
      $id(studyId),
      $set(
        "position" -> position,
        "updatedAt" -> DateTime.now)
    ).void

  def incViews(study: Study) = coll.incFieldUnchecked($id(study.id), "views")

  def updateNow(s: Study) =
    coll.updateFieldUnchecked($id(s.id), "updatedAt", DateTime.now)

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

  def like(studyId: Study.ID, userId: User.ID, v: Boolean): Fu[Study.Likes] =
    doLike(studyId, userId, v) >> countLikes(studyId).flatMap {
      case None => fuccess(Study.Likes(0))
      case Some((likes, createdAt)) => coll.update($id(studyId), $set(
        "likes" -> likes,
        "rank" -> Study.Rank.compute(likes, createdAt)
      )) inject likes
    }

  def liked(study: Study, user: User): Fu[Boolean] =
    coll.exists($id(study.id) ++ selectLiker(user.id))

  def filterLiked(user: User, studyIds: Seq[Study.ID]): Fu[Set[Study.ID]] =
    coll.primitive[Study.ID]($inIds(studyIds) ++ selectLiker(user.id), "_id").map(_.toSet)

  def resetAllRanks: Fu[Int] = coll.find(
    $empty, $doc("likes" -> true, "createdAt" -> true)
  ).cursor[Bdoc]().foldWhileM(0) { (count, doc) =>
    ~(for {
      id <- doc.getAs[Study.ID]("_id")
      likes <- doc.getAs[Study.Likes]("likes")
      createdAt <- doc.getAs[DateTime]("createdAt")
    } yield coll.update(
      $id(id), $set("rank" -> Study.Rank.compute(likes, createdAt))
    ).void) inject Cursor.Cont(count + 1)
  }

  private def doLike(studyId: Study.ID, userId: User.ID, v: Boolean): Funit =
    coll.update(
      $id(studyId),
      if (v) $addToSet("likers" -> userId)
      else $pull("likers" -> userId)
    ).void

  private def countLikes(studyId: Study.ID): Fu[Option[(Study.Likes, DateTime)]] =
    coll.aggregate(
      Match($id(studyId)),
      List(Project($doc(
        "_id" -> false,
        "likes" -> $doc("$size" -> "$likers"),
        "createdAt" -> true
      )))
    ).map { res =>
        for {
          doc <- res.firstBatch.headOption
          likes <- doc.getAs[Study.Likes]("likes")
          createdAt <- doc.getAs[DateTime]("createdAt")
        } yield likes -> createdAt
      }
}
