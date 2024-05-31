package lila.study

import lila.user.User

case class Settings(
    computer: Settings.UserSelection,
    cloneable: Settings.UserSelection,
    chat: Settings.UserSelection,
    sticky: Boolean,
    description: Boolean
)

object Settings {

  val init = Settings(
    computer = UserSelection.Everyone,
    cloneable = UserSelection.Everyone,
    chat = UserSelection.Member,
    sticky = true,
    description = false
  )

  sealed trait UserSelection {
    lazy val key = toString.toLowerCase
  }
  object UserSelection {
    case object Nobody      extends UserSelection
    case object Owner       extends UserSelection
    case object Contributor extends UserSelection
    case object Member      extends UserSelection
    case object Everyone    extends UserSelection
    val byKey = List(Nobody, Owner, Contributor, Member, Everyone).map { v =>
      v.key -> v
    }.toMap

    def allows(sel: UserSelection, study: Study, userId: Option[User.ID]): Boolean =
      sel match {
        case Nobody      => false
        case Everyone    => true
        case Member      => userId ?? study.isMember
        case Contributor => userId ?? study.canContribute
        case Owner       => userId ?? study.isOwner
      }
  }
}
