package controllers

import play.api.data._, Forms._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.video.{ View, Video => VideoModel, UserControl }
import views._

object Video extends LilaController {

  private def env = Env.video

  private def UserControl[A](f: UserControl => Fu[A])(implicit ctx: Context): Fu[A] =
    env.api.userControl(ctx.userId | "anon", 25) flatMap f

  private def renderIndex(control: UserControl)(implicit ctx: Context) =
    env.api.video.byTags(control.filter.tags, 15) map { videos =>
      Ok(html.video.index(videos, control))
    }

  def index = Open { implicit ctx =>
    UserControl { control =>
      val filter = control.filter.copy(
        tags = get("tags") ?? (_.split(',').toList.map(_.trim.toLowerCase))
      )
      env.api.filter.set(filter) >>
        renderIndex(control.copy(filter = filter))
    }
  }

  def show(id: String) = Open { implicit ctx =>
    UserControl { control =>
      env.api.video.find(id) flatMap {
        case None => fuccess(NotFound(html.video.notFound(control)))
        case Some(video) => env.api.video.similar(video, 9) map { similar =>
          Ok(html.video.show(video, similar, control))
        }
      }
    }
  }

  def author(author: String) = Open { implicit ctx =>
    UserControl { control =>
      env.api.video byAuthor author map { videos =>
        Ok(html.video.author(author, videos, control))
      }
    }
  }
}
