package views.html.relation

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object mini:

  def apply(
      userId: UserId,
      blocked: Boolean,
      followable: Boolean,
      relation: Option[lila.relation.Relation] = None
  )(using Context) =
    relation match
      case None if followable && !blocked =>
        val name   = trans.follow.txt()
        val isLong = name.sizeIs > 8
        a(
          cls      := s"btn-rack__btn relation-button${!isLong so " text"}",
          dataIcon := licon.ThumbsUp,
          href     := s"${routes.Relation.follow(userId)}?mini=1",
          title    := isLong option name
        )(!isLong option name)
      case Some(true) =>
        a(
          cls      := "btn-rack__btn relation-button text",
          title    := trans.unfollow.txt(),
          href     := s"${routes.Relation.unfollow(userId)}?mini=1",
          dataIcon := licon.ThumbsUp
        )(trans.following())
      case Some(false) =>
        a(
          cls      := "btn-rack__btn relation-button text",
          title    := trans.unblock.txt(),
          href     := s"${routes.Relation.unblock(userId)}?mini=1",
          dataIcon := licon.NotAllowed
        )(trans.blocked())
      case _ => emptyFrag
