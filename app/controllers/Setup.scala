package controllers

import play.api.data.Form
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.common.{ HTTPRequest, LilaCookie }
import lila.game.{ GameRepo, Pov, AnonCookie }
import lila.user.UserRepo
import views._

object Setup extends LilaController with TheftPrevention with play.api.http.ContentTypes {

  private def env = Env.setup

  def aiForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req)
      env.forms aiFilled get("fen") map { form =>
        html.setup.ai(form, Env.ai.aiPerfApi.intRatings)
      }
    else fuccess {
      Redirect(routes.Lobby.home + "#ai")
    }
  }

  def ai = process(env.forms.ai) { config =>
    implicit ctx =>
      env.processor ai config map { pov =>
        pov -> routes.Round.player(pov.fullId)
      }
  }

  def friendForm(userId: Option[String]) = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) userId ?? UserRepo.named flatMap {
      case None => env.forms friendFilled get("fen") map {
        html.setup.friend(_, none, none)
      }
      case Some(user) => challenge(user) flatMap { error =>
        env.forms friendFilled get("fen") map {
          html.setup.friend(_, user.some, error)
        }
      }
    }
    else fuccess {
      Redirect(routes.Lobby.home + "#friend")
    }
  }

  private def challenge(user: lila.user.User)(implicit ctx: Context): Fu[Option[String]] = ctx.me match {
    case None => fuccess("Only registered players can send challenges.".some)
    case Some(me) => Env.relation.api.blocks(user.id, me.id) flatMap {
      case true => fuccess(s"{{user}} doesn't accept challenges from you.".some)
      case false => Env.pref.api getPref user zip Env.relation.api.follows(user.id, me.id) map {
        case (pref, follow) => lila.pref.Pref.Challenge.block(me, user, pref.challenge, follow)
      }
    }
  }

  def friend(userId: Option[String]) = process(env.forms.friend) { config =>
    implicit ctx =>
      env.processor friend config map { pov =>
        pov -> routes.Setup.await(pov.fullId, userId)
      }
  }

  def decline(gameId: String) = Auth { implicit ctx =>
    me =>
      OptionResult(GameRepo game gameId) { game =>
        if (game.started) BadRequest("Cannot decline started challenge")
        else {
          Env.game.maintenance remove game.id
          Env.hub.actor.challenger ! lila.hub.actorApi.setup.DeclineChallenge(gameId)
          Ok("ok")
        }
      }
  }

  def hookForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req)
      env.forms.hookFilled map { html.setup.hook(_) }
    else fuccess {
      Redirect(routes.Lobby.home + "#hook")
    }
  }

  def hook(uid: String) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    env.forms.hook(ctx).bindFromRequest.value ?? { config =>
      env.processor.hook(config, uid, lila.common.HTTPRequest sid req)
    }
  }

  def filterForm = Open { implicit ctx =>
    env.forms.filterFilled map {
      case (form, filter) => html.setup.filter(form, filter)
    }
  }

  def filter = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    env.forms.filter(ctx).bindFromRequest.fold[Fu[Result]](
      f => fulogwarn(f.errors.toString) inject BadRequest(()),
      config => JsonOk(env.processor filter config inject config.render)
    )
  }

  def join(id: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      env.friendJoiner(game, ctx.me).fold(
        err => fuccess {
          Redirect(routes.Round.watcher(id, "white"))
        },
        _ map {
          case (p, events) => {
            Env.hub.socket.round ! lila.hub.actorApi.map.Tell(p.gameId, lila.round.actorApi.EventList(events))
            implicit val req = ctx.req
            redirectPov(p, routes.Round.player(p.fullId))
          }
        })
    }
  }

  def await(fullId: String, userId: Option[String]) = Open { implicit ctx =>
    OptionFuResult(GameRepo pov fullId) { pov =>
      pov.game.started.fold(
        Redirect(routes.Round.player(pov.fullId)).fuccess,
        Env.round.version(pov.gameId) zip
          (userId ?? UserRepo.named) flatMap {
            case (version, user) => PreventTheft(pov) {
              Ok(html.setup.await(
                pov,
                version,
                env.friendConfigMemo get pov.game.id,
                user)).fuccess
            }
          }
      )
    }
  }

  def cancel(fullId: String) = Open { implicit ctx =>
    OptionResult(GameRepo pov fullId) { pov =>
      if (pov.game.started) Redirect(routes.Round.player(pov.fullId))
      else {
        Env.game.maintenance remove pov.game.id
        Redirect(routes.Lobby.home)
      }
    }
  }

  def validateFen = Open { implicit ctx =>
    {
      for {
        fen ← get("fen")
        parsed ← chess.format.Forsyth <<< fen
        strict = get("strict").isDefined
        if (parsed.situation playable strict)
        validated = chess.format.Forsyth >> parsed
      } yield html.game.miniBoard(validated, parsed.situation.color.name)
    }.fold[Result](BadRequest)(Ok(_)).fuccess
  }

  private def process[A](form: Context => Form[A])(op: A => BodyContext => Fu[(Pov, Call)]) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      form(ctx).bindFromRequest.fold(
        f => negotiate(
          html = fuloginfo(f.errors.toString) >> Lobby.renderHome(Results.BadRequest),
          api = _ => fuccess(BadRequest(f.errorsAsJson))
        ),
        config => op(config)(ctx) flatMap {
          case (pov, call) => negotiate(
            html = fuccess(redirectPov(pov, call)),
            api = apiVersion => Env.round version pov.gameId map { v =>
              Created(Env.round.jsonView.playerJson(pov, v, ctx.pref, apiVersion)) as JSON
            }
          )
        }
      )
    }

  private def redirectPov(pov: Pov, call: Call)(implicit ctx: Context, req: RequestHeader) =
    if (ctx.isAuth) Redirect(call)
    else Redirect(call) withCookies LilaCookie.cookie(
      AnonCookie.name,
      pov.playerId,
      maxAge = AnonCookie.maxAge.some,
      httpOnly = false.some)
}
