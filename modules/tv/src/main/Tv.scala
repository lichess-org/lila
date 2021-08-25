package lila.tv

import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.SyncActor

final class Tv(
    gameRepo: GameRepo,
    trouper: SyncActor,
    gameProxyRepo: lila.round.GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Tv._
  import ChannelSyncActor._

  private def roundProxyGame = gameProxyRepo.game _

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    trouper.ask[Option[Game.ID]](TvSyncActor.GetGameId(channel, _)) flatMap { _ ?? roundProxyGame }

  def getReplacementGame(channel: Tv.Channel, oldId: Game.ID, exclude: List[Game.ID]): Fu[Option[Game]] =
    trouper
      .ask[Option[Game.ID]](TvSyncActor.GetReplacementGameId(channel, oldId, exclude, _))
      .flatMap { _ ?? roundProxyGame }

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] =
    trouper.ask[GameIdAndHistory](TvSyncActor.GetGameIdAndHistory(channel, _)) flatMap {
      case GameIdAndHistory(gameId, historyIds) =>
        for {
          game <- gameId ?? roundProxyGame
          games <-
            historyIds
              .map { id =>
                roundProxyGame(id) orElse gameRepo.game(id)
              }
              .sequenceFu
              .dmap(_.flatten)
          history = games map Pov.naturalOrientation
        } yield game map (_ -> history)
    }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    getGameIds(channel, max) flatMap {
      _.map(roundProxyGame).sequenceFu.map(_.flatten)
    }

  def getGameIds(channel: Tv.Channel, max: Int): Fu[List[Game.ID]] =
    trouper.ask[List[Game.ID]](TvSyncActor.GetGameIds(channel, max, _))

  def getBestGame = getGame(Tv.Channel.Best) orElse gameRepo.random

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    trouper.ask[Champions](TvSyncActor.GetChampions.apply)
}

object Tv {
  import chess.{ Speed => S, variant => V }
  import lila.rating.{ PerfType => P }

  case class Champion(user: LightUser, rating: Int, gameId: Game.ID)
  case class Champions(channels: Map[Channel, Champion]) {
    def get = channels.get _
  }

  private[tv] case class Candidate(game: Game, hasBot: Boolean)
  private[tv] def toCandidate(lightUser: LightUser.GetterSync)(game: Game) =
    Tv.Candidate(
      game = game,
      hasBot = game.userIds.exists { userId =>
        lightUser(userId).exists(_.isBot)
      }
    )

  sealed abstract class Channel(
      val name: String,
      val icon: String,
      val secondsSinceLastMove: Int,
      filters: Seq[Candidate => Boolean]
  ) {
    def isFresh(g: Game): Boolean     = fresh(secondsSinceLastMove, g)
    def filter(c: Candidate): Boolean = filters.forall { _(c) } && isFresh(c.game)
    val key                           = s"${toString.head.toLower}${toString.drop(1)}"
  }
  object Channel {
    case object Best
        extends Channel(
          name = "Top Rated",
          icon = "",
          secondsSinceLastMove = freshBlitz,
          filters = Seq(rated(2150), standard, noBot)
        )
    case object Bullet
        extends Channel(
          name = S.Bullet.name,
          icon = P.Bullet.iconChar.toString,
          secondsSinceLastMove = 35,
          filters = Seq(speed(S.Bullet), rated(2000), standard, noBot)
        )
    case object Blitz
        extends Channel(
          name = S.Blitz.name,
          icon = P.Blitz.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(speed(S.Blitz), rated(2000), standard, noBot)
        )
    case object Rapid
        extends Channel(
          name = S.Rapid.name,
          icon = P.Rapid.iconChar.toString,
          secondsSinceLastMove = 60 * 5,
          filters = Seq(speed(S.Rapid), rated(1800), standard, noBot)
        )
    case object Classical
        extends Channel(
          name = S.Classical.name,
          icon = P.Classical.iconChar.toString,
          secondsSinceLastMove = 60 * 8,
          filters = Seq(speed(S.Classical), rated(1650), standard, noBot)
        )
    case object Chess960
        extends Channel(
          name = V.Chess960.name,
          icon = P.Chess960.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Chess960), noBot)
        )
    case object KingOfTheHill
        extends Channel(
          name = V.KingOfTheHill.name,
          icon = P.KingOfTheHill.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.KingOfTheHill), noBot)
        )
    case object ThreeCheck
        extends Channel(
          name = V.ThreeCheck.name,
          icon = P.ThreeCheck.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.ThreeCheck), noBot)
        )
    case object Antichess
        extends Channel(
          name = V.Antichess.name,
          icon = P.Antichess.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Antichess), noBot)
        )
    case object Atomic
        extends Channel(
          name = V.Atomic.name,
          icon = P.Atomic.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Atomic), noBot)
        )
    case object Horde
        extends Channel(
          name = V.Horde.name,
          icon = P.Horde.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Horde), noBot)
        )
    case object RacingKings
        extends Channel(
          name = V.RacingKings.name,
          icon = P.RacingKings.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.RacingKings), noBot)
        )
    case object Crazyhouse
        extends Channel(
          name = V.Crazyhouse.name,
          icon = P.Crazyhouse.iconChar.toString,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Crazyhouse), noBot)
        )
    case object UltraBullet
        extends Channel(
          name = S.UltraBullet.name,
          icon = P.UltraBullet.iconChar.toString,
          secondsSinceLastMove = 20,
          filters = Seq(speed(S.UltraBullet), rated(1600), standard, noBot)
        )
    case object Bot
        extends Channel(
          name = "Bot",
          icon = "",
          secondsSinceLastMove = freshBlitz,
          filters = Seq(standard, hasBot)
        )
    case object Computer
        extends Channel(
          name = "Computer",
          icon = "",
          secondsSinceLastMove = freshBlitz,
          filters = Seq(computerFromInitialPosition)
        )
    val all = List(
      Best,
      Bullet,
      Blitz,
      Rapid,
      Classical,
      Crazyhouse,
      Chess960,
      KingOfTheHill,
      ThreeCheck,
      Antichess,
      Atomic,
      Horde,
      RacingKings,
      UltraBullet,
      Bot,
      Computer
    )
    val byKey = all.map { c =>
      c.key -> c
    }.toMap
  }

  private def rated(min: Int)                           = (c: Candidate) => c.game.rated && hasMinRating(c.game, min)
  private def speed(speed: chess.Speed)                 = (c: Candidate) => c.game.speed == speed
  private def variant(variant: chess.variant.Variant)   = (c: Candidate) => c.game.variant == variant
  private val standard                                  = variant(V.Standard)
  private val freshBlitz                                = 60 * 2
  private def computerFromInitialPosition(c: Candidate) = c.game.hasAi && !c.game.fromPosition
  private def hasBot(c: Candidate)                      = c.hasBot
  private def noBot(c: Candidate)                       = !c.hasBot

  private def fresh(seconds: Int, game: Game): Boolean = {
    game.isBeingPlayed && !game.olderThan(seconds)
  } || {
    game.finished && !game.olderThan(7)
  } // rematch time
  private def hasMinRating(g: Game, min: Int) = g.players.exists(_.rating.exists(_ >= min))

  private[tv] val titleScores = Map(
    "GM"  -> 500,
    "WGM" -> 500,
    "IM"  -> 300,
    "WIM" -> 300,
    "FM"  -> 200,
    "WFM" -> 200,
    "NM"  -> 100,
    "CM"  -> 100,
    "WCM" -> 100,
    "WNM" -> 100
  )
}
