package lila.hub

import scala.concurrent.duration._
import akka.actor._
import akka.routing._
import akka.dispatch.Dispatchers
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import actorApi._

final class Broadcast(actors: List[ActorSelection])(implicit timeout: Timeout) extends Actor {

  private val router = context.actorOf(Props.empty.withRouter(new RouterConfig {

    def routerDispatcher: String = Dispatchers.DefaultDispatcherId
    def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy

    def createRoute(routeeProvider: RouteeProvider): Route = {
      routeeProvider.registerRoutees(actors.toVector)
      val destinations = actors map { Destination(sender, _) }
      { case _ ⇒ destinations }
    }
  }))

  def receive = {

    case GetNbMembers ⇒ askAll(GetNbMembers).mapTo[List[Int]] foreach { nbs ⇒
      router ! NbMembers(nbs.sum)
    }

    case Ask(msg) ⇒ askAll(msg) pipeTo sender

    case msg      ⇒ router ! msg
  }

  private def askAll(message: Any): Fu[List[Any]] =
    actors.map(_ ? message).sequence
}
