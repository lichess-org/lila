// Generated
interface I18nString {
  (...args: (string | number)[]): string; // singular & formatted
  asArray: <T>(...args: T[]) => (T | string)[]; // vdom
}
interface I18nPlural {
  (quantity: number, ...args: (string | number)[]): string; // pluralSame
  asArray: <T>(quantity: number, ...args: T[]) => (T | string)[]; // vdomPlural / plural
}
interface I18n {
  /** Global noarg key lookup (only if absolutely necessary). */
  (key: string): string;
  quantity: (count: number) => 'zero' | 'one' | 'two' | 'few' | 'many' | 'other';

  activity: {
    /** Activity */
    activity: I18nString;
    /** Competed in %s Swiss tournaments */
    competedInNbSwissTournaments: I18nPlural;
    /** Competed in %s Arena tournaments */
    competedInNbTournaments: I18nPlural;
    /** Completed %s correspondence games */
    completedNbGames: I18nPlural;
    /** Completed %1$s %2$s correspondence games */
    completedNbVariantGames: I18nPlural;
    /** Created %s new studies */
    createdNbStudies: I18nPlural;
    /** Started following %s players */
    followedNbPlayers: I18nPlural;
    /** Gained %s new followers */
    gainedNbFollowers: I18nPlural;
    /** Hosted a live stream */
    hostedALiveStream: I18nString;
    /** Hosted %s simultaneous exhibitions */
    hostedNbSimuls: I18nPlural;
    /** in %1$s correspondence games */
    inNbCorrespondenceGames: I18nPlural;
    /** Participated in %s simultaneous exhibitions */
    joinedNbSimuls: I18nPlural;
    /** Joined %s teams */
    joinedNbTeams: I18nPlural;
    /** Played %1$s %2$s games */
    playedNbGames: I18nPlural;
    /** Played %1$s moves */
    playedNbMoves: I18nPlural;
    /** Posted %1$s messages in %2$s */
    postedNbMessages: I18nPlural;
    /** Practised %1$s positions on %2$s */
    practicedNbPositions: I18nPlural;
    /** Ranked #%1$s in %2$s */
    rankedInSwissTournament: I18nString;
    /** Ranked #%1$s (top %2$s%%) with %3$s games in %4$s */
    rankedInTournament: I18nPlural;
    /** Signed up to lichess.org */
    signedUp: I18nString;
    /** Solved %s training puzzles */
    solvedNbPuzzles: I18nPlural;
    /** Supported lichess.org for %1$s months as a %2$s */
    supportedNbMonths: I18nPlural;
  };
  appeal: {
    /** Your account is muted. */
    accountMuted: I18nString;
    /** Read our %s. Failure to follow the communication guidelines can result in accounts being muted. */
    accountMutedInfo: I18nString;
    /** Your account is banned from joining arenas. */
    arenaBanned: I18nString;
    /** blog rules */
    blogRules: I18nString;
    /** Your account is marked for rating manipulation. */
    boosterMarked: I18nString;
    /** We define this as deliberately manipulating rating by losing games on purpose or by playing against another account that is deliberately losing games. */
    boosterMarkedInfo: I18nString;
    /** Your account is not marked or restricted. You're all good! */
    cleanAllGood: I18nString;
    /** Your account was closed by moderators. */
    closedByModerators: I18nString;
    /** communication guidelines */
    communicationGuidelines: I18nString;
    /** Your account is marked for external assistance in games. */
    engineMarked: I18nString;
    /** We define this as using any external help to reinforce your knowledge and/or calculation skills in order to gain an unfair advantage over your opponent. See the %s page for more details. */
    engineMarkedInfo: I18nString;
    /** Your account has been excluded from leaderboards. */
    excludedFromLeaderboards: I18nString;
    /** We define this as using any unfair way to get on the leaderboard. */
    excludedFromLeaderboardsInfo: I18nString;
    /** Fair Play */
    fairPlay: I18nString;
    /** Your blogs have been hidden by moderators. */
    hiddenBlog: I18nString;
    /** Make sure to read again our %s. */
    hiddenBlogInfo: I18nString;
    /** You have a play timeout. */
    playTimeout: I18nString;
    /** Your account is banned from tournaments with real prizes. */
    prizeBanned: I18nString;
  };
  arena: {
    /** All averages on this page are %s. */
    allAveragesAreX: I18nString;
    /** Allow Berserk */
    allowBerserk: I18nString;
    /** Let players halve their clock time to gain an extra point */
    allowBerserkHelp: I18nString;
    /** Let players discuss in a chat room */
    allowChatHelp: I18nString;
    /** Arena */
    arena: I18nString;
    /** Arena streaks */
    arenaStreaks: I18nString;
    /** After 2 wins, consecutive wins grant 4 points instead of 2. */
    arenaStreaksHelp: I18nString;
    /** Arena tournaments */
    arenaTournaments: I18nString;
    /** Average performance */
    averagePerformance: I18nString;
    /** Average score */
    averageScore: I18nString;
    /** Arena Berserk */
    berserk: I18nString;
    /** When a player clicks the Berserk button at the beginning of the game, they lose half of their clock time, but the win is worth one extra tournament point. */
    berserkAnswer: I18nString;
    /** Best results */
    bestResults: I18nString;
    /** Created */
    created: I18nString;
    /** Custom start date */
    customStartDate: I18nString;
    /** In your own local timezone. This overrides the "Time before tournament starts" setting */
    customStartDateHelp: I18nString;
    /** Defender */
    defender: I18nString;
    /** Drawing the game within the first %s moves will earn neither player any points. */
    drawingWithinNbMoves: I18nPlural;
    /** Draw streaks: When a player has consecutive draws in an arena, only the first draw will result in a point or draws lasting more than %s moves in standard games. The draw streak can only be broken by a win, not a loss or a draw. */
    drawStreakStandard: I18nString;
    /** The minimum game length for drawn games to award points differs by variant. The table below lists the threshold for each variant. */
    drawStreakVariants: I18nString;
    /** Edit team battle */
    editTeamBattle: I18nString;
    /** Edit tournament */
    editTournament: I18nString;
    /** Arena History */
    history: I18nString;
    /** How are scores calculated? */
    howAreScoresCalculated: I18nString;
    /** A win has a base score of 2 points, a draw 1 point, and a loss is worth no points. */
    howAreScoresCalculatedAnswer: I18nString;
    /** How does it end? */
    howDoesItEnd: I18nString;
    /** The tournament has a countdown clock. When it reaches zero, the tournament rankings are frozen, and the winner is announced. Games in progress must be finished, however, they don't count for the tournament. */
    howDoesItEndAnswer: I18nString;
    /** How does the pairing work? */
    howDoesPairingWork: I18nString;
    /** At the beginning of the tournament, players are paired based on their rating. */
    howDoesPairingWorkAnswer: I18nString;
    /** How is the winner decided? */
    howIsTheWinnerDecided: I18nString;
    /** The player(s) with the most points after the tournament's set time limit will be announced the winner(s). */
    howIsTheWinnerDecidedAnswer: I18nString;
    /** Is it rated? */
    isItRated: I18nString;
    /** This tournament is *not* rated and will *not* affect your rating. */
    isNotRated: I18nString;
    /** This tournament is rated and will affect your rating. */
    isRated: I18nString;
    /** medians */
    medians: I18nString;
    /** Minimum game length */
    minimumGameLength: I18nString;
    /** My tournaments */
    myTournaments: I18nString;
    /** New Team Battle */
    newTeamBattle: I18nString;
    /** No Arena streaks */
    noArenaStreaks: I18nString;
    /** No Berserk allowed */
    noBerserkAllowed: I18nString;
    /** Only titled players */
    onlyTitled: I18nString;
    /** Require an official title to join the tournament */
    onlyTitledHelp: I18nString;
    /** Other important rules */
    otherRules: I18nString;
    /** Pick your team */
    pickYourTeam: I18nString;
    /** Points average */
    pointsAvg: I18nString;
    /** Points sum */
    pointsSum: I18nString;
    /** Rank average */
    rankAvg: I18nString;
    /** The rank average is a percentage of your ranking. Lower is better. */
    rankAvgHelp: I18nString;
    /** Recently played */
    recentlyPlayed: I18nString;
    /** Share this URL to let people join: %s */
    shareUrl: I18nString;
    /** Some tournaments are rated and will affect your rating. */
    someRated: I18nString;
    /** There is a countdown for your first move. Failing to make a move within this time will forfeit the game to your opponent. */
    thereIsACountdown: I18nString;
    /** This is a private tournament */
    thisIsPrivate: I18nString;
    /** Total */
    total: I18nString;
    /** Tournament shields */
    tournamentShields: I18nString;
    /** Tournament stats */
    tournamentStats: I18nString;
    /** Tournament winners */
    tournamentWinners: I18nString;
    /** Variant */
    variant: I18nString;
    /** View all %s teams */
    viewAllXTeams: I18nPlural;
    /** Which team will you represent in this battle? */
    whichTeamWillYouRepresentInThisBattle: I18nString;
    /** You will be notified when the tournament starts, so it is safe to play in another tab while waiting. */
    willBeNotified: I18nString;
    /** You must join one of these teams to participate! */
    youMustJoinOneOfTheseTeamsToParticipate: I18nString;
  };
  broadcast: {
    /** About broadcasts */
    aboutBroadcasts: I18nString;
    /** Add a round */
    addRound: I18nString;
    /** Age this year */
    ageThisYear: I18nString;
    /** View all broadcasts by month */
    allBroadcastsByMonth: I18nString;
    /** All teams */
    allTeams: I18nString;
    /** Boards */
    boards: I18nString;
    /** Boards can be loaded with a source or via the %s */
    boardsCanBeLoaded: I18nString;
    /** Broadcast calendar */
    broadcastCalendar: I18nString;
    /** Broadcasts */
    broadcasts: I18nString;
    /** Completed */
    completed: I18nString;
    /** Lichess detects round completion, but can get it wrong. Use this to set it manually. */
    completedHelp: I18nString;
    /** Current game URL */
    currentGameUrl: I18nString;
    /** Definitively delete the round and all its games. */
    definitivelyDeleteRound: I18nString;
    /** Definitively delete the entire tournament, all its rounds and all its games. */
    definitivelyDeleteTournament: I18nString;
    /** Delete all games of this round. The source will need to be active in order to re-create them. */
    deleteAllGamesOfThisRound: I18nString;
    /** Delete this round */
    deleteRound: I18nString;
    /** Delete this tournament */
    deleteTournament: I18nString;
    /** Download all rounds */
    downloadAllRounds: I18nString;
    /** Edit round study */
    editRoundStudy: I18nString;
    /** Embed this broadcast in your website */
    embedThisBroadcast: I18nString;
    /** Embed %s in your website */
    embedThisRound: I18nString;
    /** Federation */
    federation: I18nString;
    /** FIDE federations */
    fideFederations: I18nString;
    /** FIDE player not found */
    fidePlayerNotFound: I18nString;
    /** FIDE players */
    fidePlayers: I18nString;
    /** FIDE profile */
    fideProfile: I18nString;
    /** FIDE rating category */
    fideRatingCategory: I18nString;
    /** Full tournament description */
    fullDescription: I18nString;
    /** Optional long description of the tournament. %1$s is available. Length must be less than %2$s characters. */
    fullDescriptionHelp: I18nString;
    /** Games in this tournament */
    gamesThisTournament: I18nString;
    /** How to use Lichess Broadcasts. */
    howToUseLichessBroadcasts: I18nString;
    /** More options on the %s */
    iframeHelp: I18nString;
    /** Live tournament broadcasts */
    liveBroadcasts: I18nString;
    /** My broadcasts */
    myBroadcasts: I18nString;
    /** %s broadcasts */
    nbBroadcasts: I18nPlural;
    /** New live broadcast */
    newBroadcast: I18nString;
    /** No boards yet. These will appear once games are uploaded. */
    noBoardsYet: I18nString;
    /** The broadcast has not yet started. */
    notYetStarted: I18nString;
    /** Official Standings */
    officialStandings: I18nString;
    /** Official website */
    officialWebsite: I18nString;
    /** Ongoing */
    ongoing: I18nString;
    /** Open in Lichess */
    openLichess: I18nString;
    /** Optional details */
    optionalDetails: I18nString;
    /** Overview */
    overview: I18nString;
    /** Past broadcasts */
    pastBroadcasts: I18nString;
    /** A public, real-time PGN source for this round. We also offer a %s for faster and more efficient synchronisation. */
    pgnSourceHelp: I18nString;
    /** Rating diff */
    ratingDiff: I18nString;
    /** Recent tournaments */
    recentTournaments: I18nString;
    /** Optional: replace player names, ratings and titles */
    replacePlayerTags: I18nString;
    /** Reset this round */
    resetRound: I18nString;
    /** Round name */
    roundName: I18nString;
    /** Round number */
    roundNumber: I18nString;
    /** Score */
    score: I18nString;
    /** Show players scores based on game results */
    showScores: I18nString;
    /** Up to 64 Lichess game IDs, separated by spaces. */
    sourceGameIds: I18nString;
    /** PGN Source URL */
    sourceSingleUrl: I18nString;
    /** URL that Lichess will check to get PGN updates. It must be publicly accessible from the Internet. */
    sourceUrlHelp: I18nString;
    /** Standings */
    standings: I18nString;
    /** Optional, if you know when the event starts */
    startDateHelp: I18nString;
    /** Start date in the tournament local timezone: %s */
    startDateTimeZone: I18nString;
    /** Starts after %s */
    startsAfter: I18nString;
    /** The broadcast will start very soon. */
    startVerySoon: I18nString;
    /** Subscribed broadcasts */
    subscribedBroadcasts: I18nString;
    /** Subscribe to be notified when each round starts. You can toggle bell or push notifications for broadcasts in your account preferences. */
    subscribeTitle: I18nString;
    /** Teams */
    teams: I18nString;
    /** The new round will have the same members and contributors as the previous one. */
    theNewRoundHelp: I18nString;
    /** Time zone */
    timezone: I18nString;
    /** Top 10 rating */
    top10Rating: I18nString;
    /** Top players */
    topPlayers: I18nString;
    /** Short tournament description */
    tournamentDescription: I18nString;
    /** Tournament format */
    tournamentFormat: I18nString;
    /** Tournament Location */
    tournamentLocation: I18nString;
    /** Tournament name */
    tournamentName: I18nString;
    /** Unrated */
    unrated: I18nString;
    /** Upcoming */
    upcoming: I18nString;
    /** Upcoming broadcasts */
    upcomingBroadcasts: I18nString;
    /** Upload tournament image */
    uploadImage: I18nString;
    /** webmasters page */
    webmastersPage: I18nString;
  };
  challenge: {
    /** Cannot challenge due to provisional %s rating. */
    cannotChallengeDueToProvisionalXRating: I18nString;
    /** Challenge accepted! */
    challengeAccepted: I18nString;
    /** Challenge cancelled. */
    challengeCanceled: I18nString;
    /** Challenge declined. */
    challengeDeclined: I18nString;
    /** Challenges: %1$s */
    challengesX: I18nString;
    /** Challenge to a game */
    challengeToPlay: I18nString;
    /** Please send me a casual challenge instead. */
    declineCasual: I18nString;
    /** I'm not accepting challenges at the moment. */
    declineGeneric: I18nString;
    /** This is not the right time for me, please ask again later. */
    declineLater: I18nString;
    /** I'm not accepting challenges from bots. */
    declineNoBot: I18nString;
    /** I'm only accepting challenges from bots. */
    declineOnlyBot: I18nString;
    /** Please send me a rated challenge instead. */
    declineRated: I18nString;
    /** I'm not accepting variant challenges right now. */
    declineStandard: I18nString;
    /** I'm not accepting challenges with this time control. */
    declineTimeControl: I18nString;
    /** This time control is too fast for me, please challenge again with a slower game. */
    declineTooFast: I18nString;
    /** This time control is too slow for me, please challenge again with a faster game. */
    declineTooSlow: I18nString;
    /** I'm not willing to play this variant right now. */
    declineVariant: I18nString;
    /** Or invite a Lichess user: */
    inviteLichessUser: I18nString;
    /** Please register to send challenges to this user. */
    registerToSendChallenges: I18nString;
    /** %s does not accept challenges. */
    xDoesNotAcceptChallenges: I18nString;
    /** %s only accepts challenges from friends. */
    xOnlyAcceptsChallengesFromFriends: I18nString;
    /** You cannot challenge %s. */
    youCannotChallengeX: I18nString;
    /** Your %1$s rating is too far from %2$s. */
    yourXRatingIsTooFarFromY: I18nString;
  };
  class: {
    /** Add Lichess usernames to invite them as teachers. One per line. */
    addLichessUsernames: I18nString;
    /** Add student */
    addStudent: I18nString;
    /** A link to the class will be automatically added at the end of the message, so you don't need to include it yourself. */
    aLinkToTheClassWillBeAdded: I18nString;
    /** An invitation has been sent to %s */
    anInvitationHasBeenSentToX: I18nString;
    /** Apply to be a Lichess Teacher */
    applyToBeLichessTeacher: I18nString;
    /** Class description */
    classDescription: I18nString;
    /** Class name */
    className: I18nString;
    /** Class news */
    classNews: I18nString;
    /** Click the link to view the invitation: */
    clickToViewInvitation: I18nString;
    /** Close class */
    closeClass: I18nString;
    /** The student will never be able to use this account again. Closing is final. Make sure the student understands and agrees. */
    closeDesc1: I18nString;
    /** You may want to give the student control over the account instead so that they can continue using it. */
    closeDesc2: I18nString;
    /** Close account */
    closeStudent: I18nString;
    /** Close the student account permanently. */
    closeTheAccount: I18nString;
    /** Create a new Lichess account */
    createANewLichessAccount: I18nString;
    /** If the student doesn't have a Lichess account yet, you can create one for them here. */
    createDesc1: I18nString;
    /** No email address is required. A password will be generated, and you will have to transmit it to the student so that they can log in. */
    createDesc2: I18nString;
    /** Important: a student must not have multiple accounts. */
    createDesc3: I18nString;
    /** If they already have one, use the invite form instead. */
    createDesc4: I18nString;
    /** create more classes */
    createMoreClasses: I18nString;
    /** Create multiple Lichess accounts at once */
    createMultipleAccounts: I18nString;
    /** Only create accounts for real students. Do not use this to make multiple accounts for yourself. You would get banned. */
    createStudentWarning: I18nString;
    /** Edit news */
    editNews: I18nString;
    /** Features */
    features: I18nString;
    /** 100% free for all, forever, with no ads or trackers */
    freeForAllForever: I18nString;
    /** Generate a new password for the student */
    generateANewPassword: I18nString;
    /** Generate a new username */
    generateANewUsername: I18nString;
    /** You are invited to join the class "%s" as a student. */
    invitationToClass: I18nString;
    /** Invite */
    invite: I18nString;
    /** Invite a Lichess account */
    inviteALichessAccount: I18nString;
    /** If the student already has a Lichess account, you can invite them to the class. */
    inviteDesc1: I18nString;
    /** They will receive a message on Lichess with a link to join the class. */
    inviteDesc2: I18nString;
    /** Important: only invite students you know, and who actively want to join the class. */
    inviteDesc3: I18nString;
    /** Never send unsolicited invites to arbitrary players. */
    inviteDesc4: I18nString;
    /** Invited to %1$s by %2$s */
    invitedToXByY: I18nString;
    /** Invite the student back */
    inviteTheStudentBack: I18nString;
    /** Active */
    lastActiveDate: I18nString;
    /** Classes */
    lichessClasses: I18nString;
    /** Lichess profile %1$s created for %2$s. */
    lichessProfileXCreatedForY: I18nString;
    /** Lichess username */
    lichessUsername: I18nString;
    /** Make sure to copy or write down the password now. You won’t ever be able to see it again! */
    makeSureToCopy: I18nString;
    /** Managed */
    managed: I18nString;
    /** Note that a class can have up to %1$s students. To manage more students, %2$s. */
    maxStudentsNote: I18nString;
    /** Message all students about new class material */
    messageAllStudents: I18nString;
    /** You can also %s to create multiple Lichess accounts from a list of student names. */
    multipleAccsFormDescription: I18nString;
    /** N/A */
    na: I18nString;
    /** %s pending invitations */
    nbPendingInvitations: I18nPlural;
    /** %s students */
    nbStudents: I18nPlural;
    /** %s teachers */
    nbTeachers: I18nPlural;
    /** New class */
    newClass: I18nString;
    /** News */
    news: I18nString;
    /** All class news in a single field. */
    newsEdit1: I18nString;
    /** Add the recent news at the top. Don't delete previous news. */
    newsEdit2: I18nString;
    /** Separate news with --- */
    newsEdit3: I18nString;
    /** No classes yet. */
    noClassesYet: I18nString;
    /** No removed students. */
    noRemovedStudents: I18nString;
    /** No students in the class, yet. */
    noStudents: I18nString;
    /** Nothing here, yet. */
    nothingHere: I18nString;
    /** Notify all students */
    notifyAllStudents: I18nString;
    /** Only visible to the class teachers */
    onlyVisibleToTeachers: I18nString;
    /** or */
    orSeparator: I18nString;
    /** Over days */
    overDays: I18nString;
    /** Overview */
    overview: I18nString;
    /** Password: %s */
    passwordX: I18nString;
    /** Private. Will never be shown outside the class. Helps you remember who the student is. */
    privateWillNeverBeShown: I18nString;
    /** Progress */
    progress: I18nString;
    /** Quickly generate safe usernames and passwords for students */
    quicklyGenerateSafeUsernames: I18nString;
    /** Real name */
    realName: I18nString;
    /** Real, unique email address of the student. We will send a confirmation email to it, with a link to graduate the account. */
    realUniqueEmail: I18nString;
    /** Graduate */
    release: I18nString;
    /** A graduated account cannot be managed again. The student will be able to toggle kid mode and reset password themselves. */
    releaseDesc1: I18nString;
    /** The student will remain in the class after their account is graduated. */
    releaseDesc2: I18nString;
    /** Graduate the account so the student can manage it autonomously. */
    releaseTheAccount: I18nString;
    /** Removed by %s */
    removedByX: I18nString;
    /** Removed */
    removedStudents: I18nString;
    /** Remove student */
    removeStudent: I18nString;
    /** Reopen */
    reopen: I18nString;
    /** Reset password */
    resetPassword: I18nString;
    /** Send a message to all students. */
    sendAMessage: I18nString;
    /** Student:  %1$s */
    studentCredentials: I18nString;
    /** Students */
    students: I18nString;
    /** Students' real names, one per line */
    studentsRealNamesOnePerLine: I18nString;
    /** Teach classes of chess students with the Lichess Classes tool suite. */
    teachClassesOfChessStudents: I18nString;
    /** Teachers */
    teachers: I18nString;
    /** Teachers of the class */
    teachersOfTheClass: I18nString;
    /** Teachers: %s */
    teachersX: I18nString;
    /** This student account is managed */
    thisStudentAccountIsManaged: I18nString;
    /** Time playing */
    timePlaying: I18nString;
    /** Track student progress in games and puzzles */
    trackStudentProgress: I18nString;
    /** Upgrade from managed to autonomous */
    upgradeFromManaged: I18nString;
    /** use this form */
    useThisForm: I18nString;
    /** %1$s over last %2$s */
    variantXOverLastY: I18nString;
    /** Visible by both teachers and students of the class */
    visibleByBothStudentsAndTeachers: I18nString;
    /** Welcome to your class: %s. */
    welcomeToClass: I18nString;
    /** Win rate */
    winrate: I18nString;
    /** %s already has a pending invitation */
    xAlreadyHasAPendingInvitation: I18nString;
    /** %1$s is a kid account and can't receive your message. You must give them the invitation URL manually: %2$s */
    xIsAKidAccountWarning: I18nString;
    /** %s is now a student of the class */
    xisNowAStudentOfTheClass: I18nString;
    /** You accepted this invitation. */
    youAcceptedThisInvitation: I18nString;
    /** You declined this invitation. */
    youDeclinedThisInvitation: I18nString;
    /** You have been invited by %s. */
    youHaveBeenInvitedByX: I18nString;
  };
  coach: {
    /** About me */
    aboutMe: I18nString;
    /** Accepting students */
    accepting: I18nString;
    /** Are you a great chess coach with a %s? */
    areYouCoach: I18nString;
    /** Availability */
    availability: I18nString;
    /** Best skills */
    bestSkills: I18nString;
    /** Confirm your title here and we will review your application. */
    confirmTitle: I18nString;
    /** Hourly rate */
    hourlyRate: I18nString;
    /** Languages */
    languages: I18nString;
    /** Lichess coach */
    lichessCoach: I18nString;
    /** Lichess coaches */
    lichessCoaches: I18nString;
    /** Location */
    location: I18nString;
    /** NM or FIDE title */
    nmOrFideTitle: I18nString;
    /** Not accepting students at the moment */
    notAccepting: I18nString;
    /** Other experiences */
    otherExperiences: I18nString;
    /** Playing experience */
    playingExperience: I18nString;
    /** Public studies */
    publicStudies: I18nString;
    /** Rating */
    rating: I18nString;
    /** Send us an email at %s and we will review your application. */
    sendApplication: I18nString;
    /** Send a private message */
    sendPM: I18nString;
    /** Teaching experience */
    teachingExperience: I18nString;
    /** Teaching methodology */
    teachingMethod: I18nString;
    /** View %s Lichess profile */
    viewXProfile: I18nString;
    /** %s coaches chess students */
    xCoachesStudents: I18nString;
    /** YouTube videos */
    youtubeVideos: I18nString;
  };
  contact: {
    /** However if you indeed used engine assistance, even just once, then your account is unfortunately lost. */
    accountLost: I18nString;
    /** I need account support */
    accountSupport: I18nString;
    /** Authorisation to use Lichess */
    authorizationToUse: I18nString;
    /** Appeal for a ban or IP restriction */
    banAppeal: I18nString;
    /** In certain circumstances when playing against a bot account, a rated game may not award points if it is determined that the player is abusing the bot for rating points. */
    botRatingAbuse: I18nString;
    /** Buying Lichess */
    buyingLichess: I18nString;
    /** It is called "en passant" and is one of the rules of chess. */
    calledEnPassant: I18nString;
    /** We can't change more than the case. For technical reasons, it's downright impossible. */
    cantChangeMore: I18nString;
    /** It's not possible to clear your game history, puzzle history, or ratings. */
    cantClearHistory: I18nString;
    /** If you imported the game, or started it from a position, make sure you correctly set the castling rights. */
    castlingImported: I18nString;
    /** Castling is only prevented if the king goes through a controlled square. */
    castlingPrevented: I18nString;
    /** Make sure you understand the castling rules */
    castlingRules: I18nString;
    /** Visit this page to change the case of your username */
    changeUsernameCase: I18nString;
    /** You can close your account on this page */
    closeYourAccount: I18nString;
    /** Collaboration, legal, commercial */
    collaboration: I18nString;
    /** Contact */
    contact: I18nString;
    /** Contact Lichess */
    contactLichess: I18nString;
    /** Credit is appreciated but not required. */
    creditAppreciated: I18nString;
    /** Do not ask us by email to close an account, we won't do it. */
    doNotAskByEmail: I18nString;
    /** Do not ask us by email to reopen an account, we won't do it. */
    doNotAskByEmailToReopen: I18nString;
    /** Do not deny having cheated. If you want to be allowed to create a new account, just admit to what you did, and show that you understood that it was a mistake. */
    doNotDeny: I18nString;
    /** Please do not send direct messages to moderators. */
    doNotMessageModerators: I18nString;
    /** Do not report players in the forum. */
    doNotReportInForum: I18nString;
    /** Do not send us report emails. */
    doNotSendReportEmails: I18nString;
    /** Complete a password reset to remove your second factor */
    doPasswordReset: I18nString;
    /** Engine or cheat mark */
    engineAppeal: I18nString;
    /** Error page */
    errorPage: I18nString;
    /** Please explain your request clearly and thoroughly. State your Lichess username, and any information that could help us help you. */
    explainYourRequest: I18nString;
    /** False positives do happen sometimes, and we're sorry about that. */
    falsePositives: I18nString;
    /** According to the FIDE Laws of Chess §6.9, if a checkmate is possible with any legal sequence of moves, then the game is not a draw */
    fideMate: I18nString;
    /** I forgot my password */
    forgotPassword: I18nString;
    /** I forgot my username */
    forgotUsername: I18nString;
    /** Please describe what the bug looks like, what you expected to happen instead, and the steps to reproduce the bug. */
    howToReportBug: I18nString;
    /** I can't log in */
    iCantLogIn: I18nString;
    /** If your appeal is legitimate, we will lift the ban ASAP. */
    ifLegit: I18nString;
    /** Illegal or impossible castling */
    illegalCastling: I18nString;
    /** Illegal pawn capture */
    illegalPawnCapture: I18nString;
    /** Insufficient mating material */
    insufficientMaterial: I18nString;
    /** It is possible to checkmate with only a knight or a bishop, if the opponent has more than a king on the board. */
    knightMate: I18nString;
    /** Learn how to make your own broadcasts on Lichess */
    learnHowToMakeBroadcasts: I18nString;
    /** I lost access to my two-factor authentication codes */
    lost2FA: I18nString;
    /** Monetising Lichess */
    monetizing: I18nString;
    /** I didn't receive my confirmation email */
    noConfirmationEmail: I18nString;
    /** None of the above */
    noneOfTheAbove: I18nString;
    /** No rating points were awarded */
    noRatingPoints: I18nString;
    /** Only reporting players through the report form is effective. */
    onlyReports: I18nString;
    /** However, you can close your current account, and create a new one. */
    orCloseAccount: I18nString;
    /** Other restriction */
    otherRestriction: I18nString;
    /** Make sure you played a rated game. Casual games do not affect the players ratings. */
    ratedGame: I18nString;
    /** You can reopen your account on this page. It only works once. */
    reopenOnThisPage: I18nString;
    /** In the Lichess Discord server */
    reportBugInDiscord: I18nString;
    /** In the Lichess Feedback section of the forum */
    reportBugInForum: I18nString;
    /** If you faced an error page, you may report it: */
    reportErrorPage: I18nString;
    /** As a Lichess mobile app issue on GitHub */
    reportMobileIssue: I18nString;
    /** As a Lichess website issue on GitHub */
    reportWebsiteIssue: I18nString;
    /** You may send an appeal to %s. */
    sendAppealTo: I18nString;
    /** Send us an email at %s. */
    sendEmailAt: I18nString;
    /** To report a player, use the report form */
    toReportAPlayerUseForm: I18nString;
    /** Try this little interactive game to practice castling in chess */
    tryCastling: I18nString;
    /** Try this little interactive game to learn more about "en passant". */
    tryEnPassant: I18nString;
    /** You can show it in your videos, and you can print screenshots of Lichess in your books. */
    videosAndBooks: I18nString;
    /** Visit this page to solve the issue */
    visitThisPage: I18nString;
    /** To show your title on your Lichess profile, and participate in Titled Arenas, visit the title confirmation page */
    visitTitleConfirmation: I18nString;
    /** I want to change my username */
    wantChangeUsername: I18nString;
    /** I want to clear my history or rating */
    wantClearHistory: I18nString;
    /** I want to close my account */
    wantCloseAccount: I18nString;
    /** I want to reopen my account */
    wantReopen: I18nString;
    /** I want to report a player */
    wantReport: I18nString;
    /** I want to report a bug */
    wantReportBug: I18nString;
    /** I want my title displayed on Lichess */
    wantTitle: I18nString;
    /** You are welcome to use Lichess for your activity, even commercial. */
    welcomeToUse: I18nString;
    /** What can we help you with? */
    whatCanWeHelpYouWith: I18nString;
    /** You can also reach that page by clicking the %s report button on a profile page. */
    youCanAlsoReachReportPage: I18nString;
    /** You can login with the email address you signed up with */
    youCanLoginWithEmail: I18nString;
  };
  coordinates: {
    /** A coordinate appears on the board and you must click on the corresponding square. */
    aCoordinateAppears: I18nString;
    /** A square is highlighted on the board and you must enter its coordinate (e.g. "e4"). */
    aSquareIsHighlightedExplanation: I18nString;
    /** Average score as black: %s */
    averageScoreAsBlackX: I18nString;
    /** Average score as white: %s */
    averageScoreAsWhiteX: I18nString;
    /** Coordinates */
    coordinates: I18nString;
    /** Coordinate training */
    coordinateTraining: I18nString;
    /** Find square */
    findSquare: I18nString;
    /** Go as long as you want, there is no time limit! */
    goAsLongAsYouWant: I18nString;
    /** Knowing the chessboard coordinates is a very important skill for several reasons: */
    knowingTheChessBoard: I18nString;
    /** Most chess courses and exercises use the algebraic notation extensively. */
    mostChessCourses: I18nString;
    /** Name square */
    nameSquare: I18nString;
    /** Show coordinates */
    showCoordinates: I18nString;
    /** Coordinates on every square */
    showCoordsOnAllSquares: I18nString;
    /** Show pieces */
    showPieces: I18nString;
    /** Start training */
    startTraining: I18nString;
    /** It makes it easier to talk to your chess friends, since you both understand the 'language of chess'. */
    talkToYourChessFriends: I18nString;
    /** You can analyse a game more effectively if you can quickly recognise coordinates. */
    youCanAnalyseAGameMoreEffectively: I18nString;
    /** You have 30 seconds to correctly map as many squares as possible! */
    youHaveThirtySeconds: I18nString;
  };
  dgt: {
    /** Announce All Moves */
    announceAllMoves: I18nString;
    /** Announce Move Format */
    announceMoveFormat: I18nString;
    /** As a last resort, setup the board identically as Lichess, then %s */
    asALastResort: I18nString;
    /** The board will auto connect to any game that is already on course or any new game that starts. Ability to choose which game to play is coming soon. */
    boardWillAutoConnect: I18nString;
    /** Check that you have made your opponent's move on the DGT board first. Revert your move. Play again. */
    checkYouHaveMadeOpponentsMove: I18nString;
    /** Click to generate one */
    clickToGenerateOne: I18nString;
    /** Configuration Section */
    configurationSection: I18nString;
    /** Configure */
    configure: I18nString;
    /** Configure voice narration of the played moves, so you can keep your eyes on the board. */
    configureVoiceNarration: I18nString;
    /** Debug */
    debug: I18nString;
    /** DGT board */
    dgtBoard: I18nString;
    /** DGT board connectivity */
    dgtBoardConnectivity: I18nString;
    /** DGT Board Limitations */
    dgtBoardLimitations: I18nString;
    /** DGT Board Requirements */
    dgtBoardRequirements: I18nString;
    /** DGT - Configure */
    dgtConfigure: I18nString;
    /** A %s entry was added to your PLAY menu at the top. */
    dgtPlayMenuEntryAdded: I18nString;
    /** You can download the software here: %s. */
    downloadHere: I18nString;
    /** Enable Speech Synthesis */
    enableSpeechSynthesis: I18nString;
    /** If %1$s is running on a different machine or different port, you will need to set the IP address and port here in the %2$s. */
    ifLiveChessRunningElsewhere: I18nString;
    /** If %1$s is running on this computer, you can check your connection to it by %2$s. */
    ifLiveChessRunningOnThisComputer: I18nString;
    /** If a move is not detected */
    ifMoveNotDetected: I18nString;
    /** The play page needs to remain open on your browser. It does not need to be visible, you can minimize it or set it side to side with the Lichess game page, but don't close it or the board will stop working. */
    keepPlayPageOpen: I18nString;
    /** Keywords are in JSON format. They are used to translate moves and results into your language. Default is English, but feel free to change it. */
    keywordFormatDescription: I18nString;
    /** Keywords */
    keywords: I18nString;
    /** Lichess & DGT */
    lichessAndDgt: I18nString;
    /** Lichess connectivity */
    lichessConnectivity: I18nString;
    /** SAN is the standard on Lichess like "Nf6". UCI is common on engines like "g8f6". */
    moveFormatDescription: I18nString;
    /** No suitable OAuth token has been created. */
    noSuitableOauthToken: I18nString;
    /** opening this link */
    openingThisLink: I18nString;
    /** Play with a DGT board */
    playWithDgtBoard: I18nString;
    /** Reload this page */
    reloadThisPage: I18nString;
    /** Select YES to announce both your moves and your opponent's moves. Select NO to announce only your opponent's moves. */
    selectAnnouncePreference: I18nString;
    /** Speech synthesis voice */
    speechSynthesisVoice: I18nString;
    /** Text to speech */
    textToSpeech: I18nString;
    /** This page allows you to connect your DGT board to Lichess, and to use it for playing games. */
    thisPageAllowsConnectingDgtBoard: I18nString;
    /** Time controls for casual games: Classical, Correspondence and Rapid only. */
    timeControlsForCasualGames: I18nString;
    /** Time controls for rated games: Classical, Correspondence and some Rapids including 15+10 and 20+0 */
    timeControlsForRatedGames: I18nString;
    /** To connect to the DGT Electronic Board you will need to install %s. */
    toConnectTheDgtBoard: I18nString;
    /** To see console message press Command + Option + C (Mac) or Control + Shift + C (Windows, Linux, Chrome OS) */
    toSeeConsoleMessage: I18nString;
    /** Use "%1$s" unless %2$s is running on a different machine or different port. */
    useWebSocketUrl: I18nString;
    /** You have an OAuth token suitable for DGT play. */
    validDgtOauthToken: I18nString;
    /** Verbose logging */
    verboseLogging: I18nString;
    /** %s WebSocket URL */
    webSocketUrl: I18nString;
    /** When ready, setup your board and then click %s. */
    whenReadySetupBoard: I18nString;
  };
  emails: {
    /** To contact us, please use %s. */
    common_contact: I18nString;
    /** This is a service email related to your use of %s. */
    common_note: I18nString;
    /** (Clicking not working? Try pasting it into your browser!) */
    common_orPaste: I18nString;
    /** To confirm you have access to this email, please click the link below: */
    emailChange_click: I18nString;
    /** You have requested to change your email address. */
    emailChange_intro: I18nString;
    /** Confirm new email address, %s */
    emailChange_subject: I18nString;
    /** Click the link to enable your Lichess account: */
    emailConfirm_click: I18nString;
    /** If you did not register with Lichess you can safely ignore this message. */
    emailConfirm_ignore: I18nString;
    /** Confirm your lichess.org account, %s */
    emailConfirm_subject: I18nString;
    /** Log in to lichess.org, %s */
    logInToLichess: I18nString;
    /** If you made this request, click the link below. If not, you can ignore this email. */
    passwordReset_clickOrIgnore: I18nString;
    /** We received a request to reset the password for your account. */
    passwordReset_intro: I18nString;
    /** Reset your lichess.org password, %s */
    passwordReset_subject: I18nString;
    /** Welcome to lichess.org, %s */
    welcome_subject: I18nString;
    /** You have successfully created your account on https://lichess.org. */
    welcome_text: I18nString;
  };
  faq: {
    /** Accounts */
    accounts: I18nString;
    /** The centipawn is the unit of measure used in chess as representation of the advantage. A centipawn is equal to 1/100th of a pawn. Therefore 100 centipawns = 1 pawn. These values play no formal role in the game but are useful to players, and essential in computer chess, for evaluating positions. */
    acplExplanation: I18nString;
    /** We regularly receive messages from users asking us for help to stop them from playing too much. */
    adviceOnMitigatingAddiction: I18nString;
    /** an hourly Bullet tournament */
    aHourlyBulletTournament: I18nString;
    /** Are there websites based on Lichess? */
    areThereWebsitesBasedOnLichess: I18nString;
    /** many national master titles */
    asWellAsManyNMtitles: I18nString;
    /** Lichess time controls are based on estimated game duration = %1$s. */
    basedOnGameDuration: I18nString;
    /** being a patron */
    beingAPatron: I18nString;
    /** be in the top 10 in this rating. */
    beInTopTen: I18nString;
    /** breakdown of our costs */
    breakdownOfOurCosts: I18nString;
    /** Can I get the Lichess Master (LM) title? */
    canIbecomeLM: I18nString;
    /** Can I change my username? */
    canIChangeMyUsername: I18nString;
    /** configure */
    configure: I18nString;
    /** I lost a game due to lag/disconnection. Can I get my rating points back? */
    connexionLostCanIGetMyRatingBack: I18nString;
    /** desktop */
    desktop: I18nString;
    /** Why can a pawn capture another pawn when it is already passed? (en passant) */
    discoveringEnPassant: I18nString;
    /** display preferences */
    displayPreferences: I18nString;
    /** (clock initial time in seconds) + 40 × (clock increment) */
    durationFormula: I18nString;
    /** 8 chess variants */
    eightVariants: I18nString;
    /** Most browsers can prevent sound from playing on a freshly loaded page to protect users. Imagine if every website could immediately bombard you with audio ads. */
    enableAutoplayForSoundsA: I18nString;
    /** 1. Go to lichess.org */
    enableAutoplayForSoundsChrome: I18nString;
    /** 1. Go to lichess.org */
    enableAutoplayForSoundsFirefox: I18nString;
    /** 1. Click the three dots in the top right corner */
    enableAutoplayForSoundsMicrosoftEdge: I18nString;
    /** Enable autoplay for sounds? */
    enableAutoplayForSoundsQ: I18nString;
    /** 1. Go to lichess.org */
    enableAutoplayForSoundsSafari: I18nString;
    /** Enable or disable notification popups? */
    enableDisableNotificationPopUps: I18nString;
    /** Enable Zen-mode in the %1$s, or by pressing %2$s during a game. */
    enableZenMode: I18nString;
    /** This is a legal move known as "en passant". The Wikipedia article gives a %1$s. */
    explainingEnPassant: I18nString;
    /** Fair Play */
    fairPlay: I18nString;
    /** fair play page */
    fairPlayPage: I18nString;
    /** FAQ */
    faqAbbreviation: I18nString;
    /** fewer lobby pools */
    fewerLobbyPools: I18nString;
    /** FIDE handbook */
    fideHandbook: I18nString;
    /** FIDE handbook %s */
    fideHandbookX: I18nString;
    /** You can find out more about %1$s (including a %2$s). If you want to help Lichess by volunteering your time and skills, there are many %3$s. */
    findMoreAndSeeHowHelp: I18nString;
    /** Frequently Asked Questions */
    frequentlyAskedQuestions: I18nString;
    /** Gameplay */
    gameplay: I18nString;
    /** ZugAddict was streaming and for the last 2 hours he had been trying to defeat A.I. level 8 in a 1+0 game, without success. Thibault told him that if he successfully did it on stream, he'd get a unique trophy. One hour later, he smashed Stockfish, and the promise was honoured. */
    goldenZeeExplanation: I18nString;
    /** good introduction */
    goodIntroduction: I18nString;
    /** guidelines */
    guidelines: I18nString;
    /** have played a rated game within the last week for this rating, */
    havePlayedARatedGameAtLeastOneWeekAgo: I18nString;
    /** have played at least 30 rated games in a given rating, */
    havePlayedMoreThanThirtyGamesInThatRating: I18nString;
    /** Hear it pronounced by a specialist. */
    hearItPronouncedBySpecialist: I18nString;
    /** How are Bullet, Blitz and other time controls decided? */
    howBulletBlitzEtcDecided: I18nString;
    /** How can I become a moderator? */
    howCanIBecomeModerator: I18nString;
    /** How can I contribute to Lichess? */
    howCanIContributeToLichess: I18nString;
    /** How do ranks and leaderboards work? */
    howDoLeaderoardsWork: I18nString;
    /** How to hide ratings while playing? */
    howToHideRatingWhilePlaying: I18nString;
    /** How to... */
    howToThreeDots: I18nString;
    /** ≤ %1$ss = %2$s */
    inferiorThanXsEqualYtimeControl: I18nString;
    /** In order to get on the %1$s you must: */
    inOrderToAppearsYouMust: I18nString;
    /** Losing on time, drawing and insufficient material */
    insufficientMaterial: I18nString;
    /** Is correspondence different from normal chess? */
    isCorrespondenceDifferent: I18nString;
    /** What keyboard shortcuts are there? */
    keyboardShortcuts: I18nString;
    /** Some Lichess pages have keyboard shortcuts you can use. Try pressing the '?' key on a study, analysis, puzzle, or game page to list available keyboard shortcuts. */
    keyboardShortcutsExplanation: I18nString;
    /** If your opponent frequently aborts/leaves games, they get "play banned", which means they're temporarily banned from playing games. This is not publicly indicated on their profile. If this behaviour continues, the length of the playban increases - and prolonged behaviour of this nature may lead to account closure. */
    leavingGameWithoutResigningExplanation: I18nString;
    /** lee-chess */
    leechess: I18nString;
    /** Lichess can optionally send popup notifications, for example when it is your turn or you received a private message. */
    lichessCanOptionnalySendPopUps: I18nString;
    /** Lichess is a combination of live/light/libre and chess. It is pronounced %1$s. */
    lichessCombinationLiveLightLibrePronounced: I18nString;
    /** In the event of one player running out of time, that player will usually lose the game. However, the game is drawn if the position is such that the opponent cannot checkmate the player's king by any possible series of legal moves (%1$s). */
    lichessFollowFIDErules: I18nString;
    /** Lichess is powered by donations from patrons and the efforts of a team of volunteers. */
    lichessPoweredByDonationsAndVolunteers: I18nString;
    /** Lichess ratings */
    lichessRatings: I18nString;
    /** Lichess recognises all FIDE titles gained from OTB (over the board) play, as well as %1$s. Here is a list of FIDE titles: */
    lichessRecognizeAllOTBtitles: I18nString;
    /** Lichess supports standard chess and %1$s. */
    lichessSupportChessAnd: I18nString;
    /** Lichess training */
    lichessTraining: I18nString;
    /** Lichess userstyles */
    lichessUserstyles: I18nString;
    /** This honorific title is unofficial and only exists on Lichess. */
    lMtitleComesToYouDoNotRequestIt: I18nString;
    /** stand-alone mental health condition */
    mentalHealthCondition: I18nString;
    /** The player has not yet finished enough rated games against %1$s in the rating category. */
    notPlayedEnoughRatedGamesAgainstX: I18nString;
    /** The player hasn't played enough recent games. Depending on the number of games you've played, it might take around a year of inactivity for your rating to become provisional again. */
    notPlayedRecently: I18nString;
    /** We did not repeat moves. Why was the game still drawn by repetition? */
    notRepeatedMoves: I18nString;
    /** No. */
    noUpperCaseDot: I18nString;
    /** other ways to help */
    otherWaysToHelp: I18nString;
    /** That trophy is unique in the history of Lichess, nobody other than %1$s will ever have it. */
    ownerUniqueTrophies: I18nString;
    /** For more information, please read our %s */
    pleaseReadFairPlayPage: I18nString;
    /** positions */
    positions: I18nString;
    /** What is done about players leaving games without resigning? */
    preventLeavingGameWithoutResigning: I18nString;
    /** The question mark means the rating is provisional. Reasons include: */
    provisionalRatingExplanation: I18nString;
    /** have a rating deviation lower than %1$s, in standard chess, and lower than %2$s in variants, */
    ratingDeviationLowerThanXinChessYinVariants: I18nString;
    /** Concretely, it means that the Glicko-2 deviation is greater than 110. The deviation is the level of confidence the system has in the rating. The lower the deviation, the more stable is a rating. */
    ratingDeviationMorethanOneHundredTen: I18nString;
    /** rating leaderboard */
    ratingLeaderboards: I18nString;
    /** One minute after a player is marked, their 40 latest rated games in the last 5 days are taken. If you were their opponent in one of those games, you lost rating (because of a loss or a draw), and your rating was not provisional, you get a rating refund. The refund is capped based on your peak rating and your rating progress after the game. (For example, if your rating greatly increased after that game, you might get no refund or only a partial refund.) A refund will never exceed 150 points. */
    ratingRefundExplanation: I18nString;
    /** Ratings are calculated using the Glicko-2 rating method developed by Mark Glickman. This is a very popular rating method, and is used by a significant number of chess organisations (FIDE being a notable counter-example, as they still use the dated Elo rating system). */
    ratingSystemUsedByLichess: I18nString;
    /** Threefold repetition is about repeated %1$s, not moves. Repetition does not have to occur consecutively. */
    repeatedPositionsThatMatters: I18nString;
    /** The 2nd requirement is so that players who no longer use their accounts stop populating leaderboards. */
    secondRequirementToStopOldPlayersTrustingLeaderboards: I18nString;
    /** If you have an OTB title, you can apply to have this displayed on your account by completing the %1$s, including a clear image of an identifying document/card and a selfie of you holding the document/card. */
    showYourTitle: I18nString;
    /** opponents of similar strength */
    similarOpponents: I18nString;
    /** Stop myself from playing? */
    stopMyselfFromPlaying: I18nString;
    /** ≥ %1$ss = %2$s */
    superiorThanXsEqualYtimeControl: I18nString;
    /** Repetition needs to be claimed by one of the players. You can do so by pressing the button that is shown, or by offering a draw before your final repeating move, it won't matter if your opponent rejects the draw offer, the threefold repetition draw will be claimed anyway. You can also %1$s Lichess to automatically claim repetitions for you. Additionally, fivefold repetition always immediately ends the game. */
    threeFoldHasToBeClaimed: I18nString;
    /** Threefold repetition */
    threefoldRepetition: I18nString;
    /** If a position occurs three times, players can claim a draw by %1$s. Lichess implements the official FIDE rules, as described in Article 9.2 of the %2$s. */
    threefoldRepetitionExplanation: I18nString;
    /** threefold repetition */
    threefoldRepetitionLowerCase: I18nString;
    /** What titles are there on Lichess? */
    titlesAvailableOnLichess: I18nString;
    /** Unique trophies */
    uniqueTrophies: I18nString;
    /** No, usernames cannot be changed for technical and practical reasons. Usernames are materialized in too many places: databases, exports, logs, and people's minds. You can adjust the capitalization once. */
    usernamesCannotBeChanged: I18nString;
    /** In general, usernames should not be: offensive, impersonating someone else, or advertising. You can read more about the %1$s. */
    usernamesNotOffensive: I18nString;
    /** verification form */
    verificationForm: I18nString;
    /** View site information popup */
    viewSiteInformationPopUp: I18nString;
    /** Watch International Master Eric Rosen checkmate %s. */
    watchIMRosenCheckmate: I18nString;
    /** To get it, hiimgosu challenged himself to berserk and win all games of %s. */
    wayOfBerserkExplanation: I18nString;
    /** Unfortunately, we cannot give back rating points for games lost due to lag or disconnection, regardless of whether the problem was at your end or our end. The latter is very rare though. Also note that when Lichess restarts and you lose on time because of that, we abort the game to prevent an unfair loss. */
    weCannotDoThatEvenIfItIsServerSideButThatsRare: I18nString;
    /** We repeated a position three times. Why was the game not drawn? */
    weRepeatedthreeTimesPosButNoDraw: I18nString;
    /** What is the average centipawn loss (ACPL)? */
    whatIsACPL: I18nString;
    /** Why is there a question mark (?) next to a rating? */
    whatIsProvisionalRating: I18nString;
    /** What can my username be? */
    whatUsernameCanIchoose: I18nString;
    /** What variants can I play on Lichess? */
    whatVariantsCanIplay: I18nString;
    /** When am I eligible for the automatic rating refund from cheaters? */
    whenAmIEligibleRatinRefund: I18nString;
    /** What rating system does Lichess use? */
    whichRatingSystemUsedByLichess: I18nString;
    /** Why are ratings higher compared to other sites and organisations such as FIDE, USCF and the ICC? */
    whyAreRatingHigher: I18nString;
    /** It is best not to think of ratings as absolute numbers, or compare them against other organisations. Different organisations have different levels of players, different rating systems (Elo, Glicko, Glicko-2, or a modified version of the aforementioned). These factors can drastically affect the absolute numbers (ratings). */
    whyAreRatingHigherExplanation: I18nString;
    /** Why is Lichess called Lichess? */
    whyIsLichessCalledLichess: I18nString;
    /** Similarly, the source code for Lichess, %1$s, stands for li[chess in sca]la, seeing as the bulk of Lichess is written in %2$s, an intuitive programming language. */
    whyIsLilaCalledLila: I18nString;
    /** Live, because games are played and watched in real-time 24/7; light and libre for the fact that Lichess is open-source and unencumbered by proprietary junk that plagues other websites. */
    whyLiveLightLibre: I18nString;
    /** Yes. Lichess has indeed inspired other open-source sites that use our %1$s, %2$s, or %3$s. */
    yesLichessInspiredOtherOpenSourceWebsites: I18nString;
    /** It’s not possible to apply to become a moderator. If we see someone who we think would be good as a moderator, we will contact them directly. */
    youCannotApply: I18nString;
    /** On Lichess, the main difference in rules for correspondence chess is that an opening book is allowed. The use of engines is still prohibited and will result in being flagged for engine assistance. Although ICCF allows engine use in correspondence, Lichess does not. */
    youCanUseOpeningBookNoEngine: I18nString;
  };
  features: {
    /** All chess basics lessons */
    allChessBasicsLessons: I18nString;
    /** All features are free for everybody, forever! */
    allFeaturesAreFreeForEverybody: I18nString;
    /** All features to come, forever! */
    allFeaturesToCome: I18nString;
    /** Board editor and analysis board with %s */
    boardEditorAndAnalysisBoardWithEngine: I18nString;
    /** Chess insights (detailed analysis of your play) */
    chessInsights: I18nString;
    /** Cloud engine analysis */
    cloudEngineAnalysis: I18nString;
    /** Contribute to Lichess and get a cool looking Patron icon */
    contributeToLichessAndGetIcon: I18nString;
    /** Correspondence chess with conditional premoves */
    correspondenceWithConditionalPremoves: I18nString;
    /** Deep %s server analysis */
    deepXServerAnalysis: I18nString;
    /** Download/Upload any game as PGN */
    downloadOrUploadAnyGameAsPgn: I18nString;
    /** 7-piece endgame tablebase */
    endgameTablebase: I18nString;
    /** Yes, both accounts have the same features! */
    everybodyGetsAllFeaturesForFree: I18nString;
    /** %s games per day */
    gamesPerDay: I18nPlural;
    /** Global opening explorer (%s games!) */
    globalOpeningExplorerInNbGames: I18nString;
    /** If you love Lichess, */
    ifYouLoveLichess: I18nString;
    /** iPhone & Android phones and tablets, landscape support */
    landscapeSupportOnApp: I18nString;
    /** Light/Dark theme, custom boards, pieces and background */
    lightOrDarkThemeCustomBoardsPiecesAndBackground: I18nString;
    /** Personal opening explorer */
    personalOpeningExplorer: I18nString;
    /** %1$s (also works on %2$s) */
    personalOpeningExplorerX: I18nString;
    /** Standard chess and %s */
    standardChessAndX: I18nString;
    /** Studies (shareable and persistent analysis) */
    studies: I18nString;
    /** Support Lichess */
    supportLichess: I18nString;
    /** Support us with a Patron account! */
    supportUsWithAPatronAccount: I18nString;
    /** Tactical puzzles from user games */
    tacticalPuzzlesFromUserGames: I18nString;
    /** Blog, forum, teams, TV, messaging, friends, challenges */
    tvForumBlogTeamsMessagingFriendsChallenges: I18nString;
    /** UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence Chess */
    ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess: I18nString;
    /** We believe every chess player deserves the best, and so: */
    weBelieveEveryChessPlayerDeservesTheBest: I18nString;
    /** Zero advertisement, no tracking */
    zeroAdsAndNoTracking: I18nString;
  };
  insight: {
    /** Sorry, you cannot see %s's chess insights. */
    cantSeeInsights: I18nString;
    /** Now crunching data just for you! */
    crunchingData: I18nString;
    /** Generate %s's chess insights. */
    generateInsights: I18nString;
    /** %s's chess insights are protected */
    insightsAreProtected: I18nString;
    /** insights settings */
    insightsSettings: I18nString;
    /** Maybe ask them to change their %s? */
    maybeAskThemToChangeTheir: I18nString;
    /** %s's chess insights */
    xChessInsights: I18nString;
    /** %s has no chess insights yet! */
    xHasNoChessInsights: I18nString;
  };
  keyboardMove: {
    /** Both the letter "o" and the digit zero "0" can be used when castling */
    bothTheLetterOAndTheDigitZero: I18nString;
    /** Capitalization only matters in ambiguous situations involving a bishop and the b-pawn */
    capitalizationOnlyMattersInAmbiguousSituations: I18nString;
    /** Drop a rook at b4 (Crazyhouse variant only) */
    dropARookAtB4: I18nString;
    /** If it is legal to castle both ways, use enter to kingside castle */
    ifItIsLegalToCastleBothWays: I18nString;
    /** If the above move notation is unfamiliar, learn more here: */
    ifTheAboveMoveNotationIsUnfamiliar: I18nString;
    /** Including an "x" to indicate a capture is optional */
    includingAXToIndicateACapture: I18nString;
    /** Keyboard input commands */
    keyboardInputCommands: I18nString;
    /** Kingside castle */
    kingsideCastle: I18nString;
    /** Move knight to c3 */
    moveKnightToC3: I18nString;
    /** Move piece from e2 to e4 */
    movePieceFromE2ToE4: I18nString;
    /** Offer or accept draw */
    offerOrAcceptDraw: I18nString;
    /** Other commands */
    otherCommands: I18nString;
    /** Perform a move */
    performAMove: I18nString;
    /** Promote c8 to queen */
    promoteC8ToQueen: I18nString;
    /** Queenside castle */
    queensideCastle: I18nString;
    /** Read out clocks */
    readOutClocks: I18nString;
    /** Read out opponent's name */
    readOutOpponentName: I18nString;
    /** Tips */
    tips: I18nString;
    /** To premove, simply type the desired premove before it is your turn */
    toPremoveSimplyTypeTheDesiredPremove: I18nString;
  };
  lag: {
    /** And now, the long answer! Game lag is composed of two unrelated values (lower is better): */
    andNowTheLongAnswerLagComposedOfTwoValues: I18nString;
    /** Is Lichess lagging? */
    isLichessLagging: I18nString;
    /** Lag compensation */
    lagCompensation: I18nString;
    /** Lichess compensates network lag. This includes sustained lag and occasional lag spikes. There are limits and heuristics based on time control and the compensated lag so far, so that the result should feel reasonable for both players. As a result, having a higher network lag than your opponent is not a handicap! */
    lagCompensationExplanation: I18nString;
    /** Lichess server latency */
    lichessServerLatency: I18nString;
    /** The time it takes to process a move on the server. It's the same for everybody, and only depends on the servers load. The more players, the higher it gets, but Lichess developers do their best to keep it low. It rarely exceeds 10ms. */
    lichessServerLatencyExplanation: I18nString;
    /** Measurements in progress... */
    measurementInProgressThreeDot: I18nString;
    /** Network between Lichess and you */
    networkBetweenLichessAndYou: I18nString;
    /** The time it takes to send a move from your computer to Lichess server, and get the response back. It's specific to your distance to Lichess (France), and to the quality of your Internet connection. Lichess developers cannot fix your wifi or make light go faster. */
    networkBetweenLichessAndYouExplanation: I18nString;
    /** No. And your network is bad. */
    noAndYourNetworkIsBad: I18nString;
    /** No. And your network is good. */
    noAndYourNetworkIsGood: I18nString;
    /** Yes. It will be fixed soon! */
    yesItWillBeFixedSoon: I18nString;
    /** You can find both these values at any time, by clicking your username in the top bar. */
    youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername: I18nString;
  };
  learn: {
    /** Advanced */
    advanced: I18nString;
    /** A pawn on the second rank can move 2 squares at once! */
    aPawnOnTheSecondRank: I18nString;
    /** Attack the opponent's king */
    attackTheOpponentsKing: I18nString;
    /** Attack your opponent's king */
    attackYourOpponentsKing: I18nString;
    /** Awesome! */
    awesome: I18nString;
    /** Back to menu */
    backToMenu: I18nString;
    /** Congratulations! You can command a bishop. */
    bishopComplete: I18nString;
    /** Next we will learn how to manoeuvre a bishop! */
    bishopIntro: I18nString;
    /** Black just moved the pawn */
    blackJustMovedThePawnByTwoSquares: I18nString;
    /** Board setup */
    boardSetup: I18nString;
    /** Congratulations! You know how to set up the chess board. */
    boardSetupComplete: I18nString;
    /** The two armies face each other, ready for the battle. */
    boardSetupIntro: I18nString;
    /** by playing! */
    byPlaying: I18nString;
    /** Capture */
    capture: I18nString;
    /** Capture and defend pieces */
    captureAndDefendPieces: I18nString;
    /** Congratulations! You know how to fight with chess pieces! */
    captureComplete: I18nString;
    /** Identify the opponent's undefended pieces, and capture them! */
    captureIntro: I18nString;
    /** Capture, then promote! */
    captureThenPromote: I18nString;
    /** Move your king two squares */
    castleKingSide: I18nString;
    /** Castle king-side! */
    castleKingSideMovePiecesFirst: I18nString;
    /** Move your king two squares */
    castleQueenSide: I18nString;
    /** Castle queen-side! */
    castleQueenSideMovePiecesFirst: I18nString;
    /** Castling */
    castling: I18nString;
    /** Congratulations! You should almost always castle in a game. */
    castlingComplete: I18nString;
    /** Bring your king to safety, and deploy your rook for attack! */
    castlingIntro: I18nString;
    /** Check in one */
    checkInOne: I18nString;
    /** Congratulations! You checked your opponent, forcing them to defend their king! */
    checkInOneComplete: I18nString;
    /** Aim at the opponent's king */
    checkInOneGoal: I18nString;
    /** To check your opponent, attack their king. They must defend it! */
    checkInOneIntro: I18nString;
    /** Check in two */
    checkInTwo: I18nString;
    /** Congratulations! You checked your opponent, forcing them to defend their king! */
    checkInTwoComplete: I18nString;
    /** Threaten the opponent's king */
    checkInTwoGoal: I18nString;
    /** Find the right combination of two moves that checks the opponent's king! */
    checkInTwoIntro: I18nString;
    /** Chess pieces */
    chessPieces: I18nString;
    /** Combat */
    combat: I18nString;
    /** Congratulations! You know how to fight with chess pieces! */
    combatComplete: I18nString;
    /** A good warrior knows both attack and defence! */
    combatIntro: I18nString;
    /** Defeat the opponent's king */
    defeatTheOpponentsKing: I18nString;
    /** Defend your king */
    defendYourKing: I18nString;
    /** Don't let them take */
    dontLetThemTakeAnyUndefendedPiece: I18nString;
    /** En passant */
    enPassant: I18nString;
    /** Congratulations! You can now take en passant. */
    enPassantComplete: I18nString;
    /** When the opponent pawn moved by two squares, you can take it like if it moved by one square. */
    enPassantIntro: I18nString;
    /** En passant only works */
    enPassantOnlyWorksImmediately: I18nString;
    /** En passant only works */
    enPassantOnlyWorksOnFifthRank: I18nString;
    /** You're under attack! */
    escape: I18nString;
    /** Escape with the king */
    escapeOrBlock: I18nString;
    /** Escape with the king! */
    escapeWithTheKing: I18nString;
    /** Evaluate piece strength */
    evaluatePieceStrength: I18nString;
    /** Excellent! */
    excellent: I18nString;
    /** Exercise your tactical skills */
    exerciseYourTacticalSkills: I18nString;
    /** Find a way to */
    findAWayToCastleKingSide: I18nString;
    /** Find a way to */
    findAWayToCastleQueenSide: I18nString;
    /** First place the rooks! */
    firstPlaceTheRooks: I18nString;
    /** Fundamentals */
    fundamentals: I18nString;
    /** Get a free Lichess account */
    getAFreeLichessAccount: I18nString;
    /** Grab all the stars! */
    grabAllTheStars: I18nString;
    /** Grab all the stars! */
    grabAllTheStarsNoNeedToPromote: I18nString;
    /** Great job! */
    greatJob: I18nString;
    /** How the game starts */
    howTheGameStarts: I18nString;
    /** Intermediate */
    intermediate: I18nString;
    /** It moves diagonally */
    itMovesDiagonally: I18nString;
    /** It moves forward only */
    itMovesForwardOnly: I18nString;
    /** It moves in an L shape */
    itMovesInAnLShape: I18nString;
    /** It moves in straight lines */
    itMovesInStraightLines: I18nString;
    /** It now promotes to a stronger piece. */
    itNowPromotesToAStrongerPiece: I18nString;
    /** Keep your pieces safe */
    keepYourPiecesSafe: I18nString;
    /** You can now command the commander! */
    kingComplete: I18nString;
    /** You are the king. If you fall in battle, the game is lost. */
    kingIntro: I18nString;
    /** Congratulations! You have mastered the knight. */
    knightComplete: I18nString;
    /** Here's a challenge for you. The knight is... a tricky piece. */
    knightIntro: I18nString;
    /** Knights can jump over obstacles! */
    knightsCanJumpOverObstacles: I18nString;
    /** Knights have a fancy way */
    knightsHaveAFancyWay: I18nString;
    /** Last one! */
    lastOne: I18nString;
    /** Learn chess */
    learnChess: I18nString;
    /** Learn common chess positions */
    learnCommonChessPositions: I18nString;
    /** Let's go! */
    letsGo: I18nString;
    /** Mate in one */
    mateInOne: I18nString;
    /** Congratulations! That is how you win chess games! */
    mateInOneComplete: I18nString;
    /** You win when your opponent cannot defend against a check. */
    mateInOneIntro: I18nString;
    /** Menu */
    menu: I18nString;
    /** Most of the time promoting to a queen is the best. */
    mostOfTheTimePromotingToAQueenIsBest: I18nString;
    /** Nailed it. */
    nailedIt: I18nString;
    /** Next */
    next: I18nString;
    /** Next: %s */
    nextX: I18nString;
    /** There is no escape, */
    noEscape: I18nString;
    /** Opponents from around the world */
    opponentsFromAroundTheWorld: I18nString;
    /** Out of check */
    outOfCheck: I18nString;
    /** Congratulations! Your king can never be taken, make sure you can defend against a check! */
    outOfCheckComplete: I18nString;
    /** You are in check! You must escape or block the attack. */
    outOfCheckIntro: I18nString;
    /** Outstanding! */
    outstanding: I18nString;
    /** Congratulations! Pawns have no secrets for you. */
    pawnComplete: I18nString;
    /** Pawns are weak, but they pack a lot of potential. */
    pawnIntro: I18nString;
    /** Pawn promotion */
    pawnPromotion: I18nString;
    /** Pawns form the front line. */
    pawnsFormTheFrontLine: I18nString;
    /** Pawns move forward, */
    pawnsMoveForward: I18nString;
    /** Pawns move one square only. */
    pawnsMoveOneSquareOnly: I18nString;
    /** Perfect! */
    perfect: I18nString;
    /** Piece value */
    pieceValue: I18nString;
    /** Congratulations! You know the value of material! */
    pieceValueComplete: I18nString;
    /** Take the piece with the highest value! */
    pieceValueExchange: I18nString;
    /** Pieces with high mobility have a higher value! */
    pieceValueIntro: I18nString;
    /** Take the piece */
    pieceValueLegal: I18nString;
    /** Place the bishops! */
    placeTheBishops: I18nString;
    /** Place the king! */
    placeTheKing: I18nString;
    /** Place the queen! */
    placeTheQueen: I18nString;
    /** play! */
    play: I18nString;
    /** Play machine */
    playMachine: I18nString;
    /** Play people */
    playPeople: I18nString;
    /** Practise */
    practice: I18nString;
    /** Progress: %s */
    progressX: I18nString;
    /** Protection */
    protection: I18nString;
    /** Congratulations! A piece you don't lose is a piece you win! */
    protectionComplete: I18nString;
    /** Identify the pieces your opponent attacks, and defend them! */
    protectionIntro: I18nString;
    /** Puzzle failed! */
    puzzleFailed: I18nString;
    /** Puzzles */
    puzzles: I18nString;
    /** Queen = rook + bishop */
    queenCombinesRookAndBishop: I18nString;
    /** Congratulations! Queens have no secrets for you. */
    queenComplete: I18nString;
    /** The most powerful chess piece enters. Her majesty the queen! */
    queenIntro: I18nString;
    /** Take the piece */
    queenOverBishop: I18nString;
    /** Register */
    register: I18nString;
    /** Reset my progress */
    resetMyProgress: I18nString;
    /** Retry */
    retry: I18nString;
    /** Right on! */
    rightOn: I18nString;
    /** Congratulations! You have successfully mastered the rook. */
    rookComplete: I18nString;
    /** Click on the rook */
    rookGoal: I18nString;
    /** The rook is a powerful piece. Are you ready to command it? */
    rookIntro: I18nString;
    /** Select the piece you want! */
    selectThePieceYouWant: I18nString;
    /** Stage %s */
    stageX: I18nString;
    /** Stage %s complete */
    stageXComplete: I18nString;
    /** Stalemate */
    stalemate: I18nString;
    /** Congratulations! Better be stalemated than checkmated! */
    stalemateComplete: I18nString;
    /** To stalemate black: */
    stalemateGoal: I18nString;
    /** When a player is not in check and does not have a legal move, it's a stalemate. The game is drawn: no one wins, no one loses. */
    stalemateIntro: I18nString;
    /** Take all the pawns en passant! */
    takeAllThePawnsEnPassant: I18nString;
    /** Take the black pieces! */
    takeTheBlackPieces: I18nString;
    /** Take the black pieces! */
    takeTheBlackPiecesAndDontLoseYours: I18nString;
    /** Take the enemy pieces */
    takeTheEnemyPieces: I18nString;
    /** Take the piece */
    takeThePieceWithTheHighestValue: I18nString;
    /** Test your skills with the computer */
    testYourSkillsWithTheComputer: I18nString;
    /** The bishop */
    theBishop: I18nString;
    /** The fewer moves you make, */
    theFewerMoves: I18nString;
    /** The game is a draw */
    theGameIsADraw: I18nString;
    /** The king */
    theKing: I18nString;
    /** The king cannot escape, */
    theKingCannotEscapeButBlock: I18nString;
    /** The king is slow. */
    theKingIsSlow: I18nString;
    /** The knight */
    theKnight: I18nString;
    /** The knight is in the way! */
    theKnightIsInTheWay: I18nString;
    /** The most important piece */
    theMostImportantPiece: I18nString;
    /** Then place the knights! */
    thenPlaceTheKnights: I18nString;
    /** The pawn */
    thePawn: I18nString;
    /** The queen */
    theQueen: I18nString;
    /** The rook */
    theRook: I18nString;
    /** The special king move */
    theSpecialKingMove: I18nString;
    /** The special pawn move */
    theSpecialPawnMove: I18nString;
    /** This is the initial position */
    thisIsTheInitialPosition: I18nString;
    /** This knight is checking */
    thisKnightIsCheckingThroughYourDefenses: I18nString;
    /** Two moves to give a check */
    twoMovesToGiveCheck: I18nString;
    /** Use all the pawns! */
    useAllThePawns: I18nString;
    /** Use two rooks */
    useTwoRooks: I18nString;
    /** Videos */
    videos: I18nString;
    /** Watch instructive chess videos */
    watchInstructiveChessVideos: I18nString;
    /** Way to go! */
    wayToGo: I18nString;
    /** What next? */
    whatNext: I18nString;
    /** Yes, yes, yes! */
    yesYesYes: I18nString;
    /** You can get out of check */
    youCanGetOutOfCheckByTaking: I18nString;
    /** You cannot castle if */
    youCannotCastleIfAttacked: I18nString;
    /** You cannot castle if */
    youCannotCastleIfMoved: I18nString;
    /** You know how to play chess, congratulations! Do you want to become a stronger player? */
    youKnowHowToPlayChess: I18nString;
    /** One light-squared bishop, */
    youNeedBothBishops: I18nString;
    /** You're good at this! */
    youreGoodAtThis: I18nString;
    /** Your pawn reached the end of the board! */
    yourPawnReachedTheEndOfTheBoard: I18nString;
    /** You will lose all your progress! */
    youWillLoseAllYourProgress: I18nString;
  };
  oauthScope: {
    /** You already have played games! */
    alreadyHavePlayedGames: I18nString;
    /** API access tokens */
    apiAccessTokens: I18nString;
    /** API documentation */
    apiDocumentation: I18nString;
    /** Here's a %1$s and the %2$s. */
    apiDocumentationLinks: I18nString;
    /** Note for the attention of developers only: */
    attentionOfDevelopers: I18nString;
    /** authorization code flow */
    authorizationCodeFlow: I18nString;
    /** Play games with board API */
    boardPlay: I18nString;
    /** Play games with the bot API */
    botPlay: I18nString;
    /** You can make OAuth requests without going through the %s. */
    canMakeOauthRequests: I18nString;
    /** Carefully select what it is allowed to do on your behalf. */
    carefullySelect: I18nString;
    /** Create many games at once for other players */
    challengeBulk: I18nString;
    /** Read incoming challenges */
    challengeRead: I18nString;
    /** Send, accept and reject challenges */
    challengeWrite: I18nString;
    /** Make sure to copy your new personal access token now. You won’t be able to see it again! */
    copyTokenNow: I18nString;
    /** Created %s */
    created: I18nString;
    /** The token will grant access to your account. Do NOT share it with anyone! */
    doNotShareIt: I18nString;
    /** Read email address */
    emailRead: I18nString;
    /** View and use your external engines */
    engineRead: I18nString;
    /** Create and update external engines */
    engineWrite: I18nString;
    /** Read followed players */
    followRead: I18nString;
    /** Follow and unfollow other players */
    followWrite: I18nString;
    /** For example: %s */
    forExample: I18nString;
    /** generate a personal access token */
    generatePersonalToken: I18nString;
    /** Giving these pre-filled URLs to your users will help them get the right token scopes. */
    givingPrefilledUrls: I18nString;
    /** Guard these tokens carefully! They are like passwords. The advantage to using tokens over putting your password into a script is that tokens can be revoked, and you can generate lots of them. */
    guardTokensCarefully: I18nString;
    /** Instead, %s that you can directly use in API requests. */
    insteadGenerateToken: I18nString;
    /** Last used %s */
    lastUsed: I18nString;
    /** Send private messages to other players */
    msgWrite: I18nString;
    /** New personal API access token */
    newAccessToken: I18nString;
    /** New access token */
    newToken: I18nString;
    /** Personal API access tokens */
    personalAccessTokens: I18nString;
    /** personal token app example */
    personalTokenAppExample: I18nString;
    /** It is possible to pre-fill this form by tweaking the query parameters of the URL. */
    possibleToPrefill: I18nString;
    /** Read preferences */
    preferenceRead: I18nString;
    /** Write preference */
    preferenceWrite: I18nString;
    /** Read puzzle activity */
    puzzleRead: I18nString;
    /** Create and join puzzle races */
    racerWrite: I18nString;
    /** So you remember what this token is for */
    rememberTokenUse: I18nString;
    /** The scope codes can be found in the HTML code of the form. */
    scopesCanBeFound: I18nString;
    /** Read private studies and broadcasts */
    studyRead: I18nString;
    /** Create, update, delete studies and broadcasts */
    studyWrite: I18nString;
    /** Manage teams you lead: send PMs, kick members */
    teamLead: I18nString;
    /** Read private team information */
    teamRead: I18nString;
    /** Join and leave teams */
    teamWrite: I18nString;
    /** ticks the %1$s and %2$s scopes, and sets the token description. */
    ticksTheScopes: I18nString;
    /** Token description */
    tokenDescription: I18nString;
    /** A token grants other people permission to use your account. */
    tokenGrantsPermission: I18nString;
    /** Create, update, and join tournaments */
    tournamentWrite: I18nString;
    /** Create authenticated website sessions (grants full access!) */
    webLogin: I18nString;
    /** Use moderator tools (within bounds of your permission) */
    webMod: I18nString;
    /** What the token can do on your behalf: */
    whatTheTokenCanDo: I18nString;
  };
  onboarding: {
    /** Configure Lichess to your liking. */
    configureLichess: I18nString;
    /** Will a child use this account? You might want to enable %s. */
    enabledKidModeSuggestion: I18nString;
    /** Explore the site and have fun :) */
    exploreTheSiteAndHaveFun: I18nString;
    /** Follow your friends on Lichess. */
    followYourFriendsOnLichess: I18nString;
    /** Improve with chess tactics puzzles. */
    improveWithChessTacticsPuzzles: I18nString;
    /** Learn chess rules */
    learnChessRules: I18nString;
    /** Learn from %1$s and %2$s. */
    learnFromXAndY: I18nString;
    /** Play in tournaments. */
    playInTournaments: I18nString;
    /** Play opponents from around the world. */
    playOpponentsFromAroundTheWorld: I18nString;
    /** Play the Artificial Intelligence. */
    playTheArtificialIntelligence: I18nString;
    /** This is your profile page. */
    thisIsYourProfilePage: I18nString;
    /** Welcome! */
    welcome: I18nString;
    /** Welcome to lichess.org! */
    welcomeToLichess: I18nString;
    /** What now? Here are a few suggestions: */
    whatNowSuggestions: I18nString;
  };
  patron: {
    /** Yes, here's the act of creation (in French) */
    actOfCreation: I18nString;
    /** Amount */
    amount: I18nString;
    /** We also accept bank transfers */
    bankTransfers: I18nString;
    /** Become a Lichess Patron */
    becomePatron: I18nString;
    /** Cancel your support */
    cancelSupport: I18nString;
    /** The celebrated Patrons who make Lichess possible */
    celebratedPatrons: I18nString;
    /** Change currency */
    changeCurrency: I18nString;
    /** Change the monthly amount (%s) */
    changeMonthlyAmount: I18nString;
    /** Can I change/cancel my monthly support? */
    changeMonthlySupport: I18nString;
    /** Yes, at any time, from this page. */
    changeOrContact: I18nString;
    /** Check out your profile page! */
    checkOutProfile: I18nString;
    /** contact Lichess support */
    contactSupport: I18nString;
    /** See the detailed cost breakdown */
    costBreakdown: I18nString;
    /** Current status */
    currentStatus: I18nString;
    /** Date */
    date: I18nString;
    /** Decide what Lichess is worth to you: */
    decideHowMuch: I18nString;
    /** Donate */
    donate: I18nString;
    /** Donate as %s */
    donateAsX: I18nString;
    /** In one month, you will NOT be charged again, and your Lichess account will revert to a regular account. */
    downgradeNextMonth: I18nString;
    /** See the detailed feature comparison */
    featuresComparison: I18nString;
    /** Free account */
    freeAccount: I18nString;
    /** Free chess for everyone, forever! */
    freeChess: I18nString;
    /** Gift Patron wings to a player */
    giftPatronWings: I18nString;
    /** Gift Patron wings */
    giftPatronWingsShort: I18nString;
    /** If not renewed, your account will then revert to a regular account. */
    ifNotRenewedThenAccountWillRevert: I18nString;
    /** Lichess is registered with %s. */
    lichessIsRegisteredWith: I18nString;
    /** Lichess Patron */
    lichessPatron: I18nString;
    /** Lifetime */
    lifetime: I18nString;
    /** Lifetime Lichess Patron */
    lifetimePatron: I18nString;
    /** Log in to donate */
    logInToDonate: I18nString;
    /** Make an additional donation */
    makeAdditionalDonation: I18nString;
    /** Monthly */
    monthly: I18nString;
    /** New Patrons */
    newPatrons: I18nString;
    /** Next payment */
    nextPayment: I18nString;
    /** No ads, no subscriptions; but open-source and passion. */
    noAdsNoSubs: I18nString;
    /** No longer support Lichess */
    noLongerSupport: I18nString;
    /** No, because Lichess is entirely free, forever, and for everyone. That's a promise. */
    noPatronFeatures: I18nString;
    /** You are now a lifetime Lichess Patron! */
    nowLifetime: I18nString;
    /** You are now a Lichess Patron for one month! */
    nowOneMonth: I18nString;
    /** Is Lichess an official non-profit? */
    officialNonProfit: I18nString;
    /** One-time */
    onetime: I18nString;
    /** Please note that only the donation form above will grant the Patron status. */
    onlyDonationFromAbove: I18nString;
    /** Other */
    otherAmount: I18nString;
    /** Other methods of donation? */
    otherMethods: I18nString;
    /** Are some features reserved to Patrons? */
    patronFeatures: I18nString;
    /** Lichess Patron for %s months */
    patronForMonths: I18nPlural;
    /** You have a Patron account until %s. */
    patronUntil: I18nString;
    /** Pay %s once. Be a Lichess Patron forever! */
    payLifetimeOnce: I18nString;
    /** Payment details */
    paymentDetails: I18nString;
    /** You now have a permanent Patron account. */
    permanentPatron: I18nString;
    /** Please enter an amount in %s */
    pleaseEnterAmountInX: I18nString;
    /** Recurring billing, renewing your Patron wings every month. */
    recurringBilling: I18nString;
    /** First of all, powerful servers. */
    serversAndDeveloper: I18nString;
    /** A single donation that grants you the Patron wings for one month. */
    singleDonation: I18nString;
    /** Withdraw your credit card and stop payments: */
    stopPayments: I18nString;
    /** Cancel PayPal subscription and stop payments: */
    stopPaymentsPayPal: I18nString;
    /** Manage your subscription and download your invoices and receipts */
    stripeManageSub: I18nString;
    /** Thank you for your donation! */
    thankYou: I18nString;
    /** Your transaction has been completed, and a receipt for your donation has been emailed to you. */
    transactionCompleted: I18nString;
    /** Thank you very much for your help. You rock! */
    tyvm: I18nString;
    /** Update */
    update: I18nString;
    /** Update payment method */
    updatePaymentMethod: I18nString;
    /** View other Lichess Patrons */
    viewOthers: I18nString;
    /** We are a non‑profit association because we believe everyone should have access to a free, world-class chess platform. */
    weAreNonProfit: I18nString;
    /** We are a small team, so your support makes a huge difference! */
    weAreSmallTeam: I18nString;
    /** We rely on support from people like you to make it possible. If you enjoy using Lichess, please consider supporting us by donating and becoming a Patron! */
    weRelyOnSupport: I18nString;
    /** Where does the money go? */
    whereMoneyGoes: I18nString;
    /** Credit Card */
    withCreditCard: I18nString;
    /** %s became a Lichess Patron */
    xBecamePatron: I18nString;
    /** %1$s is a Lichess Patron for %2$s months */
    xIsPatronForNbMonths: I18nPlural;
    /** %1$s or %2$s */
    xOrY: I18nString;
    /** You have a Lifetime Patron account. That's pretty awesome! */
    youHaveLifetime: I18nString;
    /** You support lichess.org with %s per month. */
    youSupportWith: I18nString;
    /** You will be charged %1$s on %2$s. */
    youWillBeChargedXOnY: I18nString;
  };
  perfStat: {
    /** Average opponent */
    averageOpponent: I18nString;
    /** Berserked games */
    berserkedGames: I18nString;
    /** Best rated victories */
    bestRated: I18nString;
    /** Current streak: %s */
    currentStreak: I18nString;
    /** Defeats */
    defeats: I18nString;
    /** Disconnections */
    disconnections: I18nString;
    /** from %1$s to %2$s */
    fromXToY: I18nString;
    /** Games played in a row */
    gamesInARow: I18nString;
    /** Highest rating: %s */
    highestRating: I18nString;
    /** Less than one hour between games */
    lessThanOneHour: I18nString;
    /** Longest streak: %s */
    longestStreak: I18nString;
    /** Losing streak */
    losingStreak: I18nString;
    /** Lowest rating: %s */
    lowestRating: I18nString;
    /** Max time spent playing */
    maxTimePlaying: I18nString;
    /** Not enough games played */
    notEnoughGames: I18nString;
    /** Not enough rated games have been played to establish a reliable rating. */
    notEnoughRatedGames: I18nString;
    /** now */
    now: I18nString;
    /** %s stats */
    perfStats: I18nString;
    /** Progression over the last %s games: */
    progressOverLastXGames: I18nString;
    /** provisional */
    provisional: I18nString;
    /** Rated games */
    ratedGames: I18nString;
    /** Rating deviation: %s. */
    ratingDeviation: I18nString;
    /** Lower value means the rating is more stable. Above %1$s, the rating is considered provisional. To be included in the rankings, this value should be below %2$s (standard chess) or %3$s (variants). */
    ratingDeviationTooltip: I18nString;
    /** Time spent playing */
    timeSpentPlaying: I18nString;
    /** Total games */
    totalGames: I18nString;
    /** Tournament games */
    tournamentGames: I18nString;
    /** Victories */
    victories: I18nString;
    /** View the games */
    viewTheGames: I18nString;
    /** Winning streak */
    winningStreak: I18nString;
  };
  preferences: {
    /** Bell notification sound */
    bellNotificationSound: I18nString;
    /** Board coordinates (A-H, 1-8) */
    boardCoordinates: I18nString;
    /** Board highlights (last move and check) */
    boardHighlights: I18nString;
    /** Either */
    bothClicksAndDrag: I18nString;
    /** Move king onto rook */
    castleByMovingOntoTheRook: I18nString;
    /** Castling method */
    castleByMovingTheKingTwoSquaresOrOntoTheRook: I18nString;
    /** Move king two squares */
    castleByMovingTwoSquares: I18nString;
    /** Chess clock */
    chessClock: I18nString;
    /** Chess piece symbol */
    chessPieceSymbol: I18nString;
    /** Claim draw on threefold repetition automatically */
    claimDrawOnThreefoldRepetitionAutomatically: I18nString;
    /** Click two squares */
    clickTwoSquares: I18nString;
    /** Confirm resignation and draw offers */
    confirmResignationAndDrawOffers: I18nString;
    /** Correspondence and unlimited */
    correspondenceAndUnlimited: I18nString;
    /** Daily email listing your correspondence games */
    correspondenceEmailNotification: I18nString;
    /** Display */
    display: I18nString;
    /** Show board resize handle */
    displayBoardResizeHandle: I18nString;
    /** Drag a piece */
    dragPiece: I18nString;
    /** Can be disabled during a game with the board menu */
    explainCanThenBeTemporarilyDisabled: I18nString;
    /** Hold the <ctrl> key while promoting to temporarily disable auto-promotion */
    explainPromoteToQueenAutomatically: I18nString;
    /** This hides all ratings from Lichess, to help focus on the chess. Rated games still impact your rating, this is only about what you get to see. */
    explainShowPlayerRatings: I18nString;
    /** Game behaviour */
    gameBehavior: I18nString;
    /** Give more time */
    giveMoreTime: I18nString;
    /** Horizontal green progress bars */
    horizontalGreenProgressBars: I18nString;
    /** How do you move pieces? */
    howDoYouMovePieces: I18nString;
    /** In casual games only */
    inCasualGamesOnly: I18nString;
    /** Correspondence games */
    inCorrespondenceGames: I18nString;
    /** In-game only */
    inGameOnly: I18nString;
    /** Input moves with the keyboard */
    inputMovesWithTheKeyboard: I18nString;
    /** Input moves with your voice */
    inputMovesWithVoice: I18nString;
    /** Material difference */
    materialDifference: I18nString;
    /** Move confirmation */
    moveConfirmation: I18nString;
    /** Move list while playing */
    moveListWhilePlaying: I18nString;
    /** Notifications */
    notifications: I18nString;
    /** Bell notification within Lichess */
    notifyBell: I18nString;
    /** Challenges */
    notifyChallenge: I18nString;
    /** Device */
    notifyDevice: I18nString;
    /** Forum comment mentions you */
    notifyForumMention: I18nString;
    /** Correspondence game updates */
    notifyGameEvent: I18nString;
    /** New inbox message */
    notifyInboxMsg: I18nString;
    /** Study invite */
    notifyInvitedStudy: I18nString;
    /** Device notification when you're not on Lichess */
    notifyPush: I18nString;
    /** Streamer goes live */
    notifyStreamStart: I18nString;
    /** Correspondence clock running out */
    notifyTimeAlarm: I18nString;
    /** Tournament starting soon */
    notifyTournamentSoon: I18nString;
    /** Browser */
    notifyWeb: I18nString;
    /** Only on initial position */
    onlyOnInitialPosition: I18nString;
    /** Letter (K, Q, R, B, N) */
    pgnLetter: I18nString;
    /** Move notation */
    pgnPieceNotation: I18nString;
    /** Piece animation */
    pieceAnimation: I18nString;
    /** Piece destinations (valid moves and premoves) */
    pieceDestinations: I18nString;
    /** Preferences */
    preferences: I18nString;
    /** Premoves (playing during opponent turn) */
    premovesPlayingDuringOpponentTurn: I18nString;
    /** Privacy */
    privacy: I18nString;
    /** Promote to Queen automatically */
    promoteToQueenAutomatically: I18nString;
    /** Say "Good game, well played" upon defeat or draw */
    sayGgWpAfterLosingOrDrawing: I18nString;
    /** Scroll on the board to replay moves */
    scrollOnTheBoardToReplayMoves: I18nString;
    /** Show player flairs */
    showFlairs: I18nString;
    /** Show player ratings */
    showPlayerRatings: I18nString;
    /** Snap arrows to valid moves */
    snapArrowsToValidMoves: I18nString;
    /** Sound when time gets critical */
    soundWhenTimeGetsCritical: I18nString;
    /** Takebacks (with opponent approval) */
    takebacksWithOpponentApproval: I18nString;
    /** Tenths of seconds */
    tenthsOfSeconds: I18nString;
    /** When premoving */
    whenPremoving: I18nString;
    /** When time remaining < 10 seconds */
    whenTimeRemainingLessThanTenSeconds: I18nString;
    /** When time remaining < 30 seconds */
    whenTimeRemainingLessThanThirtySeconds: I18nString;
    /** Your preferences have been saved. */
    yourPreferencesHaveBeenSaved: I18nString;
    /** Zen mode */
    zenMode: I18nString;
  };
  puzzle: {
    /** Add another theme */
    addAnotherTheme: I18nString;
    /** Advanced */
    advanced: I18nString;
    /** Best move! */
    bestMove: I18nString;
    /** By openings */
    byOpenings: I18nString;
    /** Click to solve */
    clickToSolve: I18nString;
    /** Continue the streak */
    continueTheStreak: I18nString;
    /** Continue training */
    continueTraining: I18nString;
    /** Daily Puzzle */
    dailyPuzzle: I18nString;
    /** Did you like this puzzle? */
    didYouLikeThisPuzzle: I18nString;
    /** Difficulty level */
    difficultyLevel: I18nString;
    /** Down vote puzzle */
    downVote: I18nString;
    /** Easier */
    easier: I18nString;
    /** Easiest */
    easiest: I18nString;
    /** Example */
    example: I18nString;
    /** incorrect */
    failed: I18nString;
    /** Find the best move for black. */
    findTheBestMoveForBlack: I18nString;
    /** Find the best move for white. */
    findTheBestMoveForWhite: I18nString;
    /** From game %s */
    fromGameLink: I18nString;
    /** From my games */
    fromMyGames: I18nString;
    /** You have no puzzles in the database, but Lichess still loves you very much. */
    fromMyGamesNone: I18nString;
    /** Puzzles from %s' games */
    fromXGames: I18nString;
    /** %1$s puzzles found in %2$s games */
    fromXGamesFound: I18nString;
    /** Goals */
    goals: I18nString;
    /** Good move */
    goodMove: I18nString;
    /** Harder */
    harder: I18nString;
    /** Hardest */
    hardest: I18nString;
    /** hidden */
    hidden: I18nString;
    /** Puzzle history */
    history: I18nString;
    /** Improvement areas */
    improvementAreas: I18nString;
    /** Train these to optimize your progress! */
    improvementAreasDescription: I18nString;
    /** Jump to next puzzle immediately */
    jumpToNextPuzzleImmediately: I18nString;
    /** Keep going… */
    keepGoing: I18nString;
    /** Lengths */
    lengths: I18nString;
    /** Lookup puzzles from a player's games */
    lookupOfPlayer: I18nString;
    /** Mates */
    mates: I18nString;
    /** Motifs */
    motifs: I18nString;
    /** %s played */
    nbPlayed: I18nPlural;
    /** %s points above your puzzle rating */
    nbPointsAboveYourPuzzleRating: I18nPlural;
    /** %s points below your puzzle rating */
    nbPointsBelowYourPuzzleRating: I18nPlural;
    /** %s to replay */
    nbToReplay: I18nPlural;
    /** New streak */
    newStreak: I18nString;
    /** Next puzzle */
    nextPuzzle: I18nString;
    /** Nothing to show, go play some puzzles first! */
    noPuzzlesToShow: I18nString;
    /** Normal */
    normal: I18nString;
    /** That's not the move! */
    notTheMove: I18nString;
    /** Openings you played the most in rated games */
    openingsYouPlayedTheMost: I18nString;
    /** Origin */
    origin: I18nString;
    /** %s solved */
    percentSolved: I18nString;
    /** Phases */
    phases: I18nString;
    /** Played %s times */
    playedXTimes: I18nPlural;
    /** Puzzle complete! */
    puzzleComplete: I18nString;
    /** Puzzle Dashboard */
    puzzleDashboard: I18nString;
    /** Train, analyse, improve */
    puzzleDashboardDescription: I18nString;
    /** Puzzle %s */
    puzzleId: I18nString;
    /** Puzzle of the day */
    puzzleOfTheDay: I18nString;
    /** Puzzles */
    puzzles: I18nString;
    /** Puzzles by openings */
    puzzlesByOpenings: I18nString;
    /** Success! */
    puzzleSuccess: I18nString;
    /** Puzzle Themes */
    puzzleThemes: I18nString;
    /** Rating: %s */
    ratingX: I18nString;
    /** Recommended */
    recommended: I18nString;
    /** Search puzzles */
    searchPuzzles: I18nString;
    /** solved */
    solved: I18nString;
    /** Special moves */
    specialMoves: I18nString;
    /** Solve progressively harder puzzles and build a win streak. There is no clock, so take your time. One wrong move, and it's game over! But you can skip one move per session. */
    streakDescription: I18nString;
    /** Skip this move to preserve your streak! Only works once per run. */
    streakSkipExplanation: I18nString;
    /** You perform the best in these themes */
    strengthDescription: I18nString;
    /** Strengths */
    strengths: I18nString;
    /** To get personalized puzzles: */
    toGetPersonalizedPuzzles: I18nString;
    /** Try something else. */
    trySomethingElse: I18nString;
    /** Up vote puzzle */
    upVote: I18nString;
    /** Use Ctrl+f to find your favourite opening! */
    useCtrlF: I18nString;
    /** Use "Find in page" in the browser menu to find your favourite opening! */
    useFindInPage: I18nString;
    /** Vote to load the next one! */
    voteToLoadNextOne: I18nString;
    /** Your puzzle rating will not change. Note that puzzles are not a competition. Your rating helps selecting the best puzzles for your current skill. */
    yourPuzzleRatingWillNotChange: I18nString;
    /** Your streak: %s */
    yourStreakX: I18nString;
  };
  puzzleTheme: {
    /** Advanced pawn */
    advancedPawn: I18nString;
    /** One of your pawns is deep into the opponent position, maybe threatening to promote. */
    advancedPawnDescription: I18nString;
    /** Advantage */
    advantage: I18nString;
    /** Seize your chance to get a decisive advantage. (200cp ≤ eval ≤ 600cp) */
    advantageDescription: I18nString;
    /** Anastasia's mate */
    anastasiaMate: I18nString;
    /** A knight and rook or queen team up to trap the opposing king between the side of the board and a friendly piece. */
    anastasiaMateDescription: I18nString;
    /** Arabian mate */
    arabianMate: I18nString;
    /** A knight and a rook team up to trap the opposing king on a corner of the board. */
    arabianMateDescription: I18nString;
    /** Attacking f2 or f7 */
    attackingF2F7: I18nString;
    /** An attack focusing on the f2 or f7 pawn, such as in the fried liver opening. */
    attackingF2F7Description: I18nString;
    /** Attraction */
    attraction: I18nString;
    /** An exchange or sacrifice encouraging or forcing an opponent piece to a square that allows a follow-up tactic. */
    attractionDescription: I18nString;
    /** Back rank mate */
    backRankMate: I18nString;
    /** Checkmate the king on the home rank, when it is trapped there by its own pieces. */
    backRankMateDescription: I18nString;
    /** Bishop endgame */
    bishopEndgame: I18nString;
    /** An endgame with only bishops and pawns. */
    bishopEndgameDescription: I18nString;
    /** Boden's mate */
    bodenMate: I18nString;
    /** Two attacking bishops on criss-crossing diagonals deliver mate to a king obstructed by friendly pieces. */
    bodenMateDescription: I18nString;
    /** Capture the defender */
    capturingDefender: I18nString;
    /** Removing a piece that is critical to defence of another piece, allowing the now undefended piece to be captured on a following move. */
    capturingDefenderDescription: I18nString;
    /** Castling */
    castling: I18nString;
    /** Bring the king to safety, and deploy the rook for attack. */
    castlingDescription: I18nString;
    /** Clearance */
    clearance: I18nString;
    /** A move, often with tempo, that clears a square, file or diagonal for a follow-up tactical idea. */
    clearanceDescription: I18nString;
    /** Crushing */
    crushing: I18nString;
    /** Spot the opponent blunder to obtain a crushing advantage. (eval ≥ 600cp) */
    crushingDescription: I18nString;
    /** Defensive move */
    defensiveMove: I18nString;
    /** A precise move or sequence of moves that is needed to avoid losing material or another advantage. */
    defensiveMoveDescription: I18nString;
    /** Deflection */
    deflection: I18nString;
    /** A move that distracts an opponent piece from another duty that it performs, such as guarding a key square. Sometimes also called "overloading". */
    deflectionDescription: I18nString;
    /** Discovered attack */
    discoveredAttack: I18nString;
    /** Moving a piece (such as a knight), that previously blocked an attack by a long range piece (such as a rook), out of the way of that piece. */
    discoveredAttackDescription: I18nString;
    /** Double bishop mate */
    doubleBishopMate: I18nString;
    /** Two attacking bishops on adjacent diagonals deliver mate to a king obstructed by friendly pieces. */
    doubleBishopMateDescription: I18nString;
    /** Double check */
    doubleCheck: I18nString;
    /** Checking with two pieces at once, as a result of a discovered attack where both the moving piece and the unveiled piece attack the opponent's king. */
    doubleCheckDescription: I18nString;
    /** Dovetail mate */
    dovetailMate: I18nString;
    /** A queen delivers mate to an adjacent king, whose only two escape squares are obstructed by friendly pieces. */
    dovetailMateDescription: I18nString;
    /** Endgame */
    endgame: I18nString;
    /** A tactic during the last phase of the game. */
    endgameDescription: I18nString;
    /** A tactic involving the en passant rule, where a pawn can capture an opponent pawn that has bypassed it using its initial two-square move. */
    enPassantDescription: I18nString;
    /** Equality */
    equality: I18nString;
    /** Come back from a losing position, and secure a draw or a balanced position. (eval ≤ 200cp) */
    equalityDescription: I18nString;
    /** Exposed king */
    exposedKing: I18nString;
    /** A tactic involving a king with few defenders around it, often leading to checkmate. */
    exposedKingDescription: I18nString;
    /** Fork */
    fork: I18nString;
    /** A move where the moved piece attacks two opponent pieces at once. */
    forkDescription: I18nString;
    /** Hanging piece */
    hangingPiece: I18nString;
    /** A tactic involving an opponent piece being undefended or insufficiently defended and free to capture. */
    hangingPieceDescription: I18nString;
    /** Hook mate */
    hookMate: I18nString;
    /** Checkmate with a rook, knight, and pawn along with one enemy pawn to limit the enemy king's escape. */
    hookMateDescription: I18nString;
    /** Interference */
    interference: I18nString;
    /** Moving a piece between two opponent pieces to leave one or both opponent pieces undefended, such as a knight on a defended square between two rooks. */
    interferenceDescription: I18nString;
    /** Intermezzo */
    intermezzo: I18nString;
    /** Instead of playing the expected move, first interpose another move posing an immediate threat that the opponent must answer. Also known as "Zwischenzug" or "In between". */
    intermezzoDescription: I18nString;
    /** Kingside attack */
    kingsideAttack: I18nString;
    /** An attack of the opponent's king, after they castled on the king side. */
    kingsideAttackDescription: I18nString;
    /** Knight endgame */
    knightEndgame: I18nString;
    /** An endgame with only knights and pawns. */
    knightEndgameDescription: I18nString;
    /** Long puzzle */
    long: I18nString;
    /** Three moves to win. */
    longDescription: I18nString;
    /** Master games */
    master: I18nString;
    /** Puzzles from games played by titled players. */
    masterDescription: I18nString;
    /** Master vs Master games */
    masterVsMaster: I18nString;
    /** Puzzles from games between two titled players. */
    masterVsMasterDescription: I18nString;
    /** Checkmate */
    mate: I18nString;
    /** Win the game with style. */
    mateDescription: I18nString;
    /** Mate in 1 */
    mateIn1: I18nString;
    /** Deliver checkmate in one move. */
    mateIn1Description: I18nString;
    /** Mate in 2 */
    mateIn2: I18nString;
    /** Deliver checkmate in two moves. */
    mateIn2Description: I18nString;
    /** Mate in 3 */
    mateIn3: I18nString;
    /** Deliver checkmate in three moves. */
    mateIn3Description: I18nString;
    /** Mate in 4 */
    mateIn4: I18nString;
    /** Deliver checkmate in four moves. */
    mateIn4Description: I18nString;
    /** Mate in 5 or more */
    mateIn5: I18nString;
    /** Figure out a long mating sequence. */
    mateIn5Description: I18nString;
    /** Middlegame */
    middlegame: I18nString;
    /** A tactic during the second phase of the game. */
    middlegameDescription: I18nString;
    /** Healthy mix */
    mix: I18nString;
    /** A bit of everything. You don't know what to expect, so you remain ready for anything! Just like in real games. */
    mixDescription: I18nString;
    /** One-move puzzle */
    oneMove: I18nString;
    /** A puzzle that is only one move long. */
    oneMoveDescription: I18nString;
    /** Opening */
    opening: I18nString;
    /** A tactic during the first phase of the game. */
    openingDescription: I18nString;
    /** Pawn endgame */
    pawnEndgame: I18nString;
    /** An endgame with only pawns. */
    pawnEndgameDescription: I18nString;
    /** Pin */
    pin: I18nString;
    /** A tactic involving pins, where a piece is unable to move without revealing an attack on a higher value piece. */
    pinDescription: I18nString;
    /** Player games */
    playerGames: I18nString;
    /** Lookup puzzles generated from your games, or from another player's games. */
    playerGamesDescription: I18nString;
    /** Promotion */
    promotion: I18nString;
    /** Promote one of your pawn to a queen or minor piece. */
    promotionDescription: I18nString;
    /** These puzzles are in the public domain, and can be downloaded from %s. */
    puzzleDownloadInformation: I18nString;
    /** Queen endgame */
    queenEndgame: I18nString;
    /** An endgame with only queens and pawns. */
    queenEndgameDescription: I18nString;
    /** Queen and Rook */
    queenRookEndgame: I18nString;
    /** An endgame with only queens, rooks and pawns. */
    queenRookEndgameDescription: I18nString;
    /** Queenside attack */
    queensideAttack: I18nString;
    /** An attack of the opponent's king, after they castled on the queen side. */
    queensideAttackDescription: I18nString;
    /** Quiet move */
    quietMove: I18nString;
    /** A move that does neither make a check or capture, nor an immediate threat to capture, but does prepare a more hidden unavoidable threat for a later move. */
    quietMoveDescription: I18nString;
    /** Rook endgame */
    rookEndgame: I18nString;
    /** An endgame with only rooks and pawns. */
    rookEndgameDescription: I18nString;
    /** Sacrifice */
    sacrifice: I18nString;
    /** A tactic involving giving up material in the short-term, to gain an advantage again after a forced sequence of moves. */
    sacrificeDescription: I18nString;
    /** Short puzzle */
    short: I18nString;
    /** Two moves to win. */
    shortDescription: I18nString;
    /** Skewer */
    skewer: I18nString;
    /** A motif involving a high value piece being attacked, moving out the way, and allowing a lower value piece behind it to be captured or attacked, the inverse of a pin. */
    skewerDescription: I18nString;
    /** Smothered mate */
    smotheredMate: I18nString;
    /** A checkmate delivered by a knight in which the mated king is unable to move because it is surrounded (or smothered) by its own pieces. */
    smotheredMateDescription: I18nString;
    /** Super GM games */
    superGM: I18nString;
    /** Puzzles from games played by the best players in the world. */
    superGMDescription: I18nString;
    /** Trapped piece */
    trappedPiece: I18nString;
    /** A piece is unable to escape capture as it has limited moves. */
    trappedPieceDescription: I18nString;
    /** Underpromotion */
    underPromotion: I18nString;
    /** Promotion to a knight, bishop, or rook. */
    underPromotionDescription: I18nString;
    /** Very long puzzle */
    veryLong: I18nString;
    /** Four moves or more to win. */
    veryLongDescription: I18nString;
    /** X-Ray attack */
    xRayAttack: I18nString;
    /** A piece attacks or defends a square, through an enemy piece. */
    xRayAttackDescription: I18nString;
    /** Zugzwang */
    zugzwang: I18nString;
    /** The opponent is limited in the moves they can make, and all moves worsen their position. */
    zugzwangDescription: I18nString;
  };
  search: {
    /** Advanced search */
    advancedSearch: I18nString;
    /** A.I. level */
    aiLevel: I18nString;
    /** Analysis */
    analysis: I18nString;
    /** Ascending */
    ascending: I18nString;
    /** Colour */
    color: I18nString;
    /** Date */
    date: I18nString;
    /** Descending */
    descending: I18nString;
    /** Evaluation */
    evaluation: I18nString;
    /** From */
    from: I18nString;
    /** %s games found */
    gamesFound: I18nPlural;
    /** Whether the player's opponent was human or a computer */
    humanOrComputer: I18nString;
    /** Include */
    include: I18nString;
    /** Loser */
    loser: I18nString;
    /** Maximum number */
    maxNumber: I18nString;
    /** The maximum number of games to return */
    maxNumberExplanation: I18nString;
    /** Number of turns */
    nbTurns: I18nString;
    /** Only games where a computer analysis is available */
    onlyAnalysed: I18nString;
    /** Opponent name */
    opponentName: I18nString;
    /** The average rating of both players */
    ratingExplanation: I18nString;
    /** Result */
    result: I18nString;
    /** Search */
    search: I18nString;
    /** Search in %s chess games */
    searchInXGames: I18nPlural;
    /** Sort by */
    sortBy: I18nString;
    /** Source */
    source: I18nString;
    /** To */
    to: I18nString;
    /** Winner colour */
    winnerColor: I18nString;
    /** %s games found */
    xGamesFound: I18nPlural;
  };
  settings: {
    /** You will not be allowed to open a new account with the same name, even if the case is different. */
    cantOpenSimilarAccount: I18nString;
    /** I changed my mind, don't close my account */
    changedMindDoNotCloseAccount: I18nString;
    /** Close account */
    closeAccount: I18nString;
    /** Are you sure you want to close your account? Closing your account is a permanent decision. You will NEVER be able to log in EVER AGAIN. */
    closeAccountExplanation: I18nString;
    /** Closing is definitive. There is no going back. Are you sure? */
    closingIsDefinitive: I18nString;
    /** Your account is managed, and cannot be closed. */
    managedAccountCannotBeClosed: I18nString;
    /** Settings */
    settings: I18nString;
    /** This account is closed. */
    thisAccountIsClosed: I18nString;
  };
  site: {
    /** Abort game */
    abortGame: I18nString;
    /** Abort the game */
    abortTheGame: I18nString;
    /** About */
    about: I18nString;
    /** Simuls involve a single player facing several players at once. */
    aboutSimul: I18nString;
    /** Out of 50 opponents, Fischer won 47 games, drew 2 and lost 1. */
    aboutSimulImage: I18nString;
    /** The concept is taken from real world events. In real life, this involves the simul host moving from table to table to play a single move. */
    aboutSimulRealLife: I18nString;
    /** When the simul starts, every player starts a game with the host. The simul ends when all games are complete. */
    aboutSimulRules: I18nString;
    /** Simuls are always casual. Rematches, takebacks and adding time are disabled. */
    aboutSimulSettings: I18nString;
    /** About %s */
    aboutX: I18nString;
    /** Accept */
    accept: I18nString;
    /** You can login right now as %s. */
    accountCanLogin: I18nString;
    /** The account %s is closed. */
    accountClosed: I18nString;
    /** You do not need a confirmation email. */
    accountConfirmationEmailNotNeeded: I18nString;
    /** The user %s is successfully confirmed. */
    accountConfirmed: I18nString;
    /** The account %s was registered without an email. */
    accountRegisteredWithoutEmail: I18nString;
    /** Accuracy */
    accuracy: I18nString;
    /** Active players */
    activePlayers: I18nString;
    /** Add current variation */
    addCurrentVariation: I18nString;
    /** Advanced settings */
    advancedSettings: I18nString;
    /** Advantage */
    advantage: I18nString;
    /** I agree that I will at no time receive assistance during my games (from a chess computer, book, database or another person). */
    agreementAssistance: I18nString;
    /** I agree that I will not create multiple accounts (except for the reasons stated in the %s). */
    agreementMultipleAccounts: I18nString;
    /** I agree that I will always be respectful to other players. */
    agreementNice: I18nString;
    /** I agree that I will follow all Lichess policies. */
    agreementPolicy: I18nString;
    /** %1$s level %2$s */
    aiNameLevelAiLevel: I18nString;
    /** All information is public and optional. */
    allInformationIsPublicAndOptional: I18nString;
    /** All set! */
    allSet: I18nString;
    /** All squares of the board */
    allSquaresOfTheBoard: I18nString;
    /** Always */
    always: I18nString;
    /** Analysis board */
    analysis: I18nString;
    /** Analysis options */
    analysisOptions: I18nString;
    /** Press shift+click or right-click to draw circles and arrows on the board. */
    analysisShapesHowTo: I18nString;
    /** and save %s premove lines */
    andSaveNbPremoveLines: I18nPlural;
    /** Anonymous */
    anonymous: I18nString;
    /** Another was %s */
    anotherWasX: I18nString;
    /** Submit */
    apply: I18nString;
    /** as black */
    asBlack: I18nString;
    /** As free as Lichess */
    asFreeAsLichess: I18nString;
    /** Your account is managed. Ask your chess teacher about lifting kid mode. */
    askYourChessTeacherAboutLiftingKidMode: I18nString;
    /** as white */
    asWhite: I18nString;
    /** Automatically proceed to next game after moving */
    automaticallyProceedToNextGameAfterMoving: I18nString;
    /** Auto switch */
    autoSwitch: I18nString;
    /** Available in %s languages! */
    availableInNbLanguages: I18nPlural;
    /** Average centipawn loss */
    averageCentipawnLoss: I18nString;
    /** Average rating */
    averageElo: I18nString;
    /** Average opponent */
    averageOpponent: I18nString;
    /** Average rating: %s */
    averageRatingX: I18nString;
    /** Background */
    background: I18nString;
    /** Background image URL: */
    backgroundImageUrl: I18nString;
    /** Back to game */
    backToGame: I18nString;
    /** Back to tournament */
    backToTournament: I18nString;
    /** Berserk rate */
    berserkRate: I18nString;
    /** Best move arrow */
    bestMoveArrow: I18nString;
    /** Best was %s */
    bestWasX: I18nString;
    /** Better than %1$s of %2$s players */
    betterThanPercentPlayers: I18nString;
    /** Beware, the game is rated but has no clock! */
    bewareTheGameIsRatedButHasNoClock: I18nString;
    /** Biography */
    biography: I18nString;
    /** Talk about yourself, your interests, what you like in chess, your favourite openings, players, ... */
    biographyDescription: I18nString;
    /** Black */
    black: I18nString;
    /** Black O-O */
    blackCastlingKingside: I18nString;
    /** Black to checkmate in one move */
    blackCheckmatesInOneMove: I18nString;
    /** Black declines draw */
    blackDeclinesDraw: I18nString;
    /** Black didn't move */
    blackDidntMove: I18nString;
    /** Black is victorious */
    blackIsVictorious: I18nString;
    /** Black left the game */
    blackLeftTheGame: I18nString;
    /** Black offers draw */
    blackOffersDraw: I18nString;
    /** Black to play */
    blackPlays: I18nString;
    /** Black resigned */
    blackResigned: I18nString;
    /** Black time out */
    blackTimeOut: I18nString;
    /** Black wins */
    blackWins: I18nString;
    /** Black wins */
    blackWinsGame: I18nString;
    /** You have used the same password on another site, and that site has been compromised. To ensure the safety of your Lichess account, we need you to set a new password. Thank you for your understanding. */
    blankedPassword: I18nString;
    /** Blitz */
    blitz: I18nString;
    /** Fast games: 3 to 8 minutes */
    blitzDesc: I18nString;
    /** Block */
    block: I18nString;
    /** Blocked */
    blocked: I18nString;
    /** %s blocks */
    blocks: I18nPlural;
    /** Blog */
    blog: I18nString;
    /** Blunder */
    blunder: I18nString;
    /** Board */
    board: I18nString;
    /** Board editor */
    boardEditor: I18nString;
    /** Reset colours to default */
    boardReset: I18nString;
    /** Bookmark this game */
    bookmarkThisGame: I18nString;
    /** Brightness */
    brightness: I18nString;
    /** Built for the love of chess, not money */
    builtForTheLoveOfChessNotMoney: I18nString;
    /** Bullet */
    bullet: I18nString;
    /** Bullet, blitz, classical */
    bulletBlitzClassical: I18nString;
    /** Very fast games: less than 3 minutes */
    bulletDesc: I18nString;
    /** by %s */
    by: I18nString;
    /** By CPL */
    byCPL: I18nString;
    /** By registering, you agree to the %s. */
    byRegisteringYouAgreeToBeBoundByOur: I18nString;
    /** Calculating moves... */
    calculatingMoves: I18nString;
    /** Cancel */
    cancel: I18nString;
    /** Cancel rematch offer */
    cancelRematchOffer: I18nString;
    /** Cancel the simul */
    cancelSimul: I18nString;
    /** Cancel the tournament */
    cancelTournament: I18nString;
    /** If you close your account a second time, there will be no way of recovering it. */
    cantDoThisTwice: I18nString;
    /** Please solve the chess captcha. */
    'captcha.fail': I18nString;
    /** Capture */
    capture: I18nString;
    /** Castling */
    castling: I18nString;
    /** Casual */
    casual: I18nString;
    /** Casual */
    casualTournament: I18nString;
    /** Change email */
    changeEmail: I18nString;
    /** Change password */
    changePassword: I18nString;
    /** Change username */
    changeUsername: I18nString;
    /** Change your username. This can only be done once and you are only allowed to change the case of the letters in your username. */
    changeUsernameDescription: I18nString;
    /** Only the case of the letters can change. For example "johndoe" to "JohnDoe". */
    changeUsernameNotSame: I18nString;
    /** Chat */
    chat: I18nString;
    /** Chat room */
    chatRoom: I18nString;
    /** Cheat */
    cheat: I18nString;
    /** Cheat Detected */
    cheatDetected: I18nString;
    /** Checkmate */
    checkmate: I18nString;
    /** Also check your spam folder, it might end up there. If so, mark it as not spam. */
    checkSpamFolder: I18nString;
    /** Check your Email */
    checkYourEmail: I18nString;
    /** Chess960 start position: %s */
    chess960StartPosition: I18nString;
    /** Chess basics */
    chessBasics: I18nString;
    /** Claim a draw */
    claimADraw: I18nString;
    /** Classical */
    classical: I18nString;
    /** Classical games: 25 minutes and more */
    classicalDesc: I18nString;
    /** Clear board */
    clearBoard: I18nString;
    /** Clear moves */
    clearSavedMoves: I18nString;
    /** Click here to read it */
    clickHereToReadIt: I18nString;
    /** Click on the board to make your move, and prove you are human. */
    clickOnTheBoardToMakeYourMove: I18nString;
    /** [Click to reveal email address] */
    clickToRevealEmailAddress: I18nString;
    /** Clock */
    clock: I18nString;
    /** Clock increment */
    clockIncrement: I18nString;
    /** Clock initial time */
    clockInitialTime: I18nString;
    /** Close */
    close: I18nString;
    /** If you closed your account, but have since changed your mind, you get one chance of getting your account back. */
    closedAccountChangedMind: I18nString;
    /** Closing your account will withdraw your appeal */
    closingAccountWithdrawAppeal: I18nString;
    /** Cloud analysis */
    cloudAnalysis: I18nString;
    /** Coaches */
    coaches: I18nString;
    /** Coach manager */
    coachManager: I18nString;
    /** Collapse variations */
    collapseVariations: I18nString;
    /** Community */
    community: I18nString;
    /** Compose message */
    composeMessage: I18nString;
    /** Computer */
    computer: I18nString;
    /** Computer analysis */
    computerAnalysis: I18nString;
    /** Computer analysis available */
    computerAnalysisAvailable: I18nString;
    /** Computer analysis disabled */
    computerAnalysisDisabled: I18nString;
    /** Computers and computer-assisted players are not allowed to play. Please do not get assistance from chess engines, databases, or from other players while playing. Also note that making multiple accounts is strongly discouraged and excessive multi-accounting will lead to being banned. */
    computersAreNotAllowedToPlay: I18nString;
    /** Computer thinking ... */
    computerThinking: I18nString;
    /** Conditional premoves */
    conditionalPremoves: I18nString;
    /** Entry requirements: */
    conditionOfEntry: I18nString;
    /** Confirm move */
    confirmMove: I18nString;
    /** Congratulations, you won! */
    congratsYouWon: I18nString;
    /** Continue from here */
    continueFromHere: I18nString;
    /** Contribute */
    contribute: I18nString;
    /** Copy and paste the above text and send it to %s */
    copyTextToEmail: I18nString;
    /** Copy variation PGN */
    copyVariationPgn: I18nString;
    /** Correspondence */
    correspondence: I18nString;
    /** Correspondence chess */
    correspondenceChess: I18nString;
    /** Correspondence games: one or several days per move */
    correspondenceDesc: I18nString;
    /** Country or region */
    countryRegion: I18nString;
    /** CPUs */
    cpus: I18nString;
    /** Create */
    create: I18nString;
    /** Create a game */
    createAGame: I18nString;
    /** Create a new topic */
    createANewTopic: I18nString;
    /** Create a new tournament */
    createANewTournament: I18nString;
    /** Created by */
    createdBy: I18nString;
    /** Newly created simuls */
    createdSimuls: I18nString;
    /** Create the topic */
    createTheTopic: I18nString;
    /** Crosstable */
    crosstable: I18nString;
    /** Cumulative */
    cumulative: I18nString;
    /** Current games */
    currentGames: I18nString;
    /** Current match score */
    currentMatchScore: I18nString;
    /** Current password */
    currentPassword: I18nString;
    /** Custom */
    custom: I18nString;
    /** Custom position */
    customPosition: I18nString;
    /** Cycle previous/next variation */
    cyclePreviousOrNextVariation: I18nString;
    /** Dark */
    dark: I18nString;
    /** Database */
    database: I18nString;
    /** Days per turn */
    daysPerTurn: I18nString;
    /** Decline */
    decline: I18nString;
    /** Defeat */
    defeat: I18nString;
    /** %1$s vs %2$s in %3$s */
    defeatVsYInZ: I18nString;
    /** Delete */
    delete: I18nString;
    /** Delete from here */
    deleteFromHere: I18nString;
    /** Delete this imported game? */
    deleteThisImportedGame: I18nString;
    /** Depth %s */
    depthX: I18nString;
    /** Private description */
    descPrivate: I18nString;
    /** Text that only the team members will see. If set, replaces the public description for team members. */
    descPrivateHelp: I18nString;
    /** Description */
    description: I18nString;
    /** Device theme */
    deviceTheme: I18nString;
    /** Disable Kid mode */
    disableKidMode: I18nString;
    /** Conversations */
    discussions: I18nString;
    /** Do it again */
    doItAgain: I18nString;
    /** Done reviewing black mistakes */
    doneReviewingBlackMistakes: I18nString;
    /** Done reviewing white mistakes */
    doneReviewingWhiteMistakes: I18nString;
    /** Download */
    download: I18nString;
    /** Download annotated */
    downloadAnnotated: I18nString;
    /** Download imported */
    downloadImported: I18nString;
    /** Download raw */
    downloadRaw: I18nString;
    /** Draw */
    draw: I18nString;
    /** The game has been drawn by the fifty move rule. */
    drawByFiftyMoves: I18nString;
    /** Draw by mutual agreement */
    drawByMutualAgreement: I18nString;
    /** Drawn */
    drawn: I18nString;
    /** Draw offer accepted */
    drawOfferAccepted: I18nString;
    /** Draw offer cancelled */
    drawOfferCanceled: I18nString;
    /** Draw offer sent */
    drawOfferSent: I18nString;
    /** Draw rate */
    drawRate: I18nString;
    /** Draws */
    draws: I18nString;
    /** %1$s vs %2$s in %3$s */
    drawVsYInZ: I18nString;
    /** DTZ50'' with rounding, based on number of half-moves until next capture or pawn move */
    dtzWithRounding: I18nString;
    /** Duration */
    duration: I18nString;
    /** Edit */
    edit: I18nString;
    /** Edit profile */
    editProfile: I18nString;
    /** Email */
    email: I18nString;
    /** Email address associated to the account */
    emailAssociatedToaccount: I18nString;
    /** It can take some time to arrive. */
    emailCanTakeSomeTime: I18nString;
    /** Help with email confirmation */
    emailConfirmHelp: I18nString;
    /** Didn't receive your confirmation email after signing up? */
    emailConfirmNotReceived: I18nString;
    /** If everything else fails, then send us this email: */
    emailForSignupHelp: I18nString;
    /** Email me a link */
    emailMeALink: I18nString;
    /** We have sent an email to %s. */
    emailSent: I18nString;
    /** Do not set an email address suggested by someone else. They will use it to steal your account. */
    emailSuggestion: I18nString;
    /** Embed in your website */
    embedInYourWebsite: I18nString;
    /** Paste a game URL or a study chapter URL to embed it. */
    embedsAvailable: I18nString;
    /** Leave empty to name the tournament after a notable chess player. */
    emptyTournamentName: I18nString;
    /** Enable */
    enable: I18nString;
    /** Enable Kid mode */
    enableKidMode: I18nString;
    /** Endgame */
    endgame: I18nString;
    /** Endgame positions */
    endgamePositions: I18nString;
    /** Error loading engine */
    engineFailed: I18nString;
    /** This email address is invalid */
    'error.email': I18nString;
    /** This email address is not acceptable. Please double-check it, and try again. */
    'error.email_acceptable': I18nString;
    /** This is already your email address */
    'error.email_different': I18nString;
    /** Email address invalid or already taken */
    'error.email_unique': I18nString;
    /** Must be at most %s */
    'error.max': I18nString;
    /** Must be at most %s characters long */
    'error.maxLength': I18nString;
    /** Must be at least %s */
    'error.min': I18nString;
    /** Must be at least %s characters long */
    'error.minLength': I18nString;
    /** Please don't use your username as your password. */
    'error.namePassword': I18nString;
    /** Please provide at least one link to a cheated game. */
    'error.provideOneCheatedGameLink': I18nString;
    /** This field is required */
    'error.required': I18nString;
    /** Invalid value */
    'error.unknown': I18nString;
    /** This password is extremely common, and too easy to guess. */
    'error.weakPassword': I18nString;
    /** Estimated start time */
    estimatedStart: I18nString;
    /** Evaluating your move ... */
    evaluatingYourMove: I18nString;
    /** Evaluation gauge */
    evaluationGauge: I18nString;
    /** Playing now */
    eventInProgress: I18nString;
    /** Everybody gets all features for free */
    everybodyGetsAllFeaturesForFree: I18nString;
    /** Expand variations */
    expandVariations: I18nString;
    /** Export games */
    exportGames: I18nString;
    /** Fast */
    fast: I18nString;
    /** Favourite opponents */
    favoriteOpponents: I18nString;
    /** Fifty moves without progress */
    fiftyMovesWithoutProgress: I18nString;
    /** Filter games */
    filterGames: I18nString;
    /** Find a better move for black */
    findBetterMoveForBlack: I18nString;
    /** Find a better move for white */
    findBetterMoveForWhite: I18nString;
    /** Finished */
    finished: I18nString;
    /** Flair */
    flair: I18nString;
    /** Flip board */
    flipBoard: I18nString;
    /** Focus chat */
    focusChat: I18nString;
    /** Follow */
    follow: I18nString;
    /** Follow and challenge friends */
    followAndChallengeFriends: I18nString;
    /** Following */
    following: I18nString;
    /** Follow %s */
    followX: I18nString;
    /** Call draw */
    forceDraw: I18nString;
    /** Claim victory */
    forceResignation: I18nString;
    /** Force variation */
    forceVariation: I18nString;
    /** Forgot password? */
    forgotPassword: I18nString;
    /** Forum */
    forum: I18nString;
    /** Free Online Chess */
    freeOnlineChess: I18nString;
    /** Friends */
    friends: I18nString;
    /** From position */
    fromPosition: I18nString;
    /** Full featured */
    fullFeatured: I18nString;
    /** Game aborted */
    gameAborted: I18nString;
    /** Game analysis */
    gameAnalysis: I18nString;
    /** Game as GIF */
    gameAsGIF: I18nString;
    /** You have a game in progress with %s. */
    gameInProgress: I18nString;
    /** Game Over */
    gameOver: I18nString;
    /** Games */
    games: I18nString;
    /** Games played */
    gamesPlayed: I18nString;
    /** Game vs %1$s */
    gameVsX: I18nString;
    /** Get a hint */
    getAHint: I18nString;
    /** Give %s seconds */
    giveNbSeconds: I18nPlural;
    /** Glicko-2 rating */
    glicko2Rating: I18nString;
    /** Go deeper */
    goDeeper: I18nString;
    /** To that effect, we must ensure that all players follow good practice. */
    goodPractice: I18nString;
    /** Graph */
    graph: I18nString;
    /** Hang on! */
    hangOn: I18nString;
    /** Help: */
    help: I18nString;
    /** Hide best move */
    hideBestMove: I18nString;
    /** Host */
    host: I18nString;
    /** Host a new simul */
    hostANewSimul: I18nString;
    /** Host colour: %s */
    hostColorX: I18nString;
    /** How to avoid this? */
    howToAvoidThis: I18nString;
    /** Hue */
    hue: I18nString;
    /** Human */
    human: I18nString;
    /** If none, leave empty */
    ifNoneLeaveEmpty: I18nString;
    /** If rating is ± %s */
    ifRatingIsPlusMinusX: I18nString;
    /** If registered */
    ifRegistered: I18nString;
    /** If you don't see the email, check other places it might be, like your junk, spam, social, or other folders. */
    ifYouDoNotSeeTheEmailCheckOtherPlaces: I18nString;
    /** Important */
    important: I18nString;
    /** Imported by %s */
    importedByX: I18nString;
    /** Import game */
    importGame: I18nString;
    /** Variations will be erased. To keep them, import the PGN via a study. */
    importGameCaveat: I18nString;
    /** This PGN can be accessed by the public. To import a game privately, use a study. */
    importGameDataPrivacyWarning: I18nString;
    /** Paste a game PGN to get a browsable replay, computer analysis, game chat and public shareable URL. */
    importGameExplanation: I18nString;
    /** Import PGN */
    importPgn: I18nString;
    /** Inaccuracy */
    inaccuracy: I18nString;
    /** Anything even slightly inappropriate could get your account closed. */
    inappropriateNameWarning: I18nString;
    /** Inbox */
    inbox: I18nString;
    /** Incorrect password */
    incorrectPassword: I18nString;
    /** Increment */
    increment: I18nString;
    /** Increment in seconds */
    incrementInSeconds: I18nString;
    /** Infinite analysis */
    infiniteAnalysis: I18nString;
    /** In kid mode, the Lichess logo gets a %s icon, so you know your kids are safe. */
    inKidModeTheLichessLogoGetsIconX: I18nString;
    /** Inline notation */
    inlineNotation: I18nString;
    /** in local browser */
    inLocalBrowser: I18nString;
    /** Inside the board */
    insideTheBoard: I18nString;
    /** Instructions */
    instructions: I18nString;
    /** Insufficient material */
    insufficientMaterial: I18nString;
    /** in the FAQ */
    inTheFAQ: I18nString;
    /** Invalid authentication code */
    invalidAuthenticationCode: I18nString;
    /** Invalid FEN */
    invalidFen: I18nString;
    /** Invalid PGN */
    invalidPgn: I18nString;
    /** Invalid username or password */
    invalidUsernameOrPassword: I18nString;
    /** invited you to "%1$s". */
    invitedYouToX: I18nString;
    /** In your own local timezone */
    inYourLocalTimezone: I18nString;
    /** Private */
    isPrivate: I18nString;
    /** It's your turn! */
    itsYourTurn: I18nString;
    /** Join */
    join: I18nString;
    /** Join the game */
    joinTheGame: I18nString;
    /** Join the %1$s, to post in this forum */
    joinTheTeamXToPost: I18nString;
    /** Keyboard shortcuts */
    keyboardShortcuts: I18nString;
    /** Cycle selected variation */
    keyCycleSelectedVariation: I18nString;
    /** enter/exit variation */
    keyEnterOrExitVariation: I18nString;
    /** go to start/end */
    keyGoToStartOrEnd: I18nString;
    /** move backward/forward */
    keyMoveBackwardOrForward: I18nString;
    /** Next blunder */
    keyNextBlunder: I18nString;
    /** Next branch */
    keyNextBranch: I18nString;
    /** Next inaccuracy */
    keyNextInaccuracy: I18nString;
    /** Next (Learn from your mistakes) */
    keyNextLearnFromYourMistakes: I18nString;
    /** Next mistake */
    keyNextMistake: I18nString;
    /** Previous branch */
    keyPreviousBranch: I18nString;
    /** Request computer analysis, Learn from your mistakes */
    keyRequestComputerAnalysis: I18nString;
    /** show/hide comments */
    keyShowOrHideComments: I18nString;
    /** Kid mode */
    kidMode: I18nString;
    /** This is about safety. In kid mode, all site communications are disabled. Enable this for your children and school students, to protect them from other internet users. */
    kidModeExplanation: I18nString;
    /** Kid mode is enabled. */
    kidModeIsEnabled: I18nString;
    /** King in the centre */
    kingInTheCenter: I18nString;
    /** Language */
    language: I18nString;
    /** Last post */
    lastPost: I18nString;
    /** Active %s */
    lastSeenActive: I18nString;
    /** Latest forum posts */
    latestForumPosts: I18nString;
    /** Leaderboard */
    leaderboard: I18nString;
    /** Learn from this mistake */
    learnFromThisMistake: I18nString;
    /** Learn from your mistakes */
    learnFromYourMistakes: I18nString;
    /** Learn */
    learnMenu: I18nString;
    /** Less than %s minutes */
    lessThanNbMinutes: I18nPlural;
    /** Let other players challenge you */
    letOtherPlayersChallengeYou: I18nString;
    /** Let other players follow you */
    letOtherPlayersFollowYou: I18nString;
    /** Let other players invite you to study */
    letOtherPlayersInviteYouToStudy: I18nString;
    /** Let other players message you */
    letOtherPlayersMessageYou: I18nString;
    /** Level */
    level: I18nString;
    /** Rated games played on Lichess */
    lichessDbExplanation: I18nString;
    /** Lichess is a charity and entirely free/libre open source software. */
    lichessPatronInfo: I18nString;
    /** Lichess tournaments */
    lichessTournaments: I18nString;
    /** Lifetime score */
    lifetimeScore: I18nString;
    /** Light */
    light: I18nString;
    /** List */
    list: I18nString;
    /** List players you have blocked */
    listBlockedPlayers: I18nString;
    /** Loading engine... */
    loadingEngine: I18nString;
    /** Load position */
    loadPosition: I18nString;
    /** Lobby */
    lobby: I18nString;
    /** Location */
    location: I18nString;
    /** Sign in to chat */
    loginToChat: I18nString;
    /** Sign out */
    logOut: I18nString;
    /** Losing */
    losing: I18nString;
    /** Losses */
    losses: I18nString;
    /** Loss or 50 moves by prior mistake */
    lossOr50MovesByPriorMistake: I18nString;
    /** Loss prevented by 50-move rule */
    lossSavedBy50MoveRule: I18nString;
    /** You lost rating points to someone who violated the Lichess TOS */
    lostAgainstTOSViolator: I18nString;
    /** For safekeeping and sharing, consider making a study. */
    makeAStudy: I18nString;
    /** Make mainline */
    makeMainLine: I18nString;
    /** Make the tournament private, and restrict access with a password */
    makePrivateTournament: I18nString;
    /** Make sure to read %1$s */
    makeSureToRead: I18nString;
    /** %s is available for more advanced syntax. */
    markdownAvailable: I18nString;
    /** OTB games of %1$s+ FIDE-rated players from %2$s to %3$s */
    masterDbExplanation: I18nString;
    /** Mate in %s half-moves */
    mateInXHalfMoves: I18nPlural;
    /** Max depth reached! */
    maxDepthReached: I18nString;
    /** Maximum: %s characters. */
    maximumNbCharacters: I18nPlural;
    /** Maximum weekly rating */
    maximumWeeklyRating: I18nString;
    /** Maybe include more games from the preferences menu? */
    maybeIncludeMoreGamesFromThePreferencesMenu: I18nString;
    /** Member since */
    memberSince: I18nString;
    /** Memory */
    memory: I18nString;
    /** mentioned you in "%1$s". */
    mentionedYouInX: I18nString;
    /** Menu */
    menu: I18nString;
    /** Message */
    message: I18nString;
    /** Middlegame */
    middlegame: I18nString;
    /** Minimum rated games */
    minimumRatedGames: I18nString;
    /** Minimum rating */
    minimumRating: I18nString;
    /** Minutes per side */
    minutesPerSide: I18nString;
    /** Mistake */
    mistake: I18nString;
    /** Mobile */
    mobile: I18nString;
    /** Mobile App */
    mobileApp: I18nString;
    /** Mode */
    mode: I18nString;
    /** More */
    more: I18nString;
    /** ≥ %1$s %2$s rated games */
    moreThanNbPerfRatedGames: I18nPlural;
    /** ≥ %s rated games */
    moreThanNbRatedGames: I18nPlural;
    /** Mouse tricks */
    mouseTricks: I18nString;
    /** Move */
    move: I18nString;
    /** Moves played */
    movesPlayed: I18nString;
    /** Move times */
    moveTimes: I18nString;
    /** Multiple lines */
    multipleLines: I18nString;
    /** Must be in team %s */
    mustBeInTeam: I18nString;
    /** Name */
    name: I18nString;
    /** Navigate the move tree */
    navigateMoveTree: I18nString;
    /** %s blunders */
    nbBlunders: I18nPlural;
    /** %s bookmarks */
    nbBookmarks: I18nPlural;
    /** %s days */
    nbDays: I18nPlural;
    /** %s draws */
    nbDraws: I18nPlural;
    /** %s followers */
    nbFollowers: I18nPlural;
    /** %s following */
    nbFollowing: I18nPlural;
    /** %s forum posts */
    nbForumPosts: I18nPlural;
    /** %s friends online */
    nbFriendsOnline: I18nPlural;
    /** %s games */
    nbGames: I18nPlural;
    /** %s games in play */
    nbGamesInPlay: I18nPlural;
    /** %s games with you */
    nbGamesWithYou: I18nPlural;
    /** %s hours */
    nbHours: I18nPlural;
    /** %s imported games */
    nbImportedGames: I18nPlural;
    /** %s inaccuracies */
    nbInaccuracies: I18nPlural;
    /** %s losses */
    nbLosses: I18nPlural;
    /** %s minutes */
    nbMinutes: I18nPlural;
    /** %s mistakes */
    nbMistakes: I18nPlural;
    /** %1$s %2$s players this week. */
    nbPerfTypePlayersThisWeek: I18nPlural;
    /** %s players */
    nbPlayers: I18nPlural;
    /** %s playing */
    nbPlaying: I18nPlural;
    /** %s puzzles */
    nbPuzzles: I18nPlural;
    /** %s rated */
    nbRated: I18nPlural;
    /** %s seconds */
    nbSeconds: I18nPlural;
    /** %s seconds to play the first move */
    nbSecondsToPlayTheFirstMove: I18nPlural;
    /** %s simuls */
    nbSimuls: I18nPlural;
    /** %s studies */
    nbStudies: I18nPlural;
    /** %s tournament points */
    nbTournamentPoints: I18nPlural;
    /** %s wins */
    nbWins: I18nPlural;
    /** You need to play %s more rated games */
    needNbMoreGames: I18nPlural;
    /** You need to play %1$s more %2$s rated games */
    needNbMorePerfGames: I18nPlural;
    /** Network lag between you and Lichess */
    networkLagBetweenYouAndLichess: I18nString;
    /** Never */
    never: I18nString;
    /** Never type your Lichess password on another site! */
    neverTypeYourPassword: I18nString;
    /** New opponent */
    newOpponent: I18nString;
    /** New password */
    newPassword: I18nString;
    /** New password (again) */
    newPasswordAgain: I18nString;
    /** The new passwords don't match */
    newPasswordsDontMatch: I18nString;
    /** Password strength */
    newPasswordStrength: I18nString;
    /** New tournament */
    newTournament: I18nString;
    /** Next */
    next: I18nString;
    /** Next %s tournament: */
    nextXTournament: I18nString;
    /** No */
    no: I18nString;
    /** No chat */
    noChat: I18nString;
    /** No conditional premoves */
    noConditionalPremoves: I18nString;
    /** You cannot draw before 30 moves are played in a Swiss tournament. */
    noDrawBeforeSwissLimit: I18nString;
    /** No game found */
    noGameFound: I18nString;
    /** No mistakes found for black */
    noMistakesFoundForBlack: I18nString;
    /** No mistakes found for white */
    noMistakesFoundForWhite: I18nString;
    /** None */
    none: I18nString;
    /** Offline */
    noNetwork: I18nString;
    /** No note yet */
    noNoteYet: I18nString;
    /** No restriction */
    noRestriction: I18nString;
    /** Normal */
    normal: I18nString;
    /** This simultaneous exhibition does not exist. */
    noSimulExplanation: I18nString;
    /** Simul not found */
    noSimulFound: I18nString;
    /** Not a checkmate */
    notACheckmate: I18nString;
    /** Notes */
    notes: I18nString;
    /** Nothing to see here at the moment. */
    nothingToSeeHere: I18nString;
    /** Notifications */
    notifications: I18nString;
    /** Notifications: %1$s */
    notificationsX: I18nString;
    /** Offer draw */
    offerDraw: I18nString;
    /** OK */
    ok: I18nString;
    /** One day */
    oneDay: I18nString;
    /** One URL per line. */
    oneUrlPerLine: I18nString;
    /** Online and offline play */
    onlineAndOfflinePlay: I18nString;
    /** Online bots */
    onlineBots: I18nString;
    /** Online players */
    onlinePlayers: I18nString;
    /** Only existing conversations */
    onlyExistingConversations: I18nString;
    /** Only friends */
    onlyFriends: I18nString;
    /** Only members of team */
    onlyMembersOfTeam: I18nString;
    /** Only team leaders */
    onlyTeamLeaders: I18nString;
    /** Only team members */
    onlyTeamMembers: I18nString;
    /** This will only work once. */
    onlyWorksOnce: I18nString;
    /** On slow games */
    onSlowGames: I18nString;
    /** Opacity */
    opacity: I18nString;
    /** Opening */
    opening: I18nString;
    /** Opening/endgame explorer */
    openingEndgameExplorer: I18nString;
    /** Opening explorer */
    openingExplorer: I18nString;
    /** Opening explorer & tablebase */
    openingExplorerAndTablebase: I18nString;
    /** Openings */
    openings: I18nString;
    /** Open study */
    openStudy: I18nString;
    /** Open tournaments */
    openTournaments: I18nString;
    /** Opponent */
    opponent: I18nString;
    /** Your opponent left the game. You can claim victory, call the game a draw, or wait. */
    opponentLeftChoices: I18nString;
    /** Your opponent left the game. You can claim victory in %s seconds. */
    opponentLeftCounter: I18nPlural;
    /** Or let your opponent scan this QR code */
    orLetYourOpponentScanQrCode: I18nString;
    /** Or upload a PGN file */
    orUploadPgnFile: I18nString;
    /** Other */
    other: I18nString;
    /** other players */
    otherPlayers: I18nString;
    /** Our tips for organising events */
    ourEventTips: I18nString;
    /** Outside the board */
    outsideTheBoard: I18nString;
    /** Password */
    password: I18nString;
    /** Password reset */
    passwordReset: I18nString;
    /** Do not set a password suggested by someone else. They will use it to steal your account. */
    passwordSuggestion: I18nString;
    /** Paste the FEN text here */
    pasteTheFenStringHere: I18nString;
    /** Paste the PGN text here */
    pasteThePgnStringHere: I18nString;
    /** Pause */
    pause: I18nString;
    /** Pawn move */
    pawnMove: I18nString;
    /** Performance */
    performance: I18nString;
    /** Rating: %s */
    perfRatingX: I18nString;
    /** Phone and tablet */
    phoneAndTablet: I18nString;
    /** Piece set */
    pieceSet: I18nString;
    /** Play */
    play: I18nString;
    /** Play chess everywhere */
    playChessEverywhere: I18nString;
    /** Play chess in style */
    playChessInStyle: I18nString;
    /** Play best computer move */
    playComputerMove: I18nString;
    /** Player */
    player: I18nString;
    /** Players */
    players: I18nString;
    /** Play every game you start. */
    playEveryGame: I18nString;
    /** Play first opening/endgame-explorer move */
    playFirstOpeningEndgameExplorerMove: I18nString;
    /** Playing right now */
    playingRightNow: I18nString;
    /** play selected move */
    playSelectedMove: I18nString;
    /** Play a variation to create conditional premoves */
    playVariationToCreateConditionalPremoves: I18nString;
    /** Play with a friend */
    playWithAFriend: I18nString;
    /** Play with the computer */
    playWithTheMachine: I18nString;
    /** Play %s */
    playX: I18nString;
    /** We aim to provide a pleasant chess experience for everyone. */
    pleasantChessExperience: I18nString;
    /** Points */
    points: I18nString;
    /** Popular openings */
    popularOpenings: I18nString;
    /** Paste a valid FEN to start every game from a given position. */
    positionInputHelp: I18nString;
    /** Posts */
    posts: I18nString;
    /** When a potential problem is detected, we display this message. */
    potentialProblem: I18nString;
    /** Practice */
    practice: I18nString;
    /** Practice with computer */
    practiceWithComputer: I18nString;
    /** Previously on Lichess TV */
    previouslyOnLichessTV: I18nString;
    /** Privacy */
    privacy: I18nString;
    /** Privacy policy */
    privacyPolicy: I18nString;
    /** Proceed to %s */
    proceedToX: I18nString;
    /** Profile */
    profile: I18nString;
    /** Profile completion: %s */
    profileCompletion: I18nString;
    /** Promote variation */
    promoteVariation: I18nString;
    /** Propose a takeback */
    proposeATakeback: I18nString;
    /** Chess tactics trainer */
    puzzleDesc: I18nString;
    /** Puzzles */
    puzzles: I18nString;
    /** Quick pairing */
    quickPairing: I18nString;
    /** Race finished */
    raceFinished: I18nString;
    /** Random side */
    randomColor: I18nString;
    /** Rank */
    rank: I18nString;
    /** Rank is updated every %s minutes */
    rankIsUpdatedEveryNbMinutes: I18nPlural;
    /** Rank: %s */
    rankX: I18nString;
    /** Rapid */
    rapid: I18nString;
    /** Rapid games: 8 to 25 minutes */
    rapidDesc: I18nString;
    /** Rated */
    rated: I18nString;
    /** Games are rated and impact players ratings */
    ratedFormHelp: I18nString;
    /** Rated ≤ %1$s in %2$s for the last week */
    ratedLessThanInPerf: I18nString;
    /** Rated ≥ %1$s in %2$s */
    ratedMoreThanInPerf: I18nString;
    /** Rated */
    ratedTournament: I18nString;
    /** Rating */
    rating: I18nString;
    /** Rating range */
    ratingRange: I18nString;
    /** Rating stats */
    ratingStats: I18nString;
    /** %1$s rating over %2$s games */
    ratingXOverYGames: I18nPlural;
    /** Read about our %s. */
    readAboutOur: I18nString;
    /** really */
    really: I18nString;
    /** Real name */
    realName: I18nString;
    /** Real time */
    realTime: I18nString;
    /** Realtime */
    realtimeReplay: I18nString;
    /** Reason */
    reason: I18nString;
    /** Receive notifications when mentioned in the forum */
    receiveForumNotifications: I18nString;
    /** Recent games */
    recentGames: I18nString;
    /** Reconnecting */
    reconnecting: I18nString;
    /** Wait 5 minutes and refresh your email inbox. */
    refreshInboxAfterFiveMinutes: I18nString;
    /** Refund: %1$s %2$s rating points. */
    refundXpointsTimeControlY: I18nString;
    /** Rematch */
    rematch: I18nString;
    /** Rematch offer accepted */
    rematchOfferAccepted: I18nString;
    /** Rematch offer cancelled */
    rematchOfferCanceled: I18nString;
    /** Rematch offer declined */
    rematchOfferDeclined: I18nString;
    /** Rematch offer sent */
    rematchOfferSent: I18nString;
    /** Keep me logged in */
    rememberMe: I18nString;
    /** Removes the depth limit, and keeps your computer warm */
    removesTheDepthLimit: I18nString;
    /** Reopen your account */
    reopenYourAccount: I18nString;
    /** Replay mode */
    replayMode: I18nString;
    /** Replies */
    replies: I18nString;
    /** Reply */
    reply: I18nString;
    /** Reply to this topic */
    replyToThisTopic: I18nString;
    /** Report a user */
    reportAUser: I18nString;
    /** Paste the link to the game(s) and explain what is wrong about this user's behaviour. Don't just say "they cheat", but tell us how you came to this conclusion. */
    reportCheatBoostHelp: I18nString;
    /** Your report will be processed faster if written in English. */
    reportProcessedFasterInEnglish: I18nString;
    /** Explain what about this username is offensive. Don't just say "it's offensive/inappropriate", but tell us how you came to this conclusion, especially if the insult is obfuscated, not in english, is in slang, or is a historical/cultural reference. */
    reportUsernameHelp: I18nString;
    /** Report %s to moderators */
    reportXToModerators: I18nString;
    /** Request a computer analysis */
    requestAComputerAnalysis: I18nString;
    /** Required. */
    required: I18nString;
    /** Reset */
    reset: I18nString;
    /** Resign */
    resign: I18nString;
    /** Resign lost games (don't let the clock run down). */
    resignLostGames: I18nString;
    /** Resign the game */
    resignTheGame: I18nString;
    /** Resume */
    resume: I18nString;
    /** Resume learning */
    resumeLearning: I18nString;
    /** Resume practice */
    resumePractice: I18nString;
    /** %1$s vs %2$s */
    resVsX: I18nString;
    /** Retry */
    retry: I18nString;
    /** Return to simul homepage */
    returnToSimulHomepage: I18nString;
    /** Return to tournaments homepage */
    returnToTournamentsHomepage: I18nString;
    /** Review black mistakes */
    reviewBlackMistakes: I18nString;
    /** Review white mistakes */
    reviewWhiteMistakes: I18nString;
    /** revoke all sessions */
    revokeAllSessions: I18nString;
    /** Pick a very safe name for the tournament. */
    safeTournamentName: I18nString;
    /** Save */
    save: I18nString;
    /** Screenshot current position */
    screenshotCurrentPosition: I18nString;
    /** Scroll over computer variations to preview them. */
    scrollOverComputerVariationsToPreviewThem: I18nString;
    /** Search or start new conversation */
    searchOrStartNewDiscussion: I18nString;
    /** Security */
    security: I18nString;
    /** See best move */
    seeBestMove: I18nString;
    /** Send */
    send: I18nString;
    /** We've sent you an email with a link. */
    sentEmailWithLink: I18nString;
    /** Sessions */
    sessions: I18nString;
    /** Set your flair */
    setFlair: I18nString;
    /** Set the board */
    setTheBoard: I18nString;
    /** Share your chess insights data */
    shareYourInsightsData: I18nString;
    /** Show this help dialog */
    showHelpDialog: I18nString;
    /** Show me everything */
    showMeEverything: I18nString;
    /** Show threat */
    showThreat: I18nString;
    /** You have received a private message from Lichess. */
    showUnreadLichessMessage: I18nString;
    /** Show variation arrows */
    showVariationArrows: I18nString;
    /** Side */
    side: I18nString;
    /** Sign in */
    signIn: I18nString;
    /** Register */
    signUp: I18nString;
    /** We will only use it for password reset. */
    signupEmailHint: I18nString;
    /** Sign up to host or join a simul */
    signUpToHostOrJoinASimul: I18nString;
    /** Make sure to choose a family-friendly username. You cannot change it later and any accounts with inappropriate usernames will get closed! */
    signupUsernameHint: I18nString;
    /** You may add extra initial time to your clock to help you cope with the simul. */
    simulAddExtraTime: I18nString;
    /** Add initial time to your clock for each player joining the simul. */
    simulAddExtraTimePerPlayer: I18nString;
    /** Fischer Clock setup. The more players you take on, the more time you may need. */
    simulClockHint: I18nString;
    /** Simul description */
    simulDescription: I18nString;
    /** Anything you want to tell the participants? */
    simulDescriptionHelp: I18nString;
    /** Feature on %s */
    simulFeatured: I18nString;
    /** Show your simul to everyone on %s. Disable for private simuls. */
    simulFeaturedHelp: I18nString;
    /** Host colour for each game */
    simulHostcolor: I18nString;
    /** Host extra initial clock time */
    simulHostExtraTime: I18nString;
    /** Host extra clock time per player */
    simulHostExtraTimePerPlayer: I18nString;
    /** Simultaneous exhibitions */
    simultaneousExhibitions: I18nString;
    /** If you select several variants, each player gets to choose which one to play. */
    simulVariantsHint: I18nString;
    /** Since */
    since: I18nString;
    /** Free online chess server. Play chess in a clean interface. No registration, no ads, no plugin required. Play chess with the computer, friends or random opponents. */
    siteDescription: I18nString;
    /** Size */
    size: I18nString;
    /** Skip this move */
    skipThisMove: I18nString;
    /** Slow */
    slow: I18nString;
    /** Social media links */
    socialMediaLinks: I18nString;
    /** Solution */
    solution: I18nString;
    /** Someone you reported was banned */
    someoneYouReportedWasBanned: I18nString;
    /** Sorry :( */
    sorry: I18nString;
    /** Sound */
    sound: I18nString;
    /** Source Code */
    sourceCode: I18nString;
    /** Spectator room */
    spectatorRoom: I18nString;
    /** Stalemate */
    stalemate: I18nString;
    /** Standard */
    standard: I18nString;
    /** Stand by %s, pairing players, get ready! */
    standByX: I18nString;
    /** Standing */
    standing: I18nString;
    /** started streaming */
    startedStreaming: I18nString;
    /** Starting: */
    starting: I18nString;
    /** Starting position */
    startPosition: I18nString;
    /** Stats */
    stats: I18nString;
    /** Streamer manager */
    streamerManager: I18nString;
    /** Streamers */
    streamersMenu: I18nString;
    /** Strength */
    strength: I18nString;
    /** Study */
    studyMenu: I18nString;
    /** Subject */
    subject: I18nString;
    /** Subscribe */
    subscribe: I18nString;
    /** Success */
    success: I18nString;
    /** Switch sides */
    switchSides: I18nString;
    /** Takeback */
    takeback: I18nString;
    /** Takeback accepted */
    takebackPropositionAccepted: I18nString;
    /** Takeback cancelled */
    takebackPropositionCanceled: I18nString;
    /** Takeback declined */
    takebackPropositionDeclined: I18nString;
    /** Takeback sent */
    takebackPropositionSent: I18nString;
    /** Please be nice in the chat! */
    talkInChat: I18nString;
    /** %1$s team */
    teamNamedX: I18nString;
    /** We apologise for the temporary inconvenience, */
    temporaryInconvenience: I18nString;
    /** Terms of Service */
    termsOfService: I18nString;
    /** Thank you! */
    thankYou: I18nString;
    /** Thank you for reading! */
    thankYouForReading: I18nString;
    /** The first person to come to this URL will play with you. */
    theFirstPersonToComeOnThisUrlWillPlayWithYou: I18nString;
    /** the forum etiquette */
    theForumEtiquette: I18nString;
    /** The game is a draw. */
    theGameIsADraw: I18nString;
    /** Thematic */
    thematic: I18nString;
    /** This account violated the Lichess Terms of Service */
    thisAccountViolatedTos: I18nString;
    /** This game is rated */
    thisGameIsRated: I18nString;
    /** This is a chess CAPTCHA. */
    thisIsAChessCaptcha: I18nString;
    /** This topic has been archived and can no longer be replied to. */
    thisTopicIsArchived: I18nString;
    /** This topic is now closed. */
    thisTopicIsNowClosed: I18nString;
    /** Three checks */
    threeChecks: I18nString;
    /** Threefold repetition */
    threefoldRepetition: I18nString;
    /** Time */
    time: I18nString;
    /** Time is almost up! */
    timeAlmostUp: I18nString;
    /** Time before tournament starts */
    timeBeforeTournamentStarts: I18nString;
    /** Time control */
    timeControl: I18nString;
    /** Timeline */
    timeline: I18nString;
    /** Time to process a move on Lichess's server */
    timeToProcessAMoveOnLichessServer: I18nString;
    /** Today */
    today: I18nString;
    /** Toggle all computer analysis */
    toggleAllAnalysis: I18nString;
    /** Toggle move annotations */
    toggleGlyphAnnotations: I18nString;
    /** Toggle local computer analysis */
    toggleLocalAnalysis: I18nString;
    /** Toggle local evaluation */
    toggleLocalEvaluation: I18nString;
    /** Toggle position annotations */
    togglePositionAnnotations: I18nString;
    /** Toggle the chat */
    toggleTheChat: I18nString;
    /** Toggle variation arrows */
    toggleVariationArrows: I18nString;
    /** To invite someone to play, give this URL */
    toInviteSomeoneToPlayGiveThisUrl: I18nString;
    /** Tools */
    tools: I18nString;
    /** Top games */
    topGames: I18nString;
    /** Topics */
    topics: I18nString;
    /** To report a user for cheating or bad behaviour, %1$s */
    toReportSomeoneForCheatingOrBadBehavior: I18nString;
    /** To request support, %1$s */
    toRequestSupport: I18nString;
    /** Study */
    toStudy: I18nString;
    /** Tournament */
    tournament: I18nString;
    /** Tournament calendar */
    tournamentCalendar: I18nString;
    /** Tournament complete */
    tournamentComplete: I18nString;
    /** This tournament does not exist. */
    tournamentDoesNotExist: I18nString;
    /** Tournament entry code */
    tournamentEntryCode: I18nString;
    /** Arena tournament FAQ */
    tournamentFAQ: I18nString;
    /** Play fast-paced chess tournaments! Join an official scheduled tournament, or create your own. Bullet, Blitz, Classical, Chess960, King of the Hill, Threecheck, and more options available for endless chess fun. */
    tournamentHomeDescription: I18nString;
    /** Chess tournaments featuring various time controls and variants */
    tournamentHomeTitle: I18nString;
    /** The tournament is starting */
    tournamentIsStarting: I18nString;
    /** The tournament may have been cancelled if all players left before it started. */
    tournamentMayHaveBeenCanceled: I18nString;
    /** Tournament not found */
    tournamentNotFound: I18nString;
    /** The tournament pairings are now closed. */
    tournamentPairingsAreNowClosed: I18nString;
    /** Tournament points */
    tournamentPoints: I18nString;
    /** Tournaments */
    tournaments: I18nString;
    /** Tournament chat */
    tournChat: I18nString;
    /** Tournament description */
    tournDescription: I18nString;
    /** Anything special you want to tell the participants? Try to keep it short. Markdown links are available: [name](https://url) */
    tournDescriptionHelp: I18nString;
    /** Time featured on TV: %s */
    tpTimeSpentOnTV: I18nString;
    /** Time spent playing: %s */
    tpTimeSpentPlaying: I18nString;
    /** Transparent */
    transparent: I18nString;
    /** Troll */
    troll: I18nString;
    /** Try another move for black */
    tryAnotherMoveForBlack: I18nString;
    /** Try another move for white */
    tryAnotherMoveForWhite: I18nString;
    /** try the contact page */
    tryTheContactPage: I18nString;
    /** Try to win (or at least draw) every game you play. */
    tryToWin: I18nString;
    /** Type private notes here */
    typePrivateNotesHere: I18nString;
    /** Insanely fast games: less than 30 seconds */
    ultraBulletDesc: I18nString;
    /** Unblock */
    unblock: I18nString;
    /** Unfollow */
    unfollow: I18nString;
    /** Unfollow %s */
    unfollowX: I18nString;
    /** Unknown */
    unknown: I18nString;
    /** Win/loss only guaranteed if recommended tablebase line has been followed since the last capture or pawn move, due to possible rounding of DTZ values in Syzygy tablebases. */
    unknownDueToRounding: I18nString;
    /** Unlimited */
    unlimited: I18nString;
    /** Unsubscribe */
    unsubscribe: I18nString;
    /** Until */
    until: I18nString;
    /** User */
    user: I18nString;
    /** %1$s is better than %2$s of %3$s players. */
    userIsBetterThanPercentOfPerfTypePlayers: I18nString;
    /** User name */
    username: I18nString;
    /** This username is already in use, please try another one. */
    usernameAlreadyUsed: I18nString;
    /** You can use this username to create a new account */
    usernameCanBeUsedForNewAccount: I18nString;
    /** The username must only contain letters, numbers, underscores, and hyphens. Consecutive underscores and hyphens are not allowed. */
    usernameCharsInvalid: I18nString;
    /** We couldn't find any user by this name: %s. */
    usernameNotFound: I18nString;
    /** User name or email */
    usernameOrEmail: I18nString;
    /** The username must start with a letter. */
    usernamePrefixInvalid: I18nString;
    /** The username must end with a letter or a number. */
    usernameSuffixInvalid: I18nString;
    /** This username is not acceptable. */
    usernameUnacceptable: I18nString;
    /** use the report form */
    useTheReportForm: I18nString;
    /** Using server analysis */
    usingServerAnalysis: I18nString;
    /** Variant */
    variant: I18nString;
    /** Variant ending */
    variantEnding: I18nString;
    /** Variant loss */
    variantLoss: I18nString;
    /** Variants */
    variants: I18nString;
    /** Variant win */
    variantWin: I18nString;
    /** Variation arrows let you navigate without using the move list. */
    variationArrowsInfo: I18nString;
    /** Victory */
    victory: I18nString;
    /** %1$s vs %2$s in %3$s */
    victoryVsYInZ: I18nString;
    /** Video library */
    videoLibrary: I18nString;
    /** View in full size */
    viewInFullSize: I18nString;
    /** View rematch */
    viewRematch: I18nString;
    /** Views */
    views: I18nString;
    /** View the solution */
    viewTheSolution: I18nString;
    /** View tournament */
    viewTournament: I18nString;
    /** We will come back to you shortly to help you complete your signup. */
    waitForSignupHelp: I18nString;
    /** Waiting */
    waiting: I18nString;
    /** Waiting for analysis */
    waitingForAnalysis: I18nString;
    /** Waiting for opponent */
    waitingForOpponent: I18nString;
    /** Watch */
    watch: I18nString;
    /** Watch games */
    watchGames: I18nString;
    /** Webmasters */
    webmasters: I18nString;
    /** Website */
    website: I18nString;
    /** Weekly %s rating distribution */
    weeklyPerfTypeRatingDistribution: I18nString;
    /** We had to time you out for a while. */
    weHadToTimeYouOutForAWhile: I18nString;
    /** We've sent you an email. Click the link in the email to activate your account. */
    weHaveSentYouAnEmailClickTheLink: I18nString;
    /** We've sent an email to %s. Click the link in the email to reset your password. */
    weHaveSentYouAnEmailTo: I18nString;
    /** What's the matter? */
    whatIsIheMatter: I18nString;
    /** What username did you use to sign up? */
    whatSignupUsername: I18nString;
    /** When you create a Simul, you get to play several players at once. */
    whenCreateSimul: I18nString;
    /** White */
    white: I18nString;
    /** White O-O */
    whiteCastlingKingside: I18nString;
    /** White to checkmate in one move */
    whiteCheckmatesInOneMove: I18nString;
    /** White declines draw */
    whiteDeclinesDraw: I18nString;
    /** White didn't move */
    whiteDidntMove: I18nString;
    /** White / Draw / Black */
    whiteDrawBlack: I18nString;
    /** White is victorious */
    whiteIsVictorious: I18nString;
    /** White left the game */
    whiteLeftTheGame: I18nString;
    /** White offers draw */
    whiteOffersDraw: I18nString;
    /** White to play */
    whitePlays: I18nString;
    /** White resigned */
    whiteResigned: I18nString;
    /** White time out */
    whiteTimeOut: I18nString;
    /** White wins */
    whiteWins: I18nString;
    /** White wins */
    whiteWinsGame: I18nString;
    /** Why? */
    why: I18nString;
    /** Winner */
    winner: I18nString;
    /** Winning */
    winning: I18nString;
    /** Win or 50 moves by prior mistake */
    winOr50MovesByPriorMistake: I18nString;
    /** Win prevented by 50-move rule */
    winPreventedBy50MoveRule: I18nString;
    /** Win rate */
    winRate: I18nString;
    /** Wins */
    wins: I18nString;
    /** and wish you great games on lichess.org. */
    wishYouGreatGames: I18nString;
    /** Withdraw */
    withdraw: I18nString;
    /** With everybody */
    withEverybody: I18nString;
    /** With friends */
    withFriends: I18nString;
    /** With nobody */
    withNobody: I18nString;
    /** Write a private note about this user */
    writeAPrivateNoteAboutThisUser: I18nString;
    /** %1$s competes in %2$s */
    xCompetesInY: I18nString;
    /** %1$s created team %2$s */
    xCreatedTeamY: I18nString;
    /** %1$s hosts %2$s */
    xHostsY: I18nString;
    /** %1$s invited you to "%2$s". */
    xInvitedYouToY: I18nString;
    /** %1$s is a free (%2$s), libre, no-ads, open source chess server. */
    xIsAFreeYLibreOpenSourceChessServer: I18nString;
    /** %1$s joined team %2$s */
    xJoinedTeamY: I18nString;
    /** %1$s joins %2$s */
    xJoinsY: I18nString;
    /** %1$s likes %2$s */
    xLikesY: I18nString;
    /** %1$s mentioned you in "%2$s". */
    xMentionedYouInY: I18nString;
    /** %s opening explorer */
    xOpeningExplorer: I18nString;
    /** %1$s posted in topic %2$s */
    xPostedInForumY: I18nString;
    /** %s rating */
    xRating: I18nString;
    /** %1$s started following %2$s */
    xStartedFollowingY: I18nString;
    /** %s started streaming */
    xStartedStreaming: I18nString;
    /** %s was played */
    xWasPlayed: I18nString;
    /** Yes */
    yes: I18nString;
    /** Yesterday */
    yesterday: I18nString;
    /** You are better than %1$s of %2$s players. */
    youAreBetterThanPercentOfPerfTypePlayers: I18nString;
    /** You are leaving Lichess */
    youAreLeavingLichess: I18nString;
    /** You are not in the team %s */
    youAreNotInTeam: I18nString;
    /** You are now part of the team. */
    youAreNowPartOfTeam: I18nString;
    /** You are playing! */
    youArePlaying: I18nString;
    /** You browsed away */
    youBrowsedAway: I18nString;
    /** Scroll over the board to move in the game. */
    youCanAlsoScrollOverTheBoardToMoveInTheGame: I18nString;
    /** You can do better */
    youCanDoBetter: I18nString;
    /** There is a setting to hide all user flairs across the entire site. */
    youCanHideFlair: I18nString;
    /** You can't post in the forums yet. Play some games! */
    youCannotPostYetPlaySomeGames: I18nString;
    /** You can't start a new game until this one is finished. */
    youCantStartNewGame: I18nString;
    /** You do not have an established %s rating. */
    youDoNotHaveAnEstablishedPerfTypeRating: I18nString;
    /** You have been timed out. */
    youHaveBeenTimedOut: I18nString;
    /** You have joined "%1$s". */
    youHaveJoinedTeamX: I18nString;
    /** You need an account to do that */
    youNeedAnAccountToDoThat: I18nString;
    /** You play the black pieces */
    youPlayTheBlackPieces: I18nString;
    /** You play the white pieces */
    youPlayTheWhitePieces: I18nString;
    /** Your opponent offers a draw */
    yourOpponentOffersADraw: I18nString;
    /** Your opponent proposes a takeback */
    yourOpponentProposesATakeback: I18nString;
    /** Your opponent wants to play a new game with you */
    yourOpponentWantsToPlayANewGameWithYou: I18nString;
    /** Your pending simuls */
    yourPendingSimuls: I18nString;
    /** Your %s rating is provisional */
    yourPerfRatingIsProvisional: I18nString;
    /** Your %1$s rating (%2$s) is too high */
    yourPerfRatingIsTooHigh: I18nString;
    /** Your %1$s rating (%2$s) is too low */
    yourPerfRatingIsTooLow: I18nString;
    /** Your %1$s rating is %2$s. */
    yourPerfTypeRatingIsRating: I18nString;
    /** Your question may already have an answer %1$s */
    yourQuestionMayHaveBeenAnswered: I18nString;
    /** Your rating */
    yourRating: I18nString;
    /** Your score: %s */
    yourScore: I18nString;
    /** Your top weekly %1$s rating (%2$s) is too high */
    yourTopWeeklyPerfRatingIsTooHigh: I18nString;
    /** Your turn */
    yourTurn: I18nString;
    /** Zero advertisement */
    zeroAdvertisement: I18nString;
  };
  storm: {
    /** Accuracy */
    accuracy: I18nString;
    /** All-time */
    allTime: I18nString;
    /** Best run of day */
    bestRunOfDay: I18nString;
    /** Click to reload */
    clickToReload: I18nString;
    /** Combo */
    combo: I18nString;
    /** Create a new game */
    createNewGame: I18nString;
    /** End run (hotkey: Enter) */
    endRun: I18nString;
    /** Failed puzzles */
    failedPuzzles: I18nString;
    /** Get ready! */
    getReady: I18nString;
    /** Highest solved */
    highestSolved: I18nString;
    /** Highscores */
    highscores: I18nString;
    /** Highscore: %s */
    highscoreX: I18nString;
    /** Join a public race */
    joinPublicRace: I18nString;
    /** Join rematch */
    joinRematch: I18nString;
    /** Join the race! */
    joinTheRace: I18nString;
    /** Moves */
    moves: I18nString;
    /** Move to start */
    moveToStart: I18nString;
    /** New all-time highscore! */
    newAllTimeHighscore: I18nString;
    /** New daily highscore! */
    newDailyHighscore: I18nString;
    /** New monthly highscore! */
    newMonthlyHighscore: I18nString;
    /** New run (hotkey: Space) */
    newRun: I18nString;
    /** New weekly highscore! */
    newWeeklyHighscore: I18nString;
    /** Next race */
    nextRace: I18nString;
    /** Play again */
    playAgain: I18nString;
    /** Played %1$s runs of %2$s */
    playedNbRunsOfPuzzleStorm: I18nPlural;
    /** Previous highscore was %s */
    previousHighscoreWasX: I18nString;
    /** Puzzles played */
    puzzlesPlayed: I18nString;
    /** puzzles solved */
    puzzlesSolved: I18nString;
    /** Race complete! */
    raceComplete: I18nString;
    /** Race your friends */
    raceYourFriends: I18nString;
    /** Runs */
    runs: I18nString;
    /** Score */
    score: I18nString;
    /** skip */
    skip: I18nString;
    /** Skip this move to preserve your combo! Only works once per race. */
    skipExplanation: I18nString;
    /** You can skip one move per race: */
    skipHelp: I18nString;
    /** Skipped puzzle */
    skippedPuzzle: I18nString;
    /** Slow puzzles */
    slowPuzzles: I18nString;
    /** Spectating */
    spectating: I18nString;
    /** Start the race */
    startTheRace: I18nString;
    /** This month */
    thisMonth: I18nString;
    /** This run has expired! */
    thisRunHasExpired: I18nString;
    /** This run was opened in another tab! */
    thisRunWasOpenedInAnotherTab: I18nString;
    /** This week */
    thisWeek: I18nString;
    /** Time */
    time: I18nString;
    /** Time per move */
    timePerMove: I18nString;
    /** View best runs */
    viewBestRuns: I18nString;
    /** Wait for rematch */
    waitForRematch: I18nString;
    /** Waiting for more players to join... */
    waitingForMorePlayers: I18nString;
    /** Waiting to start */
    waitingToStart: I18nString;
    /** %s runs */
    xRuns: I18nPlural;
    /** You play the black pieces in all puzzles */
    youPlayTheBlackPiecesInAllPuzzles: I18nString;
    /** You play the white pieces in all puzzles */
    youPlayTheWhitePiecesInAllPuzzles: I18nString;
    /** Your rank: %s */
    yourRankX: I18nString;
  };
  streamer: {
    /** All streamers */
    allStreamers: I18nString;
    /** Your stream is approved. */
    approved: I18nString;
    /** Become a Lichess streamer */
    becomeStreamer: I18nString;
    /** Change/delete your picture */
    changePicture: I18nString;
    /** Currently streaming: %s */
    currentlyStreaming: I18nString;
    /** Download streamer kit */
    downloadKit: I18nString;
    /** Do you have a Twitch or YouTube channel? */
    doYouHaveStream: I18nString;
    /** Edit streamer page */
    editPage: I18nString;
    /** Headline */
    headline: I18nString;
    /** Here we go! */
    hereWeGo: I18nString;
    /** Keep it short: %s characters max */
    keepItShort: I18nPlural;
    /** Last stream %s */
    lastStream: I18nString;
    /** Lichess streamer */
    lichessStreamer: I18nString;
    /** Lichess streamers */
    lichessStreamers: I18nString;
    /** LIVE! */
    live: I18nString;
    /** Long description */
    longDescription: I18nString;
    /** Max size: %s */
    maxSize: I18nString;
    /** OFFLINE */
    offline: I18nString;
    /** Optional. Leave empty if none */
    optionalOrEmpty: I18nString;
    /** Your stream is being reviewed by moderators. */
    pendingReview: I18nString;
    /** Get a flaming streamer icon on your Lichess profile. */
    perk1: I18nString;
    /** Get bumped up to the top of the streamers list. */
    perk2: I18nString;
    /** Notify your Lichess followers. */
    perk3: I18nString;
    /** Show your stream in your games, tournaments and studies. */
    perk4: I18nString;
    /** Benefits of streaming with the keyword */
    perks: I18nString;
    /** Please fill in your streamer information, and upload a picture. */
    pleaseFillIn: I18nString;
    /** request a moderator review */
    requestReview: I18nString;
    /** Include the keyword "lichess.org" in your stream title and use the category "Chess" when you stream on Lichess. */
    rule1: I18nString;
    /** Remove the keyword when you stream non-Lichess stuff. */
    rule2: I18nString;
    /** Lichess will detect your stream automatically and enable the following perks: */
    rule3: I18nString;
    /** Read our %s to ensure fair play for everyone during your stream. */
    rule4: I18nString;
    /** Streaming rules */
    rules: I18nString;
    /** The Lichess streamer page targets your audience with the language provided by your streaming platform. Set the correct default language for your chess streams in the app or service you use to broadcast. */
    streamerLanguageSettings: I18nString;
    /** Your streamer name on Lichess */
    streamerName: I18nString;
    /** streaming Fairplay FAQ */
    streamingFairplayFAQ: I18nString;
    /** Tell us about your stream in one sentence */
    tellUsAboutTheStream: I18nString;
    /** Your Twitch username or URL */
    twitchUsername: I18nString;
    /** Upload a picture */
    uploadPicture: I18nString;
    /** Visible on the streamers page */
    visibility: I18nString;
    /** When approved by moderators */
    whenApproved: I18nString;
    /** When you are ready to be listed as a Lichess streamer, %s */
    whenReady: I18nString;
    /** %s is streaming */
    xIsStreaming: I18nString;
    /** %s streamer picture */
    xStreamerPicture: I18nString;
    /** Your streamer page */
    yourPage: I18nString;
    /** Your YouTube channel ID */
    youTubeChannelId: I18nString;
  };
  study: {
    /** Add members */
    addMembers: I18nString;
    /** Add a new chapter */
    addNewChapter: I18nString;
    /** Allow cloning */
    allowCloning: I18nString;
    /** All studies */
    allStudies: I18nString;
    /** All SYNC members remain on the same position */
    allSyncMembersRemainOnTheSamePosition: I18nString;
    /** Alphabetical */
    alphabetical: I18nString;
    /** Analysis mode */
    analysisMode: I18nString;
    /** Annotate with glyphs */
    annotateWithGlyphs: I18nString;
    /** Attack */
    attack: I18nString;
    /** Automatic */
    automatic: I18nString;
    /** Back */
    back: I18nString;
    /** Black is better */
    blackIsBetter: I18nString;
    /** Black is slightly better */
    blackIsSlightlyBetter: I18nString;
    /** Black is winning */
    blackIsWinning: I18nString;
    /** Blunder */
    blunder: I18nString;
    /** Brilliant move */
    brilliantMove: I18nString;
    /** Chapter PGN */
    chapterPgn: I18nString;
    /** Chapter %s */
    chapterX: I18nString;
    /** Clear all comments, glyphs and drawn shapes in this chapter */
    clearAllCommentsInThisChapter: I18nString;
    /** Clear annotations */
    clearAnnotations: I18nString;
    /** Clear chat */
    clearChat: I18nString;
    /** Clear variations */
    clearVariations: I18nString;
    /** Clone */
    cloneStudy: I18nString;
    /** Comment on this move */
    commentThisMove: I18nString;
    /** Comment on this position */
    commentThisPosition: I18nString;
    /** Delete the entire study? There is no going back! Type the name of the study to confirm: %s */
    confirmDeleteStudy: I18nString;
    /** Contributor */
    contributor: I18nString;
    /** Contributors */
    contributors: I18nString;
    /** Copy PGN */
    copyChapterPgn: I18nString;
    /** Counterplay */
    counterplay: I18nString;
    /** Create chapter */
    createChapter: I18nString;
    /** Create study */
    createStudy: I18nString;
    /** Current chapter URL */
    currentChapterUrl: I18nString;
    /** Date added (newest) */
    dateAddedNewest: I18nString;
    /** Date added (oldest) */
    dateAddedOldest: I18nString;
    /** Delete chapter */
    deleteChapter: I18nString;
    /** Delete study */
    deleteStudy: I18nString;
    /** Delete the study chat history? There is no going back! */
    deleteTheStudyChatHistory: I18nString;
    /** Delete this chapter. There is no going back! */
    deleteThisChapter: I18nString;
    /** Development */
    development: I18nString;
    /** Download all games */
    downloadAllGames: I18nString;
    /** Download game */
    downloadGame: I18nString;
    /** Dubious move */
    dubiousMove: I18nString;
    /** Edit chapter */
    editChapter: I18nString;
    /** Editor */
    editor: I18nString;
    /** Edit study */
    editStudy: I18nString;
    /** Embed in your website */
    embedInYourWebsite: I18nString;
    /** Empty */
    empty: I18nString;
    /** Enable sync */
    enableSync: I18nString;
    /** Equal position */
    equalPosition: I18nString;
    /** Everyone */
    everyone: I18nString;
    /** First */
    first: I18nString;
    /** Get a full server-side computer analysis of the mainline. */
    getAFullComputerAnalysis: I18nString;
    /** Good move */
    goodMove: I18nString;
    /** Hide next moves */
    hideNextMoves: I18nString;
    /** Hot */
    hot: I18nString;
    /** Import from %s */
    importFromChapterX: I18nString;
    /** Initiative */
    initiative: I18nString;
    /** Interactive lesson */
    interactiveLesson: I18nString;
    /** Interesting move */
    interestingMove: I18nString;
    /** Invite only */
    inviteOnly: I18nString;
    /** Invite to the study */
    inviteToTheStudy: I18nString;
    /** Kick */
    kick: I18nString;
    /** Last */
    last: I18nString;
    /** Leave the study */
    leaveTheStudy: I18nString;
    /** Like */
    like: I18nString;
    /** Load games by URLs */
    loadAGameByUrl: I18nString;
    /** Load games from PGN */
    loadAGameFromPgn: I18nString;
    /** Load games from %1$s or %2$s */
    loadAGameFromXOrY: I18nString;
    /** Load a position from FEN */
    loadAPositionFromFen: I18nString;
    /** Make sure the chapter is complete. You can only request analysis once. */
    makeSureTheChapterIsComplete: I18nString;
    /** Manage topics */
    manageTopics: I18nString;
    /** Members */
    members: I18nString;
    /** Mistake */
    mistake: I18nString;
    /** Most popular */
    mostPopular: I18nString;
    /** My favourite studies */
    myFavoriteStudies: I18nString;
    /** My private studies */
    myPrivateStudies: I18nString;
    /** My public studies */
    myPublicStudies: I18nString;
    /** My studies */
    myStudies: I18nString;
    /** My topics */
    myTopics: I18nString;
    /** %s Chapters */
    nbChapters: I18nPlural;
    /** %s Games */
    nbGames: I18nPlural;
    /** %s Members */
    nbMembers: I18nPlural;
    /** New chapter */
    newChapter: I18nString;
    /** New tag */
    newTag: I18nString;
    /** Next */
    next: I18nString;
    /** Next chapter */
    nextChapter: I18nString;
    /** Nobody */
    nobody: I18nString;
    /** No: let people browse freely */
    noLetPeopleBrowseFreely: I18nString;
    /** None yet. */
    noneYet: I18nString;
    /** None */
    noPinnedComment: I18nString;
    /** Normal analysis */
    normalAnalysis: I18nString;
    /** Novelty */
    novelty: I18nString;
    /** Only the study contributors can request a computer analysis. */
    onlyContributorsCanRequestAnalysis: I18nString;
    /** Only me */
    onlyMe: I18nString;
    /** Only move */
    onlyMove: I18nString;
    /** Only public studies can be embedded! */
    onlyPublicStudiesCanBeEmbedded: I18nString;
    /** Open */
    open: I18nString;
    /** Orientation */
    orientation: I18nString;
    /** Paste your PGN text here, up to %s games */
    pasteYourPgnTextHereUpToNbGames: I18nPlural;
    /** %s per page */
    perPage: I18nString;
    /** PGN tags */
    pgnTags: I18nString;
    /** Pinned chapter comment */
    pinnedChapterComment: I18nString;
    /** Pinned study comment */
    pinnedStudyComment: I18nString;
    /** Play again */
    playAgain: I18nString;
    /** Playing */
    playing: I18nString;
    /** Please only invite people who know you, and who actively want to join this study. */
    pleaseOnlyInvitePeopleYouKnow: I18nString;
    /** Popular topics */
    popularTopics: I18nString;
    /** Previous chapter */
    prevChapter: I18nString;
    /** Previous */
    previous: I18nString;
    /** Private */
    private: I18nString;
    /** Public */
    public: I18nString;
    /** Read more about embedding */
    readMoreAboutEmbedding: I18nString;
    /** Recently updated */
    recentlyUpdated: I18nString;
    /** Right under the board */
    rightUnderTheBoard: I18nString;
    /** Save */
    save: I18nString;
    /** Save chapter */
    saveChapter: I18nString;
    /** Search by username */
    searchByUsername: I18nString;
    /** Share & export */
    shareAndExport: I18nString;
    /** Share changes with spectators and save them on the server */
    shareChanges: I18nString;
    /** Evaluation bars */
    showEvalBar: I18nString;
    /** Spectator */
    spectator: I18nString;
    /** Start */
    start: I18nString;
    /** Start at initial position */
    startAtInitialPosition: I18nString;
    /** Start at %s */
    startAtX: I18nString;
    /** Start from custom position */
    startFromCustomPosition: I18nString;
    /** Start from initial position */
    startFromInitialPosition: I18nString;
    /** Studies created by %s */
    studiesCreatedByX: I18nString;
    /** Studies I contribute to */
    studiesIContributeTo: I18nString;
    /** Study actions */
    studyActions: I18nString;
    /** Study not found */
    studyNotFound: I18nString;
    /** Study PGN */
    studyPgn: I18nString;
    /** Study URL */
    studyUrl: I18nString;
    /** The chapter is too short to be analysed. */
    theChapterIsTooShortToBeAnalysed: I18nString;
    /** Time trouble */
    timeTrouble: I18nString;
    /** Topics */
    topics: I18nString;
    /** Unclear position */
    unclearPosition: I18nString;
    /** Unlike */
    unlike: I18nString;
    /** Unlisted */
    unlisted: I18nString;
    /** URL of the games, one per line */
    urlOfTheGame: I18nString;
    /** Visibility */
    visibility: I18nString;
    /** What are studies? */
    whatAreStudies: I18nString;
    /** What would you play in this position? */
    whatWouldYouPlay: I18nString;
    /** Where do you want to study that? */
    whereDoYouWantToStudyThat: I18nString;
    /** White is better */
    whiteIsBetter: I18nString;
    /** White is slightly better */
    whiteIsSlightlyBetter: I18nString;
    /** White is winning */
    whiteIsWinning: I18nString;
    /** With compensation */
    withCompensation: I18nString;
    /** With the idea */
    withTheIdea: I18nString;
    /** %1$s, brought to you by %2$s */
    xBroughtToYouByY: I18nString;
    /** Yes: keep everyone on the same position */
    yesKeepEveryoneOnTheSamePosition: I18nString;
    /** You are now a contributor */
    youAreNowAContributor: I18nString;
    /** You are now a spectator */
    youAreNowASpectator: I18nString;
    /** You can paste this in the forum or your Lichess blog to embed */
    youCanPasteThisInTheForumToEmbed: I18nString;
    /** Congratulations! You completed this lesson. */
    youCompletedThisLesson: I18nString;
    /** Zugzwang */
    zugzwang: I18nString;
  };
  swiss: {
    /** Absences */
    absences: I18nString;
    /** Byes */
    byes: I18nString;
    /** Comparison */
    comparison: I18nString;
    /** Predefined max rounds, but duration unknown */
    durationUnknown: I18nString;
    /** Dutch system */
    dutchSystem: I18nString;
    /** In Swiss games, players cannot draw before 30 moves are played. While this measure cannot prevent pre-arranged draws, it at least makes it harder to agree to a draw on the fly. */
    earlyDrawsAnswer: I18nString;
    /** What happens with early draws? */
    earlyDrawsQ: I18nString;
    /** FIDE handbook */
    FIDEHandbook: I18nString;
    /** If this list is non-empty, then users absent from this list will be forbidden to join. One username per line. */
    forbiddedUsers: I18nString;
    /** Forbidden pairings */
    forbiddenPairings: I18nString;
    /** Usernames of players that must not play together (Siblings, for instance). Two usernames per line, separated by a space. */
    forbiddenPairingsHelp: I18nString;
    /** Forbidden */
    identicalForbidden: I18nString;
    /** Identical pairing */
    identicalPairing: I18nString;
    /** Join or create a team */
    joinOrCreateTeam: I18nString;
    /** Late join */
    lateJoin: I18nString;
    /** Yes, until more than half the rounds have started; for example in a 11-rounds Swiss, players can join before round 6 starts and in a 12-rounds before round 7 starts. */
    lateJoinA: I18nString;
    /** Can players late-join? */
    lateJoinQ: I18nString;
    /** Yes until more than half the rounds have started */
    lateJoinUntil: I18nString;
    /** Manual pairings in next round */
    manualPairings: I18nString;
    /** Specify all pairings of the next round manually. One player pair per line. Example: */
    manualPairingsHelp: I18nString;
    /** When all possible pairings have been played, the tournament will be ended and a winner declared. */
    moreRoundsThanPlayersA: I18nString;
    /** What happens if the tournament has more rounds than players? */
    moreRoundsThanPlayersQ: I18nString;
    /** Must have played their last swiss game */
    mustHavePlayedTheirLastSwissGame: I18nString;
    /** Only let players join if they have played their last swiss game. If they failed to show up in a recent swiss event, they won't be able to enter yours. This results in a better swiss experience for the players who actually show up. */
    mustHavePlayedTheirLastSwissGameHelp: I18nString;
    /** %s rounds */
    nbRounds: I18nPlural;
    /** New Swiss tournament */
    newSwiss: I18nString;
    /** Next round */
    nextRound: I18nString;
    /** Now playing */
    nowPlaying: I18nString;
    /** A player gets a bye of one point every time the pairing system can't find a pairing for them. */
    numberOfByesA: I18nString;
    /** How many byes can a player get? */
    numberOfByesQ: I18nString;
    /** Number of games */
    numberOfGames: I18nString;
    /** As many as can be played in the allotted duration */
    numberOfGamesAsManyAsPossible: I18nString;
    /** Decided in advance, same for all players */
    numberOfGamesPreDefined: I18nString;
    /** Number of rounds */
    numberOfRounds: I18nString;
    /** An odd number of rounds allows optimal colour balance. */
    numberOfRoundsHelp: I18nString;
    /** One round every %s days */
    oneRoundEveryXDays: I18nPlural;
    /** Ongoing games */
    ongoingGames: I18nPlural;
    /** We don't plan to add more tournament systems to Lichess at the moment. */
    otherSystemsA: I18nString;
    /** What about other tournament systems? */
    otherSystemsQ: I18nString;
    /** With the %1$s, implemented by %2$s, in accordance with the %3$s. */
    pairingsA: I18nString;
    /** How are pairings decided? */
    pairingsQ: I18nString;
    /** Pairing system */
    pairingSystem: I18nString;
    /** Any available opponent with similar ranking */
    pairingSystemArena: I18nString;
    /** Best pairing based on points and tie breaks */
    pairingSystemSwiss: I18nString;
    /** Pairing wait time */
    pairingWaitTime: I18nString;
    /** Fast: doesn't wait for all players */
    pairingWaitTimeArena: I18nString;
    /** Slow: waits for all players */
    pairingWaitTimeSwiss: I18nString;
    /** Pause */
    pause: I18nString;
    /** Yes but might reduce the number of rounds */
    pauseSwiss: I18nString;
    /** Play your games */
    playYourGames: I18nString;
    /** A win is worth one point, a draw is a half point, and a loss is zero points. */
    pointsCalculationA: I18nString;
    /** How are points calculated? */
    pointsCalculationQ: I18nString;
    /** Possible, but not consecutive */
    possibleButNotConsecutive: I18nString;
    /** Predefined duration in minutes */
    predefinedDuration: I18nString;
    /** Only allow pre-defined users to join */
    predefinedUsers: I18nString;
    /** Players who sign up for Swiss events but don't play their games can be problematic. */
    protectionAgainstNoShowA: I18nString;
    /** What is done regarding no-shows? */
    protectionAgainstNoShowQ: I18nString;
    /** Swiss tournaments were not designed for online chess. They demand punctuality, dedication and patience from players. */
    restrictedToTeamsA: I18nString;
    /** Why is it restricted to teams? */
    restrictedToTeamsQ: I18nString;
    /** Interval between rounds */
    roundInterval: I18nString;
    /** We'd like to add it, but unfortunately Round Robin doesn't work online. */
    roundRobinA: I18nString;
    /** What about Round Robin? */
    roundRobinQ: I18nString;
    /** Rounds are started manually */
    roundsAreStartedManually: I18nString;
    /** Similar to OTB tournaments */
    similarToOTB: I18nString;
    /** Sonneborn–Berger score */
    sonnebornBergerScore: I18nString;
    /** Starting in */
    startingIn: I18nString;
    /** Starting soon */
    startingSoon: I18nString;
    /** Streaks and Berserk */
    streaksAndBerserk: I18nString;
    /** Swiss */
    swiss: I18nString;
    /** In a Swiss tournament %1$s, each competitor does not necessarily play all other entrants. Competitors meet one-on-one in each round and are paired using a set of rules designed to ensure that each competitor plays opponents with a similar running score, but not the same opponent more than once. The winner is the competitor with the highest aggregate points earned in all rounds. All competitors play in each round unless there is an odd number of players. */
    swissDescription: I18nString;
    /** Swiss tournaments */
    swissTournaments: I18nString;
    /** In a Swiss tournament, all participants play the same number of games, and can only play each other once. */
    swissVsArenaA: I18nString;
    /** When to use Swiss tournaments instead of arenas? */
    swissVsArenaQ: I18nString;
    /** Swiss tournaments can only be created by team leaders, and can only be played by team members. */
    teamOnly: I18nString;
    /** Tie Break */
    tieBreak: I18nString;
    /** With the %s. */
    tiebreaksCalculationA: I18nString;
    /** How are tie breaks calculated? */
    tiebreaksCalculationQ: I18nString;
    /** Duration of the tournament */
    tournDuration: I18nString;
    /** Tournament start date */
    tournStartDate: I18nString;
    /** Unlimited and free */
    unlimitedAndFree: I18nString;
    /** View all %s rounds */
    viewAllXRounds: I18nPlural;
    /** Their clock will tick, they will flag, and lose the game. */
    whatIfOneDoesntPlayA: I18nString;
    /** What happens if a player doesn't play a game? */
    whatIfOneDoesntPlayQ: I18nString;
    /** No. They're complementary features. */
    willSwissReplaceArenasA: I18nString;
    /** Will Swiss replace arena tournaments? */
    willSwissReplaceArenasQ: I18nString;
    /** %s minutes between rounds */
    xMinutesBetweenRounds: I18nPlural;
    /** %s rounds Swiss */
    xRoundsSwiss: I18nPlural;
    /** %s seconds between rounds */
    xSecondsBetweenRounds: I18nPlural;
  };
  team: {
    /** All teams */
    allTeams: I18nString;
    /** Battle of %s teams */
    battleOfNbTeams: I18nPlural;
    /** Your join request is being reviewed by a team leader. */
    beingReviewed: I18nString;
    /** Close team */
    closeTeam: I18nString;
    /** Closes the team forever. */
    closeTeamDescription: I18nString;
    /** Completed tournaments */
    completedTourns: I18nString;
    /** Declined Requests */
    declinedRequests: I18nString;
    /** Team entry code */
    entryCode: I18nString;
    /** (Optional) An entry code that new members must know to join this team. */
    entryCodeDescriptionForLeader: I18nString;
    /** Incorrect entry code. */
    incorrectEntryCode: I18nString;
    /** Inner team */
    innerTeam: I18nString;
    /** Join the official %s team for news and events */
    joinLichessVariantTeam: I18nString;
    /** Join team */
    joinTeam: I18nString;
    /** Kick someone out of the team */
    kickSomeone: I18nString;
    /** Leaders chat */
    leadersChat: I18nString;
    /** Leader teams */
    leaderTeams: I18nString;
    /** List the teams that will compete in this battle. */
    listTheTeamsThatWillCompete: I18nString;
    /** Manually review admission requests */
    manuallyReviewAdmissionRequests: I18nString;
    /** If checked, players will need to write a request to join the team, which you can decline or accept. */
    manuallyReviewAdmissionRequestsHelp: I18nString;
    /** Message all members */
    messageAllMembers: I18nString;
    /** Send a private message to ALL members of the team. */
    messageAllMembersLongDescription: I18nString;
    /** Send a private message to every member of the team */
    messageAllMembersOverview: I18nString;
    /** My teams */
    myTeams: I18nString;
    /** %s leaders per team */
    nbLeadersPerTeam: I18nPlural;
    /** %s members */
    nbMembers: I18nPlural;
    /** New team */
    newTeam: I18nString;
    /** No team found */
    noTeamFound: I18nString;
    /** Number of leaders per team. The sum of their score is the score of the team. */
    numberOfLeadsPerTeam: I18nString;
    /** You really shouldn't change this value after the tournament has started! */
    numberOfLeadsPerTeamHelp: I18nString;
    /** One team per line. Use the auto-completion. */
    oneTeamPerLine: I18nString;
    /** You can copy-paste this list from a tournament to another! */
    oneTeamPerLineHelp: I18nString;
    /** Please add a new team leader before leaving, or close the team. */
    onlyLeaderLeavesTeam: I18nString;
    /** Leave team */
    quitTeam: I18nString;
    /** Your join request was declined by a team leader. */
    requestDeclined: I18nString;
    /** Subscribe to team messages */
    subToTeamMessages: I18nString;
    /** A Swiss tournament that only members of your team can join */
    swissTournamentOverview: I18nString;
    /** Team */
    team: I18nString;
    /** This team already exists. */
    teamAlreadyExists: I18nString;
    /** Team Battle */
    teamBattle: I18nString;
    /** A battle of multiple teams, each player scores points for their team */
    teamBattleOverview: I18nString;
    /** Team leaders */
    teamLeaders: I18nPlural;
    /** Team page */
    teamPage: I18nString;
    /** Recent members */
    teamRecentMembers: I18nString;
    /** Teams */
    teams: I18nString;
    /** Teams I lead */
    teamsIlead: I18nString;
    /** Team tournament */
    teamTournament: I18nString;
    /** An Arena tournament that only members of your team can join */
    teamTournamentOverview: I18nString;
    /** This tournament is over, and the teams can no longer be updated. */
    thisTeamBattleIsOver: I18nString;
    /** Upcoming tournaments */
    upcomingTournaments: I18nString;
    /** Who do you want to kick out of the team? */
    whoToKick: I18nString;
    /** Your join request will be reviewed by a team leader. */
    willBeReviewed: I18nString;
    /** %s join requests */
    xJoinRequests: I18nPlural;
    /** You may want to link one of these upcoming tournaments? */
    youWayWantToLinkOneOfTheseTournaments: I18nString;
  };
  tfa: {
    /** Authentication code */
    authenticationCode: I18nString;
    /** Disable two-factor authentication */
    disableTwoFactor: I18nString;
    /** Enable two-factor authentication */
    enableTwoFactor: I18nString;
    /** Enter your password and the authentication code generated by the app to complete the setup. You will need an authentication code every time you log in. */
    enterPassword: I18nString;
    /** If you cannot scan the code, enter the secret key %s into your app. */
    ifYouCannotScanEnterX: I18nString;
    /** Note: If you lose access to your two-factor authentication codes, you can do a %s via email. */
    ifYouLoseAccessTwoFactor: I18nString;
    /** Open the two-factor authentication app on your device to view your authentication code and verify your identity. */
    openTwoFactorApp: I18nString;
    /** Scan the QR code with the app. */
    scanTheCode: I18nString;
    /** Please enable two-factor authentication to secure your account at https://lichess.org/account/twofactor. */
    setupReminder: I18nString;
    /** Get an app for two-factor authentication. We recommend the following apps: */
    twoFactorAppRecommend: I18nString;
    /** Two-factor authentication */
    twoFactorAuth: I18nString;
    /** Two-factor authentication enabled */
    twoFactorEnabled: I18nString;
    /** Two-factor authentication adds another layer of security to your account. */
    twoFactorHelp: I18nString;
    /** You need your password and an authentication code from your authenticator app to disable two-factor authentication. */
    twoFactorToDisable: I18nString;
  };
  timeago: {
    /** completed */
    completed: I18nString;
    /** in %s days */
    inNbDays: I18nPlural;
    /** in %s hours */
    inNbHours: I18nPlural;
    /** in %s minutes */
    inNbMinutes: I18nPlural;
    /** in %s months */
    inNbMonths: I18nPlural;
    /** in %s seconds */
    inNbSeconds: I18nPlural;
    /** in %s weeks */
    inNbWeeks: I18nPlural;
    /** in %s years */
    inNbYears: I18nPlural;
    /** just now */
    justNow: I18nString;
    /** %s days ago */
    nbDaysAgo: I18nPlural;
    /** %s hours ago */
    nbHoursAgo: I18nPlural;
    /** %s hours remaining */
    nbHoursRemaining: I18nPlural;
    /** %s minutes ago */
    nbMinutesAgo: I18nPlural;
    /** %s minutes remaining */
    nbMinutesRemaining: I18nPlural;
    /** %s months ago */
    nbMonthsAgo: I18nPlural;
    /** %s weeks ago */
    nbWeeksAgo: I18nPlural;
    /** %s years ago */
    nbYearsAgo: I18nPlural;
    /** right now */
    rightNow: I18nString;
  };
  tourname: {
    /** Classical Shield */
    classicalShield: I18nString;
    /** Classical Shield Arena */
    classicalShieldArena: I18nString;
    /** Daily Classical */
    dailyClassical: I18nString;
    /** Daily Classical Arena */
    dailyClassicalArena: I18nString;
    /** Daily Rapid */
    dailyRapid: I18nString;
    /** Daily Rapid Arena */
    dailyRapidArena: I18nString;
    /** Daily %s */
    dailyX: I18nString;
    /** Daily %s Arena */
    dailyXArena: I18nString;
    /** Eastern Classical */
    easternClassical: I18nString;
    /** Eastern Classical Arena */
    easternClassicalArena: I18nString;
    /** Eastern Rapid */
    easternRapid: I18nString;
    /** Eastern Rapid Arena */
    easternRapidArena: I18nString;
    /** Eastern %s */
    easternX: I18nString;
    /** Eastern %s Arena */
    easternXArena: I18nString;
    /** Elite %s */
    eliteX: I18nString;
    /** Elite %s Arena */
    eliteXArena: I18nString;
    /** Hourly Rapid */
    hourlyRapid: I18nString;
    /** Hourly Rapid Arena */
    hourlyRapidArena: I18nString;
    /** Hourly %s */
    hourlyX: I18nString;
    /** Hourly %s Arena */
    hourlyXArena: I18nString;
    /** Monthly Classical */
    monthlyClassical: I18nString;
    /** Monthly Classical Arena */
    monthlyClassicalArena: I18nString;
    /** Monthly Rapid */
    monthlyRapid: I18nString;
    /** Monthly Rapid Arena */
    monthlyRapidArena: I18nString;
    /** Monthly %s */
    monthlyX: I18nString;
    /** Monthly %s Arena */
    monthlyXArena: I18nString;
    /** Rapid Shield */
    rapidShield: I18nString;
    /** Rapid Shield Arena */
    rapidShieldArena: I18nString;
    /** Weekly Classical */
    weeklyClassical: I18nString;
    /** Weekly Classical Arena */
    weeklyClassicalArena: I18nString;
    /** Weekly Rapid */
    weeklyRapid: I18nString;
    /** Weekly Rapid Arena */
    weeklyRapidArena: I18nString;
    /** Weekly %s */
    weeklyX: I18nString;
    /** Weekly %s Arena */
    weeklyXArena: I18nString;
    /** %s Arena */
    xArena: I18nString;
    /** %s Shield */
    xShield: I18nString;
    /** %s Shield Arena */
    xShieldArena: I18nString;
    /** %s Team Battle */
    xTeamBattle: I18nString;
    /** Yearly Classical */
    yearlyClassical: I18nString;
    /** Yearly Classical Arena */
    yearlyClassicalArena: I18nString;
    /** Yearly Rapid */
    yearlyRapid: I18nString;
    /** Yearly Rapid Arena */
    yearlyRapidArena: I18nString;
    /** Yearly %s */
    yearlyX: I18nString;
    /** Yearly %s Arena */
    yearlyXArena: I18nString;
  };
  ublog: {
    /** Our simple tips to write great blog posts */
    blogTips: I18nString;
    /** Blog topics */
    blogTopics: I18nString;
    /** Community blogs */
    communityBlogs: I18nString;
    /** Continue reading this post */
    continueReadingPost: I18nString;
    /** Enable comments */
    createBlogDiscussion: I18nString;
    /** A forum topic will be created for people to comment on your post */
    createBlogDiscussionHelp: I18nString;
    /** Delete this blog post definitively */
    deleteBlog: I18nString;
    /** Discuss this blog post in the forum */
    discussThisBlogPostInTheForum: I18nString;
    /** Drafts */
    drafts: I18nString;
    /** Edit your blog post */
    editYourBlogPost: I18nString;
    /** Friends blogs */
    friendBlogs: I18nString;
    /** Image alternative text */
    imageAlt: I18nString;
    /** Image credit */
    imageCredit: I18nString;
    /** Anything inappropriate could get your account closed. */
    inappropriateContentAccountClosed: I18nString;
    /** Latest blog posts */
    latestBlogPosts: I18nString;
    /** Lichess blog posts in %s */
    lichessBlogPostsFromXYear: I18nString;
    /** Lichess Official Blog */
    lichessOfficialBlog: I18nString;
    /** Liked blog posts */
    likedBlogs: I18nString;
    /** More blog posts by %s */
    moreBlogPostsBy: I18nString;
    /** %s views */
    nbViews: I18nPlural;
    /** New post */
    newPost: I18nString;
    /** No drafts to show. */
    noDrafts: I18nString;
    /** No posts in this blog, yet. */
    noPostsInThisBlogYet: I18nString;
    /** Post body */
    postBody: I18nString;
    /** Post intro */
    postIntro: I18nString;
    /** Post title */
    postTitle: I18nString;
    /** Previous blog posts */
    previousBlogPosts: I18nString;
    /** Published */
    published: I18nString;
    /** Published %s blog posts */
    publishedNbBlogPosts: I18nPlural;
    /** If checked, the post will be listed on your blog. If not, it will be private, in your draft posts */
    publishHelp: I18nString;
    /** Publish on your blog */
    publishOnYourBlog: I18nString;
    /** Please only post safe and respectful content. Do not copy someone else's content. */
    safeAndRespectfulContent: I18nString;
    /** It is safe to use images from the following websites: */
    safeToUseImages: I18nString;
    /** Save draft */
    saveDraft: I18nString;
    /** Select the topics your post is about */
    selectPostTopics: I18nString;
    /** This is a draft */
    thisIsADraft: I18nString;
    /** This post is published */
    thisPostIsPublished: I18nString;
    /** Upload an image for your post */
    uploadAnImageForYourPost: I18nString;
    /** You can also use images that you made yourself, pictures you took, screenshots of Lichess... anything that is not copyrighted by someone else. */
    useImagesYouMadeYourself: I18nString;
    /** View all %s posts */
    viewAllNbPosts: I18nPlural;
    /** %s's Blog */
    xBlog: I18nString;
    /** %1$s published %2$s */
    xPublishedY: I18nString;
    /** You are blocked by the blog author. */
    youBlockedByBlogAuthor: I18nString;
  };
  voiceCommands: {
    /** Cancel timer or deny a request */
    cancelTimerOrDenyARequest: I18nString;
    /** Castle (either side) */
    castle: I18nString;
    /** Use the %1$s button to toggle voice recognition, the %2$s button to open this help dialog, and the %3$s menu to change speech settings. */
    instructions1: I18nString;
    /** We show arrows for multiple moves when we are not sure. Speak the colour or number of a move arrow to select it. */
    instructions2: I18nString;
    /** If an arrow shows a sweeping radar, that move will be played when the circle is complete. During this time, you may only say %1$s to play the move immediately, %2$s to cancel, or speak the colour/number of a different arrow. This timer can be adjusted or turned off in settings. */
    instructions3: I18nString;
    /** Enable %s in noisy surroundings. Hold shift while speaking commands when this is on. */
    instructions4: I18nString;
    /** Use the phonetic alphabet to improve recognition of chessboard files. */
    instructions5: I18nString;
    /** %s explains the voice move settings in detail. */
    instructions6: I18nString;
    /** Move to e4 or select e4 piece */
    moveToE4OrSelectE4Piece: I18nString;
    /** Phonetic alphabet is best */
    phoneticAlphabetIsBest: I18nString;
    /** Play preferred move or confirm something */
    playPreferredMoveOrConfirmSomething: I18nString;
    /** Select or capture a bishop */
    selectOrCaptureABishop: I18nString;
    /** Show puzzle solution */
    showPuzzleSolution: I18nString;
    /** Sleep (if wake word enabled) */
    sleep: I18nString;
    /** Take rook with queen */
    takeRookWithQueen: I18nString;
    /** This blog post */
    thisBlogPost: I18nString;
    /** Turn off voice recognition */
    turnOffVoiceRecognition: I18nString;
    /** Voice commands */
    voiceCommands: I18nString;
    /** Watch the video tutorial */
    watchTheVideoTutorial: I18nString;
  };
}
