package lila
package game

import importer.PgnImport

case class Metadata(
    source: Source,
    pgnImport: Option[PgnImport] = None) {

  def encode = RawMetadata(so = source.id, pgni = pgnImport)
}

case class RawMetadata(
    so: Int,
    pgni: Option[PgnImport]) {

  def decode = Source(so) map { source ⇒
    Metadata(source = source, pgnImport = pgni)
  }
}
