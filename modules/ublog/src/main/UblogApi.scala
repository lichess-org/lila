package lila.ublog

import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.*

import lila.core.shutup.{ PublicSource, ShutupApi }
import lila.core.timeline as tl
import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi

final class UblogApi(
    colls: UblogColls,
    userRepo: lila.core.user.UserRepo,
    rank: UblogRank,
    userApi: lila.core.user.UserApi,
    picfitApi: PicfitApi,
    shutupApi: ShutupApi,
    irc: lila.core.irc.IrcApi
)(using Executor)
    extends lila.core.ublog.UblogApi:

  import UblogBsonHandlers.{ *, given }

  def create(data: UblogForm.UblogPostData, author: User): Fu[UblogPost] =
    val post = data.create(author)
    colls.post.insert
      .one(bsonWriteObjTry[UblogPost](post).get ++ $doc("likers" -> List(author.id)))
      .inject(post)

  def getByPrismicId(id: String): Fu[Option[UblogPost]] = colls.post.one[UblogPost]($doc("prismicId" -> id))

  def update(data: UblogForm.UblogPostData, prev: UblogPost)(using me: Me): Fu[UblogPost] = for
    author <- userApi.byId(prev.created.by).map(_ | me.value)
    blog   <- getUserBlog(author, insertMissing = true)
    post = data.update(me.value, prev)
    _ <- colls.post.update.one($id(prev.id), $set(bsonWriteObjTry[UblogPost](post).get))
    _ <- (post.live && prev.lived.isEmpty).so(onFirstPublish(author, blog, post))
  yield post

  private def onFirstPublish(author: User, blog: UblogBlog, post: UblogPost): Funit =
    for _ <- rank
        .computeRank(blog, post)
        .so: rank =>
          colls.post.updateField($id(post.id), "rank", rank).void
    yield
      lila.common.Bus.publish(UblogPost.Create(post), "ublogPost")
      if blog.visible then
        lila.common.Bus.pub:
          tl.Propagate(tl.UblogPost(author.id, post.id, post.slug, post.title))
            .toFollowersOf(post.created.by)
        shutupApi.publicText(author.id, post.allText, PublicSource.Ublog(post.id))
        sendPostToZulip(author, post, blog.modTier)

  def getUserBlogOption(user: User, insertMissing: Boolean = false): Fu[Option[UblogBlog]] =
    getBlog(UblogBlog.Id.User(user.id))

  def getUserBlog(user: User, insertMissing: Boolean = false): Fu[UblogBlog] =
    getUserBlogOption(user).getOrElse:
      if insertMissing then
        for
          user <- userApi.withPerfs(user)
          blog = UblogBlog.make(user)
          _ <- colls.blog.insert.one(blog).void
        yield blog
      else fuccess(UblogBlog.makeWithoutPerfs(user))

  def getBlog(id: UblogBlog.Id): Fu[Option[UblogBlog]] = colls.blog.byId[UblogBlog](id.full)

  def getPost(id: UblogPostId): Fu[Option[UblogPost]] = colls.post.byIdProj[UblogPost](id, postProjection)

  def findEditableByMe(id: UblogPostId)(using me: Me): Fu[Option[UblogPost]] =
    colls.post
      .byIdProj[UblogPost](id, postProjection)
      .dmap:
        _.filter(_.allows.edit)

  def findByIdAndBlog(id: UblogPostId, blog: UblogBlog.Id): Fu[Option[UblogPost]] =
    colls.post.one[UblogPost]($id(id) ++ $doc("blog" -> blog), postProjection)

  def latestPosts(blogId: UblogBlog.Id, nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blogId, "live" -> true), previewPostProjection.some)
      .sort(userLiveSort)
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def userBlogPreviewFor(user: User, nb: Int)(using me: Option[Me]): Fu[Option[UblogPost.BlogPreview]] =
    val blogId = UblogBlog.Id.User(user.id)
    val canView = fuccess(me.exists(_.is(user))) >>|
      colls.blog
        .primitiveOne[UblogRank.Tier]($id(blogId.full), "tier")
        .dmap(_.exists(_ >= UblogRank.Tier.VISIBLE))
    canView.flatMapz { blogPreview(blogId, nb).dmap(some) }

  def blogPreview(blogId: UblogBlog.Id, nb: Int): Fu[UblogPost.BlogPreview] =
    colls.post
      .countSel($doc("blog" -> blogId, "live" -> true))
      .zip(latestPosts(blogId, nb))
      .map((UblogPost.BlogPreview.apply).tupled)

  def pinnedPosts(nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("live" -> true, "pinned" -> true), previewPostProjection.some)
      .sort($doc("rank" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def latestPosts(nb: Int): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find(
        $doc("live" -> true, "pinned".$ne(true), "topics".$ne(UblogTopic.offTopic)),
        previewPostProjection.some
      )
      .sort($doc("rank" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def otherPosts(blog: UblogBlog.Id, post: UblogPost, nb: Int = 4): Fu[List[UblogPost.PreviewPost]] =
    colls.post
      .find($doc("blog" -> blog, "live" -> true, "_id".$ne(post.id)), previewPostProjection.some)
      .sort($doc("lived.at" -> -1))
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(nb)

  def postPreview(id: UblogPostId) =
    colls.post.byId[UblogPost.PreviewPost](id, previewPostProjection)

  object image:
    private def rel(post: UblogPost) = s"ublog:${post.id}"

    def upload(user: User, post: UblogPost, picture: PicfitApi.FilePart): Fu[UblogPost] = for
      pic <- picfitApi.uploadFile(rel(post), picture, userId = user.id)
      image = post.image.fold(UblogImage(pic.id))(_.copy(id = pic.id))
      _ <- colls.post.updateField($id(post.id), "image", image)
    yield post.copy(image = image.some)

    def deleteAll(post: UblogPost): Funit = for
      _ <- deleteImage(post)
      _ <- picfitApi.deleteByIdsAndUser(PicfitApi.findInMarkdown(post.markdown).toSeq, post.created.by)
    yield ()

    def delete(post: UblogPost): Fu[UblogPost] = for
      _ <- deleteImage(post)
      _ <- colls.post.unsetField($id(post.id), "image")
    yield post.copy(image = none)

    def deleteImage(post: UblogPost): Funit = picfitApi.deleteByRel(rel(post))

  private def sendPostToZulip(user: User, post: UblogPost, modTier: Option[UblogRank.Tier]): Funit =
    val tierName = modTier.fold("non-tiered")(t => s"${UblogRank.Tier.name(t).toLowerCase} tier")
    irc.ublogPost(
      user.light,
      id = post.id,
      slug = post.slug,
      title = post.title,
      intro = post.intro,
      topic = s"$tierName new posts"
    )

  def liveLightsByIds(ids: List[UblogPostId]): Fu[List[UblogPost.LightPost]] =
    colls.post
      .find($inIds(ids) ++ $doc("live" -> true), lightPostProjection.some)
      .cursor[UblogPost.LightPost]()
      .list(30)

  def delete(post: UblogPost): Funit = for
    _ <- colls.post.delete.one($id(post.id))
    _ <- image.deleteAll(post)
  yield ()

  def setModTier(blog: UblogBlog.Id, tier: UblogRank.Tier): Funit =
    colls.blog.update
      .one($id(blog), $set("modTier" -> tier, "tier" -> tier), upsert = true)
      .void

  def setTierIfBlogExists(blog: UblogBlog.Id, tier: UblogRank.Tier): Funit =
    colls.blog.update.one($id(blog), $set("tier" -> tier)).void

  def setRankAdjust(id: UblogPostId, adjust: Int, pinned: Boolean): Funit =
    colls.post.update.one($id(id), $set("rankAdjustDays" -> adjust, "pinned" -> pinned)).void

  def onAccountClose(user: User) = setTierIfBlogExists(UblogBlog.Id.User(user.id), UblogRank.Tier.HIDDEN)

  def onAccountReopen(user: User) = getUserBlogOption(user).flatMapz: blog =>
    setTierIfBlogExists(UblogBlog.Id.User(user.id), blog.modTier | UblogRank.Tier.defaultWithoutPerfs(user))

  def onAccountDelete(user: User) = for
    _ <- colls.blog.delete.one($id(UblogBlog.Id.User(user.id)))
    _ <- colls.post.delete.one($doc("blog" -> UblogBlog.Id.User(user.id)))
  yield ()

  def postCursor(user: User): AkkaStreamCursor[UblogPost] =
    colls.post.find($doc("blog" -> s"user:${user.id}")).cursor[UblogPost](ReadPref.priTemp)

  private[ublog] def setShadowban(userId: UserId, v: Boolean) = {
    if v then fuccess(UblogRank.Tier.HIDDEN)
    else userApi.withPerfs(userId).map(_.fold(UblogRank.Tier.HIDDEN)(UblogRank.Tier.default))
  }.flatMap:
    setModTier(UblogBlog.Id.User(userId), _)

  def canBlog(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }

  // So far this only hits a prod index if $select contains `topics`, or if byDate is false
  // i.e. byDate can only be true if $select contains `topics`
  private[ublog] def aggregateVisiblePosts(
      select: Bdoc,
      offset: Int,
      length: Int,
      ranking: UblogRank.Type = UblogRank.Type.ByRank
  ) =
    colls.post
      .aggregateList(length, _.sec): framework =>
        import framework.*
        Match(select ++ $doc("live" -> true)) -> (ranking.sortingQuery(colls.post, framework) ++
          List(Limit(500))
          ++ removeUnlistedOrClosed(colls.post, framework) ++ List(
            Skip(offset),
            Limit(length)
          ))
      .map: docs =>
        for
          doc  <- docs
          post <- doc.asOpt[UblogPost.PreviewPost]
        yield post

  private[ublog] def removeUnlistedOrClosed(coll: Coll, framework: coll.AggregationFramework.type) =
    import framework.*
    List(
      PipelineOperator:
        $lookup.pipelineBC(
          from = colls.blog,
          as = "blog",
          local = "blog",
          foreign = "_id",
          pipe = List(
            $doc("$match"   -> $expr($doc("$gte" -> $arr("$tier", UblogRank.Tier.LOW)))),
            $doc("$project" -> $id(true))
          )
        )
      ,
      UnwindField("blog"),
      PipelineOperator:
        $lookup.pipelineBC(
          from = userRepo.coll,
          as = "user",
          local = "created.by",
          foreign = "_id",
          pipe = List(
            $doc("$match"   -> $doc(lila.core.user.BSONFields.enabled -> true)),
            $doc("$project" -> $id(true))
          )
        )
      ,
      UnwindField("user"),
      Project(previewPostProjection ++ $doc("blog" -> "$blog._id"))
    )
