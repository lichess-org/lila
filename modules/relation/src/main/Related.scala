package lidraughts.relation

case class Related(
    user: lidraughts.user.User,
    nbGames: Option[Int],
    followable: Boolean,
    relation: Option[Relation]
)
