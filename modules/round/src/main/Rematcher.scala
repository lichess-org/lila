package lila.round

import shogi.{ Clock, Color => ShogiColor, Game => ShogiGame }
import ShogiColor.{ Gote, Sente }
import com.github.blemale.scaffeine.Cache
import lila.memo.CacheApi
import scala.concurrent.duration._

import lila.common.Bus
import lila.game.{ AnonCookie, Event, Game, GameRepo, PerfPicker, Pov, Rematches, Source }
import lila.user.{ User, UserRepo }
import lila.i18n.{ defaultLang, I18nKeys => trans }

final private class Rematcher(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    idGenerator: lila.game.IdGenerator,
    messenger: Messenger,
    onStart: OnStart,
    rematches: Rematches
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val chatLang = defaultLang

  private val declined = new lila.memo.ExpireSetMemo(1 minute)

  private val rateLimit = new lila.memo.RateLimit[String](
    credits = 2,
    duration = 1 minute,
    key = "round.rematch"
  )

  import Rematcher.Offers

  private val offers: Cache[Game.ID, Offers] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(20 minutes)
    .build[Game.ID, Offers]()

  def isOffering(gameId: Game.ID, color: ShogiColor): Boolean =
    offers.getIfPresent(gameId).exists(_(color))

  private def isOfferingFromPov(pov: Pov): Boolean =
    isOffering(pov.gameId, pov.color)

  def yes(pov: Pov): Fu[Events] = {
    pov match {
      case Pov(game, color) if game.playerCouldRematch =>
        if (isOfferingFromPov(!pov) || game.opponent(color).isAi)
          rematches.of(game.id).fold(rematchJoin(pov))(rematchExists(pov))
        else if (!declined.get(pov.flip.fullId) && rateLimit(pov.fullId)(true)(false))
          fuccess(rematchCreate(pov))
        else fuccess(List(Event.RematchOffer(by = none)))
      case _ => fuccess(List(Event.ReloadOwner))
    }
  } addEffect { events =>
    pov.game.postGameStudy.foreach(pgs => publishForPostGameStudy(pgs, events))
  }

  def no(pov: Pov): Fu[Events] = {
    if (isOfferingFromPov(pov)) messenger.system(pov.game, trans.rematchOfferCanceled.txt())
    else if (isOfferingFromPov(!pov)) {
      declined put pov.fullId
      messenger.system(pov.game, trans.rematchOfferDeclined.txt())
    }
    offers invalidate pov.game.id
    fuccess(List(Event.RematchOffer(by = none)))
  } addEffect { events =>
    pov.game.postGameStudy.foreach(pgs => publishForPostGameStudy(pgs, events))
  }

  def publishForPostGameStudy(studyId: String, events: Events) =
    events.foreach {
      case Event.RematchTaken(gameId) =>
        Bus.publish(lila.hub.actorApi.study.RoundRematch(studyId, gameId), "studyRematch")
      case Event.RematchOffer(by) =>
        Bus.publish(lila.hub.actorApi.study.RoundRematchOffer(studyId, by), "studyRematch")
      case _ =>
    }

  private def rematchExists(pov: Pov)(nextId: Game.ID): Fu[Events] =
    gameRepo game nextId flatMap {
      _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))
    }

  private def rematchJoin(pov: Pov): Fu[Events] =
    rematches.of(pov.gameId) match {
      case None =>
        for {
          nextGame <- returnGame(pov) map (_.start)
          _ = offers invalidate pov.game.id
          _ = rematches.cache.put(pov.gameId, nextGame.id)
          _ <- gameRepo insertDenormalized nextGame
        } yield {
          messenger.system(pov.game, trans.rematchOfferAccepted.txt())
          onStart(nextGame.id)
          redirectEvents(nextGame)
        }
      case Some(rematchId) => gameRepo game rematchId map { _ ?? redirectEvents }
    }

  private def rematchCreate(pov: Pov): Events = {
    messenger.system(pov.game, trans.rematchOfferSent.txt())
    pov.opponent.userId foreach { forId =>
      Bus.publish(lila.hub.actorApi.round.RematchOffer(pov.gameId), s"rematchFor:$forId")
    }
    offers.put(pov.gameId, Offers(sente = pov.color.sente, gote = pov.color.gote))
    List(Event.RematchOffer(by = pov.color.some))
  }

  private def returnGame(pov: Pov): Fu[Game] =
    for {
      users <- userRepo byIds pov.game.userIds
      shogiGame = ShogiGame(pov.game.initialSfen, pov.game.variant)
        .copy(clock = pov.game.clock map { c =>
          Clock(c.config)
        })
      sPlayer = returnPlayer(pov.game, Sente, users)
      gPlayer = returnPlayer(pov.game, Gote, users)
      game <- Game.make(
        shogi = shogiGame,
        initialSfen = pov.game.initialSfen,
        sentePlayer = if (pov.game.isHandicap) gPlayer else sPlayer,
        gotePlayer = if (pov.game.isHandicap) sPlayer else gPlayer,
        mode = if (users.exists(_.lame)) shogi.Mode.Casual else pov.game.mode,
        source = pov.game.source | Source.Lobby,
        daysPerTurn = pov.game.daysPerTurn,
        notationImport = None
      ) withUniqueId idGenerator
    } yield game

  private def returnPlayer(game: Game, color: ShogiColor, users: List[User]): lila.game.Player =
    game.opponent(color).engineConfig match {
      case Some(ec) => lila.game.Player.make(color, ec.some)
      case None =>
        lila.game.Player.make(
          color,
          game.opponent(color).userId.flatMap { id =>
            users.find(_.id == id)
          },
          PerfPicker.mainOrDefault(game)
        )
    }

  private def redirectEvents(game: Game): Events = {
    val senteId = game fullIdOf Sente
    val goteId  = game fullIdOf Gote
    List(
      Event.RedirectOwner(if (game.isHandicap) Gote else Sente, goteId, AnonCookie.json(game pov Gote)),
      Event.RedirectOwner(if (game.isHandicap) Sente else Gote, senteId, AnonCookie.json(game pov Sente)),
      // tell spectators about the rematch
      Event.RematchTaken(game.id)
    )
  }

}

private object Rematcher {

  case class Offers(sente: Boolean, gote: Boolean) {
    def apply(color: shogi.Color) = color.fold(sente, gote)
  }
}
