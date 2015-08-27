package lila

package object search extends PackageObject with WithPlay {

  import com.sksamuel.elastic4s.{ IndexType, SearchDefinition, CountDefinition }

  type FreeSearchDefinition = IndexType => SearchDefinition
  type FreeCountDefinition = IndexType => CountDefinition
}
