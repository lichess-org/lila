package lila.push

case class WebSubscription(
    endpoint: String,
    auth: String,
    p256dh: String
)

object WebSubscription:

  import play.api.libs.json.*
  import play.api.libs.functional.syntax.*
  import reactivemongo.api.bson.{ Macros, BSONDocumentReader }

  given webSubscriptionReads: Reads[WebSubscription] = (
    (__ \ "endpoint")
      .read[String]
      .and((__ \ "keys" \ "auth").read[String])
      .and((__ \ "keys" \ "p256dh").read[String])
  )(WebSubscription.apply)

  given webSubscriptionReader: BSONDocumentReader[WebSubscription] = Macros.reader[WebSubscription]
