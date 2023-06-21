package controllers

import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.video.{ Filter, UserControl, View }
import views.*

final class Video(env: Env) extends LilaController(env):

  private def api = env.video.api

  private def WithUserControl[A](f: UserControl => Fu[A])(using Context): Fu[A] =
    val reqTags = get("tags") so (_.split('/').toList.map(_.trim.toLowerCase))
    api.tag.paths(reqTags) map { tags =>
      UserControl(
        filter = Filter(reqTags),
        tags = tags,
        query = get("q"),
        crawler = HTTPRequest isCrawler ctx.req
      )
    } flatMap f

  def index = Open:
    pageHit
    WithUserControl: control =>
      Ok.pageAsync:
        control.query match
          case Some(query) =>
            api.video.search(ctx.me, query, getInt("page") | 1) map {
              html.video.search(_, control)
            }
          case None =>
            api.video.byTags(ctx.me, control.filter.tags, getInt("page") | 1) zip
              api.video.count.apply map { (videos, count) =>
                html.video.index(videos, count, control)
              }

  def show(id: String) = Open:
    WithUserControl: control =>
      api.video.find(id) flatMap {
        case None => NotFound.page(html.video.bits.notFound(control))
        case Some(video) =>
          api.video.similar(ctx.me, video, 9) zip
            ctx.userId.so { userId =>
              api.view.add(View.make(videoId = video.id, userId = userId))
            } flatMap { (similar, _) =>
              Ok.page(html.video.show(video, similar, control))
            }
      }

  def author(author: String) = Open:
    WithUserControl: control =>
      Ok.pageAsync:
        api.video.byAuthor(ctx.me, author, getInt("page") | 1) map {
          html.video.bits.author(author, _, control)
        }

  def tags = Open:
    WithUserControl: control =>
      Ok.pageAsync:
        api.tag.allPopular map {
          html.video.bits.tags(_, control)
        }
