package lila.user

import cats.implicits._
import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.common.{ ApiVersion, EmailAddress, NormalizedEmailAddress, ThreadLocalRandom }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.rating.Glicko
import lila.rating.{ Perf, PerfType }

final class UserRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import User.{ userBSONHandler, ID, BSONFields => F }
  import Title.titleBsonHandler
  import UserMark.markBsonHandler

  def withColl[A](f: Coll => A): A = f(coll)

  val normalize = User normalize _

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledSelect).sort($sort desc "count.game").cursor[User]().list(nb)

  def byId(id: ID): Fu[Option[User]] = User.noGhost(id) ?? coll.byId[User](id)

  def byIds(ids: Iterable[ID]): Fu[List[User]] = coll.byIds[User](ids)

  def byIdsSecondary(ids: Iterable[ID]): Fu[List[User]] =
    coll.byIds[User](ids, ReadPreference.secondaryPreferred)

  def byEmail(email: NormalizedEmailAddress): Fu[Option[User]] = coll.one[User]($doc(F.email -> email))
  def byPrevEmail(
      email: NormalizedEmailAddress,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): Fu[List[User]] =
    coll.list[User]($doc(F.prevEmail -> email), readPreference)

  def idByEmail(email: NormalizedEmailAddress): Fu[Option[String]] =
    coll.primitiveOne[String]($doc(F.email -> email), "_id")

  def idCursor(
      selector: Bdoc,
      batchSize: Int = 0,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(implicit cp: CursorProducer[Bdoc]): cp.ProducedCursor = {
    coll
      .find(selector)
      .batchSize(batchSize)
      .cursor[Bdoc](readPreference)
  }

  def countRecentByPrevEmail(
      email: NormalizedEmailAddress,
      since: DateTime = DateTime.now.minusWeeks(1)
  ): Fu[Int] =
    coll.countSel($doc(F.prevEmail -> email, F.createdAt $gt since))

  def pair(x: Option[ID], y: Option[ID]): Fu[(Option[User], Option[User])] =
    coll.byIds[User](List(x, y).flatten) map { users =>
      x.??(xx => users.find(_.id == xx)) ->
        y.??(yy => users.find(_.id == yy))
    }

  def pair(x: ID, y: ID): Fu[Option[(User, User)]] =
    coll.byIds[User](List(x, y)) map { users =>
      for {
        xx <- users.find(_.id == x)
        yy <- users.find(_.id == y)
      } yield xx -> yy
    }

  def lichessAnd(id: ID) = pair(User.lichessId, id) map2 { case (lichess, user) =>
    Holder(lichess) -> user
  }

  def namePair(x: ID, y: ID): Fu[Option[(User, User)]] =
    pair(normalize(x), normalize(y))

  def byOrderedIds(ids: Seq[ID], readPreference: ReadPreference): Fu[List[User]] =
    coll.byOrderedIds[User, User.ID](ids, readPreference = readPreference)(_.id)

  def usersFromSecondary(userIds: Seq[ID]): Fu[List[User]] =
    byOrderedIds(userIds, ReadPreference.secondaryPreferred)

  def enabledByIds(ids: Iterable[ID]): Fu[List[User]] =
    coll.list[User](enabledSelect ++ $inIds(ids), ReadPreference.secondaryPreferred)

  def enabledById(id: ID): Fu[Option[User]] =
    User.noGhost(id) ?? coll.one[User](enabledSelect ++ $id(id))

  def isEnabled(id: ID): Fu[Boolean] =
    User.noGhost(id) ?? coll.exists(enabledSelect ++ $id(id))

  def disabledById(id: ID): Fu[Option[User]] =
    User.noGhost(id) ?? coll.one[User](disabledSelect ++ $id(id))

  def named(username: String): Fu[Option[User]] =
    User.noGhost(username) ?? coll.byId[User](normalize(username)).recover {
      case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => none // probably GDPRed user
    }

  def enabledNameds(usernames: List[String]): Fu[List[User]] =
    coll
      .find($inIds(usernames map normalize) ++ enabledSelect)
      .cursor[User](ReadPreference.secondaryPreferred)
      .list()

  def enabledNamed(username: String): Fu[Option[User]] = enabledById(normalize(username))

  // expensive, send to secondary
  def byIdsSortRatingNoBot(ids: Iterable[ID], nb: Int): Fu[List[User]] =
    coll
      .find(
        $doc(
          F.enabled -> true,
          F.marks $nin List(UserMark.Engine.key, UserMark.Boost.key),
          "perfs.standard.gl.d" $lt Glicko.provisionalDeviation
        ) ++ $inIds(ids) ++ botSelect(false)
      )
      .sort($sort desc "perfs.standard.gl.r")
      .cursor[User](ReadPreference.secondaryPreferred)
      .list(nb)

  def botsByIds(ids: Iterable[ID]): Fu[List[User]] =
    coll.list[User]($inIds(ids) ++ botSelect(true), ReadPreference.secondaryPreferred)

  def usernameById(id: ID) =
    coll.primitiveOne[User.ID]($id(id), F.username)

  def usernamesByIds(ids: List[ID]) =
    coll.distinctEasy[String, List](F.username, $inIds(ids), ReadPreference.secondaryPreferred)

  def createdAtById(id: ID) =
    coll.primitiveOne[DateTime]($id(id), F.createdAt)

  def orderByGameCount(u1: User.ID, u2: User.ID): Fu[Option[(User.ID, User.ID)]] = {
    coll
      .find(
        $inIds(List(u1, u2)),
        $doc(s"${F.count}.game" -> true).some
      )
      .cursor[Bdoc]()
      .list() map { docs =>
      docs
        .sortBy {
          _.child(F.count).flatMap(_.int("game"))
        }
        .flatMap(_.string("_id")) match {
        case List(u1, u2) => (u1, u2).some
        case _            => none
      }
    }
  }

  def firstGetsWhite(u1: User.ID, u2: User.ID): Fu[Boolean] =
    coll
      .find(
        $inIds(List(u1, u2)),
        $id(true).some
      )
      .sort($doc(F.colorIt -> 1))
      .one[Bdoc]
      .map {
        _.fold(ThreadLocalRandom.nextBoolean()) { doc =>
          doc.string("_id") contains u1
        }
      }
      .addEffect { v =>
        incColor(u1, if (v) 1 else -1)
        incColor(u2, if (v) -1 else 1)
      }

  def firstGetsWhite(u1O: Option[User.ID], u2O: Option[User.ID]): Fu[Boolean] =
    (u1O, u2O).mapN(firstGetsWhite) | fuccess(ThreadLocalRandom.nextBoolean())

  def incColor(userId: User.ID, value: Int): Unit =
    coll
      .update(ordered = false, WriteConcern.Unacknowledged)
      .one(
        $id(userId) ++ (value < 0).??($doc(F.colorIt $gt -3)),
        $inc(F.colorIt -> value)
      )
      .unit

  def lichess = byId(User.lichessId)

  val irwinId = "irwin"
  def irwin   = byId(irwinId)

  def setPerfs(user: User, perfs: Perfs, prev: Perfs) = {
    val diff = PerfType.all flatMap { pt =>
      perfs(pt).nb != prev(pt).nb option {
        BSONElement(
          s"${F.perfs}.${pt.key}",
          Perf.perfBSONHandler.write(perfs(pt))
        )
      }
    }
    diff.nonEmpty ?? coll.update
      .one(
        $id(user.id),
        $doc("$set" -> $doc(diff: _*))
      )
      .void
  }

  def setManagedUserInitialPerfs(id: User.ID) = {
    coll.updateField($id(id), F.perfs, Perfs.perfsBSONHandler.write(Perfs.defaultManaged)).void
  }

  def setPerf(userId: String, pt: PerfType, perf: Perf) =
    coll.update
      .one(
        $id(userId),
        $set(
          s"${F.perfs}.${pt.key}" -> Perf.perfBSONHandler.write(perf)
        )
      )
      .void

  def addStormRun  = addStormLikeRun("storm") _
  def addRacerRun  = addStormLikeRun("racer") _
  def addStreakRun = addStormLikeRun("streak") _

  private def addStormLikeRun(field: String)(userId: User.ID, score: Int): Funit = {
    val inc = $inc(s"perfs.$field.runs" -> 1)
    coll.update
      .one(
        $id(userId),
        $inc(s"perfs.$field.runs" -> 1) ++
          $doc("$max"             -> $doc(s"perfs.$field.score" -> score))
      )
      .void
  }

  def setProfile(id: ID, profile: Profile): Funit =
    coll.update
      .one(
        $id(id),
        $set(F.profile -> Profile.profileBSONHandler.writeTry(profile).get)
      )
      .void

  def setUsernameCased(id: ID, username: String): Funit = {
    if (id == username.toLowerCase) {
      coll.update.one(
        $id(id) ++ (F.changedCase $exists false),
        $set(F.username -> username, F.changedCase -> true)
      ) flatMap { result =>
        if (result.n == 0) fufail(s"You have already changed your username")
        else funit
      }
    } else fufail(s"Proposed username $username does not match old username $id")
  }

  def addTitle(id: ID, title: Title): Funit =
    coll.updateField($id(id), F.title, title).void

  def removeTitle(id: ID): Funit =
    coll.unsetField($id(id), F.title).void

  def getPlayTime(id: ID): Fu[Option[User.PlayTime]] =
    coll.primitiveOne[User.PlayTime]($id(id), F.playTime)

  val enabledSelect  = $doc(F.enabled -> true)
  val disabledSelect = $doc(F.enabled -> false)
  def markSelect(mark: UserMark)(v: Boolean): Bdoc =
    if (v) $doc(F.marks -> mark.key)
    else F.marks $ne mark.key
  def engineSelect = markSelect(UserMark.Engine) _
  def trollSelect  = markSelect(UserMark.Troll) _
  val lameOrTroll = $or(
    $doc(F.marks -> UserMark.Engine.key),
    $doc(F.marks -> UserMark.Boost.key),
    $doc(F.marks -> UserMark.Troll.key)
  )
  def stablePerfSelect(perf: String) =
    $doc(s"perfs.$perf.gl.d" -> $lt(lila.rating.Glicko.provisionalDeviation))
  val patronSelect = $doc(s"${F.plan}.active" -> true)

  def sortPerfDesc(perf: String) = $sort desc s"perfs.$perf.gl.r"
  val sortCreatedAtDesc          = $sort desc F.createdAt

  def glicko(userId: ID, perfType: PerfType): Fu[Glicko] =
    coll
      .find($id(userId), $doc(s"${F.perfs}.${perfType.key}.gl" -> true).some)
      .one[Bdoc]
      .dmap {
        _.flatMap(_ child F.perfs)
          .flatMap(_ child perfType.key)
          .flatMap(_.getAsOpt[Glicko]("gl")) | Glicko.default
      }

  def incNbGames(
      id: ID,
      rated: Boolean,
      ai: Boolean,
      result: Int,
      totalTime: Option[Int],
      tvTime: Option[Int]
  ) = {
    val incs: List[BSONElement] = List(
      "count.game".some,
      rated option "count.rated",
      ai option "count.ai",
      (result match {
        case -1 => "count.loss".some
        case 1  => "count.win".some
        case 0  => "count.draw".some
        case _  => none
      }),
      (result match {
        case -1 => "count.lossH".some
        case 1  => "count.winH".some
        case 0  => "count.drawH".some
        case _  => none
      }) ifFalse ai
    ).flatten.map(k => BSONElement(k, BSONInteger(1))) ::: List(
      totalTime map (v => BSONElement(s"${F.playTime}.total", BSONInteger(v + 2))),
      tvTime map (v => BSONElement(s"${F.playTime}.tv", BSONInteger(v + 2)))
    ).flatten

    coll.update.one($id(id), $inc($doc(incs: _*)))
  }

  def incToints(id: ID, nb: Int) = coll.update.one($id(id), $inc("toints" -> nb))
  def removeAllToints            = coll.update.one($empty, $unset("toints"), multi = true)

  def create(
      username: String,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mobileApiVersion: Option[ApiVersion],
      mustConfirmEmail: Boolean,
      lang: Option[String] = None
  ): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        val doc = newUser(username, passwordHash, email, blind, mobileApiVersion, mustConfirmEmail, lang) ++
          ("len" -> BSONInteger(username.length))
        coll.insert.one(doc) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = idExists(normalize(username))
  def idExists(id: String): Fu[Boolean]         = coll exists $id(id)

  /** Filters out invalid usernames and returns the IDs for those usernames
    *
    * @param usernames Usernames to filter out the non-existent usernames from, and return the IDs for
    * @return A list of IDs for the usernames that were given that were valid
    */
  def existingUsernameIds(usernames: Set[String]): Fu[List[User.ID]] =
    coll.primitive[String]($inIds(usernames.map(normalize)), F.id)

  def userIdsLikeWithRole(text: String, role: String, max: Int = 10): Fu[List[User.ID]] =
    userIdsLikeFilter(text, $doc(F.roles -> role), max)

  private[user] def userIdsLikeFilter(text: String, filter: Bdoc, max: Int): Fu[List[User.ID]] =
    User.couldBeUsername(text) ?? {
      coll
        .find(
          $doc(F.id $startsWith normalize(text)) ++ enabledSelect ++ filter,
          $doc(F.id -> true).some
        )
        .sort($doc("len" -> 1))
        .cursor[Bdoc](ReadPreference.secondaryPreferred)
        .list(max)
        .map {
          _ flatMap { _.string(F.id) }
        }
    }

  private def setMark(mark: UserMark)(id: ID, v: Boolean): Funit =
    coll.update.one($id(id), $addOrPull(F.marks, mark, v)).void

  def setEngine    = setMark(UserMark.Engine) _
  def setBoost     = setMark(UserMark.Boost) _
  def setTroll     = setMark(UserMark.Troll) _
  def setReportban = setMark(UserMark.Reportban) _
  def setRankban   = setMark(UserMark.Rankban) _
  def setAlt       = setMark(UserMark.Alt) _

  def setKid(user: User, v: Boolean) = coll.updateField($id(user.id), F.kid, v).void

  def isKid(id: ID) = coll.exists($id(id) ++ $doc(F.kid -> true))

  def updateTroll(user: User) = setTroll(user.id, user.marks.troll)

  def filterEngine(ids: Seq[ID]): Fu[Set[ID]] =
    coll.distinct[ID, Set]("_id", Some($inIds(ids) ++ engineSelect(true)))

  def isTroll(id: ID): Fu[Boolean] = coll.exists($id(id) ++ trollSelect(true))

  def isCreatedSince(id: ID, since: DateTime): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.createdAt $lt since))

  def setRoles(id: ID, roles: List[String]): Funit =
    coll.updateField($id(id), F.roles, roles).void

  def disableTwoFactor(id: ID) = coll.update.one($id(id), $unset(F.totpSecret))

  def setupTwoFactor(id: ID, totp: TotpSecret): Funit =
    coll.update
      .one(
        $id(id) ++ (F.totpSecret $exists false), // never overwrite existing secret
        $set(F.totpSecret -> totp.secret)
      )
      .void

  def reopen(id: ID) =
    coll.updateField($id(id), F.enabled, true) >>
      coll.update
        .one(
          $id(id) ++ $doc(F.email $exists false),
          $doc("$rename" -> $doc(F.prevEmail -> F.email))
        )
        .void
        .recover(lila.db.recoverDuplicateKey(_ => ()))

  def disable(user: User, keepEmail: Boolean): Funit =
    coll.update
      .one(
        $id(user.id),
        $set(F.enabled -> false) ++ $unset(F.roles) ++ {
          if (keepEmail) $unset(F.mustConfirmEmail)
          else $doc("$rename" -> $doc(F.email -> F.prevEmail))
        }
      )
      .void

  def isMonitoredMod(userId: User.ID) =
    coll.exists($id(userId) ++ $doc(F.roles -> "ROLE_MONITORED_MOD"))

  import Authenticator._
  def getPasswordHash(id: User.ID): Fu[Option[String]] =
    coll.byId[AuthData](id, authProjection) map {
      _.map { _.hashToken }
    }

  def setEmail(id: ID, email: EmailAddress): Funit = {
    val normalizedEmail = email.normalize
    coll.update
      .one(
        $id(id),
        $set(F.email -> normalizedEmail) ++ $unset(F.prevEmail) ++ {
          if (email.value == normalizedEmail.value) $unset(F.verbatimEmail)
          else $set(F.verbatimEmail -> email)
        }
      )
      .void
  }

  private def anyEmail(doc: Bdoc): Option[EmailAddress] =
    doc.getAsOpt[EmailAddress](F.verbatimEmail) orElse doc.getAsOpt[EmailAddress](F.email)

  private def anyEmailOrPrevious(doc: Bdoc): Option[EmailAddress] =
    anyEmail(doc) orElse doc.getAsOpt[EmailAddress](F.prevEmail)

  def email(id: ID): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true).some)
      .one[Bdoc]
      .map { _ ?? anyEmail }

  def emailOrPrevious(id: ID): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .map { _ ?? anyEmailOrPrevious }

  def enabledWithEmail(email: NormalizedEmailAddress): Fu[Option[(User, EmailAddress)]] =
    coll
      .find($doc(F.email -> email, F.enabled -> true))
      .one[Bdoc]
      .map { maybeDoc =>
        for {
          doc         <- maybeDoc
          storedEmail <- anyEmail(doc)
        } yield (userBSONHandler read doc, storedEmail)
      }

  def prevEmail(id: ID): Fu[Option[EmailAddress]] =
    coll.primitiveOne[EmailAddress]($id(id), F.prevEmail)

  def currentOrPrevEmail(id: ID): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .map {
        _ ?? { doc =>
          anyEmail(doc) orElse doc.getAsOpt[EmailAddress](F.prevEmail)
        }
      }

  def withEmails(name: String): Fu[Option[User.WithEmails]] =
    coll.find($id(normalize(name))).one[Bdoc].map {
      _ ?? { doc =>
        User
          .WithEmails(
            userBSONHandler read doc,
            User.Emails(
              current = anyEmail(doc),
              previous = doc.getAsOpt[NormalizedEmailAddress](F.prevEmail)
            )
          )
          .some
      }
    }

  def withEmails(names: List[String]): Fu[List[User.WithEmails]] =
    coll
      .list[Bdoc]($inIds(names map normalize), ReadPreference.secondaryPreferred)
      .map {
        _ map { doc =>
          User.WithEmails(
            userBSONHandler read doc,
            User.Emails(
              current = anyEmail(doc),
              previous = doc.getAsOpt[NormalizedEmailAddress](F.prevEmail)
            )
          )
        }
      }

  def withEmailsU(users: List[User]): Fu[List[User.WithEmails]] = withEmails(users.map(_.id))

  def emailMap(names: List[String]): Fu[Map[User.ID, EmailAddress]] =
    coll
      .find(
        $inIds(names map normalize),
        $doc(F.verbatimEmail -> true, F.email -> true, F.prevEmail -> true).some
      )
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .list()
      .map { docs =>
        docs.view
          .flatMap { doc =>
            anyEmailOrPrevious(doc) map { ~doc.getAsOpt[User.ID](F.id) -> _ }
          }
          .to(Map)
      }

  def hasEmail(id: ID): Fu[Boolean] = email(id).dmap(_.isDefined)

  def isManaged(id: ID): Fu[Boolean] = email(id).dmap(_.exists(_.isNoReply))

  def setBot(user: User): Funit =
    if (user.count.game > 0)
      fufail(lila.base.LilaInvalid("You already have games played. Make a new account."))
    else coll.updateField($id(user.id), F.title, Title.BOT).void

  private def botSelect(v: Boolean) =
    if (v) $doc(F.title -> Title.BOT)
    else $doc(F.title   -> $ne(Title.BOT))

  private[user] def botIds =
    coll.distinctEasy[String, Set](
      "_id",
      botSelect(true) ++ enabledSelect,
      ReadPreference.secondaryPreferred
    )

  def getTitle(id: ID): Fu[Option[Title]] = coll.primitiveOne[Title]($id(id), F.title)

  def setPlan(user: User, plan: Plan): Funit = {
    implicit val pbw: BSONWriter[Plan] = Plan.planBSONHandler
    coll.updateField($id(user.id), User.BSONFields.plan, plan).void
  }
  def unsetPlan(user: User): Funit = coll.unsetField($id(user.id), User.BSONFields.plan).void

  private def docPerf(doc: Bdoc, perfType: PerfType): Option[Perf] =
    doc.child(F.perfs).flatMap(_.getAsOpt[Perf](perfType.key))

  def perfOf(id: ID, perfType: PerfType): Fu[Option[Perf]] =
    coll
      .find(
        $id(id),
        $doc(s"${F.perfs}.${perfType.key}" -> true).some
      )
      .one[Bdoc]
      .dmap {
        _.flatMap { docPerf(_, perfType) }
      }

  def perfOf(ids: Iterable[ID], perfType: PerfType): Fu[Map[ID, Perf]] =
    coll
      .find(
        $inIds(ids),
        $doc(s"${F.perfs}.${perfType.key}" -> true).some
      )
      .cursor[Bdoc]()
      .collect[List](Int.MaxValue, err = Cursor.FailOnError[List[Bdoc]]())
      .map {
        _.view
          .map { doc =>
            ~doc.getAsOpt[ID]("_id") -> docPerf(doc, perfType).getOrElse(Perf.default)
          }
          .to(Map)
      }

  def setSeenAt(id: ID): Unit =
    coll.updateFieldUnchecked($id(id), F.seenAt, DateTime.now)

  def setLang(user: User, lang: play.api.i18n.Lang) =
    coll.updateField($id(user.id), "lang", lang.code).void

  def langOf(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), "lang")

  def filterByEnabledPatrons(userIds: List[User.ID]): Fu[Set[User.ID]] =
    coll.distinctEasy[String, Set](
      F.id,
      $inIds(userIds) ++ enabledSelect ++ patronSelect,
      ReadPreference.secondaryPreferred
    )

  def filterEnabled(userIds: Seq[User.ID]): Fu[Set[User.ID]] =
    coll.distinctEasy[String, Set](F.id, $inIds(userIds) ++ enabledSelect, ReadPreference.secondaryPreferred)

  def userIdsWithRoles(roles: List[String]): Fu[Set[User.ID]] =
    coll.distinctEasy[String, Set]("_id", $doc("roles" $in roles))

  def countEngines(userIds: List[User.ID]): Fu[Int] =
    coll.secondaryPreferred.countSel($inIds(userIds) ++ engineSelect(true))

  def countLameOrTroll(userIds: List[User.ID]): Fu[Int] =
    coll.secondaryPreferred.countSel($inIds(userIds) ++ lameOrTroll)

  def containsEngine(userIds: List[User.ID]): Fu[Boolean] =
    coll.exists($inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: User.ID): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.mustConfirmEmail $exists true))

  def setEmailConfirmed(id: User.ID): Fu[Option[EmailAddress]] =
    coll.update.one($id(id) ++ $doc(F.mustConfirmEmail $exists true), $unset(F.mustConfirmEmail)) flatMap {
      res =>
        (res.nModified == 1) ?? email(id)
    }

  private val speakerProjection = $doc(
    F.username -> true,
    F.title    -> true,
    F.plan     -> true,
    F.enabled  -> true,
    F.marks    -> true
  )

  def speaker(id: User.ID): Fu[Option[User.Speaker]] = {
    import User.speakerHandler
    coll.one[User.Speaker]($id(id), speakerProjection)
  }

  def contacts(orig: User.ID, dest: User.ID): Fu[Option[User.Contacts]] = {
    import User.contactHandler
    coll.byOrderedIds[User.Contact, User.ID](
      List(orig, dest),
      $doc(F.kid -> true, F.marks -> true, F.roles -> true, F.createdAt -> true).some
    )(_._id) map {
      case List(o, d) => User.Contacts(o, d).some
      case _          => none
    }
  }

  def isErased(user: User): Fu[User.Erased] =
    user.disabled ?? {
      coll.exists($id(user.id) ++ $doc(F.erasedAt $exists true))
    } map User.Erased.apply

  def byIdNotErased(id: ID): Fu[Option[User]] = coll.one[User]($id(id) ++ $doc(F.erasedAt $exists false))

  def filterClosedOrInactiveIds(since: DateTime)(ids: Iterable[ID]): Fu[List[ID]] =
    coll.distinctEasy[ID, List](
      F.id,
      $inIds(ids) ++ $or(disabledSelect, F.seenAt $lt since),
      ReadPreference.secondaryPreferred
    )

  def setEraseAt(user: User) =
    coll.updateField($id(user.id), F.eraseAt, DateTime.now plusDays 1).void

  private def newUser(
      username: String,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mobileApiVersion: Option[ApiVersion],
      mustConfirmEmail: Boolean,
      lang: Option[String]
  ) = {

    implicit def countHandler = Count.countBSONHandler
    import lila.db.BSON.BSONJodaDateTimeHandler

    val normalizedEmail = email.normalize
    $doc(
      F.id                    -> normalize(username),
      F.username              -> username,
      F.email                 -> normalizedEmail,
      F.mustConfirmEmail      -> mustConfirmEmail.option(DateTime.now),
      F.bpass                 -> passwordHash,
      F.perfs                 -> $empty,
      F.count                 -> Count.default,
      F.enabled               -> true,
      F.createdAt             -> DateTime.now,
      F.createdWithApiVersion -> mobileApiVersion.map(_.value),
      F.seenAt                -> DateTime.now,
      F.playTime              -> User.PlayTime(0, 0),
      F.lang                  -> lang
    ) ++ {
      (email.value != normalizedEmail.value) ?? $doc(F.verbatimEmail -> email)
    } ++ {
      if (blind) $doc(F.blind -> true) else $empty
    }
  }
}
