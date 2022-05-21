package controllers

import lila.app._

final class Ask(env: Env) extends LilaController(env) {

  def pick(id: String, choice: Int) =
    AuthBody { implicit ctx => me =>
      env.ask.api.pick(id, me.id, choice) map {
        case Some(ask) => Ok(views.html.ask.render(ask)) // redirect(ask)
        case None      => NotFound(s"Ask id ${id} not found")
      }
    }

  def conclude(id: String) = authorizedAction(id, env.ask.api.conclude)

  def reset(id: String) = authorizedAction(id, env.ask.api.reset)

  private def authorizedAction(id: String, action: lila.ask.Ask => Fu[Option[lila.ask.Ask]]) =
    AuthBody { implicit ctx => me =>
      env.ask.api.get(id) flatMap {
        case None => fuccess(NotFound(s"Ask id ${id} not found"))
        case Some(ask) =>
          if (ask.creator != me.id) fuccess(Unauthorized)
          else
            action(ask) flatMap {
              case Some(newAsk) => fuccess(Ok(views.html.ask.render(newAsk)))
              case None         => fufail(new RuntimeException("Something is so very wrong."))
            }
      }
    }
}
