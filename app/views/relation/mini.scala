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
    case None if followable && !blocked => a(
      cls := "relation-button text",
      dataIcon := "h",
      href := s"${routes.Relation.follow(userId)}?mini=1"
    )(trans.follow.frag())
    case Some(true) => a(
      cls := "relation-button text",
      title := trans.unfollow.txt(),
      href := s"${routes.Relation.unfollow(userId)}?mini=1",
      dataIcon := "h"
    )(trans.following.frag())
    case Some(false) => a(
      cls := "relation-button text",
      title := trans.unblock.txt(),
      href := s"${routes.Relation.unblock(userId)}?mini=1",
      dataIcon := "k"
    )(trans.blocked.frag())
    case _ => emptyFrag
  }
}
