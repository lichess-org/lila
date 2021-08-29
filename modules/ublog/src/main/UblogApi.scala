package lila.ublog

import reactivemongo.api._
import scala.concurrent.ExecutionContext

import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class UblogApi(coll: Coll)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  def create(data: UblogForm.UblogPostData, user: User): Fu[UblogPost] = {
    val post = UblogPost.make(user, data.title, data.intro, data.markdown)
    coll.insert.one(post) inject post
  }

  def find(id: UblogPost.Id): Fu[Option[UblogPost]] = coll.byId[UblogPost](id.value)

  def liveByUser(user: User, page: Int): Fu[Paginator[UblogPost]] =
    paginatorByUser(user, true, page)

  def draftByUser(user: User, page: Int): Fu[Paginator[UblogPost]] =
    paginatorByUser(user, false, page)

  private def paginatorByUser(user: User, live: Boolean, page: Int): Fu[Paginator[UblogPost]] =
    Paginator(
      adapter = new Adapter[UblogPost](
        collection = coll,
        selector = $doc("user" -> user.id, "live" -> live),
        projection = none,
        sort = $doc("liveAt" -> -1, "createdAt" -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
}
