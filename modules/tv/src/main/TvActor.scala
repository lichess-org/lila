package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import scala.concurrent.duration._

import lila.common.LightUser
import lila.game.{ Game, GameRepo }

private[tv] final class TvActor(
    rendererActor: ActorSelection,
    roundSocket: ActorSelection,
    selectChannel: ActorRef,
    lightUser: LightUser.GetterSync,
    onSelect: Game => Unit
) extends Actor {

  import TvActor._

  implicit private def timeout = makeTimeout(100 millis)

  val channelActors: Map[Tv.Channel, ActorRef] = Tv.Channel.all.map { c =>
    c -> context.actorOf(Props(classOf[ChannelActor], c, lightUser), name = c.toString)
  }.toMap

  var channelChampions = Map[Tv.Channel, Tv.Champion]()

  def receive = {

    case GetGameId(channel) =>
      channelActors get channel foreach { actor =>
        actor ? ChannelActor.GetGameId pipeTo sender
      }

    case GetGameIdAndHistory(channel) =>
      channelActors get channel foreach { actor =>
        actor ? ChannelActor.GetGameIdAndHistory pipeTo sender
      }

    case GetGameIds(channel, max) =>
      channelActors get channel foreach { actor =>
        actor ? ChannelActor.GetGameIds(max) pipeTo sender
      }

    case GetChampions => sender ! Tv.Champions(channelChampions)

    case Select =>
      GameRepo.featuredCandidates map (_ map Tv.toCandidate(lightUser)) foreach { candidates =>
        channelActors foreach {
          case (channel, actor) => actor forward ChannelActor.Select {
            candidates filter channel.filter map (_.game)
          }
        }
      }

    case Selected(channel, game, previousId) =>
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
      if (channel == Tv.Channel.Best)
        rendererActor ? actorApi.RenderFeaturedJs(game) onSuccess {
          case html: play.twirl.api.Html =>
            val event = lila.hub.actorApi.game.ChangeFeatured(
              game.id,
              makeMessage("featured", Json.obj(
                "html" -> html.toString,
                "color" -> game.firstColor.name,
                "id" -> game.id
              ))
            )
            context.system.lilaBus.publish(event, 'changeFeaturedGame)
        }
  }
}

private[tv] object TvActor {

  case class GetGameId(channel: Tv.Channel) extends AnyVal
  case class GetGameIds(channel: Tv.Channel, max: Int)

  case class GetGameIdAndHistory(channel: Tv.Channel) extends AnyVal

  case object Select
  case class Selected(channel: Tv.Channel, game: Game, previousId: Option[Game.ID])

  case object GetChampions
}
