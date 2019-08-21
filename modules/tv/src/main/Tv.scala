package lidraughts.tv

import scala.concurrent.duration._

import lidraughts.common.LightUser
import lidraughts.game.{ Game, Pov, GameRepo }
import lidraughts.hub.Trouper

final class Tv(trouper: Trouper, roundProxyGame: Game.ID => Fu[Option[Game]]) {

  import Tv._
  import ChannelTrouper._

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    trouper.ask[Option[Game.ID]](TvTrouper.GetGameId(channel, _)) flatMap { _ ?? roundProxyGame }

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] =
    trouper.ask[GameIdAndHistory](TvTrouper.GetGameIdAndHistory(channel, _)) flatMap {
      case GameIdAndHistory(gameId, historyIds) => for {
        game <- gameId ?? roundProxyGame
        games <- historyIds.map { id =>
          roundProxyGame(id) orElse GameRepo.game(id)
        }.sequenceFu.map(_.flatten)
        history = games map Pov.first
      } yield game map (_ -> history)
    }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    trouper.ask[List[Game.ID]](TvTrouper.GetGameIds(channel, max, _)) flatMap getGamesFromIds

  def getGamesFromIds(gameIds: List[Game.ID]): Fu[List[Game]] =
    gameIds.map(roundProxyGame).sequenceFu.map(_.flatten)

  def getBestGame = getGame(Tv.Channel.Best) orElse lidraughts.game.GameRepo.random

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    trouper.ask[Champions](TvTrouper.GetChampions.apply)
}

object Tv {
  import draughts.{ Speed => S, variant => V }
  import lidraughts.rating.{ PerfType => P }

  case class Champion(user: LightUser, rating: Int, gameId: Game.ID)
  case class Champions(channels: Map[Channel, Champion]) {
    def get = channels.get _
  }

  private[tv] case class Candidate(game: Game, hasBot: Boolean)
  private[tv] def toCandidate(lightUser: LightUser.GetterSync)(game: Game) = Tv.Candidate(
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
    def isFresh(g: Game): Boolean = fresh(secondsSinceLastMove, g)
    def filter(c: Candidate): Boolean = filters.forall { _(c) } && isFresh(c.game)
    val key = toString.head.toLower + toString.drop(1)
  }
  object Channel {
    case object Best extends Channel(
      name = "Top Rated",
      icon = "C",
      secondsSinceLastMove = freshBlitz,
      filters = Seq(rated(1800), standard, noBot)
    )
    case object Bullet extends Channel(
      name = S.Bullet.name,
      icon = P.Bullet.iconChar.toString,
      secondsSinceLastMove = 35,
      filters = Seq(speed(S.Bullet), rated(1700), standard, noBot)
    )
    case object Blitz extends Channel(
      name = S.Blitz.name,
      icon = P.Blitz.iconChar.toString,
      secondsSinceLastMove = freshBlitz,
      filters = Seq(speed(S.Blitz), rated(1600), standard, noBot)
    )
    case object Rapid extends Channel(
      name = S.Rapid.name,
      icon = P.Rapid.iconChar.toString,
      secondsSinceLastMove = 60 * 5,
      filters = Seq(speed(S.Rapid), rated(1500), standard, noBot)
    )
    case object Classical extends Channel(
      name = S.Classical.name,
      icon = P.Classical.iconChar.toString,
      secondsSinceLastMove = 60 * 8,
      filters = Seq(speed(S.Classical), standard, noBot)
    )
    case object Frisian extends Channel(
      name = V.Frisian.name,
      icon = P.Frisian.iconChar.toString,
      secondsSinceLastMove = freshBlitz,
      filters = Seq(variant(V.Frisian), noBot)
    )
    case object Frysk extends Channel(
      name = V.Frysk.name,
      icon = P.Frysk.iconChar.toString,
      secondsSinceLastMove = freshBlitz,
      filters = Seq(variant(V.Frysk), noBot)
    )
    case object Antidraughts extends Channel(
      name = V.Antidraughts.name,
      icon = P.Antidraughts.iconChar.toString,
      secondsSinceLastMove = freshBlitz,
      filters = Seq(variant(V.Antidraughts), noBot)
    )
    case object Breakthrough extends Channel(
      name = V.Breakthrough.name,
      icon = P.Breakthrough.iconChar.toString,
      secondsSinceLastMove = freshBlitz,
      filters = Seq(variant(V.Breakthrough), noBot)
    )
    case object UltraBullet extends Channel(
      name = S.UltraBullet.name,
      icon = P.UltraBullet.iconChar.toString,
      secondsSinceLastMove = 20,
      filters = Seq(speed(S.UltraBullet), standard, noBot)
    )
    case object Bot extends Channel(
      name = "Bot",
      icon = "n",
      secondsSinceLastMove = freshBlitz,
      filters = Seq(standard, hasBot)
    )
    case object Computer extends Channel(
      name = "Computer",
      icon = "n",
      secondsSinceLastMove = freshBlitz,
      filters = Seq(computerFromInitialPosition)
    )
    val visible = List(
      Best,
      Bullet, Blitz, Rapid, Classical,
      Frisian, Frysk, Antidraughts, Breakthrough,
      UltraBullet,
      Computer
    )
    val all = visible :+ Bot
    val byKey = all.map { c => c.key -> c }.toMap
  }

  private def rated(min: Int) = (c: Candidate) => c.game.rated && hasMinRating(c.game, min)
  private def speed(speed: draughts.Speed) = (c: Candidate) => c.game.speed == speed
  private def variant(variant: draughts.variant.Variant) = (c: Candidate) => c.game.variant == variant
  private val standard = variant(V.Standard)
  private val freshBlitz = 60 * 2
  private def computerFromInitialPosition(c: Candidate) = c.game.hasAi && !c.game.fromPosition
  private def hasBot(c: Candidate) = c.hasBot
  private def noBot(c: Candidate) = !c.hasBot

  private def fresh(seconds: Int, game: Game): Boolean = {
    game.isBeingPlayed && !game.olderThan(seconds)
  } || {
    game.finished && !game.olderThan(7)
  } // rematch time
  private def hasMinRating(g: Game, min: Int) = g.players.exists(_.rating.exists(_ >= min))
}
