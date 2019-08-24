package lidraughts.tv

import akka.actor._
import akka.pattern.{ ask => actorAsk }
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.concurrent.Promise

import lidraughts.common.LightUser
import lidraughts.game.Game
import lidraughts.hub.Trouper

private[tv] final class TvTrouper(
    system: ActorSystem,
    rendererActor: ActorSelection,
    selectChannel: lidraughts.socket.Channel,
    lightUser: LightUser.GetterSync,
    onSelect: Game => Unit,
    proxyGame: Game.ID => Fu[Option[Game]],
    rematchOf: Game.ID => Option[Game.ID]
) extends Trouper {

  import TvTrouper._

  system.lidraughtsBus.subscribe(this, 'startGame)

  private val channelTroupers: Map[Tv.Channel, ChannelTrouper] = Tv.Channel.all.map { c =>
    c -> new ChannelTrouper(c, lightUser, onSelect = this.!, proxyGame, rematchOf)
  }.toMap

  private var channelChampions = Map[Tv.Channel, Tv.Champion]()

  private def forward[A](channel: Tv.Channel, msg: Any) =
    channelTroupers get channel foreach { _ ! msg }

  protected val process: Trouper.Receive = {

    case GetGameId(channel, promise) =>
      forward(channel, ChannelTrouper.GetGameId(promise))

    case GetGameIdAndHistory(channel, promise) =>
      forward(channel, ChannelTrouper.GetGameIdAndHistory(promise))

    case GetGameIds(channel, max, promise) =>
      forward(channel, ChannelTrouper.GetGameIds(max, promise))

    case GetChampions(promise) => promise success Tv.Champions(channelChampions)

    case lidraughts.game.actorApi.StartGame(g) => if (g.hasClock) {
      val candidate = Tv.toCandidate(lightUser)(g)
      channelTroupers collect {
        case (chan, trouper) if chan filter candidate => trouper
      } foreach (_ addCandidate g)
    }

    case s @ TvTrouper.Select => channelTroupers.foreach(_._2 ! s)

    case Selected(channel, game) =>
      import lidraughts.socket.Socket.makeMessage
      val player = game.firstPlayer
      val user = player.userId flatMap lightUser
      (user |@| player.rating) apply {
        case (u, r) => channelChampions += (channel -> Tv.Champion(u, r, game.id))
      }
      onSelect(game)
      selectChannel ! lidraughts.socket.Channel.Publish(makeMessage("tvSelect", Json.obj(
        "channel" -> channel.key,
        "id" -> game.id,
        "color" -> game.firstColor.name,
        "player" -> user.map { u =>
          Json.obj(
            "name" -> u.name,
            "title" -> u.title,
            "rating" -> player.rating
          )
        }
      )))
      if (channel == Tv.Channel.Best) {
        implicit def timeout = makeTimeout(100 millis)
        actorAsk(rendererActor, actorApi.RenderFeaturedJs(game)) onSuccess {
          case html: String =>
            val event = lidraughts.hub.actorApi.game.ChangeFeatured(
              game.id,
              makeMessage("featured", Json.obj(
                "html" -> html,
                "color" -> game.firstColor.name,
                "id" -> game.id
              ))
            )
            system.lidraughtsBus.publish(event, 'changeFeaturedGame)
        }
      }
  }
}

private[tv] object TvTrouper {

  case class GetGameId(channel: Tv.Channel, promise: Promise[Option[Game.ID]])
  case class GetGameIds(channel: Tv.Channel, max: Int, promise: Promise[List[Game.ID]])

  case class GetGameIdAndHistory(channel: Tv.Channel, promise: Promise[ChannelTrouper.GameIdAndHistory])

  case object Select
  case class Selected(channel: Tv.Channel, game: Game)

  case class GetChampions(promise: Promise[Tv.Champions])
}
