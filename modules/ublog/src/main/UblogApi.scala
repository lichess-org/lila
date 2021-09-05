package lila.ublog

import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.db.dsl._
import lila.memo.{ PicfitApi, PicfitUrl }
import lila.user.User
import lila.hub.actorApi.timeline.Propagate

final class UblogApi(
    coll: Coll,
    picfitApi: PicfitApi,
    picfitUrl: PicfitUrl,
    timeline: lila.hub.actors.Timeline,
    irc: lila.irc.IrcApi
)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  def create(data: UblogForm.UblogPostData, user: User): Fu[UblogPost] = {
    val post = data.create(user)
    coll.insert.one(post) inject post
  }

  def update(data: UblogForm.UblogPostData, prev: UblogPost, user: User): Fu[UblogPost] = {
    val post = data.update(prev)
    coll.update.one($id(prev.id), post) >> {
      (post.live && prev.liveAt.isEmpty) ?? {
        sendImageToZulip(user, post) >>- {
          Bus.publish(UblogPost.Create(post), "ublogPost")
          if (!user.marks.troll)
            timeline ! {
              Propagate(
                lila.hub.actorApi.timeline.UblogPost(user.id, post.id.value, post.slug, post.title)
              ) toFollowersOf user.id
            }
        }
      }
    } inject post
  }

  def find(id: UblogPost.Id): Fu[Option[UblogPost]] = coll.byId[UblogPost](id.value)

  def findByAuthor(id: UblogPost.Id, author: User): Fu[Option[UblogPost]] =
    coll.one[UblogPost]($id(id) ++ $doc("user" -> author.id))

  def otherPosts(author: User, post: UblogPost): Fu[List[UblogPost.PreviewPost]] =
    coll
      .find($doc("user" -> author.id, "live" -> true, "_id" $ne post.id), previewPostProjection.some)
      .sort($doc("liveAt" -> -1, "createdAt" -> -1))
      .cursor[UblogPost.PreviewPost]()
      .list(4)

  def countLiveByUser(user: User): Fu[Int] = coll.countSel($doc("user" -> user.id, "live" -> true))

  private def imageRel(post: UblogPost) = s"ublog:${post.id}"

  def uploadImage(user: User, post: UblogPost, picture: PicfitApi.Uploaded): Fu[UblogPost] = {
    for {
      image <- picfitApi
        .upload(imageRel(post), picture, userId = post.user)
      _ <- coll.update.one($id(post.id), $set("image" -> image.id))
      newPost = post.copy(image = image.id.some)
      _ <- sendImageToZulip(user, newPost)
    } yield newPost
  }.logFailure(logger branch "upload")

  private def sendImageToZulip(user: User, post: UblogPost): Funit = post.live ?? post.image ?? { imageId =>
    irc.ublogImage(
      user,
      id = post.id.value,
      slug = post.slug,
      title = post.title,
      imageUrl = UblogPost.thumbnail(picfitUrl, imageId, _.Large)
    )
  }

  def liveLightsByIds(ids: List[UblogPost.Id]): Fu[List[UblogPost.LightPost]] =
    coll
      .find($inIds(ids) ++ $doc("live" -> true), lightPostProjection.some)
      .cursor[UblogPost.LightPost]()
      .list()

  def delete(post: UblogPost): Funit =
    coll.delete.one($id(post.id)) >>
      picfitApi.deleteByRel(imageRel(post))

  def canBlog(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }
}
