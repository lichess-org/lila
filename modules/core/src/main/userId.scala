package lila.core

import scalalib.newtypes.*

object userId:

  // the id of a user, always lowercased
  opaque type UserId = String
  object UserId extends OpaqueString[UserId]:
    extension (id: UserId)
      def isGhost: Boolean = id == ghost || id.startsWith("!")
      def noGhost: Boolean = !isGhost
    given UserIdOf[UserId] = _.value
    val lichess: UserId    = "lichess"
    val lichessAsMe: MyId  = lichess.into(MyId)
    val ghost: UserId      = "ghost"
    val explorer: UserId   = "openingexplorer"

  trait UserIdOf[U]:
    def apply(u: U): UserId
    extension (u: U)
      inline def id: UserId                        = apply(u)
      inline def is[T: UserIdOf](other: T)         = u.id == other.id
      inline def isnt[T: UserIdOf](other: T)       = u.id != other.id
      inline def is[T: UserIdOf](other: Option[T]) = other.exists(_.id == u.id)
      inline def isMe: Boolean                     = u.id == UserId("me")

  // specialized UserIds like Coach.Id
  trait OpaqueUserId[A] extends OpaqueString[A]:
    given UserIdOf[A]                          = _.into(UserId)
    extension (a: A) inline def userId: UserId = a.into(UserId)

  opaque type MyId = String
  object MyId extends TotalWrapper[MyId, String]:
    given Conversion[MyId, UserId]                 = UserId(_)
    given UserIdOf[MyId]                           = u => u
    given [M[_]]: Conversion[M[MyId], M[UserId]]   = u => UserId.from(MyId.raw(u))
    extension (me: MyId) inline def userId: UserId = me.into(UserId)

  // Properly cased for display
  opaque type UserName = String
  object UserName extends OpaqueString[UserName]:
    given UserIdOf[UserName] = n => UserId(n.value.toLowerCase)
    // what existing usernames are like
    val historicalRegex     = "(?i)[a-z0-9][a-z0-9_-]{0,28}[a-z0-9]".r
    val anonymous: UserName = "Anonymous"
    val lichess: UserName   = "lichess"

  // maybe an Id, maybe a Name... something that's probably cased wrong
  opaque type UserStr = String
  object UserStr extends OpaqueString[UserStr]:
    extension (e: UserStr)
      def couldBeUsername: Boolean   = UserId.noGhost(e.id) && UserName.historicalRegex.matches(e)
      def validateId: Option[UserId] = Option.when(couldBeUsername)(e.id)
    given UserIdOf[UserStr] = n => UserId(n.value.toLowerCase)
    def read(str: String): Option[UserStr] =
      val clean = str.trim.takeWhile(' ' !=)
      Option.when(clean.lengthIs > 1)(UserStr(clean))

  // the prefix, or entirety, of a user name.
  // "chess-" is a valid username prefix, but not a valid username
  opaque type UserSearch = String
  object UserSearch extends OpaqueString[UserSearch]:
    private val regex = "(?i)[a-z0-9][a-z0-9_-]{2,28}".r
    def read(str: String): Option[UserSearch] =
      val clean = str.trim.takeWhile(' ' !=)
      if regex.matches(clean) then Some(clean.toLowerCase) else None

  opaque type ModId = String
  object ModId extends OpaqueUserId[ModId]
