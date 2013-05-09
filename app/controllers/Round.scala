package controllers

import lila.app._
import views._
import lila.user.{ Context, UserRepo }
import lila.game.{ Pov, GameRepo, Game ⇒ GameModel }
import lila.round.{ RoomRepo, Room }
import lila.round.actorApi.GetGameVersion
import lila.tournament.{ Tournament ⇒ Tourney }

import akka.pattern.ask
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.templates.Html

object Round extends LilaController with TheftPrevention with RoundEventPerformer {

  private def env = Env.round
  // private def rematcher = Env.setup.rematcher
  private def bookmarkApi = Env.bookmark.api
  private def analyser = Env.analyse.analyser
  // private def tournamentRepo = Env.tournament.repo
  private def gameJs = Env.game.gameJs

  def websocketWatcher(gameId: String, color: String) = Socket[JsValue] { implicit ctx ⇒
    (get("sri") |@| getInt("version")).tupled zmap {
      case (uid, version) ⇒ env.socketHandler.watcher(gameId, color, version, uid, ctx)
    }
  }

  def websocketPlayer(fullId: String) = WebSocket.async[JsValue] { implicit req ⇒
    reqToCtx(req) flatMap { implicit ctx ⇒
      (get("sri") |@| getInt("version") |@| get("token")).tupled zmap {
        case (uid, version, token) ⇒ env.socketHandler.player(fullId, version, uid, token, ctx)
      }
    }
  }

  def signedJs(gameId: String) = Open { implicit ctx ⇒
    JsOk(GameRepo token gameId map gameJs.sign, CACHE_CONTROL -> "max-age=3600")
  }

  def player(fullId: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo pov fullId) { pov ⇒
      pov.game.started.fold(
        PreventTheft(pov) {
          (pov.game.hasChat optionFu {
            RoomRepo room pov.gameId map { room ⇒
              html.round.roomInner(room.decodedMessages)
            }
          }) zip
            env.version(pov.gameId) zip
            (bookmarkApi userIdsByGame pov.game) zip
            pov.opponent.userId.zmap(UserRepo.isEngine) zip
            (analyser has pov.gameId) zip
            // TODO (tournamentRepo byId pov.game.tournamentId) zip
            fuccess(none[Tourney]) map {
              case (((((roomHtml, v), bookmarkers), engine), analysed), tour) ⇒
                Ok(html.round.player(
                  pov,
                  v,
                  engine,
                  roomHtml,
                  bookmarkers,
                  analysed,
                  tour = tour))
            }
        },
        Ok("TODO").fuccess
      // TODO Redirect(routes.Setup.await(fullId)).fuccess
      )
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo.pov(gameId, color)) { pov ⇒
      pov.game.started.fold(watch _, join _)(pov)
    }
  }

  private def join(pov: Pov)(implicit ctx: Context): Fu[Result] =
    GameRepo initialFen pov.gameId zip env.version(pov.gameId) map {
      case (fen, version) ⇒ Ok("TODO") // TODO
      // Ok(html.setup.join(
      //   pov, version, env.setup.friendConfigMemo get pov.game.id, fen
      // ))
    }

  private def watch(pov: Pov)(implicit ctx: Context): Fu[Result] =
    bookmarkApi userIdsByGame pov.game zip
      env.version(pov.gameId) zip
      (RoomRepo room pov.gameId map { room ⇒
        html.round.roomInner(room.decodedMessages)
      }) zip
      (analyser has pov.gameId) zip
      fuccess(none[Tourney]) map {
        // TODO (tournamentRepo byId pov.game.tournamentId) map {
        case ((((bookmarkers, v), roomHtml), analysed), tour) ⇒
          Ok(html.round.watcher(
            pov, v, roomHtml, bookmarkers, analysed, tour))
      }

  private def hand = env.hand
  def abort(fullId: String) = performAndRedirect(fullId, hand.abort)
  def resign(fullId: String) = performAndRedirect(fullId, hand.resign)
  def resignForce(fullId: String) = performAndRedirect(fullId, hand.resignForce)
  def drawClaim(fullId: String) = performAndRedirect(fullId, hand.drawClaim)
  def drawAccept(fullId: String) = performAndRedirect(fullId, hand.drawAccept)
  def drawOffer(fullId: String) = performAndRedirect(fullId, hand.drawOffer)
  def drawCancel(fullId: String) = performAndRedirect(fullId, hand.drawCancel)
  def drawDecline(fullId: String) = performAndRedirect(fullId, hand.drawDecline)

  def rematch(fullId: String) = TODO
  // Action {
  //   rematcher offerOrAccept fullId flatMap { validResult ⇒
  //     validResult.fold(
  //       err ⇒ putFailures(err) map { _ ⇒
  //         Redirect(routes.Round.player(fullId))
  //       }, {
  //         case (nextFullId, events) ⇒ performEvents(fullId)(events) map { _ ⇒
  //           Redirect(routes.Round.player(nextFullId))
  //         }
  //       }
  //     )
  //   }
  // }
  def rematchCancel(fullId: String) = performAndRedirect(fullId, hand.rematchCancel)
  def rematchDecline(fullId: String) = performAndRedirect(fullId, hand.rematchDecline)

  def takebackAccept(fullId: String) = performAndRedirect(fullId, hand.takebackAccept)
  def takebackOffer(fullId: String) = performAndRedirect(fullId, hand.takebackOffer)
  def takebackCancel(fullId: String) = performAndRedirect(fullId, hand.takebackCancel)
  def takebackDecline(fullId: String) = performAndRedirect(fullId, hand.takebackDecline)

  def tableWatcher(gameId: String, color: String) = Open { implicit ctx ⇒
    OptionOk(GameRepo.pov(gameId, color)) { html.round.table.watch(_) }
  }

  def tablePlayer(fullId: String) = Open { implicit ctx ⇒
    OptionFuOk(GameRepo pov fullId) { pov ⇒
      fuccess(none[Tourney]) map { tour ⇒
        // TODO tournamentRepo byId pov.game.tournamentId map { tour ⇒
        pov.game.playable.fold(
          html.round.table.playing(pov),
          html.round.table.end(pov, tour))
      }
    }
  }

  def players(gameId: String) = Open { implicit ctx ⇒
    import templating.Environment.playerLink
    JsonOptionOk(GameRepo game gameId map2 { (game: GameModel) ⇒
      (game.players collect {
        case player if player.isHuman ⇒ player.color.name -> playerLink(player).body
      } toMap) ++ ctx.me.zmap(me ⇒ Map("me" -> me.usernameWithElo))
    })
  }
}
