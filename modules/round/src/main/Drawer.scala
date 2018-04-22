package lila.round

import lila.game.{ Game, Event, Progress, Pov }
import lila.pref.{ Pref, PrefApi }

import chess.Centis

private[round] final class Drawer(
    messenger: Messenger,
    finisher: Finisher,
    prefApi: PrefApi,
    isBotSync: lila.common.LightUser.IsBotSync,
    bus: lila.common.Bus
) {

  def autoThreefold(game: Game)(implicit proxy: GameProxy): Fu[Option[Pov]] = Pov(game).map { pov =>
    import Pref.PrefZero
    if (game.playerHasOfferedDraw(pov.color)) fuccess(pov.some)
    else pov.player.userId ?? prefApi.getPref map { pref =>
      pref.autoThreefold == Pref.AutoThreefold.ALWAYS || {
        pref.autoThreefold == Pref.AutoThreefold.TIME &&
          game.clock ?? { _.remainingTime(pov.color) < Centis.ofSeconds(30) }
      } || pov.player.userId.exists(isBotSync)
    } map (_ option pov)
  }.sequenceFu map (_.flatten.headOption)

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov match {
    case pov if pov.game.history.threefoldRepetition =>
      finisher.other(pov.game, _.Draw, None)
    case pov if pov.opponent.isOfferingDraw =>
      finisher.other(pov.game, _.Draw, None, Some(_.drawOfferAccepted))
    case Pov(g, color) if g playerCanOfferDraw color => proxy.save {
      messenger.system(g, color.fold(_.whiteOffersDraw, _.blackOffersDraw))
      Progress(g) map { g => g.updatePlayer(color, _ offerDraw g.turns) }
    } >>- publishDrawOffer(pov) inject List(Event.DrawOffer(by = color.some))
    case _ => fuccess(List(Event.ReloadOwner))
  }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov match {
    case Pov(g, color) if pov.player.isOfferingDraw => proxy.save {
      messenger.system(g, _.drawOfferCanceled)
      Progress(g) map { g => g.updatePlayer(color, _.removeDrawOffer) }
    } inject List(Event.DrawOffer(by = none))
    case Pov(g, color) if pov.opponent.isOfferingDraw => proxy.save {
      messenger.system(g, color.fold(_.whiteDeclinesDraw, _.blackDeclinesDraw))
      Progress(g) map { g => g.updatePlayer(!color, _.removeDrawOffer) }
    } inject List(Event.DrawOffer(by = none))
    case _ => fuccess(List(Event.ReloadOwner))
  }

  def claim(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    (pov.game.playable && pov.game.history.threefoldRepetition) ?? finisher.other(pov.game, _.Draw, None)

  def force(game: Game)(implicit proxy: GameProxy): Fu[Events] = finisher.other(game, _.Draw, None, None)

  private def publishDrawOffer(pov: Pov): Unit =
    if (pov.game.isCorrespondence && pov.game.nonAi) bus.publish(
      lila.hub.actorApi.round.CorresDrawOfferEvent(pov.gameId),
      'offerEventCorres
    )
}
