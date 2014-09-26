package lila.team

import reactivemongo.bson.{ BSONDocument, BSONString, BSONDateTime }
import acolyte.reactivemongo.{
  AcolyteDSL, 
  CountRequest,
  InClause,
  QueryResponse, 
  Request => Req, 
  SimpleBody,
  ValueDocument
}

object RequestRepoSpec extends org.specs2.mutable.Specification 
    with RequestRepoTest with RequestRepoFixtures {

  "Request repository" title

  "Request count by team" should {
    "work for a single ID using expected query" in {
      withQueryHandler(_ match {
        case CountRequest(_, ("team", BSONString("teamId4")) :: Nil) =>
          /* used query matches expected one */ QueryResponse.count(2)
      })(_ countByTeam ("teamId4")) aka "existence" must beEqualTo(2).await(5)
    }

    "work for ID list using expected query" in {
      withQueryHandler(_ match {
        case CountRequest(_, ("team", InClause(
          BSONString("teamId5") :: BSONString("teamId6") :: Nil)) :: Nil) =>
          /* used query matches expected one */ QueryResponse.count(3)
      })(_ countByTeams (List("teamId5", "teamId6"))).
        aka("existence") must beEqualTo(3).await(5)
    }
  }

  "Request by team" should {
    "be found for a single ID using expected query" in {
      withQueryHandler(_ match {
        case Req(_, SimpleBody(("team", BSONString("anyTeamId6")) :: Nil)) => 
          // used query matches expected one
          QueryResponse(List(requestBson2, requestBson1))
      })(_ findByTeam "anyTeamId6") aka "requests" must beEqualTo(
        List(request2, request1)).await(5)
    }

    "not be found for a single ID" in {
      withQueryResult(QueryResponse.empty)(
        _ findByTeam "anyTeamId6") aka "requests" must beEqualTo(Nil).await(5)
    }

    "be found for list ID using expected query" in {
      withQueryHandler(_ match {
        case Req(_, SimpleBody(("team", ValueDocument(("$in", 
          ValueList(BSONString("anyTeamId7") :: BSONString("anyTeamId8") :: 
            Nil)) :: Nil)) :: Nil)) => // used query matches expected one
          QueryResponse(requestBson2)
      })(_ findByTeams List("anyTeamId7", "anyTeamId8")).
        aka("requests") must beEqualTo(List(request2)).await(5)
    }

    "not be found for list ID" in {
      withQueryResult(QueryResponse.empty)(
        _ findByTeams List("anyTeamId9")).
        aka("requests") must beEqualTo(Nil).await(5)
    }
  }

  "Request" should {
    "exist (according expected query)" in {
      withQueryHandler(_ match {
        case CountRequest(_, ("_id", BSONString("userId3@teamId3")) :: Nil) =>
          /* used query matches expected one */QueryResponse.count(1)
      })(_ exists ("teamId3", "userId3")) aka "existence" must beTrue.await(5)
    }

    "not exist" in {
      withQueryResult(BSONDocument("ok" -> 1, "n" -> 0))(
        _ exists ("teamId4", "userId3")) aka "existence" must beFalse.await(5)
    }

    "be found using expected query" in {
      withQueryHandler(_ match {
        case Req(_, SimpleBody(("_id",BSONString("anyUserId1@anyTeamId1")) :: 
            Nil)) => /* expected used query */QueryResponse(requestBson1)
      })(_ find ("anyTeamId1", "anyUserId1")).
        aka("request") must beSome(request1).await(5)
    }

    "not be found" in {
      withQueryResult(QueryResponse.empty)(
        _ find ("anyTeamId2", "anyUserId2")) aka "request" must beNone.await(5)
    }
  }
}

object ValueList {
  import reactivemongo.bson.{ BSONArray, BSONValue }

  def unapply(arr: BSONArray): Option[List[BSONValue]] = Some(arr.values.toList)
}

sealed trait RequestRepoTest extends RepoTest {
  import lila.db.JsTubeInColl
  import lila.db.Types.Coll

  type Repo = RequestRepo
  val colName = "requests"
  def repo(coll: Coll): Repo = TestRepo(Request.tube inColl coll)

  private case class TestRepo(inColl: JsTubeInColl[Request]) extends RequestRepo
}

private[lila] trait RequestRepoFixtures {
  import org.joda.time.DateTime

  val reqDate1 = DateTime.now

  val requestBson1 = BSONDocument("_id" -> "userId1@teamId1",
    "team" -> "teamId1", "user" -> "userId1", 
    "message" -> "msg1", "date" -> BSONDateTime(reqDate1.getMillis))

  val request1 = 
    Request("userId1@teamId1", "teamId1", "userId1", "msg1", reqDate1)

  val reqDate2 = DateTime.now

  val requestBson2 = BSONDocument("_id" -> "2userId@2teamId",
    "team" -> "2teamId", "user" -> "2userId", 
    "message" -> "2msg", "date" -> BSONDateTime(reqDate2.getMillis))

  val request2 = 
    Request("2userId@2teamId", "2teamId", "2userId", "2msg", reqDate2)
}
