package controllers

import lila.app._
import views._
import http.Context

import play.api.mvc.Result
import play.api.libs.concurrent._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object Importer extends LilaController with BaseGame {

  private def forms = env.importer.forms
  private def importer = env.importer.importer

  val importGame = Open { implicit ctx ⇒
    Ok(html.game.importGame(makeListMenu, forms.importForm))
  }

  val sendGame = OpenBody { implicit ctx ⇒
    Async {
      implicit def req = ctx.body
      forms.importForm.bindFromRequest.fold(
        failure ⇒ Akka.future {
          Ok(html.game.importGame(makeListMenu, failure))
        },
        data ⇒ (importer(data, ctx.userId) map {
          _.fold(Redirect(routes.Importer.importGame)) { game =>
            Redirect(routes.Analyse.replay(game.id, "white"))
          }
        })
      )
    }
  }
}
