package lila.system
package model

import com.mongodb.casbah.Imports.ObjectId

case class User(
    id: ObjectId,
    username: String,
    isOnline: Boolean) {
}
