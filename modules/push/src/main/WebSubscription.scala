package lila.push

case class WebSubscription(
    endpoint: String,
    auth: String,
    p256dh: String,
    aes128gcm: Option[Boolean],
    aesgcm: Option[Boolean]
)

object WebSubscription:

  import play.api.libs.json.*
  import play.api.libs.functional.syntax.*
  import reactivemongo.api.bson.{ Macros, BSONDocumentReader }

  given webSubscriptionReads: Reads[WebSubscription] = (
    (__ \ "endpoint").read[String] and
      (__ \ "keys" \ "auth").read[String] and
      (__ \ "keys" \ "p256dh").read[String] and
      (__ \ "encodings" \ "aes128gcm").readNullable[Boolean] and
      (__ \ "encodings" \ "aesgcm").readNullable[Boolean]
  )(WebSubscription.apply)

  given webSubscriptionReader: BSONDocumentReader[WebSubscription] = Macros.reader[WebSubscription]
