package controllers

import scalalib.model.Language

import lila.app.{ *, given }

final class Video(env: Env) extends LilaController(env):

  import env.video.api

  def index = Open(serveIndex)
  def indexLang = LangPage(routes.Video.index)(serveIndex)
  private def serveIndex(using ctx: Context) = NoBot:
    pageHit
    api.withUserControl: control =>
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
    api.withUserControl: control =>
      api.video
        .find(id)
        .flatMap:
          case None => NotFound.page(views.video.notFound(control))
          case Some(video) =>
            api.video
              .similar(ctx.me, video, 9)
              .zip(ctx.userId.so { userId =>
                api.view.add(lila.video.View.make(videoId = video.id, userId = userId))
              })
              .flatMap { (similar, _) =>
                Ok.page(views.video.show(video, similar, control))
              }

  def author(author: String) = Open(serveAuthor(author))
  def authorLang(lang: Language, author: String) =
    LangPage(routes.Video.author(author))(serveAuthor(author))(lang)
  private def serveAuthor(author: String)(using ctx: Context) =
    api.withUserControl: control =>
      Ok.async:
        api.video
          .byAuthor(ctx.me, author, getInt("page") | 1)
          .map:
            views.video.author(author, _, control)
