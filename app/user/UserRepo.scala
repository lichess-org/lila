package lila
package user

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.{ MongoCollection, WriteConcern }
import com.mongodb.casbah.Imports._
import scalaz.effects._
import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import ornicar.scalalib.OrnicarRandom

class UserRepo(
    collection: MongoCollection) extends SalatDAO[User, String](collection) {

  val enabledQuery = DBObject("enabled" -> true)
  def byIdQuery(id: String): DBObject = DBObject("_id" -> normalize(id))
  def byIdQuery(user: User): DBObject = byIdQuery(user.id)

  def normalize(id: String) = id.toLowerCase

  def byId(id: String): IO[Option[User]] = io {
    findOneByID(normalize(id))
  }

  def byIds(ids: Iterable[String]): IO[List[User]] = io {
    find("_id" $in ids.map(normalize))
      .sort(DBObject("elo" -> -1))
      .toList
  }

  def username(userId: String): IO[Option[String]] = io {
    primitiveProjection[String](byIdQuery(userId), "username")
  }

  def rank(user: User): IO[Int] = io {
    count("elo" $gt user.elo).toInt + 1
  }

  def setElo(id: String, elo: Int): IO[Unit] = io {
    collection.update(byIdQuery(id), $set("elo" -> elo))
  }

  def incNbGames(id: String, rated: Boolean): IO[Unit] = io {
    collection.update(
      byIdQuery(id),
      if (rated) $inc("nbGames" -> 1, "nbRatedGames" -> 1)
      else $inc("nbGames" -> 1))
  }

  val averageElo: IO[Float] = io {
    val elos = find(DBObject()).toList map (_.elo)
    elos.sum / elos.size.toFloat
  }

  def toggleChatBan(user: User): IO[Unit] = io {
    collection.update(byIdQuery(user), $set("isChatBan" -> !user.isChatBan))
  }

  def saveSetting(user: User, key: String, value: String) = io {
    collection.update(byIdQuery(user), $set(("settings." + key) -> value))
  }

  def exists(username: String): IO[Boolean] = io {
    count(byIdQuery(username)) != 0
  }

  def authenticate(username: String, password: String): IO[Option[User]] = for {
    userOption ← byId(username)
    greenLight ← authenticable(username, password)
  } yield userOption filter (_ ⇒ greenLight)

  private def authenticable(username: String, password: String): IO[Boolean] = io {
    for {
      data ← collection.findOne(
        byIdQuery(username),
        DBObject("password" -> true, "salt" -> true, "enabled" -> true, "sha512" -> true)
      )
      hashed ← data.getAs[String]("password")
      salt ← data.getAs[String]("salt")
      enabled ← data.getAs[Boolean]("enabled")
      sha512 = data.getAs[Boolean]("sha512") | false
    } yield enabled && hashed == sha512.fold(
      hash512(password, salt),
      hash(password, salt)
    )
  } map (_ | false)

  def create(username: String, password: String): IO[Option[User]] = for {
    exists ← exists(username)
    userOption ← exists.fold(
      io(none),
      io {
        val salt = OrnicarRandom nextAsciiString 32
        val obj = DBObject(
          "_id" -> normalize(username),
          "username" -> username,
          "password" -> hash(password, salt),
          "salt" -> salt,
          "elo" -> User.STARTING_ELO,
          "nbGames" -> 0,
          "nbRatedGames" -> 0,
          "enabled" -> true,
          "roles" -> Nil,
          "createdAt" -> DateTime.now)
        collection.insert(obj, WriteConcern.Safe)
      } flatMap { _ ⇒ byId(username) }
    )
  } yield userOption

  val countEnabled: IO[Int] = io { count(enabledQuery).toInt }

  def usernamesLike(username: String): IO[List[String]] = io {
    val regex = "^" + normalize(username) + ".*$"
    collection.find(
      DBObject("_id" -> regex.r),
      DBObject("username" -> 1))
      .sort(DBObject("_id" -> 1))
      .limit(10)
      .toList
      .map(_.getAs[String]("username"))
      .flatten
  }

  def toggleMute(username: String): IO[Unit] = updateIO(username) { user ⇒
    $set("isChatBan" -> !user.isChatBan)
  }

  def toggleEngine(username: String): IO[Unit] = updateIO(username) { user ⇒
    $set("engine" -> !user.engine)
  }

  def isEngine(username: String): IO[Boolean] = io {
    for {
      obj ← collection.findOne(byIdQuery(username), DBObject("engine" -> true))
      engine ← obj.getAs[Boolean]("engine")
    } yield engine
  } map (_ | false)

  def setBio(user: User, bio: String) = updateIO(user)($set("bio" -> bio))

  def enable(user: User) = updateIO(user)($set("enabled" -> true))

  def disable(user: User) = updateIO(user)($set("enabled" -> false))

  def passwd(user: User, password: String): IO[Valid[Unit]] = for {
    obj ← io {
      collection.findOne(
        byIdQuery(user), DBObject("salt" -> true)
      ) flatMap (_.getAs[String]("salt"))
    }
    res ← obj.fold(
      salt ⇒ updateIO(user)($set(
        "password" -> hash(password, salt),
        "sha512" -> false
      )) map { _ ⇒ success(Unit): Valid[Unit] },
      io(!!("No salt found"))
    )
  } yield res

  def updateIO(username: String)(op: User ⇒ DBObject): IO[Unit] = for {
    userOption ← byId(username)
    _ ← userOption.fold(user ⇒ updateIO(user)(op(user)), io())
  } yield ()

  def updateIO(user: User)(obj: DBObject): IO[Unit] = io {
    update(byIdQuery(user), obj)
  }

  private def hash(pass: String, salt: String): String =
    "%s{%s}".format(pass, salt).sha1

  private def hash512(pass: String, salt: String): String =
    "%s{%s}".format(pass, salt).sha512
}
