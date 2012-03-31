package lila.system
package model

import com.novus.salat.annotations.Key
import com.mongodb.casbah.Imports.ObjectId

case class User(
    @Key("_id") id: ObjectId,
    username: String,
    isOnline: Boolean,
    elo: Int,
    nbGames: Int,
    nbRatedGames: Int) {
}
