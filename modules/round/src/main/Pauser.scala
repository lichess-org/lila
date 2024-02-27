package lila.round

import scala.concurrent.duration._
import com.github.blemale.scaffeine.Cache

import lila.game.{ Event, Game, GameRepo, Pov, Progress }
import lila.memo.CacheApi
import lila.i18n.{ defaultLang, I18nKeys => trans }

final private[round] class Pauser(
    messenger: Messenger,
    gameRepo: GameRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val chatLang = defaultLang

  private val rateLimit = new lila.memo.RateLimit[String](
    credits = 2,
    duration = 15 minute,
    key = "round.pauser"
  )

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    pov match {
      case Pov(g, color) if pov.opponent.isOfferingPause && !g.prePaused =>
        proxy.save {
          messenger.system(g, trans.adjournmentOfferAccepted.txt())
          Progress(g, g.updatePlayers(_.offerPause))
        } inject List(Event.PauseOffer(by = color.some))
      case Pov(g, color) if g.playerCanOfferPause(color) && rateLimit(pov.fullId)(true)(false).pp("RL") =>
        proxy.save {
          messenger.system(
            g,
            trans.xOffersAdjournment.txt(color.toString).toLowerCase.capitalize + s" (${g.plies}.)"
          )
          Progress(g, g.updatePlayer(color, _.offerPause))
        } inject List(Event.PauseOffer(by = color.some))
      case _ => fuccess(List(Event.ReloadOwner))
    }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    pov match {
      case Pov(g, color) if pov.player.isOfferingPause =>
        proxy.save {
          messenger.system(g, trans.adjournmentOfferCanceled.txt())
          Progress(g, g.updatePlayer(color, _.removePauseOffer))
        } inject List(Event.PauseOffer(by = none))
      case Pov(g, color) if pov.opponent.isOfferingPause =>
        proxy.save {
          messenger.system(g, trans.xDeclinesAdjournment.txt(pov.color))
          Progress(g, g.updatePlayer(!color, _.removePauseOffer))
        } inject List(Event.PauseOffer(by = none))
      case _ => fuccess(List(Event.ReloadOwner))
    }

  private val resumeOffers: Cache[Game.ID, Pauser.ResumeOffers] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(3 minutes)
    .build[Game.ID, Pauser.ResumeOffers]()

  def isOfferingResume(gameId: Game.ID, color: shogi.Color): Boolean =
    resumeOffers.getIfPresent(gameId).exists(_(color))

  private def isOfferingResumeFromPov(pov: Pov): Boolean =
    isOfferingResume(pov.gameId, pov.color)

  def resumeYes(
      pov: Pov
  )(implicit proxy: GameProxy): Fu[(Events, Option[(shogi.format.usi.Usi, Game)])] =
    pov match {
      case Pov(g, color) if g.paused =>
        if (isOfferingResume(g.id, !color)) {
          resumeOffers invalidate g.id
          messenger.system(g, trans.gameResumed.txt())
          val newGame = g.sealedUsi.flatMap(u => g.shogi(u).toOption)
          val prog    = newGame.fold(g.resumeGame(g.shogi))(nsg => g.resumeGame(nsg))
          proxy.save(prog) >>
            gameRepo.resume(g.id, prog.game.pausedSeconds, prog.game.userIds.distinct) inject (
              prog.events -> newGame.flatMap(ng => ng.usiMoves.lastOption.map(u => (u, prog.game)))
            )
        } else {
          resumeOffers.put(g.id, Pauser.ResumeOffers(sente = color.sente, gote = color.gote))
          messenger.system(g, trans.xOffersResumption.txt(color))
          fuccess(List(Event.ResumeOffer(by = color.some)) -> none)
        }
      case _ => fuccess(List(Event.ReloadOwner) -> none)
    }

  def resumeNo(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    if (pov.game.paused) {
      if (isOfferingResumeFromPov(pov)) {
        messenger.system(pov.game, trans.resumptionOfferCanceled.txt())
      } else if (isOfferingResumeFromPov(!pov)) {
        messenger.system(pov.game, trans.xDeclinesResumption.txt(pov.color))
      }
      resumeOffers invalidate pov.gameId
      fuccess(List(Event.ResumeOffer(by = none)))
    } else fuccess(List(Event.ReloadOwner))
}

private object Pauser {

  case class ResumeOffers(sente: Boolean, gote: Boolean) {
    def apply(color: shogi.Color) = color.fold(sente, gote)
  }
}
