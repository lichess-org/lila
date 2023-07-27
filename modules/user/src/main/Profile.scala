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
):

  def nonEmptyRealName =
    List(ne(firstName), ne(lastName)).flatten match
      case Nil   => none
      case names => (names mkString " ").some

  def countryInfo = country flatMap Flags.info

  def nonEmptyLocation = ne(location)

  def nonEmptyBio = ne(bio)

  def isEmpty = completionPercent == 0

  def completionPercent: Int =
    100 * List(country, bio, firstName, lastName).count(_.isDefined) / 4

  def actualLinks: List[Link] = links so Links.make

  import Profile.OfficialRating

  def officialRating: Option[OfficialRating] =
    fideRating.map { OfficialRating("fide", _) } orElse
      uscfRating.map { OfficialRating("uscf", _) } orElse
      ecfRating.map { OfficialRating("ecf", _) } orElse
      rcfRating.map { OfficialRating("rcf", _) } orElse
      cfcRating.map { OfficialRating("cfc", _) } orElse
      dsbRating.map { OfficialRating("dsb", _) }

  def filterTroll(troll: Boolean) = copy(
    bio = bio ifFalse troll,
    firstName = firstName ifFalse troll,
    lastName = lastName ifFalse troll,
    location = location ifFalse troll,
    links = links ifFalse troll
  )

  private def ne(str: Option[String]) = str.filter(_.nonEmpty)

object Profile:

  case class OfficialRating(name: String, rating: Int)

  val default = Profile()

  import reactivemongo.api.bson.*
  private[user] given BSONDocumentHandler[Profile] = Macros.handler[Profile]
