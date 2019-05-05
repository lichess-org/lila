package lila.tv

import scala.concurrent.duration._

import lila.common.LightUser
import lila.game.{ Game, Pov }
import lila.hub.Trouper

final class Tv(trouper: Trouper, roundProxyGame: Game.ID => Fu[Option[Game]]) {

  import Tv._
  import ChannelTrouper._

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    trouper.ask[Option[Game.ID]](TvTrouper.GetGameId(channel, _)) flatMap { _ ?? roundProxyGame }

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] =
    trouper.ask[GameIdAndHistory](TvTrouper.GetGameIdAndHistory(channel, _)) flatMap {
      case GameIdAndHistory(gameId, historyIds) => for {
        game <- gameId ?? roundProxyGame
        games <- historyIds.map(roundProxyGame).sequenceFu.map(_.flatten)
        history = games map Pov.first
      } yield game map (_ -> history)
    }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    trouper.ask[List[Game.ID]](TvTrouper.GetGameIds(channel, max, _)) flatMap {
      _.map(roundProxyGame).sequenceFu.map(_.flatten)
    }

  def getBestGame = getGame(Tv.Channel.Best) orElse lila.game.GameRepo.random

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    trouper.ask[Champions](TvTrouper.GetChampions.apply)
}

object Tv {
  import chess.{ Speed => S, variant => V }
  import lila.rating.{ PerfType => P }

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

  sealed abstract class Channel(val name: String, val icon: String, filters: Seq[Candidate => Boolean]) {
    def filter(c: Candidate) = filters forall { _(c) }
    val key = toString.head.toLower + toString.drop(1)
  }
  object Channel {
    case object Best extends Channel(
      name = "Top Rated",
      icon = "C",
      filters = Seq(rated, standard, freshBlitz, noBot)
    )
    case object Bullet extends Channel(
      name = S.Bullet.name,
      icon = P.Bullet.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Bullet), fresh(40), noBot)
    )
    case object Blitz extends Channel(
      name = S.Blitz.name,
      icon = P.Blitz.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Blitz), freshBlitz, noBot)
    )
    case object Rapid extends Channel(
      name = S.Rapid.name,
      icon = P.Rapid.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Rapid), fresh(60 * 5), noBot)
    )
    case object Classical extends Channel(
      name = S.Classical.name,
      icon = P.Classical.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Classical), fresh(60 * 8), noBot)
    )
    case object Chess960 extends Channel(
      name = V.Chess960.name,
      icon = P.Chess960.iconChar.toString,
      filters = Seq(variant(V.Chess960), freshBlitz, noBot)
    )
    case object KingOfTheHill extends Channel(
      name = V.KingOfTheHill.name,
      icon = P.KingOfTheHill.iconChar.toString,
      filters = Seq(variant(V.KingOfTheHill), freshBlitz, noBot)
    )
    case object ThreeCheck extends Channel(
      name = V.ThreeCheck.name,
      icon = P.ThreeCheck.iconChar.toString,
      filters = Seq(variant(V.ThreeCheck), freshBlitz, noBot)
    )
    case object Antichess extends Channel(
      name = V.Antichess.name,
      icon = P.Antichess.iconChar.toString,
      filters = Seq(variant(V.Antichess), freshBlitz, noBot)
    )
    case object Atomic extends Channel(
      name = V.Atomic.name,
      icon = P.Atomic.iconChar.toString,
      filters = Seq(variant(V.Atomic), freshBlitz, noBot)
    )
    case object Horde extends Channel(
      name = V.Horde.name,
      icon = P.Horde.iconChar.toString,
      filters = Seq(variant(V.Horde), freshBlitz, noBot)
    )
    case object RacingKings extends Channel(
      name = V.RacingKings.name,
      icon = P.RacingKings.iconChar.toString,
      filters = Seq(variant(V.RacingKings), freshBlitz, noBot)
    )
    case object Crazyhouse extends Channel(
      name = V.Crazyhouse.name,
      icon = P.Crazyhouse.iconChar.toString,
      filters = Seq(variant(V.Crazyhouse), freshBlitz, noBot)
    )
    case object UltraBullet extends Channel(
      name = S.UltraBullet.name,
      icon = P.UltraBullet.iconChar.toString,
      filters = Seq(rated, standard, speed(S.UltraBullet), fresh(20), noBot)
    )
    case object Bot extends Channel(
      name = "Bot",
      icon = "n",
      filters = Seq(standard, freshBlitz, hasBot)
    )
    case object Computer extends Channel(
      name = "Computer",
      icon = "n",
      filters = Seq(computerFromInitialPosition, freshBlitz)
    )
    val all = List(
      Best,
      Bullet, Blitz, Rapid, Classical,
      Crazyhouse, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings,
      UltraBullet,
      Bot, Computer
    )
    val byKey = all.map { c => c.key -> c }.toMap
  }

  private def rated = (c: Candidate) => c.game.rated
  private def speed(speed: chess.Speed) = (c: Candidate) => c.game.speed == speed
  private def variant(variant: chess.variant.Variant) = (c: Candidate) => c.game.variant == variant
  private val standard = variant(V.Standard)
  private def fresh(seconds: Int) = (c: Candidate) => {
    c.game.isBeingPlayed && !c.game.olderThan(seconds)
  } || {
    c.game.finished && !c.game.olderThan(7)
  } // rematch time
  private val freshBlitz = fresh(60 * 2)
  private def computerFromInitialPosition(c: Candidate) = c.game.hasAi && !c.game.fromPosition
  private def hasBot(c: Candidate) = c.hasBot
  private def noBot(c: Candidate) = !c.hasBot
}
