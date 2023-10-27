package lila.team

import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*
import java.time.Period

import lila.db.dsl.{ *, given }
import lila.hub.LightTeam

final class TeamRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: TeamId) = coll.byId[Team](id)

  def byOrderedIds(ids: Seq[TeamId]) = coll.byOrderedIds[Team, TeamId](ids)(_.id)

  def lightsByIds(ids: Iterable[TeamId]): Fu[List[LightTeam]] =
    coll
      .find($inIds(ids) ++ enabledSelect, $doc("name" -> true).some)
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

  def name(id: TeamId): Fu[Option[String]] =
    coll.primitiveOne[String]($id(id), "name")

  def mini(id: TeamId): Fu[Option[Team.Mini]] =
    name(id) map2 { Team.Mini(id, _) }

  private[team] def countCreatedSince(userId: UserId, duration: Period): Fu[Int] =
    coll.countSel(
      $doc(
        "createdAt" $gt nowInstant.minus(duration),
        "createdBy" -> userId
      )
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
        $id(teamId) ++ $doc("requests.user" $ne request.user),
        $push("requests" -> request.user)
      )
      .void

  def cursor = coll.find(enabledSelect).cursor[Team](ReadPref.sec)

  def forumAccess(id: TeamId): Fu[Option[Team.Access]] =
    coll.secondaryPreferred.primitiveOne[Team.Access]($id(id), "forum")

  def filterHideMembers(ids: Iterable[TeamId]): Fu[Set[TeamId]] =
    ids.nonEmpty so coll.secondaryPreferred
      .distinctEasy[TeamId, Set]("_id", $inIds(ids) ++ $doc("hideMembers" -> true))

  def filterHideForum(ids: Iterable[TeamId]): Fu[Set[TeamId]] =
    ids.nonEmpty so coll.secondaryPreferred
      .distinctEasy[TeamId, Set]("_id", $inIds(ids) ++ $doc("forum" $ne Team.Access.EVERYONE))

  private[team] val enabledSelect = $doc("enabled" -> true)

  private[team] val sortPopular = $sort desc "nbMembers"
