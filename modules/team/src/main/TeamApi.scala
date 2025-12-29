package lila.team

import java.time.Period
import scala.util.Try
import scalalib.actor.AsyncActorSequencers
import play.api.libs.json.{ JsSuccess, Json, Reads }
import play.api.mvc.RequestHeader

import lila.common.Bus
import lila.core.perm.Granter
import lila.core.team.*
import lila.core.timeline as tl
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class TeamApi(
    teamRepo: TeamRepo,
    memberRepo: TeamMemberRepo,
    requestRepo: TeamRequestRepo,
    userApi: lila.core.user.UserApi,
    cached: Cached,
    notifier: Notifier,
    chatApi: lila.core.chat.ChatApi
)(using Executor, Scheduler)
    extends lila.core.team.TeamApi:

  import BSONHandlers.given

  export teamRepo.{ filterHideForum, onUserDelete, deleteNewlyCreatedBy }

  private val workQueue = AsyncActorSequencers[TeamId](
    maxSize = Max(8),
    expiration = 30.seconds,
    timeout = 5.seconds,
    name = "team",
    lila.log.asyncActorMonitor.highCardinality
  )

  def team(id: TeamId) = teamRepo.byId(id)

  def teamEnabled(id: TeamId) = teamRepo.enabled(id)

  def leaderTeam(id: TeamId) = teamRepo.coll.byId[LightTeam](id, $doc("name" -> true))

  def lightsByTourLeader[U: UserIdOf](leader: U): Fu[List[LightTeam]] =
    memberRepo.teamsLedBy(leader, Some(_.Tour)).flatMap(teamRepo.lightsByIds)

  def lightsOf[U: UserIdOf](member: U): Fu[List[LightTeam]] =
    cached.teamIdsList(member).flatMap(teamRepo.lightsByIds)

  def forumAccessOf(id: TeamId) = cached.forumAccess.get(id)

  def request(id: TeamRequest.ID) = requestRepo.coll.byId[TeamRequest](id)

  def create(setup: TeamSetup, me: User): Fu[Team] =
    val bestId = Team.nameToId(setup.name)
    for
      exists <- chatApi.exists(bestId.into(ChatId))
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
      _ <- memberRepo.add(team.id, me.id, TeamSecurity.Permission.values.toSet)
    yield
      cached.invalidateTeamIds(me.id)
      Bus.pub(TeamCreate(team.data))
      lila.common.Bus.pub(tl.Propagate(tl.TeamCreate(me.id, team.id)).toFollowersOf(me.id))
      team

  def update(old: Team, edit: TeamEdit)(using me: Me): Fu[String] = update:
    old.copy(
      password = edit.password,
      intro = edit.intro,
      description = edit.description,
      descPrivate = edit.descPrivate,
      open = edit.isOpen,
      chat = edit.chat,
      forum = edit.forum,
      hideMembers = Some(edit.hideMembers),
      flair = edit.flair
    )

  def update(team: Team)(using me: Me): Fu[String] =
    import reactivemongo.api.bson.*
    for
      blocklist <- blocklist.get(team)
      _ <- teamRepo.coll.update.one($id(team.id), bsonWriteDoc(team) ++ $doc("blocklist" -> blocklist))
      isLeader <- hasPerm(team.id, me, _.Settings)
    yield
      cached.forumAccess.invalidate(team.id)
      cached.lightCache.invalidate(team.id)
      Bus.pub(TeamUpdate(team.data, byMod = !isLeader))
      team.automodText

  def mine(using me: Me): Fu[List[Team.WithMyLeadership]] =
    cached.teamIdsList(me).flatMap(teamRepo.byIdsSortPopular).flatMap(memberRepo.addMyLeadership)

  def countTeamsOf(me: Me) =
    cached.teamIds(me).dmap(_.size)

  def hasJoinedTooManyTeams(using me: Me) =
    countTeamsOf(me).dmap(Team.maxJoin(me) < _)

  def hasTeams(me: User): Fu[Boolean] = cached.teamIds(me.id).map(_.value.nonEmpty)

  def joinedTeamIdsOfUserAsSeenBy(of: User)(using viewer: Option[Me]): Fu[List[TeamId]] =
    cached
      .teamIdsList(of.id)
      .map(_.take(Team.maxJoin(of).value))
      .flatMap: allIds =>
        if viewer.exists(_.is(of)) || Granter.opt(_.UserModView) then fuccess(allIds)
        else
          allIds.nonEmpty.so:
            teamRepo.filterHideMembers(allIds).flatMap { hiddenIds =>
              if hiddenIds.isEmpty then fuccess(allIds)
              else
                viewer.map(_.userId).fold(fuccess(Team.IdsStr.empty))(cached.teamIds).map { viewerTeamIds =>
                  allIds.filter: id =>
                    !hiddenIds(id) || viewerTeamIds.contains(id)
                }
            }

  def countCreatedRecently(me: User): Fu[Int] =
    teamRepo.countCreatedSince(me.id, Period.ofWeeks(1))

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    requestRepo.findActiveByTeam(team.id, 50).flatMap(requestsWithUsers)

  def declinedRequestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    requestRepo.findDeclinedByTeam(team.id, 50).flatMap(requestsWithUsers)

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] = for
    requestManagers <- memberRepo.leadersOf(user, _.Request)
    teamIds = requestManagers.map(_.team)
    requests <- requestRepo.findActiveByTeams(teamIds)
    withUsers <- requestsWithUsers(requests)
  yield withUsers

  private def requestsWithUsers(requests: List[TeamRequest]): Fu[List[RequestWithUser]] =
    userApi
      .listWithPerfs(requests.map(_.user))
      .map: users =>
        RequestWithUser.combine(requests, users.filter(_.enabled.yes))

  def join(team: Team, request: Option[String], password: Option[String])(using me: Me): Fu[Requesting] =
    workQueue(team.id):
      blocklist
        .has(team, me.userId)
        .flatMap:
          if _ then fuccess(Requesting.Blocklist)
          else if team.open then
            if team.passwordMatches(~password)
            then doJoin(team).inject(Requesting.Joined)
            else fuccess(Requesting.NeedPassword)
          else motivateOrJoin(team, request)

  private def motivateOrJoin(team: Team, msg: Option[String])(using Me) =
    msg.fold(fuccess[Requesting](Requesting.NeedRequest)): txt =>
      createRequest(team, txt).inject(Requesting.Joined)

  def requestable(teamId: TeamId)(using Me): Fu[Option[Team]] = for
    teamOption <- teamEnabled(teamId)
    able <- teamOption.so(requestable)
  yield teamOption.ifTrue(able)

  def requestable(team: Team)(using me: Me): Fu[Boolean] = for
    belongs <- belongsTo(team.id, me)
    requested <- requestRepo.exists(team.id, me)
  yield !belongs && !requested

  def createRequest(team: Team, msg: String)(using me: Me): Funit =
    requestable(team).flatMapz:
      val request = TeamRequest.make(
        team = team.id,
        user = me,
        message = if me.marks.troll then TeamRequest.defaultMessage else msg
      )
      for _ <- requestRepo.coll.insert.one(request) yield cached.nbRequests.invalidate(team.createdBy)

  def cancelRequestOrQuit(team: Team)(using me: Me): Funit =
    requestRepo
      .cancel(team.id, me)
      .flatMap:
        if _ then funit
        else quit(team, me)

  def processRequest(team: Team, request: TeamRequest, decision: String): Funit = workQueue(team.id) {
    if decision == "decline"
    then requestRepo.coll.updateField($id(request.id), "declined", true).void
    else if decision == "accept"
    then
      for
        _ <- requestRepo.remove(request.id)
        userOption <- userApi.byId(request.user)
        _ <- userOption.so(user => doJoin(team)(using Me(user)) >> notifier.acceptRequest(team, request))
      yield ()
    else funit
  }.addEffect: _ =>
    cached.nbRequests.invalidate(team.createdBy)

  def deleteRequestsByUserId(userId: UserId): Funit =
    requestRepo
      .getByUserId(userId)
      .flatMap:
        _.sequentiallyVoid: request =>
          requestRepo.remove(request.id) >>
            memberRepo
              .leaders(request.team, Some(_.Request))
              .map:
                _.map(_.user).foreach(cached.nbRequests.invalidate)

  def doJoin(team: Team)(using me: Me): Funit = {
    belongsTo(team.id, me).not.flatMapz:
      for
        _ <- memberRepo.add(team.id, me)
        _ <- teamRepo.incMembers(team.id, +1)
      yield
        cached.invalidateTeamIds(me)
        lila.common.Bus.pub(tl.Propagate(tl.TeamJoin(me, team.id)).toFollowersOf(me))
        Bus.pub(JoinTeam(id = team.id, userId = me))
  }.recover(lila.db.ignoreDuplicateKey)

  private[team] def addMembers(team: Team, userIds: List[UserId]): Funit =
    userIds
      .sequentially: userId =>
        userApi
          .enabledById(userId)
          .flatMapz: user =>
            memberRepo
              .add(team.id, user.id)
              .map: _ =>
                cached.invalidateTeamIds(user.id)
                1
              .recover(lila.db.recoverDuplicateKey(_ => 0))
      .flatMap: inserts =>
        teamRepo.incMembers(team.id, inserts.sum)

  def teamsOf(username: UserStr) =
    cached.teamIdsList(username.id).flatMap(teamsByIds)

  def teamsByIds(ids: List[TeamId]) =
    teamRepo.coll.byIds[Team, TeamId](ids, _.sec)

  def quit(team: Team, userId: UserId): Funit = workQueue(team.id):
    doQuit(team, userId)

  private def doQuit(team: Team, userId: UserId): Funit =
    for
      res <- memberRepo.remove(team.id, userId)
      _ <- (res.n == 1).so:
        teamRepo.incMembers(team.id, -1)
    yield
      Bus.pub(LeaveTeam(teamId = team.id, userId = userId))
      cached.invalidateTeamIds(userId)

  def quitAllOnAccountClosure(userId: UserId): Fu[List[TeamId]] = for
    teamIds <- cached.teamIdsList(userId)
    _ <- memberRepo.removeByUser(userId)
    _ <- requestRepo.removeByUser(userId)
    _ <- teamIds.sequentially(teamRepo.incMembers(_, -1))
    _ = cached.invalidateTeamIds(userId)
  yield teamIds

  def searchMembersAs(teamId: TeamId, term: UserSearch, nb: Int)(using me: Option[MyId]): Fu[List[UserId]] =
    team(teamId).flatMapz: team =>
      val canSee = fuccess(team.publicMembers) >>| me.so(me => cached.teamIds(me).map(_.contains(teamId)))
      canSee.flatMapz:
        memberRepo.coll.primitive[UserId](
          selector = memberRepo.teamQuery(teamId) ++ $doc("user".$startsWith(term.value)),
          sort = $sort.desc("user"),
          nb = nb,
          field = "user"
        )

  def kick(team: Team, userId: UserId)(using me: Me): Funit = workQueue(team.id):
    for
      kicked <- memberRepo.get(team.id, userId)
      myself <- memberRepo.get(team.id, me)
      allowed = userId.isnt(team.createdBy) && kicked.exists: kicked =>
        myself.exists: myself =>
          kicked.perms.isEmpty || myself.hasPerm(_.Admin) || Granter(_.ManageTeam)
      _ <- allowed.so:
        // create a request to set declined in order to prevent kicked use to rejoin
        val request = TeamRequest.make(team.id, userId, "Kicked from team", declined = true)
        for
          _ <- requestRepo.coll.update.one($id(request.id), request, upsert = true)
          _ <- doQuit(team, userId)
        yield Bus.pub(KickFromTeam(teamId = team.id, teamName = team.name, userId = userId))
    yield ()

  def kickMembers(team: Team, json: String)(using me: Me, req: RequestHeader): Funit =
    val users = parseTagifyInput(json).toList
    val client = lila.common.HTTPRequest.printClient(req)
    logger.info:
      s"kick members ${users.size} by ${me.username} from lichess.org/team/${team.slug} $client | ${users.map(_.id).mkString(" ")}"
    users.sequentiallyVoid(kick(team, _))

  object blocklist:
    def set(team: Team, list: String): Funit =
      teamRepo.coll.updateOrUnsetField($id(team.id), "blocklist", list.nonEmpty.option(list)).void
    def get(team: Team): Fu[String] =
      teamRepo.coll
        .primitiveOne[String]($id(team.id), "blocklist")
        .dmap(~_)
    def has(team: Team, user: UserId): Fu[Boolean] =
      get(team).map: list =>
        UserStr.from(list.split("\n")).exists(_.is(user))

  private case class TagifyUser(value: String)
  private given Reads[TagifyUser] = Json.reads

  private def parseTagifyInput(json: String): Set[UserId] = Try {
    json.trim.nonEmpty.so:
      Json.parse(json).validate[List[TagifyUser]] match
        case JsSuccess(users, _) =>
          users.toList
            .flatMap(u => UserStr.read(u.value))
            .map(_.id)
            .toSet
        case _ => Set.empty[UserId]
  }.getOrElse(Set.empty)

  def toggleEnabled(team: Team, explain: String)(using me: Me): Funit =
    isCreatorGranted(team, _.Admin).flatMap: activeCreator =>
      if Granter(_.ManageTeam) || me.is(team.createdBy) || !activeCreator
      then
        logger.info(s"toggleEnabled ${team.id}: ${!team.enabled} by @${me}: $explain")
        if team.enabled then
          for
            _ <- teamRepo.disable(team)
            users <- memberRepo.userIdsByTeam(team.id)
            _ = users.foreach(cached.invalidateTeamIds)
            _ <- requestRepo.removeByTeam(team.id)
          yield ()
        else
          for _ <- teamRepo.enable(team)
          yield Bus.pub(TeamUpdate(team.data, byMod = Granter(_.ManageTeam)))
      else memberRepo.setPerms(team.id, me, Set.empty)

  def idAndLeaderIds(teamId: TeamId): Fu[Option[Team.IdAndLeaderIds]] =
    memberRepo
      .leaderIds(teamId)
      .map: ids =>
        ids.nonEmpty.option(Team.IdAndLeaderIds(teamId, ids))

  def teamsLedBy[U: UserIdOf](leader: U): Fu[List[Team]] = for
    ids <- memberRepo.teamsLedBy(leader, None)
    teams <- teamRepo.byIdsSortPopular(ids)
  yield teams

  export teamRepo.cursor
  export memberRepo.{ publicLeaderIds, leaderIds, isSubscribed, subscribe, filterUserIdsInTeam }

  // delete for ever, with members but not forums
  def delete(team: Team, by: User, explain: String): Funit = for
    _ <- teamRepo.coll.delete.one($id(team.id))
    _ <- memberRepo.removeByTeam(team.id)
  yield logger.info(s"delete team ${team.id} by @${by.id}: $explain")

  def syncBelongsTo(teamId: TeamId, userId: UserId): Boolean =
    cached.syncTeamIds(userId).contains(teamId)

  def belongsTo[U: UserIdOf](teamId: TeamId, u: U): Fu[Boolean] =
    cached.teamIds(u.id).dmap(_.contains(teamId))

  def memberOf[U: UserIdOf](teamId: TeamId, u: U): Fu[Option[TeamMember]] =
    belongsTo(teamId, u).flatMapz:
      memberRepo.get(teamId, u)

  def isCreatorGranted(team: Team, perm: TeamSecurity.Permission.Selector): Fu[Boolean] =
    memberRepo.hasPerm(team.id, team.createdBy, perm)

  def isLeader[U: UserIdOf](team: TeamId, leader: U) =
    belongsTo(team, leader).flatMapz:
      memberRepo.hasAnyPerm(team, leader)

  def isGranted(team: TeamId, user: User, perm: TeamSecurity.Permission.Selector) =
    fuccess(Granter.ofUser(_.ManageTeam)(user)) >>|
      hasPerm(team, user.id, perm)

  def hasPerm(team: TeamId, userId: UserId, perm: TeamSecurity.Permission.Selector): Fu[Boolean] =
    belongsTo(team, userId).flatMapz:
      memberRepo.hasPerm(team, userId, perm)

  def hasCommPerm(team: TeamId, userId: UserId): Fu[Boolean] = hasPerm(team, userId, _.Comm)

  def isLeaderOf[U: UserIdOf](leader: UserId, member: U) =
    cached
      .teamIdsList(member)
      .flatMap:
        memberRepo.leadsOneOf(leader, _)

  def withLeaders(team: Team): Fu[Team.WithLeaders] =
    memberRepo.leaders(team.id).map(Team.WithLeaders(team, _))

  def filterExistingIds(ids: Set[TeamId]): Fu[Set[TeamId]] =
    teamRepo.coll.distinctEasy[TeamId, Set]("_id", $inIds(ids), _.sec)

  def autocomplete(term: String, max: Int): Fu[List[Team]] =
    teamRepo.coll
      .find:
        $doc(
          "name".$startsWith(java.util.regex.Pattern.quote(term), "i"),
          "enabled" -> true
        )
      .sort($sort.desc("nbMembers"))
      .cursor[Team](ReadPref.sec)
      .list(max)

  export cached.nbRequests.get as nbRequests
