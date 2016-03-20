package lila.push

import akka.actor._
import java.io.InputStream
import scala.util.Failure

import play.api.libs.json._

private final class ApplePush(
    getDevice: String => Fu[Option[Device]],
    system: ActorSystem,
    certificate: InputStream,
    password: String,
    enabled: Boolean) {

  private val actor = system.actorOf(Props(classOf[ApnsActor], certificate, password))

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevice(userId) map {
      _ foreach { device =>
        if (enabled) actor ! ApplePush.Notification(
          token = device.deviceId,
          alert = Json.obj(
            "title" -> data.title,
            "body" -> data.body),
          payload = data.payload)
        else logger.warn(s"Sorry $userId, apple push is disabled by config!")
      }
    }
}

object ApplePush {

  case class Notification(token: String, alert: JsObject, payload: JsObject)
}

// the damn API is blocking, so at least use only one thread at a time
private final class ApnsActor(certificate: InputStream, password: String) extends Actor {

  import com.relayrides.pushy.apns._, util._

  var manager: PushManager[SimpleApnsPushNotification] = _

  def getManager = Option(manager) getOrElse {
    val m = new PushManager[SimpleApnsPushNotification](
      ApnsEnvironment.getSandboxEnvironment(),
      SSLContextUtil.createDefaultSSLContext(certificate, password),
      null, // Optional: custom event loop group
      null, // Optional: custom ExecutorService for calling listeners
      null, // Optional: custom BlockingQueue implementation
      new PushManagerConfiguration(),
      "ApplePushManager")

    m.registerRejectedNotificationListener(new RejectedNotificationListener[SimpleApnsPushNotification] {
      override def handleRejectedNotification(
        m: PushManager[_ <: SimpleApnsPushNotification],
        notification: SimpleApnsPushNotification,
        reason: RejectedNotificationReason) {
        logger.error(s"$notification was rejected with rejection reason $reason")
      }
    })
    m.registerFailedConnectionListener(new FailedConnectionListener[SimpleApnsPushNotification] {
      override def handleFailedConnection(
        m: PushManager[_ <: SimpleApnsPushNotification],
        cause: Throwable) {
        logger.error(s"Can't connect because $cause")
        cause match {
          case ssl: javax.net.ssl.SSLHandshakeException =>
            logger.error(s"This is probably a permanent failure, and we should shut down the manager")
          case _ =>
        }
      }
    })
    m.start()
    manager = m
    m
  }

  override def postStop() {
    Option(manager).foreach(_.shutdown())
  }

  def receive = {
    case ApplePush.Notification(token, alert, payload) =>

      val payloadBuilder = new ApnsPayloadBuilder()

      payloadBuilder.setAlertBody(Json stringify alert)
      payloadBuilder.setBadgeNumber(1)
      payloadBuilder.addCustomProperty("data", Json stringify payload)

      val notif = new SimpleApnsPushNotification(
        TokenUtil.tokenStringToByteArray(token),
        payloadBuilder.buildWithDefaultMaximumLength())

      logger.info(s"Sending alert=$alert, payload=$payload to $token")

      getManager.getQueue().put(notif)
  }
}
