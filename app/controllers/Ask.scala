package controllers

import play.api.data.Form
import play.api.data.Forms.single
import views.*

import lila.app.{ given, * }
import lila.ask.Ask

final class Ask(env: Env) extends LilaController(env):

  def view(aid: Ask.ID, view: Option[String], tally: Boolean) = Open: _ ?=>
    env.ask.repo
      .getAsync(aid)
      .flatMap:
        case Some(ask) => Ok.page(html.ask.renderOne(ask, intVec(view), tally))
        case _         => fuccess(NotFound(s"Ask $aid not found"))

  def picks(aid: Ask.ID, picks: Option[String], view: Option[String], anon: Boolean) = Open: _ ?=>
    effectiveId(aid, anon).flatMap:
      case Some(id) =>
        env.ask.repo
          .setPicks(aid, id, intVec(picks))
          .map:
            case Some(ask) => Ok(html.ask.renderOne(ask, intVec(view)))
            case _         => NotFound(s"Ask $aid not found")
      case _ => authenticationFailed

  def form(aid: Ask.ID, view: Option[String], anon: Boolean) = OpenBody: _ ?=>
    effectiveId(aid, anon).flatMap:
      case Some(id) =>
        env.ask.repo
          .setForm(aid, id, feedbackForm.bindFromRequest().value)
          .map:
            case Some(ask) => Ok(html.ask.renderOne(ask, intVec(view)))
            case _         => NotFound(s"Ask $aid not found")
      case _ => authenticationFailed

  def unset(aid: Ask.ID, view: Option[String], anon: Boolean) = Open: _ ?=>
    effectiveId(aid, anon).flatMap:
      case Some(id) =>
        env.ask.repo
          .unset(aid, id)
          .map:
            case Some(ask) => Ok(html.ask.renderOne(ask, intVec(view)))
            case _         => NotFound(s"Ask $aid not found")
      case _ => authenticationFailed

  def admin(aid: Ask.ID) = Auth: _ ?=>
    env.ask.repo
      .getAsync(aid)
      .map:
        case Some(ask) => Ok(html.askAdmin.renderOne(ask))
        case _         => NotFound(s"Ask $aid not found")

  def byUser(username: UserStr) = Auth: _ ?=>
    me ?=>
      Ok.pageAsync:
        for
          user <- env.user.lightUser(username.id)
          asks <- env.ask.repo.byUser(username.id)
          if (me is user) || isGranted(_.ModerateForum)
        yield html.askAdmin.show(asks, user.get)

  def json(aid: Ask.ID) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(aid)
        .map:
          case Some(ask) =>
            if (me is ask.creator) || isGranted(_.ModerateForum) then JsonOk(ask.toJson)
            else JsonBadRequest(jsonError(s"Not authorized to view ask $aid"))
          case _ => JsonBadRequest(jsonError(s"Ask $aid not found"))

  def delete(aid: Ask.ID) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(aid)
        .map:
          case Some(ask) =>
            if (me is ask.creator) || isGranted(_.ModerateForum) then
              env.ask.repo.delete(aid)
              Ok(lila.ask.AskEmbed.askNotFoundFrag)
            else Unauthorized
          case _ => NotFound(s"Ask id ${aid} not found")

  def conclude(aid: Ask.ID) = authorized(aid, env.ask.repo.conclude)

  def reset(aid: Ask.ID) = authorized(aid, env.ask.repo.reset)

  private def effectiveId(aid: Ask.ID, anon: Boolean)(using ctx: Context) =
    ctx.myId match
      case Some(u) => fuccess((if anon then Ask.anonHash(u.toString, aid) else u.toString).some)
      case _ =>
        env.ask.repo
          .isOpen(aid)
          .map:
            case true  => Ask.anonHash(ctx.ip.toString, aid).some
            case false => none[String]

  private def authorized(aid: Ask.ID, action: lila.ask.Ask.ID => Fu[Option[lila.ask.Ask]]) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(aid)
        .flatMap:
          case Some(ask) =>
            if (me is ask.creator) || isGranted(_.ModerateForum) then
              action(ask._id).map:
                case Some(newAsk) => Ok(html.ask.renderOne(newAsk))
                case _            => NotFound(s"Ask id ${aid} not found")
            else fuccess(Unauthorized)
          case _ => fuccess(NotFound(s"Ask id $aid not found"))

  private def intVec(param: Option[String]) =
    param.map(_.split('-').filter(_.nonEmpty).map(_.toInt).toVector)

  private val feedbackForm =
    Form[String](single("text" -> lila.common.Form.cleanNonEmptyText(maxLength = 80)))
