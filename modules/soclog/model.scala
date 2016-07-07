package lila.soclog

case class OAuth1Info(token: String, secret: String)

case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

case class Profile(
  providerId: String,
  userId: String,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  fullName: Option[String] = None,
  email: Option[String] = None,
  oAuth1Info: Option[OAuth1Info] = None,
  passwordInfo: Option[PasswordInfo] = None)
