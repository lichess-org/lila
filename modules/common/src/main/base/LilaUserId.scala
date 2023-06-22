package lila.base

import cats.Show
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
    given UserIdOf[UserName] = n => UserId(n.value.toLowerCase)
    given Show[UserName]     = _.value

  // maybe an Id, maybe a Name... something that's probably cased wrong
  opaque type UserStr = String
  object UserStr extends OpaqueString[UserStr]:
    given UserIdOf[UserStr] = n => UserId(n.value.toLowerCase)
    def read(str: String): Option[UserStr] =
      val clean = str.trim.takeWhile(' ' !=)
      if clean.lengthIs > 1 then Some(UserStr(clean)) else None

  opaque type UserStrOrEmail = String
  object UserStrOrEmail extends OpaqueString[UserStrOrEmail]:
    extension (e: UserStrOrEmail)
      def normalize = UserIdOrEmail(lila.common.EmailAddress.from(e).fold(e.toLowerCase)(_.normalize.value))

  opaque type UserIdOrEmail = String
  object UserIdOrEmail extends OpaqueString[UserIdOrEmail]

  opaque type ModId = String
  object ModId extends OpaqueUserId[ModId]
