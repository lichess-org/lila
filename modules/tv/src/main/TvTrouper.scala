package lila.tv

import akka.actor._
import akka.pattern.{ ask => actorAsk }
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.common.LightUser
import lila.game.{ Game, GameRepo }
import lila.hub.Trouper

private[tv] final class TvTrouper(
    system: ActorSystem,
    rendererActor: ActorSelection,
    selectChannel: lila.socket.Channel,
    lightUser: LightUser.GetterSync,
    onSelect: Game => Unit
) extends Trouper {

  import TvTrouper._

  private val channelTroupers: Map[Tv.Channel, Trouper] = Tv.Channel.all.map { c =>
    c -> new ChannelTrouper(c, lightUser, onSelect = this.!)
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

    case Select =>
      GameRepo.featuredCandidates map (_ map Tv.toCandidate(lightUser)) foreach { candidates =>
        channelTroupers foreach {
          case (channel, trouper) => trouper ! ChannelTrouper.Select {
            candidates filter channel.filter map (_.game)
          }
        }
      }

    case Selected(channel, game) =>
      import lila.socket.Socket.makeMessage
      val player = game.firstPlayer
      val user = player.userId flatMap lightUser
      (user |@| player.rating) apply {
        case (u, r) => channelChampions += (channel -> Tv.Champion(u, r, game.id))
      }
      onSelect(game)
      selectChannel ! lila.socket.Channel.Publish(makeMessage("tvSelect", Json.obj(
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
            val event = lila.hub.actorApi.game.ChangeFeatured(
              game.id,
              makeMessage("featured", Json.obj(
                "html" -> html,
                "color" -> game.firstColor.name,
                "id" -> game.id
              ))
            )
            system.lilaBus.publish(event, 'changeFeaturedGame)
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
