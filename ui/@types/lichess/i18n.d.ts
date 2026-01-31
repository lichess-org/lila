// Generated
interface I18nFormat {
  (...args: (string | number)[]): string; // formatted
  asArray: <T>(...args: T[]) => (T | string)[]; // vdom
}
interface I18nPlural {
  (quantity: number, ...args: (string | number)[]): string; // pluralSame
  asArray: <T>(quantity: number, ...args: T[]) => (T | string)[]; // vdomPlural / plural
}
interface I18n {
  /** global noarg key lookup */
  (key: string): string;
  quantity: (count: number) => 'zero' | 'one' | 'two' | 'few' | 'many' | 'other';

  activity: {
    /** Activity */
    activity: string;
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
    hostedALiveStream: string;
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
    rankedInSwissTournament: I18nFormat;
    /** Ranked #%1$s (top %2$s%%) with %3$s games in %4$s */
    rankedInTournament: I18nPlural;
    /** Signed up to lichess.org */
    signedUp: string;
    /** Solved %s training puzzles */
    solvedNbPuzzles: I18nPlural;
    /** Supported lichess.org for %1$s months as a %2$s */
    supportedNbMonths: I18nPlural;
  };
  appeal: {
    /** Your account is muted. */
    accountMuted: string;
    /** Read our %s. Failure to comply with the communication guidelines may result in accounts being muted. */
    accountMutedInfo: I18nFormat;
    /** Your account is banned from joining arenas. */
    arenaBanned: string;
    /** blog rules */
    blogRules: string;
    /** Your account is marked for rating manipulation. */
    boosterMarked: string;
    /** We define this as deliberately manipulating rating by losing games on purpose or by playing against another account that is deliberately losing games. */
    boosterMarkedInfo: string;
    /** Your account is not marked or restricted. You're all good! */
    cleanAllGood: string;
    /** Your account was closed by moderators. */
    closedByModerators: string;
    /** communication guidelines */
    communicationGuidelines: string;
    /** Your account is marked for external assistance in games. */
    engineMarked: string;
    /** We define this as using any external help to reinforce your knowledge and/or calculation skills in order to gain an unfair advantage over your opponent. See the %s page for more details. */
    engineMarkedInfo: I18nFormat;
    /** Your account has been excluded from leaderboards. */
    excludedFromLeaderboards: string;
    /** We define this as using any unfair way to get on the leaderboard. */
    excludedFromLeaderboardsInfo: string;
    /** Fair Play */
    fairPlay: string;
    /** Your blogs have been hidden by moderators. */
    hiddenBlog: string;
    /** Make sure to read again our %s. */
    hiddenBlogInfo: I18nFormat;
    /** You have a play timeout. */
    playTimeout: string;
    /** Your account is banned from tournaments with real prizes. */
    prizeBanned: string;
  };
  arena: {
    /** All averages on this page are %s. */
    allAveragesAreX: I18nFormat;
    /** Allow Berserk */
    allowBerserk: string;
    /** Let players halve their clock time to gain an extra point */
    allowBerserkHelp: string;
    /** Let players discuss in a chat room */
    allowChatHelp: string;
    /** Arena */
    arena: string;
    /** Arena streaks */
    arenaStreaks: string;
    /** After 2 wins, consecutive wins grant 4 points instead of 2. */
    arenaStreaksHelp: string;
    /** Arena tournaments */
    arenaTournaments: string;
    /** Average performance */
    averagePerformance: string;
    /** Average score */
    averageScore: string;
    /** Arena Berserk */
    berserk: string;
    /** When a player clicks the Berserk button at the beginning of the game, they lose half of their clock time, but the win is worth one extra tournament point. */
    berserkAnswer: string;
    /** Berserk rate */
    berserkRate: string;
    /** Best results */
    bestResults: string;
    /** Created */
    created: string;
    /** Custom start date */
    customStartDate: string;
    /** In your own local timezone. This overrides the "Time before tournament starts" setting */
    customStartDateHelp: string;
    /** Defender */
    defender: string;
    /** Drawing the game within the first %s moves will earn neither player any points. */
    drawingWithinNbMoves: I18nPlural;
    /** Draw streaks: When a player has consecutive draws in an arena, only the first draw will result in a point or draws lasting more than %s moves in standard games. The draw streak can only be broken by a win, not a loss or a draw. */
    drawStreakStandard: I18nFormat;
    /** The minimum game length for drawn games to award points differs by variant. The table below lists the threshold for each variant. */
    drawStreakVariants: string;
    /** Edit team battle */
    editTeamBattle: string;
    /** Edit tournament */
    editTournament: string;
    /** Arena History */
    history: string;
    /** How are scores calculated? */
    howAreScoresCalculated: string;
    /** A win has a base score of 2 points, a draw 1 point, and a loss is worth no points. */
    howAreScoresCalculatedAnswer: string;
    /** How does it end? */
    howDoesItEnd: string;
    /** The tournament has a countdown clock. When it reaches zero, the tournament rankings are frozen, and the winner is announced. Games in progress must be finished, however, they don't count for the tournament. */
    howDoesItEndAnswer: string;
    /** How does the pairing work? */
    howDoesPairingWork: string;
    /** At the beginning of the tournament, players are paired based on their rating. */
    howDoesPairingWorkAnswer: string;
    /** How is the winner decided? */
    howIsTheWinnerDecided: string;
    /** The player(s) with the most points after the tournament's set time limit will be announced the winner(s). */
    howIsTheWinnerDecidedAnswer: string;
    /** Is it rated? */
    isItRated: string;
    /** This tournament is *not* rated and will *not* affect your rating. */
    isNotRated: string;
    /** This tournament is rated and will affect your rating. */
    isRated: string;
    /** medians */
    medians: string;
    /** Minimum game length */
    minimumGameLength: string;
    /** My tournaments */
    myTournaments: string;
    /** New Team Battle */
    newTeamBattle: string;
    /** No Arena streaks */
    noArenaStreaks: string;
    /** No Berserk allowed */
    noBerserkAllowed: string;
    /** Only titled players */
    onlyTitled: string;
    /** Require an official title to join the tournament */
    onlyTitledHelp: string;
    /** Other important rules */
    otherRules: string;
    /** Pick your team */
    pickYourTeam: string;
    /** Points average */
    pointsAvg: string;
    /** Points sum */
    pointsSum: string;
    /** Rank average */
    rankAvg: string;
    /** The rank average is a percentage of your ranking. Lower is better. */
    rankAvgHelp: string;
    /** Recently played */
    recentlyPlayed: string;
    /** Share this URL to let people join: %s */
    shareUrl: I18nFormat;
    /** Some tournaments are rated and will affect your rating. */
    someRated: string;
    /** There is a countdown for your first move. Failing to make a move within this time will forfeit the game to your opponent. */
    thereIsACountdown: string;
    /** This is a private tournament */
    thisIsPrivate: string;
    /** Total */
    total: string;
    /** The tournament pairings are now closed. */
    tournamentPairingsAreNowClosed: string;
    /** Tournament shields */
    tournamentShields: string;
    /** Tournament stats */
    tournamentStats: string;
    /** Tournament winners */
    tournamentWinners: string;
    /** Variant */
    variant: string;
    /** View all %s teams */
    viewAllXTeams: I18nPlural;
    /** Which team will you represent in this battle? */
    whichTeamWillYouRepresentInThisBattle: string;
    /** You will be notified when the tournament starts, so feel free to leave this tab idle. */
    willBeNotified: string;
    /** You must join one of these teams to participate! */
    youMustJoinOneOfTheseTeamsToParticipate: string;
  };
  broadcast: {
    /** About broadcasts */
    aboutBroadcasts: string;
    /** Add a round */
    addRound: string;
    /** Age */
    age: string;
    /** View all broadcasts by month */
    allBroadcastsByMonth: string;
    /** All teams */
    allTeams: string;
    /** This name will be automatically translated. */
    automaticallyTranslated: string;
    /** Back to live move */
    backToLiveMove: string;
    /** Boards */
    boards: string;
    /** Boards can be loaded with a source or via the %s */
    boardsCanBeLoaded: I18nFormat;
    /** Broadcast calendar */
    broadcastCalendar: string;
    /** Broadcasts */
    broadcasts: string;
    /** Community broadcast */
    communityBroadcast: string;
    /** Created and managed by %s. */
    createdAndManagedBy: I18nFormat;
    /** Current game URL */
    currentGameUrl: string;
    /** Keeping the default name will automatically translate it to all other languages. */
    defaultRoundNameHelp: string;
    /** Definitively delete the round and all its games. */
    definitivelyDeleteRound: string;
    /** Definitively delete the entire tournament, all its rounds and all its games. */
    definitivelyDeleteTournament: string;
    /** Delete all games of this round. The source will need to be active in order to re-create them. */
    deleteAllGamesOfThisRound: string;
    /** Delete this round */
    deleteRound: string;
    /** Delete this tournament */
    deleteTournament: string;
    /** Download all rounds */
    downloadAllRounds: string;
    /** Edit round study */
    editRoundStudy: string;
    /** Embed this broadcast in your website */
    embedThisBroadcast: string;
    /** Federation */
    federation: string;
    /** FIDE federations */
    fideFederations: string;
    /** FIDE player not found */
    fidePlayerNotFound: string;
    /** FIDE players */
    fidePlayers: string;
    /** FIDE profile */
    fideProfile: string;
    /** FIDE rating category */
    fideRatingCategory: string;
    /** Finals */
    finals: string;
    /** Full tournament description */
    fullDescription: string;
    /** Optional long description of the tournament. %1$s is available. Length must be less than %2$s characters. */
    fullDescriptionHelp: I18nFormat;
    /** Games in this tournament */
    gamesThisTournament: string;
    /** Game %s */
    gameX: I18nFormat;
    /** Girls */
    girlsTournament: string;
    /** Girls U%s */
    girlsUnderXAgeTournament: I18nFormat;
    /** How to use Lichess Broadcasts. */
    howToUseLichessBroadcasts: string;
    /** More options on the %s */
    iframeHelp: I18nFormat;
    /** Knockouts */
    knockouts: string;
    /** Live board */
    liveboard: string;
    /** Live tournament broadcasts */
    liveBroadcasts: string;
    /** My broadcasts */
    myBroadcasts: string;
    /** %s broadcasts */
    nbBroadcasts: I18nPlural;
    /** %s viewers */
    nbViewers: I18nPlural;
    /** New live broadcast */
    newBroadcast: string;
    /** No boards yet. These will appear once games are uploaded. */
    noBoardsYet: string;
    /** The broadcast has not yet started. */
    notYetStarted: string;
    /** Official Standings */
    officialStandings: string;
    /** Official website */
    officialWebsite: string;
    /** Ongoing */
    ongoing: string;
    /** Open in Lichess */
    openLichess: string;
    /** Open */
    openTournament: string;
    /** Open U%s */
    openUnderXAgeTournament: I18nFormat;
    /** Optional details */
    optionalDetails: string;
    /** Overview */
    overview: string;
    /** Past broadcasts */
    pastBroadcasts: string;
    /** Quarterfinals */
    quarterfinals: string;
    /** Rating diff */
    ratingDiff: string;
    /** Recent tournaments */
    recentTournaments: string;
    /** Optional: replace player names, ratings and titles */
    replacePlayerTags: string;
    /** Reset this round */
    resetRound: string;
    /** Round name */
    roundName: string;
    /** Round %s */
    roundX: I18nFormat;
    /** Score */
    score: string;
    /** Semifinals */
    semifinals: string;
    /** Show players scores based on game results */
    showScores: string;
    /** Since you chose to hide the results, all the preview boards are empty to avoid spoilers. */
    sinceHideResults: string;
    /** Up to 64 Lichess game IDs, separated by spaces. */
    sourceGameIds: string;
    /** PGN Source URL */
    sourceSingleUrl: string;
    /** URL that Lichess will check to get PGN updates. It must be publicly accessible from the Internet. */
    sourceUrlHelp: string;
    /** Optional, if you know when the event starts */
    startDateHelp: string;
    /** Start date in the tournament local timezone: %s */
    startDateTimeZone: I18nFormat;
    /** Starts after %s */
    startsAfter: I18nFormat;
    /** The broadcast will start very soon. */
    startVerySoon: string;
    /** Subscribed broadcasts */
    subscribedBroadcasts: string;
    /** Subscribe to be notified when each round starts. You can toggle bell or push notifications for broadcasts in your account preferences. */
    subscribeTitle: string;
    /** Teams */
    teams: string;
    /** The new round will have the same members and contributors as the previous one. */
    theNewRoundHelp: string;
    /** Tiebreaks */
    tiebreaks: string;
    /** Time zone */
    timezone: string;
    /** Top 10 rating */
    top10Rating: string;
    /** Top players */
    topPlayers: string;
    /** Short tournament description */
    tournamentDescription: string;
    /** Tournament format */
    tournamentFormat: string;
    /** Tournament Location */
    tournamentLocation: string;
    /** Tournament name */
    tournamentName: string;
    /** Unrated */
    unrated: string;
    /** Upcoming */
    upcoming: string;
    /** Upload tournament image */
    uploadImage: string;
    /** webmasters page */
    webmastersPage: string;
    /** Women */
    womenTournament: string;
  };
  challenge: {
    /** Cannot challenge due to provisional %s rating. */
    cannotChallengeDueToProvisionalXRating: I18nFormat;
    /** Challenge accepted! */
    challengeAccepted: string;
    /** Challenge cancelled. */
    challengeCanceled: string;
    /** Challenge declined. */
    challengeDeclined: string;
    /** Challenges: %1$s */
    challengesX: I18nFormat;
    /** Challenge */
    challengeToPlay: string;
    /** Please send me a casual challenge instead. */
    declineCasual: string;
    /** I'm not accepting challenges at the moment. */
    declineGeneric: string;
    /** This is not the right time for me, please ask again later. */
    declineLater: string;
    /** I'm not accepting challenges from bots. */
    declineNoBot: string;
    /** I'm only accepting challenges from bots. */
    declineOnlyBot: string;
    /** Please send me a rated challenge instead. */
    declineRated: string;
    /** I'm not accepting variant challenges right now. */
    declineStandard: string;
    /** I'm not accepting challenges with this time control. */
    declineTimeControl: string;
    /** This time control is too fast for me, please challenge again with a slower game. */
    declineTooFast: string;
    /** This time control is too slow for me, please challenge again with a faster game. */
    declineTooSlow: string;
    /** I'm not willing to play this variant right now. */
    declineVariant: string;
    /** Or invite a Lichess user: */
    inviteLichessUser: string;
    /** Please register to send challenges to this user. */
    registerToSendChallenges: string;
    /** %s does not accept challenges. */
    xDoesNotAcceptChallenges: I18nFormat;
    /** %s only accepts challenges from friends. */
    xOnlyAcceptsChallengesFromFriends: I18nFormat;
    /** You cannot challenge %s. */
    youCannotChallengeX: I18nFormat;
    /** Your %1$s rating is too far from %2$s. */
    yourXRatingIsTooFarFromY: I18nFormat;
  };
  class: {
    /** Add Lichess usernames to invite them as teachers. One per line. */
    addLichessUsernames: string;
    /** Add student */
    addStudent: string;
    /** A link to the class will be automatically added at the end of the message, so you don't need to include it yourself. */
    aLinkToTheClassWillBeAdded: string;
    /** Allow messages between students */
    allowMessagingBetweenStudents: string;
    /** This only applies to the accounts you have created for your students. */
    allowMessagingBetweenStudentsDesc: string;
    /** An invitation has been sent to %s */
    anInvitationHasBeenSentToX: I18nFormat;
    /** Apply to be a Lichess Teacher */
    applyToBeLichessTeacher: string;
    /** Class description */
    classDescription: string;
    /** Class name */
    className: string;
    /** Class news */
    classNews: string;
    /** Click the link to view the invitation: */
    clickToViewInvitation: string;
    /** Close class */
    closeClass: string;
    /** The student will never be able to use this account again. Closing is final. Make sure the student understands and agrees. */
    closeDesc1: string;
    /** You may want to give the student control over the account instead so that they can continue using it. */
    closeDesc2: string;
    /** Close account */
    closeStudent: string;
    /** Close the student account permanently. */
    closeTheAccount: string;
    /** Create a new Lichess account */
    createANewLichessAccount: string;
    /** If the student doesn't have a Lichess account yet, you can create one for them here. */
    createDesc1: string;
    /** No email address is required. A password will be generated, and you will have to transmit it to the student so that they can log in. */
    createDesc2: string;
    /** Important: a student must not have multiple accounts. */
    createDesc3: string;
    /** If they already have one, use the invite form instead. */
    createDesc4: string;
    /** create more classes */
    createMoreClasses: string;
    /** Create multiple Lichess accounts at once */
    createMultipleAccounts: string;
    /** Only create accounts for real students. Do not use this to make multiple accounts for yourself. You would get banned. */
    createStudentWarning: string;
    /** Declined */
    declined: string;
    /** Edit news */
    editNews: string;
    /** Expiration %s */
    expirationInMomentFromNow: I18nFormat;
    /** Features */
    features: string;
    /** 100% free for all, forever, with no ads or trackers */
    freeForAllForever: string;
    /** Generate a new password for the student */
    generateANewPassword: string;
    /** Generate a new username */
    generateANewUsername: string;
    /** You are invited to join the class "%s" as a student. */
    invitationToClass: I18nFormat;
    /** Invite */
    invite: string;
    /** Invite a Lichess account */
    inviteALichessAccount: string;
    /** If the student already has a Lichess account, you can invite them to the class. */
    inviteDesc1: string;
    /** They will receive a message on Lichess with a link to join the class. */
    inviteDesc2: string;
    /** Important: only invite students you know, and who actively want to join the class. */
    inviteDesc3: string;
    /** Never send unsolicited invites to arbitrary players. */
    inviteDesc4: string;
    /** Invited to %1$s by %2$s */
    invitedToXByY: I18nFormat;
    /** Invite the student back */
    inviteTheStudentBack: string;
    /** Active */
    lastActiveDate: string;
    /** Classes */
    lichessClasses: string;
    /** Lichess profile %1$s created for %2$s. */
    lichessProfileXCreatedForY: I18nFormat;
    /** Lichess username */
    lichessUsername: string;
    /** Make sure to copy or write down the password now. You won’t ever be able to see it again! */
    makeSureToCopy: string;
    /** Managed */
    managed: string;
    /** Note that a class can have up to %1$s students. To manage more students, %2$s. */
    maxStudentsNote: I18nFormat;
    /** Message all students about new class material */
    messageAllStudents: string;
    /** Move to another class */
    moveToAnotherClass: string;
    /** Move to %s */
    moveToClass: I18nFormat;
    /** You can also %s to create multiple Lichess accounts from a list of student names. */
    multipleAccsFormDescription: I18nFormat;
    /** N/A */
    na: string;
    /** %s pending invitations */
    nbPendingInvitations: I18nPlural;
    /** %s students */
    nbStudents: I18nPlural;
    /** %s teachers */
    nbTeachers: I18nPlural;
    /** New class */
    newClass: string;
    /** News */
    news: string;
    /** All class news in a single field. */
    newsEdit1: string;
    /** Add the recent news at the top. Don't delete previous news. */
    newsEdit2: string;
    /** Separate news with --- */
    newsEdit3: string;
    /** No classes yet. */
    noClassesYet: string;
    /** No removed students */
    noRemovedStudents: string;
    /** No students in the class, yet. */
    noStudents: string;
    /** Nothing here, yet. */
    nothingHere: string;
    /** Notify all students */
    notifyAllStudents: string;
    /** Only visible to the class teachers */
    onlyVisibleToTeachers: string;
    /** or */
    orSeparator: string;
    /** Over days */
    overDays: string;
    /** Overview */
    overview: string;
    /** Password: %s */
    passwordX: I18nFormat;
    /** Pending */
    pending: string;
    /** Private. Will never be shown outside the class. Helps you remember who the student is. */
    privateWillNeverBeShown: string;
    /** Progress */
    progress: string;
    /** Quick login code */
    quickLoginCode: string;
    /** Quick login codes */
    quickLoginCodes: string;
    /** Use these codes on %s to log your students into Lichess. */
    quickLoginCodesDesc1: I18nFormat;
    /** When the codes expire, your students will remain logged in, until they manually log out. */
    quickLoginCodesDesc2: string;
    /** Quickly generate safe usernames and passwords for students */
    quicklyGenerateSafeUsernames: string;
    /** Real name */
    realName: string;
    /** Real, unique email address of the student. We will send a confirmation email to it, with a link to graduate the account. */
    realUniqueEmail: string;
    /** Graduate */
    release: string;
    /** A graduated account cannot be managed again. The student will be able to toggle kid mode and reset password themselves. */
    releaseDesc1: string;
    /** The student will remain in the class after their account is graduated. */
    releaseDesc2: string;
    /** Graduate the account so the student can manage it autonomously. */
    releaseTheAccount: string;
    /** Removed by %s */
    removedByX: I18nFormat;
    /** Removed */
    removedStudents: string;
    /** Remove student */
    removeStudent: string;
    /** Reopen */
    reopen: string;
    /** Reset password */
    resetPassword: string;
    /** Send a message to all students. */
    sendAMessage: string;
    /** Student:  %1$s */
    studentCredentials: I18nFormat;
    /** Students */
    students: string;
    /** Students' real names, one per line */
    studentsRealNamesOnePerLine: string;
    /** Teach classes of chess students with the Lichess Classes tool suite. */
    teachClassesOfChessStudents: string;
    /** Teachers */
    teachers: string;
    /** Teachers of the class */
    teachersOfTheClass: string;
    /** Teachers: %s */
    teachersX: I18nFormat;
    /** This student account is managed */
    thisStudentAccountIsManaged: string;
    /** Time playing */
    timePlaying: string;
    /** Track student progress in games and puzzles */
    trackStudentProgress: string;
    /** Upgrade from managed to autonomous */
    upgradeFromManaged: string;
    /** use this form */
    useThisForm: string;
    /** %1$s over last %2$s */
    variantXOverLastY: I18nFormat;
    /** Visible by both teachers and students of the class */
    visibleByBothStudentsAndTeachers: string;
    /** Welcome to your class: %s. */
    welcomeToClass: I18nFormat;
    /** Win rate */
    winrate: string;
    /** %s already has a pending invitation */
    xAlreadyHasAPendingInvitation: I18nFormat;
    /** %1$s is a kid account and can't receive your message. You must give them the invitation URL manually: %2$s */
    xIsAKidAccountWarning: I18nFormat;
    /** %s is now a student of the class */
    xisNowAStudentOfTheClass: I18nFormat;
    /** You accepted this invitation. */
    youAcceptedThisInvitation: string;
    /** You declined this invitation. */
    youDeclinedThisInvitation: string;
    /** You have been invited by %s. */
    youHaveBeenInvitedByX: I18nFormat;
  };
  coach: {
    /** About me */
    aboutMe: string;
    /** Accepting students */
    accepting: string;
    /** All countries */
    allCountries: string;
    /** Are you a great chess coach with a %s? */
    areYouCoach: I18nFormat;
    /** Availability */
    availability: string;
    /** Best skills */
    bestSkills: string;
    /** Confirm your title here and we will review your application. */
    confirmTitle: string;
    /** Hourly rate */
    hourlyRate: string;
    /** Languages */
    languages: string;
    /** Last login */
    lastLogin: string;
    /** Lichess coach */
    lichessCoach: string;
    /** Lichess coaches */
    lichessCoaches: string;
    /** Lichess rating */
    lichessRating: string;
    /** Location */
    location: string;
    /** NM or FIDE title */
    nmOrFideTitle: string;
    /** Not accepting students at the moment */
    notAccepting: string;
    /** Other experiences */
    otherExperiences: string;
    /** Playing experience */
    playingExperience: string;
    /** Public studies */
    publicStudies: string;
    /** Rating */
    rating: string;
    /** Send us an email at %s and we will review your application. */
    sendApplication: I18nFormat;
    /** Send a private message */
    sendPM: string;
    /** Teaching experience */
    teachingExperience: string;
    /** Teaching methodology */
    teachingMethod: string;
    /** View %s Lichess profile */
    viewXProfile: I18nFormat;
    /** %s coaches chess students */
    xCoachesStudents: I18nFormat;
    /** YouTube videos */
    youtubeVideos: string;
  };
  contact: {
    /** However if you indeed used engine assistance, even just once, then your account is unfortunately lost. */
    accountLost: string;
    /** I need account support */
    accountSupport: string;
    /** Authorisation to use Lichess */
    authorizationToUse: string;
    /** Appeal for a ban or IP restriction */
    banAppeal: string;
    /** In certain circumstances when playing against a bot account, a rated game may not award points if it is determined that the player is abusing the bot for rating points. */
    botRatingAbuse: string;
    /** Buying Lichess */
    buyingLichess: string;
    /** It is called "en passant" and is one of the rules of chess. */
    calledEnPassant: string;
    /** We can't change more than the case. For technical reasons, it's downright impossible. */
    cantChangeMore: string;
    /** It's not possible to clear your game history, puzzle history, or ratings. */
    cantClearHistory: string;
    /** If you imported the game, or started it from a position, make sure you correctly set the castling rights. */
    castlingImported: string;
    /** Castling is only prevented if the king goes through a controlled square. */
    castlingPrevented: string;
    /** Make sure you understand the castling rules */
    castlingRules: string;
    /** Visit this page to change the case of your username */
    changeUsernameCase: string;
    /** You can close your account on this page */
    closeYourAccount: string;
    /** Collaboration, legal, commercial */
    collaboration: string;
    /** Contact */
    contact: string;
    /** Contact Lichess */
    contactLichess: string;
    /** Credit is appreciated but not required. */
    creditAppreciated: string;
    /** Do not ask us by email to close an account, we won't do it. */
    doNotAskByEmail: string;
    /** Do not ask us by email to reopen an account, we won't do it. */
    doNotAskByEmailToReopen: string;
    /** Do not deny having cheated. If you want to be allowed to create a new account, just admit to what you did, and show that you understood that it was a mistake. */
    doNotDeny: string;
    /** Please do not send direct messages to moderators. */
    doNotMessageModerators: string;
    /** Do not report players in the forum. */
    doNotReportInForum: string;
    /** Do not send us report emails. */
    doNotSendReportEmails: string;
    /** Complete a password reset to remove your second factor */
    doPasswordReset: string;
    /** Engine or cheat mark */
    engineAppeal: string;
    /** Error page */
    errorPage: string;
    /** Please explain your request clearly and thoroughly. State your Lichess username, and any information that could help us help you. */
    explainYourRequest: string;
    /** False positives do happen sometimes, and we're sorry about that. */
    falsePositives: string;
    /** According to the FIDE Laws of Chess §6.9, if a checkmate is possible with any legal sequence of moves, then the game is not a draw */
    fideMate: string;
    /** I forgot my password */
    forgotPassword: string;
    /** I forgot my username */
    forgotUsername: string;
    /** Please describe what the bug looks like, what you expected to happen instead, and the steps to reproduce the bug. */
    howToReportBug: string;
    /** I can't log in */
    iCantLogIn: string;
    /** If your appeal is legitimate, we will lift the ban ASAP. */
    ifLegit: string;
    /** Illegal or impossible castling */
    illegalCastling: string;
    /** Illegal pawn capture */
    illegalPawnCapture: string;
    /** Insufficient mating material */
    insufficientMaterial: string;
    /** It is possible to checkmate with only a knight or a bishop, if the opponent has more than a king on the board. */
    knightMate: string;
    /** Learn how to make your own broadcasts on Lichess */
    learnHowToMakeBroadcasts: string;
    /** I lost access to my two-factor authentication codes */
    lost2FA: string;
    /** Monetising Lichess */
    monetizing: string;
    /** I didn't receive my confirmation email */
    noConfirmationEmail: string;
    /** None of the above */
    noneOfTheAbove: string;
    /** No rating points were awarded */
    noRatingPoints: string;
    /** Only reporting players through the report form is effective. */
    onlyReports: string;
    /** However, you can close your current account, and create a new one. */
    orCloseAccount: string;
    /** Other restriction */
    otherRestriction: string;
    /** Make sure you played a rated game. Casual games do not affect the players ratings. */
    ratedGame: string;
    /** You can reopen your account on this page. */
    reopenOnThisPage: string;
    /** In the Lichess Discord server */
    reportBugInDiscord: string;
    /** In the Lichess Feedback section of the forum */
    reportBugInForum: string;
    /** If you faced an error page, you may report it: */
    reportErrorPage: string;
    /** As a Lichess mobile app issue on GitHub */
    reportMobileIssue: string;
    /** As a Lichess website issue on GitHub */
    reportWebsiteIssue: string;
    /** You may send an appeal to %s. */
    sendAppealTo: I18nFormat;
    /** Send us an email at %s. */
    sendEmailAt: I18nFormat;
    /** To report a player, use the report form */
    toReportAPlayerUseForm: string;
    /** Try this little interactive game to practice castling in chess */
    tryCastling: string;
    /** Try this little interactive game to learn more about "en passant". */
    tryEnPassant: string;
    /** You can show it in your videos, and you can print screenshots of Lichess in your books. */
    videosAndBooks: string;
    /** Visit this page to solve the issue */
    visitThisPage: string;
    /** To show your title on your Lichess profile, and participate in Titled Arenas, visit the title confirmation page */
    visitTitleConfirmation: string;
    /** I want to change my username */
    wantChangeUsername: string;
    /** I want to clear my history or rating */
    wantClearHistory: string;
    /** I want to close my account */
    wantCloseAccount: string;
    /** I want to reopen my account */
    wantReopen: string;
    /** I want to report a player */
    wantReport: string;
    /** I want to report a bug */
    wantReportBug: string;
    /** I want my title displayed on Lichess */
    wantTitle: string;
    /** You are welcome to use Lichess for your activity, even commercial. */
    welcomeToUse: string;
    /** What can we help you with? */
    whatCanWeHelpYouWith: string;
    /** You can also reach that page by clicking the %s report button on a profile page. */
    youCanAlsoReachReportPage: I18nFormat;
    /** You can login with the email address you signed up with */
    youCanLoginWithEmail: string;
  };
  coordinates: {
    /** A coordinate appears on the board and you must click on the corresponding square. */
    aCoordinateAppears: string;
    /** A square is highlighted on the board and you must enter its coordinate (e.g. "e4"). */
    aSquareIsHighlightedExplanation: string;
    /** Average score as black: %s */
    averageScoreAsBlackX: I18nFormat;
    /** Average score as white: %s */
    averageScoreAsWhiteX: I18nFormat;
    /** Coordinates */
    coordinates: string;
    /** Coordinate training */
    coordinateTraining: string;
    /** Find square */
    findSquare: string;
    /** Go as long as you want, there is no time limit! */
    goAsLongAsYouWant: string;
    /** Knowing the chessboard coordinates is a very important skill for several reasons: */
    knowingTheChessBoard: string;
    /** Most chess courses and exercises use the algebraic notation extensively. */
    mostChessCourses: string;
    /** Name square */
    nameSquare: string;
    /** Practice only some files & ranks */
    practiceOnlySomeFilesAndRanks: string;
    /** Show coordinates */
    showCoordinates: string;
    /** Coordinates on every square */
    showCoordsOnAllSquares: string;
    /** Show pieces */
    showPieces: string;
    /** Start training */
    startTraining: string;
    /** It makes it easier to talk to your chess friends, since you both understand the 'language of chess'. */
    talkToYourChessFriends: string;
    /** You can analyse a game more effectively if you can quickly recognise coordinates. */
    youCanAnalyseAGameMoreEffectively: string;
    /** You have 30 seconds to correctly map as many squares as possible! */
    youHaveThirtySeconds: string;
  };
  dgt: {
    /** Announce All Moves */
    announceAllMoves: string;
    /** Announce Move Format */
    announceMoveFormat: string;
    /** As a last resort, setup the board identically as Lichess, then %s */
    asALastResort: I18nFormat;
    /** The board will auto connect to any game that is already on course or any new game that starts. Ability to choose which game to play is coming soon. */
    boardWillAutoConnect: string;
    /** Check that you have made your opponent's move on the DGT board first. Revert your move. Play again. */
    checkYouHaveMadeOpponentsMove: string;
    /** Click to generate one */
    clickToGenerateOne: string;
    /** Configuration Section */
    configurationSection: string;
    /** Configure */
    configure: string;
    /** Configure voice narration of the played moves, so you can keep your eyes on the board. */
    configureVoiceNarration: string;
    /** Debug */
    debug: string;
    /** DGT board */
    dgtBoard: string;
    /** DGT board connectivity */
    dgtBoardConnectivity: string;
    /** DGT Board Limitations */
    dgtBoardLimitations: string;
    /** DGT Board Requirements */
    dgtBoardRequirements: string;
    /** DGT - Configure */
    dgtConfigure: string;
    /** A %s entry was added to your PLAY menu at the top. */
    dgtPlayMenuEntryAdded: I18nFormat;
    /** You can download the software here: %s. */
    downloadHere: I18nFormat;
    /** Enable Speech Synthesis */
    enableSpeechSynthesis: string;
    /** If %1$s is running on a different machine or different port, you will need to set the IP address and port here in the %2$s. */
    ifLiveChessRunningElsewhere: I18nFormat;
    /** If %1$s is running on this computer, you can check your connection to it by %2$s. */
    ifLiveChessRunningOnThisComputer: I18nFormat;
    /** If a move is not detected */
    ifMoveNotDetected: string;
    /** The play page needs to remain open on your browser. It does not need to be visible, you can minimize it or set it side to side with the Lichess game page, but don't close it or the board will stop working. */
    keepPlayPageOpen: string;
    /** Keywords are in JSON format. They are used to translate moves and results into your language. Default is English, but feel free to change it. */
    keywordFormatDescription: string;
    /** Keywords */
    keywords: string;
    /** Lichess & DGT */
    lichessAndDgt: string;
    /** Lichess connectivity */
    lichessConnectivity: string;
    /** SAN is the standard on Lichess like "Nf6". UCI is common on engines like "g8f6". */
    moveFormatDescription: string;
    /** No suitable OAuth token has been created. */
    noSuitableOauthToken: string;
    /** opening this link */
    openingThisLink: string;
    /** Play with a DGT board */
    playWithDgtBoard: string;
    /** Reload this page */
    reloadThisPage: string;
    /** Select YES to announce both your moves and your opponent's moves. Select NO to announce only your opponent's moves. */
    selectAnnouncePreference: string;
    /** Speech synthesis voice */
    speechSynthesisVoice: string;
    /** Text to speech */
    textToSpeech: string;
    /** This page allows you to connect your DGT board to Lichess, and to use it for playing games. */
    thisPageAllowsConnectingDgtBoard: string;
    /** Time controls for casual games: Classical, Correspondence and Rapid only. */
    timeControlsForCasualGames: string;
    /** Time controls for rated games: Classical, Correspondence and some Rapids including 15+10 and 20+0 */
    timeControlsForRatedGames: string;
    /** To connect to the DGT Electronic Board you will need to install %s. */
    toConnectTheDgtBoard: I18nFormat;
    /** To see console message press Command + Option + C (Mac) or Control + Shift + C (Windows, Linux, Chrome OS) */
    toSeeConsoleMessage: string;
    /** Use "%1$s" unless %2$s is running on a different machine or different port. */
    useWebSocketUrl: I18nFormat;
    /** You have an OAuth token suitable for DGT play. */
    validDgtOauthToken: string;
    /** Verbose logging */
    verboseLogging: string;
    /** %s WebSocket URL */
    webSocketUrl: I18nFormat;
    /** When ready, setup your board and then click %s. */
    whenReadySetupBoard: I18nFormat;
  };
  emails: {
    /** To contact us, please use %s. */
    common_contact: I18nFormat;
    /** This is a service email related to your use of %s. */
    common_note: I18nFormat;
    /** (Clicking not working? Try pasting it into your browser!) */
    common_orPaste: string;
    /** To confirm you have access to this email, please click the link below: */
    emailChange_click: string;
    /** You have requested to change your email address. */
    emailChange_intro: string;
    /** Confirm new email address, %s */
    emailChange_subject: I18nFormat;
    /** Click the link to enable your Lichess account: */
    emailConfirm_click: string;
    /** If you did not register with Lichess, you can safely ignore this message. The unconfirmed account and all traces of your email address will be deleted from our system after 48 hours. */
    emailConfirm_justIgnore: string;
    /** Confirm your lichess.org account, %s */
    emailConfirm_subject: I18nFormat;
    /** Log in to lichess.org, %s */
    logInToLichess: I18nFormat;
    /** If you made this request, click the link below. If not, you can ignore this email. */
    passwordReset_clickOrIgnore: string;
    /** We received a request to reset the password for your account. */
    passwordReset_intro: string;
    /** Reset your lichess.org password, %s */
    passwordReset_subject: I18nFormat;
    /** Welcome to lichess.org, %s */
    welcome_subject: I18nFormat;
    /** You have successfully created your account on https://lichess.org. */
    welcome_text: I18nFormat;
  };
  faq: {
    /** Accounts */
    accounts: string;
    /** The centipawn is the unit of measure used in chess as representation of the advantage. A centipawn is equal to 1/100th of a pawn. Therefore 100 centipawns = 1 pawn. These values play no formal role in the game but are useful to players, and essential in computer chess, for evaluating positions. */
    acplExplanation: string;
    /** We regularly receive messages from users asking us for help to stop them from playing too much. */
    adviceOnMitigatingAddiction: I18nFormat;
    /** an hourly Bullet tournament */
    aHourlyBulletTournament: string;
    /** Are there websites based on Lichess? */
    areThereWebsitesBasedOnLichess: string;
    /** many national master titles */
    asWellAsManyNMtitles: string;
    /** Lichess time controls are based on estimated game duration = %1$s. */
    basedOnGameDuration: I18nFormat;
    /** being a patron */
    beingAPatron: string;
    /** be in the top 10 in this rating. */
    beInTopTen: string;
    /** breakdown of our costs */
    breakdownOfOurCosts: string;
    /** Can I get the Lichess Master (LM) title? */
    canIbecomeLM: string;
    /** Can I change my username? */
    canIChangeMyUsername: string;
    /** configure */
    configure: string;
    /** I lost a game due to lag/disconnection. Can I get my rating points back? */
    connexionLostCanIGetMyRatingBack: string;
    /** desktop */
    desktop: string;
    /** Why can a pawn capture another pawn when it is already passed? (en passant) */
    discoveringEnPassant: string;
    /** display preferences */
    displayPreferences: string;
    /** (clock initial time in seconds) + 40 × (clock increment) */
    durationFormula: string;
    /** 8 chess variants */
    eightVariants: string;
    /** Most browsers can prevent sound from playing on a freshly loaded page to protect users. Imagine if every website could immediately bombard you with audio ads. */
    enableAutoplayForSoundsA: string;
    /** 1. Go to lichess.org */
    enableAutoplayForSoundsChrome: string;
    /** 1. Go to lichess.org */
    enableAutoplayForSoundsFirefox: string;
    /** 1. Click the three dots in the top right corner */
    enableAutoplayForSoundsMicrosoftEdge: string;
    /** Enable autoplay for sounds? */
    enableAutoplayForSoundsQ: string;
    /** 1. Go to lichess.org */
    enableAutoplayForSoundsSafari: string;
    /** Enable or disable notification popups? */
    enableDisableNotificationPopUps: string;
    /** Enable Zen-mode in the %1$s, or by pressing %2$s during a game. */
    enableZenMode: I18nFormat;
    /** This is a legal move known as "en passant". The Wikipedia article gives a %1$s. */
    explainingEnPassant: I18nFormat;
    /** Fair Play */
    fairPlay: string;
    /** fair play page */
    fairPlayPage: string;
    /** FAQ */
    faqAbbreviation: string;
    /** fewer lobby pools */
    fewerLobbyPools: string;
    /** FIDE handbook */
    fideHandbook: string;
    /** FIDE handbook %s */
    fideHandbookX: I18nFormat;
    /** You can find out more about %1$s (including a %2$s). If you want to help Lichess by volunteering your time and skills, there are many %3$s. */
    findMoreAndSeeHowHelp: I18nFormat;
    /** Frequently Asked Questions */
    frequentlyAskedQuestions: string;
    /** Gameplay */
    gameplay: string;
    /** ZugAddict was streaming and for the last 2 hours he had been trying to defeat A.I. level 8 in a 1+0 game, without success. Thibault told him that if he successfully did it on stream, he'd get a unique trophy. One hour later, he smashed Stockfish, and the promise was honoured. */
    goldenZeeExplanation: string;
    /** good introduction */
    goodIntroduction: string;
    /** guidelines */
    guidelines: string;
    /** have played a rated game within the last week for this rating, */
    havePlayedARatedGameAtLeastOneWeekAgo: string;
    /** have played at least 30 rated games in a given rating, */
    havePlayedMoreThanThirtyGamesInThatRating: string;
    /** Hear it pronounced by a specialist. */
    hearItPronouncedBySpecialist: string;
    /** How are Bullet, Blitz and other time controls decided? */
    howBulletBlitzEtcDecided: string;
    /** How can I become a moderator? */
    howCanIBecomeModerator: string;
    /** How can I contribute to Lichess? */
    howCanIContributeToLichess: string;
    /** How do ranks and leaderboards work? */
    howDoLeaderoardsWork: string;
    /** How to hide ratings while playing? */
    howToHideRatingWhilePlaying: string;
    /** How to... */
    howToThreeDots: string;
    /** ≤ %1$ss = %2$s */
    inferiorThanXsEqualYtimeControl: I18nFormat;
    /** In order to get on the %1$s you must: */
    inOrderToAppearsYouMust: I18nFormat;
    /** Losing on time, drawing and insufficient material */
    insufficientMaterial: string;
    /** Is correspondence different from normal chess? */
    isCorrespondenceDifferent: string;
    /** What keyboard shortcuts are there? */
    keyboardShortcuts: string;
    /** Some Lichess pages have keyboard shortcuts you can use. Try pressing the '?' key on a study, analysis, puzzle, or game page to list available keyboard shortcuts. */
    keyboardShortcutsExplanation: string;
    /** If your opponent frequently aborts/leaves games, they get "play banned", which means they're temporarily banned from playing games. This is not publicly indicated on their profile. If this behaviour continues, the length of the playban increases - and prolonged behaviour of this nature may lead to account closure. */
    leavingGameWithoutResigningExplanation: string;
    /** lee-chess */
    leechess: string;
    /** Lichess can optionally send popup notifications, for example when it is your turn or you received a private message. */
    lichessCanOptionnalySendPopUps: string;
    /** Lichess is a combination of live/light/libre and chess. It is pronounced %1$s. */
    lichessCombinationLiveLightLibrePronounced: I18nFormat;
    /** In the event of one player running out of time, that player will usually lose the game. However, the game is drawn if the position is such that the opponent cannot checkmate the player's king by any possible series of legal moves (%1$s). */
    lichessFollowFIDErules: I18nFormat;
    /** Lichess is powered by donations from patrons and the efforts of a team of volunteers. */
    lichessPoweredByDonationsAndVolunteers: string;
    /** Lichess ratings */
    lichessRatings: string;
    /** Lichess recognises all FIDE titles gained from OTB (over the board) play, as well as %1$s. Here is a list of FIDE titles: */
    lichessRecognizeAllOTBtitles: I18nFormat;
    /** Lichess supports standard chess and %1$s. */
    lichessSupportChessAnd: I18nFormat;
    /** Lichess training */
    lichessTraining: string;
    /** Lichess userstyles */
    lichessUserstyles: string;
    /** This honorific title is unofficial and only exists on Lichess. */
    lMtitleComesToYouDoNotRequestIt: string;
    /** stand-alone mental health condition */
    mentalHealthCondition: string;
    /** The player has not yet finished enough rated games against %1$s in the rating category. */
    notPlayedEnoughRatedGamesAgainstX: I18nFormat;
    /** The player hasn't played enough recent games. Depending on the number of games you've played, it might take around a year of inactivity for your rating to become provisional again. */
    notPlayedRecently: string;
    /** We did not repeat moves. Why was the game still drawn by repetition? */
    notRepeatedMoves: string;
    /** No. */
    noUpperCaseDot: string;
    /** other ways to help */
    otherWaysToHelp: string;
    /** That trophy is unique in the history of Lichess, nobody other than %1$s will ever have it. */
    ownerUniqueTrophies: I18nFormat;
    /** For more information, please read our %s */
    pleaseReadFairPlayPage: I18nFormat;
    /** positions */
    positions: string;
    /** What is done about players leaving games without resigning? */
    preventLeavingGameWithoutResigning: string;
    /** The question mark means the rating is provisional. Reasons include: */
    provisionalRatingExplanation: string;
    /** have a rating deviation lower than %1$s, in standard chess, and lower than %2$s in variants, */
    ratingDeviationLowerThanXinChessYinVariants: I18nFormat;
    /** Concretely, it means that the Glicko-2 deviation is greater than 110. The deviation is the level of confidence the system has in the rating. The lower the deviation, the more stable is a rating. */
    ratingDeviationMorethanOneHundredTen: string;
    /** rating leaderboard */
    ratingLeaderboards: string;
    /** One minute after a player is marked, their 40 latest rated games in the last 5 days are taken. If you were their opponent in one of those games, you lost rating (because of a loss or a draw), and your rating was not provisional, you get a rating refund. The refund is capped based on your peak rating and your rating progress after the game. (For example, if your rating greatly increased after that game, you might get no refund or only a partial refund.) A refund will never exceed 150 points. */
    ratingRefundExplanation: string;
    /** Ratings are calculated using the Glicko-2 rating method developed by Mark Glickman. This is a very popular rating method, and is used by a significant number of chess organisations (FIDE being a notable counter-example, as they still use the dated Elo rating system). */
    ratingSystemUsedByLichess: string;
    /** Threefold repetition is about repeated %1$s, not moves. Repetition does not have to occur consecutively. */
    repeatedPositionsThatMatters: I18nFormat;
    /** The 2nd requirement is so that players who no longer use their accounts stop populating leaderboards. */
    secondRequirementToStopOldPlayersTrustingLeaderboards: string;
    /** If you have an OTB title, you can apply to have this displayed on your account by completing the %1$s, including a clear image of an identifying document/card and a selfie of you holding the document/card. */
    showYourTitle: I18nFormat;
    /** opponents of similar strength */
    similarOpponents: string;
    /** Stop myself from playing? */
    stopMyselfFromPlaying: string;
    /** ≥ %1$ss = %2$s */
    superiorThanXsEqualYtimeControl: I18nFormat;
    /** Repetition needs to be claimed by one of the players. You can do so by pressing the button that is shown, or by offering a draw before your final repeating move, it won't matter if your opponent rejects the draw offer, the threefold repetition draw will be claimed anyway. You can also %1$s Lichess to automatically claim repetitions for you. Additionally, fivefold repetition always immediately ends the game. */
    threeFoldHasToBeClaimed: I18nFormat;
    /** Threefold repetition */
    threefoldRepetition: string;
    /** If a position occurs three times, players can claim a draw by %1$s. Lichess implements the official FIDE rules, as described in Article 9.2 of the %2$s. */
    threefoldRepetitionExplanation: I18nFormat;
    /** threefold repetition */
    threefoldRepetitionLowerCase: string;
    /** What titles are there on Lichess? */
    titlesAvailableOnLichess: string;
    /** Unique trophies */
    uniqueTrophies: string;
    /** No, usernames cannot be changed for technical and practical reasons. Usernames are materialized in too many places: databases, exports, logs, and people's minds. You can adjust the capitalization once. */
    usernamesCannotBeChanged: string;
    /** In general, usernames should not be: offensive, impersonating someone else, or advertising. You can read more about the %1$s. */
    usernamesNotOffensive: I18nFormat;
    /** verification form */
    verificationForm: string;
    /** View site information popup */
    viewSiteInformationPopUp: string;
    /** Watch International Master Eric Rosen checkmate %s. */
    watchIMRosenCheckmate: I18nFormat;
    /** Unfortunately, we cannot give back rating points for games lost due to lag or disconnection, regardless of whether the problem was at your end or our end. The latter is very rare though. Also note that when Lichess restarts and you lose on time because of that, we abort the game to prevent an unfair loss. */
    weCannotDoThatEvenIfItIsServerSideButThatsRare: string;
    /** We repeated a position three times. Why was the game not drawn? */
    weRepeatedthreeTimesPosButNoDraw: string;
    /** What is the average centipawn loss (ACPL)? */
    whatIsACPL: string;
    /** Why is there a question mark (?) next to a rating? */
    whatIsProvisionalRating: string;
    /** What can my username be? */
    whatUsernameCanIchoose: string;
    /** What variants can I play on Lichess? */
    whatVariantsCanIplay: string;
    /** When am I eligible for the automatic rating refund from cheaters? */
    whenAmIEligibleRatinRefund: string;
    /** What rating system does Lichess use? */
    whichRatingSystemUsedByLichess: string;
    /** Why are ratings higher compared to other sites and organisations such as FIDE, USCF and the ICC? */
    whyAreRatingHigher: string;
    /** It is best not to think of ratings as absolute numbers, or compare them against other organisations. Different organisations have different levels of players, different rating systems (Elo, Glicko, Glicko-2, or a modified version of the aforementioned). These factors can drastically affect the absolute numbers (ratings). */
    whyAreRatingHigherExplanation: string;
    /** Why is Lichess called Lichess? */
    whyIsLichessCalledLichess: string;
    /** Similarly, the source code for Lichess, %1$s, stands for li[chess in sca]la, seeing as the bulk of Lichess is written in %2$s, an intuitive programming language. */
    whyIsLilaCalledLila: I18nFormat;
    /** Live, because games are played and watched in real-time 24/7; light and libre for the fact that Lichess is open-source and unencumbered by proprietary junk that plagues other websites. */
    whyLiveLightLibre: string;
    /** Yes. Lichess has indeed inspired other open-source sites that use our %1$s, %2$s, or %3$s. */
    yesLichessInspiredOtherOpenSourceWebsites: I18nFormat;
    /** It’s not possible to apply to become a moderator. If we see someone who we think would be good as a moderator, we will contact them directly. */
    youCannotApply: string;
    /** On Lichess, the main difference in rules for correspondence chess is that an opening book is allowed. The use of engines is still prohibited and will result in being flagged for engine assistance. Although ICCF allows engine use in correspondence, Lichess does not. */
    youCanUseOpeningBookNoEngine: string;
  };
  features: {
    /** All chess basics lessons */
    allChessBasicsLessons: string;
    /** All features are free for everybody, forever! */
    allFeaturesAreFreeForEverybody: string;
    /** All features to come, forever! */
    allFeaturesToCome: string;
    /** Board editor and analysis board with %s */
    boardEditorAndAnalysisBoardWithEngine: I18nFormat;
    /** Chess insights (detailed analysis of your play) */
    chessInsights: string;
    /** Cloud engine analysis */
    cloudEngineAnalysis: string;
    /** Correspondence chess with conditional premoves */
    correspondenceWithConditionalPremoves: string;
    /** Deep %s server analysis */
    deepXServerAnalysis: I18nFormat;
    /** Download/Upload any game as PGN */
    downloadOrUploadAnyGameAsPgn: string;
    /** 7-piece endgame tablebase */
    endgameTablebase: string;
    /** Yes, both accounts have the same features! */
    everybodyGetsAllFeaturesForFree: string;
    /** %s games per day */
    gamesPerDay: I18nPlural;
    /** Global opening explorer (%s games!) */
    globalOpeningExplorerInNbGames: I18nFormat;
    /** If you love Lichess, */
    ifYouLoveLichess: string;
    /** iPhone & Android phones and tablets, landscape support */
    landscapeSupportOnApp: string;
    /** Light/Dark theme, custom boards, pieces and background */
    lightOrDarkThemeCustomBoardsPiecesAndBackground: string;
    /** Personal opening explorer */
    personalOpeningExplorer: string;
    /** %1$s (also works on %2$s) */
    personalOpeningExplorerX: I18nFormat;
    /** Standard chess and %s */
    standardChessAndX: I18nFormat;
    /** Studies (shareable and persistent analysis) */
    studies: string;
    /** Support us with a Patron account! */
    supportUsWithAPatronAccount: string;
    /** Tactical puzzles from user games */
    tacticalPuzzlesFromUserGames: string;
    /** Blog, forum, teams, TV, messaging, friends, challenges */
    tvForumBlogTeamsMessagingFriendsChallenges: string;
    /** UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence Chess */
    ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess: string;
    /** We believe every chess player deserves the best, and so: */
    weBelieveEveryChessPlayerDeservesTheBest: string;
    /** Zero advertisement, no tracking */
    zeroAdsAndNoTracking: string;
  };
  insight: {
    /** Sorry, you cannot see %s's chess insights. */
    cantSeeInsights: I18nFormat;
    /** Now crunching data just for you! */
    crunchingData: string;
    /** Generate %s's chess insights. */
    generateInsights: I18nFormat;
    /** %s's chess insights are protected */
    insightsAreProtected: I18nFormat;
    /** insights settings */
    insightsSettings: string;
    /** Maybe ask them to change their %s? */
    maybeAskThemToChangeTheir: I18nFormat;
    /** %s's chess insights */
    xChessInsights: I18nFormat;
    /** %s has no chess insights yet! */
    xHasNoChessInsights: I18nFormat;
  };
  keyboardMove: {
    /** Both the letter "o" and the digit zero "0" can be used when castling */
    bothTheLetterOAndTheDigitZero: string;
    /** Capitalization only matters in ambiguous situations involving a bishop and the b-pawn */
    capitalizationOnlyMattersInAmbiguousSituations: string;
    /** Drop a rook at b4 (Crazyhouse variant only) */
    dropARookAtB4: string;
    /** If it is legal to castle both ways, use enter to kingside castle */
    ifItIsLegalToCastleBothWays: string;
    /** If the above move notation is unfamiliar, learn more here: */
    ifTheAboveMoveNotationIsUnfamiliar: string;
    /** Including an "x" to indicate a capture is optional */
    includingAXToIndicateACapture: string;
    /** Keyboard input commands */
    keyboardInputCommands: string;
    /** Kingside castle */
    kingsideCastle: string;
    /** Move knight to c3 */
    moveKnightToC3: string;
    /** Move piece from e2 to e4 */
    movePieceFromE2ToE4: string;
    /** Offer or accept draw */
    offerOrAcceptDraw: string;
    /** Other commands */
    otherCommands: string;
    /** Perform a move */
    performAMove: string;
    /** Promote c8 to queen */
    promoteC8ToQueen: string;
    /** Queenside castle */
    queensideCastle: string;
    /** Read out clocks */
    readOutClocks: string;
    /** Read out opponent's name */
    readOutOpponentName: string;
    /** Tips */
    tips: string;
    /** To premove, simply type the desired premove before it is your turn */
    toPremoveSimplyTypeTheDesiredPremove: string;
  };
  lag: {
    /** And now, the long answer! Game lag is composed of two unrelated values (lower is better): */
    andNowTheLongAnswerLagComposedOfTwoValues: string;
    /** Is Lichess lagging? */
    isLichessLagging: string;
    /** Lag compensation */
    lagCompensation: string;
    /** Lichess compensates network lag. This includes sustained lag and occasional lag spikes. There are limits and heuristics based on time control and the compensated lag so far, so that the result should feel reasonable for both players. As a result, having a higher network lag than your opponent is not a handicap! */
    lagCompensationExplanation: string;
    /** Lichess server latency */
    lichessServerLatency: string;
    /** The time it takes to process a move on the server. It's the same for everybody, and only depends on the servers load. The more players, the higher it gets, but Lichess developers do their best to keep it low. It rarely exceeds 10ms. */
    lichessServerLatencyExplanation: string;
    /** Measurements in progress... */
    measurementInProgressThreeDot: string;
    /** Network between Lichess and you */
    networkBetweenLichessAndYou: string;
    /** The time it takes to send a move from your computer to Lichess server, and get the response back. It's specific to your distance to Lichess (France), and to the quality of your Internet connection. Lichess developers cannot fix your wifi or make light go faster. */
    networkBetweenLichessAndYouExplanation: string;
    /** No. And your network is bad. */
    noAndYourNetworkIsBad: string;
    /** No. And your network is good. */
    noAndYourNetworkIsGood: string;
    /** Yes. It will be fixed soon! */
    yesItWillBeFixedSoon: string;
    /** You can find both these values at any time, by clicking your username in the top bar. */
    youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername: string;
  };
  learn: {
    /** Advanced */
    advanced: string;
    /** A pawn on the second rank can move 2 squares at once! */
    aPawnOnTheSecondRank: string;
    /** Attack the opponent's king */
    attackTheOpponentsKing: string;
    /** Attack your opponent's king */
    attackYourOpponentsKing: string;
    /** Awesome! */
    awesome: string;
    /** Back to menu */
    backToMenu: string;
    /** Congratulations! You can command a bishop. */
    bishopComplete: string;
    /** Next we will learn how to manoeuvre a bishop! */
    bishopIntro: string;
    /** Black just moved the pawn */
    blackJustMovedThePawnByTwoSquares: string;
    /** Board setup */
    boardSetup: string;
    /** Congratulations! You know how to set up the chess board. */
    boardSetupComplete: string;
    /** The two armies face each other, ready for the battle. */
    boardSetupIntro: string;
    /** by playing! */
    byPlaying: string;
    /** Capture */
    capture: string;
    /** Capture and defend pieces */
    captureAndDefendPieces: string;
    /** Congratulations! You know how to fight with chess pieces! */
    captureComplete: string;
    /** Identify the opponent's undefended pieces, and capture them! */
    captureIntro: string;
    /** Capture, then promote! */
    captureThenPromote: string;
    /** Move your king two squares */
    castleKingSide: string;
    /** Castle king-side! */
    castleKingSideMovePiecesFirst: string;
    /** Move your king two squares */
    castleQueenSide: string;
    /** Castle queen-side! */
    castleQueenSideMovePiecesFirst: string;
    /** Castling */
    castling: string;
    /** Congratulations! You should almost always castle in a game. */
    castlingComplete: string;
    /** Bring your king to safety, and deploy your rook for attack! */
    castlingIntro: string;
    /** Check in one */
    checkInOne: string;
    /** Congratulations! You checked your opponent, forcing them to defend their king! */
    checkInOneComplete: string;
    /** Aim at the opponent's king */
    checkInOneGoal: string;
    /** To check your opponent, attack their king. They must defend it! */
    checkInOneIntro: string;
    /** Check in two */
    checkInTwo: string;
    /** Congratulations! You checked your opponent, forcing them to defend their king! */
    checkInTwoComplete: string;
    /** Threaten the opponent's king */
    checkInTwoGoal: string;
    /** Find the right combination of two moves that checks the opponent's king! */
    checkInTwoIntro: string;
    /** Chess pieces */
    chessPieces: string;
    /** Combat */
    combat: string;
    /** Congratulations! You know how to fight with chess pieces! */
    combatComplete: string;
    /** A good warrior knows both attack and defence! */
    combatIntro: string;
    /** Defeat the opponent's king */
    defeatTheOpponentsKing: string;
    /** Defend your king */
    defendYourKing: string;
    /** Don't let them take */
    dontLetThemTakeAnyUndefendedPiece: string;
    /** Congratulations! You can now take en passant. */
    enPassantComplete: string;
    /** When the opponent pawn moved by two squares, you can take it like if it moved by one square. */
    enPassantIntro: string;
    /** En passant only works */
    enPassantOnlyWorksImmediately: string;
    /** En passant only works */
    enPassantOnlyWorksOnFifthRank: string;
    /** You're under attack! */
    escape: string;
    /** Escape with the king */
    escapeOrBlock: string;
    /** Escape with the king! */
    escapeWithTheKing: string;
    /** Evaluate piece strength */
    evaluatePieceStrength: string;
    /** Excellent! */
    excellent: string;
    /** Exercise your tactical skills */
    exerciseYourTacticalSkills: string;
    /** Find a way to */
    findAWayToCastleKingSide: string;
    /** Find a way to */
    findAWayToCastleQueenSide: string;
    /** First place the rooks! */
    firstPlaceTheRooks: string;
    /** Fundamentals */
    fundamentals: string;
    /** Get a free Lichess account */
    getAFreeLichessAccount: string;
    /** Grab all the stars! */
    grabAllTheStars: string;
    /** Grab all the stars! */
    grabAllTheStarsNoNeedToPromote: string;
    /** Great job! */
    greatJob: string;
    /** How the game starts */
    howTheGameStarts: string;
    /** Intermediate */
    intermediate: string;
    /** It moves diagonally */
    itMovesDiagonally: string;
    /** It moves forward only */
    itMovesForwardOnly: string;
    /** It moves in an L shape */
    itMovesInAnLShape: string;
    /** It moves in straight lines */
    itMovesInStraightLines: string;
    /** It now promotes to a stronger piece. */
    itNowPromotesToAStrongerPiece: string;
    /** Keep your pieces safe */
    keepYourPiecesSafe: string;
    /** You can now command the commander! */
    kingComplete: string;
    /** You are the king. If you fall in battle, the game is lost. */
    kingIntro: string;
    /** Congratulations! You have mastered the knight. */
    knightComplete: string;
    /** Here's a challenge for you. The knight is... a tricky piece. */
    knightIntro: string;
    /** Knights can jump over obstacles! */
    knightsCanJumpOverObstacles: string;
    /** Knights have a fancy way */
    knightsHaveAFancyWay: string;
    /** Last one! */
    lastOne: string;
    /** Learn chess */
    learnChess: string;
    /** Learn common chess positions */
    learnCommonChessPositions: string;
    /** Let's go! */
    letsGo: string;
    /** Mate in one */
    mateInOne: string;
    /** Congratulations! That is how you win chess games! */
    mateInOneComplete: string;
    /** You win when your opponent cannot defend against a check. */
    mateInOneIntro: string;
    /** Menu */
    menu: string;
    /** Most of the time promoting to a queen is the best. */
    mostOfTheTimePromotingToAQueenIsBest: string;
    /** Nailed it. */
    nailedIt: string;
    /** Next */
    next: string;
    /** Next: %s */
    nextX: I18nFormat;
    /** There is no escape, */
    noEscape: string;
    /** Opponents from around the world */
    opponentsFromAroundTheWorld: string;
    /** Out of check */
    outOfCheck: string;
    /** Congratulations! Your king can never be taken, make sure you can defend against a check! */
    outOfCheckComplete: string;
    /** You are in check! You must escape or block the attack. */
    outOfCheckIntro: string;
    /** Outstanding! */
    outstanding: string;
    /** Congratulations! Pawns have no secrets for you. */
    pawnComplete: string;
    /** Pawns are weak, but they pack a lot of potential. */
    pawnIntro: string;
    /** Pawn promotion */
    pawnPromotion: string;
    /** Pawns form the front line. */
    pawnsFormTheFrontLine: string;
    /** Pawns move forward, */
    pawnsMoveForward: string;
    /** Pawns move one square only. */
    pawnsMoveOneSquareOnly: string;
    /** Perfect! */
    perfect: string;
    /** Piece value */
    pieceValue: string;
    /** Congratulations! You know the value of material! */
    pieceValueComplete: string;
    /** Take the piece with the highest value! */
    pieceValueExchange: string;
    /** Pieces with high mobility have a higher value! */
    pieceValueIntro: string;
    /** Take the piece */
    pieceValueLegal: string;
    /** Place the bishops! */
    placeTheBishops: string;
    /** Place the king! */
    placeTheKing: string;
    /** Place the queen! */
    placeTheQueen: string;
    /** play! */
    play: string;
    /** Play machine */
    playMachine: string;
    /** Play people */
    playPeople: string;
    /** Practise */
    practice: string;
    /** Progress: %s */
    progressX: I18nFormat;
    /** Protection */
    protection: string;
    /** Congratulations! A piece you don't lose is a piece you win! */
    protectionComplete: string;
    /** Identify the pieces your opponent attacks, and defend them! */
    protectionIntro: string;
    /** Puzzle failed! */
    puzzleFailed: string;
    /** Puzzles */
    puzzles: string;
    /** Queen = rook + bishop */
    queenCombinesRookAndBishop: string;
    /** Congratulations! Queens have no secrets for you. */
    queenComplete: string;
    /** The most powerful chess piece enters. Her majesty the queen! */
    queenIntro: string;
    /** Take the piece */
    queenOverBishop: string;
    /** Register */
    register: string;
    /** Reset my progress */
    resetMyProgress: string;
    /** Retry */
    retry: string;
    /** Right on! */
    rightOn: string;
    /** Congratulations! You have successfully mastered the rook. */
    rookComplete: string;
    /** Click on the rook */
    rookGoal: string;
    /** The rook is a powerful piece. Are you ready to command it? */
    rookIntro: string;
    /** Select the piece you want! */
    selectThePieceYouWant: string;
    /** Stage %s */
    stageX: I18nFormat;
    /** Stage %s complete */
    stageXComplete: I18nFormat;
    /** Stalemate */
    stalemate: string;
    /** Congratulations! Better be stalemated than checkmated! */
    stalemateComplete: string;
    /** To stalemate black: */
    stalemateGoal: string;
    /** When a player is not in check and does not have a legal move, it's a stalemate. The game is drawn: no one wins, no one loses. */
    stalemateIntro: string;
    /** Take all the pawns en passant! */
    takeAllThePawnsEnPassant: string;
    /** Take the black pieces! */
    takeTheBlackPieces: string;
    /** Take the black pieces! */
    takeTheBlackPiecesAndDontLoseYours: string;
    /** Take the enemy pieces */
    takeTheEnemyPieces: string;
    /** Take the piece */
    takeThePieceWithTheHighestValue: string;
    /** Test your skills with the computer */
    testYourSkillsWithTheComputer: string;
    /** The bishop */
    theBishop: string;
    /** The fewer moves you make, */
    theFewerMoves: string;
    /** The game is a draw */
    theGameIsADraw: string;
    /** The king */
    theKing: string;
    /** The king cannot escape, */
    theKingCannotEscapeButBlock: string;
    /** The king is slow. */
    theKingIsSlow: string;
    /** The knight */
    theKnight: string;
    /** The knight is in the way! */
    theKnightIsInTheWay: string;
    /** The most important piece */
    theMostImportantPiece: string;
    /** Then place the knights! */
    thenPlaceTheKnights: string;
    /** The pawn */
    thePawn: string;
    /** The queen */
    theQueen: string;
    /** The rook */
    theRook: string;
    /** The special king move */
    theSpecialKingMove: string;
    /** The special pawn move */
    theSpecialPawnMove: string;
    /** This is the initial position */
    thisIsTheInitialPosition: string;
    /** This knight is checking */
    thisKnightIsCheckingThroughYourDefenses: string;
    /** Two moves to give a check */
    twoMovesToGiveCheck: string;
    /** Use all the pawns! */
    useAllThePawns: string;
    /** Use two rooks */
    useTwoRooks: string;
    /** Videos */
    videos: string;
    /** Watch instructive chess videos */
    watchInstructiveChessVideos: string;
    /** Way to go! */
    wayToGo: string;
    /** What next? */
    whatNext: string;
    /** Yes, yes, yes! */
    yesYesYes: string;
    /** You can get out of check */
    youCanGetOutOfCheckByTaking: string;
    /** You cannot castle if */
    youCannotCastleIfAttacked: string;
    /** You cannot castle if */
    youCannotCastleIfMoved: string;
    /** You know how to play chess, congratulations! Do you want to become a stronger player? */
    youKnowHowToPlayChess: string;
    /** One light-squared bishop, */
    youNeedBothBishops: string;
    /** You're good at this! */
    youreGoodAtThis: string;
    /** Your pawn reached the end of the board! */
    yourPawnReachedTheEndOfTheBoard: string;
    /** You will lose all your progress! */
    youWillLoseAllYourProgress: string;
  };
  nvui: {
    /** Actions */
    actions: string;
    /** Announce current square. */
    announceCurrentSquare: string;
    /** Announce last move. */
    announceLastMove: string;
    /** Announce piece captured in last move. */
    announceLastMoveCapture: string;
    /** Announce locations of pieces. Example: p capital N for white knights, p lowercase k for black king, p capital A for all white pieces. */
    announcePieceLocations: string;
    /** Announce pieces on a rank or a file. Example: s a, s 1. */
    announcePiecesOnRankOrFile: string;
    /** Announce possible captures with selected piece. */
    announcePossibleCaptures: string;
    /** Announce possible moves for the selected piece. */
    announcePossibleMoves: string;
    /** bishop */
    bishop: string;
    /** black bishop */
    blackBishop: string;
    /** black king */
    blackKing: string;
    /** black knight */
    blackKnight: string;
    /** black pawn */
    blackPawn: string;
    /** black queen */
    blackQueen: string;
    /** black rook */
    blackRook: string;
    /** Command list when the board has focus */
    boardCommandList: string;
    /** %s copied to clipboard */
    copiedToClipboard: I18nFormat;
    /** Copy %s to clipboard */
    copyToClipboard: I18nFormat;
    /** Featured events */
    featuredEvents: string;
    /** Game info */
    gameInfo: string;
    /** Game start */
    gameStart: string;
    /** Game status */
    gameStatus: string;
    /** Go to the board. Default square is e-4. You can specify a square: board a-1 or b a-1 will take you to square a-1. */
    goToBoard: string;
    /** Go to the command input form. */
    goToInputForm: string;
    /** Command input form */
    inputForm: string;
    /** Type these commands in the command input form. */
    inputFormCommandList: string;
    /** Invalid move */
    invalidMove: string;
    /** king */
    king: string;
    /** knight */
    knight: string;
    /** Last move */
    lastMove: string;
    /** Move list */
    moveList: string;
    /** To move a piece, use standard algebraic notation. */
    movePiece: string;
    /** Move to file a to h. */
    moveToFile: string;
    /** Move to squares using piece names. For example: repeated k will move to every square where there is a knight. Use uppercase to invert order. */
    moveToPieceByType: string;
    /** Move to rank 1 to 8. */
    moveToRank: string;
    /** Move to adjacent square left, right, up or down. */
    moveWithArrows: string;
    /** Opponent clock */
    opponentClock: string;
    /** pawn */
    pawn: string;
    /** PGN and FEN */
    pgnAndFen: string;
    /** Pieces */
    pieces: string;
    /** Pockets */
    pockets: string;
    /** Premove cancelled */
    premoveCancelled: string;
    /** Premove recorded: %s. Hit enter to cancel */
    premoveRecorded: I18nFormat;
    /** To promote to anything else than a queen, use equals. For example a-8-equals-n promotes to a knight. */
    promotion: string;
    /** queen */
    queen: string;
    /** rook */
    rook: string;
    /** check */
    sanCheck: string;
    /** checkmate */
    sanCheckmate: string;
    /** is dropped on */
    sanDroppedOn: string;
    /** long castling */
    sanLongCastling: string;
    /** promotes to */
    sanPromotesTo: string;
    /** short castling */
    sanShortCastling: string;
    /** K Q R B N x */
    sanSymbols: string;
    /** takes */
    sanTakes: string;
    /** white bishop */
    whiteBishop: string;
    /** white king */
    whiteKing: string;
    /** white knight */
    whiteKnight: string;
    /** white pawn */
    whitePawn: string;
    /** white queen */
    whiteQueen: string;
    /** white rook */
    whiteRook: string;
    /** Your clock */
    yourClock: string;
  };
  oauthScope: {
    /** You already have played games! */
    alreadyHavePlayedGames: string;
    /** API access tokens */
    apiAccessTokens: string;
    /** API documentation */
    apiDocumentation: string;
    /** Here's a %1$s and the %2$s. */
    apiDocumentationLinks: I18nFormat;
    /** Note for the attention of developers only: */
    attentionOfDevelopers: string;
    /** authorization code flow */
    authorizationCodeFlow: string;
    /** Play games with board API */
    boardPlay: string;
    /** Play games with the bot API */
    botPlay: string;
    /** You can make OAuth requests without going through the %s. */
    canMakeOauthRequests: I18nFormat;
    /** Carefully select what it is allowed to do on your behalf. */
    carefullySelect: string;
    /** Create many games at once for other players */
    challengeBulk: string;
    /** Read incoming challenges */
    challengeRead: string;
    /** Send, accept and reject challenges */
    challengeWrite: string;
    /** Make sure to copy your new personal access token now. You won’t be able to see it again! */
    copyTokenNow: string;
    /** Created %s */
    created: I18nFormat;
    /** The token will grant access to your account. Do NOT share it with anyone! */
    doNotShareIt: string;
    /** Read email address */
    emailRead: string;
    /** View and use your external engines */
    engineRead: string;
    /** Create and update external engines */
    engineWrite: string;
    /** Read followed players */
    followRead: string;
    /** Follow and unfollow other players */
    followWrite: string;
    /** For example: %s */
    forExample: I18nFormat;
    /** generate a personal access token */
    generatePersonalToken: string;
    /** Giving these pre-filled URLs to your users will help them get the right token scopes. */
    givingPrefilledUrls: string;
    /** Guard these tokens carefully! They are like passwords. The advantage to using tokens over putting your password into a script is that tokens can be revoked, and you can generate lots of them. */
    guardTokensCarefully: string;
    /** Instead, %s that you can directly use in API requests. */
    insteadGenerateToken: I18nFormat;
    /** Last used %s */
    lastUsed: I18nFormat;
    /** Send private messages to other players */
    msgWrite: string;
    /** New personal API access token */
    newAccessToken: string;
    /** New access token */
    newToken: string;
    /** Personal API access tokens */
    personalAccessTokens: string;
    /** personal token app example */
    personalTokenAppExample: string;
    /** It is possible to pre-fill this form by tweaking the query parameters of the URL. */
    possibleToPrefill: string;
    /** Read preferences */
    preferenceRead: string;
    /** Write preference */
    preferenceWrite: string;
    /** Read puzzle activity */
    puzzleRead: string;
    /** Create and join puzzle races */
    racerWrite: string;
    /** So you remember what this token is for */
    rememberTokenUse: string;
    /** Read private studies and broadcasts */
    studyRead: string;
    /** Create, update, delete studies and broadcasts */
    studyWrite: string;
    /** Manage teams you lead: send PMs, kick members */
    teamLead: string;
    /** Read private team information */
    teamRead: string;
    /** Join and leave teams */
    teamWrite: string;
    /** ticks the %1$s and %2$s scopes, and sets the token description. */
    ticksTheScopes: I18nFormat;
    /** Token description */
    tokenDescription: string;
    /** A token grants other people permission to use your account. */
    tokenGrantsPermission: string;
    /** Create, update, and join tournaments */
    tournamentWrite: string;
    /** Create authenticated website sessions (grants full access!) */
    webLogin: string;
    /** Use moderator tools (within bounds of your permission) */
    webMod: string;
    /** What the token can do on your behalf: */
    whatTheTokenCanDo: string;
  };
  onboarding: {
    /** Configure Lichess to your liking. */
    configureLichess: string;
    /** Will a child use this account? You might want to enable %s. */
    enabledKidModeSuggestion: I18nFormat;
    /** Explore the site and have fun :) */
    exploreTheSiteAndHaveFun: string;
    /** Follow your friends on Lichess. */
    followYourFriendsOnLichess: string;
    /** Improve with chess tactics puzzles. */
    improveWithChessTacticsPuzzles: string;
    /** Learn chess rules */
    learnChessRules: string;
    /** Learn from %1$s and %2$s. */
    learnFromXAndY: I18nFormat;
    /** Log in as %s */
    logInAsUsername: I18nFormat;
    /** Play in tournaments. */
    playInTournaments: string;
    /** Play opponents from around the world. */
    playOpponentsFromAroundTheWorld: string;
    /** Play the Artificial Intelligence. */
    playTheArtificialIntelligence: string;
    /** This is your profile page. */
    thisIsYourProfilePage: string;
    /** Welcome! */
    welcome: string;
    /** Welcome to lichess.org! */
    welcomeToLichess: string;
    /** What now? Here are a few suggestions: */
    whatNowSuggestions: string;
  };
  patron: {
    /** Yes, here's the act of creation (in French) */
    actOfCreation: string;
    /** Amount */
    amount: string;
    /** We also accept bank transfers */
    bankTransfers: string;
    /** Become a Lichess Patron */
    becomePatron: string;
    /** Cancel your support */
    cancelSupport: string;
    /** The celebrated Patrons who make Lichess possible */
    celebratedPatrons: string;
    /** Change currency */
    changeCurrency: string;
    /** Change the monthly amount (%s) */
    changeMonthlyAmount: I18nFormat;
    /** Can I change/cancel my monthly support? */
    changeMonthlySupport: string;
    /** Yes, at any time, from this page. */
    changeOrContact: I18nFormat;
    /** Check out your profile page! */
    checkOutProfile: string;
    /** contact Lichess support */
    contactSupport: string;
    /** See the detailed cost breakdown */
    costBreakdown: string;
    /** I'll add %s to help cover the cost of processing this transaction */
    coverFees: I18nFormat;
    /** Current status */
    currentStatus: string;
    /** Date */
    date: string;
    /** Decide what Lichess is worth to you: */
    decideHowMuch: string;
    /** Donate */
    donate: string;
    /** Donate as %s */
    donateAsX: I18nFormat;
    /** In one month, you will NOT be charged again, and your Lichess account will revert to a regular account. */
    downgradeNextMonth: string;
    /** See the detailed feature comparison */
    featuresComparison: string;
    /** Free account */
    freeAccount: string;
    /** Free chess for everyone, forever! */
    freeChess: string;
    /** Gift Patron wings to a player */
    giftPatronWings: string;
    /** Gift Patron wings */
    giftPatronWingsShort: string;
    /** If not renewed, your account will then revert to a regular account. */
    ifNotRenewedThenAccountWillRevert: string;
    /** Lichess is registered with %s. */
    lichessIsRegisteredWith: I18nFormat;
    /** Lichess Patron */
    lichessPatron: string;
    /** Lifetime */
    lifetime: string;
    /** Lifetime Lichess Patron */
    lifetimePatron: string;
    /** Log in to donate */
    logInToDonate: string;
    /** Make an additional donation */
    makeAdditionalDonation: string;
    /** Monthly */
    monthly: string;
    /** New Patrons */
    newPatrons: string;
    /** Next payment */
    nextPayment: string;
    /** No ads, no subscriptions; but open-source and passion. */
    noAdsNoSubs: string;
    /** No longer support Lichess */
    noLongerSupport: string;
    /** No, because Lichess is entirely free, forever, and for everyone. That's a promise. */
    noPatronFeatures: string;
    /** You are now a lifetime Lichess Patron! */
    nowLifetime: string;
    /** You are now a Lichess Patron for one month! */
    nowOneMonth: string;
    /** Is Lichess an official non-profit? */
    officialNonProfit: string;
    /** One-time */
    onetime: string;
    /** Please note that only the donation form above will grant the Patron status. */
    onlyDonationFromAbove: string;
    /** Other */
    otherAmount: string;
    /** Other methods of donation? */
    otherMethods: string;
    /** Are some features reserved to Patrons? */
    patronFeatures: string;
    /** Lichess Patron for %s months */
    patronForMonths: I18nPlural;
    /** Patron since %s */
    patronSince: I18nFormat;
    /** You have a Patron account until %s. */
    patronUntil: I18nFormat;
    /** Pay %s once. Be a Lichess Patron forever! */
    payLifetimeOnce: I18nFormat;
    /** Payment details */
    paymentDetails: string;
    /** You now have a permanent Patron account. */
    permanentPatron: string;
    /** Please enter an amount in %s */
    pleaseEnterAmountInX: I18nFormat;
    /** Recurring billing, renewing your Patron wings every month. */
    recurringBilling: string;
    /** First of all, powerful servers. */
    serversAndDeveloper: I18nFormat;
    /** A single donation that grants you the Patron wings for one month. */
    singleDonation: string;
    /** Withdraw your credit card and stop payments: */
    stopPayments: string;
    /** Cancel PayPal subscription and stop payments: */
    stopPaymentsPayPal: string;
    /** Manage your subscription and download your invoices and receipts */
    stripeManageSub: string;
    /** Thank you for your donation! */
    thankYou: string;
    /** Your transaction has been completed, and a receipt for your donation has been emailed to you. */
    transactionCompleted: string;
    /** Thank you very much for your help. You rock! */
    tyvm: string;
    /** Update */
    update: string;
    /** Update payment method */
    updatePaymentMethod: string;
    /** View other Lichess Patrons */
    viewOthers: string;
    /** We are a non‑profit association because we believe everyone should have access to a free, world-class chess platform. */
    weAreNonProfit: string;
    /** We are a small team, so your support makes a huge difference! */
    weAreSmallTeam: string;
    /** We rely on support from people like you to make it possible. If you enjoy using Lichess, please consider supporting us by donating and becoming a Patron! */
    weRelyOnSupport: string;
    /** Where does the money go? */
    whereMoneyGoes: string;
    /** Credit Card */
    withCreditCard: string;
    /** %s became a Lichess Patron */
    xBecamePatron: I18nFormat;
    /** %1$s is a Lichess Patron for %2$s months */
    xIsPatronForNbMonths: I18nPlural;
    /** %1$s or %2$s */
    xOrY: I18nFormat;
    /** You have a Lifetime Patron account. That's pretty awesome! */
    youHaveLifetime: string;
    /** You support lichess.org with %s per month. */
    youSupportWith: I18nFormat;
    /** You will be charged %1$s on %2$s. */
    youWillBeChargedXOnY: I18nFormat;
  };
  perfStat: {
    /** Average opponent */
    averageOpponent: string;
    /** Berserked games */
    berserkedGames: string;
    /** Best rated victories */
    bestRated: string;
    /** Current streak: %s */
    currentStreak: I18nFormat;
    /** Defeats */
    defeats: string;
    /** Disconnections */
    disconnections: string;
    /** from %1$s to %2$s */
    fromXToY: I18nFormat;
    /** Games played in a row */
    gamesInARow: string;
    /** Highest rating: %s */
    highestRating: I18nFormat;
    /** Less than one hour between games */
    lessThanOneHour: string;
    /** Longest streak: %s */
    longestStreak: I18nFormat;
    /** Losing streak */
    losingStreak: string;
    /** Lowest rating: %s */
    lowestRating: I18nFormat;
    /** Max time spent playing */
    maxTimePlaying: string;
    /** Not enough games played */
    notEnoughGames: string;
    /** Not enough rated games have been played to establish a reliable rating. */
    notEnoughRatedGames: string;
    /** now */
    now: string;
    /** %s stats */
    perfStats: I18nFormat;
    /** Progression over the last %s games: */
    progressOverLastXGames: I18nFormat;
    /** provisional */
    provisional: string;
    /** Rated games */
    ratedGames: string;
    /** Rating deviation: %s. */
    ratingDeviation: I18nFormat;
    /** Lower value means the rating is more stable. Above %1$s, the rating is considered provisional. To be included in the rankings, this value should be below %2$s (standard chess) or %3$s (variants). */
    ratingDeviationTooltip: I18nFormat;
    /** Time spent playing */
    timeSpentPlaying: string;
    /** Total games */
    totalGames: string;
    /** Tournament games */
    tournamentGames: string;
    /** Victories */
    victories: string;
    /** View the games */
    viewTheGames: string;
    /** Winning streak */
    winningStreak: string;
  };
  preferences: {
    /** Bell notification sound */
    bellNotificationSound: string;
    /** Blindfold */
    blindfold: string;
    /** Board coordinates (A-H, 1-8) */
    boardCoordinates: string;
    /** Board highlights (last move and check) */
    boardHighlights: string;
    /** Either */
    bothClicksAndDrag: string;
    /** Move king onto rook */
    castleByMovingOntoTheRook: string;
    /** Castling method */
    castleByMovingTheKingTwoSquaresOrOntoTheRook: string;
    /** Move king two squares */
    castleByMovingTwoSquares: string;
    /** Chess clock */
    chessClock: string;
    /** Chess piece symbol */
    chessPieceSymbol: string;
    /** Claim draw on threefold repetition automatically */
    claimDrawOnThreefoldRepetitionAutomatically: string;
    /** Click two squares */
    clickTwoSquares: string;
    /** Confirm resignation and draw offers */
    confirmResignationAndDrawOffers: string;
    /** Correspondence and unlimited */
    correspondenceAndUnlimited: string;
    /** Daily email listing your correspondence games */
    correspondenceEmailNotification: string;
    /** Display */
    display: string;
    /** Show board resize handle */
    displayBoardResizeHandle: string;
    /** Drag a piece */
    dragPiece: string;
    /** Except in-game */
    exceptInGame: string;
    /** Can be disabled during a game with the board menu */
    explainCanThenBeTemporarilyDisabled: string;
    /** Hold the <ctrl> key while promoting to temporarily disable auto-promotion */
    explainPromoteToQueenAutomatically: string;
    /** This hides all ratings from Lichess, to help focus on the chess. Rated games still impact your rating, this is only about what you get to see. */
    explainShowPlayerRatings: string;
    /** Game behaviour */
    gameBehavior: string;
    /** Give more time */
    giveMoreTime: string;
    /** Horizontal green progress bars */
    horizontalGreenProgressBars: string;
    /** How do you move pieces? */
    howDoYouMovePieces: string;
    /** In casual games only */
    inCasualGamesOnly: string;
    /** Correspondence games */
    inCorrespondenceGames: string;
    /** In-game only */
    inGameOnly: string;
    /** Input moves with the keyboard */
    inputMovesWithTheKeyboard: string;
    /** Input moves with your voice */
    inputMovesWithVoice: string;
    /** Material difference */
    materialDifference: string;
    /** Move confirmation */
    moveConfirmation: string;
    /** Move list while playing */
    moveListWhilePlaying: string;
    /** Notifications */
    notifications: string;
    /** Bell notification within Lichess */
    notifyBell: string;
    /** Broadcasts you have subscribed to */
    notifyBroadcasts: string;
    /** Challenges */
    notifyChallenge: string;
    /** Device */
    notifyDevice: string;
    /** Forum comment mentions you */
    notifyForumMention: string;
    /** Correspondence game updates */
    notifyGameEvent: string;
    /** New inbox message */
    notifyInboxMsg: string;
    /** Study invite */
    notifyInvitedStudy: string;
    /** Device notification when you're not on Lichess */
    notifyPush: string;
    /** Streamer goes live */
    notifyStreamStart: string;
    /** Correspondence clock running out */
    notifyTimeAlarm: string;
    /** Tournament starting soon */
    notifyTournamentSoon: string;
    /** Browser */
    notifyWeb: string;
    /** Only on initial position */
    onlyOnInitialPosition: string;
    /** Letter (K, Q, R, B, N) */
    pgnLetter: string;
    /** Move notation */
    pgnPieceNotation: string;
    /** Piece animation */
    pieceAnimation: string;
    /** Piece destinations (valid moves and premoves) */
    pieceDestinations: string;
    /** Preferences */
    preferences: string;
    /** Premoves (playing during opponent turn) */
    premovesPlayingDuringOpponentTurn: string;
    /** Privacy */
    privacy: string;
    /** Promote to Queen automatically */
    promoteToQueenAutomatically: string;
    /** Say "Good game, well played" upon defeat or draw */
    sayGgWpAfterLosingOrDrawing: string;
    /** Scroll on the board to replay moves */
    scrollOnTheBoardToReplayMoves: string;
    /** Show on the left on mobile devices */
    showClockOnTheLeft: string;
    /** Show player flairs */
    showFlairs: string;
    /** Show player ratings */
    showPlayerRatings: string;
    /** Snap arrows to valid moves */
    snapArrowsToValidMoves: string;
    /** Sound when time gets critical */
    soundWhenTimeGetsCritical: string;
    /** Takebacks (with opponent approval) */
    takebacksWithOpponentApproval: string;
    /** Tenths of seconds */
    tenthsOfSeconds: string;
    /** When premoving */
    whenPremoving: string;
    /** When time remaining < 10 seconds */
    whenTimeRemainingLessThanTenSeconds: string;
    /** When time remaining < 30 seconds */
    whenTimeRemainingLessThanThirtySeconds: string;
    /** Your preferences have been saved. */
    yourPreferencesHaveBeenSaved: string;
    /** Zen mode */
    zenMode: string;
  };
  puzzle: {
    /** Add another theme */
    addAnotherTheme: string;
    /** Advanced */
    advanced: string;
    /** Best move! */
    bestMove: string;
    /** By openings */
    byOpenings: string;
    /** Click to solve */
    clickToSolve: string;
    /** Continue the streak */
    continueTheStreak: string;
    /** Continue training */
    continueTraining: string;
    /** Daily Puzzle */
    dailyPuzzle: string;
    /** Did you like this puzzle? */
    didYouLikeThisPuzzle: string;
    /** Difficulty level */
    difficultyLevel: string;
    /** Down vote puzzle */
    downVote: string;
    /** Easier */
    easier: string;
    /** Easiest */
    easiest: string;
    /** Example */
    example: string;
    /** incorrect */
    failed: string;
    /** Find the best move for black. */
    findTheBestMoveForBlack: string;
    /** Find the best move for white. */
    findTheBestMoveForWhite: string;
    /** From game %s */
    fromGameLink: I18nFormat;
    /** From my games */
    fromMyGames: string;
    /** You have no puzzles in the database, but Lichess still loves you very much. */
    fromMyGamesNone: string;
    /** Goals */
    goals: string;
    /** Good move */
    goodMove: string;
    /** Harder */
    harder: string;
    /** Hardest */
    hardest: string;
    /** hidden */
    hidden: string;
    /** Puzzle history */
    history: string;
    /** Improvement areas */
    improvementAreas: string;
    /** Train these to optimize your progress! */
    improvementAreasDescription: string;
    /** Jump to next puzzle immediately */
    jumpToNextPuzzleImmediately: string;
    /** Keep going… */
    keepGoing: string;
    /** Lengths */
    lengths: string;
    /** Lookup puzzles from a player's games */
    lookupOfPlayer: string;
    /** Mates */
    mates: string;
    /** Mate themes */
    mateThemes: string;
    /** Motifs */
    motifs: string;
    /** %s played */
    nbPlayed: I18nPlural;
    /** %s points above your puzzle rating */
    nbPointsAboveYourPuzzleRating: I18nPlural;
    /** %s points below your puzzle rating */
    nbPointsBelowYourPuzzleRating: I18nPlural;
    /** %s to replay */
    nbToReplay: I18nPlural;
    /** New streak */
    newStreak: string;
    /** Next puzzle */
    nextPuzzle: string;
    /** Nothing to show, go play some puzzles first! */
    noPuzzlesToShow: string;
    /** Normal */
    normal: string;
    /** That's not the move! */
    notTheMove: string;
    /** Openings you played the most in rated games */
    openingsYouPlayedTheMost: string;
    /** Origin */
    origin: string;
    /** %s solved */
    percentSolved: I18nFormat;
    /** Phases */
    phases: string;
    /** Played %s times */
    playedXTimes: I18nPlural;
    /** Puzzle complete! */
    puzzleComplete: string;
    /** Puzzle Dashboard */
    puzzleDashboard: string;
    /** Train, analyse, improve */
    puzzleDashboardDescription: string;
    /** Puzzle %s */
    puzzleId: I18nFormat;
    /** Puzzle of the day */
    puzzleOfTheDay: string;
    /** Puzzles */
    puzzles: string;
    /** Puzzles by openings */
    puzzlesByOpenings: string;
    /** %1$s puzzles found in games by %2$s */
    puzzlesFoundInUserGames: I18nPlural;
    /** Success! */
    puzzleSuccess: string;
    /** Puzzle Themes */
    puzzleThemes: string;
    /** Rating: %s */
    ratingX: I18nFormat;
    /** Recommended */
    recommended: string;
    /** Search puzzles */
    searchPuzzles: string;
    /** solved */
    solved: string;
    /** Special moves */
    specialMoves: string;
    /** Solve progressively harder puzzles and build a win streak. There is no clock, so take your time. One wrong move, and it's game over! But you can skip one move per session. */
    streakDescription: string;
    /** Skip this move to preserve your streak! Only works once per run. */
    streakSkipExplanation: string;
    /** You perform the best in these themes */
    strengthDescription: string;
    /** Strengths */
    strengths: string;
    /** To get personalized puzzles: */
    toGetPersonalizedPuzzles: string;
    /** Try something else. */
    trySomethingElse: string;
    /** Up vote puzzle */
    upVote: string;
    /** Use Ctrl+f to find your favourite opening! */
    useCtrlF: string;
    /** Use "Find in page" in the browser menu to find your favourite opening! */
    useFindInPage: string;
    /** Vote to load the next one! */
    voteToLoadNextOne: string;
    /** Your puzzle rating will not change. Note that puzzles are not a competition. Your rating helps selecting the best puzzles for your current skill. */
    yourPuzzleRatingWillNotChange: string;
    /** Your streak: %s */
    yourStreakX: I18nFormat;
  };
  puzzleTheme: {
    /** Advanced pawn */
    advancedPawn: string;
    /** One of your pawns is deep into the opponent position, maybe threatening to promote. */
    advancedPawnDescription: string;
    /** Advantage */
    advantage: string;
    /** Seize your chance to get a decisive advantage. (200cp ≤ eval ≤ 600cp) */
    advantageDescription: string;
    /** Anastasia's mate */
    anastasiaMate: string;
    /** A knight and rook or queen team up to trap the opposing king between the side of the board and a friendly piece. */
    anastasiaMateDescription: string;
    /** Arabian mate */
    arabianMate: string;
    /** A knight and a rook team up to trap the opposing king on a corner of the board. */
    arabianMateDescription: string;
    /** Attacking f2 or f7 */
    attackingF2F7: string;
    /** An attack focusing on the f2 or f7 pawn, such as in the fried liver opening. */
    attackingF2F7Description: string;
    /** Attraction */
    attraction: string;
    /** An exchange or sacrifice encouraging or forcing an opponent piece to a square that allows a follow-up tactic. */
    attractionDescription: string;
    /** Back rank mate */
    backRankMate: string;
    /** Checkmate the king on the home rank, when it is trapped there by its own pieces. */
    backRankMateDescription: string;
    /** Balestra mate */
    balestraMate: string;
    /** A bishop delivers the checkmate, while a queen blocks the remaining escape squares */
    balestraMateDescription: string;
    /** Bishop endgame */
    bishopEndgame: string;
    /** An endgame with only bishops and pawns. */
    bishopEndgameDescription: string;
    /** Blind Swine mate */
    blindSwineMate: string;
    /** Two rooks team up to mate the king in an area of 2 by 2 squares. */
    blindSwineMateDescription: string;
    /** Boden's mate */
    bodenMate: string;
    /** Two attacking bishops on criss-crossing diagonals deliver mate to a king obstructed by friendly pieces. */
    bodenMateDescription: string;
    /** Capture the defender */
    capturingDefender: string;
    /** Removing a piece that is critical to defence of another piece, allowing the now undefended piece to be captured on a following move. */
    capturingDefenderDescription: string;
    /** Castling */
    castling: string;
    /** Bring the king to safety, and deploy the rook for attack. */
    castlingDescription: string;
    /** Clearance */
    clearance: string;
    /** A move, often with tempo, that clears a square, file or diagonal for a follow-up tactical idea. */
    clearanceDescription: string;
    /** Corner mate */
    cornerMate: string;
    /** Confine the king to the corner using a rook or queen and a knight to engage the checkmate. */
    cornerMateDescription: string;
    /** Crushing */
    crushing: string;
    /** Spot the opponent blunder to obtain a crushing advantage. (eval ≥ 600cp) */
    crushingDescription: string;
    /** Defensive move */
    defensiveMove: string;
    /** A precise move or sequence of moves that is needed to avoid losing material or another advantage. */
    defensiveMoveDescription: string;
    /** Deflection */
    deflection: string;
    /** A move that distracts an opponent piece from another duty that it performs, such as guarding a key square. Sometimes also called "overloading". */
    deflectionDescription: string;
    /** Discovered attack */
    discoveredAttack: string;
    /** Moving a piece (such as a knight), that previously blocked an attack by a long range piece (such as a rook), out of the way of that piece. */
    discoveredAttackDescription: string;
    /** Discovered check */
    discoveredCheck: string;
    /** Move a piece to reveal a check from a hidden attacking piece, which often leads to a decisive advantage. */
    discoveredCheckDescription: string;
    /** Double bishop mate */
    doubleBishopMate: string;
    /** Two attacking bishops on adjacent diagonals deliver mate to a king obstructed by friendly pieces. */
    doubleBishopMateDescription: string;
    /** Double check */
    doubleCheck: string;
    /** Checking with two pieces at once, as a result of a discovered attack where both the moving piece and the unveiled piece attack the opponent's king. */
    doubleCheckDescription: string;
    /** Dovetail mate */
    dovetailMate: string;
    /** A queen delivers mate to an adjacent king, whose only two escape squares are obstructed by friendly pieces. */
    dovetailMateDescription: string;
    /** Endgame */
    endgame: string;
    /** A tactic during the last phase of the game. */
    endgameDescription: string;
    /** A tactic involving the en passant rule, where a pawn can capture an opponent pawn that has bypassed it using its initial two-square move. */
    enPassantDescription: string;
    /** Equality */
    equality: string;
    /** Come back from a losing position, and secure a draw or a balanced position. (eval ≤ 200cp) */
    equalityDescription: string;
    /** Exposed king */
    exposedKing: string;
    /** A tactic involving a king with few defenders around it, often leading to checkmate. */
    exposedKingDescription: string;
    /** Fork */
    fork: string;
    /** A move where the moved piece attacks two opponent pieces at once. */
    forkDescription: string;
    /** Hanging piece */
    hangingPiece: string;
    /** A tactic involving an opponent piece being undefended or insufficiently defended and free to capture. */
    hangingPieceDescription: string;
    /** Hook mate */
    hookMate: string;
    /** Checkmate with a rook, knight, and pawn along with one enemy pawn to limit the enemy king's escape. */
    hookMateDescription: string;
    /** Interference */
    interference: string;
    /** Moving a piece between two opponent pieces to leave one or both opponent pieces undefended, such as a knight on a defended square between two rooks. */
    interferenceDescription: string;
    /** Intermezzo */
    intermezzo: string;
    /** Instead of playing the expected move, first interpose another move posing an immediate threat that the opponent must answer. Also known as "Zwischenzug" or "In between". */
    intermezzoDescription: string;
    /** Kill box mate */
    killBoxMate: string;
    /** A rook is next to the enemy king and supported by a queen that also blocks the king's escape squares. The rook and the queen catch the enemy king in a 3 by 3 "kill box". */
    killBoxMateDescription: string;
    /** Kingside attack */
    kingsideAttack: string;
    /** An attack of the opponent's king, after they castled on the king side. */
    kingsideAttackDescription: string;
    /** Knight endgame */
    knightEndgame: string;
    /** An endgame with only knights and pawns. */
    knightEndgameDescription: string;
    /** Long puzzle */
    long: string;
    /** Three moves to win. */
    longDescription: string;
    /** Master games */
    master: string;
    /** Puzzles from games played by titled players. */
    masterDescription: string;
    /** Master vs Master games */
    masterVsMaster: string;
    /** Puzzles from games between two titled players. */
    masterVsMasterDescription: string;
    /** Checkmate */
    mate: string;
    /** Win the game with style. */
    mateDescription: string;
    /** Mate in 1 */
    mateIn1: string;
    /** Deliver checkmate in one move. */
    mateIn1Description: string;
    /** Mate in 2 */
    mateIn2: string;
    /** Deliver checkmate in two moves. */
    mateIn2Description: string;
    /** Mate in 3 */
    mateIn3: string;
    /** Deliver checkmate in three moves. */
    mateIn3Description: string;
    /** Mate in 4 */
    mateIn4: string;
    /** Deliver checkmate in four moves. */
    mateIn4Description: string;
    /** Mate in 5 or more */
    mateIn5: string;
    /** Figure out a long mating sequence. */
    mateIn5Description: string;
    /** Middlegame */
    middlegame: string;
    /** A tactic during the second phase of the game. */
    middlegameDescription: string;
    /** Healthy mix */
    mix: string;
    /** A bit of everything. You don't know what to expect, so you remain ready for anything! Just like in real games. */
    mixDescription: string;
    /** Morphy's mate */
    morphysMate: string;
    /** Use the bishop to check the king, while your rook helps to confine it. */
    morphysMateDescription: string;
    /** One-move puzzle */
    oneMove: string;
    /** A puzzle that is only one move long. */
    oneMoveDescription: string;
    /** Opening */
    opening: string;
    /** A tactic during the first phase of the game. */
    openingDescription: string;
    /** Opera mate */
    operaMate: string;
    /** Check the king with a rook and use a bishop to defend the rook. */
    operaMateDescription: string;
    /** Pawn endgame */
    pawnEndgame: string;
    /** An endgame with only pawns. */
    pawnEndgameDescription: string;
    /** Pillsbury's mate */
    pillsburysMate: string;
    /** The rook delivers checkmate, while the bishop helps to confine it. */
    pillsburysMateDescription: string;
    /** Pin */
    pin: string;
    /** A tactic involving pins, where a piece is unable to move without revealing an attack on a higher value piece. */
    pinDescription: string;
    /** Player games */
    playerGames: string;
    /** Lookup puzzles generated from your games, or from another player's games. */
    playerGamesDescription: string;
    /** Promotion */
    promotion: string;
    /** Promote one of your pawn to a queen or minor piece. */
    promotionDescription: string;
    /** These puzzles are in the public domain, and can be downloaded from %s. */
    puzzleDownloadInformation: I18nFormat;
    /** Queen endgame */
    queenEndgame: string;
    /** An endgame with only queens and pawns. */
    queenEndgameDescription: string;
    /** Queen and Rook */
    queenRookEndgame: string;
    /** An endgame with only queens, rooks and pawns. */
    queenRookEndgameDescription: string;
    /** Queenside attack */
    queensideAttack: string;
    /** An attack of the opponent's king, after they castled on the queen side. */
    queensideAttackDescription: string;
    /** Quiet move */
    quietMove: string;
    /** A move that does neither make a check or capture, nor an immediate threat to capture, but does prepare a more hidden unavoidable threat for a later move. */
    quietMoveDescription: string;
    /** Rook endgame */
    rookEndgame: string;
    /** An endgame with only rooks and pawns. */
    rookEndgameDescription: string;
    /** Sacrifice */
    sacrifice: string;
    /** A tactic involving giving up material in the short-term, to gain an advantage again after a forced sequence of moves. */
    sacrificeDescription: string;
    /** Short puzzle */
    short: string;
    /** Two moves to win. */
    shortDescription: string;
    /** Skewer */
    skewer: string;
    /** A motif involving a high value piece being attacked, moving out the way, and allowing a lower value piece behind it to be captured or attacked, the inverse of a pin. */
    skewerDescription: string;
    /** Smothered mate */
    smotheredMate: string;
    /** A checkmate delivered by a knight in which the mated king is unable to move because it is surrounded (or smothered) by its own pieces. */
    smotheredMateDescription: string;
    /** Super GM games */
    superGM: string;
    /** Puzzles from games played by the best players in the world. */
    superGMDescription: string;
    /** Trapped piece */
    trappedPiece: string;
    /** A piece is unable to escape capture as it has limited moves. */
    trappedPieceDescription: string;
    /** Triangle mate */
    triangleMate: string;
    /** The queen and rook, one square away from the enemy king, are on the same rank or file, separated by one square, forming a triangle. */
    triangleMateDescription: string;
    /** Underpromotion */
    underPromotion: string;
    /** Promotion to a knight, bishop, or rook. */
    underPromotionDescription: string;
    /** Very long puzzle */
    veryLong: string;
    /** Four moves or more to win. */
    veryLongDescription: string;
    /** Vukovic mate */
    vukovicMate: string;
    /** A rook and knight team up to mate the king. The rook delivers mate while supported by a third piece, and the knight is used to block the king's escape squares. */
    vukovicMateDescription: string;
    /** X-Ray attack */
    xRayAttack: string;
    /** A piece attacks or defends a square, through an enemy piece. */
    xRayAttackDescription: string;
    /** Zugzwang */
    zugzwang: string;
    /** The opponent is limited in the moves they can make, and all moves worsen their position. */
    zugzwangDescription: string;
  };
  recap: {
    /** What have you been up to this year? */
    awaitQuestion: string;
    /** Your best chess foes */
    chessFoes: string;
    /** is how you started %s of your games as white */
    firstMoveStats: I18nFormat;
    /** What did it take to get there? */
    gamesNextQuestion: string;
    /** And you won %s! */
    gamesYouWon: I18nFormat;
    /** Hi, %s */
    hiUser: I18nFormat;
    /** What a chess year you've had! */
    initTitle: string;
    /** %s of them were yours. */
    lichessGamesOfThemYours: I18nFormat;
    /** %1$s games played on Lichess in %2$s */
    lichessGamesPlayedIn: I18nFormat;
    /** We didn't use your device against you */
    malwareNoAbuse: string;
    /** %s ads and trackers loaded */
    malwareNoneLoaded: I18nFormat;
    /** We didn't sell your personal data */
    malwareNoSell: string;
    /** be careful */
    malwareWarningCta: string;
    /** But other websites do, so please %s. */
    malwareWarningPrefix: I18nFormat;
    /** That's %s of wood pushed! */
    movesOfWoodPushed: I18nFormat;
    /** Standard pieces weigh about 40g each */
    movesStandardPiecesWeight: string;
    /** %s grams */
    nbGrams: I18nPlural;
    /** %s kilograms */
    nbKilograms: I18nPlural;
    /** %s moves */
    nbMoves: I18nPlural;
    /** %s moves played */
    nbMovesPlayed: I18nPlural;
    /** Wanna play now? */
    noGamesCta: string;
    /** You did not play any games this year. */
    noGamesText: string;
    /** Your most played opening as black with %s games */
    openingsMostPlayedAsBlack: I18nPlural;
    /** Your most played opening as white with %s games */
    openingsMostPlayedAsWhite: I18nPlural;
    /** We're a charity, running purely on donations. */
    patronCharity: string;
    /** If we helped entertain you this year, or you believe in our work, please consider %s! */
    patronConsiderDonating: I18nFormat;
    /** costs */
    patronCosts: string;
    /** Lichess's %1$s this year were %2$s. */
    patronCostsThisYear: I18nFormat;
    /** supporting us with a donation */
    patronMakeDonation: string;
    /** What time controls and variants did you play? */
    perfsTitle: string;
    /** You also helped tag %s of them. */
    puzzlesHelpedTagging: I18nFormat;
    /** You did not solve any puzzles this year. */
    puzzlesNone: string;
    /** Thank you for voting on %s puzzles. */
    puzzlesThanksVoting: I18nPlural;
    /** Wanna try some now? */
    puzzlesTryNow: string;
    /** You won %s of them on the first try! */
    puzzlesYouWonOnFirstTry: I18nFormat;
    /** Your %s recap is ready! */
    recapReady: I18nFormat;
    /** favourite time control */
    shareableFavouriteTimeControl: string;
    /** favourite variant */
    shareableFavouriteVariant: string;
    /** most played opponent */
    shareableMostPlayedOpponent: string;
    /** %s puzzles solved */
    shareableNbPuzzlesSolved: I18nPlural;
    /** spent playing */
    shareableSpentPlaying: string;
    /** My %s recap */
    shareableTitle: I18nFormat;
    /** Where did you find games? */
    sourcesTitle: string;
    /** We're glad you're here. Have a great %s! */
    thanksHaveAGreat: I18nFormat;
    /** Thank you for playing on Lichess! */
    thanksTitle: string;
    /** That is a lot of chess. */
    timeALot: string;
    /** How many moves did you play in all that time? */
    timeHowManyMoves: string;
    /** That seems like a reasonable amount of chess. */
    timeReasonable: string;
    /** %s spent playing! */
    timeSpentPlayingExclam: I18nFormat;
    /** That is way too much chess. */
    timeTooMuch: string;
  };
  search: {
    /** Advanced search */
    advancedSearch: string;
    /** A.I. level */
    aiLevel: string;
    /** Analysis */
    analysis: string;
    /** Ascending */
    ascending: string;
    /** Colour */
    color: string;
    /** Date */
    date: string;
    /** Descending */
    descending: string;
    /** Evaluation */
    evaluation: string;
    /** From */
    from: string;
    /** %s games found */
    gamesFound: I18nPlural;
    /** Whether the player's opponent was human or a computer */
    humanOrComputer: string;
    /** Include */
    include: string;
    /** Loser */
    loser: string;
    /** Maximum number */
    maxNumber: string;
    /** The maximum number of games to return */
    maxNumberExplanation: string;
    /** Number of turns */
    nbTurns: string;
    /** Only games where a computer analysis is available */
    onlyAnalysed: string;
    /** Opponent name */
    opponentName: string;
    /** The average rating of both players */
    ratingExplanation: string;
    /** Result */
    result: string;
    /** Search */
    search: string;
    /** Search in %s chess games */
    searchInXGames: I18nPlural;
    /** Sort by */
    sortBy: string;
    /** Source */
    source: string;
    /** To */
    to: string;
    /** Winner colour */
    winnerColor: string;
    /** %s games found */
    xGamesFound: I18nPlural;
  };
  settings: {
    /** Cancel and keep my account */
    cancelKeepAccount: string;
    /** The username will NOT be available for registration again. */
    cantOpenSimilarAccount: string;
    /** Close account */
    closeAccount: string;
    /** Are you sure you want to close your account? */
    closeAccountAreYouSure: string;
    /** Your account is managed, and cannot be closed. */
    managedAccountCannotBeClosed: string;
    /** Settings */
    settings: string;
    /** This account is closed. */
    thisAccountIsClosed: string;
  };
  site: {
    /** Abort game */
    abortGame: string;
    /** Abort the game */
    abortTheGame: string;
    /** About */
    about: string;
    /** Simuls involve a single player facing several players at once. */
    aboutSimul: string;
    /** Out of 50 opponents, Fischer won 47 games, drew 2 and lost 1. */
    aboutSimulImage: string;
    /** The concept is taken from real world events. In real life, this involves the simul host moving from table to table to play a single move. */
    aboutSimulRealLife: string;
    /** When the simul starts, every player starts a game with the host. The simul ends when all games are complete. */
    aboutSimulRules: string;
    /** Simuls are always casual. Rematches, takebacks and adding time are disabled. */
    aboutSimulSettings: string;
    /** About %s */
    aboutX: I18nFormat;
    /** Accept */
    accept: string;
    /** Accessibility */
    accessibility: string;
    /** You can login right now as %s. */
    accountCanLogin: I18nFormat;
    /** The account %s is closed. */
    accountClosed: I18nFormat;
    /** You do not need a confirmation email. */
    accountConfirmationEmailNotNeeded: string;
    /** The user %s is successfully confirmed. */
    accountConfirmed: I18nFormat;
    /** The account %s was registered without an email. */
    accountRegisteredWithoutEmail: I18nFormat;
    /** Accuracy */
    accuracy: string;
    /** Active players */
    activePlayers: string;
    /** Add current variation */
    addCurrentVariation: string;
    /** Advanced settings */
    advancedSettings: string;
    /** Advantage */
    advantage: string;
    /** I agree that I will at no time receive assistance during my games (from a chess computer, book, database or another person). */
    agreementAssistance: string;
    /** I agree that I will not create multiple accounts (except for the reasons stated in the %s). */
    agreementMultipleAccounts: I18nFormat;
    /** I agree that I will always be respectful to other players. */
    agreementNice: string;
    /** I agree that I will follow all Lichess policies. */
    agreementPolicy: string;
    /** %1$s level %2$s */
    aiNameLevelAiLevel: I18nFormat;
    /** All information is public and optional. */
    allInformationIsPublicAndOptional: string;
    /** All languages */
    allLanguages: string;
    /** All set! */
    allSet: string;
    /** All squares of the board */
    allSquaresOfTheBoard: string;
    /** Always */
    always: string;
    /** Analysis board */
    analysis: string;
    /** Analysis options */
    analysisOptions: string;
    /** Press right-click (or shift+click) to draw circles and arrows on the board. For other colours, combine the following with right-click: */
    analysisShapesHowTo: string;
    /** and save %s premove lines */
    andSaveNbPremoveLines: I18nPlural;
    /** Anonymous */
    anonymous: string;
    /** Another was %s */
    anotherWasX: I18nFormat;
    /** Submit */
    apply: string;
    /** as black */
    asBlack: string;
    /** Your account is managed. Ask your chess teacher about lifting kid mode. */
    askYourChessTeacherAboutLiftingKidMode: string;
    /** as white */
    asWhite: string;
    /** Automatically proceed to next game after moving */
    automaticallyProceedToNextGameAfterMoving: string;
    /** Auto switch */
    autoSwitch: string;
    /** Available in %s languages! */
    availableInNbLanguages: I18nPlural;
    /** Average centipawn loss */
    averageCentipawnLoss: string;
    /** Average rating */
    averageElo: string;
    /** Average opponent */
    averageOpponent: string;
    /** Average rating: %s */
    averageRatingX: I18nFormat;
    /** Background */
    background: string;
    /** Background image URL: */
    backgroundImageUrl: string;
    /** Back to game */
    backToGame: string;
    /** Back to tournament */
    backToTournament: string;
    /** Best move arrow */
    bestMoveArrow: string;
    /** Best was %s */
    bestWasX: I18nFormat;
    /** Better than %1$s of %2$s players */
    betterThanPercentPlayers: I18nFormat;
    /** Beware, the game is rated but has no clock! */
    bewareTheGameIsRatedButHasNoClock: string;
    /** Biography */
    biography: string;
    /** Talk about yourself, your interests, what you like in chess, your favourite openings, players, ... */
    biographyDescription: string;
    /** Black */
    black: string;
    /** Black accepts takeback */
    blackAcceptsTakeback: string;
    /** Black cancels takeback */
    blackCancelsTakeback: string;
    /** Black O-O */
    blackCastlingKingside: string;
    /** Black to checkmate in one move */
    blackCheckmatesInOneMove: string;
    /** Black declines draw */
    blackDeclinesDraw: string;
    /** Black declines takeback */
    blackDeclinesTakeback: string;
    /** Black didn't move */
    blackDidntMove: string;
    /** Black is victorious */
    blackIsVictorious: string;
    /** Black left the game */
    blackLeftTheGame: string;
    /** Black offers draw */
    blackOffersDraw: string;
    /** Black to play */
    blackPlays: string;
    /** Black proposes takeback */
    blackProposesTakeback: string;
    /** Black resigned */
    blackResigned: string;
    /** Black time out */
    blackTimeOut: string;
    /** Black wins */
    blackWins: string;
    /** Black wins */
    blackWinsGame: string;
    /** You have used the same password on another site, and that site has been compromised. To ensure the safety of your Lichess account, we need you to set a new password. Thank you for your understanding. */
    blankedPassword: string;
    /** Blitz */
    blitz: string;
    /** Fast games: 3 to 8 minutes */
    blitzDesc: string;
    /** Block */
    block: string;
    /** Blocked */
    blocked: string;
    /** %s blocks */
    blocks: I18nPlural;
    /** Blog */
    blog: string;
    /** Blunder */
    blunder: string;
    /** Board */
    board: string;
    /** Board editor */
    boardEditor: string;
    /** Reset colours to default */
    boardReset: string;
    /** Bookmark this game */
    bookmarkThisGame: string;
    /** Brightness */
    brightness: string;
    /** Bullet */
    bullet: string;
    /** Very fast games: less than 3 minutes */
    bulletDesc: string;
    /** by %s */
    by: I18nFormat;
    /** By CPL */
    byCPL: string;
    /** By registering, you agree to the %s. */
    byRegisteringYouAgreeToBeBoundByOur: I18nFormat;
    /** Calculating moves... */
    calculatingMoves: string;
    /** Cancel */
    cancel: string;
    /** Cancel rematch offer */
    cancelRematchOffer: string;
    /** Cancel the simul */
    cancelSimul: string;
    /** Cancel the tournament */
    cancelTournament: string;
    /** Please solve the chess captcha. */
    'captcha.fail': string;
    /** Capture */
    capture: string;
    /** Castling rights */
    castling: string;
    /** Casual */
    casual: string;
    /** Casual */
    casualTournament: string;
    /** Challenge a friend */
    challengeAFriend: string;
    /** Challenge %s */
    challengeX: I18nFormat;
    /** Change email */
    changeEmail: string;
    /** Change password */
    changePassword: string;
    /** Change username */
    changeUsername: string;
    /** Change your username. This can only be done once and you are only allowed to change the case of the letters in your username. */
    changeUsernameDescription: string;
    /** Only the case of the letters can change. For example "johndoe" to "JohnDoe". */
    changeUsernameNotSame: string;
    /** Chat */
    chat: string;
    /** Chat room */
    chatRoom: string;
    /** Cheat */
    cheat: string;
    /** Cheat Detected */
    cheatDetected: string;
    /** Checkable king */
    checkableKing: string;
    /** Check all junk, spam, and other folders */
    checkAllEmailFolders: string;
    /** Checkmate */
    checkmate: string;
    /** Also check your spam folder, it might end up there. If so, mark it as not spam. */
    checkSpamFolder: string;
    /** Check your Email */
    checkYourEmail: string;
    /** Chess960 start position: %s */
    chess960StartPosition: I18nFormat;
    /** Chess basics */
    chessBasics: string;
    /** Claim a draw */
    claimADraw: string;
    /** Classical */
    classical: string;
    /** Classical games: 25 minutes and more */
    classicalDesc: string;
    /** Clear board */
    clearBoard: string;
    /** Clear moves */
    clearSavedMoves: string;
    /** Clear search */
    clearSearch: string;
    /** Click here to read it */
    clickHereToReadIt: string;
    /** Click on the board to make your move, and prove you are human. */
    clickOnTheBoardToMakeYourMove: string;
    /** [Click to reveal email address] */
    clickToRevealEmailAddress: string;
    /** Clock */
    clock: string;
    /** Clock increment */
    clockIncrement: string;
    /** Clock initial time */
    clockInitialTime: string;
    /** Close */
    close: string;
    /** Closing your account will withdraw your appeal */
    closingAccountWithdrawAppeal: string;
    /** Cloud analysis */
    cloudAnalysis: string;
    /** Coaches */
    coaches: string;
    /** Coach manager */
    coachManager: string;
    /** Collapse variations */
    collapseVariations: string;
    /** Community */
    community: string;
    /** Message */
    composeMessage: string;
    /** Computer */
    computer: string;
    /** Computer analysis */
    computerAnalysis: string;
    /** Computer analysis available */
    computerAnalysisAvailable: string;
    /** Computer analysis disabled */
    computerAnalysisDisabled: string;
    /** Computers and computer-assisted players are not allowed to play. Please do not get assistance from chess engines, databases, or from other players while playing. Also note that making multiple accounts is strongly discouraged and excessive multi-accounting will lead to being banned. */
    computersAreNotAllowedToPlay: string;
    /** Computer thinking ... */
    computerThinking: string;
    /** Conditional premoves */
    conditionalPremoves: string;
    /** Entry requirements: */
    conditionOfEntry: string;
    /** Confirm move */
    confirmMove: string;
    /** Congratulations, you won! */
    congratsYouWon: string;
    /** Continue from here */
    continueFromHere: string;
    /** Contrast */
    contrast: string;
    /** Contribute */
    contribute: string;
    /** Copy mainline PGN */
    copyMainLinePgn: string;
    /** Copy and paste the above text and send it to %s */
    copyTextToEmail: I18nFormat;
    /** Copy to clipboard */
    copyToClipboard: string;
    /** Copy variation PGN */
    copyVariationPgn: string;
    /** Correspondence */
    correspondence: string;
    /** Correspondence games: one or several days per move */
    correspondenceDesc: string;
    /** Country or region */
    countryRegion: string;
    /** CPUs */
    cpus: string;
    /** Create */
    create: string;
    /** Create a game */
    createAGame: string;
    /** Create a new topic */
    createANewTopic: string;
    /** Create a new tournament */
    createANewTournament: string;
    /** Newly created simuls */
    createdSimuls: string;
    /** Create lobby game */
    createLobbyGame: string;
    /** Create the game */
    createTheGame: string;
    /** Create the topic */
    createTheTopic: string;
    /** Crosstable */
    crosstable: string;
    /** Cumulative */
    cumulative: string;
    /** Current games */
    currentGames: string;
    /** Current match score */
    currentMatchScore: string;
    /** Current password */
    currentPassword: string;
    /** Custom */
    custom: string;
    /** Custom position */
    customPosition: string;
    /** Cycle previous/next variation */
    cyclePreviousOrNextVariation: string;
    /** Dark */
    dark: string;
    /** Database */
    database: string;
    /** Days per turn */
    daysPerTurn: string;
    /** Decline */
    decline: string;
    /** Defeat */
    defeat: string;
    /** %1$s vs %2$s in %3$s */
    defeatVsYInZ: I18nFormat;
    /** Delete */
    delete: string;
    /** Delete from here */
    deleteFromHere: string;
    /** Delete this imported game? */
    deleteThisImportedGame: string;
    /** Depth %s */
    depthX: I18nFormat;
    /** Private description */
    descPrivate: string;
    /** Text that only the team members will see. If set, replaces the public description for team members. */
    descPrivateHelp: string;
    /** Description */
    description: string;
    /** Device theme */
    deviceTheme: string;
    /** Disable blind mode */
    disableBlindMode: string;
    /** Disable Kid mode */
    disableKidMode: string;
    /** Conversations */
    discussions: string;
    /** Do it again */
    doItAgain: string;
    /** Done reviewing black mistakes */
    doneReviewingBlackMistakes: string;
    /** Done reviewing white mistakes */
    doneReviewingWhiteMistakes: string;
    /** Download */
    download: string;
    /** Download all games */
    downloadAllGames: string;
    /** Download annotated */
    downloadAnnotated: string;
    /** Download imported */
    downloadImported: string;
    /** Download raw */
    downloadRaw: string;
    /** Draw */
    draw: string;
    /** The game has been drawn by the fifty move rule. */
    drawByFiftyMoves: string;
    /** Draw by mutual agreement */
    drawByMutualAgreement: string;
    /** Draw claimed */
    drawClaimed: string;
    /** Drawn */
    drawn: string;
    /** Draw offer accepted */
    drawOfferAccepted: string;
    /** Draw offer sent */
    drawOfferSent: string;
    /** Draw rate */
    drawRate: string;
    /** Draws */
    draws: string;
    /** %1$s vs %2$s in %3$s */
    drawVsYInZ: I18nFormat;
    /** DTZ50'' with rounding, based on number of half-moves until next capture, pawn move, or checkmate */
    dtzWithRounding: string;
    /** Duration */
    duration: string;
    /** Edit */
    edit: string;
    /** Edit profile */
    editProfile: string;
    /** Email */
    email: string;
    /** Email address associated to the account */
    emailAssociatedToaccount: string;
    /** It can take some time to arrive. */
    emailCanTakeSomeTime: string;
    /** Help with email confirmation */
    emailConfirmHelp: string;
    /** Didn't receive your confirmation email after signing up? */
    emailConfirmNotReceived: string;
    /** If everything else fails, then send us this email: */
    emailForSignupHelp: string;
    /** Email me a link */
    emailMeALink: string;
    /** We have sent an email to %s. */
    emailSent: I18nFormat;
    /** Do not set an email address suggested by someone else. They will use it to steal your account. */
    emailSuggestion: string;
    /** Embed in your website */
    embedInYourWebsite: string;
    /** Paste a game URL or a study chapter URL to embed it. */
    embedsAvailable: string;
    /** Leave empty to name the tournament after a notable chess player. */
    emptyTournamentName: string;
    /** Enable */
    enable: string;
    /** Enable blind mode */
    enableBlindMode: string;
    /** Enable Kid mode */
    enableKidMode: string;
    /** Endgame */
    endgame: string;
    /** Endgame positions */
    endgamePositions: string;
    /** Error loading engine */
    engineFailed: string;
    /** En passant rights */
    enPassant: string;
    /** This email address is invalid */
    'error.email': string;
    /** This email address is not acceptable. Please double-check it, and try again. */
    'error.email_acceptable': string;
    /** This is already your email address */
    'error.email_different': string;
    /** Email address invalid or already taken */
    'error.email_unique': string;
    /** Must be at most %s */
    'error.max': I18nFormat;
    /** Must be at most %s characters long */
    'error.maxLength': I18nFormat;
    /** Must be at least %s */
    'error.min': I18nFormat;
    /** Must be at least %s characters long */
    'error.minLength': I18nFormat;
    /** Please don't use your username as your password. */
    'error.namePassword': string;
    /** Please provide at least one link to a game with suspected cheating. */
    'error.provideOneCheatedGameLink': string;
    /** This field is required */
    'error.required': string;
    /** Invalid value */
    'error.unknown': string;
    /** This password is extremely common, and too easy to guess. */
    'error.weakPassword': string;
    /** Estimated start time */
    estimatedStart: string;
    /** Evaluating your move ... */
    evaluatingYourMove: string;
    /** Evaluation gauge */
    evaluationGauge: string;
    /** Playing now */
    eventInProgress: string;
    /** Everybody gets all features for free */
    everybodyGetsAllFeaturesForFree: string;
    /** Expand variations */
    expandVariations: string;
    /** Export games */
    exportGames: string;
    /** Fast */
    fast: string;
    /** Favourite opponents */
    favoriteOpponents: string;
    /** Fifty moves without progress */
    fiftyMovesWithoutProgress: string;
    /** Filter games */
    filterGames: string;
    /** Find a better move for black */
    findBetterMoveForBlack: string;
    /** Find a better move for white */
    findBetterMoveForWhite: string;
    /** Finished */
    finished: string;
    /** Flair */
    flair: string;
    /** Flip board */
    flipBoard: string;
    /** Focus chat */
    focusChat: string;
    /** Follow */
    follow: string;
    /** Following */
    following: string;
    /** Follow %s */
    followX: I18nFormat;
    /** Call draw */
    forceDraw: string;
    /** Claim victory */
    forceResignation: string;
    /** Force variation */
    forceVariation: string;
    /** Forgot password? */
    forgotPassword: string;
    /** Forum */
    forum: string;
    /** Free Online Chess */
    freeOnlineChess: string;
    /** Friends */
    friends: string;
    /** Game aborted */
    gameAborted: string;
    /** Game as GIF */
    gameAsGIF: string;
    /** You have a game in progress with %s. */
    gameInProgress: I18nFormat;
    /** Game mode */
    gameMode: string;
    /** Game Over */
    gameOver: string;
    /** Games */
    games: string;
    /** Game setup */
    gameSetup: string;
    /** Games played */
    gamesPlayed: string;
    /** Game vs %1$s */
    gameVsX: I18nFormat;
    /** Get a hint */
    getAHint: string;
    /** Give %s seconds */
    giveNbSeconds: I18nPlural;
    /** Glicko-2 rating */
    glicko2Rating: string;
    /** Go deeper */
    goDeeper: string;
    /** To that effect, we must ensure that all players follow good practice. */
    goodPractice: string;
    /** Graph */
    graph: string;
    /** Hang on! */
    hangOn: string;
    /** Help: */
    help: string;
    /** Hide best move */
    hideBestMove: string;
    /** Host */
    host: string;
    /** Host a new simul */
    hostANewSimul: string;
    /** Host colour: %s */
    hostColorX: I18nFormat;
    /** How to avoid this? */
    howToAvoidThis: string;
    /** Hue */
    hue: string;
    /** Human */
    human: string;
    /** If none, leave empty */
    ifNoneLeaveEmpty: string;
    /** If rating is ± %s */
    ifRatingIsPlusMinusX: I18nFormat;
    /** If registered */
    ifRegistered: string;
    /** If you do not get the email within 5 minutes: */
    ifYouDoNotGetTheEmail: string;
    /** If you don't see the email, check other places it might be, like your junk, spam, social, or other folders. */
    ifYouDoNotSeeTheEmailCheckOtherPlaces: string;
    /** Important */
    important: string;
    /** Imported by %s */
    importedByX: I18nFormat;
    /** Import game */
    importGame: string;
    /** This PGN can be accessed by the public. To import a game privately, use a study. */
    importGameDataPrivacyWarning: string;
    /** Paste a game PGN to get a browsable replay of the main line, computer analysis, game chat and public shareable URL. */
    importGameExplanation: string;
    /** Import PGN */
    importPgn: string;
    /** Inaccuracy */
    inaccuracy: string;
    /** Anything even slightly inappropriate could get your account closed. */
    inappropriateNameWarning: string;
    /** Inbox */
    inbox: string;
    /** Incorrect password */
    incorrectPassword: string;
    /** Increment */
    increment: string;
    /** Increment in seconds */
    incrementInSeconds: string;
    /** Infinite analysis */
    infiniteAnalysis: string;
    /** In kid mode, the Lichess logo gets a %s icon, so you know your kids are safe. */
    inKidModeTheLichessLogoGetsIconX: I18nFormat;
    /** Inline notation */
    inlineNotation: string;
    /** in local browser */
    inLocalBrowser: string;
    /** Inside the board */
    insideTheBoard: string;
    /** Instructions */
    instructions: string;
    /** Insufficient material */
    insufficientMaterial: string;
    /** in the FAQ */
    inTheFAQ: string;
    /** Invalid authentication code */
    invalidAuthenticationCode: string;
    /** Invalid FEN */
    invalidFen: string;
    /** Invalid PGN */
    invalidPgn: string;
    /** Invalid username or password */
    invalidUsernameOrPassword: string;
    /** invited you to "%1$s". */
    invitedYouToX: I18nFormat;
    /** In your own local timezone */
    inYourLocalTimezone: string;
    /** Private */
    isPrivate: string;
    /** It's your turn! */
    itsYourTurn: string;
    /** Join */
    join: string;
    /** Joined %s */
    joinedX: I18nFormat;
    /** Join the game */
    joinTheGame: string;
    /** Join the %1$s, to post in this forum */
    joinTheTeamXToPost: I18nFormat;
    /** Keyboard shortcuts */
    keyboardShortcuts: string;
    /** Cycle selected variation */
    keyCycleSelectedVariation: string;
    /** enter/exit variation */
    keyEnterOrExitVariation: string;
    /** go to start/end */
    keyGoToStartOrEnd: string;
    /** move backward/forward */
    keyMoveBackwardOrForward: string;
    /** Next blunder */
    keyNextBlunder: string;
    /** Next branch */
    keyNextBranch: string;
    /** Next inaccuracy */
    keyNextInaccuracy: string;
    /** Next (Learn from your mistakes) */
    keyNextLearnFromYourMistakes: string;
    /** Next mistake */
    keyNextMistake: string;
    /** Previous branch */
    keyPreviousBranch: string;
    /** Request computer analysis, Learn from your mistakes */
    keyRequestComputerAnalysis: string;
    /** show/hide comments */
    keyShowOrHideComments: string;
    /** Kid mode */
    kidMode: string;
    /** This is about safety. In kid mode, all site communications are disabled. Enable this for your children and school students, to protect them from other internet users. */
    kidModeExplanation: string;
    /** Kid mode is enabled. */
    kidModeIsEnabled: string;
    /** King in the centre */
    kingInTheCenter: string;
    /** Language */
    language: string;
    /** Last post */
    lastPost: string;
    /** Active %s */
    lastSeenActive: I18nFormat;
    /** Latest forum posts */
    latestForumPosts: string;
    /** Leaderboard */
    leaderboard: string;
    /** Learn from this mistake */
    learnFromThisMistake: string;
    /** Learn from your mistakes */
    learnFromYourMistakes: string;
    /** Learn */
    learnMenu: string;
    /** Let other players challenge you */
    letOtherPlayersChallengeYou: string;
    /** Let other players follow you */
    letOtherPlayersFollowYou: string;
    /** Let other players invite you to study */
    letOtherPlayersInviteYouToStudy: string;
    /** Let other players message you */
    letOtherPlayersMessageYou: string;
    /** Level */
    level: string;
    /** Rated games played on Lichess */
    lichessDbExplanation: string;
    /** Lichess is a charity and entirely free/libre open source software. */
    lichessPatronInfo: string;
    /** Lichess tournaments */
    lichessTournaments: string;
    /** Lifetime score */
    lifetimeScore: string;
    /** Light */
    light: string;
    /** List */
    list: string;
    /** List players you have blocked */
    listBlockedPlayers: string;
    /** Loading engine... */
    loadingEngine: string;
    /** Load position */
    loadPosition: string;
    /** Lobby */
    lobby: string;
    /** Location */
    location: string;
    /** Sign in to chat */
    loginToChat: string;
    /** Sign out */
    logOut: string;
    /** Losing */
    losing: string;
    /** Losses */
    losses: string;
    /** Loss or 50 moves by prior mistake */
    lossOr50MovesByPriorMistake: string;
    /** Loss prevented by 50-move rule */
    lossSavedBy50MoveRule: string;
    /** You lost rating points to someone who violated the Lichess TOS */
    lostAgainstTOSViolator: string;
    /** For safekeeping and sharing, consider making a study. */
    makeAStudy: string;
    /** Make mainline */
    makeMainLine: string;
    /** Make the tournament private, and restrict access with a password */
    makePrivateTournament: string;
    /** Make sure to read %1$s */
    makeSureToRead: I18nFormat;
    /** %s is available for formatting. */
    markdownIsAvailable: I18nFormat;
    /** OTB games of %1$s+ FIDE-rated players from %2$s to %3$s */
    masterDbExplanation: I18nFormat;
    /** Mate in %s half-moves */
    mateInXHalfMoves: I18nPlural;
    /** Max depth reached! */
    maxDepthReached: string;
    /** Maximum: %s characters. */
    maximumNbCharacters: I18nPlural;
    /** Maximum weekly rating */
    maximumWeeklyRating: string;
    /** Maybe include more games from the preferences menu? */
    maybeIncludeMoreGamesFromThePreferencesMenu: string;
    /** Member since */
    memberSince: string;
    /** Memory */
    memory: string;
    /** mentioned you in "%1$s". */
    mentionedYouInX: I18nFormat;
    /** Menu */
    menu: string;
    /** Message */
    message: string;
    /** Middlegame */
    middlegame: string;
    /** Minimum rated games */
    minimumRatedGames: string;
    /** Minimum rating */
    minimumRating: string;
    /** Minutes per side */
    minutesPerSide: string;
    /** Mistake */
    mistake: string;
    /** Mobile */
    mobile: string;
    /** Mobile App */
    mobileApp: string;
    /** Mode */
    mode: string;
    /** More */
    more: string;
    /** ≥ %1$s %2$s rated games */
    moreThanNbPerfRatedGames: I18nPlural;
    /** ≥ %s rated games */
    moreThanNbRatedGames: I18nPlural;
    /** Mouse tricks */
    mouseTricks: string;
    /** Move */
    move: string;
    /** Move annotations */
    moveAnnotations: string;
    /** Moves played */
    movesPlayed: string;
    /** Move times */
    moveTimes: string;
    /** Multiple lines */
    multipleLines: string;
    /** Must be in team %s */
    mustBeInTeam: I18nFormat;
    /** Name */
    name: string;
    /** Navigate the move tree */
    navigateMoveTree: string;
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
    networkLagBetweenYouAndLichess: string;
    /** Never */
    never: string;
    /** Never type your Lichess password on another site! */
    neverTypeYourPassword: string;
    /** New opponent */
    newOpponent: string;
    /** New password */
    newPassword: string;
    /** New password (again) */
    newPasswordAgain: string;
    /** The new passwords don't match */
    newPasswordsDontMatch: string;
    /** Password strength */
    newPasswordStrength: string;
    /** New tournament */
    newTournament: string;
    /** Next */
    next: string;
    /** No */
    no: string;
    /** No challenges. */
    noChallenges: string;
    /** No chat */
    noChat: string;
    /** No conditional premoves */
    noConditionalPremoves: string;
    /** You cannot draw before 30 moves are played in a Swiss tournament. */
    noDrawBeforeSwissLimit: string;
    /** No games found */
    noGameFound: string;
    /** No mistakes found for black */
    noMistakesFoundForBlack: string;
    /** No mistakes found for white */
    noMistakesFoundForWhite: string;
    /** None */
    none: string;
    /** Offline */
    noNetwork: string;
    /** No note yet */
    noNoteYet: string;
    /** No restriction */
    noRestriction: string;
    /** Normal */
    normal: string;
    /** This simultaneous exhibition does not exist. */
    noSimulExplanation: string;
    /** Simul not found */
    noSimulFound: string;
    /** Not a checkmate */
    notACheckmate: string;
    /** Notes */
    notes: string;
    /** Nothing to see here at the moment. */
    nothingToSeeHere: string;
    /** Notifications */
    notifications: string;
    /** Notifications: %1$s */
    notificationsX: I18nFormat;
    /** %s Blunders */
    numberBlunders: I18nPlural;
    /** %s Inaccuracies */
    numberInaccuracies: I18nPlural;
    /** %s Mistakes */
    numberMistakes: I18nPlural;
    /** Offer draw */
    offerDraw: string;
    /** offline */
    offline: string;
    /** OK */
    ok: string;
    /** One day */
    oneDay: string;
    /** One URL per line. */
    oneUrlPerLine: string;
    /** online */
    online: string;
    /** Online bots */
    onlineBots: string;
    /** Online players */
    onlinePlayers: string;
    /** Only existing conversations */
    onlyExistingConversations: string;
    /** Only friends */
    onlyFriends: string;
    /** Only members of team */
    onlyMembersOfTeam: string;
    /** Only team leaders */
    onlyTeamLeaders: string;
    /** Only team members */
    onlyTeamMembers: string;
    /** On slow games */
    onSlowGames: string;
    /** Opacity */
    opacity: string;
    /** Opening */
    opening: string;
    /** Opening/endgame explorer */
    openingEndgameExplorer: string;
    /** Opening explorer */
    openingExplorer: string;
    /** Opening explorer & tablebase */
    openingExplorerAndTablebase: string;
    /** Openings */
    openings: string;
    /** Open tournaments */
    openTournaments: string;
    /** Opponent */
    opponent: string;
    /** Your opponent left the game. You can claim victory, call the game a draw, or wait. */
    opponentLeftChoices: string;
    /** Your opponent left the game. You can claim victory in %s seconds. */
    opponentLeftCounter: I18nPlural;
    /** Or let your opponent scan this QR code */
    orLetYourOpponentScanQrCode: string;
    /** Or upload a PGN file */
    orUploadPgnFile: string;
    /** Other */
    other: string;
    /** other players */
    otherPlayers: string;
    /** Our tips for organising events */
    ourEventTips: string;
    /** Outside the board */
    outsideTheBoard: string;
    /** Password */
    password: string;
    /** Password reset */
    passwordReset: string;
    /** Do not set a password suggested by someone else. They will use it to steal your account. */
    passwordSuggestion: string;
    /** Paste the FEN text here */
    pasteTheFenStringHere: string;
    /** Paste the PGN text here */
    pasteThePgnStringHere: string;
    /** Pause */
    pause: string;
    /** Pawn move */
    pawnMove: string;
    /** Performance */
    performance: string;
    /** Rating: %s */
    perfRatingX: I18nFormat;
    /** A permanent link for anyone to challenge you with these exact settings. */
    permanentLinkForAnyoneToChallengeYou: string;
    /** Piece set */
    pieceSet: string;
    /** Pinned pieces */
    pinnedPieces: string;
    /** Play */
    play: string;
    /** Play against computer */
    playAgainstComputer: string;
    /** Play chess everywhere */
    playChessEverywhere: string;
    /** Play chess in style */
    playChessInStyle: string;
    /** Play best computer move */
    playComputerMove: string;
    /** Player */
    player: string;
    /** Player names */
    playerNames: string;
    /** Players */
    players: string;
    /** Play every game you start. */
    playEveryGame: string;
    /** Play first opening/endgame-explorer move */
    playFirstOpeningEndgameExplorerMove: string;
    /** Playing right now */
    playingRightNow: string;
    /** play selected move */
    playSelectedMove: string;
    /** Play a variation to create conditional premoves */
    playVariationToCreateConditionalPremoves: string;
    /** Play %s */
    playX: I18nFormat;
    /** We aim to provide a pleasant chess experience for everyone. */
    pleasantChessExperience: string;
    /** Points */
    points: string;
    /** Popular openings */
    popularOpenings: string;
    /** Paste a valid FEN to start every game from a given position. */
    positionInputHelp: I18nFormat;
    /** Posts */
    posts: string;
    /** When a potential problem is detected, we display this message. */
    potentialProblem: string;
    /** Practice */
    practice: string;
    /** Practice with computer */
    practiceWithComputer: string;
    /** Previously on Lichess TV */
    previouslyOnLichessTV: string;
    /** Ctrl or shift = red; command, alt, or meta = blue; a key from each = yellow. */
    primaryColorArrowsHowTo: string;
    /** Privacy */
    privacy: string;
    /** Privacy policy */
    privacyPolicy: string;
    /** Proceed to %s */
    proceedToX: I18nFormat;
    /** Profile */
    profile: string;
    /** Profile completion: %s */
    profileCompletion: I18nFormat;
    /** Promote variation */
    promoteVariation: string;
    /** Propose a takeback */
    proposeATakeback: string;
    /** Chess tactics trainer */
    puzzleDesc: string;
    /** Puzzles */
    puzzles: string;
    /** Quick pairing */
    quickPairing: string;
    /** Race finished */
    raceFinished: string;
    /** Random side */
    randomColor: string;
    /** Rank */
    rank: string;
    /** Rank is updated every %s minutes */
    rankIsUpdatedEveryNbMinutes: I18nPlural;
    /** Rank: %s */
    rankX: I18nFormat;
    /** Rapid */
    rapid: string;
    /** Rapid games: 8 to 25 minutes */
    rapidDesc: string;
    /** Rated */
    rated: string;
    /** Games are rated and impact players ratings */
    ratedFormHelp: string;
    /** Rated ≤ %1$s in %2$s for the last week */
    ratedLessThanInPerf: I18nFormat;
    /** Rated ≥ %1$s in %2$s */
    ratedMoreThanInPerf: I18nFormat;
    /** Rated */
    ratedTournament: string;
    /** Rating */
    rating: string;
    /** Rating filter */
    ratingFilter: string;
    /** Rating filters are locked because your rating is not stable. Playing rated games will increase stability. */
    ratingRangeIsDisabledBecauseYourRatingIsProvisional: string;
    /** Rating stats */
    ratingStats: string;
    /** %1$s rating over %2$s games */
    ratingXOverYGames: I18nPlural;
    /** Read about our %s. */
    readAboutOur: I18nFormat;
    /** really */
    really: string;
    /** Real name */
    realName: string;
    /** Real time */
    realTime: string;
    /** Realtime */
    realtimeReplay: string;
    /** Reason */
    reason: string;
    /** Receive notifications when mentioned in the forum */
    receiveForumNotifications: string;
    /** Recent games */
    recentGames: string;
    /** Reconnecting */
    reconnecting: string;
    /** Wait 5 minutes and refresh your email inbox. */
    refreshInboxAfterFiveMinutes: string;
    /** Refund: %1$s %2$s rating points. */
    refundXpointsTimeControlY: I18nFormat;
    /** Rematch */
    rematch: string;
    /** Rematch offer accepted */
    rematchOfferAccepted: string;
    /** Rematch offer cancelled */
    rematchOfferCanceled: string;
    /** Rematch offer declined */
    rematchOfferDeclined: string;
    /** Rematch offer sent */
    rematchOfferSent: string;
    /** Keep me logged in */
    rememberMe: string;
    /** Removes the depth limit, and keeps your computer warm */
    removesTheDepthLimit: string;
    /** Reopen your account */
    reopenYourAccount: string;
    /** If you closed your account, but have since changed your mind, you get a chance of getting your account back. */
    reopenYourAccountDescription: string;
    /** Replay mode */
    replayMode: string;
    /** Replies */
    replies: string;
    /** Reply */
    reply: string;
    /** Reply to this topic */
    replyToThisTopic: string;
    /** Report a user */
    reportAUser: string;
    /** Paste the link to the game(s) and explain what is wrong about this user's behaviour. Don't just say "they cheat", but tell us how you came to this conclusion. */
    reportCheatBoostHelp: string;
    /** Your report will be processed faster if written in English. */
    reportProcessedFasterInEnglish: string;
    /** Explain what about this username is offensive. Don't just say "it's offensive/inappropriate", but tell us how you came to this conclusion, especially if the insult is obfuscated, not in english, is in slang, or is a historical/cultural reference. */
    reportUsernameHelp: string;
    /** Report %s to moderators */
    reportXToModerators: I18nFormat;
    /** Request a computer analysis */
    requestAComputerAnalysis: string;
    /** Required. */
    required: string;
    /** Reset */
    reset: string;
    /** Resign */
    resign: string;
    /** Resign lost games (don't let the clock run down). */
    resignLostGames: string;
    /** Resign the game */
    resignTheGame: string;
    /** Resume */
    resume: string;
    /** Resume learning */
    resumeLearning: string;
    /** Resume practice */
    resumePractice: string;
    /** %1$s vs %2$s */
    resVsX: I18nFormat;
    /** Retry */
    retry: string;
    /** Return to simul homepage */
    returnToSimulHomepage: string;
    /** Return to tournaments homepage */
    returnToTournamentsHomepage: string;
    /** Reusable challenge URL */
    reusableChallengeUrl: string;
    /** Review black mistakes */
    reviewBlackMistakes: string;
    /** Review white mistakes */
    reviewWhiteMistakes: string;
    /** revoke all sessions */
    revokeAllSessions: string;
    /** Pick a very safe name for the tournament. */
    safeTournamentName: string;
    /** Save */
    save: string;
    /** Screenshot current position */
    screenshotCurrentPosition: string;
    /** Scroll over computer variations to preview them. */
    scrollOverComputerVariationsToPreviewThem: string;
    /** Search */
    search: string;
    /** Search or start new conversation */
    searchOrStartNewDiscussion: string;
    /** Security */
    security: string;
    /** See best move */
    seeBestMove: string;
    /** Send */
    send: string;
    /** We've sent you an email with a link. */
    sentEmailWithLink: string;
    /** Sessions */
    sessions: string;
    /** Set your flair */
    setFlair: string;
    /** Set the board */
    setTheBoard: string;
    /** Share your chess insights data */
    shareYourInsightsData: string;
    /** Show this help dialog */
    showHelpDialog: string;
    /** Show me everything */
    showMeEverything: string;
    /** Show threat */
    showThreat: string;
    /** You have received a private message from Lichess. */
    showUnreadLichessMessage: string;
    /** Show variation arrows */
    showVariationArrows: string;
    /** Side */
    side: string;
    /** Sign in */
    signIn: string;
    /** Register */
    signUp: string;
    /** We will only use it for password reset and account activation. */
    signupEmailHint: string;
    /** Sign up to host or join a simul */
    signUpToHostOrJoinASimul: string;
    /** Make sure to choose a username that's appropriate for all ages. You cannot change it later and any accounts with inappropriate usernames will get closed! */
    signupUsernameHint: string;
    /** You may add extra initial time to your clock to help you cope with the simul. */
    simulAddExtraTime: string;
    /** Add initial time to your clock for each player joining the simul. */
    simulAddExtraTimePerPlayer: string;
    /** Fischer Clock setup. The more players you take on, the more time you may need. */
    simulClockHint: string;
    /** Simul description */
    simulDescription: string;
    /** Anything you want to tell the participants? */
    simulDescriptionHelp: string;
    /** Feature on %s */
    simulFeatured: I18nFormat;
    /** Show your simul to everyone on %s. Disable for private simuls. */
    simulFeaturedHelp: I18nFormat;
    /** Host colour for each game */
    simulHostcolor: string;
    /** Host extra initial clock time */
    simulHostExtraTime: string;
    /** Host extra clock time per player */
    simulHostExtraTimePerPlayer: string;
    /** Simultaneous exhibitions */
    simultaneousExhibitions: string;
    /** If you select several variants, each player gets to choose which one to play. */
    simulVariantsHint: string;
    /** Since */
    since: string;
    /** Free online chess server. Play chess in a clean interface. No registration, no ads, no plugin required. Play chess with the computer, friends or random opponents. */
    siteDescription: string;
    /** Size */
    size: string;
    /** Skip this move */
    skipThisMove: string;
    /** Slow */
    slow: string;
    /** Social media links */
    socialMediaLinks: string;
    /** Solution */
    solution: string;
    /** Someone you reported was banned */
    someoneYouReportedWasBanned: string;
    /** Sorry :( */
    sorry: string;
    /** Sound */
    sound: string;
    /** Source Code */
    sourceCode: string;
    /** Spectator room */
    spectatorRoom: string;
    /** Stalemate */
    stalemate: string;
    /** Standard */
    standard: string;
    /** Stand by %s, pairing players, get ready! */
    standByX: I18nFormat;
    /** Standings */
    standings: string;
    /** started streaming */
    startedStreaming: string;
    /** Starting: */
    starting: string;
    /** Starting in */
    startingIn: string;
    /** Starting position */
    startPosition: string;
    /** Stats */
    stats: string;
    /** Streamer manager */
    streamerManager: string;
    /** Streamers */
    streamersMenu: string;
    /** Strength */
    strength: string;
    /** Study */
    studyMenu: string;
    /** Subject */
    subject: string;
    /** Subscribe */
    subscribe: string;
    /** Success */
    success: string;
    /** Switch sides */
    switchSides: string;
    /** Tags */
    tags: string;
    /** Takeback */
    takeback: string;
    /** Takeback sent */
    takebackPropositionSent: string;
    /** Please be nice in the chat! */
    talkInChat: string;
    /** %1$s team */
    teamNamedX: I18nFormat;
    /** We apologise for the temporary inconvenience, */
    temporaryInconvenience: string;
    /** Terms of Service */
    termsOfService: string;
    /** Thank you! */
    thankYou: string;
    /** Thank you for reading! */
    thankYouForReading: string;
    /** The first person to come to this URL will play with you. */
    theFirstPersonToComeOnThisUrlWillPlayWithYou: string;
    /** the forum etiquette */
    theForumEtiquette: string;
    /** The game is a draw. */
    theGameIsADraw: string;
    /** Thematic */
    thematic: string;
    /** This account violated the Lichess Terms of Service */
    thisAccountViolatedTos: string;
    /** This game is rated */
    thisGameIsRated: string;
    /** This is a chess CAPTCHA. */
    thisIsAChessCaptcha: string;
    /** This topic has been archived and can no longer be replied to. */
    thisTopicIsArchived: string;
    /** This topic is now closed. */
    thisTopicIsNowClosed: string;
    /** Three checks */
    threeChecks: string;
    /** Threefold repetition */
    threefoldRepetition: string;
    /** Time */
    time: string;
    /** Time is almost up! */
    timeAlmostUp: string;
    /** Time before tournament starts */
    timeBeforeTournamentStarts: string;
    /** Time control */
    timeControl: string;
    /** Timeline */
    timeline: string;
    /** Time to process a move on Lichess's server */
    timeToProcessAMoveOnLichessServer: string;
    /** Title verification */
    titleVerification: string;
    /** Today */
    today: string;
    /** Toggle all computer analysis */
    toggleAllAnalysis: string;
    /** Toggle move annotations */
    toggleGlyphAnnotations: string;
    /** Toggle local computer analysis */
    toggleLocalAnalysis: string;
    /** Toggle local evaluation */
    toggleLocalEvaluation: string;
    /** Toggle observation annotations */
    toggleObservationAnnotations: string;
    /** Toggle position annotations */
    togglePositionAnnotations: string;
    /** Toggle the chat */
    toggleTheChat: string;
    /** Toggle variation arrows */
    toggleVariationArrows: string;
    /** To invite someone to play, give this URL */
    toInviteSomeoneToPlayGiveThisUrl: string;
    /** Tools */
    tools: string;
    /** Top games */
    topGames: string;
    /** Topics */
    topics: string;
    /** To report a user for cheating or bad behaviour, %1$s */
    toReportSomeoneForCheatingOrBadBehavior: I18nFormat;
    /** To request support, %1$s */
    toRequestSupport: I18nFormat;
    /** Study */
    toStudy: string;
    /** Tournament */
    tournament: string;
    /** Tournament calendar */
    tournamentCalendar: string;
    /** Tournament complete */
    tournamentComplete: string;
    /** This tournament does not exist. */
    tournamentDoesNotExist: string;
    /** Tournament entry code */
    tournamentEntryCode: string;
    /** Arena tournament FAQ */
    tournamentFAQ: string;
    /** Play fast-paced chess tournaments! Join an official scheduled tournament, or create your own. Bullet, Blitz, Classical, Chess960, King of the Hill, Threecheck, and more options available for endless chess fun. */
    tournamentHomeDescription: string;
    /** Chess tournaments featuring various time controls and variants */
    tournamentHomeTitle: string;
    /** The tournament may have been cancelled if all players left before it started. */
    tournamentMayHaveBeenCanceled: string;
    /** Tournament not found */
    tournamentNotFound: string;
    /** Tournament points */
    tournamentPoints: string;
    /** Tournaments */
    tournaments: string;
    /** Tournament chat */
    tournChat: string;
    /** Tournament description */
    tournDescription: string;
    /** Anything special you want to tell the participants? Try to keep it short. Markdown links are available: [name](https://url) */
    tournDescriptionHelp: string;
    /** Time featured on TV: %s */
    tpTimeSpentOnTV: I18nFormat;
    /** Time spent playing: %s */
    tpTimeSpentPlaying: I18nFormat;
    /** Transparent */
    transparent: string;
    /** Troll */
    troll: string;
    /** Try another move for black */
    tryAnotherMoveForBlack: string;
    /** Try another move for white */
    tryAnotherMoveForWhite: string;
    /** try the contact page */
    tryTheContactPage: string;
    /** Try to win (or at least draw) every game you play. */
    tryToWin: string;
    /** Type private notes here */
    typePrivateNotesHere: string;
    /** UltraBullet */
    ultraBullet: string;
    /** Insanely fast games: less than 30 seconds */
    ultraBulletDesc: string;
    /** Unblock */
    unblock: string;
    /** Undefended pieces */
    undefendedPieces: string;
    /** Unfollow */
    unfollow: string;
    /** Unfollow %s */
    unfollowX: I18nFormat;
    /** Unknown */
    unknown: string;
    /** Win/loss only guaranteed if recommended tablebase line has been followed since the last capture or pawn move, due to possible rounding of DTZ values in Syzygy tablebases. */
    unknownDueToRounding: string;
    /** Unlimited */
    unlimited: string;
    /** Take all the time you need */
    unlimitedDescription: string;
    /** Unsubscribe */
    unsubscribe: string;
    /** Until */
    until: string;
    /** User */
    user: string;
    /** %1$s is better than %2$s of %3$s players. */
    userIsBetterThanPercentOfPerfTypePlayers: I18nFormat;
    /** Username */
    username: string;
    /** This username is already in use, please try another one. */
    usernameAlreadyUsed: string;
    /** You can use this username to create a new account */
    usernameCanBeUsedForNewAccount: string;
    /** The username must only contain letters, numbers, underscores, and hyphens. Consecutive underscores and hyphens are not allowed. */
    usernameCharsInvalid: string;
    /** We couldn't find any user by this name: %s. */
    usernameNotFound: I18nFormat;
    /** Username or email */
    usernameOrEmail: string;
    /** The username must start with a letter. */
    usernamePrefixInvalid: string;
    /** The username must end with a letter or a number. */
    usernameSuffixInvalid: string;
    /** This username is not acceptable. */
    usernameUnacceptable: string;
    /** use the report form */
    useTheReportForm: string;
    /** Using server analysis */
    usingServerAnalysis: string;
    /** Variant */
    variant: string;
    /** Variant ending */
    variantEnding: string;
    /** Variant loss */
    variantLoss: string;
    /** Variants */
    variants: string;
    /** More ways to play */
    variantsDescription: string;
    /** Variant win */
    variantWin: string;
    /** Variation arrows let you navigate without using the move list. */
    variationArrowsInfo: string;
    /** Verify that %s is your email address */
    verifyYourAddress: I18nFormat;
    /** Victory */
    victory: string;
    /** %1$s vs %2$s in %3$s */
    victoryVsYInZ: I18nFormat;
    /** Video library */
    videoLibrary: string;
    /** View in full size */
    viewInFullSize: string;
    /** View rematch */
    viewRematch: string;
    /** Views */
    views: string;
    /** View the solution */
    viewTheSolution: string;
    /** View tournament */
    viewTournament: string;
    /** Visual motifs */
    visualMotifs: string;
    /** We will come back to you shortly to help you complete your signup. */
    waitForSignupHelp: string;
    /** Waiting */
    waiting: string;
    /** Waiting for analysis */
    waitingForAnalysis: string;
    /** Waiting for opponent */
    waitingForOpponent: string;
    /** Watch */
    watch: string;
    /** Watch games */
    watchGames: string;
    /** Webmasters */
    webmasters: string;
    /** Website */
    website: string;
    /** Weekly %s rating distribution */
    weeklyPerfTypeRatingDistribution: I18nFormat;
    /** We had to time you out for a while. */
    weHadToTimeYouOutForAWhile: string;
    /** We've sent you an email. Click the link in the email to activate your account. */
    weHaveSentYouAnEmailClickTheLink: string;
    /** We've sent an email to %s. Click the link in the email to reset your password. */
    weHaveSentYouAnEmailTo: I18nFormat;
    /** What's the matter? */
    whatIsIheMatter: string;
    /** What username did you use to sign up? */
    whatSignupUsername: string;
    /** When you create a Simul, you get to play several players at once. */
    whenCreateSimul: string;
    /** White */
    white: string;
    /** White accepts takeback */
    whiteAcceptsTakeback: string;
    /** White cancels takeback */
    whiteCancelsTakeback: string;
    /** White O-O */
    whiteCastlingKingside: string;
    /** White to checkmate in one move */
    whiteCheckmatesInOneMove: string;
    /** White declines draw */
    whiteDeclinesDraw: string;
    /** White declines takeback */
    whiteDeclinesTakeback: string;
    /** White didn't move */
    whiteDidntMove: string;
    /** White / Draw / Black */
    whiteDrawBlack: string;
    /** White is victorious */
    whiteIsVictorious: string;
    /** White left the game */
    whiteLeftTheGame: string;
    /** White offers draw */
    whiteOffersDraw: string;
    /** White to play */
    whitePlays: string;
    /** White proposes takeback */
    whiteProposesTakeback: string;
    /** White resigned */
    whiteResigned: string;
    /** White time out */
    whiteTimeOut: string;
    /** White wins */
    whiteWins: string;
    /** White wins */
    whiteWinsGame: string;
    /** Why? */
    why: string;
    /** Winner */
    winner: string;
    /** Winning */
    winning: string;
    /** Win or 50 moves by prior mistake */
    winOr50MovesByPriorMistake: string;
    /** Win prevented by 50-move rule */
    winPreventedBy50MoveRule: string;
    /** Win rate */
    winRate: string;
    /** Wins */
    wins: string;
    /** and wish you great games on lichess.org. */
    wishYouGreatGames: string;
    /** Withdraw */
    withdraw: string;
    /** With everybody */
    withEverybody: string;
    /** With friends */
    withFriends: string;
    /** With nobody */
    withNobody: string;
    /** Write a private note about this user */
    writeAPrivateNoteAboutThisUser: string;
    /** %1$s competes in %2$s */
    xCompetesInY: I18nFormat;
    /** %1$s created team %2$s */
    xCreatedTeamY: I18nFormat;
    /** %1$s hosts %2$s */
    xHostsY: I18nFormat;
    /** %1$s invited you to "%2$s". */
    xInvitedYouToY: I18nFormat;
    /** %1$s is a free (%2$s), libre, no-ads, open source chess server. */
    xIsAFreeYLibreOpenSourceChessServer: I18nFormat;
    /** %1$s joined team %2$s */
    xJoinedTeamY: I18nFormat;
    /** %1$s joins %2$s */
    xJoinsY: I18nFormat;
    /** %1$s likes %2$s */
    xLikesY: I18nFormat;
    /** %1$s mentioned you in "%2$s". */
    xMentionedYouInY: I18nFormat;
    /** %s opening explorer */
    xOpeningExplorer: I18nFormat;
    /** %1$s posted in topic %2$s */
    xPostedInForumY: I18nFormat;
    /** %s rating */
    xRating: I18nFormat;
    /** %1$s started following %2$s */
    xStartedFollowingY: I18nFormat;
    /** %s started streaming */
    xStartedStreaming: I18nFormat;
    /** %s was played */
    xWasPlayed: I18nFormat;
    /** Yes */
    yes: string;
    /** Yesterday */
    yesterday: string;
    /** You are better than %1$s of %2$s players. */
    youAreBetterThanPercentOfPerfTypePlayers: I18nFormat;
    /** You are leaving Lichess */
    youAreLeavingLichess: string;
    /** You are not in the team %s */
    youAreNotInTeam: I18nFormat;
    /** You are now part of the team. */
    youAreNowPartOfTeam: string;
    /** You are playing! */
    youArePlaying: string;
    /** You browsed away */
    youBrowsedAway: string;
    /** Scroll over the board to move in the game. */
    youCanAlsoScrollOverTheBoardToMoveInTheGame: string;
    /** You can do better */
    youCanDoBetter: string;
    /** There is a setting to hide all user flairs across the entire site. */
    youCanHideFlair: string;
    /** You can't post in the forums yet. Play some games! */
    youCannotPostYetPlaySomeGames: string;
    /** You can't start a new game until this one is finished. */
    youCantStartNewGame: string;
    /** You do not have an established %s rating. */
    youDoNotHaveAnEstablishedPerfTypeRating: I18nFormat;
    /** You have been timed out. */
    youHaveBeenTimedOut: string;
    /** You have joined "%1$s". */
    youHaveJoinedTeamX: I18nFormat;
    /** You need an account to do that */
    youNeedAnAccountToDoThat: string;
    /** You play the black pieces */
    youPlayTheBlackPieces: string;
    /** You play the white pieces */
    youPlayTheWhitePieces: string;
    /** Your opponent offers a draw */
    yourOpponentOffersADraw: string;
    /** Your opponent proposes a takeback */
    yourOpponentProposesATakeback: string;
    /** Your opponent wants to play a new game with you */
    yourOpponentWantsToPlayANewGameWithYou: string;
    /** Your pending simuls */
    yourPendingSimuls: string;
    /** Your %s rating is provisional */
    yourPerfRatingIsProvisional: I18nFormat;
    /** Your %1$s rating (%2$s) is too high */
    yourPerfRatingIsTooHigh: I18nFormat;
    /** Your %1$s rating (%2$s) is too low */
    yourPerfRatingIsTooLow: I18nFormat;
    /** Your %1$s rating is %2$s. */
    yourPerfTypeRatingIsRating: I18nFormat;
    /** Your question may already have an answer %1$s */
    yourQuestionMayHaveBeenAnswered: I18nFormat;
    /** Your rating */
    yourRating: string;
    /** Your rating is %s */
    yourRatingIsX: I18nFormat;
    /** Your score: %s */
    yourScore: I18nFormat;
    /** Your top weekly %1$s rating (%2$s) is too high */
    yourTopWeeklyPerfRatingIsTooHigh: I18nFormat;
    /** Your turn */
    yourTurn: string;
  };
  storm: {
    /** Accuracy */
    accuracy: string;
    /** All-time */
    allTime: string;
    /** Best run of day */
    bestRunOfDay: string;
    /** Click to reload */
    clickToReload: string;
    /** Combo */
    combo: string;
    /** Create a new game */
    createNewGame: string;
    /** End run (hotkey: Enter) */
    endRun: string;
    /** Failed puzzles */
    failedPuzzles: string;
    /** Get ready! */
    getReady: string;
    /** Highest solved */
    highestSolved: string;
    /** Highscores */
    highscores: string;
    /** Highscore: %s */
    highscoreX: I18nFormat;
    /** Join a public race */
    joinPublicRace: string;
    /** Join rematch */
    joinRematch: string;
    /** Join the race! */
    joinTheRace: string;
    /** Moves */
    moves: string;
    /** Move to start */
    moveToStart: string;
    /** New all-time highscore! */
    newAllTimeHighscore: string;
    /** New daily highscore! */
    newDailyHighscore: string;
    /** New monthly highscore! */
    newMonthlyHighscore: string;
    /** New run (hotkey: Space) */
    newRun: string;
    /** New weekly highscore! */
    newWeeklyHighscore: string;
    /** Next race */
    nextRace: string;
    /** Play again */
    playAgain: string;
    /** Played %1$s runs of %2$s */
    playedNbRunsOfPuzzleStorm: I18nPlural;
    /** Previous highscore was %s */
    previousHighscoreWasX: I18nFormat;
    /** Puzzles played */
    puzzlesPlayed: string;
    /** puzzles solved */
    puzzlesSolved: string;
    /** Race complete! */
    raceComplete: string;
    /** Race your friends */
    raceYourFriends: string;
    /** Runs */
    runs: string;
    /** Score */
    score: string;
    /** skip */
    skip: string;
    /** Skip this move to preserve your combo! Only works once per race. */
    skipExplanation: string;
    /** You can skip one move per race: */
    skipHelp: string;
    /** Skipped puzzle */
    skippedPuzzle: string;
    /** Slow puzzles */
    slowPuzzles: string;
    /** Spectating */
    spectating: string;
    /** Start the race */
    startTheRace: string;
    /** This month */
    thisMonth: string;
    /** This run has expired! */
    thisRunHasExpired: string;
    /** This run was opened in another tab! */
    thisRunWasOpenedInAnotherTab: string;
    /** This week */
    thisWeek: string;
    /** Time */
    time: string;
    /** Time per move */
    timePerMove: string;
    /** View best runs */
    viewBestRuns: string;
    /** Wait for rematch */
    waitForRematch: string;
    /** Waiting for more players to join... */
    waitingForMorePlayers: string;
    /** Waiting to start */
    waitingToStart: string;
    /** %s runs */
    xRuns: I18nPlural;
    /** You play the black pieces in all puzzles */
    youPlayTheBlackPiecesInAllPuzzles: string;
    /** You play the white pieces in all puzzles */
    youPlayTheWhitePiecesInAllPuzzles: string;
    /** Your rank: %s */
    yourRankX: I18nFormat;
  };
  streamer: {
    /** All streamers */
    allStreamers: string;
    /** Your stream is approved. */
    approved: string;
    /** Become a Lichess streamer */
    becomeStreamer: string;
    /** Change/delete your picture */
    changePicture: string;
    /** Choose the YouTube channel you will use on Lichess. */
    chooseYoutubeChannel: string;
    /** Connect */
    connect: string;
    /** Currently streaming: %s */
    currentlyStreaming: I18nFormat;
    /** Disconnect */
    disconnect: string;
    /** Download streamer kit */
    downloadKit: string;
    /** Do you have a Twitch or YouTube channel? */
    doYouHaveStream: string;
    /** Edit streamer page */
    editPage: string;
    /** Headline */
    headline: string;
    /** Here we go! */
    hereWeGo: string;
    /** Keep it short: %s characters max */
    keepItShort: I18nPlural;
    /** Last stream %s */
    lastStream: I18nFormat;
    /** Lichess streamer */
    lichessStreamer: string;
    /** Lichess streamers */
    lichessStreamers: string;
    /** LIVE! */
    live: string;
    /** Long description */
    longDescription: string;
    /** Max size: %s */
    maxSize: I18nFormat;
    /** OFFLINE */
    offline: string;
    /** Your stream is being reviewed by moderators. */
    pendingReview: string;
    /** Get a flaming streamer icon on your Lichess profile. */
    perk1: string;
    /** Get bumped up to the top of the streamers list. */
    perk2: string;
    /** Notify your Lichess followers. */
    perk3: string;
    /** Show your stream in your games, tournaments and studies. */
    perk4: string;
    /** Benefits of streaming with the keyword */
    perks: string;
    /** Please allow up to 72 hours before your streamer badge and listing are approved. */
    pleaseAllow: string;
    /** Please fill in your streamer information, and upload a picture. */
    pleaseFillIn: string;
    /** Include the keyword "lichess.org" in your stream title and use the category "Chess" when you stream on Lichess. */
    rule1: string;
    /** Remove the keyword when you stream non-Lichess stuff. */
    rule2: string;
    /** Lichess will detect your stream automatically and enable the following perks: */
    rule3: string;
    /** Read our %s to ensure fair play for everyone during your stream. */
    rule4: I18nFormat;
    /** Streaming rules */
    rules: string;
    /** The Lichess streamer page targets your audience with the language provided by your streaming platform. Set the correct default language for your chess streams in the app or service you use to broadcast. */
    streamerLanguageSettings: string;
    /** Your streamer name on Lichess */
    streamerName: string;
    /** streaming Fairplay FAQ */
    streamingFairplayFAQ: string;
    /** Submit for review */
    submitForReview: string;
    /** Tell us about your stream in one sentence */
    tellUsAboutTheStream: string;
    /** Upload a picture */
    uploadPicture: string;
    /** Visible on the streamers page */
    visibility: string;
    /** When approved by moderators */
    whenApproved: string;
    /** When you are ready to be listed as a Lichess streamer, %s */
    whenReady: I18nFormat;
    /** %s is streaming */
    xIsStreaming: I18nFormat;
    /** %s streamer picture */
    xStreamerPicture: I18nFormat;
    /** Your streamer page */
    yourPage: string;
  };
  study: {
    /** Add members */
    addMembers: string;
    /** Click the %s button.<br>Then decide who can contribute or not. */
    addMembersText: I18nFormat;
    /** Add a new chapter */
    addNewChapter: string;
    /** Allow cloning */
    allowCloning: string;
    /** All studies */
    allStudies: string;
    /** All SYNC members remain on the same position */
    allSyncMembersRemainOnTheSamePosition: string;
    /** Alphabetical */
    alphabetical: string;
    /** Analysis mode */
    analysisMode: string;
    /** Click the !? button, or a right click on the move list on the right.<br>Annotation glyphs are shared and saved. */
    annotatePositionText: string;
    /** Annotate a position */
    annotatePositionTitle: string;
    /** Annotate with glyphs */
    annotateWithGlyphs: string;
    /** Attack */
    attack: string;
    /** Automatic */
    automatic: string;
    /** Back */
    back: string;
    /** Black defeat, but White can't win */
    blackDefeatWhiteCanNotWin: string;
    /** Black is better */
    blackIsBetter: string;
    /** Black is slightly better */
    blackIsSlightlyBetter: string;
    /** Black is winning */
    blackIsWinning: string;
    /** Blunder */
    blunder: string;
    /** Brilliant move */
    brilliantMove: string;
    /** Chapters are saved forever.<br>Have fun organizing your chess content! */
    chapterConclusionText: string;
    /** Chapter PGN */
    chapterPgn: string;
    /** Chapter %s */
    chapterX: I18nFormat;
    /** Clear all comments, glyphs and drawn shapes in this chapter */
    clearAllCommentsInThisChapter: string;
    /** Clear annotations */
    clearAnnotations: string;
    /** Clear chat */
    clearChat: string;
    /** Clear variations */
    clearVariations: string;
    /** Clone */
    cloneStudy: string;
    /** Click the %s button, or right click on the move list on the right.<br>Comments are shared and saved. */
    commentPositionText: I18nFormat;
    /** Comment on a position */
    commentPositionTitle: string;
    /** Comment on this move */
    commentThisMove: string;
    /** Comment on this position */
    commentThisPosition: string;
    /** You can find your <a href='/study/mine/hot'>previous studies</a> from your profile page.<br>There is also a <a href='//lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way'>blog post about studies</a>.<br>Power users might want to press "?" to see keyboard shortcuts.<br>Have fun! */
    conclusionText: string;
    /** Thanks for your time */
    conclusionTitle: string;
    /** Delete the entire study? There is no going back! Type the name of the study to confirm: %s */
    confirmDeleteStudy: I18nFormat;
    /** Contributor */
    contributor: string;
    /** Contributors */
    contributors: string;
    /** Copy PGN */
    copyChapterPgn: string;
    /** Copy Raw PGN */
    copyRawChapterPgn: string;
    /** Counterplay */
    counterplay: string;
    /** Create chapter */
    createChapter: string;
    /** A study can have several chapters.<br>Each chapter has a distinct move tree,<br>and can be created in various ways. */
    createChapterText: string;
    /** Let's create a study chapter */
    createChapterTitle: string;
    /** Create study */
    createStudy: string;
    /** Current chapter URL */
    currentChapterUrl: string;
    /** Setup the board your way.<br>Suited to explore endgames. */
    customPositionText: string;
    /** Custom position */
    customPositionTitle: string;
    /** Date added (newest) */
    dateAddedNewest: string;
    /** Date added (oldest) */
    dateAddedOldest: string;
    /** Delete chapter */
    deleteChapter: string;
    /** Delete study */
    deleteStudy: string;
    /** Delete the study chat history? There is no going back! */
    deleteTheStudyChatHistory: string;
    /** Delete this chapter. There is no going back! */
    deleteThisChapter: string;
    /** Development */
    development: string;
    /** Double defeat */
    doubleDefeat: string;
    /** Download game */
    downloadGame: string;
    /** Dubious move */
    dubiousMove: string;
    /** Edit chapter */
    editChapter: string;
    /** Editor */
    editor: string;
    /** Edit study */
    editStudy: string;
    /** Embed in your website */
    embedInYourWebsite: string;
    /** Empty */
    empty: string;
    /** Enable sync */
    enableSync: string;
    /** Equal position */
    equalPosition: string;
    /** Everyone */
    everyone: string;
    /** Paste a position in FEN format<br><i>4k3/4rb2/8/7p/8/5Q2/1PP5/1K6 w</i><br>to start the chapter from a position. */
    fromFenStringText: string;
    /** From a FEN string */
    fromFenStringTitle: string;
    /** Just a board setup for a new game.<br>Suited to explore openings. */
    fromInitialPositionText: string;
    /** From initial position */
    fromInitialPositionTitle: string;
    /** Paste a game in PGN format.<br>to load moves, comments and variations in the chapter. */
    fromPgnGameText: string;
    /** From a PGN game */
    fromPgnGameTitle: string;
    /** Get a full server-side computer analysis of the mainline. */
    getAFullComputerAnalysis: string;
    /** Need help? Get the tour! */
    getTheTour: string;
    /** Good move */
    goodMove: string;
    /** Hide next moves */
    hideNextMoves: string;
    /** Hot */
    hot: string;
    /** Import from %s */
    importFromChapterX: I18nFormat;
    /** Initiative */
    initiative: string;
    /** Interactive lesson */
    interactiveLesson: string;
    /** Interesting move */
    interestingMove: string;
    /** Invite only */
    inviteOnly: string;
    /** Invite to the study */
    inviteToTheStudy: string;
    /** Kick */
    kick: string;
    /** Leave the study */
    leaveTheStudy: string;
    /** Like */
    like: string;
    /** Load games by URLs */
    loadAGameByUrl: string;
    /** Load games from PGN */
    loadAGameFromPgn: string;
    /** Load a position from FEN */
    loadAPositionFromFen: string;
    /** Paste a lichess game URL<br>(like lichess.org/7fHIU0XI)<br>to load the game moves in the chapter. */
    loadExistingLichessGameText: string;
    /** Load an existing lichess game */
    loadExistingLichessGameTitle: string;
    /** Make sure the chapter is complete. You can only request analysis once. */
    makeSureTheChapterIsComplete: string;
    /** Manage topics */
    manageTopics: string;
    /** Members */
    members: string;
    /** Mistake */
    mistake: string;
    /** Most popular */
    mostPopular: string;
    /** My favourite studies */
    myFavoriteStudies: string;
    /** My private studies */
    myPrivateStudies: string;
    /** My public studies */
    myPublicStudies: string;
    /** My studies */
    myStudies: string;
    /** My topics */
    myTopics: string;
    /** %s Chapters */
    nbChapters: I18nPlural;
    /** %s Games */
    nbGames: I18nPlural;
    /** %s Members */
    nbMembers: I18nPlural;
    /** New chapter */
    newChapter: string;
    /** New tag */
    newTag: string;
    /** Next */
    next: string;
    /** Next chapter */
    nextChapter: string;
    /** Nobody */
    nobody: string;
    /** No: let people browse freely */
    noLetPeopleBrowseFreely: string;
    /** None yet. */
    noneYet: string;
    /** None */
    noPinnedComment: string;
    /** Normal analysis */
    normalAnalysis: string;
    /** Novelty */
    novelty: string;
    /** Only the study contributors can request a computer analysis. */
    onlyContributorsCanRequestAnalysis: string;
    /** Only me */
    onlyMe: string;
    /** Only move */
    onlyMove: string;
    /** Only public studies can be embedded! */
    onlyPublicStudiesCanBeEmbedded: string;
    /** Open */
    open: string;
    /** Orientation */
    orientation: string;
    /** Paste games as PGN text here. For each game, a new chapter is created. The study can have up to %s chapters. */
    pasteYourPgnTextHereUpToNbGames: I18nPlural;
    /** %s per page */
    perPage: I18nFormat;
    /** PGN tags */
    pgnTags: string;
    /** Pinned chapter comment */
    pinnedChapterComment: string;
    /** Pinned study comment */
    pinnedStudyComment: string;
    /** Play again */
    playAgain: string;
    /** Playing */
    playing: string;
    /** Please only invite people who know you, and who actively want to join this study. */
    pleaseOnlyInvitePeopleYouKnow: string;
    /** Popular topics */
    popularTopics: string;
    /** Previous chapter */
    prevChapter: string;
    /** Private */
    private: string;
    /** Public */
    public: string;
    /** Read more about embedding */
    readMoreAboutEmbedding: string;
    /** Recently updated */
    recentlyUpdated: string;
    /** Relevant */
    relevant: string;
    /** Right under the board */
    rightUnderTheBoard: string;
    /** Save */
    save: string;
    /** Save chapter */
    saveChapter: string;
    /** Search by username */
    searchByUsername: string;
    /** Share & export */
    shareAndExport: string;
    /** Share changes with spectators and save them on the server */
    shareChanges: string;
    /** Other members can see your moves in real time!<br>Plus, everything is saved forever. */
    sharedAndSavedText: string;
    /** Shared and saved */
    sharedAndSaveTitle: string;
    /** Evaluation bars */
    showEvalBar: string;
    /** Results */
    showResults: string;
    /** Spectator */
    spectator: string;
    /** Start */
    start: string;
    /** Start at initial position */
    startAtInitialPosition: string;
    /** Start at %s */
    startAtX: I18nFormat;
    /** Start from custom position */
    startFromCustomPosition: string;
    /** Start from initial position */
    startFromInitialPosition: string;
    /** Studies created by %s */
    studiesCreatedByX: I18nFormat;
    /** Studies I contribute to */
    studiesIContributeTo: string;
    /** Study actions */
    studyActions: string;
    /** A study can contain several chapters.<br>Each chapter has a distinct initial position and move tree. */
    studyChaptersText: string;
    /** Study chapters */
    studyChaptersTitle: string;
    /** %1$s Spectators can view the study and talk in the chat.<br><br>%2$s Contributors can make moves and update the study. */
    studyMembersText: I18nFormat;
    /** Study members */
    studyMembersTitle: string;
    /** Study not found */
    studyNotFound: string;
    /** Study PGN */
    studyPgn: string;
    /** Study URL */
    studyUrl: string;
    /** The chapter is too short to be analysed. */
    theChapterIsTooShortToBeAnalysed: string;
    /** Time trouble */
    timeTrouble: string;
    /** Topics */
    topics: string;
    /** Unclear position */
    unclearPosition: string;
    /** Unlike */
    unlike: string;
    /** Unlisted */
    unlisted: string;
    /** URL of the games, one per line */
    urlOfTheGame: string;
    /** Yes, you can study crazyhouse<br>and all lichess variants! */
    variantsAreSupportedText: string;
    /** Studies support variants */
    variantsAreSupportedTitle: string;
    /** Visibility */
    visibility: string;
    /** This is a shared analysis board.<br><br>Use it to analyse and annotate games,<br>discuss positions with friends,<br>and of course for chess lessons!<br><br>It's a powerful tool, let's take some time to see how it works. */
    welcomeToLichessStudyText: string;
    /** Welcome to Lichess Study! */
    welcomeToLichessStudyTitle: string;
    /** What are studies? */
    whatAreStudies: string;
    /** What would you play in this position? */
    whatWouldYouPlay: string;
    /** Where do you want to study that? */
    whereDoYouWantToStudyThat: string;
    /** White defeat, but Black can't win */
    whiteDefeatBlackCanNotWin: string;
    /** White is better */
    whiteIsBetter: string;
    /** White is slightly better */
    whiteIsSlightlyBetter: string;
    /** White is winning */
    whiteIsWinning: string;
    /** With compensation */
    withCompensation: string;
    /** With the idea */
    withTheIdea: string;
    /** %1$s, brought to you by %2$s */
    xBroughtToYouByY: I18nFormat;
    /** Yes: keep everyone on the same position */
    yesKeepEveryoneOnTheSamePosition: string;
    /** You are now a contributor */
    youAreNowAContributor: string;
    /** You are now a spectator */
    youAreNowASpectator: string;
    /** You can paste this in the forum or your Lichess blog to embed */
    youCanPasteThisInTheForumToEmbed: string;
    /** Congratulations! You completed this lesson. */
    youCompletedThisLesson: string;
    /** Zugzwang */
    zugzwang: string;
  };
  swiss: {
    /** Absences */
    absences: string;
    /** Byes */
    byes: string;
    /** Comparison */
    comparison: string;
    /** Predefined max rounds, but duration unknown */
    durationUnknown: string;
    /** Dutch system */
    dutchSystem: string;
    /** In Swiss games, players cannot draw before 30 moves are played. While this measure cannot prevent pre-arranged draws, it at least makes it harder to agree to a draw on the fly. */
    earlyDrawsAnswer: string;
    /** What happens with early draws? */
    earlyDrawsQ: string;
    /** FIDE handbook */
    FIDEHandbook: string;
    /** If this list is non-empty, then users absent from this list will be forbidden to join. One username per line. */
    forbiddedUsers: string;
    /** Forbidden pairings */
    forbiddenPairings: string;
    /** Usernames of players that must not play together (Siblings, for instance). Two usernames per line, separated by a space. */
    forbiddenPairingsHelp: string;
    /** Forbidden */
    identicalForbidden: string;
    /** Identical pairing */
    identicalPairing: string;
    /** Join or create a team */
    joinOrCreateTeam: string;
    /** Late join */
    lateJoin: string;
    /** Yes, until more than half the rounds have started; for example in a 11-rounds Swiss, players can join before round 6 starts and in a 12-rounds before round 7 starts. */
    lateJoinA: string;
    /** Can players late-join? */
    lateJoinQ: string;
    /** Yes until more than half the rounds have started */
    lateJoinUntil: string;
    /** Manual pairings in next round */
    manualPairings: string;
    /** Specify all pairings of the next round manually. One player pair per line. Example: */
    manualPairingsHelp: string;
    /** When all possible pairings have been played, the tournament will be ended and a winner declared. */
    moreRoundsThanPlayersA: string;
    /** What happens if the tournament has more rounds than players? */
    moreRoundsThanPlayersQ: string;
    /** Must have played their last swiss game */
    mustHavePlayedTheirLastSwissGame: string;
    /** Only let players join if they have played their last swiss game. If they failed to show up in a recent swiss event, they won't be able to enter yours. This results in a better swiss experience for the players who actually show up. */
    mustHavePlayedTheirLastSwissGameHelp: string;
    /** %s rounds */
    nbRounds: I18nPlural;
    /** New Swiss tournament */
    newSwiss: string;
    /** Next round */
    nextRound: string;
    /** Now playing */
    nowPlaying: string;
    /** A player gets a bye of one point every time the pairing system can't find a pairing for them. */
    numberOfByesA: string;
    /** How many byes can a player get? */
    numberOfByesQ: string;
    /** Number of games */
    numberOfGames: string;
    /** As many as can be played in the allotted duration */
    numberOfGamesAsManyAsPossible: string;
    /** Decided in advance, same for all players */
    numberOfGamesPreDefined: string;
    /** Number of rounds */
    numberOfRounds: string;
    /** An odd number of rounds allows optimal colour balance. */
    numberOfRoundsHelp: string;
    /** One round every %s days */
    oneRoundEveryXDays: I18nPlural;
    /** Ongoing games */
    ongoingGames: I18nPlural;
    /** We don't plan to add more tournament systems to Lichess at the moment. */
    otherSystemsA: string;
    /** What about other tournament systems? */
    otherSystemsQ: string;
    /** With the %1$s, implemented by %2$s, in accordance with the %3$s. */
    pairingsA: I18nFormat;
    /** How are pairings decided? */
    pairingsQ: string;
    /** Pairing system */
    pairingSystem: string;
    /** Any available opponent with similar ranking */
    pairingSystemArena: string;
    /** Best pairing based on points and tie breaks */
    pairingSystemSwiss: string;
    /** Pairing wait time */
    pairingWaitTime: string;
    /** Fast: doesn't wait for all players */
    pairingWaitTimeArena: string;
    /** Slow: waits for all players */
    pairingWaitTimeSwiss: string;
    /** Pause */
    pause: string;
    /** Yes but might reduce the number of rounds */
    pauseSwiss: string;
    /** Play your games */
    playYourGames: string;
    /** A win is worth one point, a draw is a half point, and a loss is zero points. */
    pointsCalculationA: string;
    /** How are points calculated? */
    pointsCalculationQ: string;
    /** Possible, but not consecutive */
    possibleButNotConsecutive: string;
    /** Predefined duration in minutes */
    predefinedDuration: string;
    /** Only allow pre-defined users to join */
    predefinedUsers: string;
    /** Players who sign up for Swiss events but don't play their games can be problematic. */
    protectionAgainstNoShowA: string;
    /** What is done regarding no-shows? */
    protectionAgainstNoShowQ: string;
    /** Swiss tournaments were not designed for online chess. They demand punctuality, dedication and patience from players. */
    restrictedToTeamsA: string;
    /** Why is it restricted to teams? */
    restrictedToTeamsQ: string;
    /** Interval between rounds */
    roundInterval: string;
    /** We'd like to add it, but unfortunately Round Robin doesn't work online. */
    roundRobinA: string;
    /** What about Round Robin? */
    roundRobinQ: string;
    /** Rounds are started manually */
    roundsAreStartedManually: string;
    /** Similar to OTB tournaments */
    similarToOTB: string;
    /** Sonneborn–Berger score */
    sonnebornBergerScore: string;
    /** Starting soon */
    startingSoon: string;
    /** Streaks and Berserk */
    streaksAndBerserk: string;
    /** Swiss */
    swiss: string;
    /** In a Swiss tournament %1$s, each competitor does not necessarily play all other entrants. Competitors meet one-on-one in each round and are paired using a set of rules designed to ensure that each competitor plays opponents with a similar running score, but not the same opponent more than once. The winner is the competitor with the highest aggregate points earned in all rounds. All competitors play in each round unless there is an odd number of players. */
    swissDescription: I18nFormat;
    /** Swiss tournaments */
    swissTournaments: string;
    /** In a Swiss tournament, all participants play the same number of games, and can only play each other once. */
    swissVsArenaA: string;
    /** When to use Swiss tournaments instead of arenas? */
    swissVsArenaQ: string;
    /** Swiss tournaments can only be created by team leaders, and can only be played by team members. */
    teamOnly: I18nFormat;
    /** Tie Break */
    tieBreak: string;
    /** With the %s. */
    tiebreaksCalculationA: I18nFormat;
    /** How are tie breaks calculated? */
    tiebreaksCalculationQ: string;
    /** Duration of the tournament */
    tournDuration: string;
    /** Tournament start date */
    tournStartDate: string;
    /** Unlimited and free */
    unlimitedAndFree: string;
    /** View all %s rounds */
    viewAllXRounds: I18nPlural;
    /** Their clock will tick, they will flag, and lose the game. */
    whatIfOneDoesntPlayA: string;
    /** What happens if a player doesn't play a game? */
    whatIfOneDoesntPlayQ: string;
    /** No. They're complementary features. */
    willSwissReplaceArenasA: string;
    /** Will Swiss replace arena tournaments? */
    willSwissReplaceArenasQ: string;
    /** %s minutes between rounds */
    xMinutesBetweenRounds: I18nPlural;
    /** %s rounds Swiss */
    xRoundsSwiss: I18nPlural;
    /** %s seconds between rounds */
    xSecondsBetweenRounds: I18nPlural;
  };
  team: {
    /** All teams */
    allTeams: string;
    /** Battle of %s teams */
    battleOfNbTeams: I18nPlural;
    /** Your join request is being reviewed by a team leader. */
    beingReviewed: string;
    /** Close team */
    closeTeam: string;
    /** Closes the team forever. */
    closeTeamDescription: string;
    /** Completed tournaments */
    completedTourns: string;
    /** Declined Requests */
    declinedRequests: string;
    /** Team entry code */
    entryCode: string;
    /** (Optional) An entry code that new members must know to join this team. */
    entryCodeDescriptionForLeader: string;
    /** Incorrect entry code. */
    incorrectEntryCode: string;
    /** Inner team */
    innerTeam: string;
    /** Introduction */
    introduction: string;
    /** Join the official %s team for news and events */
    joinLichessVariantTeam: I18nFormat;
    /** Join team */
    joinTeam: string;
    /** Kick someone out of the team */
    kickSomeone: string;
    /** Leaders chat */
    leadersChat: string;
    /** Leader teams */
    leaderTeams: string;
    /** List the teams that will compete in this battle. */
    listTheTeamsThatWillCompete: string;
    /** Manually review admission requests */
    manuallyReviewAdmissionRequests: string;
    /** If checked, players will need to write a request to join the team, which you can decline or accept. */
    manuallyReviewAdmissionRequestsHelp: string;
    /** Message all members */
    messageAllMembers: string;
    /** Send a private message to ALL members of the team. */
    messageAllMembersLongDescription: string;
    /** Send a private message to every member of the team */
    messageAllMembersOverview: string;
    /** My teams */
    myTeams: string;
    /** %s leaders per team */
    nbLeadersPerTeam: I18nPlural;
    /** %s members */
    nbMembers: I18nPlural;
    /** New team */
    newTeam: string;
    /** No team found */
    noTeamFound: string;
    /** Number of leaders per team. The sum of their score is the score of the team. */
    numberOfLeadsPerTeam: string;
    /** You really shouldn't change this value after the tournament has started! */
    numberOfLeadsPerTeamHelp: string;
    /** One team per line. Use the auto-completion. */
    oneTeamPerLine: string;
    /** You can copy-paste this list from a tournament to another! */
    oneTeamPerLineHelp: string;
    /** Please add a new team leader before leaving, or close the team. */
    onlyLeaderLeavesTeam: string;
    /** Leave team */
    quitTeam: string;
    /** Your join request was declined by a team leader. */
    requestDeclined: string;
    /** Subscribe to team messages */
    subToTeamMessages: string;
    /** A Swiss tournament that only members of your team can join */
    swissTournamentOverview: string;
    /** Team */
    team: string;
    /** This team already exists. */
    teamAlreadyExists: string;
    /** Team Battle */
    teamBattle: string;
    /** A battle of multiple teams, each player scores points for their team */
    teamBattleOverview: string;
    /** Full description visible on the team page. */
    teamDescriptionHelp: string;
    /** Brief description visible in team listings. Up to 200 chars. */
    teamIntroductionHelp: string;
    /** Team leaders */
    teamLeaders: I18nPlural;
    /** Recent members */
    teamRecentMembers: string;
    /** Teams */
    teams: string;
    /** Teams I lead */
    teamsIlead: string;
    /** Team tournament */
    teamTournament: string;
    /** An Arena tournament that only members of your team can join */
    teamTournamentOverview: string;
    /** This tournament is over, and the teams can no longer be updated. */
    thisTeamBattleIsOver: string;
    /** Upcoming tournaments */
    upcomingTournaments: string;
    /** Who do you want to kick out of the team? */
    whoToKick: string;
    /** Your join request will be reviewed by a team leader. */
    willBeReviewed: string;
    /** %s join requests */
    xJoinRequests: I18nPlural;
    /** You may want to link one of these upcoming tournaments? */
    youWayWantToLinkOneOfTheseTournaments: string;
  };
  tfa: {
    /** Authentication code */
    authenticationCode: string;
    /** Disable two-factor authentication */
    disableTwoFactor: string;
    /** Enable two-factor authentication */
    enableTwoFactor: string;
    /** Enter your password and the authentication code generated by the app to complete the setup. You will need an authentication code every time you log in. */
    enterPassword: string;
    /** If you cannot scan the code, enter the secret key %s into your app. */
    ifYouCannotScanEnterX: I18nFormat;
    /** Note: If you lose access to your two-factor authentication codes, you can do a %s via email. */
    ifYouLoseAccessTwoFactor: I18nFormat;
    /** Open the two-factor authentication app on your device to view your authentication code and verify your identity. */
    openTwoFactorApp: string;
    /** Scan the QR code with the app. */
    scanTheCode: string;
    /** Please enable two-factor authentication to secure your account at https://lichess.org/account/twofactor. */
    setupReminder: string;
    /** Get an app for two-factor authentication. We recommend the following apps: */
    twoFactorAppRecommend: string;
    /** Two-factor authentication */
    twoFactorAuth: string;
    /** Two-factor authentication enabled */
    twoFactorEnabled: string;
    /** Two-factor authentication adds another layer of security to your account. */
    twoFactorHelp: string;
    /** You need your password and an authentication code from your authenticator app to disable two-factor authentication. */
    twoFactorToDisable: string;
  };
  timeago: {
    /** completed */
    completed: string;
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
    justNow: string;
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
    rightNow: string;
  };
  tourname: {
    /** Classical Shield */
    classicalShield: string;
    /** Classical Shield Arena */
    classicalShieldArena: string;
    /** Daily Classical */
    dailyClassical: string;
    /** Daily Classical Arena */
    dailyClassicalArena: string;
    /** Daily Rapid */
    dailyRapid: string;
    /** Daily Rapid Arena */
    dailyRapidArena: string;
    /** Daily %s */
    dailyX: I18nFormat;
    /** Daily %s Arena */
    dailyXArena: I18nFormat;
    /** Eastern Classical */
    easternClassical: string;
    /** Eastern Classical Arena */
    easternClassicalArena: string;
    /** Eastern Rapid */
    easternRapid: string;
    /** Eastern Rapid Arena */
    easternRapidArena: string;
    /** Eastern %s */
    easternX: I18nFormat;
    /** Eastern %s Arena */
    easternXArena: I18nFormat;
    /** Elite %s */
    eliteX: I18nFormat;
    /** Elite %s Arena */
    eliteXArena: I18nFormat;
    /** Hourly Rapid */
    hourlyRapid: string;
    /** Hourly Rapid Arena */
    hourlyRapidArena: string;
    /** Hourly %s */
    hourlyX: I18nFormat;
    /** Hourly %s Arena */
    hourlyXArena: I18nFormat;
    /** Monthly Classical */
    monthlyClassical: string;
    /** Monthly Classical Arena */
    monthlyClassicalArena: string;
    /** Monthly Rapid */
    monthlyRapid: string;
    /** Monthly Rapid Arena */
    monthlyRapidArena: string;
    /** Monthly %s */
    monthlyX: I18nFormat;
    /** Monthly %s Arena */
    monthlyXArena: I18nFormat;
    /** Rapid Shield */
    rapidShield: string;
    /** Rapid Shield Arena */
    rapidShieldArena: string;
    /** Weekly Classical */
    weeklyClassical: string;
    /** Weekly Classical Arena */
    weeklyClassicalArena: string;
    /** Weekly Rapid */
    weeklyRapid: string;
    /** Weekly Rapid Arena */
    weeklyRapidArena: string;
    /** Weekly %s */
    weeklyX: I18nFormat;
    /** Weekly %s Arena */
    weeklyXArena: I18nFormat;
    /** %s Arena */
    xArena: I18nFormat;
    /** %s Shield */
    xShield: I18nFormat;
    /** %s Shield Arena */
    xShieldArena: I18nFormat;
    /** %s Team Battle */
    xTeamBattle: I18nFormat;
    /** Yearly Classical */
    yearlyClassical: string;
    /** Yearly Classical Arena */
    yearlyClassicalArena: string;
    /** Yearly Rapid */
    yearlyRapid: string;
    /** Yearly Rapid Arena */
    yearlyRapidArena: string;
    /** Yearly %s */
    yearlyX: I18nFormat;
    /** Yearly %s Arena */
    yearlyXArena: I18nFormat;
  };
  ublog: {
    /** %s blog posts */
    blogPosts: I18nPlural;
    /** Our simple tips to write great blog posts */
    blogTips: string;
    /** By Lichess */
    byLichess: string;
    /** By month */
    byMonth: string;
    /** By topic */
    byTopic: string;
    /** Community */
    community: string;
    /** Continue reading this post */
    continueReadingPost: string;
    /** Enable comments */
    createBlogDiscussion: string;
    /** A forum topic will be created for people to comment on your post */
    createBlogDiscussionHelp: string;
    /** Delete this blog post definitively */
    deleteBlog: string;
    /** Discuss this blog post in the forum */
    discussThisBlogPostInTheForum: string;
    /** Drafts */
    drafts: string;
    /** Edit your blog post */
    editYourBlogPost: string;
    /** Image alternative text */
    imageAlt: string;
    /** Image credit */
    imageCredit: string;
    /** Anything inappropriate could get your account closed. */
    inappropriateContentAccountClosed: string;
    /** Latest blog posts */
    latestBlogPosts: string;
    /** Lichess blog posts in %s */
    lichessBlogPostsFromXYear: I18nFormat;
    /** Liked blog posts */
    likedBlogs: string;
    /** My blog */
    myBlog: string;
    /** My friends */
    myFriends: string;
    /** My likes */
    myLikes: string;
    /** %s views */
    nbViews: I18nPlural;
    /** New post */
    newPost: string;
    /** No drafts to show. */
    noDrafts: string;
    /** No posts in this blog, yet. */
    noPostsInThisBlogYet: string;
    /** Post body */
    postBody: string;
    /** Post intro */
    postIntro: string;
    /** Post title */
    postTitle: string;
    /** Previous blog posts */
    previousBlogPosts: string;
    /** Published */
    published: string;
    /** Published %s blog posts */
    publishedNbBlogPosts: I18nPlural;
    /** If checked, the post will be listed on your blog. If not, it will be private, in your draft posts */
    publishHelp: string;
    /** Publish on your blog */
    publishOnYourBlog: string;
    /** Please only post safe and respectful content. Do not copy someone else's content. */
    safeAndRespectfulContent: string;
    /** It is safe to use images from the following websites: */
    safeToUseImages: string;
    /** Save draft */
    saveDraft: string;
    /** Select the topics your post is about */
    selectPostTopics: string;
    /** Sticky post */
    stickyPost: string;
    /** If checked, this post will be listed first in your profile recent posts and on your blog. */
    stickyPostHelp: string;
    /** This is a draft */
    thisIsADraft: string;
    /** This post is published */
    thisPostIsPublished: string;
    /** Upload an image for your post */
    uploadAnImageForYourPost: string;
    /** You can also use images that you made yourself, pictures you took, screenshots of Lichess... anything that is not copyrighted by someone else. */
    useImagesYouMadeYourself: string;
    /** View all %s posts */
    viewAllNbPosts: I18nPlural;
    /** %s's Blog */
    xBlog: I18nFormat;
    /** %1$s published %2$s */
    xPublishedY: I18nFormat;
    /** You are blocked by the blog author. */
    youBlockedByBlogAuthor: string;
  };
  variant: {
    /** Antichess */
    antichess: string;
    /** Lose all your pieces (or get stalemated) to win the game. */
    antichessTitle: string;
    /** Atomic */
    atomic: string;
    /** Nuke your opponent's king to win. */
    atomicTitle: string;
    /** Chess960 */
    chess960: string;
    /** The starting position of the home rank pieces is randomised. */
    chess960Title: string;
    /** Crazyhouse */
    crazyhouse: string;
    /** Captured pieces can be dropped back on the board instead of moving a piece. */
    crazyhouseTitle: string;
    /** From Position */
    fromPosition: string;
    /** Standard chess from a custom position */
    fromPositionTitle: string;
    /** Horde */
    horde: string;
    /** One side has a large number of pawns, the other has a normal army. */
    hordeTitle: string;
    /** King of the Hill */
    kingOfTheHill: string;
    /** Bring your King to the center to win the game. */
    kingOfTheHillTitle: string;
    /** Racing Kings */
    racingKings: string;
    /** Get your king to the other side of the board to win. */
    racingKingsTitle: string;
    /** Standard */
    standard: string;
    /** Standard rules of chess (FIDE) */
    standardTitle: string;
    /** Three-Check */
    threeCheck: string;
    /** Check your opponent 3 times to win the game. */
    threeCheckTitle: string;
  };
  video: {
    /** All %s video tags */
    allNbVideoTags: I18nFormat;
    /** All videos are free for everyone. */
    allVideosAreFree: string;
    /** Chess videos */
    chessVideos: string;
    /** Free chess videos */
    freeChessVideos: string;
    /** free for all */
    freeForAll: string;
    /** %s videos found */
    nbVideosFound: I18nPlural;
    /** No videos for these tags: */
    noVideosForTheseTags: string;
    /** Select tags to filter the videos. */
    selectTagsToFilter: string;
    /** That's all we got for these tags: */
    thatsAllWeGotForTheseTags: string;
    /** That's all we got for this search: "%s" */
    thatsAllWeGotForThisSearchX: I18nFormat;
    /** There are no results for "%s" */
    thereAreNoResultsForX: I18nFormat;
    /** Video not found! */
    videoNotFound: string;
    /** View more tags */
    viewMoreTags: string;
    /** We have carefully selected %s videos so far! */
    weHaveCarefullySelectedX: I18nFormat;
    /** %1$s by %2$s */
    xByY: I18nFormat;
    /** %s curated chess videos */
    xCuratedChessVideos: I18nFormat;
    /** %s free, carefully curated chess videos */
    xFreeCarefullyCurated: I18nFormat;
    /** %1$s with tags %2$s */
    xWithTagsY: I18nFormat;
  };
  voiceCommands: {
    /** Cancel timer or deny a request */
    cancelTimerOrDenyARequest: string;
    /** Castle (either side) */
    castle: string;
    /** Use the %1$s button to toggle voice recognition, the %2$s button to open this help dialog, and the %3$s menu to change speech settings. */
    instructions1: I18nFormat;
    /** We show arrows for multiple moves when we are not sure. Speak the colour or number of a move arrow to select it. */
    instructions2: string;
    /** If an arrow shows a sweeping radar, that move will be played when the circle is complete. During this time, you may only say %1$s to play the move immediately, %2$s to cancel, or speak the colour/number of a different arrow. This timer can be adjusted or turned off in settings. */
    instructions3: I18nFormat;
    /** Enable %s in noisy surroundings. Hold shift while speaking commands when this is on. */
    instructions4: I18nFormat;
    /** Use the phonetic alphabet to improve recognition of chessboard files. */
    instructions5: string;
    /** %s explains the voice move settings in detail. */
    instructions6: I18nFormat;
    /** Move to e4 or select e4 piece */
    moveToE4OrSelectE4Piece: string;
    /** Phonetic alphabet is best */
    phoneticAlphabetIsBest: string;
    /** Play preferred move or confirm something */
    playPreferredMoveOrConfirmSomething: string;
    /** Select or capture a bishop */
    selectOrCaptureABishop: string;
    /** Show puzzle solution */
    showPuzzleSolution: string;
    /** Sleep (if wake word enabled) */
    sleep: string;
    /** Take rook with queen */
    takeRookWithQueen: string;
    /** This blog post */
    thisBlogPost: string;
    /** Turn off voice recognition */
    turnOffVoiceRecognition: string;
    /** Voice commands */
    voiceCommands: string;
    /** Watch the video tutorial */
    watchTheVideoTutorial: string;
  };
}
