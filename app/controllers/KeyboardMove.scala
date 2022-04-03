package controllers

import views._
import lila.app._

final class KeyboardMove(
    env: Env
) extends LilaController(env) {

  def help =
    Open { implicit ctx =>
      Ok(html.keyboardMove.help()).fuccess
    }
}
