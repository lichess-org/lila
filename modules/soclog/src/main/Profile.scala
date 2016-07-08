package lila.soclog

case class Profile(
  userId: String,
  username: String,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  fullName: Option[String] = None,
  email: Option[String] = None)
