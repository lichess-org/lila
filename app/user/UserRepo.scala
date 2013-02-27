package lila
package user

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.{ MongoCollection, WriteConcern }
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.map_reduce.MapReduceInlineOutput
import scalaz.effects._
import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import ornicar.scalalib.Random

class UserRepo(collection: MongoCollection)
    extends SalatDAO[User, String](collection) {

  val enabledQuery = DBObject("enabled" -> true)
  def byIdQuery(id: String): DBObject = DBObject("_id" -> normalize(id))
  def byIdQuery(user: User): DBObject = byIdQuery(user.id)

  def normalize(id: String) = id.toLowerCase

  def byId(id: String): IO[Option[User]] = io {
    findOneById(normalize(id))
  }

  def byIds(ids: Iterable[String]): IO[List[User]] = io {
    find("_id" $in ids.map(normalize))
      .sort(DBObject("elo" -> -1))
      .toList
  }

  def byOrderedIds(ids: Iterable[String]): IO[List[User]] = io {
    find("_id" $in ids.map(normalize)).toList
  } map { us ⇒
    val usMap = us.map(u ⇒ u.id -> u).toMap
    ids.map(usMap.get).flatten.toList
  }

  def byIdsSortByElo(ids: Iterable[String], nb: Int): IO[List[User]] = io {
    find("_id" $in ids.map(normalize))
      .sort(DBObject("elo" -> -1))
      .limit(nb)
      .toList
  }

  def sortedByToints(nb: Int): IO[List[User]] = io {
    find(DBObject()).sort(DBObject("toints" -> -1)).limit(nb).toList
  }

  def idsAverageElo(ids: Iterable[String]): IO[Int] = io {
    val result = collection.mapReduce(
      mapFunction = """function() { emit("e", this.elo); }""",
      reduceFunction = """function(key, values) {
  var sum = 0;
  for(var i in values) { sum += values[i]; }
  return Math.round(sum / values.length);
}""",
      output = MapReduceInlineOutput,
      query = ("_id" $in ids.map(normalize)).some)
    (for {
      row ← result.hasNext option result.next
      sum ← row.getAs[Double]("value")
    } yield sum.toInt) | 0
  }

  def idsSumToints(ids: Iterable[String]): IO[Int] = io {
    val result = collection.mapReduce(
      mapFunction = """function() { emit("e", this.toints); }""",
      reduceFunction = """function(key, values) {
  var sum = 0;
  for(var i in values) { sum += values[i]; }
  return sum;
}""",
      output = MapReduceInlineOutput,
      query = ("_id" $in ids.map(normalize)).some)
    (for {
      row ← result.hasNext option result.next
      sum ← row.getAs[Double]("value")
    } yield sum.toInt) | 0
  }

  def username(userId: String): IO[Option[String]] = io {
    primitiveProjection[String](byIdQuery(userId), "username")
  }

  def rank(user: User): IO[Int] = io {
    count(DBObject("enabled" -> true) ++ ("elo" $gt user.elo)).toInt + 1
  }

  def setElo(id: String, elo: Int): IO[Unit] = io {
    collection.update(byIdQuery(id), $set(Seq("elo" -> elo)))
  }

  def incNbGames(
    id: String,
    rated: Boolean,
    ai: Boolean,
    result: Option[Int]): IO[Unit] = io {
    val incs = List(
      "nbGames".some,
      "nbRatedGames".some filter (_ ⇒ rated),
      "nbAi".some filter (_ ⇒ ai),
      (result match {
        case Some(-1) ⇒ "nbLosses".some
        case Some(1)  ⇒ "nbWins".some
        case Some(0)  ⇒ "nbDraws".some
        case _        ⇒ none[String]
      }),
      (result match {
        case Some(-1) ⇒ "nbLossesH".some
        case Some(1)  ⇒ "nbWinsH".some
        case Some(0)  ⇒ "nbDrawsH".some
        case _        ⇒ none[String]
      }) filterNot (_ ⇒ ai)
    ).flatten.map(_ -> 1)
    collection.update(byIdQuery(id), $inc(incs: _*))
  }

  def incToints(id: String)(nb: Int): IO[Unit] = io {
    collection.update(byIdQuery(id), $inc("toints" -> nb))
  }

  val averageElo: IO[Float] = io {
    val elos = find(DBObject()).toList map (_.elo)
    elos.sum / elos.size.toFloat
  }

  def toggleChatBan(user: User): IO[Unit] = io {
    collection.update(byIdQuery(user), $set(Seq("isChatBan" -> !user.isChatBan)))
  }

  def saveSetting(user: User, key: String, value: String) = io {
    collection.update(byIdQuery(user), $set(Seq(("settings." + key) -> value)))
  }

  def exists(username: String): IO[Boolean] = io {
    count(byIdQuery(username)) != 0
  }

  def authenticate(username: String, password: String): IO[Option[User]] = for {
    userOption ← byId(username)
    greenLight ← checkPassword(username, password)
  } yield userOption filter (_ ⇒ greenLight)

  def checkPassword(username: String, password: String): IO[Boolean] = io {
    for {
      data ← collection.findOne(
        byIdQuery(username),
        DBObject("password" -> true, "salt" -> true, "enabled" -> true, "sha512" -> true)
      )
      hashed ← data.getAs[String]("password")
      salt ← data.getAs[String]("salt")
      enabled ← data.getAs[Boolean]("enabled")
      sha512 = data.getAs[Boolean]("sha512") | false
    } yield enabled && hashed == sha512.fold(hash512(password, salt), hash(password, salt))
  } map (_ | false)

  def create(username: String, password: String): IO[Option[User]] = for {
    exists ← exists(username)
    userOption ← exists.fold(
      io(none),
      io {
        val salt = Random nextString 32
        val obj = DBObject(
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
          "roles" -> Nil,
          "createdAt" -> DateTime.now)
        collection.insert(obj, WriteConcern.Safe)
      } flatMap { _ ⇒ byId(username) }
    )
  } yield userOption

  val countEnabled: IO[Int] = io { count(enabledQuery).toInt }

  def usernamesLike(username: String, max: Int = 10): IO[List[String]] = io {
    import java.util.regex.Matcher.quoteReplacement
    val escaped = """^([\w-]*).*$""".r.replaceAllIn(normalize(username), m ⇒ quoteReplacement(m group 1))
    val regex = "^" + escaped + ".*$"
    collection.find(
      DBObject("_id" -> regex.r),
      DBObject("username" -> 1))
      .sort(DBObject("_id" -> 1))
      .limit(max)
      .toList
      .map(_.getAs[String]("username"))
      .flatten
  }

  def toggleMute(username: String): IO[Unit] = updateIO(username) { user ⇒
    $set(Seq("isChatBan" -> !user.isChatBan))
  }

  def toggleEngine(username: String): IO[Unit] = updateIO(username) { user ⇒
    $set(Seq("engine" -> !user.engine))
  }

  def isEngine(username: String): IO[Boolean] = io {
    collection.find(byIdQuery(username) ++ DBObject("engine" -> true)).size != 0
  }

  def setRoles(user: User, roles: List[String]) = updateIO(user)($set("roles" -> roles))

  def setBio(user: User, bio: String) = updateIO(user)($set(Seq("bio" -> bio)))

  def enable(user: User) = updateIO(user)($set(Seq("enabled" -> true)))

  def disable(user: User) = updateIO(user)($set(Seq("enabled" -> false)))

  def passwd(user: User, password: String): IO[Valid[Unit]] = for {
    obj ← io {
      collection.findOne(
        byIdQuery(user), DBObject("salt" -> true)
      ) flatMap (_.getAs[String]("salt"))
    }
    res ← obj.fold(io(!!("No salt found"): Valid[Unit])) { salt ⇒
      updateIO(user)($set(Seq(
        "password" -> hash(password, salt),
        "sha512" -> false
      ))) map { _ ⇒ success(Unit): Valid[Unit] }
    }
  } yield res

  def updateIO(username: String)(op: User ⇒ DBObject): IO[Unit] = for {
    userOption ← byId(username)
    _ ← ~userOption.map(user ⇒ updateIO(user)(op(user)))
  } yield ()

  def updateIO(user: User)(obj: DBObject): IO[Unit] = io {
    update(byIdQuery(user), obj)
  }

  private def hash(pass: String, salt: String): String =
    "%s{%s}".format(pass, salt).sha1

  private def hash512(pass: String, salt: String): String =
    "%s{%s}".format(pass, salt).sha512
}
