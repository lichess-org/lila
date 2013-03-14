package lila.app
package importer

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String) 
