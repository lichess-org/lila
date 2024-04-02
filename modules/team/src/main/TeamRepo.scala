package lila.team

import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*

import java.time.Period

import lila.db.dsl.{ *, given }
import lila.core.team.{ LightTeam, Access, TeamData }
import reactivemongo.akkastream.AkkaStreamCursor

final class TeamRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: TeamId) = coll.byId[Team](id)

  def byOrderedIds(ids: Seq[TeamId]) = coll.byOrderedIds[Team, TeamId](ids)(_.id)

  private val lightProjection = $doc("name" -> true, "flair" -> true)

  def light(id: TeamId): Fu[Option[LightTeam]] =
    coll.one[LightTeam]($id(id), lightProjection)

  def lightsByIds(ids: Iterable[TeamId]): Fu[List[LightTeam]] =
    coll
      .find($inIds(ids) ++ enabledSelect, lightProjection.some)
      .sort(sortPopular)
      .cursor[LightTeam](ReadPref.sec)
      .list(100)

  def enabled(id: TeamId) = coll.one[Team]($id(id) ++ enabledSelect)

  def byIdsSortPopular(ids: Iterable[TeamId]): Fu[List[Team]] =
    coll
      .find($inIds(ids) ++ enabledSelect)
      .sort(sortPopular)
      .cursor[Team](ReadPref.sec)
      .list(100)

  private[team] def countCreatedSince(userId: UserId, duration: Period): Fu[Int] =
    coll.countSel:
      $doc(
        "createdAt".$gt(nowInstant.minus(duration)),
        "createdBy" -> userId
      )

  def incMembers(teamId: TeamId, by: Int): Funit =
    coll.update.one($id(teamId), $inc("nbMembers" -> by)).void

  def enable(team: Team): Funit =
    coll.updateField($id(team.id), "enabled", true).void

  def disable(team: Team): Funit =
    coll.updateField($id(team.id), "enabled", false).void

  def addRequest(teamId: TeamId, request: TeamRequest): Funit =
    coll.update
      .one(
        $id(teamId) ++ $doc("requests.user".$ne(request.user)),
        $push("requests" -> request.user)
      )
      .void

  private[team] def cursor: AkkaStreamCursor[TeamData] =
    coll.find(enabledSelect).cursor[TeamData]()

  private[team] def forumAccess(id: TeamId): Fu[Option[Access]] =
    coll.secondaryPreferred.primitiveOne[Access]($id(id), "forum")

  def filterHideMembers(ids: Iterable[TeamId]): Fu[Set[TeamId]] =
    ids.nonEmpty.so(
      coll.secondaryPreferred
        .distinctEasy[TeamId, Set]("_id", $inIds(ids) ++ $doc("hideMembers" -> true))
    )

  def filterHideForum(ids: Iterable[TeamId]): Fu[Set[TeamId]] =
    ids.nonEmpty.so:
      coll.secondaryPreferred
        .distinctEasy[TeamId, Set]("_id", $inIds(ids) ++ $doc("forum".$ne(Access.Everyone)))

  private[team] val enabledSelect = $doc("enabled" -> true)

  private[team] val sortPopular = $sort.desc("nbMembers")
