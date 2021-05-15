package lila.user

case class Profile(
    country: Option[String] = None,
    location: Option[String] = None,
    bio: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fideRating: Option[Int] = None,
    uscfRating: Option[Int] = None,
    ecfRating: Option[Int] = None,
    rcfRating: Option[Int] = None,
    cfcRating: Option[Int] = None,
    dsbRating: Option[Int] = None,
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

  import Profile.OfficialRating

  def officialRating: Option[OfficialRating] =
    fideRating.map { OfficialRating("fide", _) } orElse
      uscfRating.map { OfficialRating("uscf", _) } orElse
      ecfRating.map { OfficialRating("ecf", _) } orElse
      rcfRating.map { OfficialRating("rcf", _) } orElse
      cfcRating.map { OfficialRating("cfc", _) } orElse
      dsbRating.map { OfficialRating("dsb", _) }

  private def ne(str: Option[String]) = str.filter(_.nonEmpty)
}

object Profile {

  case class OfficialRating(name: String, rating: Int)

  val default = Profile()

  import reactivemongo.api.bson.Macros
  private[user] val profileBSONHandler = Macros.handler[Profile]
}
