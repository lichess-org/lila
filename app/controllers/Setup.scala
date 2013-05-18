package controllers

import lila.app._
import lila.user.{ Context, BodyContext }
import lila.game.GameRepo
import views._

import play.api.mvc.{ Result, Call }
import play.api.data.Form

object Setup extends LilaController with TheftPrevention with RoundEventPerformer {

  private def env = Env.setup

  def aiForm = Open { implicit ctx ⇒
    env.forms aiFilled get("fen") map { html.setup.ai(_) }
  }

  def ai = process(env.forms.ai) { config ⇒
    implicit ctx ⇒
      env.processor ai config map { pov ⇒
        routes.Round.player(pov.fullId)
      }
  }

  def friendForm = Open { implicit ctx ⇒
    env.forms friendFilled get("fen") map { html.setup.friend(_) }
  }

  def friend = process(env.forms.friend) { config ⇒
    implicit ctx ⇒
      env.processor friend config map { pov ⇒
        routes.Setup.await(pov.fullId)
      }
  }

  def hookForm = Open { implicit ctx ⇒
    env.forms.hookFilled map { html.setup.hook(_) }
  }

  def hook = process(env.forms.hook) { config ⇒
    implicit ctx ⇒
      env.processor hook config map { hook ⇒
        routes.Lobby.hook(hook.ownerId)
      }
  }

  def filterForm = Open { implicit ctx ⇒
    env.forms.filterFilled map { html.setup.filter(_) }
  }

  def filter = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    env.forms.filter(ctx).bindFromRequest.fold[Fu[Result]](
      f ⇒ fulogwarn(f.errors.toString) inject BadRequest(),
      config ⇒ JsonOk(env.processor filter config inject config.render)
    )
  }

  def join(id: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo game id) { game ⇒
      env.friendJoiner(game, ctx.me).fold(
        err ⇒ fulogwarn("setup join " + err) inject
          Redirect(routes.Round.watcher(id, game.creatorColor.name)),
        _ map {
          case (p, events) ⇒ {
            sendEvents(p.gameId)(events)
            Redirect(routes.Round.player(p.fullId))
          }
        })
    }
  }

  def await(fullId: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        Redirect(routes.Round.player(pov.fullId)).fuccess,
        Env.round.version(pov.gameId) flatMap { version ⇒
          PreventTheft(pov) {
            Ok(html.setup.await(
              pov,
              version,
              env.friendConfigMemo get pov.game.id)).fuccess
          }
        }
      )
    }
  }

  def cancel(fullId: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        Redirect(routes.Round.player(pov.fullId)).fuccess,
        (GameRepo remove pov.game.id) >>
          (Env.bookmark.api removeByGame pov.game) inject
          Redirect(routes.Lobby.home)
      )
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
    }.fold[Result](BadRequest)(Ok(_)).fuccess
  }

  private def process[A](form: Context ⇒ Form[A])(op: A ⇒ BodyContext ⇒ Fu[Call]) =
    OpenBody { ctx ⇒
      implicit val req = ctx.body
      FuRedirect(form(ctx).bindFromRequest.fold(
        f ⇒ fulogwarn(f.errors.toString) inject routes.Lobby.home,
        config ⇒ op(config)(ctx)
      ))
    }
}
