package lila.coach

import org.joda.time.DateTime

import lila.user.User

case class CoachReview(
  _id: String, // random
  userId: User.ID, // reviewer
  coachId: Coach.Id,
  rating: Int,
  title: String,
  text: String,
  createdAt: DateTime,
  updatedAt: DateTime)
