package lila.study

import akka.stream.scaladsl.*
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.*

import lila.db.AsyncColl
import lila.db.dsl.{ *, given }
import lila.user.User
import reactivemongo.api.bson.BSONDocument

final class StudyRepo(private[study] val coll: AsyncColl)(using
    Executor,
    akka.stream.Materializer
):

  import BSONHandlers.given

  private object F:
    val uids      = "uids"
    val likers    = "likers"
    val rank      = "rank"
    val likes     = "likes"
    val topics    = "topics"
    val createdAt = "createdAt"

  private[study] val projection = $doc(
    F.uids   -> false,
    F.likers -> false,
    F.rank   -> false
  )

  private[study] val lightProjection = $doc(
    "_id"        -> false,
    "visibility" -> true,
    "members"    -> true
  )

  def byId(id: StudyId) = coll(_.find($id(id), projection.some).one[Study])

  def byIdWithChapter(
      chapterColl: AsyncColl
  )(id: StudyId, chapterId: StudyChapterId): Fu[Option[Study.WithChapter]] =
    coll:
      _.aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(
            $lookup.pipeline(
              from = chapterColl,
              local = "_id",
              foreign = "studyId",
              as = "chapter",
              pipe = List($doc("$match" -> $doc("_id" -> chapterId)))
            )
          ),
          UnwindField("chapter")
        )
      .map: docs =>
        for
          doc     <- docs
          chapter <- doc.getAsOpt[Chapter]("chapter")
          study   <- doc.asOpt[Study]
        yield Study.WithChapter(study, chapter)

  def byOrderedIds(ids: Seq[StudyId]) = coll(_.byOrderedIds[Study, StudyId](ids)(_.id))

  def lightById(id: StudyId): Fu[Option[Study.LightStudy]] =
    coll(_.find($id(id), lightProjection.some).one[Study.LightStudy])

  def sortedCursor(
      selector: Bdoc,
      sort: Bdoc,
      readPreference: ReadPreference = readPref
  ): Fu[AkkaStreamCursor[Study]] =
    coll.map(_.find(selector).sort(sort).cursor[Study](readPreference))

  def exists(id: StudyId) = coll(_.exists($id(id)))

  def lookup(local: String) = $lookup.simple(coll, "study", local, "_id")

  private[study] def selectOwnerId(ownerId: UserId)   = $doc("ownerId" -> ownerId)
  private[study] def selectMemberId(memberId: UserId) = $doc(F.uids -> memberId)
  private[study] val selectPublic = $doc(
    "visibility" -> (Study.Visibility.Public: Study.Visibility)
  )
  private[study] val selectPrivateOrUnlisted =
    "visibility" $ne (Study.Visibility.Public: Study.Visibility)
  private[study] def selectLiker(userId: UserId) = $doc(F.likers -> userId)
  private[study] def selectContributorId(userId: UserId): BSONDocument =
    selectMemberId(userId) ++ // use the index
      $doc("ownerId" $ne userId) ++
      $doc(s"members.$userId.role" -> "w")
  private[study] def selectTopic(topic: StudyTopic) = $doc(F.topics -> topic)

  def countByOwner(ownerId: UserId) = coll(_.countSel(selectOwnerId(ownerId)))

  def sourceByOwner(ownerId: UserId, isMe: Boolean): Source[Study, ?] =
    Source.futureSource:
      coll.map:
        _.find(selectOwnerId(ownerId) ++ (!isMe so selectPublic))
          .sort($sort desc "updatedAt")
          .cursor[Study](readPreference = readPref)
          .documentSource()

  def insert(s: Study): Funit =
    coll:
      _.insert.one:
        studyHandler.writeTry(s).get ++ $doc(
          F.uids   -> s.members.ids,
          F.likers -> List(s.ownerId),
          F.rank   -> Study.Rank.compute(s.likes, s.createdAt)
        )
    .void

  def updateSomeFields(s: Study): Funit =
    coll:
      _.update
        .one(
          $id(s.id),
          $set(
            "position"    -> s.position,
            "name"        -> s.name,
            "settings"    -> s.settings,
            "visibility"  -> s.visibility,
            "description" -> ~s.description,
            "updatedAt"   -> nowInstant
          )
        )
    .void

  def updateTopics(s: Study): Funit =
    coll:
      _.update
        .one(
          $id(s.id),
          $set("topics" -> s.topics, "updatedAt" -> nowInstant)
        )
    .void

  def delete(s: Study): Funit = coll(_.delete.one($id(s.id))).void

  def deleteByIds(ids: List[StudyId]): Funit = coll(_.delete.one($inIds(ids))).void

  def membersById(id: StudyId): Fu[Option[StudyMembers]] =
    coll(_.primitiveOne[StudyMembers]($id(id), "members"))

  def membersByIds(ids: Iterable[StudyId]): Fu[List[StudyMembers]] =
    coll(_.primitive[StudyMembers]($inIds(ids), "members"))

  def setPosition(studyId: StudyId, position: Position.Ref): Funit =
    coll:
      _.update
        .one(
          $id(studyId),
          $set(
            "position"  -> position,
            "updatedAt" -> nowInstant
          )
        )
    .void

  def updateNow(s: Study): Funit =
    coll.map(_.updateFieldUnchecked($id(s.id), "updatedAt", nowInstant))

  def addMember(study: Study, member: StudyMember): Funit =
    coll:
      _.update
        .one(
          $id(study.id),
          $set(s"members.${member.id}" -> member) ++ $addToSet(F.uids -> member.id)
        )
    .void

  def removeMember(study: Study, userId: UserId): Funit =
    coll:
      _.update
        .one(
          $id(study.id),
          $unset(s"members.$userId") ++ $pull(F.uids -> userId)
        )
    .void

  def setRole(study: Study, userId: UserId, role: StudyMember.Role): Funit =
    coll:
      _.update
        .one(
          $id(study.id),
          $set(s"members.$userId.role" -> role)
        )
    .void

  def uids(studyId: StudyId): Fu[Set[UserId]] =
    coll(_.primitiveOne[Set[UserId]]($id(studyId), F.uids)).dmap(~_)

  private val idNameProjection = $doc("name" -> true)

  def publicIdNames(ids: List[StudyId]): Fu[List[Study.IdName]] =
    coll(_.find($inIds(ids) ++ selectPublic, idNameProjection.some).cursor[Study.IdName]().listAll())

  def recentByOwnerWithChapterCount(
      chapterColl: AsyncColl
  )(userId: UserId, nb: Int): Fu[List[(Study.IdName, Int)]] =
    findRecentStudyWithChapterCount(selectOwnerId)(chapterColl)(userId, nb)

  def recentByContributorWithChapterCount(
      chapterColl: AsyncColl
  )(userId: UserId, nb: Int): Fu[List[(Study.IdName, Int)]] =
    findRecentStudyWithChapterCount(selectContributorId)(chapterColl)(userId, nb)

  private def findRecentStudyWithChapterCount(query: UserId => BSONDocument)(
      chapterColl: AsyncColl
  )(userId: UserId, nb: Int) =
    coll:
      _.aggregateList(nb) { framework =>
        import framework.*
        Match(query(userId)) -> List(
          Sort(Descending("updatedAt")),
          Project(idNameProjection),
          PipelineOperator(
            $lookup.pipeline(
              from = chapterColl,
              as = "chapters",
              local = "_id",
              foreign = "studyId",
              pipe = List($doc("$project" -> $id(true)))
            )
          ),
          AddFields($doc("chapters" -> $doc("$size" -> "$chapters")))
        )
      }
        .map: docs =>
          for
            doc        <- docs
            idName     <- idNameHandler.readOpt(doc)
            nbChapters <- doc.int("chapters")
          yield (idName, nbChapters)

  def isContributor(studyId: StudyId, userId: UserId) =
    coll(_.exists($id(studyId) ++ $doc(s"members.$userId.role" -> "w")))

  def isMember(studyId: StudyId, userId: UserId) =
    coll(_.exists($id(studyId) ++ (s"members.$userId" $exists true)))

  def like(studyId: StudyId, userId: UserId, v: Boolean): Fu[Study.Likes] =
    coll: c =>
      c.update.one($id(studyId), if (v) $addToSet(F.likers -> userId) else $pull(F.likers -> userId)) >> {
        countLikes(studyId).flatMap:
          case None                     => fuccess(Study.Likes(0))
          case Some((likes, createdAt)) =>
            // Multiple updates may race to set denormalized likes and rank,
            // but values should be approximately correct, match a real like
            // count (though perhaps not the latest one), and any uncontended
            // query will set the precisely correct value.
            c.update.one(
              $id(studyId),
              $set(F.likes -> likes, F.rank -> Study.Rank.compute(likes, createdAt))
            ) inject likes
      }

  def liked(study: Study, user: User): Fu[Boolean] =
    coll(_.exists($id(study.id) ++ selectLiker(user.id)))

  def filterLiked(user: User, studyIds: Seq[StudyId]): Fu[Set[StudyId]] =
    studyIds.nonEmpty so
      coll(_.primitive[StudyId]($inIds(studyIds) ++ selectLiker(user.id), "_id").dmap(_.toSet))

  def resetAllRanks: Fu[Int] =
    coll:
      _.find(
        $empty,
        $doc(F.likes -> true, F.createdAt -> true).some
      )
        .cursor[Bdoc]()
        .foldWhileM(0) { (count, doc) =>
          ~(for {
            id        <- doc.getAsOpt[StudyId]("_id")
            likes     <- doc.getAsOpt[Study.Likes](F.likes)
            createdAt <- doc.getAsOpt[Instant](F.createdAt)
          } yield coll:
            _.update
              .one(
                $id(id),
                $set(F.rank -> Study.Rank.compute(likes, createdAt))
              )
          .void) inject Cursor.Cont(count + 1)
        }

  private[study] def isAdminMember(study: Study, userId: UserId): Fu[Boolean] =
    coll(_.exists($id(study.id) ++ $doc(s"members.$userId.admin" -> true)))

  private def countLikes(studyId: StudyId): Fu[Option[(Study.Likes, Instant)]] =
    coll:
      _.aggregateWith[Bdoc](): framework =>
        import framework.*
        List(
          Match($id(studyId)),
          Project(
            $doc(
              "_id"       -> false,
              F.likes     -> $doc("$size" -> s"$$${F.likers}"), // do not use denormalized field
              F.createdAt -> true
            )
          )
        )
      .headOption
    .map: docOption =>
      for
        doc       <- docOption
        likes     <- doc.getAsOpt[Study.Likes](F.likes)
        createdAt <- doc.getAsOpt[Instant](F.createdAt)
      yield likes -> createdAt
