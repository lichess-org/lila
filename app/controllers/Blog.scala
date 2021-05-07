package controllers

import io.prismic.Document
import org.apache.commons.lang3.StringUtils
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.blog.BlogApi
import lila.common.config.MaxPerPage

final class Blog(
    env: Env,
    prismicC: Prismic
)(implicit ws: play.api.libs.ws.StandaloneWSClient)
    extends LilaController(env) {

  import prismicC._

  private def blogApi = env.blog.api

  def index(page: Int) =
    WithPrismic { implicit ctx => implicit prismic =>
      pageHit
      blogApi.recent(prismic, page, MaxPerPage(12)) flatMap {
        case Some(response) => fuccess(Ok(views.html.blog.index(response)))
        case _              => notFound
      }
    }

  def show(id: String, slug: String, ref: Option[String]) =
    WithPrismic { implicit ctx => implicit prismic =>
      pageHit
      blogApi.one(prismic, id) flatMap { maybeDocument =>
        checkSlug(maybeDocument, slug) {
          case Left(newSlug) => MovedPermanently(routes.Blog.show(id, newSlug, ref).url)
          case Right(doc)    => Ok(views.html.blog.show(doc))
        }
      } recoverWith {
        case e: RuntimeException if e.getMessage contains "Not Found" => notFound
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
    _.refreshAfterWrite(30.minutes)
      .buildAsyncFuture { _ =>
        blogApi.masterContext flatMap { implicit prismic =>
          blogApi.recent(prismic.api, 1, MaxPerPage(50), none) map {
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
    _.refreshAfterWrite(3.hours)
      .buildAsyncFuture { _ =>
        blogApi.masterContext flatMap { implicit prismic =>
          blogApi.all() map {
            _.map { doc =>
              s"${env.net.baseUrl}${routes.Blog.show(doc.id, doc.slug)}"
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
      blogApi.byYear(prismic, lila.blog.thisYear) map { posts =>
        Ok(views.html.blog.index.byYear(lila.blog.thisYear, posts))
      }
    }

  def year(year: Int) =
    WithPrismic { implicit ctx => implicit prismic =>
      if (lila.blog.allYears contains year)
        blogApi.byYear(prismic, year) map { posts =>
          Ok(views.html.blog.index.byYear(year, posts))
        }
      else notFound
    }

  def discuss(id: String) =
    WithPrismic { _ => implicit prismic =>
      val categSlug = "general-chess-discussion"
      val topicSlug = s"blog-$id"
      val redirect  = Redirect(routes.ForumTopic.show(categSlug, topicSlug))
      env.forum.topicRepo.existsByTree(categSlug, topicSlug) flatMap {
        case true => fuccess(redirect)
        case _ =>
          blogApi.one(prismic.api, none, id) flatMap {
            _ ?? { doc =>
              env.forum.categRepo.bySlug(categSlug) flatMap {
                _ ?? { categ =>
                  env.forum.topicApi.makeBlogDiscuss(
                    categ = categ,
                    slug = topicSlug,
                    name = doc.getText("blog.title") | "New blog post",
                    url = s"${env.net.baseUrl}${routes.Blog.show(doc.id, doc.slug)}"
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

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  private def checkSlug(document: Option[Document], slug: String)(
      callback: Either[String, Document] => Result
  )(implicit ctx: lila.api.Context) =
    document.collect {
      case document if document.slug == slug => fuccess(callback(Right(document)))
      case document if document.slugs.exists(StringUtils.stripEnd(_, ".") == slug) =>
        fuccess(callback(Left(document.slug)))
    } getOrElse notFound
}
