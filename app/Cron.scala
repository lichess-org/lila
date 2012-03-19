package lila.http

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.util.duration._
import akka.util.Duration
import scalaz.effects._

import lila.system.SystemEnv

final class Cron(env: SystemEnv)(implicit app: Application) {

  spawn("online-username", 3 seconds) { env ⇒
    env.userRepo updateOnlineUsernames env.usernameMemo.keys
  }

  object Tick

  def spawn(name: String, freq: Duration)(f: SystemEnv ⇒ IO[Unit]) = {
    val actor = Akka.system.actorOf(Props(new Actor {
      def receive = {
        case Tick ⇒ f(env).unsafePerformIO
      }
    }), name = name)
    Akka.system.scheduler.schedule(freq, freq, actor, Tick)
  }
}
