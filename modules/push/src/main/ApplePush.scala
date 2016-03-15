package lila.push

import akka.actor._
import java.io.InputStream
import scala.util.Failure

import com.vngrs.scala.pushy._
import com.vngrs.scala.pushy.Implicits._
import play.api.libs.json._

private final class ApplePush(
    getDevice: String => Fu[Option[Device]],
    system: ActorSystem,
    certificate: InputStream,
    password: String) {

  private val actor = system.actorOf(Props(classOf[ApnsActor], certificate, password))

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevice(userId) map {
      _ foreach { device =>
        val token = device.id
        val payload = Payload(Json stringify Json.obj(
          "alert" -> Json.obj(
            "title" -> data.title,
            "body" -> data.body
          ),
          "data" -> data.payload))
        actor ! PushNotification(token, payload)
      }
    }
}

// the damn API is blocking, so at least use only one thread at a time
private final class ApnsActor(certificate: InputStream, password: String) extends Actor {

  val logger = play.api.Logger("push")

  var manager: PushManager = _

  override def preStart() {
    manager = PushManager.sandbox("Example", SSLContext(certificate, password).get)
  }

  override def postStop() {
    Option(manager).foreach(_.shutdown())
  }

  def receive = {
    case notification: PushNotification =>
      manager send notification match {
        case Failure(ex) => logger.warn(s"iOS notification failed because ${ex.getMessage}!")
        case _           => logger.info("iOS notification sent!")
      }
  }
}
