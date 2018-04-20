package lidraughts.tv

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import lidraughts.common.LightUser
import lidraughts.game.{ Game, GameRepo, Pov }

final class Tv(actor: ActorRef, roundProxyGame: Game.ID => Fu[Option[Game]]) {

  import Tv._

  implicit private def timeout = makeTimeout(200 millis)

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    (actor ? TvActor.GetGameId(channel) mapTo manifest[Option[String]]) recover {
      case e: Exception =>
        logger.warn("Tv.getGame", e)
        none
    } flatMap { _ ?? roundProxyGame }

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] =
    (actor ? TvActor.GetGameIdAndHistory(channel) mapTo
      manifest[ChannelActor.GameIdAndHistory]) recover {
        case e: Exception =>
          logger.warn("Tv.getGame", e)
          none
      } flatMap {
        case ChannelActor.GameIdAndHistory(gameId, historyIds) => for {
          game <- gameId ?? roundProxyGame
          games <- historyIds.map(roundProxyGame).sequenceFu.map(_.flatten)
          history = games map Pov.first
        } yield game map (_ -> history)
      }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    (actor ? TvActor.GetGameIds(channel, max) mapTo manifest[List[Game.ID]]) recover {
      case e: Exception => Nil
    } flatMap GameRepo.gamesFromPrimary

  def getBestGame = getGame(Tv.Channel.Best)

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    actor ? TvActor.GetChampions mapTo manifest[Champions]
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
    case object Frisian extends Channel(
      name = V.Frisian.name,
      icon = P.Frisian.iconChar.toString,
      filters = Seq(variant(V.Frisian), freshBlitz, noBot)
    )
    case object Frysk extends Channel(
      name = V.Frysk.name,
      icon = P.Frysk.iconChar.toString,
      filters = Seq(variant(V.Frysk), freshBlitz, noBot)
    )
    case object Antidraughts extends Channel(
      name = V.Antidraughts.name,
      icon = P.Antidraughts.iconChar.toString,
      filters = Seq(variant(V.Antidraughts), freshBlitz, noBot)
    )
    case object Breakthrough extends Channel(
      name = V.Breakthrough.name,
      icon = P.Breakthrough.iconChar.toString,
      filters = Seq(variant(V.Breakthrough), freshBlitz, noBot)
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

  private def rated = (c: Candidate) => c.game.rated
  private def speed(speed: draughts.Speed) = (c: Candidate) => c.game.speed == speed
  private def variant(variant: draughts.variant.Variant) = (c: Candidate) => c.game.variant == variant
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
