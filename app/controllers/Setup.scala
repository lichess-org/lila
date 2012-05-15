package controllers

import lila._
import views._
import setup._

object Setup extends LilaController {

  def forms = env.setup.formFactory
  def processor = env.setup.processor

  val aiForm = Open { implicit ctx ⇒
    IOk(forms.aiFilled map { html.setup.ai(_) })
  }

  val ai = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    forms.ai.bindFromRequest.fold(
      _ ⇒ Redirect(routes.Lobby.home),
      config ⇒ IORedirect(
        processor ai config map { pov ⇒
          routes.Round.player(pov.playerFullId)
        }
      )
    )
  }

  val friendForm = TODO

  val friend = TODO

  val hookForm = TODO

  val hook = TODO
}
