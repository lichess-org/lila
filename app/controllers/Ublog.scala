package controllers

import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.ublog.{ UblogBlog, UblogPost }
import lila.user.{ User => UserModel }
import play.api.i18n.Lang
import lila.i18n.LangList
import lila.report.Suspect
import lila.common.config

final class Ublog(env: Env) extends LilaController(env) {

  import views.html.ublog.post.{ editUrlOfPost, urlOfPost }
  import views.html.ublog.blog.urlOfBlog
  import lila.common.paginator.Paginator.zero

  def index(username: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      OptionFuResult(env.user.repo named username) { user =>
        env.ublog.api.getUserBlog(user) flatMap { blog =>
          (canViewBlogOf(user, blog) ?? env.ublog.paginator.byUser(user, true, page)) map { posts =>
            Ok(html.ublog.blog(user, blog, posts))
          }
        }
      }
    }
  }

  def drafts(username: String, page: Int) = Auth { implicit ctx => me =>
    NotForKids {
      if (!me.is(username)) Redirect(routes.Ublog.drafts(me.username)).fuccess
      else
        env.ublog.paginator.byUser(me, false, page) map { posts =>
          Ok(html.ublog.index.drafts(me, posts))
        }
    }
  }

  def post(username: String, slug: String, id: String) = Open { implicit ctx =>
    NotForKids {
      OptionFuResult(env.user.repo named username) { user =>
        env.ublog.api.getUserBlog(user) flatMap { blog =>
          env.ublog.api.findByIdAndBlog(UblogPost.Id(id), blog.id) flatMap {
            _.filter(canViewPost(user, blog)).fold(notFound) { post =>
              if (slug != post.slug) Redirect(urlOfPost(post)).fuccess
              else {
                env.ublog.api.otherPosts(UblogBlog.Id.User(user.id), post) zip
                  ctx.me.??(env.ublog.rank.liked(post)) zip
                  ctx.userId.??(env.relation.api.fetchFollows(_, user.id)) zip
                  env.ublog.markup(post) map { case (((others, liked), followed), markup) =>
                    val viewedPost = env.ublog.viewCounter(post, ctx.ip)
                    Ok(html.ublog.post(user, blog, viewedPost, markup, others, liked, followed))
                  }
              }
            }
          }
        }
      }
    }
  }

  def discuss(id: String) = Open { implicit ctx =>
    NotForKids {
      import lila.forum.Categ.ublogSlug
      val topicSlug = s"ublog-${id}"
      val redirect  = Redirect(routes.ForumTopic.show(ublogSlug, topicSlug))
      env.forum.topicRepo.existsByTree(ublogSlug, topicSlug) flatMap {
        case true => fuccess(redirect)
        case _ =>
          env.ublog.api.getPost(UblogPost.Id(id)) flatMap {
            _ ?? { post =>
              env.forum.topicApi.makeUblogDiscuss(
                slug = topicSlug,
                name = post.title,
                url = s"${env.net.baseUrl}${routes.Ublog.post(post.created.by, post.slug, id)}",
                ublogId = id,
                authorId = post.created.by
              )
            } inject redirect
          }
      }
    }
  }

  def form(username: String) = Auth { implicit ctx => me =>
    NotForKids {
      if (env.ublog.api.canBlog(me)) {
        if (!me.is(username)) Redirect(routes.Ublog.form(me.username)).fuccess
        else
          env.ublog.form.anyCaptcha map { captcha =>
            Ok(html.ublog.form.create(me, env.ublog.form.create, captcha))
          }
      } else
        Unauthorized(
          html.site.message.notYet(
            "Please play a few games and wait 2 days before you can create blog posts."
          )
        ).fuccess
    }
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[UserModel.ID](
    credits = 5 * 3,
    duration = 24.hour,
    key = "ublog.create.user"
  )

  def create = AuthBody { implicit ctx => me =>
    NotForKids {
      env.ublog.form.create
        .bindFromRequest()(ctx.body, formBinding)
        .fold(
          err =>
            env.ublog.form.anyCaptcha map { captcha =>
              BadRequest(html.ublog.form.create(me, err, captcha))
            },
          data =>
            CreateLimitPerUser(me.id, cost = if (me.isVerified) 1 else 3) {
              env.ublog.api.create(data, me) map { post =>
                lila.mon.ublog.create(me.id).increment()
                Redirect(editUrlOfPost(post)).flashSuccess
              }
            }(rateLimitedFu)
        )
    }
  }

  def edit(id: String) = AuthBody { implicit ctx => me =>
    NotForKids {
      OptionOk(env.ublog.api.findByUserBlogOrAdmin(UblogPost.Id(id), me)) { post =>
        html.ublog.form.edit(post, env.ublog.form.edit(post))
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    NotForKids {
      env.ublog.api.findByUserBlogOrAdmin(UblogPost.Id(id), me) flatMap {
        _ ?? { prev =>
          env.ublog.form
            .edit(prev)
            .bindFromRequest()(ctx.body, formBinding)
            .fold(
              err => BadRequest(html.ublog.form.edit(prev, err)).fuccess,
              data =>
                env.ublog.api.update(data, prev, me) flatMap { post =>
                  logModAction(post, "edit") inject
                    Redirect(urlOfPost(post)).flashSuccess
                }
            )
        }
      }
    }
  }

  def delete(id: String) = AuthBody { implicit ctx => me =>
    env.ublog.api.findByUserBlogOrAdmin(UblogPost.Id(id), me) flatMap {
      _ ?? { post =>
        env.ublog.api.delete(post) >>
          logModAction(post, "delete") inject
          Redirect(urlOfBlog(post.blog)).flashSuccess
      }
    }
  }

  private def logModAction(post: UblogPost, action: String)(implicit ctx: Context): Funit =
    isGranted(_.ModerateBlog) ?? ctx.me ?? { me =>
      !me.is(post.created.by) ?? {
        env.user.repo.byId(post.created.by) flatMap {
          _ ?? { user =>
            env.mod.logApi.blogPostEdit(lila.report.Mod(me), Suspect(user), post.id.value, post.title, action)
          }
        }
      }
    }

  def like(id: String, v: Boolean) = Auth { implicit ctx => me =>
    NoBot {
      NotForKids {
        env.ublog.rank.like(UblogPost.Id(id), me, v) map { likes =>
          Ok(likes.value)
        }
      }
    }
  }

  def redirect(id: String) = Open { implicit ctx =>
    env.ublog.api.postPreview(UblogPost.Id(id)) flatMap {
      _.fold(notFound) { post =>
        Redirect(urlOfPost(post)).fuccess
      }
    }
  }

  def setTier(blogId: String) = SecureBody(_.ModerateBlog) { implicit ctx => me =>
    UblogBlog.Id(blogId).??(env.ublog.api.getBlog) flatMap {
      _ ?? { blog =>
        implicit val body = ctx.body
        lila.ublog.UblogForm.tier
          .bindFromRequest()
          .fold(
            err => Redirect(urlOfBlog(blog)).flashFailure.fuccess,
            tier =>
              for {
                user <- env.user.repo.byId(blog.userId) orFail "Missing blog user!" dmap Suspect
                _    <- env.ublog.api.setTier(blog.id, tier)
                _    <- env.ublog.rank.recomputeRankOfAllPostsOfBlog(blog.id)
                _ <- env.mod.logApi
                  .blogTier(lila.report.Mod(me.user), user, blog.id.full, UblogBlog.Tier.name(tier))
              } yield Redirect(urlOfBlog(blog)).flashSuccess
          )
      }
    }
  }

  private val ImageRateLimitPerIp = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "ublog.image.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 60, 1.day)
  )

  def image(id: String) =
    AuthBody(parse.multipartFormData) { implicit ctx => me =>
      env.ublog.api.findByUserBlogOrAdmin(UblogPost.Id(id), me) flatMap {
        _ ?? { post =>
          ctx.body.body.file("image") match {
            case Some(image) =>
              ImageRateLimitPerIp(ctx.ip) {
                env.ublog.api.uploadImage(me, post, image) map { newPost =>
                  Ok(html.ublog.form.formImage(newPost))
                } recover { case e: Exception =>
                  BadRequest(e.getMessage)
                }
              }(rateLimitedFu)
            case None =>
              env.ublog.api.deleteImage(post) flatMap { newPost =>
                logModAction(newPost, "delete image") inject
                  Ok(html.ublog.form.formImage(newPost))
              }
          }
        }
      }
    }

  def friends(page: Int) = Auth { implicit ctx => me =>
    NotForKids {
      Reasonable(page, config.Max(10)) {
        env.ublog.paginator.liveByFollowed(me, page) map { posts =>
          Ok(html.ublog.index.friends(posts))
        }
      }
    }
  }

  def community(code: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      val l = Lang.get(code).filter(LangList.popularNoRegion.contains)
      Reasonable(page, config.Max(8)) {
        env.ublog.paginator.liveByCommunity(l, page) map { posts =>
          Ok(html.ublog.index.community(l, posts))
        }
      }
    }
  }

  def communityAtom(code: String) = Action.async { implicit req =>
    val lang = Lang.get(code).filter(LangList.popularNoRegion.contains)
    env.ublog.paginator.liveByCommunity(lang, page = 1) map { posts =>
      Ok(html.ublog.atom.community(code, posts.currentPageResults)) as XML
    }
  }

  def liked(page: Int) = Auth { implicit ctx => me =>
    NotForKids {
      Reasonable(page, config.Max(15)) {
        ctx.me ?? { me =>
          env.ublog.paginator.liveByLiked(me, page) map { posts =>
            Ok(html.ublog.index.liked(posts))
          }
        }
      }
    }
  }

  def topics = Open { implicit ctx =>
    NotForKids {
      env.ublog.topic.withPosts map { topics =>
        Ok(html.ublog.index.topics(topics))
      }
    }
  }

  def topic(str: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      Reasonable(page, config.Max(5)) {
        lila.ublog.UblogTopic.fromUrl(str) ?? { top =>
          env.ublog.paginator.liveByTopic(top, page) map { posts =>
            Ok(html.ublog.index.topic(top, posts))
          }
        }
      }
    }
  }

  def userAtom(username: String) = Action.async { implicit req =>
    env.user.repo.enabledNamed(username) flatMap {
      case None => NotFound.fuccess
      case Some(user) =>
        implicit val lang = reqLang
        env.ublog.api.getUserBlog(user) flatMap { blog =>
          (isBlogVisible(user, blog) ?? env.ublog.paginator.byUser(user, true, 1)) map { posts =>
            Ok(html.ublog.atom.user(user, blog, posts.currentPageResults)) as XML
          }
        }
    }
  }

  private def isBlogVisible(user: UserModel, blog: UblogBlog) = user.enabled && blog.visible

  private def canViewBlogOf(user: UserModel, blog: UblogBlog)(implicit ctx: Context) =
    ctx.is(user) || isGranted(_.ModerateBlog) || isBlogVisible(user, blog)

  private def canViewPost(user: UserModel, blog: UblogBlog)(post: UblogPost)(implicit ctx: Context) =
    canViewBlogOf(user, blog) && (ctx.is(user) || post.live)
}
