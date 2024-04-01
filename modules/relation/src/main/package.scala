package lila.relation

export lila.Core.{ *, given }

export lila.core.relation.{ Relation, Block, Follow }

private type OnlineStudyingCache = com.github.blemale.scaffeine.Cache[UserId, String]
