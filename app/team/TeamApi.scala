package lila
package team

import scalaz.effects._
import com.github.ornicar.paginator._

final class TeamApi(
    repo: TeamRepo,
    maxPerPage: Int) {

  def popular(page: Int): Paginator[Team] = Paginator(
    SalatAdapter(
      dao = repo,
      query = repo.queryAll,
      sort = repo.sortPopular),
    currentPage = page,
    maxPerPage = maxPerPage
  ) | popular(1)
}
