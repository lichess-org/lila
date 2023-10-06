package lila.team

import play.api.libs.json.{ JsSuccess, Json, Reads }
import play.api.mvc.RequestHeader
import scala.util.chaining.*
import scala.util.Try

import lila.chat.ChatApi
import lila.common.Bus
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.team.{ CreateTeam, JoinTeam, KickFromTeam, LeaveTeam }
import lila.hub.actorApi.timeline.{ Propagate, TeamCreate, TeamJoin }
import lila.hub.LeaderTeam
import lila.memo.CacheApi.*
import lila.mod.ModlogApi
import lila.user.{ User, UserApi, UserRepo, Me }
import lila.security.Granter
import java.time.Period

final class TeamApi(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    userRepo: UserRepo,
    userApi: UserApi,
    cached: Cached,
    notifier: Notifier,
    timeline: lila.hub.actors.Timeline,
    indexer: lila.hub.actors.TeamSearch,
    modLog: ModlogApi,
    chatApi: ChatApi
)(using Executor):

  import BSONHandlers.given

  def team(id: TeamId) = teamRepo byId id

  def teamEnabled(id: TeamId) = teamRepo enabled id

  def leaderTeam(id: TeamId) = teamRepo.coll.byId[LeaderTeam](id, $doc("name" -> true))

  def lightsByLeader = teamRepo.lightsByLeader

  def request(id: Request.ID) = requestRepo.coll.byId[Request](id)

  def create(setup: TeamSetup, me: User): Fu[Team] =
    val bestId = Team.nameToId(setup.name)
    for
      exists <- chatApi.exists(bestId into ChatId)
      id = if exists then Team.randomId() else bestId
      team = Team.make(
        id = id,
        name = setup.name,
        password = setup.password,
        intro = setup.intro,
        description = setup.description,
        descPrivate = setup.descPrivate.filter(_.value.nonEmpty),
        open = setup.isOpen,
        createdBy = me
      )
      _ <- teamRepo.coll.insert.one(team)
      _ <- memberRepo.add(team.id, me.id)
    yield
      cached invalidateTeamIds me.id
      indexer ! InsertTeam(team)
      timeline ! Propagate(TeamCreate(me.id, team.id)).toFollowersOf(me.id)
      Bus.publish(CreateTeam(id = team.id, name = team.name, userId = me.id), "team")
      team

  def update(team: Team, edit: TeamEdit)(using me: Me): Funit =
    team
      .copy(
        password = edit.password,
        intro = edit.intro,
        description = edit.description,
        descPrivate = edit.descPrivate,
        open = edit.isOpen,
        chat = edit.chat,
        forum = edit.forum,
        hideMembers = Some(edit.hideMembers)
      )
      .pipe: team =>
        teamRepo.coll.update.one($id(team.id), team).void >>
          (!team.leaders(me)).so {
            modLog.teamEdit(team.createdBy, team.name)
          } andDo {
            cached.forumAccess.invalidate(team.id)
            indexer ! InsertTeam(team)
          }

  def mine(using me: Me): Fu[List[Team]] =
    cached teamIdsList me flatMap teamRepo.byIdsSortPopular

  def isSubscribed = memberRepo.isSubscribed

  def subscribe = memberRepo.subscribe

  def countTeamsOf(me: Me) =
    cached teamIdsList me dmap (_.size)

  def hasJoinedTooManyTeams(using me: Me) =
    countTeamsOf(me).dmap(_ > Team.maxJoin(me))

  def hasTeams(me: User): Fu[Boolean] = cached.teamIds(me.id).map(_.value.nonEmpty)

  def joinedTeamIdsOfUserAsSeenBy[U](member: U)(using viewer: Option[Me])(using
      userIdOf: UserIdOf[U]
  ): Fu[List[TeamId]] =
    cached
      .teamIdsList(userIdOf(member))
      .map(_.take(lila.team.Team.maxJoinCeiling)) flatMap { allIds =>
      if viewer.exists(_ is member) || Granter.opt(_.UserModView) then fuccess(allIds)
      else
        allIds.nonEmpty.so:
          teamRepo.filterHideMembers(allIds) flatMap { hiddenIds =>
            if hiddenIds.isEmpty then fuccess(allIds)
            else
              viewer.map(_.userId).fold(fuccess(Team.IdsStr.empty))(cached.teamIds) map { viewerTeamIds =>
                allIds.filter: id =>
                  !hiddenIds(id) || viewerTeamIds.contains(id)
              }
          }
    }

  def countCreatedRecently(me: User): Fu[Int] =
    teamRepo.countCreatedSince(me.id, Period.ofWeeks(1))

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    requestRepo.findActiveByTeam(team.id, 50) flatMap requestsWithUsers

  def declinedRequestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    requestRepo.findDeclinedByTeam(team.id, 50) flatMap requestsWithUsers

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] = for
    teamIds   <- teamRepo enabledTeamIdsByLeader user.id
    requests  <- requestRepo findActiveByTeams teamIds
    withUsers <- requestsWithUsers(requests)
  yield withUsers

  private def requestsWithUsers(requests: List[Request]): Fu[List[RequestWithUser]] =
    userApi
      .listWithPerfs(requests.map(_.user))
      .map: users =>
        RequestWithUser.combine(requests, users.filter(_.enabled.yes))

  def join(team: Team, request: Option[String], password: Option[String])(using Me): Fu[Requesting] =
    if team.open then
      if team.passwordMatches(~password)
      then doJoin(team) inject Requesting.Joined
      else fuccess(Requesting.NeedPassword)
    else motivateOrJoin(team, request)

  private def motivateOrJoin(team: Team, msg: Option[String])(using Me) =
    msg.fold(fuccess[Requesting](Requesting.NeedRequest)): txt =>
      createRequest(team, txt) inject Requesting.Joined

  def requestable(teamId: TeamId)(using Me): Fu[Option[Team]] = for
    teamOption <- teamEnabled(teamId)
    able       <- teamOption.so(requestable)
  yield teamOption ifTrue able

  def requestable(team: Team)(using me: Me): Fu[Boolean] = for
    belongs   <- belongsTo(team.id, me)
    requested <- requestRepo.exists(team.id, me)
  yield !belongs && !requested

  def createRequest(team: Team, msg: String)(using me: Me): Funit =
    requestable(team).flatMapz {
      val request = Request.make(
        team = team.id,
        user = me,
        message = if me.marks.troll then Request.defaultMessage else msg
      )
      requestRepo.coll.insert.one(request).void andDo cached.nbRequests.invalidate(team.createdBy)
    }

  def cancelRequestOrQuit(team: Team)(using me: Me): Funit =
    requestRepo.cancel(team.id, me) flatMap {
      if _ then funit
      else quit(team, me)
    }

  def processRequest(team: Team, request: Request, decision: String): Funit = {
    if decision == "decline"
    then requestRepo.coll.updateField($id(request.id), "declined", true).void
    else if decision == "accept"
    then
      for
        _          <- requestRepo.remove(request.id)
        userOption <- userRepo byId request.user
        _ <- userOption.so(user => doJoin(team)(using Me(user)) >> notifier.acceptRequest(team, request))
      yield ()
    else funit
  }.addEffect: _ =>
    cached.nbRequests invalidate team.createdBy

  def deleteRequestsByUserId(userId: UserId) =
    requestRepo
      .getByUserId(userId)
      .flatMap:
        _.map: request =>
          requestRepo.remove(request.id) >>
            teamRepo
              .leadersOf(request.team)
              .map:
                _ foreach cached.nbRequests.invalidate
        .parallel

  def doJoin(team: Team)(using me: Me): Funit = {
    !belongsTo(team.id, me) flatMapz {
      memberRepo.add(team.id, me) >>
        teamRepo.incMembers(team.id, +1) andDo {
          cached invalidateTeamIds me
          timeline ! Propagate(TeamJoin(me, team.id)).toFollowersOf(me)
          Bus.publish(JoinTeam(id = team.id, userId = me), "team")
        }
    }
  } recover lila.db.ignoreDuplicateKey

  private[team] def addMembers(team: Team, userIds: Seq[UserId]): Funit =
    userIds
      .traverse: userId =>
        userRepo
          .enabledById(userId)
          .flatMapz: user =>
            memberRepo
              .add(team.id, Me(user))
              .map: _ =>
                cached.invalidateTeamIds(user.id)
                1
              .recover(lila.db.recoverDuplicateKey(_ => 0))
      .flatMap: inserts =>
        teamRepo.incMembers(team.id, inserts.sum)

  def teamsOf(username: UserStr) =
    cached.teamIdsList(username.id) flatMap teamsByIds

  def teamsByIds(ids: List[TeamId]) =
    teamRepo.coll.byIds[Team, TeamId](ids, _.sec)

  def quit(team: Team, userId: UserId): Funit =
    memberRepo.remove(team.id, userId) flatMap { res =>
      (res.n == 1) so {
        teamRepo.incMembers(team.id, -1) >>
          team.leaders
            .contains(userId)
            .so:
              teamRepo.setLeaders(team.id, team.leaders - userId)
      } andDo {
        Bus.publish(LeaveTeam(teamId = team.id, userId = userId), "teamLeave")
        cached.invalidateTeamIds(userId)
      }
    }

  def quitAllOnAccountClosure(userId: UserId): Fu[List[TeamId]] = for
    teamIds <- cached.teamIdsList(userId)
    _       <- memberRepo.removeByUser(userId)
    _       <- requestRepo.removeByUser(userId)
    _       <- teamIds.map { teamRepo.incMembers(_, -1) }.parallel
    _ = cached.invalidateTeamIds(userId)
  yield teamIds

  def searchMembersAs(teamId: TeamId, term: UserStr, as: Option[User], nb: Int): Fu[List[UserId]] =
    User.validateId(term) so { valid =>
      team(teamId).flatMapz: team =>
        val canSee =
          fuccess(team.publicMembers) >>| as.so(me => cached.teamIds(me.id).map(_.contains(teamId)))
        canSee.flatMapz:
          memberRepo.coll.primitive[UserId](
            selector = memberRepo.teamQuery(teamId) ++ $doc("user" $startsWith valid.value),
            sort = $sort desc "user",
            nb = nb,
            field = "user"
          )
    }

  def kick(team: Team, userId: UserId)(using me: Me): Funit =
    (userId != team.createdBy).so:
      memberRepo
        .exists(team.id, userId)
        .flatMapz:
          // create a request to set declined in order to prevent kicked use to rejoin
          val request = Request.make(
            team = team.id,
            user = userId,
            message = "Kicked from team",
            declined = true
          )
          for
            _ <- requestRepo.coll.insert.one(request)
            _ <- quit(team, userId)
            _ <- !team.leaders(me) so modLog.teamKick(userId, team.name)
          yield Bus.publish(KickFromTeam(teamId = team.id, userId = userId), "teamLeave")

  def kickMembers(team: Team, json: String)(using me: Me, req: RequestHeader): Funit =
    val users  = parseTagifyInput(json).toList
    val client = lila.common.HTTPRequest.printClient(req)
    logger.info:
      s"kick members ${users.size} by ${me.username} from lichess.org/team/${team.slug} $client | ${users.map(_.id).mkString(" ")}"
    users.traverse_(kick(team, _))

  private case class TagifyUser(value: String)
  private given Reads[TagifyUser] = Json.reads

  private def parseTagifyInput(json: String): Set[UserId] = Try {
    json.trim.nonEmpty so {
      Json.parse(json).validate[List[TagifyUser]] match
        case JsSuccess(users, _) =>
          users.toList
            .flatMap(u => UserStr.read(u.value))
            .map(_.id)
            .toSet
        case _ => Set.empty[UserId]
    }
  } getOrElse Set.empty

  def setLeaders(team: Team, json: String, by: User, byMod: Boolean): Funit =
    val leaders: Set[UserId] = parseTagifyInput(json) take 30
    for
      allIds               <- memberRepo.filterUserIdsInTeam(team.id, leaders)
      idsNoKids            <- userRepo.filterNotKid(allIds.toSeq)
      previousValidLeaders <- memberRepo.filterUserIdsInTeam(team.id, team.leaders)
      ids =
        if idsNoKids(User.lichessId) && !byMod && !previousValidLeaders(User.lichessId)
        then idsNoKids - User.lichessId
        else idsNoKids
      _ <- ids.nonEmpty.so:
        if ids(team.createdBy) || !previousValidLeaders(team.createdBy) || by.id == team.createdBy || byMod
        then
          cached.leaders.put(team.id, fuccess(ids))
          logger.info(s"valid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          teamRepo.setLeaders(team.id, ids).void
        else
          logger.info(s"invalid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          funit
    yield ()

  def isLeaderOf(leader: UserId, member: UserId) =
    cached.teamIdsList(member) flatMap { teamIds =>
      teamIds.nonEmpty so teamRepo.coll.exists($inIds(teamIds) ++ $doc("leaders" -> leader))
    }

  def toggleEnabled(team: Team, explain: String)(using me: Me): Funit =
    if Granter(_.ManageTeam) || me.is(team.createdBy) ||
      (team.leaders(me) && !team.leaders(team.createdBy))
    then
      logger.info(s"toggleEnabled ${team.id}: ${!team.enabled} by @${me}: $explain")
      if team.enabled then
        teamRepo.disable(team).void >>
          memberRepo.userIdsByTeam(team.id).map { _ foreach cached.invalidateTeamIds } >>
          requestRepo.removeByTeam(team.id).void andDo
          (indexer ! RemoveTeam(team.id))
      else teamRepo.enable(team).void andDo (indexer ! InsertTeam(team))
    else teamRepo.setLeaders(team.id, team.leaders - me)

  // delete for ever, with members but not forums
  def delete(team: Team, by: User, explain: String): Funit =
    teamRepo.coll.delete.one($id(team.id)) >>
      memberRepo.removeByTeam(team.id) andDo {
        logger.info(s"delete team ${team.id} by @${by.id}: $explain")
        (indexer ! RemoveTeam(team.id))
      }

  def syncBelongsTo(teamId: TeamId, userId: UserId): Boolean =
    cached.syncTeamIds(userId) contains teamId

  def belongsTo[U: UserIdOf](teamId: TeamId, u: U): Fu[Boolean] =
    cached.teamIds(u.id).dmap(_ contains teamId)

  def leads[U: UserIdOf](teamId: TeamId, u: U): Fu[Boolean] =
    teamRepo.leads(teamId, u.id)

  def filterExistingIds(ids: Set[TeamId]): Fu[Set[TeamId]] =
    teamRepo.coll.distinctEasy[TeamId, Set]("_id", $inIds(ids), _.sec)

  def autocomplete(term: String, max: Int): Fu[List[Team]] =
    teamRepo.coll
      .find(
        $doc(
          "name".$startsWith(java.util.regex.Pattern.quote(term), "i"),
          "enabled" -> true
        )
      )
      .sort($sort desc "nbMembers")
      .cursor[Team](ReadPref.sec)
      .list(max)

  export cached.nbRequests.{ get as nbRequests }
