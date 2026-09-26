package lila.tv

import chess.PlayerTitle
import chess.IntRating
import scalalib.actor.SyncActor

import lila.core.LightUser
import lila.game.GameRepo
import lila.ui.Icon

final class Tv(
    gameRepo: GameRepo,
    actor: SyncActor,
    gameProxy: lila.core.game.GameProxy
)(using Executor):

  import Tv.*
  import ChannelSyncActor.*

  def getGame(channel: Tv.Channel): Fu[Option[Game]] =
    actor
      .ask[Option[GameId]](TvSyncActor.GetGameId(channel, _))
      .flatMapz(gameProxy.game)
      .orElse(gameRepo.random)

  def getReplacementGame(channel: Tv.Channel, oldId: GameId, exclude: List[GameId]): Fu[Option[Game]] =
    actor
      .ask[Option[GameId]](TvSyncActor.GetReplacementGameId(channel, oldId, exclude, _))
      .flatMapz(gameProxy.game)

  def getGameAndHistory(channel: Tv.Channel): Fu[Option[(Game, List[Pov])]] =
    actor
      .ask[GameIdAndHistory](TvSyncActor.GetGameIdAndHistory(channel, _))
      .flatMap:
        case GameIdAndHistory(gameId, historyIds) =>
          for
            game <- gameId.so(gameProxy.game).orElse(gameRepo.random)
            games <-
              historyIds
                .traverse: id =>
                  gameProxy.game(id).orElse(gameRepo.game(id))
                .dmap(_.flatten)
            history = games.map(Pov.naturalOrientation)
          yield game.map(_ -> history)

  def getGames(channel: Tv.Channel, max: Int): Fu[List[Game]] =
    getGameIds(channel, max).flatMap:
      _.map(gameProxy.game).parallel.map(_.flatten)

  def getGameIds(channel: Tv.Channel, max: Int): Fu[List[GameId]] =
    actor.ask[List[GameId]](TvSyncActor.GetGameIds(channel, max, _))

  def getBestGame = getGame(Tv.Channel.Best).orElse(gameRepo.random)

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Best)

  def getChampions: Fu[Champions] =
    actor.ask[Champions](TvSyncActor.GetChampions.apply)

