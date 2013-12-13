package controllers

import play.api.data.Form
import play.api.mvc.{ SimpleResult, Call }

import lila.app._
import lila.common.HTTPRequest
import lila.game.GameRepo
import lila.user.UserRepo
import lila.user.{ Context, BodyContext }
import views._

object Setup extends LilaController with TheftPrevention {

  private def env = Env.setup

  def aiForm = Open { implicit ctx ⇒
    if (HTTPRequest isXhr ctx.req)
      env.forms aiFilled get("fen") map { html.setup.ai(_) }
    else fuccess {
      Redirect(routes.Lobby.home + "#ai")
    }
  }

  def ai = process(env.forms.ai) { config ⇒
    implicit ctx ⇒
      env.processor ai config map { pov ⇒
        routes.Round.player(pov.fullId)
      }
  }

  def friendForm(username: Option[String]) = Open { implicit ctx ⇒
    if (HTTPRequest isXhr ctx.req)
      username ?? UserRepo.named flatMap { userOption ⇒
        (userOption |@| ctx.me).tupled ?? {
          case (user, me) ⇒ Env.relation.api.blocks(user.id, me.id) map { blocks ⇒
            !blocks option user
          }
        }
      } flatMap { user ⇒
        env.forms friendFilled get("fen") map {
          html.setup.friend(_, user map (_.username))
        }
      }
    else fuccess {
      Redirect(routes.Lobby.home + "#friend")
    }
  }

  def friend(username: Option[String]) = process(env.forms.friend) { config ⇒
    implicit ctx ⇒
      env.processor friend config map { pov ⇒
        routes.Setup.await(pov.fullId, username)
      }
  }

  def decline(gameId: String) = Auth { implicit ctx ⇒
    me ⇒
      OptionResult(GameRepo game gameId) { game ⇒
        if (game.started) BadRequest("Cannot decline started challenge")
        else {
          Env.game.maintenance remove game.id
          Env.hub.actor.challenger ! lila.hub.actorApi.setup.DeclineChallenge(gameId)
          Ok("ok")
        }
      }
  }

  def hookForm = Open { implicit ctx ⇒
    if (HTTPRequest isXhr ctx.req)
      env.forms.hookFilled map { html.setup.hook(_) }
    else fuccess {
      Redirect(routes.Lobby.home + "#hook")
    }
  }

  def hook(uid: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    env.forms.hook(ctx).bindFromRequest.value ?? { config ⇒
      env.processor.hook(config, uid, lila.common.HTTPRequest sid req)
    }
  }

  def filterForm = Open { implicit ctx ⇒
    env.forms.filterFilled map {
      case (form, filter) ⇒ html.setup.filter(form, filter)
    }
  }

  def filter = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    env.forms.filter(ctx).bindFromRequest.fold[Fu[SimpleResult]](
      f ⇒ fulogwarn(f.errors.toString) inject BadRequest(),
      config ⇒ JsonOk(env.processor filter config inject config.render)
    )
  }

  def join(id: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo game id) { game ⇒
      env.friendJoiner(game, ctx.me).fold(
        err ⇒ fulogwarn("setup join " + err) inject
          Redirect(routes.Round.watcher(id, "white")),
        _ map {
          case (p, events) ⇒ {
            Env.hub.socket.round ! lila.hub.actorApi.map.Tell(p.gameId, events)
            Redirect(routes.Round.player(p.fullId))
          }
        })
    }
  }

  def await(fullId: String, username: Option[String]) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        Redirect(routes.Round.player(pov.fullId)).fuccess,
        Env.round.version(pov.gameId) zip
          (username ?? UserRepo.named) flatMap {
            case (version, user) ⇒ PreventTheft(pov) {
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

  def cancel(fullId: String) = Open { implicit ctx ⇒
    OptionResult(GameRepo pov fullId) { pov ⇒
      if (pov.game.started) Redirect(routes.Round.player(pov.fullId))
      else {
        Env.game.maintenance remove pov.game.id
        Redirect(routes.Lobby.home)
      }
    }
  }

  def api = Open { implicit ctx ⇒
    JsonOk(env.processor.api)
  }

  def validateFen = Open { implicit ctx ⇒
    {
      for {
        fen ← get("fen")
        parsed ← chess.format.Forsyth <<< fen
        strict = get("strict").isDefined
        if (parsed.situation playable strict)
        validated = chess.format.Forsyth >> parsed
      } yield html.game.miniBoard(validated, parsed.situation.color.name)
    }.fold[SimpleResult](BadRequest)(Ok(_)).fuccess
  }

  private def process[A](form: Context ⇒ Form[A])(op: A ⇒ BodyContext ⇒ Fu[Call]) =
    OpenBody { ctx ⇒
      implicit val req = ctx.body
      FuRedirect(form(ctx).bindFromRequest.fold(
        f ⇒ fuloginfo(f.errors.toString) inject routes.Lobby.home,
        config ⇒ op(config)(ctx)
      ))
    }
}
