package controllers

import io.prismic.Document
import org.apache.commons.lang3.StringUtils
import play.api.mvc.*

import lila.app.{ given, * }
import lila.common.config.MaxPerPage
import lila.blog.{ BlogPost, MiniPost, BlogApi }
import lila.common.paginator.Paginator

final class Blog(
    env: Env,
    prismicC: Prismic
)(using play.api.libs.ws.StandaloneWSClient)
    extends LilaController(env):

  import prismicC.{ given, * }

  private def blogApi = env.blog.api

  private def pagerForMe(pager: Paginator[BlogPost])(using ctx: Context): Paginator[BlogPost] =
    if ctx.kid.yes
    then pager.mapList(_.filter(_.forKids))
    else pager
  private def filterForMe(posts: List[MiniPost])(using ctx: Context): List[MiniPost] =
    posts.filter(_.forKids || ctx.kid.no)

  def index(page: Int) =
    WithPrismic { _ ?=> prismic ?=>
      pageHit
      Found(blogApi.recent(prismic, page, MaxPerPage(12))): response =>
        Ok.page(views.html.blog.index(pagerForMe(response)))
    }

  def show(id: String, slug: String, ref: Option[String]) =
    WithPrismic { _ ?=> prismic ?=>
      pageHit
      blogApi
        .one(prismic, id)
        .flatMap: maybeDocument =>
          checkSlug(maybeDocument, slug):
            case Left(newSlug) => MovedPermanently(routes.Blog.show(id, newSlug, ref).url)
            case Right(doc) =>
              if !doc.forKids && ctx.kid.yes
              then notFound
              else Ok.page(views.html.blog.show(doc))
        .recoverWith:
          case e: RuntimeException if e.getMessage contains "Not Found" => notFound
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
      .buildAsyncFuture: _ =>
        blogApi.masterContext.flatMap: prismic =>
          blogApi.recent(prismic.api, 1, MaxPerPage(50), none) mapz { docs =>
            views.html.blog.atom(docs)(using prismic).render
          }

  def atom = Anon:
    atomCache.getUnit.map: xml =>
      Ok(xml) as XML

  private val sitemapCache = env.memo.cacheApi.unit[String]:
    _.refreshAfterWrite(3.hours).buildAsyncFuture: _ =>
      blogApi.masterContext.flatMap: prismic =>
        blogApi.all()(using prismic).map {
          _.map: doc =>
            s"${env.net.baseUrl}${routes.Blog.show(doc.id, doc.slug)}"
          .mkString("\n")
        }

  def sitemapTxt = Anon:
    sitemapCache.getUnit.map: txt =>
      Ok(txt) as TEXT

  def year(year: Int) =
    WithPrismic { _ ?=> prismic ?=>
      if lila.blog.allYears contains year then
        Ok.pageAsync:
          blogApi.byYear(prismic, year).map(filterForMe) map {
            views.html.blog.index.byYear(year, _)
          }
      else notFound
    }

  def discuss(id: String) =
    WithPrismic { _ ?=> prismic ?=>
      NotForKids:
        val categId   = ForumCategId("general-chess-discussion")
        val topicSlug = s"blog-$id"
        val redirect  = Redirect(routes.ForumTopic.show(categId.value, topicSlug))
        env.forum.topicRepo.existsByTree(categId, topicSlug) flatMap {
          if _ then redirect
          else
            Found(blogApi.one(prismic.api, none, id)): doc =>
              Found(env.forum.categRepo.byId(categId)): categ =>
                env.forum.topicApi.makeBlogDiscuss(
                  categ = categ,
                  slug = topicSlug,
                  name = doc.getText("blog.title") | "New blog post",
                  url = s"${env.net.baseUrl}${routes.Blog.show(doc.id, doc.slug)}"
                ) inject redirect
        }
    }

  private def WithPrismic(f: Context ?=> BlogApi.Context ?=> Fu[Result]) = Open:
    blogApi context ctx.req flatMap { f(using ctx)(using _) }

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  private def checkSlug(document: Option[BlogPost], slug: String)(
      callback: Either[String, BlogPost] => Fu[Result]
  )(using Context): Fu[Result] =
    document
      .collect:
        case document if document.slug == slug => callback(Right(document))
        case document
            if document.slugs
              .exists(s => StringUtils.stripEnd(s, ".") == slug || s == StringUtils.stripEnd(slug, ".")) =>
          callback(Left(document.slug))
      .|(notFound)
