package lila.ublog

import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.*

import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline.Propagate
import lila.hub.actorApi.shutup.{ PublicSource, RecordPublicText }
import lila.memo.PicfitApi
import lila.security.Granter
import lila.user.{ User, UserApi, Me }

final class UblogApi(
    colls: UblogColls,
    rank: UblogRank,
    userApi: UserApi,
    picfitApi: PicfitApi,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    irc: lila.irc.IrcApi
)(using Executor):

  import UblogBsonHandlers.{ *, given }

  def create(data: UblogForm.UblogPostData)(using me: Me): Fu[UblogPost] =
    val post = data.create(me.value)
    colls.post.insert.one(
      bsonWriteObjTry[UblogPost](post).get ++ $doc("likers" -> List(me.userId))
    ) inject post

  def update(data: UblogForm.UblogPostData, prev: UblogPost)(using me: Me): Fu[UblogPost] =
    getUserBlog(me.value, insertMissing = true) flatMap { blog =>
      val post = data.update(me.value, prev)
      colls.post.update.one($id(prev.id), $set(bsonWriteObjTry[UblogPost](post).get)) >> {
        (post.live && prev.lived.isEmpty) so onFirstPublish(me.value, blog, post)
      } inject post
    }

  private def onFirstPublish(user: User, blog: UblogBlog, post: UblogPost): Funit =
    rank
      .computeRank(blog, post)
      .so: rank =>
        colls.post.updateField($id(post.id), "rank", rank).void
      .andDo:
        lila.common.Bus.publish(UblogPost.Create(post), "ublogPost")
        if blog.visible then
          timeline ! Propagate(
            lila.hub.actorApi.timeline.UblogPost(user.id, post.id, post.slug, post.title)
          ).toFollowersOf(user.id)
          shutup ! RecordPublicText(user.id, post.allText, PublicSource.Ublog(post.id))
          if blog.modTier.isEmpty then sendPostToZulipMaybe(user, post)

  def getUserBlog(user: User, insertMissing: Boolean = false): Fu[UblogBlog] =
    getBlog(UblogBlog.Id.User(user.id)) getOrElse
      userApi
        .withPerfs(user)
        .flatMap: user =>
          val blog = UblogBlog make user
          (insertMissing so colls.blog.insert.one(blog).void) inject blog

  def getBlog(id: UblogBlog.Id): Fu[Option[UblogBlog]] = colls.blog.byId[UblogBlog](id.full)

  def getPost(id: UblogPostId): Fu[Option[UblogPost]] = colls.post.byId[UblogPost](id)

  def findByUserBlogOrAdmin(id: UblogPostId)(using me: Me): Fu[Option[UblogPost]] =
    colls.post.byId[UblogPost](id) dmap:
      _.filter(_.isBy(me) || Granter(_.ModerateBlog))

  def findByIdAndBlog(id: UblogPostId, blog: UblogBlog.Id): Fu[Option[UblogPost]] =
    colls.post.one[UblogPost]($id(id) ++ $doc("blog" -> blog))

  def latestPosts(blogId: UblogBlog.Id, nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blogId, "live" -> true), previewPostProjection.some)
      .sort($doc("lived.at" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def userBlogPreviewFor(user: User, nb: Int)(using me: Option[Me]): Fu[Option[UblogPost.BlogPreview]] =
    val blogId = UblogBlog.Id.User(user.id)
    val canView = fuccess(me.exists(_ is user)) >>|
      colls.blog
        .primitiveOne[UblogBlog.Tier]($id(blogId.full), "tier")
        .dmap(_.exists(_ >= UblogBlog.Tier.VISIBLE))
    canView flatMapz { blogPreview(blogId, nb).dmap(some) }

  def blogPreview(blogId: UblogBlog.Id, nb: Int): Fu[UblogPost.BlogPreview] =
    colls.post.countSel($doc("blog" -> blogId, "live" -> true)) zip
      latestPosts(blogId, nb) map
      (UblogPost.BlogPreview.apply).tupled

  def latestPosts(nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("live" -> true), previewPostProjection.some)
      .sort($doc("rank" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def otherPosts(blog: UblogBlog.Id, post: UblogPost, nb: Int = 4): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blog, "live" -> true, "_id" $ne post.id), previewPostProjection.some)
      .sort($doc("lived.at" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def postPreview(id: UblogPostId) =
    colls.post.byId[UblogPost.PreviewPost](id, previewPostProjection)

  private def imageRel(post: UblogPost) = s"ublog:${post.id}"

  def uploadImage(user: User, post: UblogPost, picture: PicfitApi.FilePart): Fu[UblogPost] =
    for
      pic <- picfitApi.uploadFile(imageRel(post), picture, userId = user.id)
      image = post.image.fold(UblogImage(pic.id))(_.copy(id = pic.id))
      _ <- colls.post.updateField($id(post.id), "image", image)
    yield post.copy(image = image.some)

  def deleteImage(post: UblogPost): Fu[UblogPost] =
    picfitApi.deleteByRel(imageRel(post)) >>
      colls.post.unsetField($id(post.id), "image") inject post.copy(image = none)

  private def sendPostToZulipMaybe(user: User, post: UblogPost): Funit =
    (post.markdown.value.sizeIs > 1000) so
      irc.ublogPost(
        user,
        id = post.id,
        slug = post.slug,
        title = post.title,
        intro = post.intro
      )

  def liveLightsByIds(ids: List[UblogPostId]): Fu[List[UblogPost.LightPost]] =
    colls.post
      .find($inIds(ids) ++ $doc("live" -> true), lightPostProjection.some)
      .cursor[UblogPost.LightPost]()
      .list(30)

  def delete(post: UblogPost): Funit =
    colls.post.delete.one($id(post.id)) >>
      picfitApi.deleteByRel(imageRel(post))

  def setTier(blog: UblogBlog.Id, tier: UblogBlog.Tier): Funit =
    colls.blog.update
      .one($id(blog), $set("modTier" -> tier, "tier" -> tier), upsert = true)
      .void

  def postCursor(user: User): AkkaStreamCursor[UblogPost] =
    colls.post.find($doc("blog" -> s"user:${user.id}")).cursor[UblogPost](ReadPref.priTemp)

  private[ublog] def setShadowban(userId: UserId, v: Boolean) = {
    if v then fuccess(UblogBlog.Tier.HIDDEN)
    else userApi.withPerfs(userId).map(_.fold(UblogBlog.Tier.HIDDEN)(UblogBlog.Tier.default))
  }.flatMap:
    setTier(UblogBlog.Id.User(userId), _)

  def canBlog(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }
