package lila.round

import chess.Centis
import play.api.i18n.Lang

import lila.common.Bus
import lila.core.i18n.{ I18nKey as trans, Translator, defaultLang }
import lila.game.GameExt.playerCanOfferDraw
import lila.game.{ Event, Progress }
import lila.pref.{ Pref, PrefApi }

final private[round] class Drawer(
    messenger: Messenger,
    finisher: Finisher,
    prefApi: PrefApi,
    isBotSync: lila.core.LightUser.IsBotSync
)(using Executor, Translator):

  private given Lang = defaultLang

  def autoThreefold(game: Game): Fu[Option[Pov]] = game.drawable.so:
    lila.game.Pov
      .list(game)
      .map: pov =>
        if game.playerHasOfferedDrawRecently(pov.color) then fuccess(pov.some)
        else
          pov.player.userId
            .so { uid => prefApi.get(uid, _.autoThreefold) }
            .map { autoThreefold =>
              autoThreefold == Pref.AutoThreefold.ALWAYS || {
                autoThreefold == Pref.AutoThreefold.TIME &&
                game.clock.so { _.remainingTime(pov.color) < Centis.ofSeconds(30) }
              } || pov.player.userId.exists(isBotSync)
            }
            .map(_.option(pov))
      .parallel
      .dmap(_.flatten.headOption)

  def apply(pov: Pov, confirm: Boolean)(using GameProxy): Fu[Events] =
    if confirm then yes(pov) else no(pov)

  def yes(pov: Pov)(using proxy: GameProxy): Fu[Events] = pov.game.drawable.so:
    pov match
      case pov if pov.game.history.threefoldRepetition =>
        finisher.other(pov.game, _.Draw, None)
      case pov if pov.opponent.isOfferingDraw =>
        finisher.other(
          pov.game,
          _.Draw,
          None,
          Messenger.SystemMessage.Persistent(trans.site.drawOfferAccepted.txt()).some
        )
      case Pov(g, color) if g.playerCanOfferDraw(color) =>
        val progress = Progress(g).map { _.offerDraw(color) }
        messenger.system(g, color.fold(trans.site.whiteOffersDraw, trans.site.blackOffersDraw).txt())
        proxy
          .save(progress)
          .andDo(publishDrawOffer(progress.game))
          .inject(List(Event.DrawOffer(by = color.some)))
      case _ => fuccess(List(Event.ReloadOwner))

  def no(pov: Pov)(using proxy: GameProxy): Fu[Events] = pov.game.drawable.so:
    pov match
      case Pov(g, color) if pov.player.isOfferingDraw =>
        proxy
          .save {
            messenger.system(g, trans.site.drawOfferCanceled.txt())
            Progress(g).map: g =>
              g.updatePlayer(color, _.copy(isOfferingDraw = false))
          }
          .inject(List(Event.DrawOffer(by = none)))
      case Pov(g, color) if pov.opponent.isOfferingDraw =>
        proxy
          .save {
            messenger.system(g, color.fold(trans.site.whiteDeclinesDraw, trans.site.blackDeclinesDraw).txt())
            Progress(g).map: g =>
              g.updatePlayer(!color, _.copy(isOfferingDraw = false))
          }
          .inject(List(Event.DrawOffer(by = none)))
      case _ => fuccess(List(Event.ReloadOwner))
    : Fu[Events]

  def claim(pov: Pov)(using GameProxy): Fu[Events] =
    (pov.game.drawable && pov.game.history.threefoldRepetition).so(finisher.other(pov.game, _.Draw, None))

  def force(game: Game)(using GameProxy): Fu[Events] = finisher.other(game, _.Draw, None, None)

  private def publishDrawOffer(game: Game): Unit = if game.nonAi then
    if game.isCorrespondence then
      Bus.publish(
        lila.core.round.CorresDrawOfferEvent(game.id),
        "offerEventCorres"
      )
    if lila.game.Game.isBoardOrBotCompatible(game) then
      Bus.publish(
        lila.game.actorApi.BoardDrawOffer(game),
        lila.game.actorApi.BoardDrawOffer.makeChan(game.id)
      )
