package lila.user

case class Profile(
    country: Option[String] = None,
    location: Option[String] = None,
    bio: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fideRating: Option[Int] = None,
    uscfRating: Option[Int] = None,
    ecfRating: Option[Int] = None) {

  def nonEmptyRealName = List(ne(firstName), ne(lastName)).flatten match {
    case Nil   => none
    case names => (names mkString " ").some
  }

  def countryInfo = country flatMap Countries.info

  def nonEmptyLocation = ne(location)

  def nonEmptyBio = ne(bio)

  def isEmpty = completionPercent == 0

  def isComplete = completionPercent == 100

  def completionPercent: Int = {
    val c = List(country, location, bio, firstName, lastName)
    100 * c.count(_.isDefined) / c.size
  }

  import Profile.OfficialRating

  def officialRating: Option[OfficialRating] =
    fideRating.map { OfficialRating("fide", _) } orElse
      uscfRating.map { OfficialRating("uscf", _) } orElse
      ecfRating.map { OfficialRating("ecf", _) }

  private def ne(str: Option[String]) = str.filter(_.nonEmpty)
}

object Profile {

  case class OfficialRating(name: String, rating: Int)

  val default = Profile()

  private[user] val profileBSONHandler = reactivemongo.bson.Macros.handler[Profile]
}
