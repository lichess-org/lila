package lila
package team

import scalaz.effects._
import com.github.ornicar.paginator._
import org.scala_tools.time.Imports._

import user.User

final class TeamApi(
    repo: TeamRepo,
    maxPerPage: Int) {

  val creationPeriod = 1 week

  def popular(page: Int): Paginator[Team] = Paginator(
    SalatAdapter(
      dao = repo,
      query = repo.enabledQuery,
      sort = repo.sortPopular),
    currentPage = page,
    maxPerPage = maxPerPage
  ) | popular(1)

  def create(setup: TeamSetup, me: User): IO[Team] = setup.trim |> { s ⇒
    Team(
      name = s.name,
      location = s.location,
      description = s.description,
      createdBy = me) |> { team ⇒
        repo saveIO team inject team
      }
  }

  def hasCreatedRecently(me: User): IO[Boolean] =
    repo.userHasCreatedSince(me.id, creationPeriod)
}
