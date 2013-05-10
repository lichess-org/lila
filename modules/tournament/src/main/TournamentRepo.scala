package lila.tournament

import lila.db.api._
import lila.db.Implicits._
import tube.tournamentTube

import play.api.libs.json._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

// object TournamentRepo {

//   def createdById(id: String): Fu[Option[Created]] = byIdAs(id, _.created)

//   def startedById(id: String): Fu[Option[Started]] = byIdAs(id, _.started)

//   def createdByIdAndCreator(id: String, userId: String): Fu[Option[Created]] =
//     createdById(id) map (_ filter (_ isCreator userId))

//   private def byIdAs[A](id: String, as: Tournament ⇒ Option[A]): Fu[Option[A]] = io {
//     $find byId id map (_ flatMap as)
//   }

//   def created: Fu[List[Created]] = 
//     $find($query(
//       Json.obj("status" -> Status.Created.id)) sort $sort.createdDesc
//     )).toList map2 { (tour: Tournament) => tour.created }

//   def started: Fu[List[Started]] = io {
//     find(DBObject("status" -> Status.Started.id))
//       .sort(DBObject("createdAt" -> -1))
//       .toList.map(_.started).flatten
//   }

//   def finished(limit: Int): Fu[List[Finished]] = io {
//     find(DBObject("status" -> Status.Finished.id))
//       .sort(DBObject("createdAt" -> -1))
//       .limit(limit)
//       .toList.map(_.finished).flatten
//   }

//   def setUsers(tourId: String, userIds: List[String]): Fu[Unit] = io {
//     update(idSelector(tourId), $set(Seq("data.users" -> userIds.distinct)))
//   }

//   def userHasRunningTournament(username: String): Fu[Boolean] = io {
//     collection.findOne(
//       ("status" $ne Status.Finished.id) ++ ("data.users" -> username)
//     ).isDefined
//   }

//   val inProgressIds: Fu[List[String]] = io {
//     primitiveProjections(DBObject("status" -> Status.Started.id), "_id")
//   }

//   def saveFu(tournament: Tournament): Fu[Unit] = io {
//     save(tournament.encode)
//   }

//   def removeFu(tournament: Tournament): Fu[Unit] = io {
//     remove(idSelector(tournament))
//   }

//   def withdraw(userId: String): Fu[List[String]] = for {
//     createds ← created
//     createdIds ← (createds map (_ withdraw userId) collect {
//       case Success(tour) ⇒ saveFu(tour) map (_ ⇒ tour.id)
//     }).sequence
//     starteds ← started
//     startedIds ← (starteds map (_ withdraw userId) collect {
//       case Success(tour) ⇒ saveFu(tour) map (_ ⇒ tour.id)
//     }).sequence
//   } yield createdIds ::: startedIds

//   private def idSelector(id: String): DBObject = DBObject("_id" -> id)
//   private def idSelector(tournament: Tournament): DBObject = idSelector(tournament.id)
// }
