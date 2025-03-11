package controllers

import play.api.i18n.Lang
import play.api.mvc.Result

import lila.app.{ *, given }
import scalalib.model.Language
import lila.i18n.{ LangList, LangPicker }
import lila.report.Suspect
import lila.ublog.{ UblogBlog, UblogPost, UblogRank }
import lila.core.i18n.toLanguage

final class Ublog(env: Env) extends LilaController(env):

  import views.ublog.ui.{ editUrlOfPost, urlOfPost, urlOfBlog }
  import scalalib.paginator.Paginator.given

  def index(username: UserStr, page: Int) = Open:
    NotForKidsUnlessOfficial(username):
      FoundPage(meOrFetch(username)): user =>
        env.ublog.api
          .getUserBlog(user)
          .flatMap: blog =>
            canViewBlogOf(user, blog)
              .so(env.ublog.paginator.byUser(user, true, page))
              .map:
                views.ublog.ui.blogPage(user, blog, _)

  def drafts(username: UserStr, page: Int) = Auth { ctx ?=> me ?=>
    NotForKids:
      WithBlogOf(username, _.draft): (user, blog) =>
        Ok.async:
          env.ublog.paginator
            .byBlog(blog.id, false, page)
            .map:
              views.ublog.ui.drafts(user, blog, _)
  }

  def post(username: UserStr, slug: String, id: UblogPostId) = Open: ctx ?=>
    Found(env.ublog.api.getPost(id)): post =>
      if slug == post.slug && post.isUserBlog(username)
      then handlePost(post)
      else
        val url = urlOfPost(post).url
        if url != ctx.req.path then Redirect(urlOfPost(post)) else handlePost(post)

  private def handlePost(post: UblogPost)(using Context) =
    val createdBy = post.created.by
    NotForKidsUnlessOfficial(createdBy):
      WithBlogOf(createdBy): (user, blog) =>
        canViewPost(user, blog)(post).so:
          for
            others         <- env.ublog.api.otherPosts(UblogBlog.Id.User(user.id), post)
            liked          <- ctx.user.so(env.ublog.rank.liked(post))
            followed       <- ctx.userId.so(env.relation.api.fetchFollows(_, user.id))
            prefFollowable <- ctx.isAuth.so(env.pref.api.followable(user.id))
            blocked        <- ctx.userId.so(env.relation.api.fetchBlocks(user.id, _))
            followable = prefFollowable && !blocked
            markup <- env.ublog.markup(post)
            viewedPost = env.ublog.viewCounter(post, ctx.ip)
            page <- renderPage:
              views.ublog.post.page(user, blog, viewedPost, markup, others, liked, followable, followed)
          yield Ok(page)

  def discuss(id: UblogPostId) = Open:
    NotForKids:
      import lila.forum.ForumCateg.ublogId
      val topicSlug = s"ublog-${id}"
      val redirect  = Redirect(routes.ForumTopic.show(ublogId, topicSlug))
      env.forum.topicRepo
        .existsByTree(ublogId, topicSlug)
        .flatMap:
          if _ then redirect
          else
            env.ublog.api
              .getPost(id)
              .flatMapz: post =>
                env.forum.topicApi.makeUblogDiscuss(
                  slug = topicSlug,
                  name = post.title,
                  url = s"${env.net.baseUrl}${routes.Ublog.post(post.created.by, post.slug, id)}",
                  ublogId = id,
                  authorId = post.created.by
                )
              .inject(redirect)
  private def WithBlogOf[U: UserIdOf](
      u: U
  )(f: (UserModel, UblogBlog) => Fu[Result])(using Context): Fu[Result] =
    Found(meOrFetch(u)): user =>
      env.ublog.api
        .getUserBlog(user)
        .flatMap: blog =>
          f(user, blog)

  private def WithBlogOf[U: UserIdOf](u: U, allows: UblogBlog.Allows => Boolean)(
      f: (UserModel, UblogBlog) => Fu[Result]
  )(using
      ctx: Context
  ): Fu[Result] =
    WithBlogOf(u): (user, blog) =>
      if !ctx.me.exists(env.ublog.api.canBlog) then
        Unauthorized.page:
          views.site.message.notYet:
            "Please play a few games and wait 2 days before you can create blog posts."
      else if allows(blog.allows) then f(user, blog)
      else Unauthorized("Not your blog to edit")

  def form(username: UserStr) = Auth { ctx ?=> me ?=>
    NotForKids:
      WithBlogOf(username, _.edit): (user, blog) =>
        Ok.page(views.ublog.form.create(user, env.ublog.form.create, anyCaptcha))
  }

  def create(username: UserStr) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      WithBlogOf(username, _.edit): (user, blog) =>
        bindForm(env.ublog.form.create)(
          err => BadRequest.page(views.ublog.form.create(user, err, anyCaptcha)),
          data =>
            limit.ublog(me, rateLimited, cost = if me.isVerified then 1 else 3):
              env.ublog.api
                .create(data, user)
                .map: post =>
                  lila.mon.ublog.create(user.id.value).increment()
                  Redirect(editUrlOfPost(post)).flashSuccess
        )
  }

  def edit(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      FoundPage(env.ublog.api.findEditableByMe(id)): post =>
        views.ublog.form.edit(post, env.ublog.form.edit(post))
  }

  def update(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      Found(env.ublog.api.findEditableByMe(id)): prev =>
        bindForm(env.ublog.form.edit(prev))(
          err => BadRequest.page(views.ublog.form.edit(prev, err)),
          data =>
            env.ublog.api.update(data, prev).flatMap { post =>
              logModAction(post, "edit").inject(Redirect(urlOfPost(post)).flashSuccess)
            }
        )

  }

  def delete(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    Found(env.ublog.api.findEditableByMe(id)): post =>
      for
        _ <- env.ublog.api.delete(post)
        _ <- logModAction(post, "delete")
      yield Redirect(urlOfBlog(post.blog)).flashSuccess
  }

  private def logModAction(post: UblogPost, action: String, logIncludingMe: Boolean = false)(using
      ctx: Context,
      me: Me
  ): Funit =
    isGrantedOpt(_.ModerateBlog).so:
      (logIncludingMe || !me.is(post.created.by)).so:
        env.user.repo
          .byId(post.created.by)
          .flatMapz: user =>
            env.mod.logApi.blogPostEdit(Suspect(user), post.id, post.title, action)

  def like(id: UblogPostId, v: Boolean) = Auth { ctx ?=> _ ?=>
    NoBot:
      NotForKids:
        env.ublog.rank
          .like(id, v)
          .map: likes =>
            Ok(likes.value)
  }

  def redirect(id: UblogPostId) = Open:
    Found(env.ublog.api.postPreview(id)): post =>
      Redirect(urlOfPost(post))

  def setTier(blogId: String) = SecureBody(_.ModerateBlog) { ctx ?=> me ?=>
    Found(UblogBlog.Id(blogId).so(env.ublog.api.getBlog)): blog =>
      bindForm(lila.ublog.UblogForm.tier)(
        _ => Redirect(urlOfBlog(blog)).flashFailure,
        tier =>
          for
            user <- env.user.repo.byId(blog.userId).orFail("Missing blog user!").dmap(Suspect.apply)
            _    <- env.ublog.api.setModTier(blog.id, tier)
            _    <- env.ublog.rank.recomputeRankOfAllPostsOfBlog(blog.id)
            _    <- env.mod.logApi.blogTier(user, UblogRank.Tier.name(tier))
          yield Redirect(urlOfBlog(blog)).flashSuccess
      )
  }

  def rankAdjust(postId: UblogPostId) = SecureBody(_.ModerateBlog) { ctx ?=> me ?=>
    Found(env.ublog.api.getPost(postId)): post =>
      bindForm(lila.ublog.UblogForm.adjust)(
        _ => Redirect(urlOfPost(post)).flashFailure,
        (pinned, tier, rankAdjustDays) =>
          for
            _ <- env.ublog.api.setModTier(post.blog, tier)
            _ <- env.ublog.api.setRankAdjust(post.id, ~rankAdjustDays, pinned)
            _ <- logModAction(
              post,
              s"Set tier: $tier, pinned: $pinned, post adjust: ${~rankAdjustDays} days",
              logIncludingMe = true
            )
            _ <- env.ublog.rank.recomputeRankOfAllPostsOfBlog(post.blog)
          yield Redirect(urlOfPost(post)).flashSuccess
      )
  }

  def image(id: UblogPostId) = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    Found(env.ublog.api.findEditableByMe(id)): post =>
      ctx.body.body
        .file("image")
        .match
          case Some(image) =>
            limit.imageUpload(ctx.ip, rateLimited):
              env.ublog.api.image.upload(me, post, image)
          case None =>
            env.ublog.api.image
              .delete(post)
              .flatMap: newPost =>
                logModAction(newPost, "delete image")
        .inject(Redirect(urlOfPost(post)).flashSuccess)
        .recover { case e: Exception =>
          BadRequest(e.getMessage)
        }
  }

  def friends(page: Int) = Auth { _ ?=> me ?=>
    NotForKids:
      Reasonable(page, Max(100)):
        Ok.async:
          env.ublog.paginator.liveByFollowed(me, page).map(views.ublog.ui.friends)
  }

  def communityLang(language: Language, page: Int = 1) = Open:
    import LangPicker.ByHref
    LangPicker.byHref(language, ctx.req) match
      case ByHref.NotFound        => Redirect(routes.Ublog.communityAll(page))
      case ByHref.Redir(language) => Redirect(routes.Ublog.communityLang(language, page))
      case ByHref.Refused(lang)   => communityIndex(lang.some, page)
      case ByHref.Found(lang) =>
        if ctx.isAuth then communityIndex(lang.some, page)
        else communityIndex(lang.some, page)(using ctx.withLang(lang))

  def communityAll(page: Int) = Open:
    communityIndex(none, page)

  private def communityIndex(l: Option[Lang], page: Int)(using ctx: Context) =
    NotForKids:
      Reasonable(page, Max(100)):
        pageHit
        Ok.async:
          val language = l.map(toLanguage)
          env.ublog.paginator
            .liveByCommunity(language, page)
            .map:
              views.ublog.community(language, _)

  def communityAtom(language: Language) = Anon:
    val found: Option[Lang] = LangList.popularNoRegion.find(l => toLanguage(l) == language)
    env.ublog.paginator
      .liveByCommunity(found.map(toLanguage), page = 1)
      .map: posts =>
        Ok.snip(views.ublog.ui.atom.community(language, posts.currentPageResults)).as(XML)

  def liked(page: Int) = Auth { ctx ?=> me ?=>
    NotForKids:
      Reasonable(page, Max(100)):
        Ok.async:
          env.ublog.paginator
            .liveByLiked(page)
            .map:
              views.ublog.ui.liked(_)
  }

  def topics = Open:
    NotForKids:
      Ok.async:
        env.ublog.topic.withPosts.map:
          views.ublog.ui.topics(_)

  def topic(str: String, page: Int, byDate: Boolean) = Open:
    NotForKids:
      Reasonable(page, Max(100)):
        lila.ublog.UblogTopic
          .fromUrl(str)
          .so: top =>
            Ok.async:
              env.ublog.paginator
                .liveByTopic(top, page, byDate)
                .map:
                  views.ublog.ui.topic(top, _, byDate)

  def userAtom(username: UserStr) = Anon:
    env.user.repo
      .enabledById(username)
      .flatMap:
        _.fold(notFound): user =>
          env.ublog.api
            .getUserBlog(user)
            .flatMap: blog =>
              (isBlogVisible(user, blog)
                .so(env.ublog.paginator.byUser(user, true, 1)))
                .map: posts =>
                  Ok.snip(views.ublog.ui.atom.user(user, posts.currentPageResults)).as(XML)

  def historicalBlogPost(id: String, slug: String) = Open:
    Found(env.ublog.api.getByPrismicId(id)): post =>
      Redirect(routes.Ublog.post(UserName.lichess, post.slug, post.id), MOVED_PERMANENTLY)

  private def isBlogVisible(user: UserModel, blog: UblogBlog) = user.enabled.yes && blog.visible

  def NotForKidsUnlessOfficial(username: UserStr)(f: => Fu[Result])(using Context): Fu[Result] =
    if username.is(UserId.lichess) then f else NotForKids(f)

  private def canViewBlogOf(user: UserModel, blog: UblogBlog)(using ctx: Context) =
    ctx.is(user) || isGrantedOpt(_.ModerateBlog) || isBlogVisible(user, blog)

  private def canViewPost(user: UserModel, blog: UblogBlog)(post: UblogPost)(using Context) =
    canViewBlogOf(user, blog) && post.canView
