package lila.team

import actorApi._
import org.joda.time.DateTime
import org.joda.time.Period
import play.api.libs.json.{ JsSuccess, Json }
import reactivemongo.api.{ Cursor, ReadPreference }
import scala.util.chaining._
import scala.util.Try

import lila.chat.ChatApi
import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.team.{ CreateTeam, JoinTeam, KickFromTeam, LeaveTeam }
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

  def teamEnabled(id: Team.ID) = teamRepo enabled id

  def leaderTeam(id: Team.ID) = teamRepo.coll.byId[LeaderTeam](id, $doc("name" -> true))

  def lightsByLeader = teamRepo.lightsByLeader _

  def request(id: Request.ID) = requestRepo.coll.byId[Request](id)

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
        descPrivate = setup.descPrivate.filter(_.value.nonEmpty),
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
        } >>- {
          cached.forumAccess.invalidate(team.id)
          indexer ! InsertTeam(team)
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

  def joinedTeamsOfUserAsSeenBy(member: User, viewer: Option[User]): Fu[List[Team.ID]] =
    cached
      .teamIdsList(member.id)
      .map(_.take(lila.team.Team.maxJoinCeiling)) flatMap { allIds =>
      if (viewer.exists(_ is member)) fuccess(allIds)
      else
        allIds.nonEmpty ?? {
          teamRepo.filterHideMembers(allIds) flatMap { hiddenIds =>
            if (hiddenIds.isEmpty) fuccess(allIds)
            else
              viewer.map(_.id).fold(fuccess(Team.IdsStr.empty))(cached.teamIds) map { viewerTeamIds =>
                allIds.filter { id =>
                  !hiddenIds(id) || viewerTeamIds.contains(id)
                }
              }
          }
        }
    }

  def countCreatedRecently(me: User): Fu[Int] =
    teamRepo.countCreatedSince(me.id, Period weeks 1)

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    for {
      requests  <- requestRepo.findActiveByTeam(team.id, 50)
      withUsers <- requestsWithUsers(requests)
    } yield withUsers

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] =
    for {
      teamIds   <- teamRepo enabledTeamIdsByLeader user.id
      requests  <- requestRepo findActiveByTeams teamIds
      withUsers <- requestsWithUsers(requests)
    } yield withUsers

  private def requestsWithUsers(requests: List[Request]): Fu[List[RequestWithUser]] =
    userRepo optionsByIds requests.map(_.user) map { users =>
      requests zip users collect {
        case (request, Some(user)) if user.enabled => RequestWithUser(request, user)
      }
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
      belongs   <- belongsTo(team.id, user.id)
      requested <- requestRepo.exists(team.id, user.id)
    } yield !belongs && !requested

  def createRequest(team: Team, user: User, msg: String): Funit =
    requestable(team, user) flatMap {
      _ ?? {
        val request = Request.make(
          team = team.id,
          user = user.id,
          message = if (user.marks.troll) Request.defaultMessage else msg
        )
        requestRepo.coll.insert.one(request).void >>- (cached.nbRequests invalidate team.createdBy)
      }
    }

  def cancelRequestOrQuit(team: Team, user: User): Funit =
    requestRepo.cancel(team.id, user) flatMap {
      case false => quit(team, user.id)
      case true  => funit
    }

  def processRequest(team: Team, request: Request, decision: String): Funit = {
    if (decision == "decline") requestRepo.coll.updateField($id(request.id), "declined", true).void
    else if (decision == "accept") for {
      _          <- requestRepo.remove(request.id)
      userOption <- userRepo byId request.user
      _ <-
        userOption.??(user => doJoin(team, user) >> notifier.acceptRequest(team, request))
    } yield ()
    else funit
  } addEffect { _ =>
    cached.nbRequests invalidate team.createdBy
  }

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

  def teamsOf(username: String) =
    cached.teamIdsList(User normalize username) flatMap {
      teamRepo.coll.byIds[Team](_, ReadPreference.secondaryPreferred)
    }

  def quit(team: Team, userId: User.ID): Funit =
    memberRepo.remove(team.id, userId) flatMap { res =>
      (res.n == 1) ?? {
        teamRepo.incMembers(team.id, -1) >>
          (team.leaders contains userId) ?? teamRepo.setLeaders(team.id, team.leaders - userId)
      } >>- {
        Bus.publish(LeaveTeam(teamId = team.id, userId = userId), "teamLeave")
        cached.invalidateTeamIds(userId)
      }
    }

  def quitAllOnAccountClosure(userId: User.ID): Fu[List[Team.ID]] =
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
    quit(team, userId) >>
      (!team.leaders(me.id)).?? {
        modLog.teamKick(me.id, userId, team.name)
      } >>-
      Bus.publish(KickFromTeam(teamId = team.id, userId = userId), "teamLeave")

  def kickMembers(team: Team, json: String, me: User) =
    (parseTagifyInput(json) - team.createdBy).map(kick(team, _, me))

  private case class TagifyUser(value: String)
  implicit private val TagifyUserReads = Json.reads[TagifyUser]

  private def parseTagifyInput(json: String) = Try {
    json.trim.nonEmpty ?? {
      Json.parse(json).validate[List[TagifyUser]] match {
        case JsSuccess(users, _) =>
          users
            .map(_.value.toLowerCase.trim)
            .toSet
        case _ => Set.empty[User.ID]
      }
    }
  } getOrElse Set.empty

  def setLeaders(team: Team, json: String, by: User, byMod: Boolean): Funit = {
    val leaders: Set[User.ID] = parseTagifyInput(json) take 30
    for {
      allIds               <- memberRepo.filterUserIdsInTeam(team.id, leaders)
      idsNoKids            <- userRepo.filterNotKid(allIds.toSeq)
      previousValidLeaders <- memberRepo.filterUserIdsInTeam(team.id, team.leaders)
      ids =
        (if (idsNoKids(User.lichessId) && !byMod && !previousValidLeaders(User.lichessId))
           idsNoKids - User.lichessId
         else idsNoKids)
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

  def toggleEnabled(team: Team, by: User, explain: String): Funit =
    if (
      lila.security.Granter(_.ManageTeam)(by) || team.createdBy == by.id ||
      (team.leaders(by.id) && !team.leaders(team.createdBy))
    ) {
      logger.info(s"toggleEnabled ${team.id}: ${!team.enabled} by @${by.id}: $explain")
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
  def delete(team: Team, by: User, explain: String): Funit =
    teamRepo.coll.delete.one($id(team.id)) >>
      memberRepo.removeByTeam(team.id) >>- {
        logger.info(s"delete team ${team.id} by @${by.id}: $explain")
        (indexer ! RemoveTeam(team.id))
      }

  def syncBelongsTo(teamId: Team.ID, userId: User.ID): Boolean =
    cached.syncTeamIds(userId) contains teamId

  def belongsTo(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    cached.teamIds(userId).dmap(_ contains teamId)

  def leads(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    teamRepo.leads(teamId, userId)

  def filterExistingIds(ids: Set[String]): Fu[Set[Team.ID]] =
    teamRepo.coll.distinctEasy[Team.ID, Set]("_id", $inIds(ids), ReadPreference.secondaryPreferred)

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
