package controllers

import lila._
import views._
import http.Context
import game.{ DbGame, Pov }
import round.Event
import socket.Util.connectionFail

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.templates.Html
import scalaz.effects._

object Round extends LilaController with TheftPrevention {

  def gameRepo = env.game.gameRepo
  def socket = env.round.socket
  def hand = env.round.hand
  def messenger = env.round.messenger
  def rematcher = env.setup.rematcher
  def joiner = env.setup.friendJoiner
  def bookmarkApi = env.bookmark.api
  def userRepo = env.user.userRepo

  def websocketWatcher(gameId: String, color: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.joinWatcher(
      gameId, color, getInt("version"), get("uid"), ctx.me map (_.username)
    ).unsafePerformIO
  }

  def websocketPlayer(fullId: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.joinPlayer(
      fullId, getInt("version"), get("uid"), ctx.me map (_.username)
    ).unsafePerformIO
  }

  def player(fullId: String) = Open { implicit ctx ⇒
    IOptionIOResult(gameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        for {
          roomHtml ← messenger render pov.game
          bookmarkers ← bookmarkApi usersByGame pov.game
          engine ← pov.opponent.userId.fold(
            u ⇒ userRepo isEngine u,
            io(false))
        } yield PreventTheft(pov) {
          Ok(html.round.player(
            pov,
            version(pov.gameId),
            engine,
            roomHtml map { Html(_) },
            bookmarkers))
        },
        io(Redirect(routes.Setup.await(fullId)))
      )
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx ⇒
    IOptionIOResult(gameRepo.pov(gameId, color)) { pov ⇒
      pov.game.started.fold(watch(pov), join(pov))
    }
  }

  private def watch(pov: Pov)(implicit ctx: Context): IO[Result] = for {
    bookmarkers ← pov.game.hasBookmarks.fold(
      bookmarkApi usersByGame pov.game,
      io(Nil)
    )
    roomHtml ← messenger renderWatcher pov.game
  } yield Ok(html.round.watcher(pov, version(pov.gameId), Html(roomHtml), bookmarkers))

  private def join(pov: Pov)(implicit ctx: Context): IO[Result] =
    joiner(pov.game, ctx.me).fold(
      err ⇒ putFailures(err) flatMap (_ ⇒ watch(pov)),
      _ flatMap {
        case (p, events) ⇒ performEvents(p.gameId)(events) map { _ ⇒
          Redirect(routes.Round.player(p.fullId))
        }
      })

  def abort(fullId: String) = performAndRedirect(fullId, hand.abort)
  def resign(fullId: String) = performAndRedirect(fullId, hand.resign)
  def resignForce(fullId: String) = performAndRedirect(fullId, hand.resignForce)
  def drawClaim(fullId: String) = performAndRedirect(fullId, hand.drawClaim)
  def drawAccept(fullId: String) = performAndRedirect(fullId, hand.drawAccept)
  def drawOffer(fullId: String) = performAndRedirect(fullId, hand.drawOffer)
  def drawCancel(fullId: String) = performAndRedirect(fullId, hand.drawCancel)
  def drawDecline(fullId: String) = performAndRedirect(fullId, hand.drawDecline)

  def rematch(fullId: String) = Action {
    rematcher offerOrAccept fullId flatMap { validResult ⇒
      validResult.fold(
        err ⇒ putFailures(err) map { _ ⇒
          Redirect(routes.Round.player(fullId))
        }, {
          case (nextFullId, events) ⇒ performEvents(fullId)(events) map { _ ⇒
            Redirect(routes.Round.player(nextFullId))
          }
        }
      )
    } unsafePerformIO
  }
  def rematchCancel(fullId: String) = performAndRedirect(fullId, hand.rematchCancel)
  def rematchDecline(fullId: String) = performAndRedirect(fullId, hand.rematchDecline)

  def takebackAccept(fullId: String) = performAndRedirect(fullId, hand.takebackAccept)
  def takebackOffer(fullId: String) = performAndRedirect(fullId, hand.takebackOffer)
  def takebackCancel(fullId: String) = performAndRedirect(fullId, hand.takebackCancel)
  def takebackDecline(fullId: String) = performAndRedirect(fullId, hand.takebackDecline)

  def tableWatcher(gameId: String, color: String) = Open { implicit ctx ⇒
    IOptionOk(gameRepo.pov(gameId, color)) { html.round.table.watch(_) }
  }

  def tablePlayer(fullId: String) = Open { implicit ctx ⇒
    IOptionOk(gameRepo pov fullId) { pov ⇒
      pov.game.playable.fold(
        html.round.table.playing(pov),
        html.round.table.end(pov))
    }
  }

  def players(gameId: String) = Open { implicit ctx ⇒
    import templating.Environment.playerLink
    JsonIOk(gameRepo game gameId map { gameOption ⇒
      gameOption.fold(
        game ⇒ (game.players collect {
          case player if player.isHuman ⇒ player.color.name -> playerLink(player).text
        } toMap) ++ ctx.me.fold(me ⇒ Map("me" -> me.usernameWithElo), Map()),
        Map()
      )
    })
  }

  private type IOValidEvents = IO[Valid[List[Event]]]

  private def performAndRedirect(fullId: String, op: String ⇒ IOValidEvents) =
    Action {
      perform(fullId, op).unsafePerformIO
      Redirect(routes.Round.player(fullId))
    }

  private def perform(fullId: String, op: String ⇒ IOValidEvents): IO[Unit] =
    op(fullId) flatMap { validEvents ⇒
      validEvents.fold(putFailures, performEvents(fullId))
    }

  private def performEvents(fullId: String)(events: List[Event]): IO[Unit] =
    env.round.socket.send(DbGame takeGameId fullId, events)

  private def version(gameId: String): Int = socket blockingVersion gameId
}
