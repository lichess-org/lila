package controllers

import play.api.i18n.Lang
import views.*

import lila.app.{ given, * }
import lila.common.config
import lila.i18n.{ I18nLangPicker, LangList }
import lila.report.Suspect
import lila.ublog.{ UblogBlog, UblogPost }
import lila.user.{ User as UserModel }

final class Ublog(env: Env) extends LilaController(env):

  import views.html.ublog.post.{ editUrlOfPost, urlOfPost }
  import views.html.ublog.blog.urlOfBlog
  import lila.common.paginator.Paginator.given

  def index(username: UserStr, page: Int) = Open:
    NotForKids:
      val userFu = if username == UserStr("me") then fuccess(ctx.user) else env.user.repo.byId(username)
      FoundPage(userFu): user =>
        env.ublog.api
          .getUserBlog(user)
          .flatMap: blog =>
            (canViewBlogOf(user, blog) so env.ublog.paginator.byUser(user, true, page)) map {
              html.ublog.blog(user, blog, _)
            }

  def drafts(username: UserStr, page: Int) = Auth { ctx ?=> me ?=>
    NotForKids:
      if !me.is(username) then Redirect(routes.Ublog.drafts(me.username))
      else
        Ok.pageAsync:
          env.ublog.paginator.byUser(me, false, page) map {
            html.ublog.index.drafts(me, _)
          }
  }

  def post(username: UserStr, slug: String, id: UblogPostId) = Open:
    NotForKids:
      Found(env.user.repo byId username): user =>
        env.ublog.api
          .getUserBlog(user)
          .flatMap: blog =>
            env.ublog.api.findByIdAndBlog(id, blog.id) flatMap {
              _.filter(canViewPost(user, blog)).fold(notFound): post =>
                if slug != post.slug then Redirect(urlOfPost(post))
                else
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
                      html.ublog.post(user, blog, viewedPost, markup, others, liked, followable, followed)
                  yield Ok(page)
            }

  def discuss(id: UblogPostId) = Open:
    NotForKids:
      import lila.forum.ForumCateg.ublogId
      val topicSlug = s"ublog-${id}"
      val redirect  = Redirect(routes.ForumTopic.show(ublogId.value, topicSlug))
      env.forum.topicRepo.existsByTree(ublogId, topicSlug) flatMap {
        if _ then redirect
        else
          env.ublog.api.getPost(id) flatMapz { post =>
            env.forum.topicApi.makeUblogDiscuss(
              slug = topicSlug,
              name = post.title,
              url = s"${env.net.baseUrl}${routes.Ublog.post(post.created.by, post.slug, id)}",
              ublogId = id,
              authorId = post.created.by
            )
          } inject redirect
      }

  def form(username: UserStr) = Auth { ctx ?=> me ?=>
    NotForKids:
      if env.ublog.api.canBlog(me) then
        if !me.is(username)
        then Redirect(routes.Ublog.form(me.username))
        else
          Ok.pageAsync:
            env.ublog.form.anyCaptcha.map:
              html.ublog.form.create(me, env.ublog.form.create, _)
      else
        Unauthorized.page:
          html.site.message.notYet:
            "Please play a few games and wait 2 days before you can create blog posts."
  }

  private val CreateLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 5 * 3,
    duration = 24.hour,
    key = "ublog.create.user"
  )

  def create = AuthBody { ctx ?=> me ?=>
    NotForKids:
      env.ublog.api.canBlog(me) so {
        env.ublog.form.create
          .bindFromRequest()
          .fold(
            err =>
              BadRequest.pageAsync:
                env.ublog.form.anyCaptcha.map:
                  html.ublog.form.create(me, err, _)
            ,
            data =>
              CreateLimitPerUser(me, rateLimited, cost = if me.isVerified then 1 else 3):
                env.ublog.api.create(data) map { post =>
                  lila.mon.ublog.create(me.userId.value).increment()
                  Redirect(editUrlOfPost(post)).flashSuccess
                }
          )
      }
  }

  def edit(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      FoundPage(env.ublog.api.findByUserBlogOrAdmin(id)): post =>
        html.ublog.form.edit(post, env.ublog.form.edit(post))
  }

  def update(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    NotForKids:
      Found(env.ublog.api.findByUserBlogOrAdmin(id)): prev =>
        env.ublog.form
          .edit(prev)
          .bindFromRequest()
          .fold(
            err => BadRequest.page(html.ublog.form.edit(prev, err)),
            data =>
              env.ublog.api.update(data, prev) flatMap { post =>
                logModAction(post, "edit") inject
                  Redirect(urlOfPost(post)).flashSuccess
              }
          )

  }

  def delete(id: UblogPostId) = AuthBody { ctx ?=> me ?=>
    Found(env.ublog.api.findByUserBlogOrAdmin(id)): post =>
      env.ublog.api.delete(post) >>
        logModAction(post, "delete") inject
        Redirect(urlOfBlog(post.blog)).flashSuccess

  }

  private def logModAction(post: UblogPost, action: String)(using ctx: Context, me: Me): Funit =
    isGrantedOpt(_.ModerateBlog).so:
      !me.is(post.created.by) so {
        env.user.repo.byId(post.created.by) flatMapz { user =>
          env.mod.logApi.blogPostEdit(Suspect(user), post.id, post.title, action)
        }
      }

  def like(id: UblogPostId, v: Boolean) = Auth { ctx ?=> _ ?=>
    NoBot:
      NotForKids:
        env.ublog.rank.like(id, v) map { likes =>
          Ok(likes.value)
        }
  }

  def redirect(id: UblogPostId) = Open:
    Found(env.ublog.api.postPreview(id)): post =>
      Redirect(urlOfPost(post))

  def setTier(blogId: String) = SecureBody(_.ModerateBlog) { ctx ?=> me ?=>
    Found(UblogBlog.Id(blogId).so(env.ublog.api.getBlog)): blog =>
      lila.ublog.UblogForm.tier
        .bindFromRequest()
        .fold(
          _ => Redirect(urlOfBlog(blog)).flashFailure,
          tier =>
            for
              user <- env.user.repo.byId(blog.userId) orFail "Missing blog user!" dmap Suspect.apply
              _    <- env.ublog.api.setTier(blog.id, tier)
              _    <- env.ublog.rank.recomputeRankOfAllPostsOfBlog(blog.id)
              _    <- env.mod.logApi.blogTier(user, UblogBlog.Tier.name(tier))
            yield Redirect(urlOfBlog(blog)).flashSuccess
        )
  }

  private val ImageRateLimitPerIp = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "ublog.image.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 60, 1.day)
  )

  def image(id: UblogPostId) = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    Found(env.ublog.api.findByUserBlogOrAdmin(id)): post =>
      ctx.body.body.file("image") match
        case Some(image) =>
          ImageRateLimitPerIp(ctx.ip, rateLimited):
            env.ublog.api.uploadImage(me, post, image) map { newPost =>
              Ok(html.ublog.form.formImage(newPost))
            } recover { case e: Exception =>
              BadRequest(e.getMessage)
            }
        case None =>
          env.ublog.api.deleteImage(post) flatMap { newPost =>
            logModAction(newPost, "delete image") inject
              Ok(html.ublog.form.formImage(newPost))
          }
  }

  def friends(page: Int) = Auth { _ ?=> me ?=>
    NotForKids:
      Reasonable(page, config.Max(100)):
        Ok.pageAsync:
          env.ublog.paginator.liveByFollowed(me, page) map html.ublog.index.friends
  }

  def communityLang(language: String, page: Int = 1) = Open:
    import I18nLangPicker.ByHref
    I18nLangPicker.byHref(language, ctx.req) match
      case ByHref.NotFound      => Redirect(routes.Ublog.communityAll(page))
      case ByHref.Redir(code)   => Redirect(routes.Ublog.communityLang(code, page))
      case ByHref.Refused(lang) => communityIndex(lang.some, page)
      case ByHref.Found(lang) =>
        if ctx.isAuth then communityIndex(lang.some, page)
        else communityIndex(lang.some, page)(using ctx.withLang(lang))

  def communityAll(page: Int) = Open:
    communityIndex(none, page)

  def communityIndex(l: Option[Lang], page: Int)(using ctx: Context) =
    NotForKids:
      Reasonable(page, config.Max(100)):
        pageHit
        Ok.pageAsync:
          env.ublog.paginator.liveByCommunity(l, page) map {
            html.ublog.index.community(l, _)
          }

  def communityLangBC(code: String) = Anon:
    val l = LangList.popularNoRegion.find(_.code == code)
    Redirect:
      l.fold(routes.Ublog.communityAll())(l => routes.Ublog.communityLang(l.language))

  def communityAtom(language: String) = Anon:
    val l = LangList.popularNoRegion.find(l => l.language == language || l.code == language)
    env.ublog.paginator
      .liveByCommunity(l, page = 1)
      .map: posts =>
        Ok(html.ublog.atom.community(language, posts.currentPageResults)) as XML

  def liked(page: Int) = Auth { ctx ?=> me ?=>
    NotForKids:
      Reasonable(page, config.Max(100)):
        Ok.pageAsync:
          env.ublog.paginator.liveByLiked(page) map {
            html.ublog.index.liked(_)
          }
  }

  def topics = Open:
    NotForKids:
      Ok.pageAsync:
        env.ublog.topic.withPosts.map:
          html.ublog.index.topics(_)

  def topic(str: String, page: Int, byDate: Boolean) = Open:
    NotForKids:
      Reasonable(page, config.Max(100)):
        lila.ublog.UblogTopic.fromUrl(str) so { top =>
          Ok.pageAsync:
            env.ublog.paginator.liveByTopic(top, page, byDate) map {
              html.ublog.index.topic(top, _, byDate)
            }
        }

  def userAtom(username: UserStr) = Anon:
    env.user.repo
      .enabledById(username)
      .flatMap:
        case None => NotFound
        case Some(user) =>
          env.ublog.api
            .getUserBlog(user)
            .flatMap: blog =>
              (isBlogVisible(user, blog) so env.ublog.paginator.byUser(user, true, 1)) map { posts =>
                Ok(html.ublog.atom.user(user, posts.currentPageResults)) as XML
              }

  private def isBlogVisible(user: UserModel, blog: UblogBlog) = user.enabled.yes && blog.visible

  private def canViewBlogOf(user: UserModel, blog: UblogBlog)(using ctx: Context) =
    ctx.is(user) || isGrantedOpt(_.ModerateBlog) || isBlogVisible(user, blog)

  private def canViewPost(user: UserModel, blog: UblogBlog)(post: UblogPost)(using ctx: Context) =
    canViewBlogOf(user, blog) && (ctx.is(user) || post.live)
