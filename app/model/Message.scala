package lila
package model

import com.novus.salat.annotations.Key

case class Message(
    username: String,
    text: String) {

  def render = Map(
    "txt" -> text,
    "u" -> username)
}
