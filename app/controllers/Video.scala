package controllers

import play.api.data._, Forms._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.video.{ View, Video => VideoModel, UserControl, Filter }
import views._

object Video extends LilaController {

  private def env = Env.video

  private def WithUserControl[A](f: UserControl => Fu[A])(implicit ctx: Context): Fu[A] = {
    val reqTags = get("tags") ?? (_.split(',').toList.map(_.trim.toLowerCase))
    env.api.tag.popularAnd(25, reqTags) map { tags =>
      UserControl(
        filter = Filter(reqTags),
        tags = tags)
    } flatMap f
  }

  private def renderIndex(control: UserControl)(implicit ctx: Context) =
    env.api.video.byTags(control.filter.tags, getInt("page") | 1) map { videos =>
      Ok(html.video.index(videos, control))
    }

  def index = Open { implicit ctx =>
    WithUserControl { control =>
      val filter = control.filter.copy(
        tags = get("tags") ?? (_.split(',').toList.map(_.trim.toLowerCase))
      )
      renderIndex(control.copy(filter = filter))
    }
  }

  def show(id: String) = Open { implicit ctx =>
    WithUserControl { control =>
      env.api.video.find(id) flatMap {
        case None => fuccess(NotFound(html.video.notFound(control)))
        case Some(video) => env.api.video.similar(video, 9) zip
          ctx.userId.?? { userId =>
            env.api.view.add(View.make(videoId = video.id, userId = userId))
          } map {
            case (similar, _) =>
              Ok(html.video.show(video, similar, control))
          }
      }
    }
  }

  def author(author: String) = Open { implicit ctx =>
    WithUserControl { control =>
      env.api.video.byAuthor(author, getInt("page") | 1) map { videos =>
        Ok(html.video.author(author, videos, control))
      }
    }
  }
}
