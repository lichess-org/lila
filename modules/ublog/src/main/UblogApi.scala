package lila.ublog

import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.hub.actorApi.timeline.Propagate
import lila.memo.{ PicfitApi, PicfitUrl }
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class UblogApi(
    colls: UblogColls,
    rank: UblogRank,
    userRepo: UserRepo,
    picfitApi: PicfitApi,
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
        (post.live && prev.lived.isEmpty) ?? onFirstPublish(user, blog, post)
      } inject post
    }

  private def onFirstPublish(user: User, blog: UblogBlog, post: UblogPost): Funit =
    rank.computeRank(blog, post).?? { rank =>
      colls.post.updateField($id(post.id), "rank", rank).void
    } >>- {
      lila.common.Bus.publish(UblogPost.Create(post), "ublogPost")
      if (blog.visible) {
        timeline ! Propagate(
          lila.hub.actorApi.timeline.UblogPost(user.id, post.id.value, post.slug, post.title)
        ).toFollowersOf(user.id)
        if (blog.modTier.isEmpty) sendPostToZulip(user, blog, post).unit
      }
    }

  def getUserBlog(user: User, insertMissing: Boolean = false): Fu[UblogBlog] =
    getBlog(UblogBlog.Id.User(user.id)) getOrElse {
      val blog = UblogBlog make user
      (insertMissing ?? colls.blog.insert.one(blog).void) inject blog
    }

  def getBlog(id: UblogBlog.Id): Fu[Option[UblogBlog]] = colls.blog.byId[UblogBlog](id.full)

  def getPost(id: UblogPost.Id): Fu[Option[UblogPost]] = colls.post.byId[UblogPost](id.value)

  def findByUserBlogOrAdmin(id: UblogPost.Id, user: User): Fu[Option[UblogPost]] =
    colls.post.byId[UblogPost](id.value) dmap {
      _.filter(_.blog == UblogBlog.Id.User(user.id) || Granter(_.ModerateBlog)(user))
    }

  def findByIdAndBlog(id: UblogPost.Id, blog: UblogBlog.Id): Fu[Option[UblogPost]] =
    colls.post.one[UblogPost]($id(id) ++ $doc("blog" -> blog))

  def latestPosts(blogId: UblogBlog.Id, nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blogId, "live" -> true), previewPostProjection.some)
      .sort($doc("lived.at" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPreference.secondaryPreferred)
      .list(nb)

  def userBlogPreviewFor(user: User, nb: Int, forUser: Option[User]): Fu[Option[UblogPost.BlogPreview]] = {
    val blogId = UblogBlog.Id.User(user.id)
    val canView = fuccess(forUser exists user.is) >>|
      colls.blog.primitiveOne[UblogBlog.Tier]($id(blogId.full), "tier").dmap(~_ >= UblogBlog.Tier.VISIBLE)
    canView flatMap { _ ?? blogPreview(blogId, nb).dmap(some) }
  }

  def blogPreview(blogId: UblogBlog.Id, nb: Int): Fu[UblogPost.BlogPreview] =
    colls.post.countSel($doc("blog" -> blogId, "live" -> true)) zip
      latestPosts(blogId, nb) map
      (UblogPost.BlogPreview.apply _).tupled

  def latestPosts(nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("live" -> true), previewPostProjection.some)
      .sort($doc("rank" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPreference.secondaryPreferred)
      .list(nb)

  def otherPosts(blog: UblogBlog.Id, post: UblogPost, nb: Int = 4): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blog, "live" -> true, "_id" $ne post.id), previewPostProjection.some)
      .sort($doc("lived.at" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPreference.secondaryPreferred)
      .list(nb)

  private def imageRel(post: UblogPost) = s"ublog:${post.id}"

  def uploadImage(user: User, post: UblogPost, picture: PicfitApi.FilePart): Fu[UblogPost] =
    for {
      image <- picfitApi
        .uploadFile(imageRel(post), picture, userId = user.id)
      _ <- colls.post.update.one($id(post.id), $set("image" -> image.id))
    } yield post.copy(image = image.id.some)

  private def sendPostToZulip(user: User, blog: UblogBlog, post: UblogPost): Funit =
    irc.ublogPost(
      user,
      id = post.id.value,
      slug = post.slug,
      title = post.title,
      intro = post.intro
    )

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
      .one($id(blog), $set("modTier" -> tier, "tier" -> tier), upsert = true)
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
