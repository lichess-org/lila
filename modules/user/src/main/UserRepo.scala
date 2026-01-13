package lila.user

import chess.PlayerTitle
import com.roundeights.hasher.Implicits.*
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom
import scalalib.model.{ Days, LangTag }

import lila.core.LightUser
import lila.core.email.NormalizedEmailAddress
import lila.core.net.ApiVersion
import lila.core.security.HashedPassword
import lila.core.user.{ Plan, PlayTime, Profile, TotpSecret, UserMark, RoleDbKey, KidMode }
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }

final class UserRepo(c: Coll)(using Executor) extends lila.core.user.UserRepo(c):

  import lila.user.BSONFields as F
  export lila.user.BSONHandlers.given

  private def recoverDeleted(user: Fu[Option[User]]): Fu[Option[User]] =
    user.recover:
      case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => none

  def withColl[A](f: Coll => A): A = f(coll)

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledNoBotSelect ++ notLame).sort($sort.desc("count.game")).cursor[User]().list(nb)

  def byId[U: UserIdOf](u: U): Fu[Option[User]] =
    u.id.noGhost.so:
      recoverDeleted:
        coll.byId[User](u.id)

  def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]] =
    val ids = us.map(_.id).filter(_.noGhost)
    ids.nonEmpty.so(coll.byIds[User, UserId](ids))

  def byIdsSecondary(ids: Iterable[UserId]): Fu[List[User]] =
    coll.byIds[User, UserId](ids, _.sec)

  def enabledById[U: UserIdOf](u: U): Fu[Option[User]] =
    u.id.noGhost.so:
      recoverDeleted:
        coll.one[User](enabledSelect ++ $id(u.id))

  def enabledByIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]] =
    val ids = us.map(_.id).filter(_.noGhost)
    coll.list[User](enabledSelect ++ $inIds(ids), _.sec)

  def byIdOrGhost(id: UserId): Fu[Option[Either[LightUser.Ghost, User]]] =
    if id.isGhost
    then fuccess(Left(LightUser.ghost).some)
    else
      coll.byId[User](id).map2(Right.apply).recover { case _: exceptions.BSONValueNotFoundException =>
        Left(LightUser.ghost).some
      }

  def me(id: UserId): Fu[Option[Me]] = enabledById(id).dmap(Me.from(_))
  def me[U: UserIdOf](u: U): Fu[Option[Me]] = me(u.id)

  def byEmail(email: NormalizedEmailAddress): Fu[Option[User]] = coll.one[User]($doc(F.email -> email))
  def byPrevEmail(
      email: NormalizedEmailAddress,
      readPref: ReadPref = _.sec
  ): Fu[List[User]] =
    coll.list[User]($doc(F.prevEmail -> email), readPref)

  def idByAnyEmail(emails: List[NormalizedEmailAddress]): Fu[Option[UserId]] =
    coll.primitiveOne[UserId](F.email.$in(emails), "_id")

  def countRecentByPrevEmail(email: NormalizedEmailAddress, since: Instant): Fu[Int] =
    coll.countSel($doc(F.prevEmail -> email, F.createdAt.$gt(since)))

  def pair(x: Option[UserId], y: Option[UserId]): Fu[(Option[User], Option[User])] =
    coll.byIds[User, UserId](List(x, y).flatten).map { users =>
      x.so(xx => users.find(_.id == xx)) ->
        y.so(yy => users.find(_.id == yy))
    }

  def pair(x: UserId, y: UserId): Fu[Option[(User, User)]] =
    coll.byIds[User, UserId](List(x, y)).map { users =>
      for
        xx <- users.find(_.id == x)
        yy <- users.find(_.id == y)
      yield xx -> yy
    }

  def lichessAnd(id: UserId): Future[Option[(User, User)]] = pair(UserId.lichess, id)

  def byOrderedIds(ids: Seq[UserId], readPref: ReadPref): Fu[List[User]] =
    coll.byOrderedIds[User, UserId](ids, readPref = readPref)(_.id)

  def usersFromSecondary(userIds: Seq[UserId]): Fu[List[User]] =
    byOrderedIds(userIds, _.sec)

  def optionsByIds(userIds: Seq[UserId]): Fu[List[Option[User]]] =
    coll.optionsByOrderedIds[User, UserId](userIds, readPref = _.sec)(_.id)

  def isEnabled(id: UserId): Fu[Boolean] =
    id.noGhost.so(coll.exists(enabledSelect ++ $id(id)))

  def disabledById(id: UserId): Fu[Option[User]] =
    id.noGhost.so(coll.one[User](disabledSelect ++ $id(id)))

  def usernameById(id: UserId): Fu[Option[UserName]] =
    coll.primitiveOne[UserName]($id(id), F.username)

  def usernamesByIds(ids: List[UserId]): Fu[List[UserName]] =
    coll.distinctEasy[UserName, List](F.username, $inIds(ids), _.sec)

  def createdAtById(id: UserId): Fu[Option[Instant]] =
    coll.primitiveOne[Instant]($id(id), F.createdAt)

  def orderByGameCount(u1: UserId, u2: UserId): Fu[Option[(UserId, UserId)]] =
    coll
      .find(
        $inIds(List(u1, u2)),
        $doc(s"${F.count}.game" -> true).some
      )
      .cursor[Bdoc]()
      .listAll()
      .map: docs =>
        docs
          .sortBy:
            _.child(F.count).flatMap(_.int("game"))
          .flatMap(_.getAsOpt[UserId]("_id")) match
          case List(u1, u2) => (u1, u2).some
          case _ => none

  def firstGetsWhite(u1: UserId, u2: UserId): Fu[Boolean] =
    coll
      .find($inIds(List(u1, u2)), $id(true).some)
      .sort($doc(F.colorIt -> 1))
      .one[Bdoc]
      .map:
        _.fold(ThreadLocalRandom.nextBoolean()): doc =>
          doc.string("_id") contains u1
      .addEffect: v =>
        val u1Color = Color.fromWhite(v)
        incColor(u1, u1Color)
        incColor(u2, !u1Color)

  def firstGetsWhite(u1O: Option[UserId], u2O: Option[UserId]): Fu[Boolean] =
    (u1O, u2O).mapN(firstGetsWhite) | fuccess(ThreadLocalRandom.nextBoolean())

  def incColor(userId: UserId, color: Color): Unit =
    coll
      .update(ordered = false, WriteConcern.Unacknowledged)
      .one(
        // limit to -3 <= colorIt <= 5 but set when undefined
        $id(userId) ++ $doc(F.colorIt -> $not(color.fold($gte(5), $lte(-3)))),
        $inc(F.colorIt -> color.fold(1, -1))
      )

  def mustPlayAsColor(userId: UserId): Fu[Option[Color]] =
    coll
      .primitiveOne[Int]($id(userId), F.colorIt)
      .map:
        _.flatMap: i =>
          if i > 2 then Color.black.some
          else if i < -2 then Color.white.some
          else none

  def setProfile(id: UserId, profile: Profile): Funit =
    coll.updateField($id(id), F.profile, profile).void

  def setRealName(id: UserId, name: String): Funit =
    coll.updateField($id(id), s"${F.profile}.realName", name).void

  def setUsernameCased(id: UserId, name: UserName): Funit =
    if id.is(name) then
      coll.update
        .one(
          $id(id) ++ F.changedCase.$exists(false),
          $set(F.username -> name.value, F.changedCase -> true)
        )
        .flatMap: result =>
          if result.n == 0 then fufail(s"You have already changed your username")
          else funit
    else fufail(s"Proposed username $name does not match old username $id")

  def setTitle(id: UserId, title: PlayerTitle): Funit =
    coll.updateField($id(id), F.title, title).void

  def removeTitle(id: UserId): Funit =
    coll.unsetField($id(id), F.title).void

  def getPlayTime(id: UserId): Fu[Option[PlayTime]] =
    coll.primitiveOne[PlayTime]($id(id), F.playTime)

  val enabledSelect = $doc(F.enabled -> true)
  val disabledSelect = $doc(F.enabled -> false)
  def markSelect(mark: UserMark)(v: Boolean): Bdoc =
    if v then $doc(F.marks -> mark.key)
    else F.marks.$ne(mark.key)
  def engineSelect = markSelect(UserMark.engine)
  def trollSelect = markSelect(UserMark.troll)
  val lame = $doc(F.marks.$in(List(UserMark.engine, UserMark.boost)))
  val lameOrTroll = $doc(F.marks.$in(List(UserMark.engine, UserMark.boost, UserMark.troll)))
  val notLame = $doc(F.marks.$nin(List(UserMark.engine, UserMark.boost)))
  val enabledNoBotSelect = enabledSelect ++ $doc(F.title.$ne(PlayerTitle.BOT))
  val patronSelect = $doc(s"${F.plan}.active" -> true)

  val sortCreatedAtDesc = $sort.desc(F.createdAt)

  def incNbGames(
      id: UserId,
      rated: chess.Rated,
      result: Int,
      totalTime: Option[Int],
      tvTime: Option[Int],
      botVsHuman: Boolean
  ) =
    val incs: List[BSONElement] = List(
      "count.game".some,
      rated.yes.option("count.rated"),
      (result match
        case -1 => "count.loss".some
        case 1 => "count.win".some
        case 0 => "count.draw".some
        case _ => none
      )
    ).flatten.map(k => BSONElement(k, BSONInteger(1))) ::: List(
      totalTime.map(v => BSONElement(s"${F.playTime}.total", BSONInteger(v + 2))),
      tvTime.map(v => BSONElement(s"${F.playTime}.tv", BSONInteger(v + 2))),
      totalTime.ifTrue(botVsHuman).map(v => BSONElement(s"${F.playTime}.human", BSONInteger(v + 2)))
    ).flatten

    coll.update.one($id(id), $inc($doc(incs*)))

  def incToints(id: UserId, nb: Int): Funit = coll.update.one($id(id), $inc("toints" -> nb)).void
  def removeAllToints = coll.update.one($empty, $unset("toints"), multi = true)

  def create(
      name: UserName,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mobileApiVersion: Option[ApiVersion],
      mustConfirmEmail: Boolean,
      lang: Option[LangTag] = None,
      kid: KidMode = KidMode.No
  ): Fu[Option[User]] =
    exists(name).not.flatMapz:
      val doc = newUser(name, passwordHash, email, blind, mobileApiVersion, mustConfirmEmail, lang, kid) ++
        ("len" -> BSONInteger(name.value.length))
      coll.insert.one(doc) >> byId(name.id)

  def exists[U: UserIdOf](u: U): Fu[Boolean] = coll.exists($id(u.id))
  def existsSec[U: UserIdOf](u: U): Fu[Boolean] = coll.secondary.exists($id(u.id))

  def filterExists(ids: Set[UserId]): Fu[List[UserId]] =
    coll.primitive[UserId]($inIds(ids), F.id)

  def userIdsLikeWithRole(text: UserSearch, role: RoleDbKey, max: Int = 10): Fu[List[UserId]] =
    userIdsLikeFilter(text, $doc(F.roles -> role), max)

  def userIdsLikeClosed(text: UserSearch, max: Int = 10): Fu[List[UserId]] =
    userIdsLikeFilter(text, $doc(F.enabled -> false), max)

  private[user] def userIdsLikeFilter(text: UserSearch, filter: Bdoc, max: Int): Fu[List[UserId]] =
    coll
      .find(
        $doc(F.id.$startsWith(text.value)) ++ enabledSelect ++ filter,
        $doc(F.id -> true).some
      )
      .sort($doc("len" -> 1))
      .cursor[Bdoc](ReadPref.sec)
      .list(max)
      .map:
        _.flatMap { _.getAsOpt[UserId](F.id) }

  private def setMark(mark: UserMark)(id: UserId, v: Boolean): Funit =
    coll.update.one($id(id), $addOrPull(F.marks, mark, v)).void

  def setEngine = setMark(UserMark.engine)
  def setBoost = setMark(UserMark.boost)
  def setTroll = setMark(UserMark.troll)
  def setIsolate = setMark(UserMark.isolate)
  def setReportban = setMark(UserMark.reportban)
  def setRankban = setMark(UserMark.rankban)
  def setArenaBan = setMark(UserMark.arenaban)
  def setPrizeban = setMark(UserMark.prizeban)
  def setAlt = setMark(UserMark.alt)

  private[user] def setKid(user: User, v: KidMode) = coll.updateField($id(user.id), F.kid, v).void

  def isKid[U: UserIdOf](u: U): Fu[KidMode] = KidMode.from:
    coll.exists($id(u.id) ++ $doc(F.kid -> true))

  def updateTroll(user: User) = setTroll(user.id, user.marks.troll)

  def filterLame(ids: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some($inIds(ids) ++ lame))

  def filterNotKid(ids: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some($inIds(ids) ++ $doc(F.kid.$ne(true))))

  def filterKid[U: UserIdOf](ids: Seq[U]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some($inIds(ids.map(_.id)) ++ $doc(F.kid.$eq(true))))

  def isTroll(id: UserId): Fu[Boolean] = coll.exists($id(id) ++ trollSelect(true))

  def isBot(id: UserId): Fu[Boolean] = coll.exists($id(id) ++ botSelect(true))

  def isAlt(id: UserId): Fu[Boolean] = coll.exists($id(id) ++ markSelect(UserMark.alt)(true))

  def isCreatedSince(id: UserId, since: Instant): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.createdAt.$lt(since)))

  def setRoles(id: UserId, roles: List[RoleDbKey]): Funit =
    coll.updateOrUnsetField($id(id), F.roles, Option.when(roles.nonEmpty)(roles)).void

  def getRoles[U: UserIdOf](u: U): Fu[List[RoleDbKey]] =
    coll.primitiveOne[List[RoleDbKey]]($id(u.id), BSONFields.roles).dmap(_.orZero)

  def addPermission(id: UserId, perm: lila.core.perm.Permission): Funit =
    coll.update.one($id(id), $push(F.roles -> perm.dbKey)).void

  def accountAge(id: UserId): Fu[Days] =
    coll
      .primitiveOne[Instant]($id(id), F.createdAt)
      .map:
        _.fold(Days(0)): date =>
          Days(scalalib.time.daysBetween(date.withTimeAtStartOfDay, nowInstant.withTimeAtStartOfDay))

  def disableTwoFactor(id: UserId) = coll.update.one($id(id), $unset(F.totpSecret))

  def setupTwoFactor(id: UserId, totp: TotpSecret): Funit =
    coll.update
      .one(
        $id(id) ++ (F.totpSecret.$exists(false)), // never overwrite existing secret
        $set(F.totpSecret -> totp.secret)
      )
      .void

  def reopen(id: UserId) =
    coll.updateField($id(id), F.enabled, true) >>
      coll.update
        .one(
          $id(id) ++ $doc(F.email.$exists(false)),
          $doc("$rename" -> $doc(F.prevEmail -> F.email)) ++
            $doc("$unset" -> $doc(F.delete -> true))
        )
        .void
        .recover(lila.db.recoverDuplicateKey(_ => ()))

  def disable(user: User, keepEmail: Boolean, forever: Boolean): Funit =
    val sets = $doc(F.enabled -> false).++(forever.so($doc(F.foreverClosed -> true)))
    val unsets = List(F.roles.some, keepEmail.option(F.mustConfirmEmail)).flatten
    coll.update
      .one(
        $id(user.id),
        $doc("$set" -> sets) ++
          $unset(unsets) ++
          keepEmail.not.so($doc("$rename" -> $doc(F.email -> F.prevEmail)))
      )
      .void

  object delete:

    def nowWithTosViolation(user: User) =
      import F.*
      val fields = List(
        profile,
        roles,
        toints,
        "time",
        kid,
        lang,
        title,
        plan,
        totpSecret,
        changedCase,
        blind,
        salt,
        bpass,
        "mustConfirmEmail",
        colorIt,
        F.foreverClosed,
        F.delete
      )
      coll.update.one(
        $id(user.id),
        $unset(fields) ++ $set("deletedAt" -> nowInstant)
      )

    def nowFully(user: User) = for
      lockEmail <- emailOrPrevious(user.id)
      _ <- coll.update.one(
        $id(user.id),
        $doc(
          "prevEmail" -> lockEmail,
          "createdAt" -> user.createdAt,
          "deletedAt" -> nowInstant
        )
      )
    yield ()

    def findNextScheduled: Fu[Option[User]] =
      val requestedAt = nowInstant.minusDays(7)
      coll
        .find:
          $doc( // hits the delete.requested_1 index
            s"${F.delete}.requested".$lt(requestedAt),
            s"${F.delete}.done" -> false
          )
        .sort($doc(s"${F.delete}.requested" -> 1))
        .one[User]

    def schedule(userId: UserId, delete: Option[UserDelete]): Funit =
      coll.updateOrUnsetField($id(userId), F.delete, delete).void

  def getPasswordHash(id: UserId): Fu[Option[String]] =
    coll.byId[AuthData](id, AuthData.projection).map2(_.bpass.bytes.sha512.hex)

  def blankPassword(id: UserId): Funit =
    coll.updateField($id(id), F.bpass, HashedPassword(Array.empty)).void

  def setEmail(id: UserId, email: EmailAddress): Funit =
    val normalized = email.normalize
    coll.update
      .one(
        $id(id),
        if email.value == normalized.value then
          $set(F.email -> normalized) ++ $unset(F.prevEmail, F.verbatimEmail)
        else $set(F.email -> normalized, F.verbatimEmail -> email) ++ $unset(F.prevEmail)
      )
      .map: _ =>
        lila.common.Bus.pub(lila.core.user.ChangeEmail(id, email))

  private[user] def anyEmail(doc: Bdoc): Option[EmailAddress] =
    doc.getAsOpt[EmailAddress](F.verbatimEmail).orElse(doc.getAsOpt[EmailAddress](F.email))

  private def anyEmailOrPrevious(doc: Bdoc): Option[EmailAddress] =
    anyEmail(doc).orElse(doc.getAsOpt[EmailAddress](F.prevEmail))

  def email(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true).some)
      .one[Bdoc]
      .mapz(anyEmail)

  def emailOrPrevious(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .mapz(anyEmailOrPrevious)

  def enabledWithEmail(email: NormalizedEmailAddress): Fu[Option[(User, EmailAddress)]] =
    coll
      .find($doc(F.email -> email, F.enabled -> true))
      .one[Bdoc]
      .map: maybeDoc =>
        for
          doc <- maybeDoc
          storedEmail <- anyEmail(doc)
          user <- summon[BSONHandler[User]].readOpt(doc)
        yield (user, storedEmail)

  def prevEmail(id: UserId): Fu[Option[EmailAddress]] =
    coll.primitiveOne[EmailAddress]($id(id), F.prevEmail)

  def currentOrPrevEmail(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .mapz: doc =>
        anyEmail(doc).orElse(doc.getAsOpt[EmailAddress](F.prevEmail))

  def emailMap(ids: List[UserId]): Fu[Map[UserId, EmailAddress]] =
    coll
      .find(
        $inIds(ids),
        $doc(F.verbatimEmail -> true, F.email -> true, F.prevEmail -> true).some
      )
      .cursor[Bdoc](ReadPref.sec)
      .listAll()
      .map: docs =>
        for
          doc <- docs
          email <- anyEmailOrPrevious(doc)
          id <- doc.getAsOpt[UserId](F.id)
        yield id -> email
      .dmap(_.toMap)

  def isManaged(id: UserId): Fu[Boolean] = email(id).dmap(_.exists(_.isNoReply))

  def botSelect(v: Boolean) =
    if v then $doc(F.title -> PlayerTitle.BOT)
    else $doc(F.title -> $ne(PlayerTitle.BOT))

  def botWithBioSelect = botSelect(true) ++ $doc(s"${F.profile}.bio" -> $exists(true))

  private[user] def botIds =
    coll.distinctEasy[UserId, Set]("_id", botSelect(true) ++ enabledSelect, _.sec)

  def getTitle(id: UserId): Fu[Option[PlayerTitle]] = coll.primitiveOne[PlayerTitle]($id(id), F.title)

  def hasTitle(id: UserId): Fu[Boolean] = getTitle(id).dmap(_.exists(PlayerTitle.BOT != _))

  def setPlan(user: User, plan: Option[Plan]): Funit =
    coll.updateOrUnsetField($id(user.id), BSONFields.plan, plan).void

  def setSeenAt(id: UserId): Unit =
    coll.updateFieldUnchecked($id(id), F.seenAt, nowInstant)

  def setLang(user: User, lang: play.api.i18n.Lang) =
    coll.updateField($id(user.id), "lang", lang.code).void

  def langOf(id: UserId): Fu[Option[String]] = coll.primitiveOne[String]($id(id), "lang")

  def filterByEnabledPatrons(userIds: List[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, $inIds(userIds) ++ enabledSelect ++ patronSelect, _.sec)

  def filterEnabled(userIds: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, $inIds(userIds) ++ enabledSelect, _.sec)

  def filterDisabled(userIds: Iterable[UserId]): Fu[Set[UserId]] =
    userIds.nonEmpty.so:
      coll.distinctEasy[UserId, Set](F.id, $inIds(userIds) ++ disabledSelect, _.sec)

  def containsDisabled(userIds: Iterable[UserId]): Fu[Boolean] =
    userIds.nonEmpty.so:
      coll.secondary.exists($inIds(userIds) ++ disabledSelect)

  def userIdsWithRoles(roles: List[RoleDbKey]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("_id", $doc("roles".$in(roles)))

  def countEngines(userIds: List[UserId]): Fu[Int] =
    coll.secondary.countSel($inIds(userIds) ++ engineSelect(true))

  def filterEngines(userIds: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, $inIds(userIds) ++ engineSelect(true), _.sec)

  def countLameOrTroll(userIds: List[UserId]): Fu[Int] =
    coll.secondary.countSel($inIds(userIds) ++ lameOrTroll)

  def containsEngine(userIds: List[UserId]): Fu[Boolean] =
    coll.exists($inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: UserId): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.mustConfirmEmail.$exists(true)))

  def setEmailConfirmed(id: UserId): Fu[Option[EmailAddress]] = for
    res <- coll.update.one($id(id) ++ $doc(F.mustConfirmEmail.$exists(true)), $unset(F.mustConfirmEmail))
    email <- (res.nModified == 1).so(email(id))
  yield email

  def setFlair(user: User, flair: Option[Flair]): Funit =
    coll.updateOrUnsetField($id(user.id), F.flair, flair).void

  def unsetFlairs(all: Set[(UserId, Flair)]): Funit = all.nonEmpty.so:
    all.toList.sequentiallyVoid: (userId, flair) =>
      coll.unsetField($id(userId) ++ $doc(BSONFields.flair -> flair), BSONFields.flair)

  def byIdAs[A: BSONDocumentReader](id: String, proj: Bdoc): Fu[Option[A]] =
    coll.one[A]($id(id), proj)

  def isDeleted(user: User): Fu[Boolean] =
    user.enabled.no.so:
      coll.exists($id(user.id) ++ $doc(s"${F.delete}.done" -> true))

  def isForeverClosed(user: User): Fu[Boolean] =
    user.enabled.no.so:
      coll.exists($id(user.id) ++ $doc(F.foreverClosed -> true))

  def filterClosedOrInactiveIds(since: Instant)(ids: Iterable[UserId]): Fu[List[UserId]] =
    coll.distinctEasy[UserId, List](F.id, $inIds(ids) ++ $or(disabledSelect, F.seenAt.$lt(since)), _.sec)

  def createdWithApiVersion(userId: UserId) =
    coll.primitiveOne[ApiVersion]($id(userId), F.createdWithApiVersion)

  private val defaultCount = lila.core.user.Count(0, 0, 0, 0, 0)

  private def newUser(
      name: UserName,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mobileApiVersion: Option[ApiVersion],
      mustConfirmEmail: Boolean,
      lang: Option[LangTag],
      kid: KidMode
  ) =
    val normalizedEmail = email.normalize
    val now = nowInstant
    $doc(
      F.id -> name.id,
      F.username -> name.value,
      F.email -> normalizedEmail,
      F.mustConfirmEmail -> mustConfirmEmail.option(now),
      F.bpass -> passwordHash,
      F.count -> defaultCount,
      F.enabled -> true,
      F.createdAt -> now,
      F.createdWithApiVersion -> mobileApiVersion,
      F.seenAt -> now,
      F.playTime -> PlayTime(0, 0, none),
      F.lang -> lang
    ) ++ {
      (email.value != normalizedEmail.value).so($doc(F.verbatimEmail -> email))
    } ++ {
      blind.so($doc(F.blind -> true))
    } ++ {
      kid.yes.so($doc(F.kid -> kid))
    }
