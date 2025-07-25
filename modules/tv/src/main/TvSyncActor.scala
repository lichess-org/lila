package lila.tv

import play.api.libs.json.Json
import scalalib.actor.SyncActor

import lila.common.Bus
import lila.common.Json.given

final private class TvSyncActor(
    lightUserApi: lila.core.user.LightUserApi,
    onTvGame: lila.game.core.OnTvGame,
    gameProxy: lila.core.game.GameProxy,
    rematches: lila.game.Rematches,
    userApi: lila.core.user.UserApi
)(using Executor, Scheduler)
    extends SyncActor:

  import TvSyncActor.*

  Bus.subscribeActor[lila.core.game.StartGame](this)

  private val channelActors: Map[Tv.Channel, ChannelSyncActor] = Tv.Channel.values.map { c =>
    c -> ChannelSyncActor(
      c,
      onSelect = this.!,
      gameProxy,
      rematches.getAcceptedId,
      lightUserApi.sync,
      userApi
    )
  }.toMap

  private var channelChampions = Map[Tv.Channel, Tv.Champion]()

  private def forward[A](channel: Tv.Channel, msg: Any) =
    channelActors.get(channel).foreach { _ ! msg }

  protected val process: SyncActor.Receive =

    case GetGameId(channel, promise) =>
      forward(channel, ChannelSyncActor.GetGameId(promise))

    case GetGameIdAndHistory(channel, promise) =>
      forward(channel, ChannelSyncActor.GetGameIdAndHistory(promise))

    case GetGameIds(channel, max, promise) =>
      forward(channel, ChannelSyncActor.GetGameIds(max, promise))

    case GetReplacementGameId(channel, oldId, exclude, promise) =>
      forward(channel, ChannelSyncActor.GetReplacementGameId(oldId, exclude, promise))

    case GetChampions(promise) => promise.success(Tv.Champions(channelChampions))

    case lila.core.game.StartGame(g) =>
      if g.hasClock then
        val candidate = Tv.Candidate(g, g.userIds.exists(lightUserApi.isBotSync))
        channelActors
          .collect:
            case (chan, actor) if chan.filter(candidate) => actor
          .foreach(_.addCandidate(g))

    case s @ TvSyncActor.Select => channelActors.foreach(_._2 ! s)

    case Selected(channel, game) =>
      import lila.core.socket.makeMessage
      given Ordering[lila.core.game.Player] = Ordering.by: p =>
        p.rating.fold(0)(_.value) + ~p.userId
          .flatMap(lightUserApi.sync)
          .flatMap(_.title)
          .flatMap(Tv.titleScores.get)
      val player = game.players.all.sorted.lastOption | game.player(game.naturalOrientation)
      val user = player.userId.flatMap(lightUserApi.sync)
      (user, player.rating).mapN: (u, r) =>
        channelChampions += (channel -> Tv.Champion(u, r, game.id, game.naturalOrientation))
      onTvGame(game)
      val data = Json.obj(
        "channel" -> channel.key,
        "id" -> game.id,
        "color" -> game.naturalOrientation.name,
        "player" -> user.map: u =>
          Json.obj(
            "name" -> u.name,
            "title" -> u.title,
            "rating" -> player.rating
          )
      )
      Bus.pub(lila.core.game.TvSelect(game.id, game.speed, channel.key, data))
      if channel == Tv.Channel.Best then
        lila.common.Bus
          .ask[Html, RenderFeaturedJs](RenderFeaturedJs(game, _))
          .foreach: html =>
            Bus.pub:
              lila.core.game.ChangeFeatured:
                makeMessage(
                  "featured",
                  Json.obj(
                    "html" -> html,
                    "color" -> game.naturalOrientation.name,
                    "id" -> game.id
                  )
                )

private object TvSyncActor:

  case class GetGameId(channel: Tv.Channel, promise: Promise[Option[GameId]])
  case class GetGameIds(channel: Tv.Channel, max: Int, promise: Promise[List[GameId]])
  case class GetReplacementGameId(
      channel: Tv.Channel,
      oldId: GameId,
      exclude: List[GameId],
      promise: Promise[Option[GameId]]
  )

  case class GetGameIdAndHistory(channel: Tv.Channel, promise: Promise[ChannelSyncActor.GameIdAndHistory])

  case object Select
  case class Selected(channel: Tv.Channel, game: Game)

  case class GetChampions(promise: Promise[Tv.Champions])
