package lila.system
package model

import com.novus.salat.annotations.Key

case class Message(
    @Key("_id") id: Int,
    username: String,
    message: String) {
}
