package controllers

import lila._
import views._
import setup._
import http.BodyContext

import play.api.mvc.Call
import play.api.data.Form

import scalaz.effects._

object Setup extends LilaController {

  def forms = env.setup.formFactory
  def processor = env.setup.processor
  def gameRepo = env.game.gameRepo

  val aiForm = Open { implicit ctx ⇒
    IOk(forms.aiFilled map { html.setup.ai(_) })
  }

  val ai = process(forms.ai) { config ⇒
    implicit ctx ⇒
      processor ai config map { pov ⇒
        routes.Round.player(pov.fullId)
      }
  }

  val friendForm = Open { implicit ctx ⇒
    IOk(forms.friendFilled map { html.setup.friend(_) })
  }

  val friend = process(forms.friend) { config ⇒
    implicit ctx ⇒
      processor friend config map { pov ⇒
        routes.Setup.await(pov.fullId)
      }
  }

  def await(fullId: String) = Open { implicit ctx ⇒
    IOptionResult(gameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        Redirect(routes.Round.player(pov.fullId)),
        Ok(html.setup.await(pov, None))
      )
    }
  }

  def cancel(fullId: String) = TODO

  val hookForm = TODO

  val hook = TODO

  private def process[A](form: Form[A])(op: A ⇒ BodyContext ⇒ IO[Call]) =
    OpenBody { ctx ⇒
      implicit val req = ctx.body
      IORedirect(form.bindFromRequest.fold(
        f ⇒ putStrLn(f.errors.toString) map { _ ⇒ routes.Lobby.home },
        config ⇒ op(config)(ctx)
      ))
    }
}
