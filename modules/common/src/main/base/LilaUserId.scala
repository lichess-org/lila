package lila.base

import ornicar.scalalib.newtypes.*

trait LilaUserId:

  trait UserIdOf[U]:
    def apply(u: U): UserId
    extension (u: U)
      inline def id: UserId                        = apply(u)
      inline def is[T: UserIdOf](other: T)         = u.id == other.id
      inline def isnt[T: UserIdOf](other: T)       = u.id != other.id
      inline def is[T: UserIdOf](other: Option[T]) = other.exists(_.id == u.id)

  opaque type UserId = String
  object UserId extends OpaqueString[UserId]:
    given UserIdOf[UserId] = _.value

  // specialized UserIds like Coach.Id
  trait OpaqueUserId[A] extends OpaqueString[A]:
    given UserIdOf[A]                          = _.value
    extension (a: A) inline def userId: UserId = a into UserId

  // Properly cased for display
  opaque type UserName = String
  object UserName extends OpaqueString[UserName]:
    given UserIdOf[UserName]  = n => UserId(n.value.toLowerCase)
    given cats.Show[UserName] = _.value

  // maybe an Id, maybe a Name... something that's probably cased wrong
  opaque type UserStr = String
  object UserStr extends OpaqueString[UserStr]:
    given UserIdOf[UserStr] = n => UserId(n.value.toLowerCase)
    def read(str: String): Option[UserStr] =
      val clean = str.trim.takeWhile(' ' !=)
      if clean.lengthIs > 1 then Some(UserStr(clean)) else None

  // the prefix, or entirety, of a user name.
  // "chess-" is a valid username prefix, but not a valid username
  opaque type UserSearch = String
  object UserSearch extends OpaqueString[UserSearch]:
    private val regex = "(?i)[a-z0-9][a-z0-9_-]{2,28}".r
    def read(str: String): Option[UserSearch] =
      val clean = str.trim.takeWhile(' ' !=)
      if regex.matches(clean) then Some(clean.toLowerCase) else None

  opaque type UserStrOrEmail = String
  object UserStrOrEmail extends OpaqueString[UserStrOrEmail]:
    extension (e: UserStrOrEmail)
      def normalize = UserIdOrEmail(lila.common.EmailAddress.from(e).fold(e.toLowerCase)(_.normalize.value))

  opaque type UserIdOrEmail = String
  object UserIdOrEmail extends OpaqueString[UserIdOrEmail]

  opaque type ModId = String
  object ModId extends OpaqueUserId[ModId]
