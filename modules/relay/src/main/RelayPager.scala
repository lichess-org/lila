package lila.relay

import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.user.User

final class RelayPager(
    repo: RelayRepo,
    withStudy: RelayWithStudy
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  private lazy val maxPerPage = MaxPerPage(20)

  def finished(me: Option[User], page: Int) =
    paginator(
      repo.selectors finished true,
      me,
      page,
      fuccess(9999).some
    )

  private def paginator(
      selector: Bdoc,
      me: Option[User],
      page: Int,
      nbResults: Option[Fu[Int]]
  ): Fu[Paginator[Relay.WithStudyAndLiked]] = {
    val adapter = new Adapter[Relay](
      collection = repo.coll,
      selector = selector,
      projection = none,
      sort = $sort desc "startedAt"
    ) mapFutureList withStudy.andLiked(me)
    Paginator(
      adapter = nbResults.fold(adapter) { nb =>
        new CachedAdapter(adapter, nb)
      },
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }
}
