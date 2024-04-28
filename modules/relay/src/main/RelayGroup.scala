package lila.relay

import scalalib.ThreadLocalRandom
import reactivemongo.api.bson.Macros.Annotations.Key

case class RelayGroup(@Key("_id") id: RelayGroup.Id, name: RelayGroup.Name, tours: List[RelayTourId])

object RelayGroup:

  opaque type Id = String
  object Id extends OpaqueString[Id]:
    def make = Id(ThreadLocalRandom.nextString(8))

  opaque type Name = String
  object Name extends OpaqueString[Name]

  case class WithTours(group: RelayGroup, tours: List[RelayTour.IdName]):
    def withShorterTourNames = copy(tours = tours.map: tour =>
      if tour.name.value.startsWith(group.name.value)
      then
        val shortName = tour.name.value.drop(group.name.value.size + 1).dropWhile(!_.isLetterOrDigit)
        tour.copy(name = RelayTour.Name(shortName))
      else tour)

  private[relay] object form:
    import play.api.data.*
    import play.api.data.Forms.*
    import play.api.data.format.Formatter
    import lila.common.Form.formatter
    case class Data(name: RelayGroup.Name, tours: List[RelayTour.IdName]):
      override def toString = s"$name\n${tours.map(t => s"${t.id} ${t.name}").mkString("\n")}"
      def tourIds           = tours.map(_.id)
      def update(group: RelayGroup): RelayGroup = group.copy(name = name, tours = tourIds)
      def make: RelayGroup                      = RelayGroup(RelayGroup.Id.make, name, tourIds)
    object Data:
      def apply(group: RelayGroup.WithTours): Data = Data(group.group.name, group.tours)
      def parse(value: String): Option[Data] =
        value.split("\n").toList match
          case Nil => none
          case name :: tourIds =>
            val tours = tourIds
              .take(50)
              .map(_.trim.take(8))
              .map: id =>
                RelayTour.IdName(RelayTourId(id), RelayTour.Name(""))
            Data(RelayGroup.Name(name.linesIterator.next.trim), tours).some

    given Formatter[Data]              = formatter.stringOptionFormatter(_.toString, Data.parse)
    val mapping: Mapping[Option[Data]] = optional(of[Data])

import lila.db.dsl.{ *, given }

final private class RelayGroupRepo(coll: Coll)(using Executor):

  import reactivemongo.api.bson.*
  import BSONHandlers.given

  def byTour(tourId: RelayTourId): Fu[Option[RelayGroup]] =
    coll.find($doc("tours" -> tourId)).one[RelayGroup]

  def update(tourId: RelayTourId, data: RelayGroup.form.Data): Funit =
    for
      prev <- byTour(tourId)
      curId <- prev match
        case Some(prev) if data.tours.isEmpty => coll.delete.one($id(prev.id)).inject(none)
        case Some(prev) => coll.update.one($id(prev.id), data.update(prev)).inject(prev.id.some)
        case None =>
          val newGroup = data.make
          coll.insert.one(newGroup).inject(newGroup.id.some)
      // make sure the tours of this group are not in other groups
      _ <- curId.so: id =>
        data.tours.map(_.id).traverse_ { tourId =>
          coll.update.one($doc("_id".$ne(id), "tours" -> tourId), $pull("tours" -> tourId), multi = true)
        }
    yield ()
