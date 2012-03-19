package lila.http

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.util.duration._
import akka.util.Duration
import scalaz.effects._

import lila.system.SystemEnv

final class Cron(env: SystemEnv)(implicit app: Application) {

  spawn("online_username") { env ⇒
    env.userRepo updateOnlineUsernames env.usernameMemo.keys
  }

  def spawn(name: String)(f: SystemEnv ⇒ IO[Unit]) = {
    val freq = env.getMilliseconds("cron.online_username.frequency") millis
    val actor = Akka.system.actorOf(Props(new Actor {
      def receive = {
        case "tick" ⇒ f(env).unsafePerformIO
      }
    }), name = name)
    Akka.system.scheduler.schedule(freq, freq, actor, "tick")
  }
}
