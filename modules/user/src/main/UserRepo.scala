package lila.user

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.ApiVersion
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.rating.{ Glicko, Perf, PerfType }

object UserRepo {

  import User.userBSONHandler

  import User.ID
  import User.{ BSONFields => F }

  // dirty
  private val coll = Env.current.userColl
  import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, Project, Group, GroupField, SumField, SumValue }

  val normalize = User normalize _

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledSelect).sort($sort desc "count.game").cursor[User]().gather[List](nb)

  def byId(id: ID): Fu[Option[User]] = coll.byId[User](id)

  def byIds(ids: Iterable[ID]): Fu[List[User]] = coll.byIds[User](ids)

  def byEmail(email: String): Fu[Option[User]] = coll.uno[User]($doc(F.email -> email))

  def idByEmail(email: String): Fu[Option[String]] =
    coll.primitiveOne[String]($doc(F.email -> email), "_id")

  def enabledByEmail(email: String): Fu[Option[User]] = byEmail(email) map (_ filter (_.enabled))

  def pair(x: Option[ID], y: Option[ID]): Fu[(Option[User], Option[User])] =
    coll.byIds[User](List(x, y).flatten) map { users =>
      x.??(xx => users.find(_.id == xx)) ->
        y.??(yy => users.find(_.id == yy))
    }

  def byOrderedIds(ids: Seq[ID]): Fu[List[User]] =
    coll.byOrderedIds[User](ids)(_.id)

  def enabledByIds(ids: Iterable[ID]): Fu[List[User]] =
    coll.list[User](enabledSelect ++ $inIds(ids))

  def enabledById(id: ID): Fu[Option[User]] =
    coll.uno[User](enabledSelect ++ $id(id))

  def named(username: String): Fu[Option[User]] = coll.byId[User](normalize(username))

  def nameds(usernames: List[String]): Fu[List[User]] = coll.byIds[User](usernames.map(normalize))

  // expensive, send to secondary
  def byIdsSortRating(ids: Iterable[ID], nb: Int) =
    coll.find($inIds(ids) ++ goodLadSelectBson)
      .sort($sort desc "perfs.standard.gl.r")
      .cursor[User](ReadPreference.secondaryPreferred)
      .gather[List](nb)

  // expensive, send to secondary
  def idsByIdsSortRating(ids: Iterable[ID], nb: Int): Fu[List[User.ID]] =
    coll.find(
      $inIds(ids) ++ goodLadSelectBson,
      $id(true))
      .sort($doc(s"perfs.standard.gl.r" -> -1))
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .gather[List](nb).map {
        _.flatMap { _.getAs[String]("_id") }
      }

  def allSortToints(nb: Int) =
    coll.find($empty).sort($sort desc F.toints).cursor[User]().gather[List](nb)

  def usernameById(id: ID) =
    coll.primitiveOne[String]($id(id), F.username)

  def usernamesByIds(ids: List[ID]) =
    coll.distinct(F.username, $inIds(ids).some) map lila.db.BSON.asStrings

  def orderByGameCount(u1: String, u2: String): Fu[Option[(String, String)]] = {
    coll.find(
      $doc("_id".$in(u1, u2)),
      $doc(s"${F.count}.game" -> true)
    ).cursor[Bdoc]().gather[List]() map { docs =>
        docs.sortBy {
          _.getAs[Bdoc](F.count).flatMap(_.getAs[BSONNumberLike]("game")).??(_.toInt)
        }.map(_.getAs[String]("_id")).flatten match {
          case List(u1, u2) => (u1, u2).some
          case _            => none
        }
      }
  }

  def firstGetsWhite(u1O: Option[String], u2O: Option[String]): Fu[Boolean] =
    (u1O |@| u2O).tupled.fold(fuccess(scala.util.Random.nextBoolean)) {
      case (u1, u2) => coll.find(
        $doc("_id".$in(u1, u2)),
        $id(true)
      ).sort($doc(F.colorIt -> 1)).uno[Bdoc].map {
          _.fold(scala.util.Random.nextBoolean) { doc =>
            doc.getAs[String]("_id") contains u1
          }
        }.addEffect { v =>
          incColor(u1, v.fold(1, -1))
          incColor(u2, v.fold(-1, 1))
        }
    }

  def incColor(userId: User.ID, value: Int): Unit =
    coll.uncheckedUpdate($id(userId), $inc(F.colorIt -> value))

  val lichessId = "lichess"
  def lichess = byId(lichessId)

  def setPerfs(user: User, perfs: Perfs, prev: Perfs) = {
    val diff = PerfType.all flatMap { pt =>
      perfs(pt).nb != prev(pt).nb option {
        s"perfs.${pt.key}" -> Perf.perfBSONHandler.write(perfs(pt))
      }
    }
    diff.nonEmpty ?? coll.update(
      $id(user.id),
      $doc("$set" -> $doc(diff))
    ).void
  }

  def setPerf(userId: String, perfName: String, perf: Perf) =
    coll.update($id(userId), $set(
      s"${F.perfs}.$perfName" -> Perf.perfBSONHandler.write(perf)
    )).void

  def setProfile(id: ID, profile: Profile): Funit =
    coll.update(
      $id(id),
      $set(F.profile -> Profile.profileBSONHandler.write(profile))
    ).void

  def setTitle(id: ID, title: Option[String]): Funit = title match {
    case Some(t) => coll.updateField($id(id), F.title, t).void
    case None    => coll.update($id(id), $unset(F.title)).void
  }

  def setPlayTime(u: User, playTime: User.PlayTime): Funit =
    coll.update($id(u.id), $set(F.playTime -> User.playTimeHandler.write(playTime))).void

  val enabledSelect = $doc(F.enabled -> true)
  def engineSelect(v: Boolean) = $doc(F.engine -> v.fold[BSONValue]($boolean(true), $ne(true)))
  def trollSelect(v: Boolean) = $doc(F.troll -> v.fold[BSONValue]($boolean(true), $ne(true)))
  def boosterSelect(v: Boolean) = $doc(F.booster -> v.fold[BSONValue]($boolean(true), $ne(true)))
  def stablePerfSelect(perf: String) = $doc(
    s"perfs.$perf.nb" -> $gte(30),
    s"perfs.$perf.gl.d" -> $lt(lila.rating.Glicko.provisionalDeviation))
  val goodLadSelect = enabledSelect ++ engineSelect(false) ++ boosterSelect(false)
  val goodLadSelectBson = $doc(
    F.enabled -> true,
    F.engine -> $doc("$ne" -> true),
    F.booster -> $doc("$ne" -> true))

  def sortPerfDesc(perf: String) = $sort desc s"perfs.$perf.gl.r"
  val sortCreatedAtDesc = $sort desc F.createdAt

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Int, totalTime: Option[Int], tvTime: Option[Int]) = {
    val incs: List[(String, BSONInteger)] = List(
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
    ).flatten.map(_ -> BSONInteger(1)) ::: List(
        totalTime map BSONInteger.apply map (s"${F.playTime}.total" -> _),
        tvTime map BSONInteger.apply map (s"${F.playTime}.tv" -> _)
      ).flatten

    coll.update($id(id), $inc(incs))
  }

  def incToints(id: ID, nb: Int) = coll.update($id(id), $inc("toints" -> nb))
  def removeAllToints = coll.update($empty, $unset("toints"), multi = true)

  def authenticateById(id: ID, password: String): Fu[Option[User]] =
    checkPasswordById(id) map { _ flatMap { _(password) } }

  def authenticateByEmail(email: String, password: String): Fu[Option[User]] =
    checkPasswordByEmail(email) map { _ flatMap { _(password) } }

  private case class AuthData(password: String, salt: String, sha512: Option[Boolean]) {
    def compare(p: String) = password == (~sha512).fold(hash512(p, salt), hash(p, salt))
  }

  private implicit val AuthDataBSONHandler = Macros.handler[AuthData]

  def checkPasswordById(id: ID): Fu[Option[User.LoginCandidate]] =
    checkPassword($id(id))

  def checkPasswordByEmail(email: String): Fu[Option[User.LoginCandidate]] =
    checkPassword($doc(F.email -> email))

  private def checkPassword(select: Bdoc): Fu[Option[User.LoginCandidate]] =
    coll.uno[AuthData](select) zip coll.uno[User](select) map {
      case (Some(login), Some(user)) if user.enabled => User.LoginCandidate(user, login.compare).some
      case _                                         => none
    }

  def getPasswordHash(id: ID): Fu[Option[String]] =
    coll.primitiveOne[String]($id(id), "password")

  def create(username: String, password: String, email: Option[String], blind: Boolean, mobileApiVersion: Option[ApiVersion]): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        val doc = newUser(username, password, email, blind, mobileApiVersion) ++
          ("len" -> BSONInteger(username.size))
        coll.insert(doc) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = idExists(normalize(username))
  def idExists(id: String): Fu[Boolean] = coll exists $id(id)

  /**
   * Filters out invalid usernames and returns the IDs for those usernames
   *
   * @param usernames Usernames to filter out the non-existent usernames from, and return the IDs for
   * @return A list of IDs for the usernames that were given that were valid
   */
  def existingUsernameIds(usernames: Set[String]): Fu[List[String]] =
    coll.primitive[String]($inIds(usernames.map(normalize)), "_id")

  def engineIds: Fu[Set[String]] =
    coll.distinct("_id", $doc("engine" -> true).some) map lila.db.BSON.asStringSet

  private val userIdPattern = """^[\w-]{3,20}$""".r.pattern

  def usernamesLike(text: String, max: Int = 10): Fu[List[String]] = {
    val id = normalize(text)
    if (!userIdPattern.matcher(id).matches) fuccess(Nil)
    else {
      import java.util.regex.Matcher.quoteReplacement
      val regex = "^" + id + ".*$"
      coll.find($doc("_id".$regex(regex, "")), $doc(F.username -> true))
        .sort($doc("enabled" -> -1, "len" -> 1))
        .cursor[Bdoc](ReadPreference.secondaryPreferred).gather[List](max)
        .map {
          _ flatMap { _.getAs[String](F.username) }
        }
    }
  }

  def toggleEngine(id: ID): Funit =
    coll.fetchUpdate[User]($id(id)) { u =>
      $set("engine" -> !u.engine)
    }

  def setEngine(id: ID, v: Boolean): Funit = coll.updateField($id(id), "engine", v).void

  def setBooster(id: ID, v: Boolean): Funit = coll.updateField($id(id), "booster", v).void

  def toggleIpBan(id: ID) = coll.fetchUpdate[User]($id(id)) { u => $set("ipBan" -> !u.ipBan) }

  def toggleKid(user: User) = coll.updateField($id(user.id), "kid", !user.kid)

  def isKid(id: ID) = coll.exists($id(id) ++ $doc("kid" -> true))

  def updateTroll(user: User) = coll.updateField($id(user.id), "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = coll.exists($id(id) ++ engineSelect(true))

  def isTroll(id: ID): Fu[Boolean] = coll.exists($id(id) ++ trollSelect(true))

  def setRoles(id: ID, roles: List[String]) = coll.updateField($id(id), "roles", roles)

  def enable(id: ID) = coll.updateField($id(id), "enabled", true)

  def disable(user: User) = coll.update(
    $id(user.id),
    $doc("$set" -> $doc("enabled" -> false)) ++
      user.lameOrTroll.fold(
        $doc(),
        $doc("$unset" -> $doc("email" -> true))
      )
  )

  def passwd(id: ID, password: String): Funit =
    coll.primitiveOne[String]($id(id), "salt") flatMap { saltOption =>
      saltOption ?? { salt =>
        coll.update($id(id), $set(
          "password" -> hash(password, salt),
          "sha512" -> false)).void
      }
    }

  def email(id: ID, email: String): Funit = coll.updateField($id(id), F.email, email).void

  def email(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.email)

  def hasEmail(id: ID): Fu[Boolean] = email(id).map(_.isDefined)

  def setPlan(user: User, plan: Plan): Funit = {
    implicit val pbw: BSONValueWriter[Plan] = Plan.planBSONHandler
    coll.updateField($id(user.id), "plan", plan).void
  }

  def perfOf(id: ID, perfType: PerfType): Fu[Option[Perf]] = coll.find(
    $id(id),
    $doc(s"${F.perfs}.${perfType.key}" -> true)
  ).uno[Bdoc].map {
      _.flatMap(_.getAs[Bdoc](F.perfs)).flatMap(_.getAs[Perf](perfType.key))
    }

  def setSeenAt(id: ID) {
    coll.updateFieldUnchecked($id(id), "seenAt", DateTime.now)
  }

  def recentlySeenNotKidIdsCursor(since: DateTime) =
    coll.find($doc(
      F.enabled -> true,
      "seenAt" -> $doc("$gt" -> since),
      "count.game" -> $doc("$gt" -> 9),
      "kid" -> $doc("$ne" -> true)
    ), $id(true)).cursor[Bdoc]()

  def setLang(id: ID, lang: String) = coll.updateField($id(id), "lang", lang).void

  def idsSumToints(ids: Iterable[String]): Fu[Int] =
    ids.nonEmpty ?? coll.aggregate(Match($inIds(ids)),
      List(Group(BSONNull)(F.toints -> SumField(F.toints)))).map(
        _.documents.headOption flatMap { _.getAs[Int](F.toints) }
      ).map(~_)

  def filterByEngine(userIds: List[String]): Fu[List[String]] =
    coll.primitive[String]($inIds(userIds) ++ engineSelect(true), F.username)

  def countEngines(userIds: List[String]): Fu[Int] =
    coll.countSel($inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: String): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.mustConfirmEmail $exists true))

  def setEmailConfirmed(id: String): Funit = coll.update($id(id), $unset(F.mustConfirmEmail)).void

  private def newUser(username: String, password: String, email: Option[String], blind: Boolean, mobileApiVersion: Option[ApiVersion]) = {

    val salt = ornicar.scalalib.Random nextStringUppercase 32
    implicit def countHandler = Count.countBSONHandler
    implicit def perfsHandler = Perfs.perfsBSONHandler
    import lila.db.BSON.BSONJodaDateTimeHandler

    $doc(
      F.id -> normalize(username),
      F.username -> username,
      F.email -> email,
      F.mustConfirmEmail -> (email.isDefined && mobileApiVersion.isEmpty).option(DateTime.now),
      "password" -> hash(password, salt),
      "salt" -> salt,
      F.perfs -> $empty,
      F.count -> Count.default,
      F.enabled -> true,
      F.createdAt -> DateTime.now,
      F.createdWithApiVersion -> mobileApiVersion.map(_.value),
      F.seenAt -> DateTime.now) ++ {
        if (blind) $doc("blind" -> true) else $empty
      }
  }

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}
