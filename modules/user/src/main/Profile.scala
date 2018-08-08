package lidraughts.user

case class Profile(
    country: Option[String] = None,
    location: Option[String] = None,
    bio: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fmjdRating: Option[Int] = None,
    kndbRating: Option[Int] = None,
    links: Option[String] = None
) {

  def nonEmptyRealName = List(ne(firstName), ne(lastName)).flatten match {
    case Nil => none
    case names => (names mkString " ").some
  }

  def countryInfo = country flatMap Countries.info

  def nonEmptyLocation = ne(location)

  def nonEmptyBio = ne(bio)

  def isEmpty = completionPercent == 0

  def isComplete = completionPercent == 100

  def completionPercent: Int = {
    val c = List(country, bio, firstName, lastName)
    100 * c.count(_.isDefined) / c.size
  }

  def actualLinks: List[Link] = links ?? Links.make

  import Profile.OfficialRating

  def officialRating: Option[OfficialRating] =
    fmjdRating.map { OfficialRating("fmjd", _) } orElse
      kndbRating.map { OfficialRating("kndb", _) }

  private def ne(str: Option[String]) = str.filter(_.nonEmpty)
}

object Profile {

  case class OfficialRating(name: String, rating: Int)

  val default = Profile()

  import reactivemongo.bson.Macros
  private[user] val profileBSONHandler = Macros.handler[Profile]
}
