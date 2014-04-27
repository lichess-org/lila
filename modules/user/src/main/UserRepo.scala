package lila.user

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.rating.{ Glicko, Perf }
import tube.userTube

object UserRepo extends UserRepo {
  protected def userTube = tube.userTube
}

trait UserRepo {

  protected implicit def userTube: lila.db.BsTubeInColl[User]

  import User.ID

  val normalize = User normalize _

  def all: Fu[List[User]] = $find.all

  def topRating(nb: Int): Fu[List[User]] =
    $find($query(stableGoodLadSelect) sort sortRatingDesc, nb)
  def topRatingSince(since: DateTime, nb: Int): Fu[List[User]] =
    $find($query(stableGoodLadSelect ++ perfSince("global", since)) sort sortRatingDesc, nb)

  def topProgress(nb: Int): Fu[List[User]] =
    $find($query(stableGoodLadSelect) sort sortProgressDesc, nb)

  def topProgressSince(since: DateTime, nb: Int): Fu[List[User]] =
    $find($query(stableGoodLadSelect ++ minRatingSelect(1500) ++ perfSince("global", since)) sort sortProgressDesc, nb)

  def topBullet = topPerf("bullet") _
  def topBlitz = topPerf("blitz") _
  def topSlow = topPerf("slow") _

  def topPerf(perf: String)(nb: Int): Fu[List[User]] =
    $find($query(
      goodLadSelect ++ Json.obj(s"perfs.$perf.gl.d" -> $lt(100))
    ) sort ($sort desc s"perfs.$perf.gl.r"), nb)

  def topNbGame(nb: Int): Fu[List[User]] =
    $find($query(enabledSelect) sort ($sort desc "count.game"), nb)

  def byId(id: ID): Fu[Option[User]] = $find byId id

  def byIds(ids: Iterable[ID]): Fu[List[User]] = $find byIds ids

  def pair(x: Option[ID], y: Option[ID]): Fu[(Option[User], Option[User])] =
    $find byIds List(x, y).flatten map { users =>
      x.??(xx => users.find(_.id == xx)) ->
        y.??(yy => users.find(_.id == yy))
    }

  def byOrderedIds(ids: Iterable[ID]): Fu[List[User]] = $find byOrderedIds ids

  def enabledByIds(ids: Seq[ID]): Fu[List[User]] =
    $find(enabledSelect ++ $select.byIds(ids))

  def named(username: String): Fu[Option[User]] = $find byId normalize(username)

  def nameds(usernames: List[String]): Fu[List[User]] = $find byIds usernames.map(normalize)

  def byIdsSortRating(ids: Iterable[ID], max: Int) = $find($query byIds ids sort sortRatingDesc, max)

  def allSortToints(nb: Int) = $find($query.all sort ($sort desc "toints"), nb)

  def usernameById(id: ID) = $primitive.one($select(id), "username")(_.asOpt[String])

  def rank(user: User) = $count(enabledSelect ++ Json.obj("rating" -> $gt(Glicko.default.rating))) map (1+)

  def orderByGameCount(u1: String, u2: String): Fu[Option[(String, String)]] =
    userTube.coll.find(
      BSONDocument("_id" -> BSONDocument("$in" -> List(u1, u2))),
      BSONDocument("count.game" -> true)
    ).cursor[BSONDocument].collect[List]() map { docs =>
        docs.sortBy {
          _.getAs[BSONDocument]("count") flatMap (_.getAs[Double]("game").map(_.toInt)) getOrElse 0
        }.map(_.getAs[String]("_id")).flatten match {
          case List(u1, u2) => (u1, u2).some
          case _            => none
        }
      }

  def lichess = byId("lichess")

  def setPerfs(user: User, perfs: Perfs, progress: Int) = $update($select(user.id), $setBson(
    "perfs" -> Perfs.tube.handler.write(perfs),
    "rating" -> BSONInteger(user.engine.fold(Glicko.default, perfs.global.glicko).intRating),
    "progress" -> BSONInteger(progress)
  ))

  def setPerf(userId: String, perfName: String, perf: Perf) = $update($select(userId), $setBson(
    s"perfs.$perfName" -> Perf.perfBSONHandler.write(perf)
  ))

  def setProfile(id: ID, profile: Profile): Funit =
    $update($select(id), $setBson("profile" -> Profile.tube.handler.write(profile)))

  def setTitle(id: ID, title: Option[String]): Funit = title match {
    case Some(t) => $update.field(id, "title", t)
    case None    => $update($select(id), $unset("title"))
  }

  val enabledSelect = Json.obj("enabled" -> true)
  def engineSelect(v: Boolean) = Json.obj(User.BSONFields.engine -> v.fold(JsBoolean(true), $ne(true)))
  val stableSelect = Json.obj("perfs.global.gl.d" -> $lt(100))
  val goodLadSelect = enabledSelect ++ engineSelect(false)
  val stableGoodLadSelect = stableSelect ++ goodLadSelect
  def minRatingSelect(rating: Int) = Json.obj(User.BSONFields.rating -> $gt(rating))
  def perfSince(perf: String, since: DateTime) = Json.obj(
    s"perfs.$perf.la" -> Json.obj("$gt" -> $date(since))
  )
  val goodLadQuery = $query(goodLadSelect)

