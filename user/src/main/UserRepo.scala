package lila.user

import lila.db.Implicits._
import lila.db.api._

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._

import play.modules.reactivemongo.MongoJSONHelpers.RegEx

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import ornicar.scalalib.Random

object UserRepo {

  implicit def tube = userTube

  type ID = String

  val normalize = Users normalize _

  def named(username: String): Fu[Option[User]] = $find byId normalize(username)

  def byIdsSortElo(ids: Seq[ID], max: Int) = $find($query byIds ids sort sortEloDesc limit max)

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

  val averageElo: Fu[Float] = $primitive($select.all, "elo")(_.asOpt[Float]) map { elos ⇒
    elos.sum / elos.size.toFloat
  }

  def saveSetting(id: ID, key: String, value: String): Funit = 
    $update($select(id), $set(("settings." + key) -> value))

  def authenticate(id: ID, password: String): Fu[Option[User]] = for {
    greenLight ← checkPassword(id, password)
    if greenLight
    userOption ← $find byId id
  } yield userOption

  private case class AuthData(password: String, salt: String, enabled: Boolean, sha512: Boolean) {
    def compare(p: String) = password == sha512.fold(hash512(p, salt), hash(p, salt))
  }

  def checkPassword(id: ID, password: String): Fu[Boolean] = for {
    dataOption ← $projection.one($select(id), Seq("password", "salt", "enabled", "sha512")) { obj ⇒
      (Json.reads[AuthData] reads obj).asOpt
    }
  } yield dataOption zmap (data ⇒ data.enabled && data.compare(password))

  def create(username: String, password: String): Fu[Option[User]] = for {
    existing ← $count exists normalize(username)
    userOption ← existing.fold(
      fuccess(none),
      $insert(newUser(username, password)) >> ($find byId normalize(username)) 
    )
  } yield userOption

  def countEnabled: Fu[Int] = $count(enabledQuery)

  def usernamesLike(username: String, max: Int = 10): Fu[List[String]] = {
    import java.util.regex.Matcher.quoteReplacement
    val escaped = """^([\w-]*).*$""".r.replaceAllIn(normalize(username), m ⇒ quoteReplacement(m group 1))
    val regex = "^" + escaped + ".*$"
    $primitive(
      Json.obj("_id" -> RegEx(regex)),
      "username",
      _ sort ("_id" -> $sort.desc) limit max
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

  private def newUser(username: String, password: String) = (Random nextString 32) |> { salt ⇒
    Json.obj(
      "_id" -> normalize(username),
      "username" -> username,
      "password" -> hash(password, salt),
      "salt" -> salt,
      "elo" -> Users.STARTING_ELO,
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
      "roles" -> Json.arr(),
      "createdAt" -> DateTime.now)
  }

  private def hash(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha1
  private def hash512(pass: String, salt: String): String = "%s{%s}".format(pass, salt).sha512
}

//   def idsAverageElo(ids: Iterable[String]): IO[Int] = io {
//     val result = collection.mapReduce(
//       mapFunction = """function() { emit("e", this.elo); }""",
//       reduceFunction = """function(key, values) {
//   var sum = 0;
//   for(var i in values) { sum += values[i]; }
//   return Math.round(sum / values.length);
// }""",
//       output = MapReduceInlineOutput,
//       query = ("_id" $in ids.map(normalize)).some)
//     (for {
//       row ← result.hasNext option result.next
//       sum ← row.getAs[Double]("value")
//     } yield sum.toInt) | 0
//   }

//   def idsSumToints(ids: Iterable[String]): IO[Int] = io {
//     val result = collection.mapReduce(
//       mapFunction = """function() { emit("e", this.toints); }""",
//       reduceFunction = """function(key, values) {
//   var sum = 0;
//   for(var i in values) { sum += values[i]; }
//   return sum;
// }""",
//       output = MapReduceInlineOutput,
//       query = ("_id" $in ids.map(normalize)).some)
//     (for {
//       row ← result.hasNext option result.next
//       sum ← row.getAs[Double]("value")
//     } yield sum.toInt) | 0
//   }
// }
