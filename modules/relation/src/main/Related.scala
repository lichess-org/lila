package lila.relation

case class Related(
    user: lila.user.User,
    nbGames: Option[Int],
    followable: Boolean,
    relation: Option[Relation]
)
