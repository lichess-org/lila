package lila.relation

// case class Status(
//     user1: String,
//     user2: String,
//     relation: Option[Relation],
//     request: Option[Request]) {

//   def areRelations = relation.isDefined

//   def requestBy(userId: String) = request ?? (_.user == userId)

//   def not(userId: String) = (user1 == userId) ? user2 | user1
// }

// object Status {

//   def fromDb(u1: String, u2: String): Fu[Status] =
//     RelationRepo.byUsers(u1, u2) flatMap {
//       _.fold(RequestRepo.byUsers(u1, u2) map {
//         _.fold(Status(u1, u2))(apply)
//       })(relation ⇒ fuccess(apply(relation)))
//     }

//   def apply(relation: Relation): Status = Status(relation.user1, relation.user2, relation.some, none)

//   def apply(request: Request): Status = Status(request.user, request.relation, none, request.some)

//   def apply(u1: String, u2: String): Status = Status(u1, u2, none, none)
// }
