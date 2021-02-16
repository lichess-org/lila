package lila.team

import actorApi._
import org.joda.time.Period
import play.api.libs.json.{ JsSuccess, Json }
import reactivemongo.api.{ Cursor, ReadPreference }
import scala.util.chaining._
import scala.util.Try

import lila.chat.ChatApi
import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.team.{ CreateTeam, JoinTeam, KickFromTeam }
import lila.hub.actorApi.timeline.{ Propagate, TeamCreate, TeamJoin }
import lila.hub.LeaderTeam
import lila.memo.CacheApi._
import lila.mod.ModlogApi
import lila.user.{ User, UserRepo }

final class TeamApi(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    userRepo: UserRepo,
    cached: Cached,
    notifier: Notifier,
    timeline: lila.hub.actors.Timeline,
    indexer: lila.hub.actors.TeamSearch,
    modLog: ModlogApi,
    chatApi: ChatApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def team(id: Team.ID) = teamRepo byId id

  def leaderTeam(id: Team.ID) = teamRepo.coll.byId[LeaderTeam](id, $doc("name" -> true))

  def lightsByLeader = teamRepo.lightsByLeader _

  def request(id: Team.ID) = requestRepo.coll.byId[Request](id)

  def create(setup: TeamSetup, me: User): Fu[Team] = {
    val s      = setup.trim
    val bestId = Team.nameToId(s.name)
    chatApi.exists(bestId) map {
      case true  => Team.randomId()
      case false => bestId
    } flatMap { id =>
      val team = Team.make(
        id = id,
        name = s.name,
        location = s.location,
        password = s.password,
        description = s.description,
        open = s.isOpen,
        createdBy = me
      )
      teamRepo.coll.insert.one(team) >>
        memberRepo.add(team.id, me.id) >>- {
          cached invalidateTeamIds me.id
          indexer ! InsertTeam(team)
          timeline ! Propagate(
            TeamCreate(me.id, team.id)
          ).toFollowersOf(me.id)
          Bus.publish(CreateTeam(id = team.id, name = team.name, userId = me.id), "team")
        } inject team
    }
  }

  def update(team: Team, edit: TeamEdit, me: User): Funit =
    edit.trim pipe { e =>
      team.copy(
        location = e.location,
        password = e.password,
        description = e.description,
        open = e.isOpen,
        chat = e.chat
      ) pipe { team =>
        teamRepo.coll.update.one($id(team.id), team).void >>
          !team.leaders(me.id) ?? {
            modLog.teamEdit(me.id, team.createdBy, team.name)
          } >>-
          (indexer ! InsertTeam(team))
      }
    }

  def mine(me: User): Fu[List[Team]] =
    cached teamIdsList me.id flatMap teamRepo.byIdsSortPopular

  def isSubscribed = memberRepo.isSubscribed _

  def subscribe = memberRepo.subscribe _

  def countTeamsOf(me: User) =
    cached teamIdsList me.id dmap (_.size)

  def hasJoinedTooManyTeams(me: User) =
    countTeamsOf(me).dmap(_ > Team.maxJoin(me))

  def hasTeams(me: User): Fu[Boolean] = cached.teamIds(me.id).map(_.value.nonEmpty)

  def countCreatedRecently(me: User): Fu[Int] =
    teamRepo.countCreatedSince(me.id, Period weeks 1)

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    for {
      requests <- requestRepo findByTeam team.id
      users    <- userRepo usersFromSecondary requests.map(_.user)
    } yield requests zip users map { case (request, user) =>
      RequestWithUser(request, user)
    }

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] =
    for {
      teamIds  <- teamRepo enabledTeamIdsByLeader user.id
      requests <- requestRepo findByTeams teamIds
      users    <- userRepo usersFromSecondary requests.map(_.user)
    } yield requests zip users map { case (request, user) =>
      RequestWithUser(request, user)
    }

  def join(team: Team, me: User, request: Option[String], password: Option[String]): Fu[Requesting] =
    if (team.open) {
      if (team.password.fold(true)(_ == ~password)) doJoin(team, me) inject Requesting.Joined
      else fuccess(Requesting.NeedPassword)
    } else motivateOrJoin(team, me, request)

  def joinApi(team: Team, me: User, oAuthAppOwner: Option[User.ID], msg: Option[String]): Fu[Requesting] =
    if (team.open || oAuthAppOwner.contains(team.createdBy)) doJoin(team, me) inject Requesting.Joined
    else motivateOrJoin(team, me, msg)

  private def motivateOrJoin(team: Team, me: User, msg: Option[String]) =
    msg.fold(fuccess[Requesting](Requesting.NeedRequest)) { txt =>
      createRequest(team, me, txt) inject Requesting.Joined
    }

  def requestable(teamId: Team.ID, user: User): Fu[Option[Team]] =
    for {
      teamOption <- teamRepo.coll.byId[Team](teamId)
      able       <- teamOption.??(requestable(_, user))
    } yield teamOption ifTrue able

  def requestable(team: Team, user: User): Fu[Boolean] =
    for {
      belongs   <- belongsTo(team.id, user.id)
      requested <- requestRepo.exists(team.id, user.id)
    } yield !belongs && !requested

  def createRequest(team: Team, user: User, msg: String): Funit =
    requestable(team, user) flatMap {
      _ ?? {
        val request = Request.make(team = team.id, user = user.id, message = msg)
        requestRepo.coll.insert.one(request).void >>- (cached.nbRequests invalidate team.createdBy)
      }
    }

  def cancelRequest(teamId: Team.ID, user: User): Fu[Option[Team]] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _ ?? { team =>
        requestRepo.cancel(team.id, user) map (_ option team)
      }
    }

  def processRequest(team: Team, request: Request, accept: Boolean): Funit =
    for {
      _ <- requestRepo.coll.delete.one(request)
      _ = cached.nbRequests invalidate team.createdBy
      userOption <- userRepo byId request.user
      _ <-
        userOption
          .filter(_ => accept)
          .??(user => doJoin(team, user) >> notifier.acceptRequest(team, request))
    } yield ()

  def deleteRequestsByUserId(userId: User.ID) =
    requestRepo.getByUserId(userId) flatMap {
      _.map { request =>
        requestRepo.remove(request.id) >>
          teamRepo.leadersOf(request.team).map {
            _ foreach cached.nbRequests.invalidate
          }
      }.sequenceFu
    }

  def doJoin(team: Team, user: User): Funit =
    !belongsTo(team.id, user.id) flatMap {
      _ ?? {
        memberRepo.add(team.id, user.id) >>
          teamRepo.incMembers(team.id, +1) >>- {
            cached invalidateTeamIds user.id
            timeline ! Propagate(TeamJoin(user.id, team.id)).toFollowersOf(user.id)
            Bus.publish(JoinTeam(id = team.id, userId = user.id), "team")
          }
      } recover lila.db.ignoreDuplicateKey
    }

  def quit(teamId: Team.ID, me: User): Fu[Option[Team]] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _ ?? { team =>
        doQuit(team, me.id) inject team.some
      }
    }

  def teamsOf(username: String) =
    cached.teamIdsList(User normalize username) flatMap {
      teamRepo.coll.byIds[Team](_, ReadPreference.secondaryPreferred)
    }

  private def doQuit(team: Team, userId: User.ID): Funit =
    memberRepo.remove(team.id, userId) map { res =>
      if (res.n == 1) teamRepo.incMembers(team.id, -1)
      cached.invalidateTeamIds(userId)
    }

  def quitAll(userId: User.ID): Fu[List[Team.ID]] =
    cached.teamIdsList(userId) flatMap { teamIds =>
      memberRepo.removeByUser(userId) >>
        teamIds.map { teamRepo.incMembers(_, -1) }.sequenceFu.void inject teamIds
    }

  def kick(team: Team, userId: User.ID, me: User): Funit =
    doQuit(team, userId) >>
      (!team.leaders(me.id)).?? {
        modLog.teamKick(me.id, userId, team.name)
      } >>-
      Bus.publish(KickFromTeam(teamId = team.id, userId = userId), "teamKick")

  private case class TagifyUser(value: String)
  implicit private val TagifyUserReads = Json.reads[TagifyUser]

  def setLeaders(team: Team, json: String, by: User, byMod: Boolean): Funit = {
    val leaders: Set[User.ID] = Try {
      json.trim.nonEmpty ?? {
        Json.parse(json).validate[List[TagifyUser]] match {
          case JsSuccess(users, _) =>
            users
              .map(_.value.toLowerCase.trim)
              .filter(User.lichessId !=)
              .toSet take 30
          case _ => Set.empty[User.ID]
        }
      }
    } getOrElse Set.empty
    memberRepo.filterUserIdsInTeam(team.id, leaders) flatMap { ids =>
      ids.nonEmpty ?? {
        if (ids(team.createdBy) || !team.leaders(team.createdBy) || by.id == team.createdBy || byMod) {
          cached.leaders.put(team.id, fuccess(ids))
          logger.info(s"valid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          teamRepo.setLeaders(team.id, ids).void
        } else {
          logger.info(s"invalid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          funit
        }
      }
    }
  }

  def isLeaderOf(leader: User.ID, member: User.ID) =
    cached.teamIdsList(member) flatMap { teamIds =>
      teamIds.nonEmpty ?? teamRepo.coll.exists($inIds(teamIds) ++ $doc("leaders" -> leader))
    }

  def enable(team: Team): Funit =
    teamRepo.enable(team).void >>- (indexer ! InsertTeam(team))

  def disable(team: Team, by: User): Funit =
    if (lila.security.Granter(_.ManageTeam)(by) || team.createdBy == by.id || !team.leaders(team.createdBy))
      teamRepo.disable(team).void >>
        memberRepo.userIdsByTeam(team.id).map {
          _ foreach cached.invalidateTeamIds
        } >>
        requestRepo.removeByTeam(team.id).void >>-
        (indexer ! RemoveTeam(team.id))
    else
      teamRepo.setLeaders(team.id, team.leaders - by.id)

  // delete for ever, with members but not forums
  def delete(team: Team): Funit =
    teamRepo.coll.delete.one($id(team.id)) >>
      memberRepo.removeByteam(team.id) >>-
      (indexer ! RemoveTeam(team.id))

  def syncBelongsTo(teamId: Team.ID, userId: User.ID): Boolean =
    cached.syncTeamIds(userId) contains teamId

  def belongsTo(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    cached.teamIds(userId).dmap(_ contains teamId)

  def leads(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    teamRepo.leads(teamId, userId)

  def filterExistingIds(ids: Set[String]): Fu[Set[Team.ID]] =
    teamRepo.coll.distinctEasy[Team.ID, Set]("_id", $doc("_id" $in ids), ReadPreference.secondaryPreferred)

  def autocomplete(term: String, max: Int): Fu[List[Team]] =
    teamRepo.coll
      .find(
        $doc(
          "name".$startsWith(java.util.regex.Pattern.quote(term), "i"),
          "enabled" -> true
        )
      )
      .sort($sort desc "nbMembers")
      .cursor[Team](ReadPreference.secondaryPreferred)
      .list(max)

  def nbRequests(teamId: Team.ID) = cached.nbRequests get teamId

  private[team] def recomputeNbMembers: Funit =
    teamRepo.coll
      .find($empty, $id(true).some)
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .foldWhileM {} { (_, doc) =>
        (doc.string("_id") ?? recomputeNbMembers) inject Cursor.Cont {}
      }

  private[team] def recomputeNbMembers(teamId: Team.ID): Funit =
    memberRepo.countByTeam(teamId) flatMap { nb =>
      teamRepo.coll.updateField($id(teamId), "nbMembers", nb).void
    }
}
