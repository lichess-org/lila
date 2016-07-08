package lila.soclog

case class AccessToken(token: String, secret: String)

case class Profile(
  providerId: String,
  userId: String,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  fullName: Option[String] = None,
  email: Option[String] = None)
