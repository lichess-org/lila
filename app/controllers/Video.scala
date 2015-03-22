package controllers

import play.api.data._, Forms._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.video.{ View, Video => VideoModel }
import views._

object Video extends LilaController {

  private def env = Env.video

  def index = Open { implicit ctx =>
    env.api.video.popular(9) map { videos =>
      html.video.index(videos)
    }
  }

  def show(id: String) = Open { implicit ctx =>
    env.api.video.find(id) flatMap {
      case None => fuccess(html.video.notFound())
      case Some(video) => env.api.video.similar(video, 6) map { similar =>
        html.video.show(video, similar)
      }
    }
  }
}
