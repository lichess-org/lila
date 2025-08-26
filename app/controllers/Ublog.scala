package controllers

import scala.annotation.nowarn
import play.api.i18n.Lang
import play.api.mvc.Result
import play.api.libs.json.*

import lila.app.{ *, given }
import scalalib.model.Language
import lila.i18n.{ LangList, LangPicker }
import lila.report.Suspect
import lila.ublog.{ UblogBlog, UblogPost, UblogByMonth }
import lila.core.ublog.{ BlogsBy, Quality, QualityFilter }
import lila.core.i18n.toLanguage
import lila.ublog.UblogForm.ModPostData
import lila.common.HTTPRequest

final class Ublog(env: Env) extends LilaController(env):

  import views.ublog.ui.{ editUrlOfPost, urlOfPost, urlOfBlog }
  import scalalib.paginator.Paginator.given

  def index(username: UserStr, page: Int) = Open:
    NotForKidsUnlessOfficial(username):
      FoundPage(meOrFetch(username)): user =>
        for
          blog <- env.ublog.api.getUserBlog(user)
          posts <- canViewBlogOf(user, blog).so(env.ublog.paginator.byUser(user, true, page))
        yield views.ublog.ui.blogPage(user, blog, posts)

  def drafts(username: UserStr, page: Int) = Auth { ctx ?=> me ?=>
    NotForKids:
      WithBlogOf(username, _.draft): (user, blog) =>
        for
          posts <- env.ublog.paginator.byBlog(blog.id, false, page)
          page <- renderPage(views.ublog.ui.drafts(user, blog, posts))
        yield Ok(page).hasPersonalData
  }

  def post(username: UserStr, slug: String, id: UblogPostId) = Open: ctx ?=>
    Found(env.ublog.api.getPost(id)): post =>
      if !post.visibleByCrawlers && HTTPRequest.isCrawler(req).yes
      then notFound
      else if slug == post.slug && post.isUserBlog(username) then handlePost(post)
      else if urlOfPost(post).url != ctx.req.path then Redirect(urlOfPost(post))
      else handlePost(post)

  private def handlePost(post: UblogPost)(using Context) =
    val createdBy = post.created.by
    NotForKidsUnlessOfficial(createdBy):
      WithBlogOf(createdBy): (user, blog) =>
        (canViewBlogOf(user, blog) && post.canView).so:
          for
            otherPosts <- env.ublog.api.recommend(UblogBlog.Id.User(user.id), post)
            liked <- ctx.user.so(env.ublog.api.liked(post))
            followed <- ctx.userId.so(env.relation.api.fetchFollows(_, user.id))
            prefFollowable <- ctx.isAuth.so(env.pref.api.followable(user.id))
            blocked <- ctx.userId.so(env.relation.api.fetchBlocks(user.id, _))
            isInCarousel <- isGrantedOpt(_.ModerateBlog)
              .so(env.ublog.api.fetchCarouselFromDb().map(_.has(post.id)))
            followable = prefFollowable && !blocked
            markup <- env.ublog.markup(post)
            viewedPost = env.ublog.viewCounter(post, ctx.ip)
            page <- renderPage:
              views.ublog.post.page(
                user,
                blog,
                viewedPost,
                markup,
                otherPosts,
                liked,
                followable,
                followed,
                isInCarousel
              )
          yield Ok(page)

  def discuss(id: UblogPostId) = Open:
    NotForKids:
      import lila.forum.ForumCateg.ublogId
      val topicSlug = lila.core.id.ForumTopicSlug(s"ublog-$id")
      val redirect = Redirect(routes.ForumTopic.show(ublogId, topicSlug))
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
      for
        blog <- env.ublog.api.getUserBlog(user)
        res <- f(user, blog)
      yield res

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
      WithBlogOf(username, _.edit): (user, _) =>
        Ok.page(views.ublog.form.create(user, env.ublog.form.create, anyCaptcha))
  }

  def create(username: UserStr) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      WithBlogOf(username, _.edit): (user, _) =>
        bindForm(env.ublog.form.create)(
          err => BadRequest.page(views.ublog.form.create(user, err, anyCaptcha)),
          data =>
            limit.ublog(me, rateLimited, cost = if me.isVerified then 1 else 3):
              env.ublog.api
                .create(data, user)
                .map: post =>
                  lila.mon.ublog.create(user.id).increment()
                  Redirect(editUrlOfPost(post)).flashSuccess
        )
  }

  def edit(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      FoundPage(env.ublog.api.findEditableByMe(id)): post =>
        views.ublog.form.edit(post, env.ublog.form.edit(post))
      .map(_.hasPersonalData)
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
        env.ublog.api.like(id, v).map(Ok(_))
  }

  def redirect(id: UblogPostId) = Open:
    Found(env.ublog.api.postPreview(id)): post =>
      Redirect(urlOfPost(post))

  def modBlog(blogId: String) = SecureBody(_.ModerateBlog) { ctx ?=> me ?=>
    import UblogBlog.Tier
    def tierStr(tier: Tier) = Tier.name(tier).toUpperCase()
    Found(UblogBlog.Id(blogId).so(env.ublog.api.getBlog)): blog =>
      bindForm(lila.ublog.UblogForm.modBlogForm)(
        _ => Redirect(urlOfBlog(blog)).flashFailure,
        (tier, note) =>
          val tierChange = if blog.tier == tier then none else tier.some
          val noteChange = if blog.modNote.exists(_ == note) then none else note.some
          val log = List(
            tierChange.map(t => tierStr(blog.tier) + " -> " + tierStr(t)),
            noteChange.map(_.take(80))
          ).flatten.mkString(" - ")
          for
            user <- env.user.repo.byId(blog.userId).orFail("Missing blog user!").dmap(Suspect.apply)
            _ <- env.ublog.api.modBlog(blog.userId, tierChange, noteChange, me.some)
            _ <- env.mod.logApi.blogEdit(user, log)
          yield Redirect(urlOfBlog(blog)).flashSuccess
      )
  }

  def modShowCarousel = Secure(_.ModerateBlog) { ctx ?=> me ?=>
    env.ublog.api
      .fetchCarouselFromDb()
      .flatMap: carousel =>
        Ok.page(views.ublog.ui.modShowCarousel(carousel))
  }

  def modPull(postId: UblogPostId) = Secure(_.ModerateBlog) { ctx ?=> me ?=>
    Found(env.ublog.api.getPost(postId)): post =>
      for
        _ <- env.ublog.api.setFeatured(post, ModPostData(featured = false.some))
        _ <- logModAction(post, "pull from carousel")
      yield Redirect(routes.Ublog.modShowCarousel)
  }

  def modPost(postId: UblogPostId) = SecureBody(parse.json)(_.ModerateBlog) { ctx ?=> me ?=>
    Found(env.ublog.api.getPost(postId)): post =>
      ctx.body.body.validate(using ModPostData.reads) match
        case JsError(errors) => fuccess(BadRequest(errors.flatMap(_._2.map(_.message)).mkString(", ")))
        case JsSuccess(data, _) =>
          for
            mod <- env.ublog.api.modPost(post, data)
            featured <- env.ublog.api.setFeatured(post, data)
            carousel <- env.ublog.api.fetchCarouselFromDb()
          yield
            if data.hasUpdates then logModAction(post, data.diff(post))
            Ok.snip(
              views.ublog.post.modTools(
                post.copy(automod = mod.orElse(post.automod), featured = featured.orElse(post.featured)),
                carousel.has(post.id)
              )
            )
  }

  def modAssess(postId: UblogPostId) = Secure(_.ModerateBlog) { ctx ?=> me ?=>
    Found(env.ublog.api.getPost(postId)): post =>
      for
        mod <- env.ublog.api.triggerAutomod(post.copy(automod = none, featured = none))
        _ <- env.ublog.api.setFeatured(post, ModPostData(featured = false.some))
        _ <- logModAction(post, "reassess")
      yield Ok.snip(
        views.ublog.post.modTools(
          post.copy(automod = mod.orElse(post.automod), featured = none),
          isInCarousel = false
        )
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
      Reasonable(page, Max(50)):
        Ok.async:
          env.ublog.paginator.liveByFollowed(me, page).map(views.ublog.ui.friends)
  }

  def communityLang(language: Language, filterOpt: Option[QualityFilter], page: Int = 1) = Open:
    import LangPicker.ByHref
    LangPicker.byHref(language, ctx.req) match
      case ByHref.NotFound => Redirect(routes.Ublog.communityAll(filterOpt, page))
      case ByHref.Redir(language) => Redirect(routes.Ublog.communityLang(language, filterOpt, page))
      case ByHref.Refused(lang) => communityIndex(lang.some, filterOpt, page)
      case ByHref.Found(lang) =>
        if ctx.isAuth then communityIndex(lang.some, filterOpt, page)
        else communityIndex(lang.some, filterOpt, page)(using ctx.withLang(lang))

  def communityAll(filterOpt: Option[QualityFilter], page: Int) = Open:
    communityIndex(none, filterOpt, page)

  private def communityIndex(l: Option[Lang], filterOpt: Option[QualityFilter], page: Int)(using Context) =
    NotForKids:
      Reasonable(page, Max(200)):
        pageHit
        Ok.async:
          val language = l.map(toLanguage)
          val filter = updateFilter(filterOpt)
          env.ublog.paginator
            .liveByCommunity(language, filter, page)
            .map:
              views.ublog.community(language, filter, _)

  def communityAtom(language: Language) = Anon:
    val found: Option[Lang] = LangList.popularNoRegion.find(l => toLanguage(l) == language)
    env.ublog.paginator
      .liveByCommunity(found.map(toLanguage), QualityFilter.best, page = 1)
      .map: posts =>
        Ok.snip(views.ublog.ui.atom.community(language, posts.currentPageResults)).as(XML)

  def liked(page: Int) = Auth { ctx ?=> me ?=>
    NotForKids:
      Reasonable(page, Max(50)):
        Ok.async:
          env.ublog.paginator.liveByLiked(page).map(views.ublog.ui.liked)
  }

  def topics = Open:
    NotForKids:
      Ok.async:
        env.ublog.topic.withPosts.map(views.ublog.ui.topics)

  def topic(str: String, filterOpt: Option[QualityFilter], by: BlogsBy, page: Int) = Open:
    NotForKids:
      Reasonable(page, Max(50)):
        Found(lila.ublog.UblogTopic.fromUrl(str)): top =>
          val filter = updateFilter(filterOpt)
          Ok.async:
            env.ublog.paginator
              .liveByTopic(top, filter, by, page)
              .map:
                views.ublog.ui.topic(top, filter, by, _)

  def thisMonth(filter: Option[QualityFilter], by: BlogsBy, page: Int) =
    val now = nowInstant.date
    byMonth(now.getYear(), now.getMonth().getValue(), filter, by, page)

  def byMonth(year: Int, month: Int, filterOpt: Option[QualityFilter], by: BlogsBy, page: Int) = Open: ctx ?=>
    NotForKids:
      Reasonable(page, Max(50)):
        Found(UblogByMonth.readYearMonth(year, month)): yearMonth =>
          val filter = updateFilter(filterOpt)
          Ok.async:
            env.ublog.paginator
              .liveByMonth(yearMonth, filter, by, page)
              .map(views.ublog.ui.month(yearMonth, filter, by, _))

  def userAtom(username: UserStr) = Anon:
    Found(env.user.repo.enabledById(username)): user =>
      for
        blog <- env.ublog.api.getUserBlog(user)
        posts <- isBlogVisible(user, blog).so(env.ublog.paginator.byUser(user, true, 1))
      yield Ok.snip(views.ublog.ui.atom.user(user, posts.currentPageResults)).as(XML)

  def historicalBlogPost(id: String, @nowarn slug: String) = Open:
    Found(env.ublog.api.getByPrismicId(id)): post =>
      Redirect(routes.Ublog.post(UserName.lichess, post.slug, post.id), MOVED_PERMANENTLY)

  def search(text: String, by: BlogsBy, page: Int) = Open: ctx ?=>
    val queryText = text.take(100).trim
    NotForKids:
      for
        ids <- env.ublog.search.fetchResults(queryText, by, Quality.weak.some, page)
        posts <- ids.mapFutureList(env.ublog.api.postPreviews)
        page <- renderPage(views.ublog.ui.search(queryText, by, posts.some))
      yield Ok(page)

  private def isBlogVisible(user: UserModel, blog: UblogBlog) = user.enabled.yes && blog.visible

  private def NotForKidsUnlessOfficial(username: UserStr)(f: => Fu[Result])(using Context): Fu[Result] =
    if username.is(UserId.lichess) then f else NotForKids(f)

  private def canViewBlogOf(user: UserModel, blog: UblogBlog)(using ctx: Context) =
    ctx.is(user) || isGrantedOpt(_.ModerateBlog) || isBlogVisible(user, blog)

  private def updateFilter(filterOpt: Option[QualityFilter])(using ctx: Context): QualityFilter =
    for
      filter <- filterOpt
      if filter != ctx.pref.blogFilter
      me <- ctx.me
    do env.pref.api.setPref(me, _.copy(blogFilter = filter))
    filterOpt | ctx.pref.blogFilter
