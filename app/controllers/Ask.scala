package controllers

import play.api.data.Form
import play.api.data.Forms.single
import views.*

import lila.app.{ given, * }
import lila.core.id.AskId
import lila.core.ask.Ask

final class Ask(env: Env) extends LilaController(env):

  def view(aid: AskId, view: Option[String], tally: Boolean) = Open: _ ?=>
    env.ask.repo.getAsync(aid).flatMap {
      case Some(ask) => Ok.snip(views.askUi.renderOne(ask, intVec(view), tally))
      case _         => fuccess(NotFound(s"Ask $aid not found"))
    }

  def picks(aid: AskId, picks: Option[String], view: Option[String], anon: Boolean) = OpenBody: _ ?=>
    effectiveId(aid, anon).flatMap:
      case Some(id) =>
        val setPicks = () =>
          env.ask.repo.setPicks(aid, id, intVec(picks)).map {
            case Some(ask) => Ok.snip(views.askUi.renderOne(ask, intVec(view)))
            case _         => NotFound(s"Ask $aid not found")
          }
        feedbackForm
          .bindFromRequest()
          .fold(
            _ => setPicks(),
            text =>
              setPicks() >> env.ask.repo.setForm(aid, id, text.some).flatMap {
                case Some(ask) => Ok.snip(views.askUi.renderOne(ask, intVec(view)))
                case _         => NotFound(s"Ask $aid not found")
              }
          )
      case _ => authenticationFailed

  def form(aid: AskId, view: Option[String], anon: Boolean) = OpenBody: _ ?=>
    effectiveId(aid, anon).flatMap:
      case Some(id) =>
        env.ask.repo.setForm(aid, id, feedbackForm.bindFromRequest().value).map {
          case Some(ask) => Ok.snip(views.askUi.renderOne(ask, intVec(view)))
          case _         => NotFound(s"Ask $aid not found")
        }
      case _ => authenticationFailed

  def unset(aid: AskId, view: Option[String], anon: Boolean) = Open: _ ?=>
    effectiveId(aid, anon).flatMap:
      case Some(id) =>
        env.ask.repo
          .unset(aid, id)
          .map:
            case Some(ask) => Ok.snip(views.askUi.renderOne(ask, intVec(view)))
            case _         => NotFound(s"Ask $aid not found")

      case _ => authenticationFailed

  def admin(aid: AskId) = Auth: _ ?=>
    env.ask.repo
      .getAsync(aid)
      .map:
        case Some(ask) => Ok.snip(views.askAdminUi.renderOne(ask))
        case _         => NotFound(s"Ask $aid not found")

  def byUser(username: UserStr) = Auth: _ ?=>
    me ?=>
      Ok.async:
        for
          user <- env.user.lightUser(username.id)
          asks <- env.ask.repo.byUser(username.id)
          if (me.is(user)) || isGranted(_.ModerateForum)
        yield views.askAdminUi.show(asks, user.get)

  def json(aid: AskId) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(aid)
        .map:
          case Some(ask) =>
            if (me.is(ask.creator)) || isGranted(_.ModerateForum) then JsonOk(ask.toJson)
            else JsonBadRequest(jsonError(s"Not authorized to view ask $aid"))
          case _ => JsonBadRequest(jsonError(s"Ask $aid not found"))

  def delete(aid: AskId) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(aid)
        .map:
          case Some(ask) =>
            if (me.is(ask.creator)) || isGranted(_.ModerateForum) then
              env.ask.repo.delete(aid)
              Ok
            else Unauthorized
          case _ => NotFound(s"Ask id ${aid} not found")

  def conclude(aid: AskId) = authorized(aid, env.ask.repo.conclude)

  def reset(aid: AskId) = authorized(aid, env.ask.repo.reset)

  private def effectiveId(aid: AskId, anon: Boolean)(using ctx: Context) =
    ctx.myId match
      case Some(u) => fuccess((if anon then Ask.anonHash(u.toString, aid) else u.toString).some)
      case _ =>
        env.ask.repo
          .isOpen(aid)
          .map:
            case true  => Ask.anonHash(ctx.ip.toString, aid).some
            case false => none[String]

  private def authorized(aid: AskId, action: AskId => Fu[Option[lila.core.ask.Ask]]) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(aid)
        .flatMap:
          case Some(ask) =>
            if (me.is(ask.creator)) || isGranted(_.ModerateForum) then
              action(ask._id).map:
                case Some(newAsk) => Ok.snip(views.askUi.renderOne(newAsk))
                case _            => NotFound(s"Ask id ${aid} not found")
            else fuccess(Unauthorized)
          case _ => fuccess(NotFound(s"Ask id $aid not found"))

  private def intVec(param: Option[String]) =
    param.map(_.split('-').filter(_.nonEmpty).map(_.toInt).toVector)

  private val feedbackForm =
    Form[String](single("text" -> lila.common.Form.cleanNonEmptyText(maxLength = 80)))