object Tv:
  import chess.{ variant as V, Speed as S }
  import lila.rating.PerfType as P

  case class Champion(user: LightUser, rating: IntRating, gameId: GameId, color: Color)
  case class Champions(channels: Map[Channel, Champion]):
    export channels.get

  import play.api.libs.json.*
  import lila.common.Json.given
  given Writes[lila.tv.Tv.Champion] = Json.writes

  private[tv] case class Candidate(game: Game, hasBot: Boolean)

  enum Channel(
      val name: String,
      val icon: Icon,
      val secondsSinceLastMove: Int,
      filters: Seq[Candidate => Boolean],
      val speed: Option[S] = None,
      val variant: Option[V.Variant] = None
  ):
    def isFresh(g: Game): Boolean = fresh(secondsSinceLastMove, g)
    def filter(c: Candidate): Boolean = filters.forall { _(c) } && isFresh(c.game)
    val key = lila.common.String.lcfirst(toString)
    case Best
        extends Channel(
          name = "Top Rated",
          icon = Icon.CrownElite,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(rated(2150), standard, noBot)
        )
    case Bullet
        extends Channel(
          name = S.Bullet.name,
          icon = P.Bullet.icon,
          secondsSinceLastMove = 35,
          filters = Seq(speed(S.Bullet), rated(2000), standard, noBot),
          speed = Some(S.Bullet)
        )
    case Blitz
        extends Channel(
          name = S.Blitz.name,
          icon = P.Blitz.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(speed(S.Blitz), rated(2000), standard, noBot),
          speed = Some(S.Blitz)
        )
    case Rapid
        extends Channel(
          name = S.Rapid.name,
          icon = P.Rapid.icon,
          secondsSinceLastMove = 60 * 5,
          filters = Seq(speed(S.Rapid), rated(1800), standard, noBot),
          speed = Some(S.Rapid)
        )
    case Classical
        extends Channel(
          name = S.Classical.name,
          icon = P.Classical.icon,
          secondsSinceLastMove = 60 * 8,
          filters = Seq(speed(S.Classical), rated(1650), standard, noBot),
          speed = Some(S.Classical)
        )
    case Chess960
        extends Channel(
          name = V.Chess960.name,
          icon = P.Chess960.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Chess960), noBot),
          variant = Some(V.Chess960)
        )
    case KingOfTheHill
        extends Channel(
          name = V.KingOfTheHill.name,
          icon = P.KingOfTheHill.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.KingOfTheHill), noBot),
          variant = Some(V.KingOfTheHill)
        )
    case ThreeCheck
        extends Channel(
          name = V.ThreeCheck.name,
          icon = P.ThreeCheck.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.ThreeCheck), noBot),
          variant = Some(V.ThreeCheck)
        )
    case Antichess
        extends Channel(
          name = V.Antichess.name,
          icon = P.Antichess.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Antichess), noBot),
          variant = Some(V.Antichess)
        )
    case Atomic
        extends Channel(
          name = V.Atomic.name,
          icon = P.Atomic.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Atomic), noBot),
          variant = Some(V.Atomic)
        )
    case Horde
        extends Channel(
          name = V.Horde.name,
          icon = P.Horde.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Horde), noBot),
          variant = Some(V.Horde)
        )
    case RacingKings
        extends Channel(
          name = V.RacingKings.name,
          icon = P.RacingKings.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.RacingKings), noBot),
          variant = Some(V.RacingKings)
        )
    case Crazyhouse
        extends Channel(
          name = V.Crazyhouse.name,
          icon = P.Crazyhouse.icon,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(variant(V.Crazyhouse), noBot),
          variant = Some(V.Crazyhouse)
        )
    case UltraBullet
        extends Channel(
          name = S.UltraBullet.name,
          icon = P.UltraBullet.icon,
          secondsSinceLastMove = 20,
          filters = Seq(speed(S.UltraBullet), rated(1600), standard, noBot),
          speed = Some(S.UltraBullet)
        )
    case Bot
        extends Channel(
          name = "Bot",
          icon = Icon.Cogs,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(standard, hasBot)
        )
    case Computer
        extends Channel(
          name = "Computer",
          icon = Icon.Cogs,
          secondsSinceLastMove = freshBlitz,
          filters = Seq(computerFromInitialPosition)
        )

  object Channel:
    val list = values.toList
    val byKey = values.mapBy(_.key)

  private def rated(min: Int) = (c: Candidate) => c.game.rated.yes && hasMinRating(c.game, IntRating(min))
  private def speed(speed: chess.Speed) = (c: Candidate) => c.game.speed == speed
  private def variant(variant: chess.variant.Variant) = (c: Candidate) => c.game.variant == variant
  private val standard = variant(V.Standard)
  private val freshBlitz = 60 * 2
  private def computerFromInitialPosition(c: Candidate) = c.game.hasAi && !c.game.fromPosition
  private def hasBot(c: Candidate) = c.hasBot
  private def noBot(c: Candidate) = !c.hasBot

  private def olderThan(g: Game, seconds: Int) = g.movedAt.isBefore(nowInstant.minusSeconds(seconds))
  private def fresh(seconds: Int, game: Game): Boolean =
    (game.isBeingPlayed && !olderThan(game, seconds)) ||
      (game.finished && !olderThan(game, 7)) // rematch time

  private def hasMinRating(g: Game, min: IntRating) =
    g.players.exists(_.rating.exists(_ >= min))

  private[tv] val titleScores: Map[PlayerTitle, Int] = Map(
    PlayerTitle.GM -> 600,
    PlayerTitle.WGM -> 600,
    PlayerTitle.IM -> 400,
    PlayerTitle.WIM -> 400,
    PlayerTitle.FM -> 250,
    PlayerTitle.WFM -> 250,
    PlayerTitle.NM -> 150,
    PlayerTitle.CM -> 150,
    PlayerTitle.WCM -> 150,
    PlayerTitle.WNM -> 150
  )
