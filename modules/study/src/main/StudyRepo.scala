package lila.study

import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api._

import lila.db.dsl._
import lila.user.User

final class StudyRepo(private[study] val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  private object F {
    val uids      = "uids"
    val likers    = "likers"
    val views     = "views"
    val rank      = "rank"
    val likes     = "likes"
    val topics    = "topics"
    val createdAt = "createdAt"
  }

  private[study] val projection = $doc(
    F.uids   -> false,
    F.likers -> false,
    F.views  -> false,
    F.rank   -> false
  )

  private[study] val lightProjection = $doc(
    "_id"        -> false,
    "visibility" -> true,
    "members"    -> true
  )

  def byId(id: Study.Id) = coll.find($id(id), projection.some).one[Study]

  def byOrderedIds(ids: Seq[Study.Id]) = coll.byOrderedIds[Study, Study.Id](ids)(_.id)

  def lightById(id: Study.Id): Fu[Option[Study.LightStudy]] =
    coll.find($id(id), lightProjection.some).one[Study.LightStudy]

  def sortedCursor(
      selector: Bdoc,
      sort: Bdoc,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Study] =
    coll.find(selector).sort(sort).cursor[Study](readPreference)

  def exists(id: Study.Id) = coll.exists($id(id))

  private[study] def selectOwnerId(ownerId: User.ID)   = $doc("ownerId" -> ownerId)
  private[study] def selectMemberId(memberId: User.ID) = $doc(F.uids -> memberId)
  private[study] val selectPublic = $doc(
    "visibility" -> VisibilityHandler.writeTry(Study.Visibility.Public).get
  )
  private[study] val selectPrivateOrUnlisted = "visibility" $ne VisibilityHandler
    .writeTry(Study.Visibility.Public)
    .get
  private[study] def selectLiker(userId: User.ID) = $doc(F.likers -> userId)
  private[study] def selectContributorId(userId: User.ID) =
    selectMemberId(userId) ++ // use the index
      $doc("ownerId" $ne userId) ++
      $doc(s"members.$userId.role" -> "w")
  private[study] def selectTopic(topic: StudyTopic) = $doc(F.topics -> topic)

  def countByOwner(ownerId: User.ID) = coll.countSel(selectOwnerId(ownerId))

  def insert(s: Study): Funit =
    coll.insert.one {
      StudyBSONHandler.writeTry(s).get ++ $doc(
        F.uids   -> s.members.ids,
        F.likers -> List(s.ownerId),
        F.rank   -> Study.Rank.compute(s.likes, s.createdAt)
      )
    }.void

  def updateSomeFields(s: Study): Funit =
    coll.update
      .one(
        $id(s.id),
        $set(
          "position"    -> s.position,
          "name"        -> s.name,
          "settings"    -> s.settings,
          "visibility"  -> s.visibility,
          "description" -> ~s.description,
          "updatedAt"   -> DateTime.now
        )
      )
      .void

  def updateTopics(s: Study): Funit =
    coll.update
      .one(
        $id(s.id),
        $set("topics" -> s.topics, "updatedAt" -> DateTime.now)
      )
      .void

  def delete(s: Study): Funit = coll.delete.one($id(s.id)).void

  def deleteByIds(ids: List[Study.Id]): Funit = coll.delete.one($inIds(ids)).void

  def membersById(id: Study.Id): Fu[Option[StudyMembers]] =
    coll.primitiveOne[StudyMembers]($id(id), "members")

  def setPosition(studyId: Study.Id, position: Position.Ref): Funit =
    coll.update
      .one(
        $id(studyId),
        $set(
          "position"  -> position,
          "updatedAt" -> DateTime.now
        )
      )
      .void

  def incViews(study: Study) = coll.incFieldUnchecked($id(study.id), F.views)

  def updateNow(s: Study) =
    coll.updateFieldUnchecked($id(s.id), "updatedAt", DateTime.now)

  def addMember(study: Study, member: StudyMember): Funit =
    coll.update
      .one(
        $id(study.id),
        $set(s"members.${member.id}" -> member) ++ $addToSet(F.uids -> member.id)
      )
      .void

  def removeMember(study: Study, userId: User.ID): Funit =
    coll.update
      .one(
        $id(study.id),
        $unset(s"members.$userId") ++ $pull(F.uids -> userId)
      )
      .void

  def setRole(study: Study, userId: User.ID, role: StudyMember.Role): Funit =
    coll.update
      .one(
        $id(study.id),
        $set(s"members.$userId.role" -> role)
      )
      .void

  def uids(studyId: Study.Id): Fu[Set[User.ID]] =
    coll.primitiveOne[Set[User.ID]]($id(studyId), F.uids) map (~_)

  private val idNameProjection = $doc("name" -> true)

  def publicIdNames(ids: List[Study.Id]): Fu[List[Study.IdName]] =
    coll.find($inIds(ids) ++ selectPublic, idNameProjection.some).cursor[Study.IdName]().list()

  def recentByOwner(userId: User.ID, nb: Int) =
    coll
      .find(selectOwnerId(userId), idNameProjection.some)
      .sort($sort desc "updatedAt")
      .cursor[Study.IdName](ReadPreference.secondaryPreferred)
      .list(nb)

  // heavy AF. Only use for GDPR.
  private[study] def allIdsByOwner(userId: User.ID): Fu[List[Study.Id]] =
    coll.distinctEasy[Study.Id, List]("_id", selectOwnerId(userId), ReadPreference.secondaryPreferred)

  def recentByContributor(userId: User.ID, nb: Int) =
    coll
      .find(selectContributorId(userId), idNameProjection.some)
      .sort($sort desc "updatedAt")
      .cursor[Study.IdName](ReadPreference.secondaryPreferred)
      .list(nb)

  def isContributor(studyId: Study.Id, userId: User.ID) =
    coll.exists($id(studyId) ++ $doc(s"members.$userId.role" -> "w"))

  def isMember(studyId: Study.Id, userId: User.ID) =
    coll.exists($id(studyId) ++ (s"members.$userId" $exists true))

  def like(studyId: Study.Id, userId: User.ID, v: Boolean): Fu[Study.Likes] =
    countLikes(studyId).flatMap {
      case None => fuccess(Study.Likes(0))
      case Some((prevLikes, createdAt)) =>
        val likes = Study.Likes(prevLikes.value + (if (v) 1 else -1))
        coll.update.one(
          $id(studyId),
          $set(
            F.likes -> likes,
            F.rank  -> Study.Rank.compute(likes, createdAt)
          ) ++ {
            if (v) $addToSet(F.likers -> userId) else $pull(F.likers -> userId)
          }
        ) inject likes
    }

  def liked(study: Study, user: User): Fu[Boolean] =
    coll.exists($id(study.id) ++ selectLiker(user.id))

  def filterLiked(user: User, studyIds: Seq[Study.Id]): Fu[Set[Study.Id]] =
    studyIds.nonEmpty ??
      coll.primitive[Study.Id]($inIds(studyIds) ++ selectLiker(user.id), "_id").dmap(_.toSet)

  def resetAllRanks: Fu[Int] =
    coll
      .find(
        $empty,
        $doc(F.likes -> true, F.createdAt -> true).some
      )
      .cursor[Bdoc]()
      .foldWhileM(0) { (count, doc) =>
        ~(for {
          id        <- doc.getAsOpt[Study.Id]("_id")
          likes     <- doc.getAsOpt[Study.Likes](F.likes)
          createdAt <- doc.getAsOpt[DateTime](F.createdAt)
        } yield coll.update
          .one(
            $id(id),
            $set(F.rank -> Study.Rank.compute(likes, createdAt))
          )
          .void) inject Cursor.Cont(count + 1)
      }

  private[study] def isAdminMember(study: Study, userId: User.ID): Fu[Boolean] =
    coll.exists($id(study.id) ++ $doc(s"members.$userId.admin" -> true))

  private def countLikes(studyId: Study.Id): Fu[Option[(Study.Likes, DateTime)]] =
    coll
      .aggregateWith[Bdoc]() { framework =>
        import framework._
        List(
          Match($id(studyId)),
          Project(
            $doc(
              "_id"       -> false,
              F.likes     -> $doc("$size" -> s"$$${F.likers}"),
              F.createdAt -> true
            )
          )
        )
      }
      .headOption
      .map { docOption =>
        for {
          doc       <- docOption
          likes     <- doc.getAsOpt[Study.Likes](F.likes)
          createdAt <- doc.getAsOpt[DateTime](F.createdAt)
        } yield likes -> createdAt
      }
}
