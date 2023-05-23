package controllers

import io.prismic.Document
import org.apache.commons.lang3.StringUtils
import play.api.mvc.*

import lila.api.Context
import lila.app.{ given, * }
import lila.blog.BlogApi
import lila.common.config.MaxPerPage

final class Blog(
    env: Env,
    prismicC: Prismic
)(using play.api.libs.ws.StandaloneWSClient)
    extends LilaController(env):

  import prismicC.*

  private def blogApi = env.blog.api

  def index(page: Int) =
    WithPrismic { _ ?=> prismic ?=>
      pageHit
      blogApi.recent(prismic, page, MaxPerPage(12)) flatMap {
        case Some(response) => fuccess(Ok(views.html.blog.index(response)))
        case _              => notFound
      }
    }

  def show(id: String, slug: String, ref: Option[String]) =
    WithPrismic { _ ?=> prismic ?=>
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
    WithPrismic { _ ?=> prismic ?=>
      prismic.api.previewSession(token, prismic.linkResolver, routes.Lobby.home.url) map { redirectUrl =>
        Redirect(redirectUrl).withCookies:
          Cookie(
            io.prismic.Prismic.previewCookie,
            token,
            path = "/",
            maxAge = Some(30 * 60 * 1000),
            httpOnly = false
          )
      }
    }

  import lila.memo.CacheApi.*
  private val atomCache = env.memo.cacheApi.unit[String]:
    _.refreshAfterWrite(30.minutes)
      .buildAsyncFuture { _ =>
        blogApi.masterContext flatMap { prismic =>
          blogApi.recent(prismic.api, 1, MaxPerPage(50), none) mapz { docs =>
            views.html.blog.atom(docs)(using prismic).render
          }
        }
      }

  def atom = Anon:
    atomCache.getUnit.map: xml =>
      Ok(xml) as XML

  private val sitemapCache = env.memo.cacheApi.unit[String] {
    _.refreshAfterWrite(3.hours)
      .buildAsyncFuture { _ =>
        blogApi.masterContext flatMap { prismic =>
          blogApi.all()(using prismic) map {
            _.map { doc =>
              s"${env.net.baseUrl}${routes.Blog.show(doc.id, doc.slug)}"
            } mkString "\n"
          }
        }
      }
  }

  def sitemapTxt = Anon:
    sitemapCache.getUnit.map: txt =>
      Ok(txt) as TEXT

  def all =
    WithPrismic { _ ?=> prismic ?=>
      blogApi.byYear(prismic, lila.blog.thisYear) map { posts =>
        Ok(views.html.blog.index.byYear(lila.blog.thisYear, posts))
      }
    }

  def year(year: Int) =
    WithPrismic { _ ?=> prismic ?=>
      if (lila.blog.allYears contains year)
        blogApi.byYear(prismic, year) map { posts =>
          Ok(views.html.blog.index.byYear(year, posts))
        }
      else notFound
    }

  def discuss(id: String) =
    WithPrismic { _ ?=> prismic ?=>
      val categId   = ForumCategId("general-chess-discussion")
      val topicSlug = s"blog-$id"
      val redirect  = Redirect(routes.ForumTopic.show(categId.value, topicSlug))
      env.forum.topicRepo.existsByTree(categId, topicSlug) flatMap {
        if _ then fuccess(redirect)
        else
          blogApi.one(prismic.api, none, id) flatMapz { doc =>
            env.forum.categRepo.byId(categId) flatMapz { categ =>
              env.forum.topicApi.makeBlogDiscuss(
                categ = categ,
                slug = topicSlug,
                name = doc.getText("blog.title") | "New blog post",
                url = s"${env.net.baseUrl}${routes.Blog.show(doc.id, doc.slug)}"
              )
            } inject redirect
          }
      }
    }

  private def WithPrismic(f: Context ?=> BlogApi.Context ?=> Fu[Result]) = Open:
    blogApi context ctx.req flatMap { f(using ctx)(using _) }

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  private def checkSlug(document: Option[Document], slug: String)(
      callback: Either[String, Document] => Result
  )(using lila.api.Context) =
    document.collect {
      case document if document.slug == slug => fuccess(callback(Right(document)))
      case document
          if document.slugs
            .exists(s => StringUtils.stripEnd(s, ".") == slug || s == StringUtils.stripEnd(slug, ".")) =>
        fuccess(callback(Left(document.slug)))
    } getOrElse notFound
