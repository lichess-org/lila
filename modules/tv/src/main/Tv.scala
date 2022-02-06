package lila.tv

import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.Trouper

final class Tv(
    gameRepo: GameRepo,
    trouper: Trouper,
    gameProxyRepo: lila.round.GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Tv._
  import ChannelTrouper._

  private def roundProxyGame = gameProxyRepo.game _

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    trouper.ask[Option[Game.ID]](TvTrouper.GetGameId(channel, _)) flatMap { _ ?? roundProxyGame }

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] = {
    trouper.ask[GameIdAndHistory](TvTrouper.GetGameIdAndHistory(channel, _)) flatMap {
      case GameIdAndHistory(gameId, historyIds) => {
        for {
          game <- gameId ?? roundProxyGame
          games <-
            historyIds
              .map { id =>
                roundProxyGame(id) orElse gameRepo.game(id)
              }
              .sequenceFu
              .dmap(_.flatten)
          history = games map Pov.first
        } yield game map (_ -> history)
      }
    }
  }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    trouper.ask[List[Game.ID]](TvTrouper.GetGameIds(channel, max, _)) flatMap {
      _.map(roundProxyGame).sequenceFu.map(_.flatten)
    }

  def getBestGame = getGame(Tv.Channel.Best) orElse gameRepo.random

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    trouper.ask[Champions](TvTrouper.GetChampions.apply)
}

object Tv {
  import shogi.{ Speed => S, variant => V }
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
          icon = "'",
          secondsSinceLastMove = 60 * 3,
          filters = Seq(minRating(1400), standardShogiRules)
        )
    case object Bullet
        extends Channel(
          name = S.Bullet.name,
          icon = P.Bullet.iconChar.toString,
          secondsSinceLastMove = 60,
          filters = Seq(speed(S.Bullet), standardShogiRules)
        )
    case object Blitz
        extends Channel(
          name = S.Blitz.name,
          icon = P.Blitz.iconChar.toString,
          secondsSinceLastMove = 60 * 3,
          filters = Seq(speed(S.Blitz), standardShogiRules)
        )
    case object Rapid
        extends Channel(
          name = S.Rapid.name,
          icon = P.Rapid.iconChar.toString,
          secondsSinceLastMove = 60 * 5,
          filters = Seq(speed(S.Rapid), standardShogiRules)
        )
    case object Classical
        extends Channel(
          name = S.Classical.name,
          icon = P.Classical.iconChar.toString,
          secondsSinceLastMove = 60 * 8,
          filters = Seq(speed(S.Classical), standardShogiRules)
        )
    case object UltraBullet
        extends Channel(
          name = S.UltraBullet.name,
          icon = P.UltraBullet.iconChar.toString,
          secondsSinceLastMove = 30,
          filters = Seq(speed(S.UltraBullet), standardShogiRules)
        )
    case object Minishogi
        extends Channel(
          name = V.Minishogi.name,
          icon = P.Minishogi.iconChar.toString,
          secondsSinceLastMove = 30,
          filters = Seq(variant(V.Minishogi))
        )
    case object Bot
        extends Channel(
          name = "Bot",
          icon = "n",
          secondsSinceLastMove = 60 * 2,
          filters = Seq(standardShogiRules, hasBot)
        )
    case object Computer
        extends Channel(
          name = "Computer",
          icon = "n",
          secondsSinceLastMove = 60 * 2,
          filters = Seq(computerStandardRules)
        )
    val all = List(
      Best,
      Bullet,
      Blitz,
      Rapid,
      Classical,
      UltraBullet,
      Minishogi,
      Bot,
      Computer
    )
    val byKey = all.map { c =>
      c.key -> c
    }.toMap
  }

  //private def rated(min: Int)                         = (c: Candidate) => c.game.rated && hasMinRating(c.game, min)
  private def minRating(min: Int)                     = (c: Candidate) => hasMinRating(c.game, min)
  private def speed(speed: shogi.Speed)               = (c: Candidate) => c.game.speed == speed
  private def variant(variant: shogi.variant.Variant) = (c: Candidate) => c.game.variant == variant
  private def standardShogiRules(c: Candidate)        = c.game.variant == V.Standard
  private def computerStandardRules(c: Candidate)     = c.game.hasAi && standardShogiRules(c)
  private def hasBot(c: Candidate)                    = c.hasBot

  private def fresh(seconds: Int, game: Game): Boolean = {
    game.isBeingPlayed && !game.olderThan(seconds)
  } || {
    game.finished && !game.olderThan(10)
  } // rematch time
  private def hasMinRating(g: Game, min: Int) = g.players.exists(_.rating.exists(_ >= min))
}
