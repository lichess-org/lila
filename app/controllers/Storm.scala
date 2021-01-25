package controllers

import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._

final class Storm(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      env.storm.selector.apply map { puzzles =>
        Ok(views.html.storm.home(env.storm.json(puzzles), env.storm.json.pref(ctx.pref)))
      }
    }

  def record =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      env.storm.forms.run
        .bindFromRequest()
        .fold(
          _ => funit,
          data => env.storm.runApi.record(data, ctx.me)
        ) inject jsonOkResult
    }
}
