package lila.user

case class Profile(
    country: Option[String] = None,
    location: Option[String] = None,
    bio: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    links: Option[String] = None
) {

  def nonEmptyRealName =
    List(ne(firstName), ne(lastName)).flatten match {
      case Nil   => none
      case names => (names mkString " ").some
    }

  def countryInfo = country flatMap Countries.info

  def nonEmptyLocation = ne(location)

  def nonEmptyBio = ne(bio)

  def isEmpty = completionPercent == 0

  def completionPercent: Int =
    100 * List(country, bio, firstName, lastName).count(_.isDefined) / 4

  def actualLinks: List[Link] = links ?? Links.make

  private def ne(str: Option[String]) = str.filter(_.nonEmpty)
}

object Profile {

  val default = Profile()

  import reactivemongo.api.bson.Macros
  private[user] val profileBSONHandler = Macros.handler[Profile]
}
