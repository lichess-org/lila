package lila.user
import lila.core.user.{ Flag, Profile }

object Profile:

  import lila.core.user.Profile.*

  extension (p: Profile)
    def flagInfo: Option[Flag] = p.flag.flatMap(Flags.info)

    def officialRating: Option[OfficialRating] =
      import p.*
      fideRating
        .map { OfficialRating("fide", _) }
        .orElse(uscfRating.map { OfficialRating("uscf", _) })
        .orElse(ecfRating.map { OfficialRating("ecf", _) })
        .orElse(rcfRating.map { OfficialRating("rcf", _) })
        .orElse(cfcRating.map { OfficialRating("cfc", _) })
        .orElse(dsbRating.map { OfficialRating("dsb", _) })

    def filterTroll(troll: Boolean): Profile = p.copy(
      bio = p.bio.ifFalse(troll),
      realName = p.realName.ifFalse(troll),
      location = p.location.ifFalse(troll),
      links = p.links.ifFalse(troll)
    )

    def actualLinks: List[Link] = p.links.so(Links.make)

  case class OfficialRating(name: String, rating: Int)
