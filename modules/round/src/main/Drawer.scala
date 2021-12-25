package lila.round

import chess.Centis

import lila.common.Bus
import lila.game.{ Event, Game, Pov, Progress }
import lila.i18n.{ defaultLang, I18nKeys => trans }
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
        if (game.playerHasOfferedDrawRecently(pov.color)) fuccess(pov.some)
        else
          pov.player.userId ?? { uid => prefApi.getPref(uid, _.autoThreefold) } map { autoThreefold =>
            autoThreefold == Pref.AutoThreefold.ALWAYS || {
              autoThreefold == Pref.AutoThreefold.TIME &&
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
        finisher.other(pov.game, _.Draw, None, Messenger.Persistent(trans.drawOfferAccepted.txt()).some)
      case Pov(g, color) if g playerCanOfferDraw color =>
        val progress = Progress(g) map { _ offerDraw color }
        messenger.system(g, color.fold(trans.whiteOffersDraw, trans.blackOffersDraw).txt())
        proxy.save(progress) >>-
          publishDrawOffer(progress.game) inject
          List(Event.DrawOffer(by = color.some))
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

  private def publishDrawOffer(game: Game): Unit = if (game.nonAi) {
    if (game.isCorrespondence)
      Bus.publish(
        lila.hub.actorApi.round.CorresDrawOfferEvent(game.id),
        "offerEventCorres"
      )
    if (lila.game.Game.isBoardCompatible(game))
      Bus.publish(
        lila.game.actorApi.BoardDrawOffer(game),
        lila.game.actorApi.BoardDrawOffer makeChan game.id
      )
  }
}
