package lila.study

case class Settings(
    computer: Settings.UserSelection,
    explorer: Settings.UserSelection,
    cloneable: Settings.UserSelection,
    shareable: Settings.UserSelection,
    chat: Settings.UserSelection,
    sticky: Boolean,
    description: Boolean
)

object Settings:

  val init = Settings(
    computer = UserSelection.Everyone,
    explorer = UserSelection.Everyone,
    cloneable = UserSelection.Everyone,
    shareable = UserSelection.Everyone,
    chat = UserSelection.Member,
    sticky = true,
    description = false
  )

  enum UserSelection:
    case Nobody extends UserSelection
    case Owner extends UserSelection
    case Contributor extends UserSelection
    case Member extends UserSelection
    case Everyone extends UserSelection
    val key = UserSelection.this.toString.toLowerCase

  object UserSelection:

    val byKey = values.mapBy(_.key)

    def allows(sel: UserSelection, study: Study, userId: Option[UserId]): Boolean = sel match
      case Nobody => false
      case Everyone => true
      case Member => userId.so(study.isMember)
      case Contributor => userId.so(study.canContribute)
      case Owner => userId.so(study.isOwner)
