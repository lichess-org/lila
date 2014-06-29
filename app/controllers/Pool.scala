package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.game.GameRepo
import lila.pool.{ Pool => PoolModel }
import views._

object Pool extends LilaController {

  private def env = Env.pool

  def show(id: String) = Open { implicit ctx =>
    OptionFuOk(env.repo byId id) { pool =>
      env version id zip
        chatOf(pool.setup) zip
        pool.userCurrentPov(ctx.me).??(GameRepo.pov) map {
          case ((version, chat), pov) => html.pool.show(pool, version, chat, pov)
        }
    }
  }

  def reload(id: String) = Open { implicit ctx =>
    OptionFuOk(env.repo byId id) { pool =>
      env.api.gamesOf(pool) map { games =>
        html.pool.reload(pool, games)
      }
    }
  }

  def help(id: String) = Open { implicit ctx =>
    OptionOk(fuccess(env.setups get id)) { setup =>
      html.pool.help(setup)
    }
  }

  def enter(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        OptionFuRedirect(env.repo byId id) { pool =>
          env.api.enter(pool, me) inject
            routes.Pool.show(pool.setup.id)
        }
      }
  }

  def leave(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        OptionFuRedirect(env.repo byId id) { pool =>
          env.api.leave(pool, me) inject
            routes.Pool.show(pool.setup.id)
        }
      }
  }

  def websocket(id: String) = Socket[JsValue] { implicit ctx =>
    ~(getInt("version") |@| get("sri") apply {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    })
  }

  private def chatOf(setup: lila.pool.PoolSetup)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find setup.id map (_.forUser(ctx.me).some)
    }

  // private def showStarted(pool)(implicit ctx: Context) =
  //   env.version(tour.id) zip
  //     chatOf(tour) zip
  //     GameRepo.games(tour recentGameIds 4) zip
  //     tour.userCurrentPov(ctx.me).??(GameRepo.pov) map {
  //       case (((version, chat), games), pov) =>
  //         html.tournament.show.started(tour, version, chat, games, pov)
  //     }
}
