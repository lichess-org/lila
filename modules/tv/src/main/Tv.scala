package lila.tv

import lila.common.licon
import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.SyncActor

final class Tv(
    gameRepo: GameRepo,
    trouper: SyncActor,
    gameProxyRepo: lila.round.GameProxyRepo
)(using Executor):

  import Tv.*
  import ChannelSyncActor.*

  private def roundProxyGame = gameProxyRepo.game

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    trouper.ask[Option[GameId]](TvSyncActor.GetGameId(channel, _)) flatMapz roundProxyGame

  def getReplacementGame(channel: Tv.Channel, oldId: GameId, exclude: List[GameId]): Fu[Option[Game]] =
    trouper
      .ask[Option[GameId]](TvSyncActor.GetReplacementGameId(channel, oldId, exclude, _))
      .flatMapz(roundProxyGame)

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
              .parallel
              .dmap(_.flatten)
          history = games map Pov.naturalOrientation
        } yield game map (_ -> history)
    }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    getGameIds(channel, max) flatMap {
      _.map(roundProxyGame).parallel.map(_.flatten)
    }

  def getGameIds(channel: Tv.Channel, max: Int): Fu[List[GameId]] =
    trouper.ask[List[GameId]](TvSyncActor.GetGameIds(channel, max, _))

  def getBestGame = getGame(Tv.Channel.Best) orElse gameRepo.random

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    trouper.ask[Champions](TvSyncActor.GetChampions.apply)

object Tv:
  import chess.{ variant as V, Speed as S }
  import lila.rating.{ PerfType as P }

  case class Champion(user: LightUser, rating: IntRating, gameId: GameId)
  case class Champions(channels: Map[Channel, Champion]):
    def get = channels.get

  private[tv] case class Candidate(game: Game, hasBot: Boolean)

  sealed abstract class Channel(
      val name: String,
      val icon: licon.Icon,
      val secondsSinceLastMove: Int,
      filters: Seq[Candidate => Boolean]
  ):
    def isFresh(g: Game): Boolean     = fresh(secondsSinceLastMove, g)
    def filter(c: Candidate): Boolean = filters.forall { _(c) } && isFresh(c.game)
    val key                           = s"${toString.head.toLower}${toString.drop(1)}"
  object Channel:
    case object Best
        extends Channel(
          name = "Top Rated",
          icon = licon.CrownElite,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(rated(2150), standard, noBot)
        )
    case object Bullet
        extends Channel(
          name = S.Bullet.name,
          icon = P.Bullet.icon,
          secondsSinceLastMove = 35,
          filters = Seq(speed(S.Bullet), rated(2000), standard, noBot)
        )
    case object Blitz
        extends Channel(
          name = S.Blitz.name,
          icon = P.Blitz.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(speed(S.Blitz), rated(2000), standard, noBot)
        )
    case object Rapid
        extends Channel(
          name = S.Rapid.name,
          icon = P.Rapid.icon,
          secondsSinceLastMove = 60 * 5,
          filters = Seq(speed(S.Rapid), rated(1800), standard, noBot)
        )
    case object Classical
        extends Channel(
          name = S.Classical.name,
          icon = P.Classical.icon,
          secondsSinceLastMove = 60 * 8,
          filters = Seq(speed(S.Classical), rated(1650), standard, noBot)
        )
    case object Chess960
        extends Channel(
          name = V.Chess960.name,
          icon = P.Chess960.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Chess960), noBot)
        )
    case object KingOfTheHill
        extends Channel(
          name = V.KingOfTheHill.name,
          icon = P.KingOfTheHill.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.KingOfTheHill), noBot)
        )
    case object ThreeCheck
        extends Channel(
          name = V.ThreeCheck.name,
          icon = P.ThreeCheck.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.ThreeCheck), noBot)
        )
    case object Antichess
        extends Channel(
          name = V.Antichess.name,
          icon = P.Antichess.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Antichess), noBot)
        )
    case object Atomic
        extends Channel(
          name = V.Atomic.name,
          icon = P.Atomic.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Atomic), noBot)
        )
    case object Horde
        extends Channel(
          name = V.Horde.name,
          icon = P.Horde.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Horde), noBot)
        )
    case object RacingKings
        extends Channel(
          name = V.RacingKings.name,
          icon = P.RacingKings.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.RacingKings), noBot)
        )
    case object Crazyhouse
        extends Channel(
          name = V.Crazyhouse.name,
          icon = P.Crazyhouse.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Crazyhouse), noBot)
        )
    case object UltraBullet
        extends Channel(
          name = S.UltraBullet.name,
          icon = P.UltraBullet.icon,
          secondsSinceLastMove = 20,
          filters = Seq(speed(S.UltraBullet), rated(1600), standard, noBot)
        )
    case object Bot
        extends Channel(
          name = "Bot",
          icon = licon.Cogs,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(standard, hasBot)
        )
    case object Computer
        extends Channel(
          name = "Computer",
          icon = licon.Cogs,
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
    val byKey = all.mapBy(_.key)

  private def rated(min: Int)           = (c: Candidate) => c.game.rated && hasMinRating(c.game, min)
  private def speed(speed: chess.Speed) = (c: Candidate) => c.game.speed == speed
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

  private[tv] val titleScores: Map[UserTitle, Int] = Map(
    UserTitle("GM")  -> 500,
    UserTitle("WGM") -> 500,
    UserTitle("IM")  -> 300,
    UserTitle("WIM") -> 300,
    UserTitle("FM")  -> 200,
    UserTitle("WFM") -> 200,
    UserTitle("NM")  -> 100,
    UserTitle("CM")  -> 100,
    UserTitle("WCM") -> 100,
    UserTitle("WNM") -> 100
  )
