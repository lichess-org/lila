package lila.ublog

import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.memo.PicfitApi
import lila.user.User
import lila.hub.actorApi.timeline.Propagate

final class UblogApi(coll: Coll, picfitApi: PicfitApi, timeline: lila.hub.actors.Timeline)(implicit
    ec: ExecutionContext
) {

  import UblogBsonHandlers._

  def create(data: UblogForm.UblogPostData, user: User): Fu[UblogPost] = {
    val post = data.create(user)
    coll.insert.one(post) inject post
  }

  def update(data: UblogForm.UblogPostData, prev: UblogPost, user: User): Fu[UblogPost] = {
    val post = data.update(prev)
    coll.update.one($id(prev.id), post) >>- {
      if (post.live && prev.liveAt.isEmpty) {
        Bus.publish(UblogPost.Create(post), "ublogPost")
        if (!user.marks.troll)
          timeline ! {
            Propagate(
              lila.hub.actorApi.timeline.UblogPost(user.id, post.id.value, post.slug, post.title)
            ) toFollowersOf user.id
          }
      }
    } inject post
  }

  def find(id: UblogPost.Id): Fu[Option[UblogPost]] = coll.byId[UblogPost](id.value)

  def findByAuthor(id: UblogPost.Id, author: User): Fu[Option[UblogPost]] =
    coll.one[UblogPost]($id(id) ++ $doc("user" -> author.id))

  def countLiveByUser(user: User): Fu[Int] = coll.countSel($doc("user" -> user.id, "live" -> true))

  def liveByUser(user: User, page: Int): Fu[Paginator[UblogPost]] =
    paginatorByUser(user, true, page)

  def draftByUser(user: User, page: Int): Fu[Paginator[UblogPost]] =
    paginatorByUser(user, false, page)

  def uploadImage(post: UblogPost, picture: PicfitApi.Uploaded): Fu[UblogPost] =
    picfitApi
      .upload(s"ublog:${post.id}", picture, userId = post.user)
      .flatMap { image =>
        coll.update.one($id(post.id), $set("image" -> image.id)) inject post.copy(image = image.id.some)
      }
      .logFailure(logger branch "upload")

  def lightsByIds(ids: List[UblogPost.Id]): Fu[List[UblogPost.LightPost]] =
    coll
      .find($inIds(ids), $doc("title" -> true).some)
      .cursor[UblogPost.LightPost]()
      .list()

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
