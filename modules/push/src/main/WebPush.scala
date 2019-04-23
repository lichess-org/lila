package lila.push

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import scala.concurrent.duration._
import scala.compat.java8.FutureConverters

import org.bouncycastle.jce.provider.BouncyCastleProvider

import play.api.libs.json._
import play.api.libs.ws.WS

import nl.martijndwars.webpush.{ PushService, Notification }

import lila.user.User

private final class WebPush(
    getSubscriptions: User.ID => Fu[List[WebSubscription]],
    vapidSubject: String,
    vapidPublicKey: String,
    vapidPrivateKey: String
) {

  java.security.Security.addProvider(new BouncyCastleProvider())

  def apply(userId: User.ID)(data: => PushApi.Data): Funit =
    getSubscriptions(userId) flatMap {
      case Nil => funit
      case subscription :: _ =>
        send(subscription, data) // TODO: Support multiple subscriptions
    }

  private def send(sub: WebSubscription, data: PushApi.Data): Funit = {
    val notification = new Notification(
      sub.endpoint,
      sub.p256dh,
      sub.auth,
      Json.obj(
        "title" -> data.title,
        "body" -> data.body,
        "stacking" -> data.stacking.key,
        "payload" -> data.payload
      ).toString
    )

    val pushService = new PushService()
    pushService.setPublicKey(vapidPublicKey)
    pushService.setPrivateKey(vapidPrivateKey)

    val javaFuture = pushService.sendAsync(notification)

    FutureConverters.toScala(CompletableFuture.supplyAsync(new Supplier[Unit] {
      override def get(): Unit = javaFuture.get
    }))
  }
}
