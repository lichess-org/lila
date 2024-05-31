package lila.round

import scala.concurrent.duration._
import com.github.blemale.scaffeine.Cache
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

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

  private val dateTimeStyle     = "MS"
  private val dateTimeFormatter = DateTimeFormat forStyle dateTimeStyle

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    pov match {
      case Pov(g, color) if pov.opponent.isOfferingPause && !g.prePaused =>
        proxy.save {
          messenger.system(
            g,
            timestampMessage(trans.adjournmentOfferAccepted.txt(), g.plies)
          )
          Progress(g, g.updatePlayers(_.offerPause))
        } inject List(Event.PauseOffer(by = color.some))
      case Pov(g, color) if g.playerCanOfferPause(color) && rateLimit(pov.fullId)(true)(false) =>
        proxy.save {
          messenger.system(
            g,
            timestampMessage(trans.xOffersAdjournment.txt(color.toString), g.plies)
          )
          Progress(g, g.updatePlayer(color, _.offerPause))
        } inject List(Event.PauseOffer(by = color.some))
      case _ => fuccess(List(Event.ReloadOwner))
    }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    pov match {
      case Pov(g, color) if pov.player.isOfferingPause =>
        proxy.save {
          messenger.system(g, timestampMessage(trans.adjournmentOfferCanceled.txt(), g.plies))
          Progress(g, g.updatePlayer(color, _.removePauseOffer))
        } inject List(Event.PauseOffer(by = none))
      case Pov(g, color) if pov.opponent.isOfferingPause =>
        proxy.save {
          messenger.system(g, timestampMessage(trans.xDeclinesAdjournment.txt(color), g.plies))
          Progress(g, g.updatePlayer(!color, _.removePauseOffer))
        } inject List(Event.PauseOffer(by = none))
      case _ => fuccess(List(Event.ReloadOwner))
    }

  private def timestampMessage(str: String, stepNumber: Int): String =
    s"[${dateTimeFormatter print DateTime.now} ($stepNumber. move)]${str.toLowerCase.capitalize}"

  private val resumeOffers: Cache[Game.ID, Pauser.ResumeOffers] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(3 minutes)
    .build[Game.ID, Pauser.ResumeOffers]()

  def isOfferingResume(gameId: Game.ID, color: shogi.Color): Boolean =
    resumeOffers.getIfPresent(gameId).exists(_(color))

  private def isOfferingResumeFromPov(pov: Pov): Boolean =
    isOfferingResume(pov.gameId, pov.color)

  def resumeYes(
      pov: Pov
  )(implicit proxy: GameProxy): Fu[Either[Events, (shogi.format.usi.Usi, Progress)]] =
    pov match {
      case Pov(g, color) if g.paused =>
        if (isOfferingResume(g.id, !color)) {
          resumeOffers invalidate g.id
          messenger.system(g, trans.gameResumed.txt())
          val prog = Progress(g, g.resume, List(Event.Reload))
          proxy.save(prog) >>
            gameRepo.resume(prog.game.id, prog.game.pausedSeconds, prog.game.userIds.distinct) inject (
              prog.game.usis.lastOption
                .filter(usi => Some(usi) == g.sealedUsi && g.plies < prog.game.plies)
                .fold {
                  messenger.system(g, "Couldn't play sealed move") // should never happen
                  Left(prog.events)
                    .withRight[(shogi.format.usi.Usi, Progress)]
                } { usi =>
                  Right((usi, prog)).withLeft[Events]
                }
            )
        } else if (!isOfferingResume(g.id, color)) {
          resumeOffers.put(g.id, Pauser.ResumeOffers(sente = color.sente, gote = color.gote))
          messenger.system(g, trans.xOffersResumption.txt(color))
          fuccess(Left(List(Event.ResumeOffer(by = color.some))))
        } else fuccess(Left(List(Event.ReloadOwner)))
      case _ => fuccess(Left(List(Event.ReloadOwner)))
    }

  def resumeNo(pov: Pov): Fu[Events] =
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
