package controllers

import scalalib.model.Language

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.video.{ Filter, UserControl, View }

final class Video(env: Env) extends LilaController(env):

  import env.video.api

  private def WithUserControl[A](f: UserControl => Fu[A])(using Context): Fu[A] =
    val reqTags = get("tags").so(_.split('/').toList.map(_.trim.toLowerCase))
    api.tag
      .paths(reqTags)
      .map { tags =>
        UserControl(
          filter = Filter(reqTags),
          tags = tags,
          query = get("q"),
          crawler = HTTPRequest.isCrawler(ctx.req)
        )
      }
      .flatMap(f)

  def index = Open(serveIndex)
  def indexLang = LangPage(routes.Video.index)(serveIndex)
  private def serveIndex(using ctx: Context) = NoBot:
    pageHit
    WithUserControl: control =>
      Ok.async:
        control.query match
          case Some(query) =>
            api.video
              .search(ctx.me, query, getInt("page") | 1)
              .map:
                views.video.search(_, control)
          case None =>
            api.video
              .byTags(ctx.me, control.filter.tags, getInt("page") | 1)
              .zip(api.video.count.apply)
              .map: (videos, count) =>
                views.video.index(videos, count, control)

  def show(id: String) = Open(serveShow(id))
  def showLang(lang: Language, id: String) = LangPage(routes.Video.show(id))(serveShow(id))(lang)
  private def serveShow(id: String)(using ctx: Context) =
    WithUserControl: control =>
      api.video
        .find(id)
        .flatMap:
          case None => NotFound.page(views.video.notFound(control))
          case Some(video) =>
            api.video
              .similar(ctx.me, video, 9)
              .zip(ctx.userId.so { userId =>
                api.view.add(View.make(videoId = video.id, userId = userId))
              })
              .flatMap { (similar, _) =>
                Ok.page(views.video.show(video, similar, control))
              }

  def author(author: String) = Open(serveAuthor(author))
  def authorLang(lang: Language, author: String) =
    LangPage(routes.Video.author(author))(serveAuthor(author))(lang)
  private def serveAuthor(author: String)(using ctx: Context) =
    WithUserControl: control =>
      Ok.async:
        api.video
          .byAuthor(ctx.me, author, getInt("page") | 1)
          .map:
            views.video.author(author, _, control)

  def tags = Open(serveTags)
  def tagsLang = LangPage(routes.Video.tags)(serveTags)
  private def serveTags(using ctx: Context) =
    WithUserControl: control =>
      Ok.async:
        api.tag.allPopular.map:
          views.video.tags(_, control)
