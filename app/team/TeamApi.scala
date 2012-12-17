package lila
package team

import scalaz.effects._
import org.scala_tools.time.Imports._
import com.github.ornicar.paginator.Paginator

import user.{ User, UserRepo }
import http.Context

final class TeamApi(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    cached: Cached,
    userRepo: UserRepo,
    messenger: TeamMessenger,
    makeForum: (String, String) ⇒ IO[Unit],
    paginator: PaginatorBuilder) {

  val creationPeriod = 1 week

  def create(setup: TeamSetup, me: User): IO[Team] = setup.trim |> { s ⇒
    Team(
      name = s.name,
      location = s.location,
      description = s.description,
      open = s.isOpen,
      createdBy = me) |> { team ⇒
        for {
          _ ← teamRepo saveIO team
          _ ← memberRepo.add(team.id, me.id)
          _ ← io(cached invalidateTeamIds me.id)
          _ ← makeForum(team.id, team.name)
        } yield team
      }
  }

  def update(team: Team, edit: TeamEdit, me: User): IO[Unit] = edit.trim |> { e ⇒
    team.copy(
      location = e.location,
      description = e.description,
      open = e.isOpen
    ) |> teamRepo.saveIO
  }

  def mine(me: User): IO[List[Team]] = for {
    teamIds ← memberRepo teamIdsByUserId me.id
    teams ← teamRepo byOrderedIds teamIds
  } yield teams

  def hasCreatedRecently(me: User): IO[Boolean] =
    teamRepo.userHasCreatedSince(me.id, creationPeriod)

  def requestsWithUsers(team: Team): IO[List[RequestWithUser]] = for {
    requests ← requestRepo findByTeamId team.id
    users ← userRepo byOrderedIds requests.map(_.user)
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  def join(teamId: String)(implicit ctx: Context): IO[Option[Requesting]] = for {
    teamOption ← teamRepo byId teamId
    result ← ~(teamOption |@| ctx.me)({
      case (team, user) if team.open ⇒
        (doJoin(team, user) inject Joined(team).some): IO[Option[Requesting]]
      case (team, user) ⇒
        io(Motivate(team).some: Option[Requesting])
    })
  } yield result

  def requestable(teamId: String, user: User): IO[Option[Team]] = for {
    teamOption ← teamRepo byId teamId
    able ← ~teamOption.map(requestable(_, user))
  } yield teamOption filter (_ ⇒ able)

  def requestable(team: Team, user: User): IO[Boolean] =
    requestRepo.exists(team.id, user.id) map { exists ⇒
      !exists && !belongsTo(team.id, user.id)
    }

  def createRequest(team: Team, setup: RequestSetup, user: User): IO[Unit] = for {
    able ← requestable(team, user)
    request = Request(team = team.id, user = user.id, message = setup.message)
    rwu = RequestWithUser(request, user)
    _ ← (requestRepo.add(request) >> messenger.joinRequest(team, rwu)) doIf able
  } yield ()

  def processRequest(team: Team, request: Request, accept: Boolean): IO[Unit] = for {
    _ ← requestRepo remove request.id
    userOption ← userRepo byId request.user
    _ ← ~userOption.map(user ⇒ accept.fold(
      doJoin(team, user) >> messenger.acceptRequest(team, request),
      messenger.declineRequest(team, request)
    ))
  } yield ()

  def doJoin(team: Team, user: User): IO[Unit] = {
    memberRepo.add(team.id, user.id) >>
      teamRepo.incMembers(team.id, +1) >>
      io(cached invalidateTeamIds user.id)
  } doUnless belongsTo(team.id, user.id)

  def quit(teamId: String)(implicit ctx: Context): IO[Option[Team]] = for {
    teamOption ← teamRepo byId teamId
    result ← ~(teamOption |@| ctx.me)({
      case (team, user) ⇒ doQuit(team, user) inject team.some
    })
  } yield result

  def doQuit(team: Team, user: User): IO[Unit] = {
    memberRepo.remove(team.id, user.id) >>
      teamRepo.incMembers(team.id, -1) >>
      io(cached invalidateTeamIds user.id)
  } doIf belongsTo(team.id, user.id)

  // delete for ever, with members but not forums
  def delete(team: Team): IO[Unit] = 
    teamRepo.removeIO(team) >> memberRepo.removeByteamId(team.id) 

  def belongsTo(teamId: String, userId: String): Boolean = 
    cached teamIds userId contains teamId
}
