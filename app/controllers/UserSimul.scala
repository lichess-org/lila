package controllers

import lila.app.{ given, * }
import views.*

final class UserSimul(env: Env) extends LilaController(env):

  def path(username: UserStr, path: String, page: Int) = Open:
    Reasonable(page):
      val userOption =
        env.user.repo.byId(username).map { _.filter(_.enabled.yes || isGranted(_.SeeReport)) }
      OptionFuResult(userOption): user =>
        path match
          case "hosted" =>
            env.simul.api.hostedByUser(user.id, page).map { entries =>
              Ok(html.userSimul.hosted(user, entries))
            }
          case _ => notFound
