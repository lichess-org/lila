package lila.team

import reactivemongo.pekkostream.{ PekkoStreamCursor, cursorProducer }
import reactivemongo.api.*
import reactivemongo.api.bson.*

import java.time.Period

import lila.core.team.{ Access, LightTeam, TeamData }
import lila.db.dsl.{ *, given }

final class TeamRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: TeamId) = coll.byId[Team](id)

  def byOrderedIds(ids: Seq[TeamId]) = coll.byOrderedIds[Team, TeamId](ids)(_.id)

  private val lightProjection = bdoc("name" -> true, "flair" -> true)

  def light(id: TeamId): Fu[Option[LightTeam]] =
    coll.one[LightTeam](bid(id), lightProjection)

  def lightsByIds(ids: Iterable[TeamId]): Fu[List[LightTeam]] =
    coll
      .find(inIds(ids) ++ enabledSelect, lightProjection.some)
      .sort(sortPopular)
      .cursor[LightTeam](ReadPref.sec)
      .list(100)

  def enabled(id: TeamId) = coll.one[Team](bid(id) ++ enabledSelect)

  def byIdsSortPopular(ids: Iterable[TeamId]): Fu[List[Team]] =
    coll
      .find(inIds(ids) ++ enabledSelect)
      .sort(sortPopular)
      .cursor[Team](ReadPref.sec)
      .list(100)

  private[team] def countCreatedSince(userId: UserId, duration: Period): Fu[Int] =
    coll.countSel:
      bdoc(
        "createdAt".gt(nowInstant.minus(duration)),
        "createdBy" -> userId
      )

  def incMembers(teamId: TeamId, by: Int): Funit =
    coll.update.one(bid(teamId), inc("nbMembers" -> by)).void

  def enable(team: Team): Funit =
    coll.updateField(bid(team.id), "enabled", true).void

  def disable(team: Team): Funit =
    coll.updateField(bid(team.id), "enabled", false).void

  def addRequest(teamId: TeamId, request: TeamRequest): Funit =
    coll.update
      .one(
        bid(teamId) ++ bdoc("requests.user".neq(request.user)),
        push("requests" -> request.user)
      )
      .void

  def creatorOf(id: TeamId): Fu[Option[UserId]] =
    coll.secondary.primitiveOne[UserId](bid(id), "createdBy")

  private[team] def cursor: PekkoStreamCursor[TeamData] =
    coll.find(enabledSelect).cursor[TeamData]()

  private[team] def forumAccess(id: TeamId): Fu[Option[Access]] =
    coll.secondary.primitiveOne[Access](bid(id), "forum")

  def filterHideMembers(ids: Iterable[TeamId]): Fu[Set[TeamId]] =
    ids.nonEmpty.so:
      coll.secondary.distinctEasy[TeamId, Set]("_id", inIds(ids) ++ bdoc("hideMembers" -> true))

  def filterHideForum(ids: Iterable[TeamId]): Fu[Set[TeamId]] =
    ids.nonEmpty.so:
      coll.secondary.distinctEasy[TeamId, Set]("_id", inIds(ids) ++ bdoc("forum".neq(Access.Everyone)))

  def onUserDelete(userId: UserId): Funit = for
    _ <- coll.update.one(bdoc("createdBy" -> userId), set("createdBy" -> UserId.ghost), multi = true)
    _ <- coll.update.one(bdoc("leaders" -> userId), pull("leaders" -> userId), multi = true)
  yield ()

  def deleteNewlyCreatedBy(userId: UserId): Funit =
    coll.delete.one(bdoc("createdBy" -> userId, "createdAt" -> gte(nowInstant.minusDays(2)))).void

  def isClas(id: TeamId): Fu[Boolean] = coll.secondary.exists(bid(id) ++ clasSelect)
  def byClasId(id: TeamId): Fu[Option[Team]] = coll.secondary.one[Team](bid(id) ++ clasSelect)

  private[team] val enabledSelect = bdoc("enabled" -> true)
  private val clasSelect = bdoc("ofClas" -> true)
  val noClasSelect = "ofClas".neq(true)

  private[team] val sortPopular = sort.desc("nbMembers")
