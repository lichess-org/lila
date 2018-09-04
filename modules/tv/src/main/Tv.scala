package lidraughts.tv

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import lidraughts.common.LightUser
import lidraughts.game.{ Game, GameRepo, Pov }

final class Tv(actor: ActorRef) {

  import Tv._

  implicit private def timeout = makeTimeout(200 millis)

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    (actor ? TvActor.GetGameId(channel) mapTo manifest[Option[String]]) recover {
      case e: Exception =>
        logger.warn("Tv.getGame", e)
        none
    } flatMap { _ ?? GameRepo.game }

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] =
    (actor ? TvActor.GetGameIdAndHistory(channel) mapTo
      manifest[ChannelActor.GameIdAndHistory]) recover {
        case e: Exception =>
          logger.warn("Tv.getGame", e)
          none
      } flatMap {
        case ChannelActor.GameIdAndHistory(gameId, historyIds) => for {
          game <- gameId ?? GameRepo.game
          games <- GameRepo gamesFromPrimary historyIds
          history = games map Pov.first
        } yield game map (_ -> history)
      }

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    (actor ? TvActor.GetGameIds(channel, max) mapTo manifest[List[String]]) recover {
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

  sealed abstract class Channel(val name: String, val icon: String, filters: Seq[Game => Boolean]) {
    def filter(g: Game) = filters forall { _(g) }
    val key = toString.head.toLower + toString.drop(1)
  }
  object Channel {
    case object Best extends Channel(
      name = "Top Rated",
      icon = "C",
      filters = Seq(rated, standard, freshBlitz)
    )
    case object Bullet extends Channel(
      name = S.Bullet.name,
      icon = P.Bullet.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Bullet), fresh(40))
    )
    case object Blitz extends Channel(
      name = S.Blitz.name,
      icon = P.Blitz.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Blitz), freshBlitz)
    )
    case object Rapid extends Channel(
      name = S.Rapid.name,
      icon = P.Rapid.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Rapid), fresh(60 * 5))
    )
    case object Classical extends Channel(
      name = S.Classical.name,
      icon = P.Classical.iconChar.toString,
      filters = Seq(rated, standard, speed(S.Classical), fresh(60 * 8))
    )
    case object Frisian extends Channel(
      name = V.Frisian.name,
      icon = P.Frisian.iconChar.toString,
      filters = Seq(variant(V.Frisian), freshBlitz)
    )
    case object Antidraughts extends Channel(
      name = V.Antidraughts.name,
      icon = P.Antidraughts.iconChar.toString,
      filters = Seq(variant(V.Antidraughts), freshBlitz)
    )
    case object UltraBullet extends Channel(
      name = S.UltraBullet.name,
      icon = P.UltraBullet.iconChar.toString,
      filters = Seq(rated, standard, speed(S.UltraBullet), fresh(20))
    )
    case object Computer extends Channel(
      name = "Computer",
      icon = "n",
      filters = Seq(computerFromInitialPosition, freshBlitz)
    )
    val all = List(
      Best,
      Bullet, Blitz, Rapid, Classical,
      Frisian, Antidraughts,
      UltraBullet
    //Computer
    )
    val byKey = all.map { c => c.key -> c }.toMap
  }

  private def rated = (g: Game) => g.rated
  private def speed(speed: draughts.Speed) = (g: Game) => g.speed == speed
  private def variant(variant: draughts.variant.Variant) = (g: Game) => g.variant == variant
  private val standard = variant(V.Standard)
  private def fresh(seconds: Int) = (g: Game) => {
    g.isBeingPlayed && !g.olderThan(seconds)
  } || {
    g.finished && !g.olderThan(7)
  } // rematch time
  private val freshBlitz = fresh(60 * 2)
  private def computerFromInitialPosition = (g: Game) => g.hasAi && !g.fromPosition
}
