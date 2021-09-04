package lila.team

import actorApi._
import com.softwaremill.tagging._
import org.joda.time.DateTime
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
    requestRepo: RequestRepo @@ NewRequest,
    declinedRequestRepo: RequestRepo @@ DeclinedRequest,
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

  def teamEnabled(id: Team.ID) = teamRepo enabled id

  def leaderTeam(id: Team.ID) = teamRepo.coll.byId[LeaderTeam](id, $doc("name" -> true))

  def lightsByLeader = teamRepo.lightsByLeader _

  def request(id: Team.ID) = requestRepo.coll.byId[Request](id)

  def create(setup: TeamSetup, me: User): Fu[Team] = {
    val bestId = Team.nameToId(setup.name)
    chatApi.exists(bestId) map {
      case true  => Team.randomId()
      case false => bestId
    } flatMap { id =>
      val team = Team.make(
        id = id,
        name = setup.name,
        password = setup.password,
        description = setup.description,
        descPrivate = setup.descPrivate.filter(_.nonEmpty),
        open = setup.isOpen,
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
    team.copy(
      password = edit.password,
      description = edit.description,
      descPrivate = edit.descPrivate,
      open = edit.isOpen,
      chat = edit.chat,
      forum = edit.forum,
      hideMembers = Some(edit.hideMembers)
    ) pipe { team =>
      teamRepo.coll.update.one($id(team.id), team).void >>
        !team.leaders(me.id) ?? {
          modLog.teamEdit(me.id, team.createdBy, team.name)
        } >>-
        (indexer ! InsertTeam(team))
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
      if (team.passwordMatches(~password)) doJoin(team, me) inject Requesting.Joined
      else fuccess(Requesting.NeedPassword)
    } else motivateOrJoin(team, me, request)

  private def motivateOrJoin(team: Team, me: User, msg: Option[String]) =
    msg.fold(fuccess[Requesting](Requesting.NeedRequest)) { txt =>
      createRequest(team, me, txt) inject Requesting.Joined
    }

  def requestable(teamId: Team.ID, user: User): Fu[Option[Team]] =
    for {
      teamOption <- teamEnabled(teamId)
      able       <- teamOption.??(requestable(_, user))
    } yield teamOption ifTrue able

  def requestable(team: Team, user: User): Fu[Boolean] =
    for {
      belongs         <- belongsTo(team.id, user.id)
      requested       <- requestRepo.exists(team.id, user.id)
      requestDeclined <- declinedRequestRepo.exists(team._id, user.id)
    } yield !belongs && !requested && !requestDeclined

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
      _ <- !accept ?? declinedRequestRepo.coll.insert.one(request.copy(date = DateTime.now())).void
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
        requestRepo.removeByUser(userId) >>
        teamIds.map { teamRepo.incMembers(_, -1) }.sequenceFu.void >>-
        cached.invalidateTeamIds(userId) inject teamIds
    }

  def searchMembers(teamId: Team.ID, term: String, nb: Int): Fu[List[User.ID]] =
    User.validateId(term) ?? { valid =>
      memberRepo.coll.primitive[User.ID](
        selector = memberRepo.teamQuery(teamId) ++ $doc("user" $startsWith valid),
        sort = $sort desc "user",
        nb = nb,
        field = "user"
      )
    }

  def kick(team: Team, userId: User.ID, me: User): Funit =
    doQuit(team, userId) >>
      (!team.leaders(me.id)).?? {
        modLog.teamKick(me.id, userId, team.name)
      } >>-
      Bus.publish(KickFromTeam(teamId = team.id, userId = userId), "teamKick")

  def kickMembers(team: Team, json: String, me: User) =
    parseTagifyInput(json) map (kick(team, _, me))

  private case class TagifyUser(value: String)
  implicit private val TagifyUserReads = Json.reads[TagifyUser]

  private def parseTagifyInput(json: String) = Try {
    json.trim.nonEmpty ?? {
      Json.parse(json).validate[List[TagifyUser]] match {
        case JsSuccess(users, _) =>
          users
            .map(_.value.toLowerCase.trim)
            .filter(User.lichessId !=)
            .toSet
        case _ => Set.empty[User.ID]
      }
    }
  } getOrElse Set.empty

  def setLeaders(team: Team, json: String, by: User, byMod: Boolean): Funit = {
    val leaders: Set[User.ID] = parseTagifyInput(json) take 30
    for {
      allIds               <- memberRepo.filterUserIdsInTeam(team.id, leaders)
      ids                  <- userRepo.filterNotKid(allIds.toSeq)
      previousValidLeaders <- memberRepo.filterUserIdsInTeam(team.id, team.leaders)
      _ <- ids.nonEmpty ?? {
        if (
          ids(team.createdBy) || !previousValidLeaders(team.createdBy) || by.id == team.createdBy || byMod
        ) {
          cached.leaders.put(team.id, fuccess(ids))
          logger.info(s"valid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          teamRepo.setLeaders(team.id, ids).void
        } else {
          logger.info(s"invalid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          funit
        }
      }
    } yield ()
  }

  def isLeaderOf(leader: User.ID, member: User.ID) =
    cached.teamIdsList(member) flatMap { teamIds =>
      teamIds.nonEmpty ?? teamRepo.coll.exists($inIds(teamIds) ++ $doc("leaders" -> leader))
    }

  def toggleEnabled(team: Team, by: User): Funit =
    if (
      lila.security.Granter(_.ManageTeam)(by) || team.createdBy == by.id ||
      (team.leaders(by.id) && !team.leaders(team.createdBy))
    ) {
      if (team.enabled)
        teamRepo.disable(team).void >>
          memberRepo.userIdsByTeam(team.id).map { _ foreach cached.invalidateTeamIds } >>
          requestRepo.removeByTeam(team.id).void >>-
          (indexer ! RemoveTeam(team.id))
      else
        teamRepo.enable(team).void >>- (indexer ! InsertTeam(team))
    } else
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
}
