package lila.round

import lila.game.{ Event, Game, Pov, Progress }
import lila.i18n.{ defaultLang, I18nKeys => trans }

import lila.common.Bus

final private[round] class Drawer(
    messenger: Messenger,
    finisher: Finisher
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val chatLang = defaultLang

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov.game.drawable ?? {
    pov match {
      case pov if pov.opponent.isOfferingDraw =>
        finisher.other(pov.game, _.Draw, None, Some(trans.drawOfferAccepted.txt()))
      case Pov(g, color) if g playerCanOfferDraw color =>
        val progress = Progress(g) map { g =>
          g.updatePlayer(color, _ offerDraw g.plies)
        }
        messenger.system(g, trans.xOffersDraw.txt(color.toString).toLowerCase.capitalize)
        proxy.save(progress) >>-
          publishDrawOffer(progress.game) inject
          List(Event.DrawOffer(by = color.some))
      case _ => fuccess(List(Event.ReloadOwner))
    }
  }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov.game.drawable ?? {
    pov match {
      case Pov(g, color) if pov.player.isOfferingDraw =>
        proxy.save {
          messenger.system(g, trans.drawOfferCanceled.txt())
          Progress(g) map { g =>
            g.updatePlayer(color, _.removeDrawOffer)
          }
        } inject List(Event.DrawOffer(by = none))
      case Pov(g, color) if pov.opponent.isOfferingDraw =>
        proxy.save {
          messenger.system(g, trans.xDeclinesDraw.txt(color.toString).toLowerCase.capitalize)
          Progress(g) map { g =>
            g.updatePlayer(!color, _.removeDrawOffer)
          }
        } inject List(Event.DrawOffer(by = none))
      case _ => fuccess(List(Event.ReloadOwner))
    }
  }

  def claim(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    (pov.game.playable && pov.game.history.fourfoldRepetition) ?? finisher.other(pov.game, _.Draw, None)

  def force(game: Game)(implicit proxy: GameProxy): Fu[Events] = finisher.other(game, _.Draw, None, None)

  private def publishDrawOffer(game: Game): Unit = if (game.nonAi) {
    if (game.isCorrespondence)
      Bus.publish(
        lila.hub.actorApi.round.CorresDrawOfferEvent(game.id),
        "offerEventCorres"
      )
    if (lila.game.Game.isBoardOrBotCompatible(game))
      Bus.publish(
        lila.game.actorApi.BoardDrawOffer(game),
        lila.game.actorApi.BoardDrawOffer makeChan game.id
      )
  }
}
