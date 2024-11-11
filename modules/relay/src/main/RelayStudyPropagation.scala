package lila.relay

import lila.study.{ StudyRepo, Study, StudyApi }

/* Propagates study changes across all studies of a broadcast,
 * or even all studies of all broadcasts of the group */
private final class RelayStudyPropagation(
    roundRepo: RelayRoundRepo,
    groupRepo: RelayGroupRepo,
    studyRepo: StudyRepo,
    studyApi: StudyApi
)(using Executor):

  // force studies visibility based on broadcast tier
  def onTierChange(tour: RelayTour) =
    roundRepo
      .studyIdsOf(tour.id)
      .flatMap:
        _.sequentiallyVoid: id =>
          studyApi.setVisibility(id, tour.studyVisibility)

  // if the study is a round, propagate members to all round studies of the tournament group
  def onStudyMembersChange(study: Study) =
    study.isRelay.so:
      roundRepo
        .tourIdByStudyId(study.id)
        .flatMapz: tourId =>
          studyRepo
            .membersDoc(study.id)
            .flatMapz: members =>
              groupRepo
                .allTourIdsOfGroup(tourId)
                .flatMap:
                  _.sequentiallyVoid: tourId =>
                    roundRepo
                      .studyIdsOf(tourId)
                      .flatMap:
                        studyRepo.setMembersDoc(_, members)
