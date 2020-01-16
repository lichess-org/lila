package lila.team

import actorApi._
import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.team.{ CreateTeam, JoinTeam }
import lila.hub.actorApi.timeline.{ Propagate, TeamCreate, TeamJoin }
import lila.hub.LightTeam
import lila.mod.ModlogApi
import lila.memo.CacheApi._
import lila.user.{ User, UserRepo }
import org.joda.time.Period
import reactivemongo.api.{ Cursor, ReadPreference }

final class TeamApi(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    userRepo: UserRepo,
    cached: Cached,
    notifier: Notifier,
    timeline: lila.hub.actors.Timeline,
    indexer: lila.hub.actors.TeamSearch,
    modLog: ModlogApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  val creationPeriod = Period weeks 1

  def team(id: Team.ID) = teamRepo.coll.byId[Team](id)

  def light(id: Team.ID) = teamRepo.coll.byId[LightTeam](id, $doc("name" -> true))

  def request(id: Team.ID) = requestRepo.coll.byId[Request](id)

  def create(setup: TeamSetup, me: User): Fu[Team] = {
    val s = setup.trim
    val team = Team.make(
      name = s.name,
      location = s.location,
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

  def update(team: Team, edit: TeamEdit, me: User): Funit = edit.trim |> { e =>
    team.copy(
      location = e.location,
      description = e.description,
      open = e.isOpen
    ) |> { team =>
      teamRepo.coll.update.one($id(team.id), team).void >>
        !team.isCreator(me.id) ?? {
          modLog.teamEdit(me.id, team.createdBy, team.name)
        } >>-
        (indexer ! InsertTeam(team))
    }
  }

  def mine(me: User): Fu[List[Team]] =
    cached teamIds me.id flatMap { ids =>
      teamRepo.coll.byIds[Team](ids.toArray)
    }

  def hasTeams(me: User): Fu[Boolean] = cached.teamIds(me.id).map(_.value.nonEmpty)

  def hasCreatedRecently(me: User): Fu[Boolean] =
    teamRepo.userHasCreatedSince(me.id, creationPeriod)

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] =
    for {
      requests <- requestRepo findByTeam team.id
      users    <- userRepo usersFromSecondary requests.map(_.user)
    } yield requests zip users map {
      case (request, user) => RequestWithUser(request, user)
    }

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] =
    for {
      teamIds  <- teamRepo teamIdsByCreator user.id
      requests <- requestRepo findByTeams teamIds
      users    <- userRepo usersFromSecondary requests.map(_.user)
    } yield requests zip users map {
      case (request, user) => RequestWithUser(request, user)
    }

  def join(teamId: Team.ID, me: User): Fu[Option[Requesting]] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _ ?? { team =>
        if (team.open) doJoin(team, me) inject Joined(team).some
        else fuccess(Motivate(team).some)
      }
    }

  def joinApi(teamId: Team.ID, me: User, oAuthAppOwner: User.ID): Fu[Option[Requesting]] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _ ?? { team =>
        if (team.open || team.createdBy == oAuthAppOwner) doJoin(team, me) inject Joined(team).some
        else fuccess(Motivate(team).some)
      }
    }

  def requestable(teamId: Team.ID, user: User): Fu[Option[Team]] =
    for {
      teamOption <- teamRepo.coll.byId[Team](teamId)
      able       <- teamOption.??(requestable(_, user))
    } yield teamOption filter (_ => able)

  def requestable(team: Team, user: User): Fu[Boolean] =
    for {
      belongs   <- belongsTo(team.id, user.id)
      requested <- requestRepo.exists(team.id, user.id)
    } yield !belongs && !requested

  def createRequest(team: Team, setup: RequestSetup, user: User): Funit =
    requestable(team, user) flatMap {
      _ ?? {
        val request = Request.make(team = team.id, user = user.id, message = setup.message)
        requestRepo.coll.insert.one(request).void >>- (cached.nbRequests invalidate team.createdBy)
      }
    }

  def processRequest(team: Team, request: Request, accept: Boolean): Funit =
    for {
      _ <- requestRepo.coll.delete.one(request)
      _ = cached.nbRequests invalidate team.createdBy
      userOption <- userRepo byId request.user
      _ <- userOption
        .filter(_ => accept)
        .??(user => doJoin(team, user) >>- notifier.acceptRequest(team, request))
    } yield ()

  def deleteRequestsByUserId(userId: lila.user.User.ID) =
    requestRepo.getByUserId(userId) flatMap {
      _.map { request =>
        requestRepo.remove(request.id) >>
          teamRepo.creatorOf(request.team).map { _ ?? cached.nbRequests.invalidate }
      }.sequenceFu
    }

  def doJoin(team: Team, user: User): Funit = !belongsTo(team.id, user.id) flatMap {
    _ ?? {
      memberRepo.add(team.id, user.id) >>
        teamRepo.incMembers(team.id, +1) >>- {
        cached invalidateTeamIds user.id
        timeline ! Propagate(TeamJoin(user.id, team.id)).toFollowersOf(user.id)
        Bus.publish(JoinTeam(id = team.id, userId = user.id), "team")
      }
    } recover lila.db.recoverDuplicateKey(_ => ())
  }

  def quit(teamId: Team.ID, me: User): Fu[Option[Team]] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _ ?? { team =>
        doQuit(team, me.id) inject team.some
      }
    }

  private def doQuit(team: Team, userId: User.ID): Funit = belongsTo(team.id, userId) flatMap {
    _ ?? {
      memberRepo.remove(team.id, userId) >>
        teamRepo.incMembers(team.id, -1) >>-
        cached.invalidateTeamIds(userId)
    }
  }

  def quitAll(userId: User.ID): Funit = memberRepo.removeByUser(userId)

  def kick(team: Team, userId: User.ID, me: User): Funit =
    doQuit(team, userId) >>
      !team.isCreator(me.id) ?? {
        modLog.teamKick(me.id, userId, team.name)
      }

  def changeOwner(team: Team, userId: User.ID, me: User): Funit =
    memberRepo.exists(team.id, userId) flatMap { e =>
      e ?? {
        teamRepo.changeOwner(team.id, userId) >>
          modLog.teamMadeOwner(me.id, userId, team.name) >>-
          notifier.madeOwner(team, userId)
      }
    }

  def enable(team: Team): Funit =
    teamRepo.enable(team).void >>- (indexer ! InsertTeam(team))

  def disable(team: Team): Funit =
    teamRepo.disable(team).void >>- (indexer ! RemoveTeam(team.id))

  // delete for ever, with members but not forums
  def delete(team: Team): Funit =
    teamRepo.coll.delete.one($id(team.id)) >>
      memberRepo.removeByteam(team.id) >>-
      (indexer ! RemoveTeam(team.id))

  def syncBelongsTo(teamId: Team.ID, userId: User.ID): Boolean =
    cached.syncTeamIds(userId) contains teamId

  def belongsTo(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    cached.teamIds(userId) dmap (_ contains teamId)

  def owns(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    teamRepo ownerOf teamId dmap (_ has userId)

  def filterExistingIds(ids: Set[String]): Fu[Set[Team.ID]] =
    teamRepo.coll.distinctEasy[Team.ID, Set]("_id", $doc("_id" $in ids))

  def autocomplete(term: String, max: Int): Fu[List[Team]] =
    teamRepo.coll.ext
      .find(
        $doc(
          "name".$startsWith(java.util.regex.Pattern.quote(term), "i"),
          "enabled" -> true
        )
      )
      .sort($sort desc "nbMembers")
      .list[Team](max, ReadPreference.secondaryPreferred)

  def nbRequests(teamId: Team.ID) = cached.nbRequests get teamId

  private[team] def recomputeNbMembers: Funit =
    teamRepo.coll.ext
      .find($empty, $id(true))
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .foldWhileM({}) { (_, doc) =>
        (doc.string("_id") ?? recomputeNbMembers) inject Cursor.Cont({})
      }

  private[team] def recomputeNbMembers(teamId: Team.ID): Funit =
    memberRepo.countByTeam(teamId) flatMap { nb =>
      teamRepo.coll.updateField($id(teamId), "nbMembers", nb).void
    }
}
