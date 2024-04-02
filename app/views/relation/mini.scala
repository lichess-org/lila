package views.html.relation

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object mini:

  def apply(
      userId: UserId,
      blocked: Boolean,
      followable: Boolean,
      relation: Option[lila.relation.Relation] = None
  )(using Context) =
    relation match
      case None if followable && !blocked =>
        val name   = trans.site.follow.txt()
        val isLong = name.sizeIs > 8
        a(
          cls      := s"btn-rack__btn relation-button${(!isLong).so(" text")}",
          dataIcon := licon.ThumbsUp,
          href     := s"${routes.Relation.follow(userId)}?mini=1",
          title    := isLong.option(name)
        )((!isLong).option(name))
      case Some(true) =>
        a(
          cls      := "btn-rack__btn relation-button text",
          title    := trans.site.unfollow.txt(),
          href     := s"${routes.Relation.unfollow(userId)}?mini=1",
          dataIcon := licon.ThumbsUp
        )(trans.site.following())
      case Some(false) =>
        a(
          cls      := "btn-rack__btn relation-button text",
          title    := trans.site.unblock.txt(),
          href     := s"${routes.Relation.unblock(userId)}?mini=1",
          dataIcon := licon.NotAllowed
        )(trans.site.blocked())
      case _ => emptyFrag
