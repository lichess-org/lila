package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.blog.{ BlogApi, BlogLang }
import lila.common.config.MaxPerPage

final class Blog(
    env: Env,
    prismicC: Prismic
)(implicit ws: play.api.libs.ws.WSClient)
    extends LilaController(env) {

  import prismicC._

  private def blogApi = env.blog.api

  def index(page: Int) =
    WithPrismic { implicit ctx => implicit prismic =>
      pageHit
      blogApi.recent(prismic, page, MaxPerPage(12), BlogLang.fromLang(ctx.lang)) flatMap {
        case Some(response) => fuccess(Ok(views.html.blog.index(response)))
        case _              => notFound
      }
    }

  def show(id: String, @scala.annotation.unused ref: Option[String]) =
    WithPrismic { implicit ctx => implicit prismic =>
      pageHit
      blogApi.one(prismic, id) flatMap {
        case Some(post) if (post.isJapanese && ctx.isAnon && ctx.req.session.get("lang").isEmpty) => {
          val langCtx = ctx withLang lila.i18n.I18nLangPicker.byStr("ja-JP").getOrElse(ctx.lang)
          fuccess(Ok(views.html.blog.show(post)(langCtx, prismic)))
        }
        case Some(post) => fuccess(Ok(views.html.blog.show(post)))
        case _          => notFound
      }
    }

  def showBc(id: String, slug: String, ref: Option[String]) =
    WithPrismic { implicit ctx => implicit prismic =>
      blogApi.one(prismic, id) flatMap {
        case Some(post) if post.doc.slugs.contains(slug) =>
          fuccess(MovedPermanently(routes.Blog.show(post.id, ref).url))
        case _ => notFound
      }
    }

  def latest(ref: Option[String]) =
    WithPrismic { implicit ctx => implicit prismic =>
      blogApi.latest(prismic, BlogLang.fromLang(ctx.lang)) flatMap {
        case Some(post) =>
          fuccess(Redirect(routes.Blog.show(post.id, ref)))
        case _ => notFound
      }
    }

  def preview(token: String) =
    WithPrismic { _ => implicit prismic =>
      prismic.api.previewSession(token, prismic.linkResolver, routes.Lobby.home.url) map { redirectUrl =>
        Redirect(redirectUrl)
          .withCookies(
            Cookie(
              io.prismic.Prismic.previewCookie,
              token,
              path = "/",
              maxAge = Some(30 * 60 * 1000),
              httpOnly = false
            )
          )
      }
    }

  import scala.concurrent.duration._
  import lila.memo.CacheApi._
  private val atomCache = env.memo.cacheApi.unit[String] {
    _.refreshAfterWrite(60.minutes)
      .buildAsyncFuture { _ =>
        blogApi.masterContext flatMap { implicit prismic =>
          blogApi.recent(prismic.api, 1, MaxPerPage(50), BlogLang.default, none) map {
            _ ?? { docs =>
              views.html.blog.atom(docs, env.net.baseUrl).render
            }
          }
        }
      }
  }

  def atom =
    Action.async {
      atomCache.getUnit map { xml =>
        Ok(xml) as XML
      }
    }

  private val sitemapCache = env.memo.cacheApi.unit[String] {
    _.refreshAfterWrite(6.hours)
      .buildAsyncFuture { _ =>
        blogApi.masterContext flatMap { implicit prismic =>
          blogApi.all() map {
            _.map { doc =>
              s"${env.net.baseUrl}${routes.Blog.show(doc.id)}"
            } mkString "\n"
          }
        }
      }
  }

  def sitemapTxt =
    Action.async {
      sitemapCache.getUnit map { txt =>
        Ok(txt) as TEXT
      }
    }

  def all =
    WithPrismic { implicit ctx => implicit prismic =>
      blogApi.byYear(prismic, lila.blog.thisYear, BlogLang.fromLang(ctx.lang)) map { posts =>
        Ok(views.html.blog.index.byYear(lila.blog.thisYear, posts))
      }
    }

  def year(year: Int) =
    WithPrismic { implicit ctx => implicit prismic =>
      if (lila.blog.allYears contains year)
        blogApi.byYear(prismic, year, BlogLang.fromLang(ctx.lang)) map { posts =>
          Ok(views.html.blog.index.byYear(year, posts))
        }
      else notFound
    }

  def discuss(id: String) =
    WithPrismic { _ => implicit prismic =>
      val categSlug = "general-shogi-discussion"
      val topicSlug = s"blog-$id"
      val redirect  = Redirect(routes.ForumTopic.show(categSlug, topicSlug))
      env.forum.topicRepo.existsByTree(categSlug, topicSlug) flatMap {
        case true => fuccess(redirect)
        case _ =>
          blogApi.one(prismic.api, none, id) flatMap {
            _ ?? { post =>
              env.forum.categRepo.bySlug(categSlug) flatMap {
                _ ?? { categ =>
                  env.forum.topicApi.makeBlogDiscuss(
                    categ = categ,
                    slug = topicSlug,
                    name = post.title,
                    url = s"${env.net.baseUrl}${routes.Blog.show(post.id)}"
                  )
                }
              } inject redirect
            }
          }
      }
    }

  private def WithPrismic(f: Context => BlogApi.Context => Fu[Result]): Action[Unit] =
    Open { ctx =>
      blogApi context ctx.req flatMap { prismic =>
        f(ctx)(prismic)
      }
    }

}
