package controllers

import lila.api.Context
import lila.app._
import play.api.libs.json.Json
import play.api.mvc.Result
import views._

object Coach extends LilaController {

  private def env = Env.coach

  def opening(username: String, colorStr: String) = Open { implicit ctx =>
    chess.Color(colorStr).fold(notFound) { color =>
      Accessible(username) { user =>
        env.statApi.count(user.id) map { nbPeriods =>
          Ok(html.coach.opening(user, color, nbPeriods))
        }
      }
    }
  }

  def openingJson(username: String, colorStr: String) = Open { implicit ctx =>
    chess.Color(colorStr).fold(notFoundJson(s"No such color: $colorStr")) { color =>
      AccessibleJson(username) { user =>
        WithRange { range =>
          env.statApi.fetchRangeForOpenings(user.id, range) flatMap {
            _.fold(notFoundJson(s"Data not generated yet")) { period =>
              env.jsonView.opening(period, color) map { data =>
                Ok(data)
              }
            }
          }
        }
      }
    }
  }

  def move(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.statApi.count(user.id) map { nbPeriods =>
        Ok(html.coach.move(user, nbPeriods))
      }
    }
  }

  def moveJson(username: String) = Open { implicit ctx =>
    AccessibleJson(username) { user =>
      WithRange { range =>
        env.statApi.fetchRangeForMoves(user.id, range) flatMap {
          _.fold(notFoundJson(s"Data not generated yet")) { period =>
            env.jsonView.move(period) map { data =>
              Ok(data)
            }
          }
        }
      }
    }
  }

  def refresh(username: String) = Open { implicit ctx =>
    Accessible(username) { user =>
      env.aggregator(user) inject Ok
    }
  }

  private def WithRange(f: Range => Fu[Result])(implicit ctx: Context): Fu[Result] =
    get("range").flatMap {
      _.split('-') match {
        case Array(a, b) => (parseIntOption(a) |@| parseIntOption(b))(Range.apply)
        case _           => none
      }
    }.fold(notFoundJson("No range provided"))(f)

  private def Accessible(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo named username flatMap {
      case None => notFound
      case Some(u) => env.share.grant(u, ctx.me) flatMap {
        case true                          => f(u)
        case false if isGranted(_.UserSpy) => f(u)
        case false                         => fuccess(Forbidden(html.coach.forbidden(u)))
      }
    }

  private def AccessibleJson(username: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo named username flatMap {
      case None => notFoundJson(s"No such user: $username")
      case Some(u) => env.share.grant(u, ctx.me) flatMap {
        case true                          => f(u)
        case false if isGranted(_.UserSpy) => f(u)
        case false                         => fuccess(Forbidden(Json.obj("error" -> s"User $username data is protected")))
      }
    } map (_ as JSON)
}
