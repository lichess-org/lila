package controllers

import lila._
import views._

object Notification extends LilaController {

  private def api = env.notificationApi

  def remove(id: String) = Auth { implicit ctx ⇒
    me ⇒
      Ok(api.remove(me.id, id))
  }
}
