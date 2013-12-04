package lila.user

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.api._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import tube.userTube

object UserRepo {

  import User.ID

  val normalize = User normalize _

  def all: Fu[List[User]] = $find.all

  def topElo(nb: Int): Fu[List[User]] = $find(goodLadQuery sort sortEloDesc, nb)

  def topBullet = topSpeed("bullet") _
  def topBlitz = topSpeed("blitz") _
  def topSlow = topSpeed("slow") _

  def topSpeed(speed: String)(nb: Int): Fu[List[User]] =
    $find(goodLadQuery sort ($sort desc "speedElos." + speed + ".elo"), nb)

  def topNbGame(nb: Int): Fu[List[User]] =
    $find(goodLadQuery sort ($sort desc "count.game"), nb)

  def byId(id: ID): Fu[Option[User]] = $find byId id

  def byIds(ids: Iterable[ID]): Fu[List[User]] = $find byIds ids

  def byOrderedIds(ids: Iterable[ID]): Fu[List[User]] = $find byOrderedIds ids

  def enabledByIds(ids: Seq[ID]): Fu[List[User]] =
    $find(enabledSelect ++ $select.byIds(ids))

  def named(username: String): Fu[Option[User]] = $find byId normalize(username)

  def nameds(usernames: List[String]): Fu[List[User]] = $find byIds usernames.map(normalize)

  def byIdsSortElo(ids: Iterable[ID], max: Int) = $find($query byIds ids sort sortEloDesc, max)

  def allSortToints(nb: Int) = $find($query.all sort ($sort desc "toints"), nb)

  def usernameById(id: ID) = $primitive.one($select(id), "username")(_.asOpt[String])

  def rank(user: User) = $count(enabledSelect ++ Json.obj("elo" -> $gt(user.elo))) map (1+)

  def setElo(id: ID, elo: Int, speedElo: (String, SubElo), variantElo: (String, SubElo)): Funit = (speedElo, variantElo) match {
    case ((speed, sElo), (variant, vElo)) ⇒ $update($select(id), $set(
      "elo" -> elo,
      "speedElos.%s.nb".format(speed) -> sElo.nb,
      "speedElos.%s.elo".format(speed) -> sElo.elo,
      "variantElos.%s.nb".format(variant) -> vElo.nb,
      "variantElos.%s.elo".format(variant) -> vElo.elo
    ))
  }

  def setEloOnly(id: ID, elo: Int): Funit = $update($select(id), $set("elo" -> elo))

  def setProfile(id: ID, profile: Profile): Funit = {
    import tube.profileTube
    $update($select(id), $set("profile" -> profile))
  }

  val enabledSelect = Json.obj("enabled" -> true)
  val noEngineSelect = Json.obj("engine" -> $ne(true))
  val goodLadQuery = $query(enabledSelect ++ noEngineSelect)

  val sortEloDesc = $sort desc "elo"

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Option[Int]) = {
    val incs = List(
      "count.game".some,
      "count.rated".some filter (_ ⇒ rated),
      "count.ai".some filter (_ ⇒ ai),
      (result match {
        case Some(-1) ⇒ "count.loss".some
        case Some(1)  ⇒ "count.win".some
        case Some(0)  ⇒ "count.draw".some
        case _        ⇒ none
      }),
      (result match {
        case Some(-1) ⇒ "count.lossH".some
        case Some(1)  ⇒ "count.winH".some
        case Some(0)  ⇒ "count.drawH".some
        case _        ⇒ none
      }) filterNot (_ ⇒ ai)
    ).flatten map (_ -> 1)

    $update($select(id), $inc(incs: _*))
  }

  def incToints(id: ID)(nb: Int) = $update($select(id), $inc("toints" -> nb))

  def averageElo: Fu[Float] = $primitive($select.all, "elo")(_.asOpt[Float]) map { elos ⇒
    elos.sum / elos.size.toFloat
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
    $projection.one($select(id), Seq("password", "salt", "enabled", "sha512")) { obj ⇒
      (AuthData.reader reads obj).asOpt
    } map {
      _ ?? (data ⇒ data.enabled && data.compare(password))
    }

  def create(username: String, password: String): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        $insert(newUser(username, password)) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = $count exists normalize(username)

  def usernamesLike(username: String, max: Int = 10): Fu[List[String]] = {
    import java.util.regex.Matcher.quoteReplacement
    val escaped = """^([\w-]*).*$""".r.replaceAllIn(normalize(username), m ⇒ quoteReplacement(m group 1))
    val regex = "^" + escaped + ".*$"
    $primitive(
      $select byId $regex(regex),
      "username",
      _ sort ("_id" -> $sort.desc),
      max.some
    )(_.asOpt[String])
  }

  def toggleEngine(id: ID): Funit = $update.doc[ID, User](id) { u ⇒ $set("engine" -> !u.engine) }

  def toggleIpBan(id: ID) = $update.doc[ID, User](id) { u ⇒ $set("ipBan" -> !u.ipBan) }

  def updateTroll(user: User) = $update.field(user.id, "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = $count.exists($select(id) ++ Json.obj("engine" -> true))

  def setRoles(id: ID, roles: List[String]) = $update.field(id, "roles", roles)

  def setSpeedElos(id: ID, ses: SpeedElos) = {
    import tube.speedElosTube
    $update.field(id, "speedElos", ses)
  }

  def setVariantElos(id: ID, ses: VariantElos) = {
    import tube.variantElosTube
    $update.field(id, "variantElos", ses)
  }

  def enable(id: ID) = $update.field(id, "enabled", true)

  def disable(id: ID) = $update.field(id, "enabled", false)

  def passwd(id: ID, password: String): Funit =
    $primitive.one($select(id), "salt")(_.asOpt[String]) flatMap { saltOption ⇒
      saltOption ?? { salt ⇒
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

  def idsAverageElo(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Match(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))("elo" -> SumField("elo"))
    ))
    userTube.coll.db.command(command) map { stream ⇒
      stream.toList.headOption flatMap { obj ⇒
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int "elo" }
    } map (~_ / ids.size)
  }

  def idsSumToints(ids: Iterable[String]): Fu[Int] = ids.isEmpty ? fuccess(0) | {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    val command = Aggregate(userTube.coll.name, Seq(
      Sort(BSONDocument("_id" -> BSONDocument("$in" -> ids))),
      Group(BSONBoolean(true))("toints" -> SumField("toints"))
    ))
    userTube.coll.db.command(command) map { stream ⇒
      stream.toList.headOption flatMap { obj ⇒
        toJSON(obj).asOpt[JsObject]
      } flatMap { _ int "toints" }
    } map (~_)
  }

  private def newUser(username: String, password: String) = {

    val salt = Random nextString 32
    implicit def speedElosTube = SpeedElos.tube
    implicit def countTube = Count.tube

    Json.obj(
      "_id" -> normalize(username),
      "username" -> username,
      "password" -> hash(password, salt),
      "salt" -> salt,
      "elo" -> User.STARTING_ELO,
      "count" -> Count.default,
      "enabled" -> true,
      "createdAt" -> $date(DateTime.now),
      "seenAt" -> $date(DateTime.now))
  }

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}
