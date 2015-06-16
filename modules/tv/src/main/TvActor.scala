package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo }

private[tv] final class TvActor(rendererActor: ActorSelection) extends Actor {

  import TvActor._

  implicit private def timeout = makeTimeout(50 millis)

  val channelActors: Map[Tv.Channel, ActorRef] = Tv.Channel.all.map { c =>
    c -> context.actorOf(Props(classOf[ChannelActor], c), name = c.toString)
  }.toMap

  def receive = {

    case GetGame(channel) =>
      channelActors get channel foreach { actor =>
        actor ? ChannelActor.GetGame pipeTo sender
      }

    case Select =>
      GameRepo.featuredCandidates foreach { candidates =>
        channelActors foreach {
          case (channel, actor) => actor forward ChannelActor.Select(candidates filter channel.filter)
        }
      }

    case Selected(channel, game) =>
      import lila.socket.Socket.makeMessage
      rendererActor ? actorApi.RenderFeaturedJs(game) onSuccess {
        case html: play.twirl.api.Html =>
          context.system.lilaBus.publish(
            lila.hub.actorApi.tv.Select(makeMessage("tvSelect", channel.key)),
            'tvSelect)
          if (channel == Tv.Channel.Best) context.system.lilaBus.publish(
            lila.hub.actorApi.game.ChangeFeatured(game.id, makeMessage("featured", Json.obj(
              "html" -> html.toString,
              "color" -> game.firstColor.name,
              "id" -> game.id))),
            'changeFeaturedGame)
      }
      GameRepo setTv game.id
  }
}

private[tv] object TvActor {

  case class GetGame(channel: Tv.Channel)
  case object Select
  case class Selected(channel: Tv.Channel, game: lila.game.Game)
}
