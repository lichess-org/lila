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

object Round extends LilaController with TheftPrevention with RoundEventPerformer {

  private def gameRepo = env.game.gameRepo
  private def socket = env.round.socket
  private def hand = env.round.hand
  private def messenger = env.round.messenger
  private def rematcher = env.setup.rematcher
  private def bookmarkApi = env.bookmark.api
  private def userRepo = env.user.userRepo
  private def analyser = env.analyse.analyser
  private def tournamentRepo = env.tournament.repo
  private def gameJs = env.game.gameJs

  def websocketWatcher(gameId: String, color: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.joinWatcher(
      gameId,
      color,
      getInt("version"),
      get("sri"),
      ctx).unsafePerformIO
  }

  def websocketPlayer(fullId: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.joinPlayer(
      fullId,
      getInt("version"),
      get("sri"),
      get("tk2"),
      ctx).unsafePerformIO
  }

  def signedJs(gameId: String) = Open { implicit ctx ⇒
    JsIOk(gameRepo token gameId map gameJs.sign, CACHE_CONTROL -> "max-age=3600")
  }

  def player(fullId: String) = Open { implicit ctx ⇒
    IOptionIOResult(gameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        for {
          roomHtml ← messenger render pov.game
          bookmarkers ← bookmarkApi userIdsByGame pov.game
          engine ← pov.opponent.userId.fold(io(false))(userRepo.isEngine)
          analysed ← analyser has pov.gameId
          tour ← tournamentRepo byId pov.game.tournamentId
        } yield PreventTheft(pov) {
          Ok(html.round.player(
            pov,
            version(pov.gameId),
            engine,
            roomHtml map Html.apply,
            bookmarkers,
            analysed,
            tour = tour))
        },
        io(Redirect(routes.Setup.await(fullId)))
      )
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx ⇒
    IOptionIOResult(gameRepo.pov(gameId, color)) { pov ⇒
      pov.game.started.fold(watch _, join _)(pov)
    }
  }

  private def join(pov: Pov)(implicit ctx: Context): IO[Result] =
    gameRepo initialFen pov.gameId map { initialFen ⇒
      Ok(html.setup.join(
        pov, version(pov.gameId), env.setup.friendConfigMemo get pov.game.id, initialFen
      ))
    }

  private def watch(pov: Pov)(implicit ctx: Context): IO[Result] = for {
    bookmarkers ← bookmarkApi userIdsByGame pov.game
    roomHtml ← messenger renderWatcher pov.game
    analysed ← analyser has pov.gameId
    tour ← tournamentRepo byId pov.game.tournamentId
  } yield Ok(html.round.watcher(pov, version(pov.gameId), Html(roomHtml), bookmarkers, analysed, tour))

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
    IOptionIOk(gameRepo pov fullId) { pov ⇒
      tournamentRepo byId pov.game.tournamentId map { tour ⇒
        pov.game.playable.fold(
          html.round.table.playing(pov),
          html.round.table.end(pov, tour))
      }
    }
  }

  def players(gameId: String) = Open { implicit ctx ⇒
    import templating.Environment.playerLink
    JsonIOk(gameRepo game gameId map { gameOption ⇒
      ~(gameOption map { game ⇒
        (game.players collect {
          case player if player.isHuman ⇒ player.color.name -> playerLink(player).body
        } toMap) ++ ~ctx.me.map(me ⇒ Map("me" -> me.usernameWithElo))
      })
    })
  }

  private def version(gameId: String): Int = socket blockingVersion gameId
}
