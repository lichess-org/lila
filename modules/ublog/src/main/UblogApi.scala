package lila.ublog

import reactivemongo.api._
import scala.concurrent.ExecutionContext

import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.memo.PicfitApi
import lila.user.User

final class UblogApi(coll: Coll, picfitApi: PicfitApi)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  def create(data: UblogForm.UblogPostData, user: User): Fu[UblogPost] = {
    val post = data.create(user)
    coll.insert.one(post) inject post
  }

  def update(data: UblogForm.UblogPostData, prev: UblogPost): Fu[UblogPost] = {
    val post = data.update(prev)
    coll.update.one($id(prev.id), post) inject post
  }

  def find(id: UblogPost.Id): Fu[Option[UblogPost]] = coll.byId[UblogPost](id.value)

  def findByAuthor(id: UblogPost.Id, author: User): Fu[Option[UblogPost]] =
    coll.one[UblogPost]($id(id) ++ $doc("user" -> author.id))

  def liveByUser(user: User, page: Int): Fu[Paginator[UblogPost]] =
    paginatorByUser(user, true, page)

  def draftByUser(user: User, page: Int): Fu[Paginator[UblogPost]] =
    paginatorByUser(user, false, page)

  def uploadImage(post: UblogPost, picture: PicfitApi.Uploaded) =
    picfitApi.upload(s"ublog:${post.id}", picture, userId = post.user).flatMap { image =>
      coll.update.one($id(post.id), $set("image" -> image.id)).void
    }

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
