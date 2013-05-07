package controllers

import lila.app._
import views._

object Importer extends LilaController with BaseGame {

  // private def forms = env.importer.forms
  // private def importer = env.importer.importer

  def importGame = TODO
  // Open { implicit ctx ⇒
  //   Ok(html.game.importGame(makeListMenu, forms.importForm))
  // }

  def sendGame = TODO 
  // OpenBody { implicit ctx ⇒
  //   Async {
  //     implicit def req = ctx.body
  //     forms.importForm.bindFromRequest.fold(
  //       failure ⇒ Akka.future {
  //         Ok(html.game.importGame(makeListMenu, failure))
  //       },
  //       data ⇒ (importer(data, ctx.userId) map {
  //         _.fold(Redirect(routes.Importer.importGame)) { game =>
  //           Redirect(routes.Analyse.replay(game.id, "white"))
  //         }
  //       })
  //     )
  //   }
  // }
}
