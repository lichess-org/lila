package controllers

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.video.{ Filter, UserControl, View }
import views._

final class Video(env: Env) extends LilaController(env) {

  private def api = env.video.api

  private def WithUserControl[A](f: UserControl => Fu[A])(implicit ctx: Context): Fu[A] = {
    val reqTags = get("tags") ?? (_.split('/').toList.map(_.trim.toLowerCase))
    api.tag.paths(reqTags) map { tags =>
      UserControl(
        filter = Filter(reqTags),
        tags = tags,
        query = get("q"),
        bot = HTTPRequest isCrawler ctx.req
      )
    } flatMap f
  }

  def index =
    Open { implicit ctx =>
      pageHit
      WithUserControl { control =>
        control.query match {
          case Some(query) =>
            api.video.search(ctx.me, query, getInt("page") | 1) map { videos =>
              Ok(html.video.search(videos, control))
            }
          case None =>
            api.video.byTags(ctx.me, control.filter.tags, getInt("page") | 1) zip
              api.video.count.apply map {
              case (videos, count) =>
                Ok(html.video.index(videos, count, control))
            }
        }
      }
    }

  def show(id: String) =
    Open { implicit ctx =>
      WithUserControl { control =>
        api.video.find(id) flatMap {
          case None => fuccess(NotFound(html.video.bits.notFound(control)))
          case Some(video) =>
            api.video.similar(ctx.me, video, 9) zip
              ctx.userId.?? { userId =>
                api.view.add(View.make(videoId = video.id, userId = userId))
              } map {
              case (similar, _) =>
                Ok(html.video.show(video, similar, control))
            }
        }
      }
    }

  def author(author: String) =
    Open { implicit ctx =>
      WithUserControl { control =>
        api.video.byAuthor(ctx.me, author, getInt("page") | 1) map { videos =>
          Ok(html.video.bits.author(author, videos, control))
        }
      }
    }

  def tags =
    Open { implicit ctx =>
      WithUserControl { control =>
        api.tag.allPopular map { tags =>
          Ok(html.video.bits.tags(tags, control))
        }
      }
    }
}
