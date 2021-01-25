package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._

final class Storm(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      NoBot {
        env.storm.selector.apply map { puzzles =>
          NoCache {
            Ok(views.html.storm.home(env.storm.json(puzzles), env.storm.json.pref(ctx.pref)))
          }
        }
      }
    }

  def record =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        env.storm.forms.run
          .bindFromRequest()
          .fold(
            _ => fuccess(none),
            data => env.storm.dayApi.addRun(data, ctx.me)
          ) map env.storm.json.newHigh map { json =>
          Ok(json) as JSON
        }
      }
    }

  def dashboard(page: Int) =
    Auth { implicit ctx => me =>
      get("u")
        .ifTrue(isGranted(_.Hunter))
        .??(env.user.repo.named)
        .map(_ | me)
        .flatMap { user =>
          env.storm.dayApi.history(user.id, page) flatMap { history =>
            env.storm.highApi.get(user.id) map { high =>
              Ok(views.html.storm.dashboard(user, history, high))
            }
          }
        }
    }
}
