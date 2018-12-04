package views.html.relation

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object mini {

  def apply(
    userId: lila.user.User.ID,
    blocked: Boolean,
    followable: Boolean,
    relation: Option[lila.relation.Relation] = None
  )(implicit ctx: Context) = relation match {
    case None if followable && !blocked =>
      a(cls := "relation button", href := s"${routes.Relation.follow(userId)}?mini=1")(
        i(dataIcon := "h", cls := "text")(trans.follow())
      )
    case Some(true) =>
      a(cls := "relation button hint--bottom", dataHint := trans.unfollow.txt(), href := s"${routes.Relation.unfollow(userId)}?mini=1")(
        i(dataIcon := "h", cls := "text")(trans.following())
      )
    case Some(false) =>
      a(cls := "relation button hint--bottom hover_text", dataHint := trans.unblock.txt(), href := s"${routes.Relation.unblock(userId)}?mini=1")(
        i(dataIcon := "k", cls := "text")(trans.blocked())
      )
    case _ => emptyFrag
  }
}
