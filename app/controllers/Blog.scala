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

  def show(id: String, slug: String) = Open:
    Found(env.ublog.api.getByPrismicId(id)): post =>
      Redirect(routes.Ublog.post("lichess", post.slug, post.id), MOVED_PERMANENTLY)

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
    blogApi.context flatMap { f(using ctx)(using _) }
