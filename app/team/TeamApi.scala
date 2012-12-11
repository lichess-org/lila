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
    userRepo: UserRepo,
    paginator: PaginatorBuilder) {

  val creationPeriod = 1 week

  def create(setup: TeamSetup, me: User): IO[Team] = setup.trim |> { s ⇒
    Team(
      name = s.name,
      location = s.location,
      description = s.description,
      createdBy = me) |> { team ⇒
        teamRepo saveIO team inject team
      }
  }

  def hasCreatedRecently(me: User): IO[Boolean] =
    teamRepo.userHasCreatedSince(me.id, creationPeriod)

  def isMine(team: Team)(implicit ctx: Context): IO[Boolean] =
    ~ctx.me.map(me ⇒ belongsTo(team, me))

  def join(teamId: String)(implicit ctx: Context): IO[Option[Team]] = for {
    teamOption ← teamRepo byId teamId
    result ← ~(teamOption |@| ctx.me).tupled.map({
      case (team, user) ⇒ for {
        exists ← belongsTo(team, user)
        _ ← (for {
          _ ← memberRepo.add(team.id, user.id)
          _ ← teamRepo.incMembers(team.id, +1)
        } yield ()) doUnless exists
      } yield team.some
    })
  } yield result

  def quit(teamId: String)(implicit ctx: Context): IO[Option[Team]] = for {
    teamOption ← teamRepo byId teamId
    result ← ~(teamOption |@| ctx.me).tupled.map({
      case (team, user) ⇒ for {
        exists ← belongsTo(team, user)
        _ ← (for {
          _ ← memberRepo.remove(team.id, user.id)
          _ ← teamRepo.incMembers(team.id, -1)
        } yield ()) doIf exists
      } yield team.some
    })
  } yield result

  def belongsTo(team: Team, user: User): IO[Boolean] =
    memberRepo.exists(teamId = team.id, userId = user.id)
}
