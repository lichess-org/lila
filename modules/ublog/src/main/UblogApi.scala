package lila.ublog

import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.*
import reactivemongo.api.bson.BSONDocument

import lila.core.shutup.{ PublicSource, ShutupApi }
import lila.core.timeline as tl
import lila.core.LightUser
import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi
import lila.core.user.KidMode
import lila.core.ublog.{ BlogsBy, Quality }
import lila.core.timeline.{ Propagate, UblogPostLike }
import lila.common.LilaFuture.delay

final class UblogApi(
    colls: UblogColls,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    picfitApi: PicfitApi,
    shutupApi: ShutupApi,
    irc: lila.core.irc.IrcApi,
    automod: UblogAutomod,
    config: UblogConfig
)(using Executor, Scheduler)
    extends lila.core.ublog.UblogApi:

  import UblogBsonHandlers.{ *, given }
  import UblogBlog.Tier
  import UblogAutomod.Assessment

  def create(data: UblogForm.UblogPostData, author: User): Fu[UblogPost] =
    val post = data.create(author)
    colls.post.insert
      .one(bsonWriteObjTry[UblogPost](post).get ++ $doc("likers" -> List(author.id)))
      .inject(post)

  def getByPrismicId(id: String): Fu[Option[UblogPost]] = colls.post.one[UblogPost]($doc("prismicId" -> id))

  def update(data: UblogForm.UblogPostData, prev: UblogPost)(using me: Me): Fu[UblogPost] = for
    author <- userApi.byId(prev.created.by).map(_ | me.value)
    blog <- getUserBlog(author, insertMissing = true)
    post = data.update(me.value, prev)
    isFirstPublish = prev.lived.isEmpty && post.live
    _ <- colls.post.update.one($id(prev.id), $set(bsonWriteObjTry[UblogPost](post).get))
    _ = if isFirstPublish then onFirstPublish(author.light, blog, post)
  yield
    triggerAutomod(post).foreach: res =>
      if isFirstPublish && blog.visible
      then sendPostToZulip(author.light, post, blog.modTier.getOrElse(blog.tier), res)
    post

  private def onFirstPublish(author: LightUser, blog: UblogBlog, post: UblogPost) =
    lila.common.Bus.pub(UblogPost.Create(post))
    if blog.visible then
      lila.common.Bus.pub:
        tl.Propagate(tl.UblogPost(author.id, post.id, post.slug, post.title))
          .toFollowersOf(post.created.by)
      shutupApi.publicText(author.id, post.allText, PublicSource.Ublog(post.id))

  def getUserBlogOption(user: User): Fu[Option[UblogBlog]] =
    getBlog(UblogBlog.Id.User(user.id))

  def getUserBlog(user: User, insertMissing: Boolean = false): Fu[UblogBlog] =
    getUserBlogOption(user).getOrElse:
      if insertMissing then
        val blog = UblogBlog.make(user)
        for _ <- colls.blog.insert.one(blog).void yield blog
      else fuccess(UblogBlog.make(user))

  def getBlog(id: UblogBlog.Id): Fu[Option[UblogBlog]] = colls.blog.byId[UblogBlog](id.full)

  def getPost(id: UblogPostId): Fu[Option[UblogPost]] = colls.post.byIdProj[UblogPost](id, postProjection)

  def findEditableByMe(id: UblogPostId)(using me: Me): Fu[Option[UblogPost]] =
    colls.post
      .byIdProj[UblogPost](id, postProjection)
      .dmap:
        _.filter(_.allows.edit)

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
        .primitiveOne[Tier]($id(blogId.full), "tier")
        .dmap(_.exists(_ > Tier.HIDDEN))
    canView.flatMapz { blogPreview(blogId, nb).dmap(some) }

  def blogPreview(blogId: UblogBlog.Id, nb: Int): Fu[UblogPost.BlogPreview] =
    colls.post
      .countSel($doc("blog" -> blogId, "live" -> true))
      .zip(latestPosts(blogId, nb))
      .map((UblogPost.BlogPreview.apply).tupled)

  def fetchCarouselFromDb(): Fu[UblogPost.CarouselPosts] =
    for
      pinned <- colls.post
        .find($doc("live" -> true, "featured.until" -> $gte(nowInstant)), previewPostProjection.some)
        .sort($doc("featured.until" -> -1))
        .cursor[UblogPost.PreviewPost](ReadPref.sec)
        .list(config.carouselSize)

      queue <- colls.post
        .find(
          $doc("live" -> true, "featured.at" -> $gte(nowInstant.minusMonths(1))),
          previewPostProjection.some
        )
        .sort($doc("featured.at" -> -1))
        .cursor[UblogPost.PreviewPost](ReadPref.sec)
        .list(config.carouselSize - pinned.size)
    yield UblogPost.CarouselPosts(pinned, queue)

  def postPreview(id: UblogPostId) =
    colls.post.byId[UblogPost.PreviewPost](id, previewPostProjection)

  def postPreviews(ids: Seq[UblogPostId]): Future[Seq[UblogPost.PreviewPost]] = ids.nonEmpty.so:
    colls.post
      .find($inIds(ids) ++ $doc("live" -> true), previewPostProjection.some)
      .cursor[UblogPost.PreviewPost](ReadPref.sec)
      .list(config.searchPageSize.value)
      .map: results =>
        ids.flatMap(results.mapBy(_.id).get) // lila-search order

  def recommend(blog: UblogBlog.Id, post: UblogPost)(using kid: KidMode): Fu[List[UblogPost.PreviewPost]] =
    for
      sameAuthor <- colls.post
        .find(
          $doc("blog" -> blog, "live" -> true, "_id".$ne(post.id), "automod.evergreen".$ne(false)),
          previewPostProjection.some
        )
        .sort($doc("lived.at" -> -1))
        .cursor[UblogPost.PreviewPost](ReadPref.sec)
        .list(3)
      similarIds = post.similar.so(_.filterNot(s => s.count < 4 || sameAuthor.exists(_.id == s.id)).map(_.id))
      similar <- colls.post
        .find(
          $inIds(similarIds) ++ $doc("live" -> true, "automod.evergreen".$ne(false)),
          previewPostProjection.some
        )
        .cursor[UblogPost.PreviewPost](ReadPref.sec)
        .list(9)
      mix = (similar ++ sameAuthor).filter(_.isLichess || kid.no)
    yield scala.util.Random.shuffle(mix).take(6)

  private def sendPostToZulip(
      user: LightUser,
      post: UblogPost,
      tier: Tier,
      assessment: Option[UblogAutomod.Assessment]
  ): Funit =
    val source =
      if tier == Tier.UNLISTED then "unlisted tier"
      else assessment.fold(Tier.name(tier).toLowerCase + " tier")(_.quality.name + " quality")
    val emdashes = post.markdown.value.count(_ == '—')
    val automodNotes = assessment.map: r =>
      ~r.flagged.map("Flagged: " + _ + "\n") +
        ~r.commercial.map("Commercial: " + _ + "\n") +
        (emdashes match
          case 0 => ""
          case 1 => s"#### 1 emdash found\n"
          case n => s"#### $n emdashes found\n")
    irc.ublogPost(
      user,
      id = post.id,
      slug = post.slug,
      title = post.title,
      intro = post.intro,
      topic = s"$source new posts",
      automodNotes
    )

  private def triggerAutomod(post: UblogPost): Fu[Option[UblogAutomod.Assessment]] =
    val retries = 5 // 30s, 1m, 2m, 4m, 8m
    def attempt(n: Int): Fu[Option[UblogAutomod.Assessment]] =
      automod(post, n * 0.1)
        .flatMapz: llm =>
          val result = post.automod.foldLeft(llm)(_.updateByLLM(_))
          for _ <- colls.post.updateField($id(post.id), "automod", result)
          yield result.some
        .recoverWith: e =>
          if n < retries then delay((30 * math.pow(2, n).toInt).seconds)(attempt(n + 1))
          else
            logger.warn(s"automod ${post.id} failed after $retries retry attempts", e)
            fuccess(none)
    attempt(0)

  def liveLightsByIds(ids: List[UblogPostId]): Fu[List[UblogPost.LightPost]] =
    colls.post
      .find($inIds(ids) ++ $doc("live" -> true), lightPostProjection.some)
      .cursor[UblogPost.LightPost]()
      .list(30)

  def delete(post: UblogPost): Funit = for
    _ <- colls.post.delete.one($id(post.id))
    _ <- image.deleteAll(post)
  yield ()

  def setTierIfBlogExists(blog: UblogBlog.Id, tier: Tier): Funit =
    colls.blog.update.one($id(blog), $set("tier" -> tier)).void

  def onAccountClose(user: User) = setTierIfBlogExists(UblogBlog.Id.User(user.id), Tier.HIDDEN)

  def onAccountReopen(user: User) = getUserBlogOption(user).flatMapz: blog =>
    setTierIfBlogExists(UblogBlog.Id.User(user.id), blog.modTier | Tier.default(user))

  def onAccountDelete(user: User) = for
    _ <- colls.blog.delete.one($id(UblogBlog.Id.User(user.id)))
    _ <- colls.post.delete.one($doc("blog" -> UblogBlog.Id.User(user.id)))
  yield ()

  def postCursor(user: User): AkkaStreamCursor[UblogPost] =
    colls.post.find($doc("blog" -> s"user:${user.id}")).cursor[UblogPost](ReadPref.sec)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    colls.post.exists($id(post.id) ++ $doc("likers" -> user.id))

  def like(postId: UblogPostId, v: Boolean)(using me: Me): Fu[UblogPost.Likes] = for
    res <- colls.post.update.one($id(postId), $addOrPull("likers", me.userId, v))
    aggResult <- colls.post.aggregateOne(): framework =>
      import framework.*
      Match($id(postId)) -> List(
        PipelineOperator(
          $lookup.simple(from = colls.blog, as = "blog", local = "blog", foreign = "_id")
        ),
        UnwindField("blog"),
        Project($doc("tier" -> "$blog.tier", "likes" -> $doc("$size" -> "$likers"), "title" -> true))
      )
    found = for
      doc <- aggResult
      id <- doc.getAsOpt[UblogPostId]("_id")
      likes <- doc.getAsOpt[UblogPost.Likes]("likes")
      tier <- doc.getAsOpt[Tier]("tier")
      title <- doc.string("title")
    yield (id, likes, tier, title)
    likes <- found match
      case None => fuccess(UblogPost.Likes(0))
      case Some(id, likes, tier, title) =>
        for
          _ <- colls.post.updateField($id(postId), "likes", likes)
          _ =
            if res.nModified > 0 && v && tier > Tier.HIDDEN
            then lila.common.Bus.pub(Propagate(UblogPostLike(me, id, title)).toFollowersOf(me))
        yield likes
  yield likes

  def modBlog(blogger: UserId, tier: Option[Tier], note: Option[String], mod: Option[Me] = None): Funit =
    val setFields = tier.so(t => $doc("modTier" -> t, "tier" -> t))
      ++ note.filter(_ != "").so(n => $doc("modNote" -> n))
    val unsets = note.exists(_ == "").so($unset("modNote")) // "" is unset, none to ignore
    mod.foreach(m => irc.ublogBlog(blogger, m.username, tier.map(Tier.name), note))
    colls.blog.update.one($id(UblogBlog.Id.User(blogger)), $set(setFields) ++ unsets, upsert = true).void

  def modPost(
      post: UblogPost,
      d: UblogForm.ModPostData
  )(using mod: Me): Fu[Option[UblogAutomod.Assessment]] =
    def maybeCopy(v: Option[String], base: Option[String]) =
      v match
        case Some("") => none // form sends empty string to unset
        case None => base
        case _ => v
    if !d.hasUpdates then fuccess(post.automod)
    else
      val base = post.automod.getOrElse(Assessment(quality = Quality.good))
      val assessment = Assessment(
        quality = d.quality | base.quality,
        evergreen = d.evergreen.orElse(base.evergreen),
        flagged = maybeCopy(d.flagged, base.flagged),
        commercial = maybeCopy(d.commercial, base.commercial),
        lockedBy = mod.some
      )
      for _ <- colls.post.updateField($id(post.id), "automod", assessment)
      yield assessment.some

  def setFeatured(post: UblogPost, data: UblogForm.ModPostData)(using
      me: Me
  ): Fu[Option[UblogPost.Featured]] =
    if data.featured.isEmpty && data.featuredUntil.isEmpty then fuccess(post.featured)
    else
      val featured = data.featured.orZero.option:
        UblogPost.Featured(
          me.userId,
          at = data.featuredUntil.isEmpty.option(nowInstant),
          until = data.featuredUntil.map(nowInstant.plusDays)
        )
      for _ <- colls.post.updateOrUnsetField($id(post.id), "featured", featured)
      yield featured

  private[ublog] def setShadowban(userId: UserId, v: Boolean) = {
    if v then fuccess(Tier.HIDDEN)
    else userApi.byId(userId).map(_.fold(Tier.HIDDEN)(Tier.default))
  }.flatMap: t =>
    modBlog(userId, t.some, none)

  def canBlog(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }

  private[ublog] def aggregateVisiblePosts(
      select: Bdoc,
      offset: Int,
      length: Int,
      sort: BlogsBy = BlogsBy.newest
  ) =
    colls.post
      .aggregateList(length, _.sec): framework =>
        import framework.*
        val aggSort = sort match
          case BlogsBy.oldest => Ascending("lived.at")
          case BlogsBy.likes => Descending("likes")
          case _ => Descending("lived.at")
        Match(select ++ $doc("live" -> true)) -> {
          Sort(aggSort) ::
            removeUnlistedOrClosedAndProjectForPreview(colls.post, framework) :::
            List(Skip(offset), Limit(length))
        }
      .map: docs =>
        for
          doc <- docs
          post <- doc.asOpt[UblogPost.PreviewPost]
        yield post

  private[ublog] def removeUnlistedOrClosedAndProjectForPreview(
      coll: Coll,
      framework: coll.AggregationFramework.type
  ) =
    import framework.*
    List(
      PipelineOperator:
        $lookup.simple(
          from = colls.blog,
          as = "blog",
          local = "blog",
          foreign = "_id",
          pipe = List(
            $doc("$match" -> $expr($doc("$gt" -> $arr("$tier", Tier.UNLISTED)))),
            $doc("$project" -> $id(true))
          )
        )
      ,
      UnwindField("blog"),
      PipelineOperator:
        $lookup.simple(
          from = userRepo.coll,
          as = "user",
          local = "created.by",
          foreign = "_id",
          pipe = List(
            $doc("$match" -> $doc(lila.core.user.BSONFields.enabled -> true)),
            $doc("$project" -> $id(true))
          )
        )
      ,
      UnwindField("user"),
      Project(previewPostProjection ++ $doc("blog" -> "$blog._id"))
    )

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
