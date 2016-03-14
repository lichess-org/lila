package lila.push

import akka.actor._
import java.io.File

import com.relayrides.pushy.apns
import play.api.libs.json._

private final class ApplePush(
    getDevice: String => Fu[Option[Device]],
    system: ActorSystem,
    certificate: File,
    password: String) {

  private val actor = system.actorOf(Props(classOf[ApnsActor], certificate, password))

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevice(userId) map {
      _ foreach { device =>
        val token = apns.util.TokenUtil.sanitizeTokenString(device.id)
        val topic = "org.lichess.mobileapp"
        val payload = Json stringify Json.obj(
          "alert" -> Json.obj(
            "title" -> data.title,
            "body" -> data.body
          ),
          "data" -> data.payload)
        actor ! new apns.util.SimpleApnsPushNotification(token, topic, payload)
      }
    }
}

// the damn API is blocking, so at least use only one thread at a time
private final class ApnsActor(certificate: File, password: String) extends Actor {

  val logger = play.api.Logger("push")

  val client = new apns.ApnsClient[apns.util.SimpleApnsPushNotification](certificate, password)

  override def preStart {
    // blocking
    client.connect(apns.ApnsClient.DEVELOPMENT_APNS_HOST).await()
  }

  def receive = {
    case notification: apns.util.SimpleApnsPushNotification =>
      // blocking
      val res = client.sendNotification(notification).get()
      if (res.isAccepted())
        logger.info("Push notitification accepted by APNs gateway.")
      else {
        logger.warn("Notification rejected by the APNs gateway: " + res.getRejectionReason())
        if (res.getTokenInvalidationTimestamp() != null)
          logger.warn("\t…and the token is invalid as of " + res.getTokenInvalidationTimestamp())
      }
  }
}
