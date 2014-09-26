package lila.team

import org.joda.time.{ DateTime, Period }

import reactivemongo.bson.{ 
  BSONBoolean, BSONDocument, BSONDouble, BSONString, BSONDateTime 
}
import acolyte.reactivemongo.{
  AcolyteDSL, 
  CountRequest,
  QueryResponse, 
  Request => Req, 
  RequestBody,
  SimpleBody,
  UpdateOp,
  ValueDocument,
  WriteResponse
}

object TeamRepoSpec extends org.specs2.mutable.Specification 
    with TeamRepoTest with TeamRepoFixtures with RequestRepoFixtures {

  "Team repository" title

  "Team" should {
    "be found if owned using expected query" in {
      withQueryHandler(_ match {
        case Req(_, SimpleBody(("_id", BSONString("teamId1")) :: 
            ("createdBy", BSONString("creator")) :: Nil)) =>
          /* used query matches expected one */ QueryResponse(teamBson1)
      })(_ owned ("teamId1", "creator")).
        aka("team") must beSome[Team].which(_ must_== team1).await(5)
    }

    "not be found if not owned" in {
      withQueryResult(QueryResponse.empty)(_ owned ("team1", "torcre")).
        aka("team") must beNone.await(5)
    }

    "be enabled using expected command" in {
      withWriteHandler({
        case (UpdateOp, Req(_, RequestBody(
          List(("_id",BSONString("teamId1"))) :: 
            List(("$set",ValueDocument(("enabled",BSONBoolean(true)) :: 
              Nil))) :: Nil))) =>
          /* used command is expected one */ WriteResponse(1)
      })(_ enable team1) aka "activation" must beEqualTo(()).await(5)
    }

    "be disabled using expected command" in {
      withWriteHandler({
        case (UpdateOp, Req(_, RequestBody(
          List(("_id",BSONString("teamId1"))) :: 
            List(("$set",ValueDocument(("enabled",BSONBoolean(false)) :: 
              Nil))) :: Nil))) =>
          /* used command is expected one */ WriteResponse(1)
      })(_ disable team1) aka "activation" must beEqualTo(()).await(5)
    }

    "be found by creator using expected query" in {
      withQueryHandler(_ match {
        case Req(_, RequestBody(List(("createdBy", BSONString("creator1"))) ::
            List(("_id", BSONBoolean(true))) :: Nil)) =>
          /* used query is expected one */ 
          QueryResponse(List(teamBson1, teamBson2))
      })(_ teamIdsByCreator "creator1") aka "team IDs" must beEqualTo(
        List("teamId1", "2teamId")).await(5)
    }

    "not be found by creator" in {
      withQueryResult(QueryResponse.empty)(_ teamIdsByCreator "creator2").
        aka("team IDs") must beEqualTo(Nil).await(5)

    }
  }

  "Team name" should {
    "be found by ID using expected query" in {
      withQueryHandler(_ match {
        case Req(_, RequestBody(List(("_id", BSONString("teamId6"))) :: 
            List(("name", BSONBoolean(true))) :: Nil)) =>
          /* user query is expected one */ QueryResponse(teamBson1)
      })(_ name "teamId6") aka "team name" must beSome[String].which(
        _ must_== "Team #1").await(5)
    }

    "not be found by ID" in {
      withQueryResult(QueryResponse.empty)(_ name "teamId7").
        aka("team name") must beNone.await(5)
    }
  }

  "Team owner" should {
    "be found by ID using expected query" in {
      withQueryHandler(_ match {
        case Req(_, RequestBody(List(("_id", BSONString("teamId8"))) :: 
            List(("createdBy", BSONBoolean(true))) :: Nil)) =>
          /* user query is expected one */ QueryResponse(teamBson2)
      })(_ ownerOf "teamId8") aka "team owner" must beSome[String].which(
        _ must_== "creator2").await(5)
    }

    "not be found by ID" in {
      withQueryResult(QueryResponse.empty)(_ ownerOf "teamId9").
        aka("team owner") must beNone.await(5)
    }
  }

  "Team member count" should {
    "be incremented using expected command" in {
      withWriteHandler({
        case (UpdateOp, Req(_, RequestBody(List(
          ("_id", BSONString("teamId9"))) :: 
            List(("$inc", ValueDocument(
              ("nbMembers", BSONDouble(2.0)) :: Nil))) :: Nil))) =>
          /* command is expected one */ WriteResponse(1)
        case cmd => WriteResponse.failed(s"Unexpected commmand: $cmd")
      })(_ incMembers ("teamId9", 2)).
        aka("increment") must beEqualTo(()).await(5)
    }
  }

  "User creation" should {
    "be successfully checked using expected query" in {
      val period = new Period()
      val millis = ((DateTime.now.getMillis - period.getMillis) / 10000).toInt

      withQueryHandler(_ match {
        case CountRequest(_, ("createdAt", ValueDocument(
          ("$gt", BSONDateTime(ts)) :: Nil)) ::
            ("createdBy", BSONString("userId2")) :: Nil) if (
          (ts / 10000).toInt == millis) =>
          /* user query is expected one */ QueryResponse.count(3)
      })(_ userHasCreatedSince ("userId2", period)).
        aka("created since") must beTrue.await(5)
    }
  }

  "Request" should {
    "be added for team using expected command" in {
      withWriteHandler({ 
        case (UpdateOp, Req(_, SimpleBody(("_id", BSONString("teamId5")) :: 
            ("requests.user",ValueDocument(("$ne", BSONString("userId1")) :: 
              Nil)) :: Nil))) =>
          /* used command is expected one */ WriteResponse(1)
      })(_ addRequest ("teamId5", request1)) must beEqualTo(()).await(5)
    }
  }
}

sealed trait TeamRepoTest extends RepoTest {
  import lila.db.JsTubeInColl
  import lila.db.Types.Coll

  type Repo = TeamRepo
  val colName = "teams"
  def repo(coll: Coll): Repo = TestRepo(Team.tube inColl coll)

  private case class TestRepo(inColl: JsTubeInColl[Team]) extends TeamRepo
}

sealed trait TeamRepoFixtures {
  import org.joda.time.DateTime

  val date1 = DateTime.now

  val teamBson1 = BSONDocument("_id" -> "teamId1",
    "name" -> "Team #1", "location" -> "loca", 
    "description" -> "descr", "nbMembers" -> 2, "enabled" -> true,
    "open" -> true, "irc" -> false, 
    "createdAt" -> BSONDateTime(date1.getMillis),
    "createdBy" -> "creator")

  val team1 = Team("teamId1", "Team #1", Some("loca"), 
    "descr", 2, true, true, false, date1, "creator")

  val date2 = DateTime.now

  val teamBson2 = BSONDocument("_id" -> "2teamId",
    "name" -> "2.Team", "location" -> "loca2", 
    "description" -> "descr2", "nbMembers" -> 7, "enabled" -> true,
    "open" -> false, "irc" -> false, 
    "createdAt" -> BSONDateTime(date2.getMillis),
    "createdBy" -> "creator2")

}
