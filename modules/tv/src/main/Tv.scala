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
    getGameIds(channel, max) flatMap {
      _.map(roundProxyGame).sequenceFu.map(_.flatten)
    }

  def getGameIds(channel: Tv.Channel, max: Int): Fu[List[Game.ID]] =
    trouper.ask[List[Game.ID]](TvTrouper.GetGameIds(channel, max, _))

  def getBestGame = getGame(Tv.Channel.Standard) orElse gameRepo.randomStandard

  def getBestAndHistory = getGameAndHistory(Tv.Channel.Standard)

  def getChampions: Fu[Champions] =
    trouper.ask[Champions](TvTrouper.GetChampions.apply)
}

object Tv {
  import shogi.{ variant => V }
  import lila.rating.{ PerfType => P }

  case class Champion(user: LightUser, rating: Int, gameId: Game.ID)
  case class Champions(channels: Map[Channel, Champion]) {
    def get = channels.get _
  }

  sealed abstract class Channel(
      val key: String,
      val icon: String,
      filters: Seq[Game => Boolean]
  ) {

    def filter(g: Game): Boolean = filters.forall { _(g) }

  }

  object Channel {

    case object Standard
        extends Channel(
          key = V.Standard.key,
          icon = "C",
          filters = Seq(variant(V.Standard), someHuman)
        )
    case object Minishogi
        extends Channel(
          key = V.Minishogi.key,
          icon = P.Minishogi.iconChar.toString,
          filters = Seq(variant(V.Minishogi), someHuman)
        )
    case object Chushogi
        extends Channel(
          key = V.Chushogi.key,
          icon = P.Chushogi.iconChar.toString,
          filters = Seq(variant(V.Chushogi), someHuman)
        )
    case object Annanshogi
        extends Channel(
          key = V.Annanshogi.key,
          icon = P.Annanshogi.iconChar.toString,
          filters = Seq(variant(V.Annanshogi), someHuman)
        )
    case object Kyotoshogi
        extends Channel(
          key = V.Kyotoshogi.key,
          icon = P.Kyotoshogi.iconChar.toString,
          filters = Seq(variant(V.Kyotoshogi), someHuman)
        )
    case object Checkshogi
        extends Channel(
          key = V.Checkshogi.key,
          icon = P.Checkshogi.iconChar.toString,
          filters = Seq(variant(V.Checkshogi), someHuman)
        )
    case object Computer
        extends Channel(
          key = "computer",
          icon = "n",
          filters = Seq(someComputer)
        )

    val all = List(
      Standard,
      Minishogi,
      Chushogi,
      Annanshogi,
      Kyotoshogi,
      Checkshogi,
      Computer
    )
    val byKey = all.map { c =>
      c.key -> c
    }.toMap
  }

  private def someComputer(g: Game) = g.hasBot || g.hasAi
  private def someHuman(g: Game)    = g.hasHuman

  private def variant(variant: shogi.variant.Variant) =
    (g: Game) => g.variant == variant

}
