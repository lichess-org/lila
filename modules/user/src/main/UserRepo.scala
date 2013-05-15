package lila.user

import lila.db.Implicits._
import lila.db.api._
import tube.userTube
import lila.common.PimpedJson._

import play.api.libs.json.Json

import reactivemongo.api._
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import play.modules.reactivemongo.json.BSONFormats.toJSON

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import ornicar.scalalib.Random

object UserRepo {

  type ID = String

  val normalize = User normalize _

  def byId(id: ID): Fu[Option[User]] = $find byId id

  def byIds(id: Seq[ID]): Fu[List[User]] = $find byIds id

  def named(username: String): Fu[Option[User]] = $find byId normalize(username)

  def nameds(usernames: List[String]): Fu[List[User]] = $find byIds usernames.map(normalize)

  def byIdsSortElo(ids: Seq[ID], max: Int) = $find($query byIds ids sort sortEloDesc, max)

  def allSortToints(nb: Int) = $find($query.all sort ("toints" -> $sort.desc), nb)

  def usernameById(id: ID) = $primitive.one($select(id), "username")(_.asOpt[String])

  def rank(user: User) = $count(enabledQuery ++ Json.obj("elo" -> $gt(user.elo))) map (1+)

  def setElo(id: ID, elo: Int): Funit = $update($select(id), $set("elo" -> elo))

  val enabledQuery = Json.obj("enabled" -> true)

  val sortEloDesc = $sort desc "elo"

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Option[Int]) = {
    val incs = List(
      "nbGames".some,
      "nbRatedGames".some filter (_ ⇒ rated),
      "nbAi".some filter (_ ⇒ ai),
      (result match {
        case Some(-1) ⇒ "nbLosses".some
        case Some(1)  ⇒ "nbWins".some
        case Some(0)  ⇒ "nbDraws".some
        case _        ⇒ none
      }),
      (result match {
        case Some(-1) ⇒ "nbLossesH".some
        case Some(1)  ⇒ "nbWinsH".some
        case Some(0)  ⇒ "nbDrawsH".some
        case _        ⇒ none
      }) filterNot (_ ⇒ ai)
    ).flatten map (_ -> 1)

    $update($select(id), $inc(incs: _*))
  }

  def incToints(id: ID)(nb: Int) = $update($select(id), $inc("toints" -> nb))

  def averageElo: Fu[Float] = $primitive($select.all, "elo")(_.asOpt[Float]) map { elos ⇒
    elos.sum / elos.size.toFloat
  }

  def saveSetting(id: ID, key: String, value: String): Funit =
    $update($select(id), $set(("settings." + key) -> value))

  def authenticate(id: ID, password: String): Fu[Option[User]] =
    checkPassword(id, password) flatMap { _ ?? ($find byId id) }

  private case class AuthData(password: String, salt: String, enabled: Boolean, sha512: Boolean) {
    def compare(p: String) = password == sha512.fold(hash512(p, salt), hash(p, salt))
  }

  private object AuthData {

    import lila.db.Tube.Helpers._
    import play.api.libs.json._

    private def defaults = Json.obj("sha512" -> false)

    lazy val reader = (__.json update merge(defaults)) andThen Json.reads[AuthData]
  }

  def checkPassword(id: ID, password: String): Fu[Boolean] =
    $projection.one($select(id), Seq("password", "salt", "enabled", "sha512")) { obj ⇒
      (AuthData.reader reads obj).asOpt
    } map {
      _ zmap (data ⇒ data.enabled && data.compare(password))
    }

  def create(username: String, password: String): Fu[Option[User]] = for {
    exists ← $count exists normalize(username)
    userOption ← !exists ?? {
      $insert(newUser(username, password)) >> named(normalize(username))
    }
  } yield userOption

  def countEnabled: Fu[Int] = $count(enabledQuery)

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

  def toggleMute(id: ID) = $update.doc[ID, User](id) { u ⇒ $set("isChatBan" -> !u.isChatBan) }

  def toggleEngine(id: ID): Funit = $update.doc[ID, User](id) { u ⇒ $set("engine" -> !u.engine) }

  def isEngine(id: ID): Fu[Boolean] = $count.exists($select(id) ++ Json.obj("engine" -> true))

  def setRoles(id: ID, roles: List[String]) = $update.field(id, "roles", roles)

  def setBio(id: ID, bio: String) = $update.field(id, "bio", bio)

  def enable(id: ID) = $update.field(id, "enabled", true)

  def disable(id: ID) = $update.field(id, "enabled", false)

  def passwd(id: ID, password: String): Funit =
    $primitive.one($select(id), "salt")(_.asOpt[String]) flatMap { saltOption ⇒
      saltOption zmap { salt ⇒
        $update($select(id), $set("password" -> hash(password, salt)) ++ $set("sha512" -> false))
      }
    }

  def idsAverageElo(ids: Iterable[String]): Fu[Int] = {
    val command = MapReduce(
      collectionName = userTube.coll.name,
      mapFunction = """function() { emit("e", this.elo); }""",
      reduceFunction = """function(key, values) {
  var sum = 0;
  for(var i in values) { sum += values[i]; }
  return Math.round(sum / values.length);
}""",
      query = Some {
        JsObjectWriter write Json.obj("_id" -> $in(ids map normalize))
      }
    )
    userTube.coll.db.command(command) map { res ⇒
      toJSON(res).arr("results").flatMap(_.apply(0) int "value")
    } map (~_)
  }

  def idsSumToints(ids: Iterable[String]): Fu[Int] = {
    val command = MapReduce(
      collectionName = userTube.coll.name,
      mapFunction = """function() { emit("e", this.toints); }""",
      reduceFunction = """function(key, values) {
    var sum = 0;
    for(var i in values) { sum += values[i]; }
    return sum;
  }""",
      query = Some {
        JsObjectWriter write Json.obj("_id" -> $in(ids map normalize))
      }
    )
    userTube.coll.db.command(command) map { res ⇒
      toJSON(res).arr("results").flatMap(_.apply(0) int "value")
    } map (~_)
  }

  private def newUser(username: String, password: String) = (Random nextString 32) |> { salt ⇒
    Json.obj(
      "_id" -> normalize(username),
      "username" -> username,
      "password" -> hash(password, salt),
      "salt" -> salt,
      "elo" -> User.STARTING_ELO,
      "nbGames" -> 0,
      "nbRatedGames" -> 0,
      "nbAi" -> 0,
      "nbWins" -> 0,
      "nbLosses" -> 0,
      "nbDraws" -> 0,
      "nbWinsH" -> 0,
      "nbLossesH" -> 0,
      "nbDrawsH" -> 0,
      "enabled" -> true,
      "createdAt" -> $date(DateTime.now))
  }

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}
