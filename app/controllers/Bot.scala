package controllers

import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._

object Bot extends LilaController {

  def gameState(id: String) = Open { _ =>
    Env.round.roundProxyGame(id) flatMap {
      _ ?? { game =>
        Env.bot.jsonView.gameFull(game) map { json =>
          Ok(json) as JSON
        }
      }
    }
  }

  def gameStream(id: String) = Open { ctx =>
    RequireHttp11(ctx.req) {
      Ok.chunked(Env.bot.gameStateStream(id, Env.round.roundProxyGame _)).fuccess
    }
  }

  def move(id: String, uci: String) = Scoped(_.Bot.Play) { req => me =>
    Env.round.roundProxyGame(id) flatMap {
      _ ?? { game =>
        Env.bot.player(game, me, uci) inject jsonOkResult recover {
          case e: Exception => BadRequest(jsonError(e.getMessage))
        }
      }
    }
  }
}
