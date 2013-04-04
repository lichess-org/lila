package lila.team

import org.scala_tools.time.Imports._
import akka.actor.ActorRef

import lila.user.{ User, Context }
import lila.user.tube.userTube
import tube._

final class TeamApi(
    cached: Cached,
    notifier: Notifier,
    forum: ActorRef,
    paginator: PaginatorBuilder) { //, indexer: SearchIndexer) {

  val creationPeriod = 1.week

//   def create(setup: TeamSetup, me: User): Option[IO[Team]] = me.canTeam option {
//     val s = setup.trim
//     val team = Team(
//       name = s.name,
//       location = s.location,
//       description = s.description,
//       open = s.isOpen,
//       createdBy = me)
//     (teamRepo saveIO team) >>
//       memberRepo.add(team.id, me.id) >>
//       io(cached invalidateTeamIds me.id) >>
//       makeForum(team.id, team.name) >>
//       (indexer insertOne team) inject team
//   }

//   def update(team: Team, edit: TeamEdit, me: User): IO[Unit] = edit.trim |> { e ⇒
//     team.copy(
//       location = e.location,
//       description = e.description,
//       open = e.isOpen
//     ) |> { team ⇒ teamRepo.saveIO(team) >> indexer.insertOne(team) }
//   }

//   def mine(me: User): IO[List[Team]] = for {
//     teamIds ← memberRepo teamIdsByUserId me.id
//     teams ← teamRepo byOrderedIds teamIds
//   } yield teams

//   def hasCreatedRecently(me: User): IO[Boolean] =
//     teamRepo.userHasCreatedSince(me.id, creationPeriod)

//   def requestsWithUsers(team: Team): IO[List[RequestWithUser]] = for {
//     requests ← requestRepo findByTeamId team.id
//     users ← userRepo byOrderedIds requests.map(_.user)
//   } yield requests zip users map {
//     case (request, user) ⇒ RequestWithUser(request, user)
//   }

//   def requestsWithUsers(user: User): IO[List[RequestWithUser]] = for {
//     teamIds ← teamRepo teamIdsByCreator user.id
//     requests ← requestRepo findByTeamIds teamIds
//     users ← userRepo byOrderedIds requests.map(_.user)
//   } yield requests zip users map {
//     case (request, user) ⇒ RequestWithUser(request, user)
//   }

//   def join(teamId: String)(implicit ctx: Context): IO[Option[Requesting]] = for {
//     teamOption ← teamRepo byId teamId
//     result ← ~(teamOption |@| ctx.me.filter(_.canTeam))({
//       case (team, user) if team.open ⇒
//         (doJoin(team, user.id) inject Joined(team).some): IO[Option[Requesting]]
//       case (team, user) ⇒
//         io(Motivate(team).some: Option[Requesting])
//     })
//   } yield result

//   def requestable(teamId: String, user: User): IO[Option[Team]] = for {
//     teamOption ← teamRepo byId teamId
//     able ← ~teamOption.map(requestable(_, user))
//   } yield teamOption filter (_ ⇒ able)

//   def requestable(team: Team, user: User): IO[Boolean] =
//     requestRepo.exists(team.id, user.id) map { exists ⇒
//       !exists && !belongsTo(team.id, user.id)
//     }

//   def createRequest(team: Team, setup: RequestSetup, user: User): IO[Unit] = for {
//     able ← requestable(team, user)
//     request = Request(team = team.id, user = user.id, message = setup.message)
//     rwu = RequestWithUser(request, user)
//     _ ← {
//       requestRepo.add(request) >>
//         io(cached invalidateNbRequests team.createdBy)
//     } doIf able
//   } yield ()

//   def processRequest(team: Team, request: Request, accept: Boolean): IO[Unit] = for {
//     _ ← requestRepo remove request.id
//     _ ← io(cached invalidateNbRequests team.createdBy)
//     userOption ← userRepo byId request.user
//     _ ← ~userOption.map(user ⇒
//       (doJoin(team, user.id) >> messenger.acceptRequest(team, request)) doIf accept
//     )
//   } yield ()

//   def doJoin(team: Team, userId: String): IO[Unit] = {
//     memberRepo.add(team.id, userId) >>
//       teamRepo.incMembers(team.id, +1) >>
//       io(cached invalidateTeamIds userId)
//   } doUnless belongsTo(team.id, userId)

//   def quit(teamId: String)(implicit ctx: Context): IO[Option[Team]] = for {
//     teamOption ← teamRepo byId teamId
//     result ← ~(teamOption |@| ctx.me)({
//       case (team, user) ⇒ doQuit(team, user.id) inject team.some
//     })
//   } yield result

//   def doQuit(team: Team, userId: String): IO[Unit] = {
//     memberRepo.remove(team.id, userId) >>
//       teamRepo.incMembers(team.id, -1) >>
//       io(cached invalidateTeamIds userId)
//   } doIf belongsTo(team.id, userId)

//   def quitAll(userId: String): IO[Unit] = memberRepo.removeByUserId(userId)

//   def kick(team: Team, userId: String): IO[Unit] = doQuit(team, userId)

//   def enable(team: Team): IO[Unit] =
//     teamRepo.enable(team) >> indexer.insertOne(team)

//   def disable(team: Team): IO[Unit] =
//     teamRepo.disable(team) >> indexer.removeOne(team)

//   // delete for ever, with members but not forums
//   def delete(team: Team): IO[Unit] =
//     teamRepo.removeIO(team) >>
//       memberRepo.removeByteamId(team.id) >>
//       indexer.removeOne(team)

//   def belongsTo(teamId: String, userId: String): Boolean =
//     cached teamIds userId contains teamId
}
