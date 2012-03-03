package lila.system
package model

case class DbClock(
    color: String,
    increment: Int,
    limit: Int,
    times: Map[String, Float]) {
}
