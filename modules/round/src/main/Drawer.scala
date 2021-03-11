package lila.round

import chess.Centis

import lila.common.Bus
import lila.game.{ Event, Game, Pov, Progress }
import lila.i18n.{ I18nKeys => trans, defaultLang }
import lila.pref.{ Pref, PrefApi }

final private[round] class Drawer(
    messenger: Messenger,
    finisher: Finisher,
    prefApi: PrefApi,
    isBotSync: lila.common.LightUser.IsBotSync
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val chatLang = defaultLang

  def autoThreefold(game: Game): Fu[Option[Pov]] = game.playable ??
    Pov(game)
      .map { pov =>
        import Pref.PrefZero
        if (game.playerHasOfferedDraw(pov.color)) fuccess(pov.some)
        else
          pov.player.userId ?? prefApi.getPref map { pref =>
            pref.autoThreefold == Pref.AutoThreefold.ALWAYS || {
              pref.autoThreefold == Pref.AutoThreefold.TIME &&
              game.clock ?? { _.remainingTime(pov.color) < Centis.ofSeconds(30) }
            } || pov.player.userId.exists(isBotSync)
          } map (_ option pov)
      }
      .sequenceFu
      .dmap(_.flatten.headOption)

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov.game.playable ?? {
    pov match {
      case pov if pov.game.history.threefoldRepetition =>
        finisher.other(pov.game, _.Draw, None)
      case pov if pov.opponent.isOfferingDraw =>
        finisher.other(pov.game, _.Draw, None, Some(trans.drawOfferAccepted.txt()))
      case Pov(g, color) if g playerCanOfferDraw color =>
        proxy.save {
          messenger.system(g, color.fold(trans.whiteOffersDraw, trans.blackOffersDraw).txt())
          Progress(g) map { _ offerDraw color }
        } >>- publishDrawOffer(pov) inject List(Event.DrawOffer(by = color.some))
      case _ => fuccess(List(Event.ReloadOwner))
    }
  }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov.game.playable ?? {
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
          messenger.system(g, color.fold(trans.whiteDeclinesDraw, trans.blackDeclinesDraw).txt())
          Progress(g) map { g =>
            g.updatePlayer(!color, _.removeDrawOffer)
          }
        } inject List(Event.DrawOffer(by = none))
      case _ => fuccess(List(Event.ReloadOwner))
    }
  }

  def claim(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    (pov.game.playable && pov.game.history.threefoldRepetition) ?? finisher.other(
      pov.game,
      _.Draw,
      None
    )

  def force(game: Game)(implicit proxy: GameProxy): Fu[Events] = finisher.other(game, _.Draw, None, None)

  private def publishDrawOffer(pov: Pov)(implicit proxy: GameProxy): Unit = {
    if (pov.game.isCorrespondence && pov.game.nonAi)
      Bus.publish(
        lila.hub.actorApi.round.CorresDrawOfferEvent(pov.gameId),
        "offerEventCorres"
      )
    if (lila.game.Game.isBoardCompatible(pov.game))
      proxy
        .withPov(pov.color) { p =>
          fuccess(
            Bus.publish(
              lila.game.actorApi.BoardDrawOffer(p),
              s"boardDrawOffer:${pov.gameId}"
            )
          )
        }
        .unit
  }
}
