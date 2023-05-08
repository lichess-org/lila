package controllers

import lila.app.{ given, * }
import play.api.data.Form
import play.api.data.Forms.single

final class Ask(env: Env) extends LilaController(env):

  def view(aid: String, view: Option[String], tally: Boolean) = Open: _ ?=>
    env.ask.api
      .get(aid)
      .map:
        case Some(ask) => Ok(views.html.ask.renderInner(ask, paramToList(view), tally))
        case None      => NotFound(s"Ask $aid not found")

  def picks(aid: String, picks: Option[String], view: Option[String], anon: Boolean) = AuthBody { _ ?=> me =>
    // don't validate picks here. parseable but invalid picks are handled elsewhere
    env.ask.api
      .setPicks(
        aid,
        if anon then lila.ask.Ask.anon(me.id, aid) else me.id,
        paramToList(picks)
      )
      .map:
        case Some(ask) => Ok(views.html.ask.renderInner(ask, paramToList(view)))
        case None      => NotFound(s"Ask $aid not found")
  }

  def feedback(aid: String, view: Option[String], anon: Boolean) = AuthBody { _ ?=> me =>
    env.ask.api
      .setFeedback(
        aid,
        if anon then lila.ask.Ask.anon(me.id, aid) else me.id,
        feedbackForm.bindFromRequest().value
      )
      .map:
        case Some(ask) => Ok(views.html.ask.renderInner(ask, paramToList(view)))
        case None      => NotFound(s"Ask $aid not found")
  }

  def unset(aid: String, view: Option[String], anon: Boolean) = AuthBody { _ ?=> me =>
    env.ask.api
      .unset(aid, if anon then lila.ask.Ask.anon(me.id, aid) else me.id)
      .map:
        case Some(ask) => Ok(views.html.ask.renderInner(ask, paramToList(view)))
        case None      => NotFound(s"Ask $aid not found")
  }

  def admin(aid: String) = AuthBody { _ ?=> me =>
    env.ask.api
      .get(aid)
      .map:
        case Some(ask) => Ok(views.html.askAdmin.renderInner(ask))
        case None      => NotFound(s"Ask $aid not found")
  }

  def byUser(username: UserStr) = AuthBody { ctx ?=> me =>
    for
      user <- env.user.lightUser(username.id)
      asks <- env.ask.api.byUser(username.id)
      if user.nonEmpty
    yield Ok(views.html.askAdmin.show(asks, user.get))
  }

  def delete(aid: String) = AuthBody { ctx ?=> me =>
    env.ask.api
      .get(aid)
      .flatMap:
        case None => fuccess(NotFound(s"Ask id ${aid} not found"))
        case Some(ask) =>
          if ask.creator != me.id then fuccess(Unauthorized)
          else
            env.ask.api.delete(aid)
            fuccess(Ok(lila.ask.AskApi.askNotFoundFrag))

  }

  def conclude(aid: String) = authorizedAction(aid, env.ask.api.conclude)

  def reset(aid: String) = authorizedAction(aid, env.ask.api.reset)

  private def authorizedAction(aid: String, action: lila.ask.Ask => Fu[Option[lila.ask.Ask]]) =
    AuthBody { ctx ?=> me =>
      env.ask.api
        .get(aid)
        .flatMap:
          case None => fuccess(NotFound(s"Ask id ${aid} not found"))
          case Some(ask) =>
            if ask.creator != me.id then fuccess(Unauthorized)
            else
              action(ask).flatMap:
                case Some(newAsk) => fuccess(Ok(views.html.ask.renderInner(newAsk)))
                case None         => fufail(new RuntimeException("Something is so very wrong."))
    }

  private def paramToList(param: Option[String]) =
    param map (_ split ('-') filter (_ nonEmpty) map (_ toInt) toList)

  private val feedbackForm =
    Form[String](single("text" -> lila.common.Form.cleanNonEmptyText(maxLength = 80)))
