package lila.user

import play.api.libs.iteratee._
import reactivemongo.api._
import reactivemongo.play.iteratees.cursorProducer

import Authenticator._
import lila.db.dsl._

// disposable class, one time use.
// uses only one thread.
// if interrupted, it can be started again,
// and will just skip all already migrated users.
private object BcryptMigration {

  def apply(authenticator: Authenticator, coll: Coll): Funit = coll
    .find($doc("bpass" $exists false), authProjection)
    .cursor[AuthData](readPreference = ReadPreference.secondaryPreferred)
    .enumerator() |>>>
    Iteratee.foldM[AuthData, Int](0) {
      case (count, authData) =>
        if (count % 1000 == 0) println(s"Migrated $count passwords")
        authenticator.upgradePassword(authData) inject count + 1
    } void
}
