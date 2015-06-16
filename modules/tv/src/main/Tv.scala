package lila.tv

import scala.concurrent.duration._

import akka.actor._
import akka.actor.ActorSelection
import akka.pattern.{ ask, pipe }

import lila.game.{ Game, GameRepo }

final class Tv(
    rendererActor: ActorSelection,
    system: ActorSystem) {

  import Tv._

  implicit private def timeout = makeTimeout(50 millis)

  private[tv] val actor = system.actorOf(Props(classOf[TvActor], rendererActor), name = "tv")

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    (actor ? TvActor.GetGame(channel) mapTo manifest[Option[String]]) recover {
      case e: Exception =>
        logwarn("[TV]" + e.getMessage)
        none
    } flatMap { _ ?? GameRepo.game }

  def getBest = getGame(Tv.Channel.Best)
}

object Tv {
  import chess.{ Speed => S, variant => V }
  import lila.rating.{ PerfType => P }

  sealed abstract class Channel(val name: String, val icon: String, filters: Seq[Game => Boolean]) {
    def filter(g: Game) = filters forall { _(g) }
    val key = toString.head.toLower + toString.drop(1)
  }
  object Channel {
    case object Best extends Channel(
      name = "Skill",
      icon = "C",
      filters = Seq(freshBlitz))
    case object Bullet extends Channel(
      name = S.Bullet.name,
      icon = P.Bullet.iconChar.toString,
      filters = Seq(standard, speed(S.Bullet), fresh(15)))
    case object Blitz extends Channel(
      name = S.Blitz.name,
      icon = P.Blitz.iconChar.toString,
      filters = Seq(standard, speed(S.Blitz), freshBlitz))
    case object Classical extends Channel(
      name = S.Classical.name,
      icon = P.Classical.iconChar.toString,
      filters = Seq(standard, speed(S.Classical), fresh(120)))
    case object Chess960 extends Channel(
      name = V.Chess960.name,
      icon = P.Chess960.iconChar.toString,
      filters = Seq(variant(V.Chess960), freshBlitz))
    case object KingOfTheHill extends Channel(
      name = V.KingOfTheHill.name,
      icon = P.KingOfTheHill.iconChar.toString,
      filters = Seq(variant(V.KingOfTheHill), freshBlitz))
    case object ThreeCheck extends Channel(
      name = V.ThreeCheck.name,
      icon = P.ThreeCheck.iconChar.toString,
      filters = Seq(variant(V.ThreeCheck), freshBlitz))
    case object Antichess extends Channel(
      name = V.Antichess.name,
      icon = P.Antichess.iconChar.toString,
      filters = Seq(variant(V.Antichess), freshBlitz))
    case object Atomic extends Channel(
      name = V.Atomic.name,
      icon = P.Atomic.iconChar.toString,
      filters = Seq(variant(V.Atomic), freshBlitz))
    case object Horde extends Channel(
      name = V.Horde.name,
      icon = P.Horde.iconChar.toString,
      filters = Seq(variant(V.Horde), freshBlitz))
    val all = List(
      Best,
      Bullet, Blitz, Classical,
      Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde)
    val byKey = all.map { c => c.key -> c }.toMap
  }

  private def speed(speed: chess.Speed) = (g: Game) => g.speed == speed
  private def variant(variant: chess.variant.Variant) = (g: Game) => g.variant == variant
  private val standard = variant(V.Standard)
  private def fresh(seconds: Int) = (g: Game) => g.isBeingPlayed && !g.olderThan(seconds)
  private val freshBlitz = fresh(30)
}
