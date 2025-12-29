package lila.study

import akka.stream.scaladsl.*
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.*

import lila.core.study as hub
import lila.core.study.Visibility
import lila.db.AsyncColl
import lila.db.dsl.{ *, given }

final class StudyRepo(private[study] val coll: AsyncColl)(using
    Executor,
    akka.stream.Materializer
):

  import BSONHandlers.given

  private object F:
    val uids = "uids"
    val likers = "likers"
    val rank = "rank"
    val likes = "likes"
    val topics = "topics"
    val createdAt = "createdAt"

  private[study] val projection = $doc(
    F.uids -> false,
    F.likers -> false,
    F.rank -> false
  )

  private[study] val lightProjection = $doc(
    "_id" -> false,
    "visibility" -> true,
    "members" -> true
  )

  def byId(id: StudyId) = coll(_.find($id(id), projection.some).one[Study])
  def publicById(id: StudyId) = coll(_.find($id(id) ++ selectPublic, projection.some).one[Study])

  def byIdWithChapter(
      chapterColl: AsyncColl
  )(id: StudyId, chapterId: StudyChapterId): Fu[Option[Study.WithChapter]] =
    coll:
      _.aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          Project(projection),
          PipelineOperator(
            $lookup.simple(
              from = chapterColl.name,
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
          doc <- docs
          chapter <- doc.getAsOpt[Chapter]("chapter")
          study <- doc.asOpt[Study]
        yield Study.WithChapter(study, chapter)

  def byOrderedIds(ids: Seq[StudyId]) = coll(_.byOrderedIds[Study, StudyId](ids, projection.some)(_.id))

  def lightById(id: StudyId): Fu[Option[Study.LightStudy]] =
    coll(_.find($id(id), lightProjection.some).one[Study.LightStudy])

  def sortedCursor(
      selector: Bdoc,
      sort: Bdoc,
      readPref: ReadPref = _.pri
  ): Fu[AkkaStreamCursor[Study]] =
    coll.map(_.find(selector, projection.some).sort(sort).cursor[Study](readPref))

  def exists(id: StudyId) = coll(_.exists($id(id)))

  private[study] def selectOwnerId(ownerId: UserId) = $doc("ownerId" -> ownerId)
  def selectMemberId(memberId: UserId) = $doc(F.uids -> memberId)
  private[study] val selectPublic = $doc("visibility" -> Visibility.public)
  private[study] val selectPrivateOrUnlisted = "visibility".$ne(Visibility.public)
  private[study] def selectLiker(userId: UserId) = $doc(F.likers -> userId)
  private[study] def selectContributorId(userId: UserId): Bdoc =
    selectMemberId(userId) ++ // use the index
      $doc("ownerId".$ne(userId)) ++
      $doc(s"members.$userId.role" -> "w")
  private[study] def selectTopic(topic: StudyTopic) = $doc(F.topics -> topic)
  def selectBroadcast = selectTopic(StudyTopic.broadcast)
  private[study] def selectNotBroadcast = $doc(F.topics.$ne(StudyTopic.broadcast))

  private def hasMemberOrIsPublic(using as: Option[MyId]) = as.fold(selectPublic): me =>
    $or($doc(s"members.$me".$exists(true)), selectPublic)

  def countByOwner(ownerId: UserId)(using as: Option[MyId]) = coll:
    _.secondary.countSel(selectOwnerId(ownerId) ++ as.forall(_.isnt(ownerId)).so(hasMemberOrIsPublic))

  def sourceByOwner(ownerId: UserId, isMe: Boolean): Source[Study, ?] =
    Source.futureSource:
      coll.map:
        _.find(selectOwnerId(ownerId) ++ (!isMe).so(selectPublic), projection.some)
          .sort($sort.desc("updatedAt"))
          .cursor[Study]()
          .documentSource()

  def sourceByMember(memberId: UserId, isMe: Boolean, select: Bdoc = $empty): Source[Study, ?] =
    Source.futureSource:
      coll.map:
        _.find(selectMemberId(memberId) ++ select ++ (!isMe).so(selectPublic), projection.some)
          .sort($sort.desc("rank"))
          .cursor[Study]()
          .documentSource()

  def insert(s: Study): Funit =
    coll:
      _.insert.one:
        studyHandler.writeTry(s).get ++ $doc(
          F.uids -> s.members.ids,
          F.likers -> List(s.ownerId),
          F.rank -> Study.Rank.compute(s.likes, s.createdAt)
        )
    .void

  def updateSomeFields(s: Study): Funit =
    import toBSONValueOption.given
    coll:
      _.update
        .one(
          $id(s.id),
          $setsAndUnsets(
            "position" -> s.position.some,
            "name" -> s.name.some,
            "flair" -> s.flair,
            "settings" -> s.settings.some,
            "visibility" -> s.visibility.some,
            "description" -> s.description,
            "updatedAt" -> nowInstant.some
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
      _.update.one(
        $id(studyId),
        $set(
          "position" -> position,
          "updatedAt" -> nowInstant
        )
      )
    .void

  def updateNow(s: Study): Funit =
    updateNow(s.id)

  def updateNow(id: StudyId): Funit =
    coll.map(_.updateFieldUnchecked($id(id), "updatedAt", nowInstant))

  def addMember(study: Study, member: StudyMember): Funit =
    coll:
      _.update.one(
        $id(study.id),
        $set(s"members.${member.id}" -> member) ++ $addToSet(F.uids -> member.id)
      )
    .void

  def removeMember(study: Study, userId: UserId): Funit =
    coll:
      _.update.one(
        $id(study.id),
        $unset(s"members.$userId") ++ $pull(F.uids -> userId)
      )
    .void

  def setRole(study: Study, userId: UserId, role: StudyMember.Role): Funit =
    coll:
      _.update.one(
        $id(study.id),
        $set(s"members.$userId.role" -> role)
      )
    .void

  def membersDoc(id: StudyId): Fu[Option[Bdoc]] =
    coll(_.primitiveOne[Bdoc]($id(id), "members"))

  def setMembersDoc(ids: Seq[StudyId], members: Bdoc): Funit =
    coll(
      _.update.one(
        $inIds(ids),
        $set(
          "members" -> members,
          "uids" -> members.toMap.keys
        ),
        multi = true
      )
    ).void

  private val idNameProjection = $doc("name" -> true)

  def publicIdNames(ids: List[StudyId]): Fu[List[hub.IdName]] =
    coll(_.find($inIds(ids) ++ selectPublic, idNameProjection.some).cursor[hub.IdName]().listAll())

  def recentByOwnerWithChapterCount(
      chapterColl: AsyncColl
  )(userId: UserId, nb: Int): Fu[List[(hub.IdName, Int)]] =
    findRecentStudyWithChapterCount(selectOwnerId)(chapterColl)(userId, nb)

  def recentByContributorWithChapterCount(
      chapterColl: AsyncColl
  )(userId: UserId, nb: Int): Fu[List[(hub.IdName, Int)]] =
    findRecentStudyWithChapterCount(selectContributorId)(chapterColl)(userId, nb)

  private def findRecentStudyWithChapterCount(query: UserId => Bdoc)(
      chapterColl: AsyncColl
  )(userId: UserId, nb: Int): Future[List[(hub.IdName, Int)]] =
    coll:
      _.aggregateList(nb): framework =>
        import framework.*
        Match(query(userId) ++ selectNotBroadcast) -> List(
          Sort(Descending("updatedAt")),
          Project(idNameProjection),
          PipelineOperator(
            $lookup.simple(
              from = chapterColl.name,
              as = "chapters",
              local = "_id",
              foreign = "studyId",
              pipe = List($doc("$project" -> $id(true)))
            )
          ),
          AddFields($doc("chapters" -> $doc("$size" -> "$chapters")))
        )
      .map: docs =>
        for
          doc <- docs
          idName <- studyIdNameHandler.readOpt(doc)
          nbChapters <- doc.int("chapters")
        yield (idName, nbChapters)

  def isContributor(studyId: StudyId, userId: UserId) =
    coll(_.exists($id(studyId) ++ $doc(s"members.$userId.role" -> "w")))

  def isMember(studyId: StudyId, userId: UserId) =
    coll(_.exists($id(studyId) ++ (s"members.$userId".$exists(true))))

  def like(studyId: StudyId, userId: UserId, v: Boolean): Fu[Study.Likes] = for
    c <- coll.get
    _ <- c.update.one($id(studyId), if v then $addToSet(F.likers -> userId) else $pull(F.likers -> userId))
    likes <- countLikes(studyId)
    updated <- likes match
      case None => fuccess(Study.Likes(0))
      case Some(likes, createdAt) =>
        // Multiple updates may race to set denormalized likes and rank,
        // but values should be approximately correct, match a real like
        // count (though perhaps not the latest one), and any uncontended
        // query will set the precisely correct value.
        c.update
          .one(
            $id(studyId),
            $set(F.likes -> likes, F.rank -> Study.Rank.compute(likes, createdAt))
          )
          .inject(likes)
  yield updated

  def liked(study: Study, user: User): Fu[Boolean] =
    coll(_.exists($id(study.id) ++ selectLiker(user.id)))

  def filterLiked(user: User, studyIds: Seq[StudyId]): Fu[Set[StudyId]] =
    studyIds.nonEmpty.so(
      coll(_.primitive[StudyId]($inIds(studyIds) ++ selectLiker(user.id), "_id").dmap(_.toSet))
    )

  def resetAllRanks: Fu[Int] =
    coll:
      _.find(
        $empty,
        $doc(F.likes -> true, F.createdAt -> true).some
      )
        .cursor[Bdoc]()
        .foldWhileM(0): (count, doc) =>
          (for
            id <- doc.getAsOpt[StudyId]("_id")
            likes <- doc.getAsOpt[Study.Likes](F.likes)
            createdAt <- doc.getAsOpt[Instant](F.createdAt)
          yield coll:
            _.update
              .one(
                $id(id),
                $set(F.rank -> Study.Rank.compute(likes, createdAt))
              )
              .void
          ).orZero.inject(Cursor.Cont(count + 1))

  private[study] def isAdminMember(study: Study, userId: UserId): Fu[Boolean] =
    coll(_.exists($id(study.id) ++ $doc(s"members.$userId.admin" -> true)))

  private[study] def deletePrivateByOwner(u: UserId): Fu[List[StudyId]] = for
    c <- coll.get
    privateSelector = selectOwnerId(u) ++ selectPrivateOrUnlisted
    ids <- c.distinctEasy[StudyId, List]("_id", privateSelector)
    _ <- c.delete.one(privateSelector)
  yield ids

  private[study] def anonymizeAllOf(u: UserId): Funit = for
    c <- coll.get
    _ <- c.update.one(selectOwnerId(u), $set("ownerId" -> UserId.ghost), multi = true)
    _ <- c.update.one($doc(F.likers -> u), $pull(F.likers -> u), multi = true)
    _ <- c.update.one($doc(F.uids -> u), $pull(F.uids -> u) ++ $unset(s"members.$u"), multi = true)
  yield ()

  private def countLikes(studyId: StudyId): Fu[Option[(Study.Likes, Instant)]] =
    coll:
      _.aggregateWith[Bdoc](): framework =>
        import framework.*
        List(
          Match($id(studyId)),
          Project(
            $doc(
              "_id" -> false,
              F.likes -> $doc("$size" -> s"$$${F.likers}"), // do not use denormalized field
              F.createdAt -> true
            )
          )
        )
      .headOption
    .map: docOption =>
      for
        doc <- docOption
        likes <- doc.getAsOpt[Study.Likes](F.likes)
        createdAt <- doc.getAsOpt[Instant](F.createdAt)
      yield likes -> createdAt
