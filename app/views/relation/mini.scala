package views.html.relation

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object mini {

  def apply(
    userId: lidraughts.user.User.ID,
    blocked: Boolean,
    followable: Boolean,
    relation: Option[lidraughts.relation.Relation] = None
  )(implicit ctx: Context) = relation match {
    case None if followable && !blocked => a(
      cls := "btn-rack__btn relation-button text",
      dataIcon := "h",
      href := s"${routes.Relation.follow(userId)}?mini=1"
    )(trans.follow())
    case Some(true) => a(
      cls := "btn-rack__btn relation-button text",
      title := trans.unfollow.txt(),
      href := s"${routes.Relation.unfollow(userId)}?mini=1",
      dataIcon := "h"
    )(trans.following())
    case Some(false) => a(
      cls := "btn-rack__btn relation-button text",
      title := trans.unblock.txt(),
      href := s"${routes.Relation.unblock(userId)}?mini=1",
      dataIcon := "k"
    )(trans.blocked())
    case _ => emptyFrag
  }
}
