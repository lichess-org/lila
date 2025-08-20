package lila.core

import scalalib.newtypes.*

import lila.core.user.Me

object userId:

  // the id of a user, always lowercased
  opaque type UserId = String
  object UserId extends OpaqueString[UserId]:
    extension (id: UserId)
      def isGhost: Boolean = id == ghost || id.startsWith("!")
      def noGhost: Boolean = !isGhost
    given UserIdOf[UserId] = _.value
    val lichess: UserId = "lichess"
    val lichessAsMe: MyId = lichess.into(MyId)
    val ghost: UserId = "ghost"
    val explorer: UserId = "openingexplorer"
    val broadcaster: UserId = "broadcaster"
    val irwin: UserId = "irwin"
    val kaladin: UserId = "kaladin"
    val ai: UserId = "ai"
    val lichess4545: UserId = "lichess4545"
    val challengermode: UserId = "challengermode"
    val watcherbot: UserId = "watcherbot"
    def isOfficial[U: UserIdOf](user: U) = lichess.is(user) || broadcaster.is(user)

  trait UserIdOf[U]:
    def apply(u: U): UserId
    extension (u: U)
      inline def id: UserId = apply(u)
      inline def is[T: UserIdOf](other: T) = u.id == other.id
      inline def isnt[T: UserIdOf](other: T) = u.id != other.id
      inline def is[T: UserIdOf](other: Option[T]) = other.exists(_.id == u.id)

  // specialized UserIds like Coach.Id
  trait OpaqueUserId[A] extends OpaqueString[A]:
    given UserIdOf[A] = _.into(UserId)
    extension (a: A) inline def userId: UserId = a.into(UserId)

  opaque type MyId = String
  object MyId extends TotalWrapper[MyId, String]:
    given Conversion[MyId, UserId] = UserId(_)
    given UserIdOf[MyId] = u => u
    given (using id: MyId): Option[MyId] = Some(id)
    given (using me: Me): MyId = me.myId
    given [M[_]]: Conversion[M[MyId], M[UserId]] = u => UserId.from(MyId.raw(u))
    given Conversion[Me, MyId] = _.myId
    extension (me: MyId)
      inline def userId: UserId = me.into(UserId)
      inline def modId: ModId = me.into(ModId)

  // Properly cased for display
  opaque type UserName = String
  object UserName extends OpaqueString[UserName]:
    extension (e: UserName) def str = UserStr(e)
    given UserIdOf[UserName] = n => UserId(n.value.toLowerCase)
    // what existing usernames are like
    val historicalRegex = "[a-zA-Z0-9_-]{2,30}".r
    val anonymous: UserName = "Anonymous"
    val lichess: UserName = "lichess"
    val anonMod: String = "A Lichess Moderator"

  // maybe an Id, maybe a Name... something that's probably cased wrong
  opaque type UserStr = String
  object UserStr extends OpaqueString[UserStr]:
    extension (e: UserStr)
      def couldBeUsername: Boolean = UserId.noGhost(e.id) && UserName.historicalRegex.matches(e)
      def validate: Option[UserStr] = e.couldBeUsername.option(e)
      def validateId: Option[UserId] = validate.map(_.id)
    given UserIdOf[UserStr] = n => UserId(n.value.toLowerCase)
    // these conversions are using when generating routes containing UserStr
    // so we can give them usernames and userIds
    given Conversion[UserName, UserStr] = _.value
    given Conversion[Option[UserName], Option[UserStr]] = _.map(_.value)
    given Conversion[UserId, UserStr] = _.value
    def read(str: String): Option[UserStr] =
      val trimmed = str.trim
      Option.when(UserName.historicalRegex.matches(trimmed))(UserStr(trimmed))

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
