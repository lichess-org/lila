package controllers

import play.api.data.Form
import play.api.data.Forms.single

import lila.app.{ given, * }
import lila.core.id.AskId
import lila.core.ask.Ask

final class Ask(env: Env) extends LilaController(env):

  def view(askId: AskId, viewOrder: Option[String], tally: Boolean) = Open: _ ?=>
    env.ask.repo
      .getAsync(askId)
      .flatMap:
        case Some(ask) => Ok.snip(views.askUi.renderOne(ask, parseIntVector(viewOrder), tally))
        case _ => fuccess(NotFound(s"Ask $askId not found"))

  def picks(askId: AskId, picks: Option[String], viewOrder: Option[String], anon: Boolean) = OpenBody: _ ?=>
    voterId(askId, anon).flatMap:
      case Some(id) =>
        def setPicks() =
          env.ask.repo
            .setPicks(askId, id, parseIntVector(picks))
            .map:
              case Some(ask) => Ok.snip(views.askUi.renderOne(ask, parseIntVector(viewOrder)))
              case _ => NotFound(s"Ask $askId not found")

        feedbackForm
          .bindFromRequest()
          .fold(
            _ => setPicks(),
            text =>
              setPicks() >> env.ask.repo
                .setForm(askId, id, text.some)
                .flatMap:
                  case Some(ask) => Ok.snip(views.askUi.renderOne(ask, parseIntVector(viewOrder)))
                  case _ => NotFound(s"Ask $askId not found")
          )
      case _ => authenticationFailed

  def form(askId: AskId, viewOrder: Option[String], anon: Boolean) = OpenBody: _ ?=>
    voterId(askId, anon).flatMap:
      case Some(id) =>
        env.ask.repo
          .setForm(askId, id, feedbackForm.bindFromRequest().value)
          .map:
            case Some(ask) => Ok.snip(views.askUi.renderOne(ask, parseIntVector(viewOrder)))
            case _ => NotFound(s"Ask $askId not found")
      case _ => authenticationFailed

  def unset(askId: AskId, viewOrder: Option[String], anon: Boolean) = Open: _ ?=>
    voterId(askId, anon).flatMap:
      case Some(id) =>
        env.ask.repo
          .unset(askId, id)
          .map:
            case Some(ask) => Ok.snip(views.askUi.renderOne(ask, parseIntVector(viewOrder)))
            case _ => NotFound(s"Ask $askId not found")

      case _ => authenticationFailed

  def admin(askId: AskId) = Auth: _ ?=>
    askId.pp
    env.ask.repo
      .getAsync(askId)
      .map:
        case Some(ask) => Ok.snip(views.askAdminUi.renderOne(ask))
        case _ => NotFound(s"Ask $askId not found")

  def byUser(username: UserStr) = Auth: _ ?=>
    me ?=>
      Ok.async:
        for
          user <- env.user.lightUser(username.id)
          asks <- env.ask.repo.byUser(username.id)
          if (me.is(user)) || isGranted(_.ModerateForum)
        yield views.askAdminUi.show(asks, user.get)

  def json(askId: AskId) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(askId)
        .map:
          case Some(ask) =>
            if (me.is(ask.creator)) || isGranted(_.ModerateForum) then JsonOk(ask.toJson)
            else JsonBadRequest(jsonError(s"Not authorized to view ask $askId"))
          case _ => JsonBadRequest(jsonError(s"Ask $askId not found"))

  def jsonByUrl(url: String) = authorizedByUrl(url): asks =>
    fuccess(JsonOk(play.api.libs.json.JsArray(asks.map(_.toJson))))

  def delete(askId: AskId) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(askId)
        .map:
          case Some(ask) =>
            if (me.is(ask.creator)) || isGranted(_.ModerateForum) then
              env.ask.repo.delete(askId)
              Ok
            else Unauthorized
          case _ => NotFound(s"Ask id ${askId} not found")

  def conclude(askId: AskId) = authorized(askId, env.ask.repo.conclude)

  def concludeByUrl(url: String) = authorizedByUrl(url): asks =>
    asks.map(ask => env.ask.repo.conclude(ask._id)).parallel.map(_ => Ok)

  def reset(askId: AskId) = authorized(askId, env.ask.repo.reset)

  def resetByUrl(url: String) = authorizedByUrl(url): asks =>
    asks.map(ask => env.ask.repo.reset(ask._id)).parallel.map(_ => Ok)

  private def voterId(askId: AskId, anon: Boolean)(using ctx: Context) =
    ctx.myId match
      case Some(u) => fuccess((if anon then Ask.anonHash(u.toString, askId) else u.toString).some)
      case _ =>
        env.ask.repo
          .isOpen(askId)
          .map:
            case true => Ask.anonHash(ctx.ip.toString, askId).some
            case false => none[String]

  private def authorized(askId: AskId, action: AskId => Fu[Option[lila.core.ask.Ask]]) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .getAsync(askId)
        .flatMap:
          case Some(ask) =>
            if (me.is(ask.creator)) || isGranted(_.ModerateForum) then
              action(ask._id).map:
                case Some(newAsk) => Ok.snip(views.askUi.renderOne(newAsk))
                case _ => NotFound(s"Ask id ${askId} not found")
            else fuccess(Unauthorized)
          case _ => fuccess(NotFound(s"Ask id $askId not found"))

  private def authorizedByUrl(url: String)(
      action: List[lila.core.ask.Ask] => Fu[play.api.mvc.Result]
  ) = Auth: _ ?=>
    me ?=>
      env.ask.repo
        .byUrl(url)
        .flatMap: asks =>
          if asks.forall(ask => me.is(ask.creator)) || isGranted(_.ModerateForum) then action(asks)
          else fuccess(Unauthorized)

  private def parseIntVector(param: Option[String]) =
    param.flatMap: s =>
      val parts = s.split('-').iterator.filter(_.nonEmpty).toVector
      val ints = parts.flatMap(_.toIntOption)
      Option.when(ints.size == parts.size)(ints.pp)

  private val feedbackForm =
    Form[String](single("text" -> lila.common.Form.cleanNonEmptyText(maxLength = 80)))