  val sortRatingDesc = $sort desc "rating"
  val sortProgressDesc = $sort desc "progress"
  val sortCreatedAtDesc = $sort desc User.BSONFields.createdAt

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Int) = {
    val incs = List(
      "count.game".some,
      "count.rated".some filter (_ => rated),
      "count.ai".some filter (_ => ai),
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
      }) filterNot (_ => ai)
    ).flatten map (_ -> 1)

    $update($select(id), $incBson(incs: _*))
  }

  def incToints(id: ID)(nb: Int) = $update($select(id), $incBson("toints" -> nb))
  def removeAllToints = $update($select.all, $unset("toints"), multi = true)

  def averageRating: Fu[Float] = $primitive($select.all, "rating")(_.asOpt[Float]) map { ratings =>
    ratings.sum / ratings.size.toFloat
  }

  def authenticate(id: ID, password: String): Fu[Option[User]] =
    checkPassword(id, password) flatMap { _ ?? ($find byId id) }

  private case class AuthData(password: String, salt: String, enabled: Boolean, sha512: Boolean) {
    def compare(p: String) = password == sha512.fold(hash512(p, salt), hash(p, salt))
  }

  private object AuthData {

    import lila.db.JsTube.Helpers._
    import play.api.libs.json._

    private def defaults = Json.obj("sha512" -> false)

    lazy val reader = (__.json update merge(defaults)) andThen Json.reads[AuthData]
  }

  def checkPassword(id: ID, password: String): Fu[Boolean] =
    $projection.one($select(id), Seq("password", "salt", "enabled", "sha512")) { obj =>
      (AuthData.reader reads obj).asOpt
    } map {
      _ ?? (data => data.enabled && data.compare(password))
    }

  def create(username: String, password: String): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        $insert.bson(newUser(username, password)) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = idExists(normalize(username))
  def idExists(id: String): Fu[Boolean] = $count exists id

  def engineIds: Fu[Set[String]] = $primitive(Json.obj("engine" -> true), "_id")(_.asOpt[String]) map (_.toSet)

  def usernamesLike(username: String, max: Int = 10): Fu[List[String]] = {
    import java.util.regex.Matcher.quoteReplacement
    val escaped = """^([\w-]*).*$""".r.replaceAllIn(normalize(username), m => quoteReplacement(m group 1))
    val regex = "^" + escaped + ".*$"
    $primitive(
      $select byId $regex(regex),
      "username",
      _ sort ("_id" -> $sort.desc),
      max.some
    )(_.asOpt[String])
  }

  def toggleEngine(id: ID): Funit = $update.docBson[ID, User](id) { u =>
    $setBson(
      "engine" -> BSONBoolean(!u.engine),
      "rating" -> BSONInteger(u.engine.fold(u.perfs.global.glicko, Glicko.default).intRating)
    )
  }

  def toggleIpBan(id: ID) = $update.doc[ID, User](id) { u => $set("ipBan" -> !u.ipBan) }

  def updateTroll(user: User) = $update.field(user.id, "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = $count.exists($select(id) ++ engineSelect(true))
  def isArtificial(id: ID): Fu[Boolean] = $count.exists($select(id) ++ Json.obj("artificial" -> true))

  def setRoles(id: ID, roles: List[String]) = $update.field(id, "roles", roles)

  def enable(id: ID) = $update.field(id, "enabled", true)

  def disable(id: ID) = $update.field(id, "enabled", false)

  def passwd(id: ID, password: String): Funit =
    $primitive.one($select(id), "salt")(_.asOpt[String]) flatMap { saltOption =>
      saltOption ?? { salt =>
        $update($select(id), $set(Json.obj(
          "password" -> hash(password, salt),
          "sha512" -> false)))
      }
    }

  def setSeenAt(id: ID) {
    $update.fieldUnchecked(id, "seenAt", $date(DateTime.now))
  }

  def setLang(id: ID, lang: String) {
    $update.fieldUnchecked(id, "lang", lang)
  }

  def idsAverageRating(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Match(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))("rating" -> SumField("rating"))
    ))
    userTube.coll.db.command(command) map { stream =>
      stream.toList.headOption flatMap { obj =>
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int "rating" }
    } map (~_ / ids.size)
  }

  def idsSumToints(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Match(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))("toints" -> SumField("toints"))
    ))
    userTube.coll.db.command(command) map { stream =>
      stream.toList.headOption flatMap { obj =>
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int "toints" }
    } map (~_)
  }

  def filterByEngine(userIds: List[String]): Fu[List[String]] =
    $primitive(Json.obj("_id" -> $in(userIds)) ++ engineSelect(true), "username")(_.asOpt[String])

  private def newUser(username: String, password: String) = {

    val salt = ornicar.scalalib.Random nextStringUppercase 32
    val perfs = Perfs.default
    implicit def countHandler = Count.tube.handler
    implicit def perfsHandler = Perfs.tube.handler
    import lila.db.BSON.BSONJodaDateTimeHandler
    import User.BSONFields

    BSONDocument(
      BSONFields.id -> normalize(username),
      BSONFields.username -> username,
      "password" -> hash(password, salt),
      "salt" -> salt,
      BSONFields.perfs -> perfs,
      BSONFields.rating -> perfs.global.glicko.intRating,
      BSONFields.progress -> 0,
      BSONFields.count -> Count.default,
      BSONFields.enabled -> true,
      BSONFields.createdAt -> DateTime.now,
      BSONFields.seenAt -> DateTime.now)
  }

  def artificialSetPassword(id: String, password: String) =
    passwd(id, password) >> $update($select(id), $unset("artificial") ++ $set("enabled" -> true))

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}
