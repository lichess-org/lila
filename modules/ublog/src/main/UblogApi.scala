package lila.ublog

import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.{ PicfitApi, PicfitUrl }
import lila.user.{ User, UserRepo }
import lila.hub.actorApi.timeline.Propagate

final class UblogApi(
    colls: UblogColls,
    userRepo: UserRepo,
    picfitApi: PicfitApi,
    picfitUrl: PicfitUrl,
    timeline: lila.hub.actors.Timeline,
    irc: lila.irc.IrcApi
)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  def create(data: UblogForm.UblogPostData, user: User): Fu[UblogPost] = {
    val post = data.create(user)
    colls.post.insert.one(
      postBSONHandler.writeTry(post).get ++ $doc(
        "likers" -> List(user.id)
      )
    ) inject post
  }

  def update(data: UblogForm.UblogPostData, prev: UblogPost, user: User): Fu[UblogPost] =
    getUserBlog(user, insertMissing = true) flatMap { blog =>
      val post = data.update(user, prev)
      colls.post.update.one($id(prev.id), $set(postBSONHandler.writeTry(post).get)) >> {
        (post.live && prev.lived.isEmpty) ?? {
          sendImageToZulip(user, post) >>- {
            lila.common.Bus.publish(UblogPost.Create(post), "ublogPost")
            if (blog.visible)
              timeline ! {
                Propagate(
                  lila.hub.actorApi.timeline.UblogPost(user.id, post.id.value, post.slug, post.title)
                ) toFollowersOf user.id
              }
          }
        }
      } inject post
    }

  def getUserBlog(user: User, insertMissing: Boolean = false): Fu[UblogBlog] =
    getBlog(UblogBlog.Id.User(user.id)) getOrElse {
      val blog = UblogBlog make user
      (insertMissing ?? colls.blog.insert.one(blog).void) inject blog
    }

  def getBlog(id: UblogBlog.Id): Fu[Option[UblogBlog]] = colls.blog.byId[UblogBlog](id.full)

  def getPost(id: UblogPost.Id): Fu[Option[UblogPost]] = colls.post.byId[UblogPost](id.value)

  def findByUserBlog(id: UblogPost.Id, user: User): Fu[Option[UblogPost]] =
    findByIdAndBlog(id, UblogBlog.Id.User(user.id))

  def findByIdAndBlog(id: UblogPost.Id, blog: UblogBlog.Id): Fu[Option[UblogPost]] =
    colls.post.one[UblogPost]($id(id) ++ $doc("blog" -> blog))

  def otherPosts(blog: UblogBlog.Id, post: UblogPost): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blog, "live" -> true, "_id" $ne post.id), previewPostProjection.some)
      .sort($doc("live.at" -> -1))
      .cursor[UblogPost.PreviewPost]()
      .list(4)

  def countLiveByBlog(blog: UblogBlog.Id): Fu[Int] =
    colls.post.countSel($doc("blog" -> blog, "live" -> true))

  private def imageRel(post: UblogPost) = s"ublog:${post.id}"

  def uploadImage(user: User, post: UblogPost, picture: PicfitApi.Uploaded): Fu[UblogPost] = {
    for {
      image <- picfitApi
        .upload(imageRel(post), picture, userId = user.id)
      _ <- colls.post.update.one($id(post.id), $set("image" -> image.id))
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
    colls.post
      .find($inIds(ids) ++ $doc("live" -> true), lightPostProjection.some)
      .cursor[UblogPost.LightPost]()
      .list()

  def delete(post: UblogPost): Funit =
    colls.post.delete.one($id(post.id)) >>
      picfitApi.deleteByRel(imageRel(post))

  def setTier(blog: UblogBlog.Id, tier: Int): Funit =
    colls.blog.update
      .one($id(blog), $set("modTier" -> tier, "tier" -> tier))
      .void

  private[ublog] def setShadowban(userId: User.ID, v: Boolean) = {
    if (v) fuccess(UblogBlog.Tier.HIDDEN)
    else userRepo.byId(userId).map(_.fold(UblogBlog.Tier.HIDDEN)(UblogBlog.Tier.default))
  } flatMap {
    setTier(UblogBlog.Id.User(userId), _)
  }

  def canBlog(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }
}
