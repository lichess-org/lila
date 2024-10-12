// Generated
interface I18nFormat {
  (...args: (string | number)[]): string;
  asArray: <T>(...args: T[]) => (T | string)[]; // vdom
}
interface I18nPlural {
  (quantity: number, ...args: (string | number)[]): string; // pluralSame
  raw: (quantity: number, ...args: (string | number)[]) => string; // plural
  asArray: <T>(quantity: number, ...args: T[]) => (T | string)[]; // vdomPlural
}
interface I18n {
  activity: {
    activity: string; // Activity
    competedInNbSwissTournaments: I18nPlural; // Competed in %s Swiss tournaments
    competedInNbTournaments: I18nPlural; // Competed in %s Arena tournaments
    completedNbGames: I18nPlural; // Completed %s correspondence games
    completedNbVariantGames: I18nPlural; // Completed %1$s %2$s correspondence games
    createdNbStudies: I18nPlural; // Created %s new studies
    followedNbPlayers: I18nPlural; // Started following %s players
    gainedNbFollowers: I18nPlural; // Gained %s new followers
    hostedALiveStream: string; // Hosted a live stream
    hostedNbSimuls: I18nPlural; // Hosted %s simultaneous exhibitions
    inNbCorrespondenceGames: I18nPlural; // in %1$s correspondence games
    joinedNbSimuls: I18nPlural; // Participated in %s simultaneous exhibitions
    joinedNbTeams: I18nPlural; // Joined %s teams
    playedNbGames: I18nPlural; // Played %1$s %2$s games
    playedNbMoves: I18nPlural; // Played %1$s moves
    postedNbMessages: I18nPlural; // Posted %1$s messages in %2$s
    practicedNbPositions: I18nPlural; // Practised %1$s positions on %2$s
    rankedInSwissTournament: I18nFormat; // Ranked #%1$s in %2$s
    rankedInTournament: I18nPlural; // Ranked #%1$s (top %2$s%%) with %3$s games in %4$s
    signedUp: string; // Signed up to lichess.org
    solvedNbPuzzles: I18nPlural; // Solved %s training puzzles
    supportedNbMonths: I18nPlural; // Supported lichess.org for %1$s months as a %2$s
  };
  appeal: {
    accountMuted: string; // Your account is muted.
    accountMutedInfo: I18nFormat; // Read our %s. Failure to follow the communication guidelines can result in accounts being muted.
    arenaBanned: string; // Your account is banned from joining arenas.
    blogRules: string; // blog rules
    boosterMarked: string; // Your account is marked for rating manipulation.
    boosterMarkedInfo: string; // We define this as deliberately manipulating rating by losing games on purpose or by playing against another account that is deliberately losing games.
    cleanAllGood: string; // Your account is not marked or restricted. You're all good!
    closedByModerators: string; // Your account was closed by moderators.
    communicationGuidelines: string; // communication guidelines
    engineMarked: string; // Your account is marked for external assistance in games.
    engineMarkedInfo: I18nFormat; // We define this as using any external help to reinforce your knowledge and/or calculation skills in order to gain an unfair advantage over your opponent. See the %s page for more details.
    excludedFromLeaderboards: string; // Your account has been excluded from leaderboards.
    excludedFromLeaderboardsInfo: string; // We define this as using any unfair way to get on the leaderboard.
    fairPlay: string; // Fair Play
    hiddenBlog: string; // Your blogs have been hidden by moderators.
    hiddenBlogInfo: I18nFormat; // Make sure to read again our %s.
    playTimeout: string; // You have a play timeout.
    prizeBanned: string; // Your account is banned from tournaments with real prizes.
  };
  arena: {
    allAveragesAreX: I18nFormat; // All averages on this page are %s.
    allowBerserk: string; // Allow Berserk
    allowBerserkHelp: string; // Let players halve their clock time to gain an extra point
    allowChatHelp: string; // Let players discuss in a chat room
    arena: string; // Arena
    arenaStreaks: string; // Arena streaks
    arenaStreaksHelp: string; // After 2 wins, consecutive wins grant 4 points instead of 2.
    arenaTournaments: string; // Arena tournaments
    averagePerformance: string; // Average performance
    averageScore: string; // Average score
    berserk: string; // Arena Berserk
    berserkAnswer: string; // When a player clicks the Berserk button at the beginning of the game, they lose half of their clock time, but the win is worth one extra tournament point.
    bestResults: string; // Best results
    created: string; // Created
    customStartDate: string; // Custom start date
    customStartDateHelp: string; // In your own local timezone. This overrides the "Time before tournament starts" setting
    defender: string; // Defender
    drawingWithinNbMoves: I18nPlural; // Drawing the game within the first %s moves will earn neither player any points.
    drawStreakStandard: I18nFormat; // Draw streaks: When a player has consecutive draws in an arena, only the first draw will result in a point or draws lasting more than %s moves in standard games. The draw streak can only be broken by a win, not a loss or a draw.
    drawStreakVariants: string; // The minimum game length for drawn games to award points differs by variant. The table below lists the threshold for each variant.
    editTeamBattle: string; // Edit team battle
    editTournament: string; // Edit tournament
    history: string; // Arena History
    howAreScoresCalculated: string; // How are scores calculated?
    howAreScoresCalculatedAnswer: string; // A win has a base score of 2 points, a draw 1 point, and a loss is worth no points.
    howDoesItEnd: string; // How does it end?
    howDoesItEndAnswer: string; // The tournament has a countdown clock. When it reaches zero, the tournament rankings are frozen, and the winner is announced. Games in progress must be finished, however, they don't count for the tournament.
    howDoesPairingWork: string; // How does the pairing work?
    howDoesPairingWorkAnswer: string; // At the beginning of the tournament, players are paired based on their rating.
    howIsTheWinnerDecided: string; // How is the winner decided?
    howIsTheWinnerDecidedAnswer: string; // The player(s) with the most points after the tournament's set time limit will be announced the winner(s).
    isItRated: string; // Is it rated?
    isNotRated: string; // This tournament is *not* rated and will *not* affect your rating.
    isRated: string; // This tournament is rated and will affect your rating.
    medians: string; // medians
    minimumGameLength: string; // Minimum game length
    myTournaments: string; // My tournaments
    newTeamBattle: string; // New Team Battle
    noArenaStreaks: string; // No Arena streaks
    noBerserkAllowed: string; // No Berserk allowed
    onlyTitled: string; // Only titled players
    onlyTitledHelp: string; // Require an official title to join the tournament
    otherRules: string; // Other important rules
    pickYourTeam: string; // Pick your team
    pointsAvg: string; // Points average
    pointsSum: string; // Points sum
    rankAvg: string; // Rank average
    rankAvgHelp: string; // The rank average is a percentage of your ranking. Lower is better.
    recentlyPlayed: string; // Recently played
    shareUrl: I18nFormat; // Share this URL to let people join: %s
    someRated: string; // Some tournaments are rated and will affect your rating.
    stats: string; // Stats
    thereIsACountdown: string; // There is a countdown for your first move. Failing to make a move within this time will forfeit the game to your opponent.
    thisIsPrivate: string; // This is a private tournament
    total: string; // Total
    tournamentShields: string; // Tournament shields
    tournamentStats: string; // Tournament stats
    tournamentWinners: string; // Tournament winners
    variant: string; // Variant
    viewAllXTeams: I18nPlural; // View all %s teams
    whichTeamWillYouRepresentInThisBattle: string; // Which team will you represent in this battle?
    willBeNotified: string; // You will be notified when the tournament starts, so it is safe to play in another tab while waiting.
    youMustJoinOneOfTheseTeamsToParticipate: string; // You must join one of these teams to participate!
  };
  broadcast: {
    aboutBroadcasts: string; // About broadcasts
    addRound: string; // Add a round
    ageThisYear: string; // Age this year
    broadcastCalendar: string; // Broadcast calendar
    broadcasts: string; // Broadcasts
    completed: string; // Completed
    completedHelp: string; // Lichess detects round completion, but can get it wrong. Use this to set it manually.
    credits: string; // Credit the source
    currentGameUrl: string; // Current game URL
    definitivelyDeleteRound: string; // Definitively delete the round and all its games.
    definitivelyDeleteTournament: string; // Definitively delete the entire tournament, all its rounds and all its games.
    deleteAllGamesOfThisRound: string; // Delete all games of this round. The source will need to be active in order to re-create them.
    deleteRound: string; // Delete this round
    deleteTournament: string; // Delete this tournament
    downloadAllRounds: string; // Download all rounds
    editRoundStudy: string; // Edit round study
    federation: string; // Federation
    fideFederations: string; // FIDE federations
    fidePlayerNotFound: string; // FIDE player not found
    fidePlayers: string; // FIDE players
    fideProfile: string; // FIDE profile
    fullDescription: string; // Full tournament description
    fullDescriptionHelp: I18nFormat; // Optional long description of the tournament. %1$s is available. Length must be less than %2$s characters.
    howToUseLichessBroadcasts: string; // How to use Lichess Broadcasts.
    liveBroadcasts: string; // Live tournament broadcasts
    myBroadcasts: string; // My broadcasts
    nbBroadcasts: I18nPlural; // %s broadcasts
    newBroadcast: string; // New live broadcast
    ongoing: string; // Ongoing
    periodInSeconds: string; // Period in seconds
    periodInSecondsHelp: string; // Optional, how long to wait between requests. Min 2s, max 60s. Defaults to automatic based on the number of viewers.
    recentTournaments: string; // Recent tournaments
    replacePlayerTags: string; // Optional: replace player names, ratings and titles
    resetRound: string; // Reset this round
    roundName: string; // Round name
    roundNumber: string; // Round number
    showScores: string; // Show players scores based on game results
    sourceGameIds: string; // Up to 64 Lichess game IDs, separated by spaces.
    sourceSingleUrl: string; // PGN Source URL
    sourceUrlHelp: string; // URL that Lichess will check to get PGN updates. It must be publicly accessible from the Internet.
    startDateHelp: string; // Optional, if you know when the event starts
    startDateTimeZone: I18nFormat; // Start date in the tournament local timezone: %s
    subscribedBroadcasts: string; // Subscribed broadcasts
    theNewRoundHelp: string; // The new round will have the same members and contributors as the previous one.
    top10Rating: string; // Top 10 rating
    tournamentDescription: string; // Short tournament description
    tournamentName: string; // Tournament name
    unrated: string; // Unrated
    upcoming: string; // Upcoming
  };
  challenge: {
    cannotChallengeDueToProvisionalXRating: I18nFormat; // Cannot challenge due to provisional %s rating.
    challengeAccepted: string; // Challenge accepted!
    challengeCanceled: string; // Challenge cancelled.
    challengeDeclined: string; // Challenge declined.
    challengesX: I18nFormat; // Challenges: %1$s
    challengeToPlay: string; // Challenge to a game
    declineCasual: string; // Please send me a casual challenge instead.
    declineGeneric: string; // I'm not accepting challenges at the moment.
    declineLater: string; // This is not the right time for me, please ask again later.
    declineNoBot: string; // I'm not accepting challenges from bots.
    declineOnlyBot: string; // I'm only accepting challenges from bots.
    declineRated: string; // Please send me a rated challenge instead.
    declineStandard: string; // I'm not accepting variant challenges right now.
    declineTimeControl: string; // I'm not accepting challenges with this time control.
    declineTooFast: string; // This time control is too fast for me, please challenge again with a slower game.
    declineTooSlow: string; // This time control is too slow for me, please challenge again with a faster game.
    declineVariant: string; // I'm not willing to play this variant right now.
    inviteLichessUser: string; // Or invite a Lichess user:
    registerToSendChallenges: string; // Please register to send challenges to this user.
    xDoesNotAcceptChallenges: I18nFormat; // %s does not accept challenges.
    xOnlyAcceptsChallengesFromFriends: I18nFormat; // %s only accepts challenges from friends.
    youCannotChallengeX: I18nFormat; // You cannot challenge %s.
    yourXRatingIsTooFarFromY: I18nFormat; // Your %1$s rating is too far from %2$s.
  };
  class: {
    addLichessUsernames: string; // Add Lichess usernames to invite them as teachers. One per line.
    addStudent: string; // Add student
    aLinkToTheClassWillBeAdded: string; // A link to the class will be automatically added at the end of the message, so you don't need to include it yourself.
    anInvitationHasBeenSentToX: I18nFormat; // An invitation has been sent to %s
    applyToBeLichessTeacher: string; // Apply to be a Lichess Teacher
    classDescription: string; // Class description
    className: string; // Class name
    classNews: string; // Class news
    clickToViewInvitation: string; // Click the link to view the invitation:
    closeClass: string; // Close class
    closeDesc1: string; // The student will never be able to use this account again. Closing is final. Make sure the student understands and agrees.
    closeDesc2: string; // You may want to give the student control over the account instead so that they can continue using it.
    closeStudent: string; // Close account
    closeTheAccount: string; // Close the student account permanently.
    createANewLichessAccount: string; // Create a new Lichess account
    createDesc1: string; // If the student doesn't have a Lichess account yet, you can create one for them here.
    createDesc2: string; // No email address is required. A password will be generated, and you will have to transmit it to the student so that they can log in.
    createDesc3: string; // Important: a student must not have multiple accounts.
    createDesc4: string; // If they already have one, use the invite form instead.
    createMoreClasses: string; // create more classes
    createMultipleAccounts: string; // Create multiple Lichess accounts at once
    createStudentWarning: string; // Only create accounts for real students. Do not use this to make multiple accounts for yourself. You would get banned.
    editNews: string; // Edit news
    features: string; // Features
    freeForAllForever: string; // 100% free for all, forever, with no ads or trackers
    generateANewPassword: string; // Generate a new password for the student
    generateANewUsername: string; // Generate a new username
    invitationToClass: I18nFormat; // You are invited to join the class "%s" as a student.
    invite: string; // Invite
    inviteALichessAccount: string; // Invite a Lichess account
    inviteDesc1: string; // If the student already has a Lichess account, you can invite them to the class.
    inviteDesc2: string; // They will receive a message on Lichess with a link to join the class.
    inviteDesc3: string; // Important: only invite students you know, and who actively want to join the class.
    inviteDesc4: string; // Never send unsolicited invites to arbitrary players.
    invitedToXByY: I18nFormat; // Invited to %1$s by %2$s
    inviteTheStudentBack: string; // Invite the student back
    lastActiveDate: string; // Active
    lichessClasses: string; // Classes
    lichessProfileXCreatedForY: I18nFormat; // Lichess profile %1$s created for %2$s.
    lichessUsername: string; // Lichess username
    makeSureToCopy: string; // Make sure to copy or write down the password now. You won’t ever be able to see it again!
    managed: string; // Managed
    maxStudentsNote: I18nFormat; // Note that a class can have up to %1$s students. To manage more students, %2$s.
    messageAllStudents: string; // Message all students about new class material
    multipleAccsFormDescription: I18nFormat; // You can also %s to create multiple Lichess accounts from a list of student names.
    na: string; // N/A
    nbPendingInvitations: I18nPlural; // %s pending invitations
    nbStudents: I18nPlural; // %s students
    nbTeachers: I18nPlural; // %s teachers
    newClass: string; // New class
    news: string; // News
    newsEdit1: string; // All class news in a single field.
    newsEdit2: string; // Add the recent news at the top. Don't delete previous news.
    newsEdit3: string; // Separate news with ---
    noClassesYet: string; // No classes yet.
    noRemovedStudents: string; // No removed students.
    noStudents: string; // No students in the class, yet.
    nothingHere: string; // Nothing here, yet.
    notifyAllStudents: string; // Notify all students
    onlyVisibleToTeachers: string; // Only visible to the class teachers
    orSeparator: string; // or
    overDays: string; // Over days
    overview: string; // Overview
    passwordX: I18nFormat; // Password: %s
    privateWillNeverBeShown: string; // Private. Will never be shown outside the class. Helps you remember who the student is.
    progress: string; // Progress
    quicklyGenerateSafeUsernames: string; // Quickly generate safe usernames and passwords for students
    realName: string; // Real name
    realUniqueEmail: string; // Real, unique email address of the student. We will send a confirmation email to it, with a link to graduate the account.
    release: string; // Graduate
    releaseDesc1: string; // A graduated account cannot be managed again. The student will be able to toggle kid mode and reset password themselves.
    releaseDesc2: string; // The student will remain in the class after their account is graduated.
    releaseTheAccount: string; // Graduate the account so the student can manage it autonomously.
    removedByX: I18nFormat; // Removed by %s
    removedStudents: string; // Removed
    removeStudent: string; // Remove student
    reopen: string; // Reopen
    resetPassword: string; // Reset password
    sendAMessage: string; // Send a message to all students.
    studentCredentials: I18nFormat; // Student:  %1$s
    students: string; // Students
    studentsRealNamesOnePerLine: string; // Students' real names, one per line
    teachClassesOfChessStudents: string; // Teach classes of chess students with the Lichess Classes tool suite.
    teachers: string; // Teachers
    teachersOfTheClass: string; // Teachers of the class
    teachersX: I18nFormat; // Teachers: %s
    thisStudentAccountIsManaged: string; // This student account is managed
    timePlaying: string; // Time playing
    trackStudentProgress: string; // Track student progress in games and puzzles
    upgradeFromManaged: string; // Upgrade from managed to autonomous
    useThisForm: string; // use this form
    variantXOverLastY: I18nFormat; // %1$s over last %2$s
    visibleByBothStudentsAndTeachers: string; // Visible by both teachers and students of the class
    welcomeToClass: I18nFormat; // Welcome to your class: %s.
    winrate: string; // Win rate
    xAlreadyHasAPendingInvitation: I18nFormat; // %s already has a pending invitation
    xIsAKidAccountWarning: I18nFormat; // %1$s is a kid account and can't receive your message. You must give them the invitation URL manually: %2$s
    xisNowAStudentOfTheClass: I18nFormat; // %s is now a student of the class
    youAcceptedThisInvitation: string; // You accepted this invitation.
    youDeclinedThisInvitation: string; // You declined this invitation.
    youHaveBeenInvitedByX: I18nFormat; // You have been invited by %s.
  };
  coach: {
    aboutMe: string; // About me
    accepting: string; // Accepting students
    areYouCoach: I18nFormat; // Are you a great chess coach with a %s?
    availability: string; // Availability
    bestSkills: string; // Best skills
    confirmTitle: string; // Confirm your title here and we will review your application.
    hourlyRate: string; // Hourly rate
    languages: string; // Languages
    lichessCoach: string; // Lichess coach
    lichessCoaches: string; // Lichess coaches
    location: string; // Location
    nmOrFideTitle: string; // NM or FIDE title
    notAccepting: string; // Not accepting students at the moment
    otherExperiences: string; // Other experiences
    playingExperience: string; // Playing experience
    publicStudies: string; // Public studies
    rating: string; // Rating
    sendApplication: I18nFormat; // Send us an email at %s and we will review your application.
    sendPM: string; // Send a private message
    teachingExperience: string; // Teaching experience
    teachingMethod: string; // Teaching methodology
    viewXProfile: I18nFormat; // View %s Lichess profile
    xCoachesStudents: I18nFormat; // %s coaches chess students
    youtubeVideos: string; // YouTube videos
  };
  contact: {
    accountLost: string; // However if you indeed used engine assistance, even just once, then your account is unfortunately lost.
    accountSupport: string; // I need account support
    authorizationToUse: string; // Authorisation to use Lichess
    banAppeal: string; // Appeal for a ban or IP restriction
    botRatingAbuse: string; // In certain circumstances when playing against a bot account, a rated game may not award points if it is determined that the player is abusing the bot for rating points.
    buyingLichess: string; // Buying Lichess
    calledEnPassant: string; // It is called "en passant" and is one of the rules of chess.
    cantChangeMore: string; // We can't change more than the case. For technical reasons, it's downright impossible.
    cantClearHistory: string; // It's not possible to clear your game history, puzzle history, or ratings.
    castlingImported: string; // If you imported the game, or started it from a position, make sure you correctly set the castling rights.
    castlingPrevented: string; // Castling is only prevented if the king goes through a controlled square.
    castlingRules: string; // Make sure you understand the castling rules
    changeUsernameCase: string; // Visit this page to change the case of your username
    closeYourAccount: string; // You can close your account on this page
    collaboration: string; // Collaboration, legal, commercial
    contact: string; // Contact
    contactLichess: string; // Contact Lichess
    creditAppreciated: string; // Credit is appreciated but not required.
    doNotAskByEmail: string; // Do not ask us by email to close an account, we won't do it.
    doNotAskByEmailToReopen: string; // Do not ask us by email to reopen an account, we won't do it.
    doNotDeny: string; // Do not deny having cheated. If you want to be allowed to create a new account, just admit to what you did, and show that you understood that it was a mistake.
    doNotMessageModerators: string; // Please do not send direct messages to moderators.
    doNotReportInForum: string; // Do not report players in the forum.
    doNotSendReportEmails: string; // Do not send us report emails.
    doPasswordReset: string; // Complete a password reset to remove your second factor
    engineAppeal: string; // Engine or cheat mark
    errorPage: string; // Error page
    explainYourRequest: string; // Please explain your request clearly and thoroughly. State your Lichess username, and any information that could help us help you.
    falsePositives: string; // False positives do happen sometimes, and we're sorry about that.
    fideMate: string; // According to the FIDE Laws of Chess §6.9, if a checkmate is possible with any legal sequence of moves, then the game is not a draw
    forgotPassword: string; // I forgot my password
    forgotUsername: string; // I forgot my username
    howToReportBug: string; // Please describe what the bug looks like, what you expected to happen instead, and the steps to reproduce the bug.
    iCantLogIn: string; // I can't log in
    ifLegit: string; // If your appeal is legitimate, we will lift the ban ASAP.
    illegalCastling: string; // Illegal or impossible castling
    illegalPawnCapture: string; // Illegal pawn capture
    insufficientMaterial: string; // Insufficient mating material
    knightMate: string; // It is possible to checkmate with only a knight or a bishop, if the opponent has more than a king on the board.
    learnHowToMakeBroadcasts: string; // Learn how to make your own broadcasts on Lichess
    lost2FA: string; // I lost access to my two-factor authentication codes
    monetizing: string; // Monetising Lichess
    noConfirmationEmail: string; // I didn't receive my confirmation email
    noneOfTheAbove: string; // None of the above
    noRatingPoints: string; // No rating points were awarded
    onlyReports: string; // Only reporting players through the report form is effective.
    orCloseAccount: string; // However, you can close your current account, and create a new one.
    otherRestriction: string; // Other restriction
    ratedGame: string; // Make sure you played a rated game. Casual games do not affect the players ratings.
    reopenOnThisPage: string; // You can reopen your account on this page. It only works once.
    reportBugInDiscord: string; // In the Lichess Discord server
    reportBugInForum: string; // In the Lichess Feedback section of the forum
    reportErrorPage: string; // If you faced an error page, you may report it:
    reportMobileIssue: string; // As a Lichess mobile app issue on GitHub
    reportWebsiteIssue: string; // As a Lichess website issue on GitHub
    sendAppealTo: I18nFormat; // You may send an appeal to %s.
    sendEmailAt: I18nFormat; // Send us an email at %s.
    toReportAPlayerUseForm: string; // To report a player, use the report form
    tryCastling: string; // Try this little interactive game to practice castling in chess
    tryEnPassant: string; // Try this little interactive game to learn more about "en passant".
    videosAndBooks: string; // You can show it in your videos, and you can print screenshots of Lichess in your books.
    visitThisPage: string; // Visit this page to solve the issue
    visitTitleConfirmation: string; // To show your title on your Lichess profile, and participate in Titled Arenas, visit the title confirmation page
    wantChangeUsername: string; // I want to change my username
    wantClearHistory: string; // I want to clear my history or rating
    wantCloseAccount: string; // I want to close my account
    wantReopen: string; // I want to reopen my account
    wantReport: string; // I want to report a player
    wantReportBug: string; // I want to report a bug
    wantTitle: string; // I want my title displayed on Lichess
    welcomeToUse: string; // You are welcome to use Lichess for your activity, even commercial.
    whatCanWeHelpYouWith: string; // What can we help you with?
    youCanAlsoReachReportPage: I18nFormat; // You can also reach that page by clicking the %s report button on a profile page.
    youCanLoginWithEmail: string; // You can login with the email address you signed up with
  };
  coordinates: {
    aCoordinateAppears: string; // A coordinate appears on the board and you must click on the corresponding square.
    aSquareIsHighlightedExplanation: string; // A square is highlighted on the board and you must enter its coordinate (e.g. "e4").
    averageScoreAsBlackX: I18nFormat; // Average score as black: %s
    averageScoreAsWhiteX: I18nFormat; // Average score as white: %s
    coordinates: string; // Coordinates
    coordinateTraining: string; // Coordinate training
    findSquare: string; // Find square
    goAsLongAsYouWant: string; // Go as long as you want, there is no time limit!
    knowingTheChessBoard: string; // Knowing the chessboard coordinates is a very important skill for several reasons:
    mostChessCourses: string; // Most chess courses and exercises use the algebraic notation extensively.
    nameSquare: string; // Name square
    showCoordinates: string; // Show coordinates
    showCoordsOnAllSquares: string; // Coordinates on every square
    showPieces: string; // Show pieces
    startTraining: string; // Start training
    talkToYourChessFriends: string; // It makes it easier to talk to your chess friends, since you both understand the 'language of chess'.
    youCanAnalyseAGameMoreEffectively: string; // You can analyse a game more effectively if you can quickly recognise coordinates.
    youHaveThirtySeconds: string; // You have 30 seconds to correctly map as many squares as possible!
  };
  dgt: {
    announceAllMoves: string; // Announce All Moves
    announceMoveFormat: string; // Announce Move Format
    asALastResort: I18nFormat; // As a last resort, setup the board identically as Lichess, then %s
    boardWillAutoConnect: string; // The board will auto connect to any game that is already on course or any new game that starts. Ability to choose which game to play is coming soon.
    checkYouHaveMadeOpponentsMove: string; // Check that you have made your opponent's move on the DGT board first. Revert your move. Play again.
    clickToGenerateOne: string; // Click to generate one
    configurationSection: string; // Configuration Section
    configure: string; // Configure
    configureVoiceNarration: string; // Configure voice narration of the played moves, so you can keep your eyes on the board.
    debug: string; // Debug
    dgtBoard: string; // DGT board
    dgtBoardConnectivity: string; // DGT board connectivity
    dgtBoardLimitations: string; // DGT Board Limitations
    dgtBoardRequirements: string; // DGT Board Requirements
    dgtConfigure: string; // DGT - Configure
    dgtPlayMenuEntryAdded: I18nFormat; // A %s entry was added to your PLAY menu at the top.
    downloadHere: I18nFormat; // You can download the software here: %s.
    enableSpeechSynthesis: string; // Enable Speech Synthesis
    ifLiveChessRunningElsewhere: I18nFormat; // If %1$s is running on a different machine or different port, you will need to set the IP address and port here in the %2$s.
    ifLiveChessRunningOnThisComputer: I18nFormat; // If %1$s is running on this computer, you can check your connection to it by %2$s.
    ifMoveNotDetected: string; // If a move is not detected
    keepPlayPageOpen: string; // The play page needs to remain open on your browser. It does not need to be visible, you can minimize it or set it side to side with the Lichess game page, but don't close it or the board will stop working.
    keywordFormatDescription: string; // Keywords are in JSON format. They are used to translate moves and results into your language. Default is English, but feel free to change it.
    keywords: string; // Keywords
    lichessAndDgt: string; // Lichess & DGT
    lichessConnectivity: string; // Lichess connectivity
    moveFormatDescription: string; // SAN is the standard on Lichess like "Nf6". UCI is common on engines like "g8f6".
    noSuitableOauthToken: string; // No suitable OAuth token has been created.
    openingThisLink: string; // opening this link
    playWithDgtBoard: string; // Play with a DGT board
    reloadThisPage: string; // Reload this page
    selectAnnouncePreference: string; // Select YES to announce both your moves and your opponent's moves. Select NO to announce only your opponent's moves.
    speechSynthesisVoice: string; // Speech synthesis voice
    textToSpeech: string; // Text to speech
    thisPageAllowsConnectingDgtBoard: string; // This page allows you to connect your DGT board to Lichess, and to use it for playing games.
    timeControlsForCasualGames: string; // Time controls for casual games: Classical, Correspondence and Rapid only.
    timeControlsForRatedGames: string; // Time controls for rated games: Classical, Correspondence and some Rapids including 15+10 and 20+0
    toConnectTheDgtBoard: I18nFormat; // To connect to the DGT Electronic Board you will need to install %s.
    toSeeConsoleMessage: string; // To see console message press Command + Option + C (Mac) or Control + Shift + C (Windows, Linux, Chrome OS)
    useWebSocketUrl: I18nFormat; // Use \"%1$s\" unless %2$s is running on a different machine or different port.
    validDgtOauthToken: string; // You have an OAuth token suitable for DGT play.
    verboseLogging: string; // Verbose logging
    webSocketUrl: I18nFormat; // %s WebSocket URL
    whenReadySetupBoard: I18nFormat; // When ready, setup your board and then click %s.
  };
  emails: {
    common_contact: I18nFormat; // To contact us, please use %s.
    common_note: I18nFormat; // This is a service email related to your use of %s.
    common_orPaste: string; // (Clicking not working? Try pasting it into your browser!)
    emailChange_click: string; // To confirm you have access to this email, please click the link below:
    emailChange_intro: string; // You have requested to change your email address.
    emailChange_subject: I18nFormat; // Confirm new email address, %s
    emailConfirm_click: string; // Click the link to enable your Lichess account:
    emailConfirm_ignore: string; // If you did not register with Lichess you can safely ignore this message.
    emailConfirm_subject: I18nFormat; // Confirm your lichess.org account, %s
    logInToLichess: I18nFormat; // Log in to lichess.org, %s
    passwordReset_clickOrIgnore: string; // If you made this request, click the link below. If not, you can ignore this email.
    passwordReset_intro: string; // We received a request to reset the password for your account.
    passwordReset_subject: I18nFormat; // Reset your lichess.org password, %s
    welcome_subject: I18nFormat; // Welcome to lichess.org, %s
    welcome_text: I18nFormat; // You have successfully created your account on https://lichess.org.
  };
  faq: {
    accounts: string; // Accounts
    acplExplanation: string; // The centipawn is the unit of measure used in chess as representation of the advantage. A centipawn is equal to 1/100th of a pawn. Therefore 100 centipawns = 1 pawn. These values play no formal role in the game but are useful to players, and essential in computer chess, for evaluating positions.
    adviceOnMitigatingAddiction: I18nFormat; // We regularly receive messages from users asking us for help to stop them from playing too much.
    aHourlyBulletTournament: string; // an hourly Bullet tournament
    areThereWebsitesBasedOnLichess: string; // Are there websites based on Lichess?
    asWellAsManyNMtitles: string; // many national master titles
    basedOnGameDuration: I18nFormat; // Lichess time controls are based on estimated game duration = %1$s.
    beingAPatron: string; // being a patron
    beInTopTen: string; // be in the top 10 in this rating.
    breakdownOfOurCosts: string; // breakdown of our costs
    canIbecomeLM: string; // Can I get the Lichess Master (LM) title?
    canIChangeMyUsername: string; // Can I change my username?
    configure: string; // configure
    connexionLostCanIGetMyRatingBack: string; // I lost a game due to lag/disconnection. Can I get my rating points back?
    desktop: string; // desktop
    discoveringEnPassant: string; // Why can a pawn capture another pawn when it is already passed? (en passant)
    displayPreferences: string; // display preferences
    durationFormula: string; // (clock initial time in seconds) + 40 × (clock increment)
    eightVariants: string; // 8 chess variants
    enableAutoplayForSoundsA: string; // Most browsers can prevent sound from playing on a freshly loaded page to protect users. Imagine if every website could immediately bombard you with audio ads.
    enableAutoplayForSoundsChrome: string; // 1. Go to lichess.org
    enableAutoplayForSoundsFirefox: string; // 1. Go to lichess.org
    enableAutoplayForSoundsMicrosoftEdge: string; // 1. Click the three dots in the top right corner
    enableAutoplayForSoundsQ: string; // Enable autoplay for sounds?
    enableAutoplayForSoundsSafari: string; // 1. Go to lichess.org
    enableDisableNotificationPopUps: string; // Enable or disable notification popups?
    enableZenMode: I18nFormat; // Enable Zen-mode in the %1$s, or by pressing %2$s during a game.
    explainingEnPassant: I18nFormat; // This is a legal move known as "en passant". The Wikipedia article gives a %1$s.
    fairPlay: string; // Fair Play
    fairPlayPage: string; // fair play page
    faqAbbreviation: string; // FAQ
    fewerLobbyPools: string; // fewer lobby pools
    fideHandbook: string; // FIDE handbook
    fideHandbookX: I18nFormat; // FIDE handbook %s
    findMoreAndSeeHowHelp: I18nFormat; // You can find out more about %1$s (including a %2$s). If you want to help Lichess by volunteering your time and skills, there are many %3$s.
    frequentlyAskedQuestions: string; // Frequently Asked Questions
    gameplay: string; // Gameplay
    goldenZeeExplanation: string; // ZugAddict was streaming and for the last 2 hours he had been trying to defeat A.I. level 8 in a 1+0 game, without success. Thibault told him that if he successfully did it on stream, he'd get a unique trophy. One hour later, he smashed Stockfish, and the promise was honoured.
    goodIntroduction: string; // good introduction
    guidelines: string; // guidelines
    havePlayedARatedGameAtLeastOneWeekAgo: string; // have played a rated game within the last week for this rating,
    havePlayedMoreThanThirtyGamesInThatRating: string; // have played at least 30 rated games in a given rating,
    hearItPronouncedBySpecialist: string; // Hear it pronounced by a specialist.
    howBulletBlitzEtcDecided: string; // How are Bullet, Blitz and other time controls decided?
    howCanIBecomeModerator: string; // How can I become a moderator?
    howCanIContributeToLichess: string; // How can I contribute to Lichess?
    howDoLeaderoardsWork: string; // How do ranks and leaderboards work?
    howToHideRatingWhilePlaying: string; // How to hide ratings while playing?
    howToThreeDots: string; // How to...
    inferiorThanXsEqualYtimeControl: I18nFormat; // ≤ %1$ss = %2$s
    inOrderToAppearsYouMust: I18nFormat; // In order to get on the %1$s you must:
    insufficientMaterial: string; // Losing on time, drawing and insufficient material
    isCorrespondenceDifferent: string; // Is correspondence different from normal chess?
    keyboardShortcuts: string; // What keyboard shortcuts are there?
    keyboardShortcutsExplanation: string; // Some Lichess pages have keyboard shortcuts you can use. Try pressing the '?' key on a study, analysis, puzzle, or game page to list available keyboard shortcuts.
    leavingGameWithoutResigningExplanation: string; // If your opponent frequently aborts/leaves games, they get "play banned", which means they're temporarily banned from playing games. This is not publicly indicated on their profile. If this behaviour continues, the length of the playban increases - and prolonged behaviour of this nature may lead to account closure.
    leechess: string; // lee-chess
    lichessCanOptionnalySendPopUps: string; // Lichess can optionally send popup notifications, for example when it is your turn or you received a private message.
    lichessCombinationLiveLightLibrePronounced: I18nFormat; // Lichess is a combination of live/light/libre and chess. It is pronounced %1$s.
    lichessFollowFIDErules: I18nFormat; // In the event of one player running out of time, that player will usually lose the game. However, the game is drawn if the position is such that the opponent cannot checkmate the player's king by any possible series of legal moves (%1$s).
    lichessPoweredByDonationsAndVolunteers: string; // Lichess is powered by donations from patrons and the efforts of a team of volunteers.
    lichessRatings: string; // Lichess ratings
    lichessRecognizeAllOTBtitles: I18nFormat; // Lichess recognises all FIDE titles gained from OTB (over the board) play, as well as %1$s. Here is a list of FIDE titles:
    lichessSupportChessAnd: I18nFormat; // Lichess supports standard chess and %1$s.
    lichessTraining: string; // Lichess training
    lichessUserstyles: string; // Lichess userstyles
    lMtitleComesToYouDoNotRequestIt: string; // This honorific title is unofficial and only exists on Lichess.
    mentalHealthCondition: string; // stand-alone mental health condition
    notPlayedEnoughRatedGamesAgainstX: I18nFormat; // The player has not yet finished enough rated games against %1$s in the rating category.
    notPlayedRecently: string; // The player hasn't played enough recent games. Depending on the number of games you've played, it might take around a year of inactivity for your rating to become provisional again.
    notRepeatedMoves: string; // We did not repeat moves. Why was the game still drawn by repetition?
    noUpperCaseDot: string; // No.
    otherWaysToHelp: string; // other ways to help
    ownerUniqueTrophies: I18nFormat; // That trophy is unique in the history of Lichess, nobody other than %1$s will ever have it.
    pleaseReadFairPlayPage: I18nFormat; // For more information, please read our %s
    positions: string; // positions
    preventLeavingGameWithoutResigning: string; // What is done about players leaving games without resigning?
    provisionalRatingExplanation: string; // The question mark means the rating is provisional. Reasons include:
    ratingDeviationLowerThanXinChessYinVariants: I18nFormat; // have a rating deviation lower than %1$s, in standard chess, and lower than %2$s in variants,
    ratingDeviationMorethanOneHundredTen: string; // Concretely, it means that the Glicko-2 deviation is greater than 110. The deviation is the level of confidence the system has in the rating. The lower the deviation, the more stable is a rating.
    ratingLeaderboards: string; // rating leaderboard
    ratingRefundExplanation: string; // One minute after a player is marked, their 40 latest rated games in the last 5 days are taken. If you were their opponent in one of those games, you lost rating (because of a loss or a draw), and your rating was not provisional, you get a rating refund. The refund is capped based on your peak rating and your rating progress after the game. (For example, if your rating greatly increased after that game, you might get no refund or only a partial refund.) A refund will never exceed 150 points.
    ratingSystemUsedByLichess: string; // Ratings are calculated using the Glicko-2 rating method developed by Mark Glickman. This is a very popular rating method, and is used by a significant number of chess organisations (FIDE being a notable counter-example, as they still use the dated Elo rating system).
    repeatedPositionsThatMatters: I18nFormat; // Threefold repetition is about repeated %1$s, not moves. Repetition does not have to occur consecutively.
    secondRequirementToStopOldPlayersTrustingLeaderboards: string; // The 2nd requirement is so that players who no longer use their accounts stop populating leaderboards.
    showYourTitle: I18nFormat; // If you have an OTB title, you can apply to have this displayed on your account by completing the %1$s, including a clear image of an identifying document/card and a selfie of you holding the document/card.
    similarOpponents: string; // opponents of similar strength
    stopMyselfFromPlaying: string; // Stop myself from playing?
    superiorThanXsEqualYtimeControl: I18nFormat; // ≥ %1$ss = %2$s
    threeFoldHasToBeClaimed: I18nFormat; // Repetition needs to be claimed by one of the players. You can do so by pressing the button that is shown, or by offering a draw before your final repeating move, it won't matter if your opponent rejects the draw offer, the threefold repetition draw will be claimed anyway. You can also %1$s Lichess to automatically claim repetitions for you. Additionally, fivefold repetition always immediately ends the game.
    threefoldRepetition: string; // Threefold repetition
    threefoldRepetitionExplanation: I18nFormat; // If a position occurs three times, players can claim a draw by %1$s. Lichess implements the official FIDE rules, as described in Article 9.2 of the %2$s.
    threefoldRepetitionLowerCase: string; // threefold repetition
    titlesAvailableOnLichess: string; // What titles are there on Lichess?
    uniqueTrophies: string; // Unique trophies
    usernamesCannotBeChanged: string; // No, usernames cannot be changed for technical and practical reasons. Usernames are materialized in too many places: databases, exports, logs, and people's minds. You can adjust the capitalization once.
    usernamesNotOffensive: I18nFormat; // In general, usernames should not be: offensive, impersonating someone else, or advertising. You can read more about the %1$s.
    verificationForm: string; // verification form
    viewSiteInformationPopUp: string; // View site information popup
    watchIMRosenCheckmate: I18nFormat; // Watch International Master Eric Rosen checkmate %s.
    wayOfBerserkExplanation: I18nFormat; // To get it, hiimgosu challenged himself to berserk and win all games of %s.
    weCannotDoThatEvenIfItIsServerSideButThatsRare: string; // Unfortunately, we cannot give back rating points for games lost due to lag or disconnection, regardless of whether the problem was at your end or our end. The latter is very rare though. Also note that when Lichess restarts and you lose on time because of that, we abort the game to prevent an unfair loss.
    weRepeatedthreeTimesPosButNoDraw: string; // We repeated a position three times. Why was the game not drawn?
    whatIsACPL: string; // What is the average centipawn loss (ACPL)?
    whatIsProvisionalRating: string; // Why is there a question mark (?) next to a rating?
    whatUsernameCanIchoose: string; // What can my username be?
    whatVariantsCanIplay: string; // What variants can I play on Lichess?
    whenAmIEligibleRatinRefund: string; // When am I eligible for the automatic rating refund from cheaters?
    whichRatingSystemUsedByLichess: string; // What rating system does Lichess use?
    whyAreRatingHigher: string; // Why are ratings higher compared to other sites and organisations such as FIDE, USCF and the ICC?
    whyAreRatingHigherExplanation: string; // It is best not to think of ratings as absolute numbers, or compare them against other organisations. Different organisations have different levels of players, different rating systems (Elo, Glicko, Glicko-2, or a modified version of the aforementioned). These factors can drastically affect the absolute numbers (ratings).
    whyIsLichessCalledLichess: string; // Why is Lichess called Lichess?
    whyIsLilaCalledLila: I18nFormat; // Similarly, the source code for Lichess, %1$s, stands for li[chess in sca]la, seeing as the bulk of Lichess is written in %2$s, an intuitive programming language.
    whyLiveLightLibre: string; // Live, because games are played and watched in real-time 24/7; light and libre for the fact that Lichess is open-source and unencumbered by proprietary junk that plagues other websites.
    yesLichessInspiredOtherOpenSourceWebsites: I18nFormat; // Yes. Lichess has indeed inspired other open-source sites that use our %1$s, %2$s, or %3$s.
    youCannotApply: string; // It’s not possible to apply to become a moderator. If we see someone who we think would be good as a moderator, we will contact them directly.
    youCanUseOpeningBookNoEngine: string; // On Lichess, the main difference in rules for correspondence chess is that an opening book is allowed. The use of engines is still prohibited and will result in being flagged for engine assistance. Although ICCF allows engine use in correspondence, Lichess does not.
  };
  features: {
    allChessBasicsLessons: string; // All chess basics lessons
    allFeaturesAreFreeForEverybody: string; // All features are free for everybody, forever!
    allFeaturesToCome: string; // All features to come, forever!
    boardEditorAndAnalysisBoardWithEngine: I18nFormat; // Board editor and analysis board with %s
    chessInsights: string; // Chess insights (detailed analysis of your play)
    cloudEngineAnalysis: string; // Cloud engine analysis
    contributeToLichessAndGetIcon: string; // Contribute to Lichess and get a cool looking Patron icon
    correspondenceWithConditionalPremoves: string; // Correspondence chess with conditional premoves
    deepXServerAnalysis: I18nFormat; // Deep %s server analysis
    downloadOrUploadAnyGameAsPgn: string; // Download/Upload any game as PGN
    endgameTablebase: string; // 7-piece endgame tablebase
    everybodyGetsAllFeaturesForFree: string; // Yes, both accounts have the same features!
    gamesPerDay: I18nPlural; // %s games per day
    globalOpeningExplorerInNbGames: I18nFormat; // Global opening explorer (%s games!)
    ifYouLoveLichess: string; // If you love Lichess,
    landscapeSupportOnApp: string; // iPhone & Android phones and tablets, landscape support
    lightOrDarkThemeCustomBoardsPiecesAndBackground: string; // Light/Dark theme, custom boards, pieces and background
    personalOpeningExplorer: string; // Personal opening explorer
    personalOpeningExplorerX: I18nFormat; // %1$s (also works on %2$s)
    standardChessAndX: I18nFormat; // Standard chess and %s
    studies: string; // Studies (shareable and persistent analysis)
    supportLichess: string; // Support Lichess
    supportUsWithAPatronAccount: string; // Support us with a Patron account!
    tacticalPuzzlesFromUserGames: string; // Tactical puzzles from user games
    tvForumBlogTeamsMessagingFriendsChallenges: string; // Blog, forum, teams, TV, messaging, friends, challenges
    ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess: string; // UltraBullet, Bullet, Blitz, Rapid, Classical, Correspondence Chess
    weBelieveEveryChessPlayerDeservesTheBest: string; // We believe every chess player deserves the best, and so:
    zeroAdsAndNoTracking: string; // Zero advertisement, no tracking
  };
  insight: {
    cantSeeInsights: I18nFormat; // Sorry, you cannot see %s's chess insights.
    crunchingData: string; // Now crunching data just for you!
    generateInsights: I18nFormat; // Generate %s's chess insights.
    insightsAreProtected: I18nFormat; // %s's chess insights are protected
    insightsSettings: string; // insights settings
    maybeAskThemToChangeTheir: I18nFormat; // Maybe ask them to change their %s?
    xChessInsights: I18nFormat; // %s's chess insights
    xHasNoChessInsights: I18nFormat; // %s has no chess insights yet!
  };
  keyboardMove: {
    bothTheLetterOAndTheDigitZero: string; // Both the letter "o" and the digit zero "0" can be used when castling
    capitalizationOnlyMattersInAmbiguousSituations: string; // Capitalization only matters in ambiguous situations involving a bishop and the b-pawn
    dropARookAtB4: string; // Drop a rook at b4 (Crazyhouse variant only)
    ifItIsLegalToCastleBothWays: string; // If it is legal to castle both ways, use enter to kingside castle
    ifTheAboveMoveNotationIsUnfamiliar: string; // If the above move notation is unfamiliar, learn more here:
    includingAXToIndicateACapture: string; // Including an "x" to indicate a capture is optional
    keyboardInputCommands: string; // Keyboard input commands
    kingsideCastle: string; // Kingside castle
    moveKnightToC3: string; // Move knight to c3
    movePieceFromE2ToE4: string; // Move piece from e2 to e4
    offerOrAcceptDraw: string; // Offer or accept draw
    otherCommands: string; // Other commands
    performAMove: string; // Perform a move
    promoteC8ToQueen: string; // Promote c8 to queen
    queensideCastle: string; // Queenside castle
    readOutClocks: string; // Read out clocks
    readOutOpponentName: string; // Read out opponent's name
    tips: string; // Tips
    toPremoveSimplyTypeTheDesiredPremove: string; // To premove, simply type the desired premove before it is your turn
  };
  lag: {
    andNowTheLongAnswerLagComposedOfTwoValues: string; // And now, the long answer! Game lag is composed of two unrelated values (lower is better):
    isLichessLagging: string; // Is Lichess lagging?
    lagCompensation: string; // Lag compensation
    lagCompensationExplanation: string; // Lichess compensates network lag. This includes sustained lag and occasional lag spikes. There are limits and heuristics based on time control and the compensated lag so far, so that the result should feel reasonable for both players. As a result, having a higher network lag than your opponent is not a handicap!
    lichessServerLatency: string; // Lichess server latency
    lichessServerLatencyExplanation: string; // The time it takes to process a move on the server. It's the same for everybody, and only depends on the servers load. The more players, the higher it gets, but Lichess developers do their best to keep it low. It rarely exceeds 10ms.
    measurementInProgressThreeDot: string; // Measurements in progress...
    networkBetweenLichessAndYou: string; // Network between Lichess and you
    networkBetweenLichessAndYouExplanation: string; // The time it takes to send a move from your computer to Lichess server, and get the response back. It's specific to your distance to Lichess (France), and to the quality of your Internet connection. Lichess developers cannot fix your wifi or make light go faster.
    noAndYourNetworkIsBad: string; // No. And your network is bad.
    noAndYourNetworkIsGood: string; // No. And your network is good.
    yesItWillBeFixedSoon: string; // Yes. It will be fixed soon!
    youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername: string; // You can find both these values at any time, by clicking your username in the top bar.
  };
  learn: {
    advanced: string; // Advanced
    aPawnOnTheSecondRank: string; // A pawn on the second rank can move 2 squares at once!
    attackTheOpponentsKing: string; // Attack the opponent's king
    attackYourOpponentsKing: string; // Attack your opponent's king
    awesome: string; // Awesome!
    backToMenu: string; // Back to menu
    bishopComplete: string; // Congratulations! You can command a bishop.
    bishopIntro: string; // Next we will learn how to manoeuvre a bishop!
    blackJustMovedThePawnByTwoSquares: string; // Black just moved the pawn
    boardSetup: string; // Board setup
    boardSetupComplete: string; // Congratulations! You know how to set up the chess board.
    boardSetupIntro: string; // The two armies face each other, ready for the battle.
    byPlaying: string; // by playing!
    capture: string; // Capture
    captureAndDefendPieces: string; // Capture and defend pieces
    captureComplete: string; // Congratulations! You know how to fight with chess pieces!
    captureIntro: string; // Identify the opponent's undefended pieces, and capture them!
    captureThenPromote: string; // Capture, then promote!
    castleKingSide: string; // Move your king two squares
    castleKingSideMovePiecesFirst: string; // Castle king-side!
    castleQueenSide: string; // Move your king two squares
    castleQueenSideMovePiecesFirst: string; // Castle queen-side!
    castling: string; // Castling
    castlingComplete: string; // Congratulations! You should almost always castle in a game.
    castlingIntro: string; // Bring your king to safety, and deploy your rook for attack!
    checkInOne: string; // Check in one
    checkInOneComplete: string; // Congratulations! You checked your opponent, forcing them to defend their king!
    checkInOneGoal: string; // Aim at the opponent's king
    checkInOneIntro: string; // To check your opponent, attack their king. They must defend it!
    checkInTwo: string; // Check in two
    checkInTwoComplete: string; // Congratulations! You checked your opponent, forcing them to defend their king!
    checkInTwoGoal: string; // Threaten the opponent's king
    checkInTwoIntro: string; // Find the right combination of two moves that checks the opponent's king!
    chessPieces: string; // Chess pieces
    combat: string; // Combat
    combatComplete: string; // Congratulations! You know how to fight with chess pieces!
    combatIntro: string; // A good warrior knows both attack and defence!
    defeatTheOpponentsKing: string; // Defeat the opponent's king
    defendYourKing: string; // Defend your king
    dontLetThemTakeAnyUndefendedPiece: string; // Don't let them take
    enPassant: string; // En passant
    enPassantComplete: string; // Congratulations! You can now take en passant.
    enPassantIntro: string; // When the opponent pawn moved by two squares, you can take it like if it moved by one square.
    enPassantOnlyWorksImmediately: string; // En passant only works
    enPassantOnlyWorksOnFifthRank: string; // En passant only works
    escape: string; // You're under attack!
    escapeOrBlock: string; // Escape with the king
    escapeWithTheKing: string; // Escape with the king!
    evaluatePieceStrength: string; // Evaluate piece strength
    excellent: string; // Excellent!
    exerciseYourTacticalSkills: string; // Exercise your tactical skills
    findAWayToCastleKingSide: string; // Find a way to
    findAWayToCastleQueenSide: string; // Find a way to
    firstPlaceTheRooks: string; // First place the rooks!
    fundamentals: string; // Fundamentals
    getAFreeLichessAccount: string; // Get a free Lichess account
    grabAllTheStars: string; // Grab all the stars!
    grabAllTheStarsNoNeedToPromote: string; // Grab all the stars!
    greatJob: string; // Great job!
    howTheGameStarts: string; // How the game starts
    intermediate: string; // Intermediate
    itMovesDiagonally: string; // It moves diagonally
    itMovesForwardOnly: string; // It moves forward only
    itMovesInAnLShape: string; // It moves in an L shape
    itMovesInStraightLines: string; // It moves in straight lines
    itNowPromotesToAStrongerPiece: string; // It now promotes to a stronger piece.
    keepYourPiecesSafe: string; // Keep your pieces safe
    kingComplete: string; // You can now command the commander!
    kingIntro: string; // You are the king. If you fall in battle, the game is lost.
    knightComplete: string; // Congratulations! You have mastered the knight.
    knightIntro: string; // Here's a challenge for you. The knight is... a tricky piece.
    knightsCanJumpOverObstacles: string; // Knights can jump over obstacles!
    knightsHaveAFancyWay: string; // Knights have a fancy way
    lastOne: string; // Last one!
    learnChess: string; // Learn chess
    learnCommonChessPositions: string; // Learn common chess positions
    letsGo: string; // Let's go!
    mateInOne: string; // Mate in one
    mateInOneComplete: string; // Congratulations! That is how you win chess games!
    mateInOneIntro: string; // You win when your opponent cannot defend against a check.
    menu: string; // Menu
    mostOfTheTimePromotingToAQueenIsBest: string; // Most of the time promoting to a queen is the best.
    nailedIt: string; // Nailed it.
    next: string; // Next
    nextX: I18nFormat; // Next: %s
    noEscape: string; // There is no escape,
    opponentsFromAroundTheWorld: string; // Opponents from around the world
    outOfCheck: string; // Out of check
    outOfCheckComplete: string; // Congratulations! Your king can never be taken, make sure you can defend against a check!
    outOfCheckIntro: string; // You are in check! You must escape or block the attack.
    outstanding: string; // Outstanding!
    pawnComplete: string; // Congratulations! Pawns have no secrets for you.
    pawnIntro: string; // Pawns are weak, but they pack a lot of potential.
    pawnPromotion: string; // Pawn promotion
    pawnsFormTheFrontLine: string; // Pawns form the front line.
    pawnsMoveForward: string; // Pawns move forward,
    pawnsMoveOneSquareOnly: string; // Pawns move one square only.
    perfect: string; // Perfect!
    pieceValue: string; // Piece value
    pieceValueComplete: string; // Congratulations! You know the value of material!
    pieceValueExchange: string; // Take the piece with the highest value!
    pieceValueIntro: string; // Pieces with high mobility have a higher value!
    pieceValueLegal: string; // Take the piece
    placeTheBishops: string; // Place the bishops!
    placeTheKing: string; // Place the king!
    placeTheQueen: string; // Place the queen!
    play: string; // play!
    playMachine: string; // Play machine
    playPeople: string; // Play people
    practice: string; // Practise
    progressX: I18nFormat; // Progress: %s
    protection: string; // Protection
    protectionComplete: string; // Congratulations! A piece you don't lose is a piece you win!
    protectionIntro: string; // Identify the pieces your opponent attacks, and defend them!
    puzzleFailed: string; // Puzzle failed!
    puzzles: string; // Puzzles
    queenCombinesRookAndBishop: string; // Queen = rook + bishop
    queenComplete: string; // Congratulations! Queens have no secrets for you.
    queenIntro: string; // The most powerful chess piece enters. Her majesty the queen!
    queenOverBishop: string; // Take the piece
    register: string; // Register
    resetMyProgress: string; // Reset my progress
    retry: string; // Retry
    rightOn: string; // Right on!
    rookComplete: string; // Congratulations! You have successfully mastered the rook.
    rookGoal: string; // Click on the rook
    rookIntro: string; // The rook is a powerful piece. Are you ready to command it?
    selectThePieceYouWant: string; // Select the piece you want!
    stageX: I18nFormat; // Stage %s
    stageXComplete: I18nFormat; // Stage %s complete
    stalemate: string; // Stalemate
    stalemateComplete: string; // Congratulations! Better be stalemated than checkmated!
    stalemateGoal: string; // To stalemate black:
    stalemateIntro: string; // When a player is not in check and does not have a legal move, it's a stalemate. The game is drawn: no one wins, no one loses.
    takeAllThePawnsEnPassant: string; // Take all the pawns en passant!
    takeTheBlackPieces: string; // Take the black pieces!
    takeTheBlackPiecesAndDontLoseYours: string; // Take the black pieces!
    takeTheEnemyPieces: string; // Take the enemy pieces
    takeThePieceWithTheHighestValue: string; // Take the piece
    testYourSkillsWithTheComputer: string; // Test your skills with the computer
    theBishop: string; // The bishop
    theFewerMoves: string; // The fewer moves you make,
    theGameIsADraw: string; // The game is a draw
    theKing: string; // The king
    theKingCannotEscapeButBlock: string; // The king cannot escape,
    theKingIsSlow: string; // The king is slow.
    theKnight: string; // The knight
    theKnightIsInTheWay: string; // The knight is in the way!
    theMostImportantPiece: string; // The most important piece
    thenPlaceTheKnights: string; // Then place the knights!
    thePawn: string; // The pawn
    theQueen: string; // The queen
    theRook: string; // The rook
    theSpecialKingMove: string; // The special king move
    theSpecialPawnMove: string; // The special pawn move
    thisIsTheInitialPosition: string; // This is the initial position
    thisKnightIsCheckingThroughYourDefenses: string; // This knight is checking
    twoMovesToGiveCheck: string; // Two moves to give a check
    useAllThePawns: string; // Use all the pawns!
    useTwoRooks: string; // Use two rooks
    videos: string; // Videos
    watchInstructiveChessVideos: string; // Watch instructive chess videos
    wayToGo: string; // Way to go!
    whatNext: string; // What next?
    yesYesYes: string; // Yes, yes, yes!
    youCanGetOutOfCheckByTaking: string; // You can get out of check
    youCannotCastleIfAttacked: string; // You cannot castle if
    youCannotCastleIfMoved: string; // You cannot castle if
    youKnowHowToPlayChess: string; // You know how to play chess, congratulations! Do you want to become a stronger player?
    youNeedBothBishops: string; // One light-squared bishop,
    youreGoodAtThis: string; // You're good at this!
    yourPawnReachedTheEndOfTheBoard: string; // Your pawn reached the end of the board!
    youWillLoseAllYourProgress: string; // You will lose all your progress!
  };
  oauthScope: {
    alreadyHavePlayedGames: string; // You already have played games!
    apiAccessTokens: string; // API access tokens
    apiDocumentation: string; // API documentation
    apiDocumentationLinks: I18nFormat; // Here's a %1$s and the %2$s.
    attentionOfDevelopers: string; // Note for the attention of developers only:
    authorizationCodeFlow: string; // authorization code flow
    boardPlay: string; // Play games with board API
    botPlay: string; // Play games with the bot API
    canMakeOauthRequests: I18nFormat; // You can make OAuth requests without going through the %s.
    carefullySelect: string; // Carefully select what it is allowed to do on your behalf.
    challengeBulk: string; // Create many games at once for other players
    challengeRead: string; // Read incoming challenges
    challengeWrite: string; // Send, accept and reject challenges
    copyTokenNow: string; // Make sure to copy your new personal access token now. You won’t be able to see it again!
    created: I18nFormat; // Created %s
    doNotShareIt: string; // The token will grant access to your account. Do NOT share it with anyone!
    emailRead: string; // Read email address
    engineRead: string; // View and use your external engines
    engineWrite: string; // Create and update external engines
    followRead: string; // Read followed players
    followWrite: string; // Follow and unfollow other players
    forExample: I18nFormat; // For example: %s
    generatePersonalToken: string; // generate a personal access token
    givingPrefilledUrls: string; // Giving these pre-filled URLs to your users will help them get the right token scopes.
    guardTokensCarefully: string; // Guard these tokens carefully! They are like passwords. The advantage to using tokens over putting your password into a script is that tokens can be revoked, and you can generate lots of them.
    insteadGenerateToken: I18nFormat; // Instead, %s that you can directly use in API requests.
    lastUsed: I18nFormat; // Last used %s
    msgWrite: string; // Send private messages to other players
    newAccessToken: string; // New personal API access token
    newToken: string; // New access token
    personalAccessTokens: string; // Personal API access tokens
    personalTokenAppExample: string; // personal token app example
    possibleToPrefill: string; // It is possible to pre-fill this form by tweaking the query parameters of the URL.
    preferenceRead: string; // Read preferences
    preferenceWrite: string; // Write preference
    puzzleRead: string; // Read puzzle activity
    racerWrite: string; // Create and join puzzle races
    rememberTokenUse: string; // So you remember what this token is for
    scopesCanBeFound: string; // The scope codes can be found in the HTML code of the form.
    studyRead: string; // Read private studies and broadcasts
    studyWrite: string; // Create, update, delete studies and broadcasts
    teamLead: string; // Manage teams you lead: send PMs, kick members
    teamRead: string; // Read private team information
    teamWrite: string; // Join and leave teams
    ticksTheScopes: I18nFormat; // ticks the %1$s and %2$s scopes, and sets the token description.
    tokenDescription: string; // Token description
    tokenGrantsPermission: string; // A token grants other people permission to use your account.
    tournamentWrite: string; // Create, update, and join tournaments
    webLogin: string; // Create authenticated website sessions (grants full access!)
    webMod: string; // Use moderator tools (within bounds of your permission)
    whatTheTokenCanDo: string; // What the token can do on your behalf:
  };
  onboarding: {
    configureLichess: string; // Configure Lichess to your liking.
    enabledKidModeSuggestion: I18nFormat; // Will a child use this account? You might want to enable %s.
    exploreTheSiteAndHaveFun: string; // Explore the site and have fun :)
    followYourFriendsOnLichess: string; // Follow your friends on Lichess.
    improveWithChessTacticsPuzzles: string; // Improve with chess tactics puzzles.
    learnChessRules: string; // Learn chess rules
    learnFromXAndY: I18nFormat; // Learn from %1$s and %2$s.
    playInTournaments: string; // Play in tournaments.
    playOpponentsFromAroundTheWorld: string; // Play opponents from around the world.
    playTheArtificialIntelligence: string; // Play the Artificial Intelligence.
    thisIsYourProfilePage: string; // This is your profile page.
    welcome: string; // Welcome!
    welcomeToLichess: string; // Welcome to lichess.org!
    whatNowSuggestions: string; // What now? Here are a few suggestions:
  };
  patron: {
    actOfCreation: string; // Yes, here's the act of creation (in French)
    amount: string; // Amount
    bankTransfers: string; // We also accept bank transfers
    becomePatron: string; // Become a Lichess Patron
    cancelSupport: string; // Cancel your support
    celebratedPatrons: string; // The celebrated Patrons who make Lichess possible
    changeCurrency: string; // Change currency
    changeMonthlyAmount: I18nFormat; // Change the monthly amount (%s)
    changeMonthlySupport: string; // Can I change/cancel my monthly support?
    changeOrContact: I18nFormat; // Yes, at any time, from this page.
    checkOutProfile: string; // Check out your profile page!
    contactSupport: string; // contact Lichess support
    costBreakdown: string; // See the detailed cost breakdown
    currentStatus: string; // Current status
    date: string; // Date
    decideHowMuch: string; // Decide what Lichess is worth to you:
    donate: string; // Donate
    donateAsX: I18nFormat; // Donate as %s
    downgradeNextMonth: string; // In one month, you will NOT be charged again, and your Lichess account will revert to a regular account.
    featuresComparison: string; // See the detailed feature comparison
    freeAccount: string; // Free account
    freeChess: string; // Free chess for everyone, forever!
    giftPatronWings: string; // Gift Patron wings to a player
    giftPatronWingsShort: string; // Gift Patron wings
    ifNotRenewedThenAccountWillRevert: string; // If not renewed, your account will then revert to a regular account.
    lichessIsRegisteredWith: I18nFormat; // Lichess is registered with %s.
    lichessPatron: string; // Lichess Patron
    lifetime: string; // Lifetime
    lifetimePatron: string; // Lifetime Lichess Patron
    logInToDonate: string; // Log in to donate
    makeAdditionalDonation: string; // Make an additional donation
    monthly: string; // Monthly
    newPatrons: string; // New Patrons
    nextPayment: string; // Next payment
    noAdsNoSubs: string; // No ads, no subscriptions; but open-source and passion.
    noLongerSupport: string; // No longer support Lichess
    noPatronFeatures: string; // No, because Lichess is entirely free, forever, and for everyone. That's a promise.
    nowLifetime: string; // You are now a lifetime Lichess Patron!
    nowOneMonth: string; // You are now a Lichess Patron for one month!
    officialNonProfit: string; // Is Lichess an official non-profit?
    onetime: string; // One-time
    onlyDonationFromAbove: string; // Please note that only the donation form above will grant the Patron status.
    otherAmount: string; // Other
    otherMethods: string; // Other methods of donation?
    patronFeatures: string; // Are some features reserved to Patrons?
    patronForMonths: I18nPlural; // Lichess Patron for %s months
    patronUntil: I18nFormat; // You have a Patron account until %s.
    payLifetimeOnce: I18nFormat; // Pay %s once. Be a Lichess Patron forever!
    paymentDetails: string; // Payment details
    permanentPatron: string; // You now have a permanent Patron account.
    pleaseEnterAmountInX: I18nFormat; // Please enter an amount in %s
    recurringBilling: string; // Recurring billing, renewing your Patron wings every month.
    serversAndDeveloper: I18nFormat; // First of all, powerful servers.
    singleDonation: string; // A single donation that grants you the Patron wings for one month.
    stopPayments: string; // Withdraw your credit card and stop payments:
    stopPaymentsPayPal: string; // Cancel PayPal subscription and stop payments:
    stripeManageSub: string; // Manage your subscription and download your invoices and receipts
    thankYou: string; // Thank you for your donation!
    transactionCompleted: string; // Your transaction has been completed, and a receipt for your donation has been emailed to you.
    tyvm: string; // Thank you very much for your help. You rock!
    update: string; // Update
    updatePaymentMethod: string; // Update payment method
    viewOthers: string; // View other Lichess Patrons
    weAreNonProfit: string; // We are a non‑profit association because we believe everyone should have access to a free, world-class chess platform.
    weAreSmallTeam: string; // We are a small team, so your support makes a huge difference!
    weRelyOnSupport: string; // We rely on support from people like you to make it possible. If you enjoy using Lichess, please consider supporting us by donating and becoming a Patron!
    whereMoneyGoes: string; // Where does the money go?
    withCreditCard: string; // Credit Card
    xBecamePatron: I18nFormat; // %s became a Lichess Patron
    xIsPatronForNbMonths: I18nPlural; // %1$s is a Lichess Patron for %2$s months
    xOrY: I18nFormat; // %1$s or %2$s
    youHaveLifetime: string; // You have a Lifetime Patron account. That's pretty awesome!
    youSupportWith: I18nFormat; // You support lichess.org with %s per month.
    youWillBeChargedXOnY: I18nFormat; // You will be charged %1$s on %2$s.
  };
  perfStat: {
    averageOpponent: string; // Average opponent
    berserkedGames: string; // Berserked games
    bestRated: string; // Best rated victories
    currentStreak: I18nFormat; // Current streak: %s
    defeats: string; // Defeats
    disconnections: string; // Disconnections
    fromXToY: I18nFormat; // from %1$s to %2$s
    gamesInARow: string; // Games played in a row
    highestRating: I18nFormat; // Highest rating: %s
    lessThanOneHour: string; // Less than one hour between games
    longestStreak: I18nFormat; // Longest streak: %s
    losingStreak: string; // Losing streak
    lowestRating: I18nFormat; // Lowest rating: %s
    maxTimePlaying: string; // Max time spent playing
    notEnoughGames: string; // Not enough games played
    notEnoughRatedGames: string; // Not enough rated games have been played to establish a reliable rating.
    now: string; // now
    perfStats: I18nFormat; // %s stats
    progressOverLastXGames: I18nFormat; // Progression over the last %s games:
    provisional: string; // provisional
    ratedGames: string; // Rated games
    ratingDeviation: I18nFormat; // Rating deviation: %s.
    ratingDeviationTooltip: I18nFormat; // Lower value means the rating is more stable. Above %1$s, the rating is considered provisional. To be included in the rankings, this value should be below %2$s (standard chess) or %3$s (variants).
    timeSpentPlaying: string; // Time spent playing
    totalGames: string; // Total games
    tournamentGames: string; // Tournament games
    victories: string; // Victories
    viewTheGames: string; // View the games
    winningStreak: string; // Winning streak
  };
  preferences: {
    bellNotificationSound: string; // Bell notification sound
    boardCoordinates: string; // Board coordinates (A-H, 1-8)
    boardHighlights: string; // Board highlights (last move and check)
    bothClicksAndDrag: string; // Either
    castleByMovingOntoTheRook: string; // Move king onto rook
    castleByMovingTheKingTwoSquaresOrOntoTheRook: string; // Castling method
    castleByMovingTwoSquares: string; // Move king two squares
    chessClock: string; // Chess clock
    chessPieceSymbol: string; // Chess piece symbol
    claimDrawOnThreefoldRepetitionAutomatically: string; // Claim draw on threefold repetition automatically
    clickTwoSquares: string; // Click two squares
    confirmResignationAndDrawOffers: string; // Confirm resignation and draw offers
    correspondenceAndUnlimited: string; // Correspondence and unlimited
    correspondenceEmailNotification: string; // Daily email listing your correspondence games
    display: string; // Display
    displayBoardResizeHandle: string; // Show board resize handle
    dragPiece: string; // Drag a piece
    explainCanThenBeTemporarilyDisabled: string; // Can be disabled during a game with the board menu
    explainPromoteToQueenAutomatically: string; // Hold the <ctrl> key while promoting to temporarily disable auto-promotion
    explainShowPlayerRatings: string; // This hides all ratings from Lichess, to help focus on the chess. Rated games still impact your rating, this is only about what you get to see.
    gameBehavior: string; // Game behaviour
    giveMoreTime: string; // Give more time
    horizontalGreenProgressBars: string; // Horizontal green progress bars
    howDoYouMovePieces: string; // How do you move pieces?
    inCasualGamesOnly: string; // In casual games only
    inCorrespondenceGames: string; // Correspondence games
    inGameOnly: string; // In-game only
    inputMovesWithTheKeyboard: string; // Input moves with the keyboard
    inputMovesWithVoice: string; // Input moves with your voice
    materialDifference: string; // Material difference
    moveConfirmation: string; // Move confirmation
    moveListWhilePlaying: string; // Move list while playing
    notifications: string; // Notifications
    notifyBell: string; // Bell notification within Lichess
    notifyChallenge: string; // Challenges
    notifyDevice: string; // Device
    notifyForumMention: string; // Forum comment mentions you
    notifyGameEvent: string; // Correspondence game updates
    notifyInboxMsg: string; // New inbox message
    notifyInvitedStudy: string; // Study invite
    notifyPush: string; // Device notification when you're not on Lichess
    notifyStreamStart: string; // Streamer goes live
    notifyTimeAlarm: string; // Correspondence clock running out
    notifyTournamentSoon: string; // Tournament starting soon
    notifyWeb: string; // Browser
    onlyOnInitialPosition: string; // Only on initial position
    pgnLetter: string; // Letter (K, Q, R, B, N)
    pgnPieceNotation: string; // Move notation
    pieceAnimation: string; // Piece animation
    pieceDestinations: string; // Piece destinations (valid moves and premoves)
    preferences: string; // Preferences
    premovesPlayingDuringOpponentTurn: string; // Premoves (playing during opponent turn)
    privacy: string; // Privacy
    promoteToQueenAutomatically: string; // Promote to Queen automatically
    sayGgWpAfterLosingOrDrawing: string; // Say "Good game, well played" upon defeat or draw
    scrollOnTheBoardToReplayMoves: string; // Scroll on the board to replay moves
    showFlairs: string; // Show player flairs
    showPlayerRatings: string; // Show player ratings
    snapArrowsToValidMoves: string; // Snap arrows to valid moves
    soundWhenTimeGetsCritical: string; // Sound when time gets critical
    takebacksWithOpponentApproval: string; // Takebacks (with opponent approval)
    tenthsOfSeconds: string; // Tenths of seconds
    whenPremoving: string; // When premoving
    whenTimeRemainingLessThanTenSeconds: string; // When time remaining < 10 seconds
    whenTimeRemainingLessThanThirtySeconds: string; // When time remaining < 30 seconds
    yourPreferencesHaveBeenSaved: string; // Your preferences have been saved.
    zenMode: string; // Zen mode
  };
  puzzle: {
    addAnotherTheme: string; // Add another theme
    advanced: string; // Advanced
    bestMove: string; // Best move!
    byOpenings: string; // By openings
    clickToSolve: string; // Click to solve
    continueTheStreak: string; // Continue the streak
    continueTraining: string; // Continue training
    dailyPuzzle: string; // Daily Puzzle
    didYouLikeThisPuzzle: string; // Did you like this puzzle?
    difficultyLevel: string; // Difficulty level
    downVote: string; // Down vote puzzle
    easier: string; // Easier
    easiest: string; // Easiest
    example: string; // Example
    failed: string; // incorrect
    findTheBestMoveForBlack: string; // Find the best move for black.
    findTheBestMoveForWhite: string; // Find the best move for white.
    fromGameLink: I18nFormat; // From game %s
    fromMyGames: string; // From my games
    fromMyGamesNone: string; // You have no puzzles in the database, but Lichess still loves you very much.
    fromXGames: I18nFormat; // Puzzles from %s' games
    fromXGamesFound: I18nFormat; // %1$s puzzles found in %2$s games
    goals: string; // Goals
    goodMove: string; // Good move
    harder: string; // Harder
    hardest: string; // Hardest
    hidden: string; // hidden
    history: string; // Puzzle history
    improvementAreas: string; // Improvement areas
    improvementAreasDescription: string; // Train these to optimize your progress!
    jumpToNextPuzzleImmediately: string; // Jump to next puzzle immediately
    keepGoing: string; // Keep going…
    lengths: string; // Lengths
    lookupOfPlayer: string; // Lookup puzzles from a player's games
    mates: string; // Mates
    motifs: string; // Motifs
    nbPlayed: I18nPlural; // %s played
    nbPointsAboveYourPuzzleRating: I18nPlural; // %s points above your puzzle rating
    nbPointsBelowYourPuzzleRating: I18nPlural; // %s points below your puzzle rating
    nbToReplay: I18nPlural; // %s to replay
    newStreak: string; // New streak
    nextPuzzle: string; // Next puzzle
    noPuzzlesToShow: string; // Nothing to show, go play some puzzles first!
    normal: string; // Normal
    notTheMove: string; // That's not the move!
    openingsYouPlayedTheMost: string; // Openings you played the most in rated games
    origin: string; // Origin
    percentSolved: I18nFormat; // %s solved
    phases: string; // Phases
    playedXTimes: I18nPlural; // Played %s times
    puzzleComplete: string; // Puzzle complete!
    puzzleDashboard: string; // Puzzle Dashboard
    puzzleDashboardDescription: string; // Train, analyse, improve
    puzzleId: I18nFormat; // Puzzle %s
    puzzleOfTheDay: string; // Puzzle of the day
    puzzles: string; // Puzzles
    puzzlesByOpenings: string; // Puzzles by openings
    puzzleSuccess: string; // Success!
    puzzleThemes: string; // Puzzle Themes
    ratingX: I18nFormat; // Rating: %s
    recommended: string; // Recommended
    searchPuzzles: string; // Search puzzles
    solved: string; // solved
    specialMoves: string; // Special moves
    streakDescription: string; // Solve progressively harder puzzles and build a win streak. There is no clock, so take your time. One wrong move, and it's game over! But you can skip one move per session.
    streakSkipExplanation: string; // Skip this move to preserve your streak! Only works once per run.
    strengthDescription: string; // You perform the best in these themes
    strengths: string; // Strengths
    toGetPersonalizedPuzzles: string; // To get personalized puzzles:
    trySomethingElse: string; // Try something else.
    upVote: string; // Up vote puzzle
    useCtrlF: string; // Use Ctrl+f to find your favourite opening!
    useFindInPage: string; // Use "Find in page" in the browser menu to find your favourite opening!
    voteToLoadNextOne: string; // Vote to load the next one!
    yourPuzzleRatingWillNotChange: string; // Your puzzle rating will not change. Note that puzzles are not a competition. Your rating helps selecting the best puzzles for your current skill.
    yourStreakX: I18nFormat; // Your streak: %s
  };
  puzzleTheme: {
    advancedPawn: string; // Advanced pawn
    advancedPawnDescription: string; // One of your pawns is deep into the opponent position, maybe threatening to promote.
    advantage: string; // Advantage
    advantageDescription: string; // Seize your chance to get a decisive advantage. (200cp ≤ eval ≤ 600cp)
    anastasiaMate: string; // Anastasia's mate
    anastasiaMateDescription: string; // A knight and rook or queen team up to trap the opposing king between the side of the board and a friendly piece.
    arabianMate: string; // Arabian mate
    arabianMateDescription: string; // A knight and a rook team up to trap the opposing king on a corner of the board.
    attackingF2F7: string; // Attacking f2 or f7
    attackingF2F7Description: string; // An attack focusing on the f2 or f7 pawn, such as in the fried liver opening.
    attraction: string; // Attraction
    attractionDescription: string; // An exchange or sacrifice encouraging or forcing an opponent piece to a square that allows a follow-up tactic.
    backRankMate: string; // Back rank mate
    backRankMateDescription: string; // Checkmate the king on the home rank, when it is trapped there by its own pieces.
    bishopEndgame: string; // Bishop endgame
    bishopEndgameDescription: string; // An endgame with only bishops and pawns.
    bodenMate: string; // Boden's mate
    bodenMateDescription: string; // Two attacking bishops on criss-crossing diagonals deliver mate to a king obstructed by friendly pieces.
    capturingDefender: string; // Capture the defender
    capturingDefenderDescription: string; // Removing a piece that is critical to defence of another piece, allowing the now undefended piece to be captured on a following move.
    castling: string; // Castling
    castlingDescription: string; // Bring the king to safety, and deploy the rook for attack.
    clearance: string; // Clearance
    clearanceDescription: string; // A move, often with tempo, that clears a square, file or diagonal for a follow-up tactical idea.
    crushing: string; // Crushing
    crushingDescription: string; // Spot the opponent blunder to obtain a crushing advantage. (eval ≥ 600cp)
    defensiveMove: string; // Defensive move
    defensiveMoveDescription: string; // A precise move or sequence of moves that is needed to avoid losing material or another advantage.
    deflection: string; // Deflection
    deflectionDescription: string; // A move that distracts an opponent piece from another duty that it performs, such as guarding a key square. Sometimes also called "overloading".
    discoveredAttack: string; // Discovered attack
    discoveredAttackDescription: string; // Moving a piece (such as a knight), that previously blocked an attack by a long range piece (such as a rook), out of the way of that piece.
    doubleBishopMate: string; // Double bishop mate
    doubleBishopMateDescription: string; // Two attacking bishops on adjacent diagonals deliver mate to a king obstructed by friendly pieces.
    doubleCheck: string; // Double check
    doubleCheckDescription: string; // Checking with two pieces at once, as a result of a discovered attack where both the moving piece and the unveiled piece attack the opponent's king.
    dovetailMate: string; // Dovetail mate
    dovetailMateDescription: string; // A queen delivers mate to an adjacent king, whose only two escape squares are obstructed by friendly pieces.
    endgame: string; // Endgame
    endgameDescription: string; // A tactic during the last phase of the game.
    enPassantDescription: string; // A tactic involving the en passant rule, where a pawn can capture an opponent pawn that has bypassed it using its initial two-square move.
    equality: string; // Equality
    equalityDescription: string; // Come back from a losing position, and secure a draw or a balanced position. (eval ≤ 200cp)
    exposedKing: string; // Exposed king
    exposedKingDescription: string; // A tactic involving a king with few defenders around it, often leading to checkmate.
    fork: string; // Fork
    forkDescription: string; // A move where the moved piece attacks two opponent pieces at once.
    hangingPiece: string; // Hanging piece
    hangingPieceDescription: string; // A tactic involving an opponent piece being undefended or insufficiently defended and free to capture.
    healthyMix: string; // Healthy mix
    healthyMixDescription: string; // A bit of everything. You don't know what to expect, so you remain ready for anything! Just like in real games.
    hookMate: string; // Hook mate
    hookMateDescription: string; // Checkmate with a rook, knight, and pawn along with one enemy pawn to limit the enemy king's escape.
    interference: string; // Interference
    interferenceDescription: string; // Moving a piece between two opponent pieces to leave one or both opponent pieces undefended, such as a knight on a defended square between two rooks.
    intermezzo: string; // Intermezzo
    intermezzoDescription: string; // Instead of playing the expected move, first interpose another move posing an immediate threat that the opponent must answer. Also known as "Zwischenzug" or "In between".
    kingsideAttack: string; // Kingside attack
    kingsideAttackDescription: string; // An attack of the opponent's king, after they castled on the king side.
    knightEndgame: string; // Knight endgame
    knightEndgameDescription: string; // An endgame with only knights and pawns.
    long: string; // Long puzzle
    longDescription: string; // Three moves to win.
    master: string; // Master games
    masterDescription: string; // Puzzles from games played by titled players.
    masterVsMaster: string; // Master vs Master games
    masterVsMasterDescription: string; // Puzzles from games between two titled players.
    mate: string; // Checkmate
    mateDescription: string; // Win the game with style.
    mateIn1: string; // Mate in 1
    mateIn1Description: string; // Deliver checkmate in one move.
    mateIn2: string; // Mate in 2
    mateIn2Description: string; // Deliver checkmate in two moves.
    mateIn3: string; // Mate in 3
    mateIn3Description: string; // Deliver checkmate in three moves.
    mateIn4: string; // Mate in 4
    mateIn4Description: string; // Deliver checkmate in four moves.
    mateIn5: string; // Mate in 5 or more
    mateIn5Description: string; // Figure out a long mating sequence.
    middlegame: string; // Middlegame
    middlegameDescription: string; // A tactic during the second phase of the game.
    oneMove: string; // One-move puzzle
    oneMoveDescription: string; // A puzzle that is only one move long.
    opening: string; // Opening
    openingDescription: string; // A tactic during the first phase of the game.
    pawnEndgame: string; // Pawn endgame
    pawnEndgameDescription: string; // An endgame with only pawns.
    pin: string; // Pin
    pinDescription: string; // A tactic involving pins, where a piece is unable to move without revealing an attack on a higher value piece.
    playerGames: string; // Player games
    playerGamesDescription: string; // Lookup puzzles generated from your games, or from another player's games.
    promotion: string; // Promotion
    promotionDescription: string; // Promote one of your pawn to a queen or minor piece.
    puzzleDownloadInformation: I18nFormat; // These puzzles are in the public domain, and can be downloaded from %s.
    queenEndgame: string; // Queen endgame
    queenEndgameDescription: string; // An endgame with only queens and pawns.
    queenRookEndgame: string; // Queen and Rook
    queenRookEndgameDescription: string; // An endgame with only queens, rooks and pawns.
    queensideAttack: string; // Queenside attack
    queensideAttackDescription: string; // An attack of the opponent's king, after they castled on the queen side.
    quietMove: string; // Quiet move
    quietMoveDescription: string; // A move that does neither make a check or capture, nor an immediate threat to capture, but does prepare a more hidden unavoidable threat for a later move.
    rookEndgame: string; // Rook endgame
    rookEndgameDescription: string; // An endgame with only rooks and pawns.
    sacrifice: string; // Sacrifice
    sacrificeDescription: string; // A tactic involving giving up material in the short-term, to gain an advantage again after a forced sequence of moves.
    short: string; // Short puzzle
    shortDescription: string; // Two moves to win.
    skewer: string; // Skewer
    skewerDescription: string; // A motif involving a high value piece being attacked, moving out the way, and allowing a lower value piece behind it to be captured or attacked, the inverse of a pin.
    smotheredMate: string; // Smothered mate
    smotheredMateDescription: string; // A checkmate delivered by a knight in which the mated king is unable to move because it is surrounded (or smothered) by its own pieces.
    superGM: string; // Super GM games
    superGMDescription: string; // Puzzles from games played by the best players in the world.
    trappedPiece: string; // Trapped piece
    trappedPieceDescription: string; // A piece is unable to escape capture as it has limited moves.
    underPromotion: string; // Underpromotion
    underPromotionDescription: string; // Promotion to a knight, bishop, or rook.
    veryLong: string; // Very long puzzle
    veryLongDescription: string; // Four moves or more to win.
    xRayAttack: string; // X-Ray attack
    xRayAttackDescription: string; // A piece attacks or defends a square, through an enemy piece.
    zugzwang: string; // Zugzwang
    zugzwangDescription: string; // The opponent is limited in the moves they can make, and all moves worsen their position.
  };
  search: {
    advancedSearch: string; // Advanced search
    aiLevel: string; // A.I. level
    analysis: string; // Analysis
    ascending: string; // Ascending
    color: string; // Colour
    date: string; // Date
    descending: string; // Descending
    evaluation: string; // Evaluation
    from: string; // From
    gamesFound: I18nPlural; // %s games found
    humanOrComputer: string; // Whether the player's opponent was human or a computer
    include: string; // Include
    loser: string; // Loser
    maxNumber: string; // Maximum number
    maxNumberExplanation: string; // The maximum number of games to return
    nbTurns: string; // Number of turns
    onlyAnalysed: string; // Only games where a computer analysis is available
    opponentName: string; // Opponent name
    ratingExplanation: string; // The average rating of both players
    result: string; // Result
    search: string; // Search
    searchInXGames: I18nPlural; // Search in %s chess games
    sortBy: string; // Sort by
    source: string; // Source
    to: string; // To
    winnerColor: string; // Winner colour
    xGamesFound: I18nPlural; // %s games found
  };
  settings: {
    cantOpenSimilarAccount: string; // You will not be allowed to open a new account with the same name, even if the case is different.
    changedMindDoNotCloseAccount: string; // I changed my mind, don't close my account
    closeAccount: string; // Close account
    closeAccountExplanation: string; // Are you sure you want to close your account? Closing your account is a permanent decision. You will NEVER be able to log in EVER AGAIN.
    closingIsDefinitive: string; // Closing is definitive. There is no going back. Are you sure?
    managedAccountCannotBeClosed: string; // Your account is managed, and cannot be closed.
    settings: string; // Settings
    thisAccountIsClosed: string; // This account is closed.
  };
  site: {
    abortGame: string; // Abort game
    abortTheGame: string; // Abort the game
    about: string; // About
    aboutSimul: string; // Simuls involve a single player facing several players at once.
    aboutSimulImage: string; // Out of 50 opponents, Fischer won 47 games, drew 2 and lost 1.
    aboutSimulRealLife: string; // The concept is taken from real world events. In real life, this involves the simul host moving from table to table to play a single move.
    aboutSimulRules: string; // When the simul starts, every player starts a game with the host. The simul ends when all games are complete.
    aboutSimulSettings: string; // Simuls are always casual. Rematches, takebacks and adding time are disabled.
    aboutX: I18nFormat; // About %s
    accept: string; // Accept
    accountCanLogin: I18nFormat; // You can login right now as %s.
    accountClosed: I18nFormat; // The account %s is closed.
    accountConfirmationEmailNotNeeded: string; // You do not need a confirmation email.
    accountConfirmed: I18nFormat; // The user %s is successfully confirmed.
    accountRegisteredWithoutEmail: I18nFormat; // The account %s was registered without an email.
    accuracy: string; // Accuracy
    activePlayers: string; // Active players
    addCurrentVariation: string; // Add current variation
    advancedSettings: string; // Advanced settings
    advantage: string; // Advantage
    agreementAssistance: string; // I agree that I will at no time receive assistance during my games (from a chess computer, book, database or another person).
    agreementMultipleAccounts: I18nFormat; // I agree that I will not create multiple accounts (except for the reasons stated in the %s).
    agreementNice: string; // I agree that I will always be respectful to other players.
    agreementPolicy: string; // I agree that I will follow all Lichess policies.
    aiNameLevelAiLevel: I18nFormat; // %1$s level %2$s
    allInformationIsPublicAndOptional: string; // All information is public and optional.
    allSet: string; // All set!
    allSquaresOfTheBoard: string; // All squares of the board
    always: string; // Always
    analysis: string; // Analysis board
    analysisOptions: string; // Analysis options
    analysisShapesHowTo: string; // Press shift+click or right-click to draw circles and arrows on the board.
    andSaveNbPremoveLines: I18nPlural; // and save %s premove lines
    anonymous: string; // Anonymous
    anotherWasX: I18nFormat; // Another was %s
    apply: string; // Submit
    asBlack: string; // as black
    asFreeAsLichess: string; // As free as Lichess
    askYourChessTeacherAboutLiftingKidMode: string; // Your account is managed. Ask your chess teacher about lifting kid mode.
    asWhite: string; // as white
    automaticallyProceedToNextGameAfterMoving: string; // Automatically proceed to next game after moving
    autoSwitch: string; // Auto switch
    availableInNbLanguages: I18nPlural; // Available in %s languages!
    averageCentipawnLoss: string; // Average centipawn loss
    averageElo: string; // Average rating
    averageOpponent: string; // Average opponent
    averageRatingX: I18nFormat; // Average rating: %s
    background: string; // Background
    backgroundImageUrl: string; // Background image URL:
    backToGame: string; // Back to game
    backToTournament: string; // Back to tournament
    berserkRate: string; // Berserk rate
    bestMoveArrow: string; // Best move arrow
    bestWasX: I18nFormat; // Best was %s
    betterThanPercentPlayers: I18nFormat; // Better than %1$s of %2$s players
    bewareTheGameIsRatedButHasNoClock: string; // Beware, the game is rated but has no clock!
    biography: string; // Biography
    biographyDescription: string; // Talk about yourself, your interests, what you like in chess, your favourite openings, players, ...
    black: string; // Black
    blackCastlingKingside: string; // Black O-O
    blackCheckmatesInOneMove: string; // Black to checkmate in one move
    blackDeclinesDraw: string; // Black declines draw
    blackDidntMove: string; // Black didn't move
    blackIsVictorious: string; // Black is victorious
    blackLeftTheGame: string; // Black left the game
    blackOffersDraw: string; // Black offers draw
    blackPlays: string; // Black to play
    blackResigned: string; // Black resigned
    blackTimeOut: string; // Black time out
    blackWins: string; // Black wins
    blackWinsGame: string; // Black wins
    blankedPassword: string; // You have used the same password on another site, and that site has been compromised. To ensure the safety of your Lichess account, we need you to set a new password. Thank you for your understanding.
    blitz: string; // Blitz
    blitzDesc: string; // Fast games: 3 to 8 minutes
    block: string; // Block
    blocked: string; // Blocked
    blocks: I18nPlural; // %s blocks
    blog: string; // Blog
    blunder: string; // Blunder
    board: string; // Board
    boardEditor: string; // Board editor
    boardReset: string; // Reset colours to default
    bookmarkThisGame: string; // Bookmark this game
    brightness: string; // Brightness
    builtForTheLoveOfChessNotMoney: string; // Built for the love of chess, not money
    bullet: string; // Bullet
    bulletBlitzClassical: string; // Bullet, blitz, classical
    bulletDesc: string; // Very fast games: less than 3 minutes
    by: I18nFormat; // by %s
    byCPL: string; // By CPL
    byRegisteringYouAgreeToBeBoundByOur: I18nFormat; // By registering, you agree to the %s.
    calculatingMoves: string; // Calculating moves...
    cancel: string; // Cancel
    cancelRematchOffer: string; // Cancel rematch offer
    cancelSimul: string; // Cancel the simul
    cancelTournament: string; // Cancel the tournament
    cantDoThisTwice: string; // If you close your account a second time, there will be no way of recovering it.
    'captcha.fail': string; // Please solve the chess captcha.
    capture: string; // Capture
    castling: string; // Castling
    casual: string; // Casual
    casualTournament: string; // Casual
    changeEmail: string; // Change email
    changePassword: string; // Change password
    changeUsername: string; // Change username
    changeUsernameDescription: string; // Change your username. This can only be done once and you are only allowed to change the case of the letters in your username.
    changeUsernameNotSame: string; // Only the case of the letters can change. For example "johndoe" to "JohnDoe".
    chat: string; // Chat
    chatRoom: string; // Chat room
    cheat: string; // Cheat
    cheatDetected: string; // Cheat Detected
    checkmate: string; // Checkmate
    checkSpamFolder: string; // Also check your spam folder, it might end up there. If so, mark it as not spam.
    checkYourEmail: string; // Check your Email
    chess960StartPosition: I18nFormat; // Chess960 start position: %s
    chessBasics: string; // Chess basics
    claimADraw: string; // Claim a draw
    classical: string; // Classical
    classicalDesc: string; // Classical games: 25 minutes and more
    clearBoard: string; // Clear board
    clearSavedMoves: string; // Clear moves
    clickHereToReadIt: string; // Click here to read it
    clickOnTheBoardToMakeYourMove: string; // Click on the board to make your move, and prove you are human.
    clickToRevealEmailAddress: string; // [Click to reveal email address]
    clock: string; // Clock
    clockIncrement: string; // Clock increment
    clockInitialTime: string; // Clock initial time
    close: string; // Close
    closedAccountChangedMind: string; // If you closed your account, but have since changed your mind, you get one chance of getting your account back.
    closingAccountWithdrawAppeal: string; // Closing your account will withdraw your appeal
    cloudAnalysis: string; // Cloud analysis
    coaches: string; // Coaches
    coachManager: string; // Coach manager
    collapseVariations: string; // Collapse variations
    community: string; // Community
    composeMessage: string; // Compose message
    computer: string; // Computer
    computerAnalysis: string; // Computer analysis
    computerAnalysisAvailable: string; // Computer analysis available
    computerAnalysisDisabled: string; // Computer analysis disabled
    computersAreNotAllowedToPlay: string; // Computers and computer-assisted players are not allowed to play. Please do not get assistance from chess engines, databases, or from other players while playing. Also note that making multiple accounts is strongly discouraged and excessive multi-accounting will lead to being banned.
    computerThinking: string; // Computer thinking ...
    conditionalPremoves: string; // Conditional premoves
    conditionOfEntry: string; // Entry requirements:
    confirmMove: string; // Confirm move
    congratsYouWon: string; // Congratulations, you won!
    continueFromHere: string; // Continue from here
    contribute: string; // Contribute
    copyTextToEmail: I18nFormat; // Copy and paste the above text and send it to %s
    copyVariationPgn: string; // Copy variation PGN
    correspondence: string; // Correspondence
    correspondenceChess: string; // Correspondence chess
    correspondenceDesc: string; // Correspondence games: one or several days per move
    countryRegion: string; // Country or region
    cpus: string; // CPUs
    create: string; // Create
    createAGame: string; // Create a game
    createANewTopic: string; // Create a new topic
    createANewTournament: string; // Create a new tournament
    createdBy: string; // Created by
    createdSimuls: string; // Newly created simuls
    createTheTopic: string; // Create the topic
    crosstable: string; // Crosstable
    cumulative: string; // Cumulative
    currentGames: string; // Current games
    currentMatchScore: string; // Current match score
    currentPassword: string; // Current password
    custom: string; // Custom
    customPosition: string; // Custom position
    cyclePreviousOrNextVariation: string; // Cycle previous/next variation
    dark: string; // Dark
    database: string; // Database
    daysPerTurn: string; // Days per turn
    decline: string; // Decline
    defeat: string; // Defeat
    defeatVsYInZ: I18nFormat; // %1$s vs %2$s in %3$s
    delete: string; // Delete
    deleteFromHere: string; // Delete from here
    deleteThisImportedGame: string; // Delete this imported game?
    depthX: I18nFormat; // Depth %s
    descPrivate: string; // Private description
    descPrivateHelp: string; // Text that only the team members will see. If set, replaces the public description for team members.
    description: string; // Description
    deviceTheme: string; // Device theme
    disableKidMode: string; // Disable Kid mode
    discussions: string; // Conversations
    doItAgain: string; // Do it again
    doneReviewingBlackMistakes: string; // Done reviewing black mistakes
    doneReviewingWhiteMistakes: string; // Done reviewing white mistakes
    download: string; // Download
    downloadAnnotated: string; // Download annotated
    downloadImported: string; // Download imported
    downloadRaw: string; // Download raw
    draw: string; // Draw
    drawByFiftyMoves: string; // The game has been drawn by the fifty move rule.
    drawByMutualAgreement: string; // Draw by mutual agreement
    drawn: string; // Drawn
    drawOfferAccepted: string; // Draw offer accepted
    drawOfferCanceled: string; // Draw offer cancelled
    drawOfferSent: string; // Draw offer sent
    drawRate: string; // Draw rate
    draws: string; // Draws
    drawVsYInZ: I18nFormat; // %1$s vs %2$s in %3$s
    dtzWithRounding: string; // DTZ50'' with rounding, based on number of half-moves until next capture or pawn move
    duration: string; // Duration
    edit: string; // Edit
    editProfile: string; // Edit profile
    email: string; // Email
    emailAssociatedToaccount: string; // Email address associated to the account
    emailCanTakeSomeTime: string; // It can take some time to arrive.
    emailConfirmHelp: string; // Help with email confirmation
    emailConfirmNotReceived: string; // Didn't receive your confirmation email after signing up?
    emailForSignupHelp: string; // If everything else fails, then send us this email:
    emailMeALink: string; // Email me a link
    emailSent: I18nFormat; // We have sent an email to %s.
    emailSuggestion: string; // Do not set an email address suggested by someone else. They will use it to steal your account.
    embedInYourWebsite: string; // Embed in your website
    embedsAvailable: string; // Paste a game URL or a study chapter URL to embed it.
    emptyTournamentName: string; // Leave empty to name the tournament after a notable chess player.
    enable: string; // Enable
    enableKidMode: string; // Enable Kid mode
    endgame: string; // Endgame
    endgamePositions: string; // Endgame positions
    engineFailed: string; // Error loading engine
    engineManager: string; // Engine manager
    'error.email': string; // This email address is invalid
    'error.email_acceptable': string; // This email address is not acceptable. Please double-check it, and try again.
    'error.email_different': string; // This is already your email address
    'error.email_unique': string; // Email address invalid or already taken
    'error.max': I18nFormat; // Must be at most %s
    'error.maxLength': I18nFormat; // Must be at most %s characters long
    'error.min': I18nFormat; // Must be at least %s
    'error.minLength': I18nFormat; // Must be at least %s characters long
    'error.namePassword': string; // Please don't use your username as your password.
    'error.provideOneCheatedGameLink': string; // Please provide at least one link to a cheated game.
    'error.required': string; // This field is required
    'error.unknown': string; // Invalid value
    'error.weakPassword': string; // This password is extremely common, and too easy to guess.
    estimatedStart: string; // Estimated start time
    evaluatingYourMove: string; // Evaluating your move ...
    evaluationGauge: string; // Evaluation gauge
    eventInProgress: string; // Playing now
    everybodyGetsAllFeaturesForFree: string; // Everybody gets all features for free
    expandVariations: string; // Expand variations
    exportGames: string; // Export games
    fast: string; // Fast
    favoriteOpponents: string; // Favourite opponents
    fiftyMovesWithoutProgress: string; // Fifty moves without progress
    filterGames: string; // Filter games
    findBetterMoveForBlack: string; // Find a better move for black
    findBetterMoveForWhite: string; // Find a better move for white
    finished: string; // Finished
    flair: string; // Flair
    flipBoard: string; // Flip board
    focusChat: string; // Focus chat
    follow: string; // Follow
    followAndChallengeFriends: string; // Follow and challenge friends
    following: string; // Following
    followsYou: string; // Follows you
    followX: I18nFormat; // Follow %s
    forceDraw: string; // Call draw
    forceResignation: string; // Claim victory
    forceVariation: string; // Force variation
    forgotPassword: string; // Forgot password?
    forum: string; // Forum
    freeOnlineChess: string; // Free Online Chess
    friends: string; // Friends
    fromPosition: string; // From position
    fullFeatured: string; // Full featured
    gameAborted: string; // Game aborted
    gameAnalysis: string; // Game analysis
    gameAsGIF: string; // Game as GIF
    gameInProgress: I18nFormat; // You have a game in progress with %s.
    gameOver: string; // Game Over
    games: string; // Games
    gamesPlayed: string; // Games played
    gameVsX: I18nFormat; // Game vs %1$s
    getAHint: string; // Get a hint
    giveNbSeconds: I18nPlural; // Give %s seconds
    glicko2Rating: string; // Glicko-2 rating
    goDeeper: string; // Go deeper
    goodPractice: string; // To that effect, we must ensure that all players follow good practice.
    graph: string; // Graph
    hangOn: string; // Hang on!
    help: string; // Help:
    hideBestMove: string; // Hide best move
    host: string; // Host
    hostANewSimul: string; // Host a new simul
    hostColorX: I18nFormat; // Host colour: %s
    howToAvoidThis: string; // How to avoid this?
    hue: string; // Hue
    human: string; // Human
    ifNoneLeaveEmpty: string; // If none, leave empty
    ifRatingIsPlusMinusX: I18nFormat; // If rating is ± %s
    ifRegistered: string; // If registered
    ifYouDoNotSeeTheEmailCheckOtherPlaces: string; // If you don't see the email, check other places it might be, like your junk, spam, social, or other folders.
    important: string; // Important
    importedByX: I18nFormat; // Imported by %s
    importGame: string; // Import game
    importGameCaveat: string; // Variations will be erased. To keep them, import the PGN via a study.
    importGameDataPrivacyWarning: string; // This PGN can be accessed by the public. To import a game privately, use a study.
    importGameExplanation: string; // Paste a game PGN to get a browsable replay, computer analysis, game chat and public shareable URL.
    importPgn: string; // Import PGN
    inaccuracy: string; // Inaccuracy
    inappropriateNameWarning: string; // Anything even slightly inappropriate could get your account closed.
    inbox: string; // Inbox
    incorrectPassword: string; // Incorrect password
    increment: string; // Increment
    incrementInSeconds: string; // Increment in seconds
    infiniteAnalysis: string; // Infinite analysis
    inKidModeTheLichessLogoGetsIconX: I18nFormat; // In kid mode, the Lichess logo gets a %s icon, so you know your kids are safe.
    inlineNotation: string; // Inline notation
    inLocalBrowser: string; // in local browser
    insideTheBoard: string; // Inside the board
    instructions: string; // Instructions
    insufficientMaterial: string; // Insufficient material
    inTheFAQ: string; // in the FAQ
    invalidAuthenticationCode: string; // Invalid authentication code
    invalidFen: string; // Invalid FEN
    invalidPgn: string; // Invalid PGN
    invalidUsernameOrPassword: string; // Invalid username or password
    invitedYouToX: I18nFormat; // invited you to "%1$s".
    inYourLocalTimezone: string; // In your own local timezone
    isPrivate: string; // Private
    itsYourTurn: string; // It's your turn!
    join: string; // Join
    joinTheGame: string; // Join the game
    joinTheTeamXToPost: I18nFormat; // Join the %1$s, to post in this forum
    keyboardShortcuts: string; // Keyboard shortcuts
    keyCycleSelectedVariation: string; // Cycle selected variation
    keyEnterOrExitVariation: string; // enter/exit variation
    keyGoToStartOrEnd: string; // go to start/end
    keyMoveBackwardOrForward: string; // move backward/forward
    keyNextBlunder: string; // Next blunder
    keyNextBranch: string; // Next branch
    keyNextInaccuracy: string; // Next inaccuracy
    keyNextLearnFromYourMistakes: string; // Next (Learn from your mistakes)
    keyNextMistake: string; // Next mistake
    keyPreviousBranch: string; // Previous branch
    keyRequestComputerAnalysis: string; // Request computer analysis, Learn from your mistakes
    keyShowOrHideComments: string; // show/hide comments
    kidMode: string; // Kid mode
    kidModeExplanation: string; // This is about safety. In kid mode, all site communications are disabled. Enable this for your children and school students, to protect them from other internet users.
    kidModeIsEnabled: string; // Kid mode is enabled.
    kingInTheCenter: string; // King in the centre
    language: string; // Language
    lastPost: string; // Last post
    lastSeenActive: I18nFormat; // Active %s
    latestForumPosts: string; // Latest forum posts
    leaderboard: string; // Leaderboard
    learnFromThisMistake: string; // Learn from this mistake
    learnFromYourMistakes: string; // Learn from your mistakes
    learnMenu: string; // Learn
    lessThanNbMinutes: I18nPlural; // Less than %s minutes
    letOtherPlayersChallengeYou: string; // Let other players challenge you
    letOtherPlayersFollowYou: string; // Let other players follow you
    letOtherPlayersInviteYouToStudy: string; // Let other players invite you to study
    letOtherPlayersMessageYou: string; // Let other players message you
    level: string; // Level
    lichessDbExplanation: string; // Rated games played on Lichess
    lichessPatronInfo: string; // Lichess is a charity and entirely free/libre open source software.
    lichessTournaments: string; // Lichess tournaments
    lifetimeScore: string; // Lifetime score
    light: string; // Light
    list: string; // List
    listBlockedPlayers: string; // List players you have blocked
    loadingEngine: string; // Loading engine...
    loadPosition: string; // Load position
    lobby: string; // Lobby
    location: string; // Location
    loginToChat: string; // Sign in to chat
    logOut: string; // Sign out
    losing: string; // Losing
    losses: string; // Losses
    lossOr50MovesByPriorMistake: string; // Loss or 50 moves by prior mistake
    lossSavedBy50MoveRule: string; // Loss prevented by 50-move rule
    lostAgainstTOSViolator: string; // You lost rating points to someone who violated the Lichess TOS
    makeAStudy: string; // For safekeeping and sharing, consider making a study.
    makeMainLine: string; // Make mainline
    makePrivateTournament: string; // Make the tournament private, and restrict access with a password
    makeSureToRead: I18nFormat; // Make sure to read %1$s
    markdownAvailable: I18nFormat; // %s is available for more advanced syntax.
    masterDbExplanation: I18nFormat; // OTB games of %1$s+ FIDE-rated players from %2$s to %3$s
    mateInXHalfMoves: I18nPlural; // Mate in %s half-moves
    maxDepthReached: string; // Max depth reached!
    maximumNbCharacters: I18nPlural; // Maximum: %s characters.
    maximumWeeklyRating: string; // Maximum weekly rating
    maybeIncludeMoreGamesFromThePreferencesMenu: string; // Maybe include more games from the preferences menu?
    memberSince: string; // Member since
    memory: string; // Memory
    mentionedYouInX: I18nFormat; // mentioned you in "%1$s".
    menu: string; // Menu
    message: string; // Message
    middlegame: string; // Middlegame
    minimumRatedGames: string; // Minimum rated games
    minimumRating: string; // Minimum rating
    minutesPerSide: string; // Minutes per side
    mistake: string; // Mistake
    mobile: string; // Mobile
    mobileApp: string; // Mobile App
    mode: string; // Mode
    more: string; // More
    moreThanNbPerfRatedGames: I18nPlural; // ≥ %1$s %2$s rated games
    moreThanNbRatedGames: I18nPlural; // ≥ %s rated games
    mouseTricks: string; // Mouse tricks
    move: string; // Move
    movesPlayed: string; // Moves played
    moveTimes: string; // Move times
    multipleLines: string; // Multiple lines
    mustBeInTeam: I18nFormat; // Must be in team %s
    name: string; // Name
    navigateMoveTree: string; // Navigate the move tree
    nbBlunders: I18nPlural; // %s blunders
    nbBookmarks: I18nPlural; // %s bookmarks
    nbDays: I18nPlural; // %s days
    nbDraws: I18nPlural; // %s draws
    nbFollowers: I18nPlural; // %s followers
    nbFollowing: I18nPlural; // %s following
    nbForumPosts: I18nPlural; // %s forum posts
    nbFriendsOnline: I18nPlural; // %s friends online
    nbGames: I18nPlural; // %s games
    nbGamesInPlay: I18nPlural; // %s games in play
    nbGamesWithYou: I18nPlural; // %s games with you
    nbHours: I18nPlural; // %s hours
    nbImportedGames: I18nPlural; // %s imported games
    nbInaccuracies: I18nPlural; // %s inaccuracies
    nbLosses: I18nPlural; // %s losses
    nbMinutes: I18nPlural; // %s minutes
    nbMistakes: I18nPlural; // %s mistakes
    nbPerfTypePlayersThisWeek: I18nPlural; // %1$s %2$s players this week.
    nbPlayers: I18nPlural; // %s players
    nbPlaying: I18nPlural; // %s playing
    nbPuzzles: I18nPlural; // %s puzzles
    nbRated: I18nPlural; // %s rated
    nbSeconds: I18nPlural; // %s seconds
    nbSecondsToPlayTheFirstMove: I18nPlural; // %s seconds to play the first move
    nbSimuls: I18nPlural; // %s simuls
    nbStudies: I18nPlural; // %s studies
    nbTournamentPoints: I18nPlural; // %s tournament points
    nbWins: I18nPlural; // %s wins
    needNbMoreGames: I18nPlural; // You need to play %s more rated games
    needNbMorePerfGames: I18nPlural; // You need to play %1$s more %2$s rated games
    networkLagBetweenYouAndLichess: string; // Network lag between you and Lichess
    never: string; // Never
    neverTypeYourPassword: string; // Never type your Lichess password on another site!
    newOpponent: string; // New opponent
    newPassword: string; // New password
    newPasswordAgain: string; // New password (again)
    newPasswordsDontMatch: string; // The new passwords don't match
    newPasswordStrength: string; // Password strength
    newTournament: string; // New tournament
    next: string; // Next
    nextXTournament: I18nFormat; // Next %s tournament:
    no: string; // No
    noChat: string; // No chat
    noConditionalPremoves: string; // No conditional premoves
    noDrawBeforeSwissLimit: string; // You cannot draw before 30 moves are played in a Swiss tournament.
    noGameFound: string; // No game found
    noMistakesFoundForBlack: string; // No mistakes found for black
    noMistakesFoundForWhite: string; // No mistakes found for white
    none: string; // None
    noNetwork: string; // Offline
    noNoteYet: string; // No note yet
    noRestriction: string; // No restriction
    normal: string; // Normal
    noSimulExplanation: string; // This simultaneous exhibition does not exist.
    noSimulFound: string; // Simul not found
    notACheckmate: string; // Not a checkmate
    notes: string; // Notes
    nothingToSeeHere: string; // Nothing to see here at the moment.
    notifications: string; // Notifications
    notificationsX: I18nFormat; // Notifications: %1$s
    offerDraw: string; // Offer draw
    oneDay: string; // One day
    oneUrlPerLine: string; // One URL per line.
    onlineAndOfflinePlay: string; // Online and offline play
    onlineBots: string; // Online bots
    onlinePlayers: string; // Online players
    onlyExistingConversations: string; // Only existing conversations
    onlyFriends: string; // Only friends
    onlyMembersOfTeam: string; // Only members of team
    onlyTeamLeaders: string; // Only team leaders
    onlyTeamMembers: string; // Only team members
    onlyWorksOnce: string; // This will only work once.
    onSlowGames: string; // On slow games
    opacity: string; // Opacity
    opening: string; // Opening
    openingEndgameExplorer: string; // Opening/endgame explorer
    openingExplorer: string; // Opening explorer
    openingExplorerAndTablebase: string; // Opening explorer & tablebase
    openings: string; // Openings
    openStudy: string; // Open study
    openTournaments: string; // Open tournaments
    opponent: string; // Opponent
    opponentLeftChoices: string; // Your opponent left the game. You can claim victory, call the game a draw, or wait.
    opponentLeftCounter: I18nPlural; // Your opponent left the game. You can claim victory in %s seconds.
    orLetYourOpponentScanQrCode: string; // Or let your opponent scan this QR code
    orUploadPgnFile: string; // Or upload a PGN file
    other: string; // Other
    otherPlayers: string; // other players
    ourEventTips: string; // Our tips for organising events
    outsideTheBoard: string; // Outside the board
    password: string; // Password
    passwordReset: string; // Password reset
    passwordSuggestion: string; // Do not set a password suggested by someone else. They will use it to steal your account.
    pasteTheFenStringHere: string; // Paste the FEN text here
    pasteThePgnStringHere: string; // Paste the PGN text here
    pause: string; // Pause
    pawnMove: string; // Pawn move
    performance: string; // Performance
    perfRatingX: I18nFormat; // Rating: %s
    phoneAndTablet: string; // Phone and tablet
    pieceSet: string; // Piece set
    play: string; // Play
    playChessEverywhere: string; // Play chess everywhere
    playChessInStyle: string; // Play chess in style
    playComputerMove: string; // Play best computer move
    player: string; // Player
    players: string; // Players
    playEveryGame: string; // Play every game you start.
    playFirstOpeningEndgameExplorerMove: string; // Play first opening/endgame-explorer move
    playingRightNow: string; // Playing right now
    playSelectedMove: string; // play selected move
    playVariationToCreateConditionalPremoves: string; // Play a variation to create conditional premoves
    playWithAFriend: string; // Play with a friend
    playWithTheMachine: string; // Play with the computer
    playX: I18nFormat; // Play %s
    pleasantChessExperience: string; // We aim to provide a pleasant chess experience for everyone.
    points: string; // Points
    popularOpenings: string; // Popular openings
    positionInputHelp: I18nFormat; // Paste a valid FEN to start every game from a given position.
    posts: string; // Posts
    potentialProblem: string; // When a potential problem is detected, we display this message.
    practice: string; // Practice
    practiceWithComputer: string; // Practice with computer
    previouslyOnLichessTV: string; // Previously on Lichess TV
    privacy: string; // Privacy
    privacyPolicy: string; // Privacy policy
    proceedToX: I18nFormat; // Proceed to %s
    profile: string; // Profile
    profileCompletion: I18nFormat; // Profile completion: %s
    promoteVariation: string; // Promote variation
    proposeATakeback: string; // Propose a takeback
    puzzleDesc: string; // Chess tactics trainer
    puzzles: string; // Puzzles
    quickPairing: string; // Quick pairing
    raceFinished: string; // Race finished
    randomColor: string; // Random side
    rank: string; // Rank
    rankIsUpdatedEveryNbMinutes: I18nPlural; // Rank is updated every %s minutes
    rankX: I18nFormat; // Rank: %s
    rapid: string; // Rapid
    rapidDesc: string; // Rapid games: 8 to 25 minutes
    rated: string; // Rated
    ratedFormHelp: string; // Games are rated and impact players ratings
    ratedLessThanInPerf: I18nFormat; // Rated ≤ %1$s in %2$s for the last week
    ratedMoreThanInPerf: I18nFormat; // Rated ≥ %1$s in %2$s
    ratedTournament: string; // Rated
    rating: string; // Rating
    ratingRange: string; // Rating range
    ratingStats: string; // Rating stats
    ratingXOverYGames: I18nPlural; // %1$s rating over %2$s games
    readAboutOur: I18nFormat; // Read about our %s.
    really: string; // really
    realName: string; // Real name
    realTime: string; // Real time
    realtimeReplay: string; // Realtime
    reason: string; // Reason
    receiveForumNotifications: string; // Receive notifications when mentioned in the forum
    recentGames: string; // Recent games
    reconnecting: string; // Reconnecting
    refreshInboxAfterFiveMinutes: string; // Wait 5 minutes and refresh your email inbox.
    refundXpointsTimeControlY: I18nFormat; // Refund: %1$s %2$s rating points.
    rematch: string; // Rematch
    rematchOfferAccepted: string; // Rematch offer accepted
    rematchOfferCanceled: string; // Rematch offer cancelled
    rematchOfferDeclined: string; // Rematch offer declined
    rematchOfferSent: string; // Rematch offer sent
    rememberMe: string; // Keep me logged in
    removesTheDepthLimit: string; // Removes the depth limit, and keeps your computer warm
    reopenYourAccount: string; // Reopen your account
    replayMode: string; // Replay mode
    replies: string; // Replies
    reply: string; // Reply
    replyToThisTopic: string; // Reply to this topic
    reportAUser: string; // Report a user
    reportCheatBoostHelp: string; // Paste the link to the game(s) and explain what is wrong about this user's behaviour. Don't just say "they cheat", but tell us how you came to this conclusion.
    reportProcessedFasterInEnglish: string; // Your report will be processed faster if written in English.
    reportUsernameHelp: string; // Explain what about this username is offensive. Don't just say "it's offensive/inappropriate", but tell us how you came to this conclusion, especially if the insult is obfuscated, not in english, is in slang, or is a historical/cultural reference.
    reportXToModerators: I18nFormat; // Report %s to moderators
    requestAComputerAnalysis: string; // Request a computer analysis
    required: string; // Required.
    reset: string; // Reset
    resign: string; // Resign
    resignLostGames: string; // Resign lost games (don't let the clock run down).
    resignTheGame: string; // Resign the game
    resume: string; // Resume
    resumeLearning: string; // Resume learning
    resumePractice: string; // Resume practice
    resVsX: I18nFormat; // %1$s vs %2$s
    retry: string; // Retry
    returnToSimulHomepage: string; // Return to simul homepage
    returnToTournamentsHomepage: string; // Return to tournaments homepage
    reviewBlackMistakes: string; // Review black mistakes
    reviewWhiteMistakes: string; // Review white mistakes
    revokeAllSessions: string; // revoke all sessions
    safeTournamentName: string; // Pick a very safe name for the tournament.
    save: string; // Save
    screenshotCurrentPosition: string; // Screenshot current position
    scrollOverComputerVariationsToPreviewThem: string; // Scroll over computer variations to preview them.
    searchOrStartNewDiscussion: string; // Search or start new conversation
    security: string; // Security
    seeBestMove: string; // See best move
    send: string; // Send
    sentEmailWithLink: string; // We've sent you an email with a link.
    sessions: string; // Sessions
    setFlair: string; // Set your flair
    setTheBoard: string; // Set the board
    shareYourInsightsData: string; // Share your chess insights data
    showHelpDialog: string; // Show this help dialog
    showMeEverything: string; // Show me everything
    showThreat: string; // Show threat
    showUnreadLichessMessage: string; // You have received a private message from Lichess.
    showVariationArrows: string; // Show variation arrows
    side: string; // Side
    signIn: string; // Sign in
    signUp: string; // Register
    signupEmailHint: string; // We will only use it for password reset.
    signUpToHostOrJoinASimul: string; // Sign up to host or join a simul
    signupUsernameHint: string; // Make sure to choose a family-friendly username. You cannot change it later and any accounts with inappropriate usernames will get closed!
    simulAddExtraTime: string; // You may add extra initial time to your clock to help you cope with the simul.
    simulAddExtraTimePerPlayer: string; // Add initial time to your clock for each player joining the simul.
    simulClockHint: string; // Fischer Clock setup. The more players you take on, the more time you may need.
    simulDescription: string; // Simul description
    simulDescriptionHelp: string; // Anything you want to tell the participants?
    simulFeatured: I18nFormat; // Feature on %s
    simulFeaturedHelp: I18nFormat; // Show your simul to everyone on %s. Disable for private simuls.
    simulHostcolor: string; // Host colour for each game
    simulHostExtraTime: string; // Host extra initial clock time
    simulHostExtraTimePerPlayer: string; // Host extra clock time per player
    simultaneousExhibitions: string; // Simultaneous exhibitions
    simulVariantsHint: string; // If you select several variants, each player gets to choose which one to play.
    since: string; // Since
    siteDescription: string; // Free online chess server. Play chess in a clean interface. No registration, no ads, no plugin required. Play chess with the computer, friends or random opponents.
    size: string; // Size
    skipThisMove: string; // Skip this move
    slow: string; // Slow
    socialMediaLinks: string; // Social media links
    solution: string; // Solution
    someoneYouReportedWasBanned: string; // Someone you reported was banned
    sorry: string; // Sorry :(
    sound: string; // Sound
    sourceCode: string; // Source Code
    spectatorRoom: string; // Spectator room
    stalemate: string; // Stalemate
    standard: string; // Standard
    standByX: I18nFormat; // Stand by %s, pairing players, get ready!
    standing: string; // Standing
    startedStreaming: string; // started streaming
    starting: string; // Starting:
    startPosition: string; // Starting position
    streamerManager: string; // Streamer manager
    streamersMenu: string; // Streamers
    strength: string; // Strength
    studyMenu: string; // Study
    subject: string; // Subject
    subscribe: string; // Subscribe
    success: string; // Success
    switchSides: string; // Switch sides
    takeback: string; // Takeback
    takebackPropositionAccepted: string; // Takeback accepted
    takebackPropositionCanceled: string; // Takeback cancelled
    takebackPropositionDeclined: string; // Takeback declined
    takebackPropositionSent: string; // Takeback sent
    talkInChat: string; // Please be nice in the chat!
    teamNamedX: I18nFormat; // %1$s team
    temporaryInconvenience: string; // We apologise for the temporary inconvenience,
    termsOfService: string; // Terms of Service
    thankYou: string; // Thank you!
    thankYouForReading: string; // Thank you for reading!
    theFirstPersonToComeOnThisUrlWillPlayWithYou: string; // The first person to come to this URL will play with you.
    theForumEtiquette: string; // the forum etiquette
    theGameIsADraw: string; // The game is a draw.
    thematic: string; // Thematic
    thisAccountViolatedTos: string; // This account violated the Lichess Terms of Service
    thisGameIsRated: string; // This game is rated
    thisIsAChessCaptcha: string; // This is a chess CAPTCHA.
    thisTopicIsArchived: string; // This topic has been archived and can no longer be replied to.
    thisTopicIsNowClosed: string; // This topic is now closed.
    threeChecks: string; // Three checks
    threefoldRepetition: string; // Threefold repetition
    time: string; // Time
    timeAlmostUp: string; // Time is almost up!
    timeBeforeTournamentStarts: string; // Time before tournament starts
    timeControl: string; // Time control
    timeline: string; // Timeline
    timeToProcessAMoveOnLichessServer: string; // Time to process a move on Lichess's server
    today: string; // Today
    toggleAllAnalysis: string; // Toggle all computer analysis
    toggleGlyphAnnotations: string; // Toggle move annotations
    toggleLocalAnalysis: string; // Toggle local computer analysis
    toggleLocalEvaluation: string; // Toggle local evaluation
    togglePositionAnnotations: string; // Toggle position annotations
    toggleTheChat: string; // Toggle the chat
    toggleVariationArrows: string; // Toggle variation arrows
    toInviteSomeoneToPlayGiveThisUrl: string; // To invite someone to play, give this URL
    tools: string; // Tools
    topGames: string; // Top games
    topics: string; // Topics
    toReportSomeoneForCheatingOrBadBehavior: I18nFormat; // To report a user for cheating or bad behaviour, %1$s
    toRequestSupport: I18nFormat; // To request support, %1$s
    toStudy: string; // Study
    tournament: string; // Tournament
    tournamentCalendar: string; // Tournament calendar
    tournamentComplete: string; // Tournament complete
    tournamentDoesNotExist: string; // This tournament does not exist.
    tournamentEntryCode: string; // Tournament entry code
    tournamentFAQ: string; // Arena tournament FAQ
    tournamentHomeDescription: string; // Play fast-paced chess tournaments! Join an official scheduled tournament, or create your own. Bullet, Blitz, Classical, Chess960, King of the Hill, Threecheck, and more options available for endless chess fun.
    tournamentHomeTitle: string; // Chess tournaments featuring various time controls and variants
    tournamentIsStarting: string; // The tournament is starting
    tournamentMayHaveBeenCanceled: string; // The tournament may have been cancelled if all players left before it started.
    tournamentNotFound: string; // Tournament not found
    tournamentPairingsAreNowClosed: string; // The tournament pairings are now closed.
    tournamentPoints: string; // Tournament points
    tournaments: string; // Tournaments
    tournChat: string; // Tournament chat
    tournDescription: string; // Tournament description
    tournDescriptionHelp: string; // Anything special you want to tell the participants? Try to keep it short. Markdown links are available: [name](https://url)
    tpTimeSpentOnTV: I18nFormat; // Time featured on TV: %s
    tpTimeSpentPlaying: I18nFormat; // Time spent playing: %s
    transparent: string; // Transparent
    troll: string; // Troll
    tryAnotherMoveForBlack: string; // Try another move for black
    tryAnotherMoveForWhite: string; // Try another move for white
    tryTheContactPage: string; // try the contact page
    tryToWin: string; // Try to win (or at least draw) every game you play.
    typePrivateNotesHere: string; // Type private notes here
    ultraBulletDesc: string; // Insanely fast games: less than 30 seconds
    unblock: string; // Unblock
    unfollow: string; // Unfollow
    unfollowX: I18nFormat; // Unfollow %s
    unknown: string; // Unknown
    unknownDueToRounding: string; // Win/loss only guaranteed if recommended tablebase line has been followed since the last capture or pawn move, due to possible rounding of DTZ values in Syzygy tablebases.
    unlimited: string; // Unlimited
    unsubscribe: string; // Unsubscribe
    until: string; // Until
    user: string; // User
    userIsBetterThanPercentOfPerfTypePlayers: I18nFormat; // %1$s is better than %2$s of %3$s players.
    username: string; // User name
    usernameAlreadyUsed: string; // This username is already in use, please try another one.
    usernameCanBeUsedForNewAccount: string; // You can use this username to create a new account
    usernameCharsInvalid: string; // The username must only contain letters, numbers, underscores, and hyphens. Consecutive underscores and hyphens are not allowed.
    usernameNotFound: I18nFormat; // We couldn't find any user by this name: %s.
    usernameOrEmail: string; // User name or email
    usernamePrefixInvalid: string; // The username must start with a letter.
    usernameSuffixInvalid: string; // The username must end with a letter or a number.
    usernameUnacceptable: string; // This username is not acceptable.
    useTheReportForm: string; // use the report form
    usingServerAnalysis: string; // Using server analysis
    variant: string; // Variant
    variantEnding: string; // Variant ending
    variantLoss: string; // Variant loss
    variants: string; // Variants
    variantWin: string; // Variant win
    variationArrowsInfo: string; // Variation arrows let you navigate without using the move list.
    victory: string; // Victory
    victoryVsYInZ: I18nFormat; // %1$s vs %2$s in %3$s
    videoLibrary: string; // Video library
    viewInFullSize: string; // View in full size
    viewRematch: string; // View rematch
    views: string; // Views
    viewTheSolution: string; // View the solution
    viewTournament: string; // View tournament
    waitForSignupHelp: string; // We will come back to you shortly to help you complete your signup.
    waiting: string; // Waiting
    waitingForAnalysis: string; // Waiting for analysis
    waitingForOpponent: string; // Waiting for opponent
    watch: string; // Watch
    watchGames: string; // Watch games
    webmasters: string; // Webmasters
    website: string; // Website
    weeklyPerfTypeRatingDistribution: I18nFormat; // Weekly %s rating distribution
    weHadToTimeYouOutForAWhile: string; // We had to time you out for a while.
    weHaveSentYouAnEmailClickTheLink: string; // We've sent you an email. Click the link in the email to activate your account.
    weHaveSentYouAnEmailTo: I18nFormat; // We've sent an email to %s. Click the link in the email to reset your password.
    whatIsIheMatter: string; // What's the matter?
    whatSignupUsername: string; // What username did you use to sign up?
    whenCreateSimul: string; // When you create a Simul, you get to play several players at once.
    white: string; // White
    whiteCastlingKingside: string; // White O-O
    whiteCheckmatesInOneMove: string; // White to checkmate in one move
    whiteDeclinesDraw: string; // White declines draw
    whiteDidntMove: string; // White didn't move
    whiteDrawBlack: string; // White / Draw / Black
    whiteIsVictorious: string; // White is victorious
    whiteLeftTheGame: string; // White left the game
    whiteOffersDraw: string; // White offers draw
    whitePlays: string; // White to play
    whiteResigned: string; // White resigned
    whiteTimeOut: string; // White time out
    whiteWins: string; // White wins
    whiteWinsGame: string; // White wins
    why: string; // Why?
    winner: string; // Winner
    winning: string; // Winning
    winOr50MovesByPriorMistake: string; // Win or 50 moves by prior mistake
    winPreventedBy50MoveRule: string; // Win prevented by 50-move rule
    winRate: string; // Win rate
    wins: string; // Wins
    wishYouGreatGames: string; // and wish you great games on lichess.org.
    withdraw: string; // Withdraw
    withEverybody: string; // With everybody
    withFriends: string; // With friends
    withNobody: string; // With nobody
    writeAPrivateNoteAboutThisUser: string; // Write a private note about this user
    xCompetesInY: I18nFormat; // %1$s competes in %2$s
    xCreatedTeamY: I18nFormat; // %1$s created team %2$s
    xHostsY: I18nFormat; // %1$s hosts %2$s
    xInvitedYouToY: I18nFormat; // %1$s invited you to "%2$s".
    xIsAFreeYLibreOpenSourceChessServer: I18nFormat; // %1$s is a free (%2$s), libre, no-ads, open source chess server.
    xJoinedTeamY: I18nFormat; // %1$s joined team %2$s
    xJoinsY: I18nFormat; // %1$s joins %2$s
    xLikesY: I18nFormat; // %1$s likes %2$s
    xMentionedYouInY: I18nFormat; // %1$s mentioned you in "%2$s".
    xOpeningExplorer: I18nFormat; // %s opening explorer
    xPostedInForumY: I18nFormat; // %1$s posted in topic %2$s
    xRating: I18nFormat; // %s rating
    xStartedFollowingY: I18nFormat; // %1$s started following %2$s
    xStartedStreaming: I18nFormat; // %s started streaming
    xWasPlayed: I18nFormat; // %s was played
    yes: string; // Yes
    yesterday: string; // Yesterday
    youAreBetterThanPercentOfPerfTypePlayers: I18nFormat; // You are better than %1$s of %2$s players.
    youAreLeavingLichess: string; // You are leaving Lichess
    youAreNotInTeam: I18nFormat; // You are not in the team %s
    youAreNowPartOfTeam: string; // You are now part of the team.
    youArePlaying: string; // You are playing!
    youBrowsedAway: string; // You browsed away
    youCanAlsoScrollOverTheBoardToMoveInTheGame: string; // Scroll over the board to move in the game.
    youCanDoBetter: string; // You can do better
    youCanHideFlair: string; // There is a setting to hide all user flairs across the entire site.
    youCannotPostYetPlaySomeGames: string; // You can't post in the forums yet. Play some games!
    youCantStartNewGame: string; // You can't start a new game until this one is finished.
    youDoNotHaveAnEstablishedPerfTypeRating: I18nFormat; // You do not have an established %s rating.
    youHaveBeenTimedOut: string; // You have been timed out.
    youHaveJoinedTeamX: I18nFormat; // You have joined "%1$s".
    youNeedAnAccountToDoThat: string; // You need an account to do that
    youPlayTheBlackPieces: string; // You play the black pieces
    youPlayTheWhitePieces: string; // You play the white pieces
    yourOpponentOffersADraw: string; // Your opponent offers a draw
    yourOpponentProposesATakeback: string; // Your opponent proposes a takeback
    yourOpponentWantsToPlayANewGameWithYou: string; // Your opponent wants to play a new game with you
    yourPendingSimuls: string; // Your pending simuls
    yourPerfRatingIsProvisional: I18nFormat; // Your %s rating is provisional
    yourPerfRatingIsTooHigh: I18nFormat; // Your %1$s rating (%2$s) is too high
    yourPerfRatingIsTooLow: I18nFormat; // Your %1$s rating (%2$s) is too low
    yourPerfTypeRatingIsRating: I18nFormat; // Your %1$s rating is %2$s.
    yourQuestionMayHaveBeenAnswered: I18nFormat; // Your question may already have an answer %1$s
    yourRating: string; // Your rating
    yourScore: I18nFormat; // Your score: %s
    yourTopWeeklyPerfRatingIsTooHigh: I18nFormat; // Your top weekly %1$s rating (%2$s) is too high
    yourTurn: string; // Your turn
    zeroAdvertisement: string; // Zero advertisement
  };
  storm: {
    accuracy: string; // Accuracy
    allTime: string; // All-time
    bestRunOfDay: string; // Best run of day
    clickToReload: string; // Click to reload
    combo: string; // Combo
    createNewGame: string; // Create a new game
    endRun: string; // End run (hotkey: Enter)
    failedPuzzles: string; // Failed puzzles
    getReady: string; // Get ready!
    highestSolved: string; // Highest solved
    highscores: string; // Highscores
    highscoreX: I18nFormat; // Highscore: %s
    joinPublicRace: string; // Join a public race
    joinRematch: string; // Join rematch
    joinTheRace: string; // Join the race!
    moves: string; // Moves
    moveToStart: string; // Move to start
    newAllTimeHighscore: string; // New all-time highscore!
    newDailyHighscore: string; // New daily highscore!
    newMonthlyHighscore: string; // New monthly highscore!
    newRun: string; // New run (hotkey: Space)
    newWeeklyHighscore: string; // New weekly highscore!
    nextRace: string; // Next race
    playAgain: string; // Play again
    playedNbRunsOfPuzzleStorm: I18nPlural; // Played %1$s runs of %2$s
    previousHighscoreWasX: I18nFormat; // Previous highscore was %s
    puzzlesPlayed: string; // Puzzles played
    puzzlesSolved: string; // puzzles solved
    raceComplete: string; // Race complete!
    raceYourFriends: string; // Race your friends
    runs: string; // Runs
    score: string; // Score
    skip: string; // skip
    skipExplanation: string; // Skip this move to preserve your combo! Only works once per race.
    skipHelp: string; // You can skip one move per race:
    skippedPuzzle: string; // Skipped puzzle
    slowPuzzles: string; // Slow puzzles
    spectating: string; // Spectating
    startTheRace: string; // Start the race
    thisMonth: string; // This month
    thisRunHasExpired: string; // This run has expired!
    thisRunWasOpenedInAnotherTab: string; // This run was opened in another tab!
    thisWeek: string; // This week
    time: string; // Time
    timePerMove: string; // Time per move
    viewBestRuns: string; // View best runs
    waitForRematch: string; // Wait for rematch
    waitingForMorePlayers: string; // Waiting for more players to join...
    waitingToStart: string; // Waiting to start
    xRuns: I18nPlural; // %s runs
    youPlayTheBlackPiecesInAllPuzzles: string; // You play the black pieces in all puzzles
    youPlayTheWhitePiecesInAllPuzzles: string; // You play the white pieces in all puzzles
    yourRankX: I18nFormat; // Your rank: %s
  };
  streamer: {
    allStreamers: string; // All streamers
    approved: string; // Your stream is approved.
    becomeStreamer: string; // Become a Lichess streamer
    changePicture: string; // Change/delete your picture
    currentlyStreaming: I18nFormat; // Currently streaming: %s
    downloadKit: string; // Download streamer kit
    doYouHaveStream: string; // Do you have a Twitch or YouTube channel?
    editPage: string; // Edit streamer page
    headline: string; // Headline
    hereWeGo: string; // Here we go!
    keepItShort: I18nPlural; // Keep it short: %s characters max
    lastStream: I18nFormat; // Last stream %s
    lichessStreamer: string; // Lichess streamer
    lichessStreamers: string; // Lichess streamers
    live: string; // LIVE!
    longDescription: string; // Long description
    maxSize: I18nFormat; // Max size: %s
    offline: string; // OFFLINE
    optionalOrEmpty: string; // Optional. Leave empty if none
    pendingReview: string; // Your stream is being reviewed by moderators.
    perk1: string; // Get a flaming streamer icon on your Lichess profile.
    perk2: string; // Get bumped up to the top of the streamers list.
    perk3: string; // Notify your Lichess followers.
    perk4: string; // Show your stream in your games, tournaments and studies.
    perks: string; // Benefits of streaming with the keyword
    pleaseFillIn: string; // Please fill in your streamer information, and upload a picture.
    requestReview: string; // request a moderator review
    rule1: string; // Include the keyword \"lichess.org\" in your stream title and use the category \"Chess\" when you stream on Lichess.
    rule2: string; // Remove the keyword when you stream non-Lichess stuff.
    rule3: string; // Lichess will detect your stream automatically and enable the following perks:
    rule4: I18nFormat; // Read our %s to ensure fair play for everyone during your stream.
    rules: string; // Streaming rules
    streamerLanguageSettings: string; // The Lichess streamer page targets your audience with the language provided by your streaming platform. Set the correct default language for your chess streams in the app or service you use to broadcast.
    streamerName: string; // Your streamer name on Lichess
    streamingFairplayFAQ: string; // streaming Fairplay FAQ
    tellUsAboutTheStream: string; // Tell us about your stream in one sentence
    twitchUsername: string; // Your Twitch username or URL
    uploadPicture: string; // Upload a picture
    visibility: string; // Visible on the streamers page
    whenApproved: string; // When approved by moderators
    whenReady: I18nFormat; // When you are ready to be listed as a Lichess streamer, %s
    xIsStreaming: I18nFormat; // %s is streaming
    xStreamerPicture: I18nFormat; // %s streamer picture
    yourPage: string; // Your streamer page
    youTubeChannelId: string; // Your YouTube channel ID
  };
  study: {
    addMembers: string; // Add members
    addNewChapter: string; // Add a new chapter
    allowCloning: string; // Allow cloning
    allStudies: string; // All studies
    allSyncMembersRemainOnTheSamePosition: string; // All SYNC members remain on the same position
    alphabetical: string; // Alphabetical
    analysisMode: string; // Analysis mode
    annotateWithGlyphs: string; // Annotate with glyphs
    attack: string; // Attack
    automatic: string; // Automatic
    back: string; // Back
    blackIsBetter: string; // Black is better
    blackIsSlightlyBetter: string; // Black is slightly better
    blackIsWinning: string; // Black is winning
    blunder: string; // Blunder
    brilliantMove: string; // Brilliant move
    chapterPgn: string; // Chapter PGN
    chapterX: I18nFormat; // Chapter %s
    clearAllCommentsInThisChapter: string; // Clear all comments, glyphs and drawn shapes in this chapter
    clearAnnotations: string; // Clear annotations
    clearChat: string; // Clear chat
    clearVariations: string; // Clear variations
    cloneStudy: string; // Clone
    commentThisMove: string; // Comment on this move
    commentThisPosition: string; // Comment on this position
    confirmDeleteStudy: I18nFormat; // Delete the entire study? There is no going back! Type the name of the study to confirm: %s
    contributor: string; // Contributor
    contributors: string; // Contributors
    copyChapterPgn: string; // Copy PGN
    counterplay: string; // Counterplay
    createChapter: string; // Create chapter
    createStudy: string; // Create study
    currentChapterUrl: string; // Current chapter URL
    dateAddedNewest: string; // Date added (newest)
    dateAddedOldest: string; // Date added (oldest)
    deleteChapter: string; // Delete chapter
    deleteStudy: string; // Delete study
    deleteTheStudyChatHistory: string; // Delete the study chat history? There is no going back!
    deleteThisChapter: string; // Delete this chapter. There is no going back!
    development: string; // Development
    downloadAllGames: string; // Download all games
    downloadGame: string; // Download game
    dubiousMove: string; // Dubious move
    editChapter: string; // Edit chapter
    editor: string; // Editor
    editStudy: string; // Edit study
    embedInYourWebsite: string; // Embed in your website
    empty: string; // Empty
    enableSync: string; // Enable sync
    equalPosition: string; // Equal position
    everyone: string; // Everyone
    first: string; // First
    getAFullComputerAnalysis: string; // Get a full server-side computer analysis of the mainline.
    goodMove: string; // Good move
    hideNextMoves: string; // Hide next moves
    hot: string; // Hot
    importFromChapterX: I18nFormat; // Import from %s
    initiative: string; // Initiative
    interactiveLesson: string; // Interactive lesson
    interestingMove: string; // Interesting move
    inviteOnly: string; // Invite only
    inviteToTheStudy: string; // Invite to the study
    kick: string; // Kick
    last: string; // Last
    leaveTheStudy: string; // Leave the study
    like: string; // Like
    loadAGameByUrl: string; // Load games by URLs
    loadAGameFromPgn: string; // Load games from PGN
    loadAGameFromXOrY: I18nFormat; // Load games from %1$s or %2$s
    loadAPositionFromFen: string; // Load a position from FEN
    makeSureTheChapterIsComplete: string; // Make sure the chapter is complete. You can only request analysis once.
    manageTopics: string; // Manage topics
    members: string; // Members
    mistake: string; // Mistake
    mostPopular: string; // Most popular
    myFavoriteStudies: string; // My favourite studies
    myPrivateStudies: string; // My private studies
    myPublicStudies: string; // My public studies
    myStudies: string; // My studies
    myTopics: string; // My topics
    nbChapters: I18nPlural; // %s Chapters
    nbGames: I18nPlural; // %s Games
    nbMembers: I18nPlural; // %s Members
    newChapter: string; // New chapter
    newTag: string; // New tag
    next: string; // Next
    nextChapter: string; // Next chapter
    nobody: string; // Nobody
    noLetPeopleBrowseFreely: string; // No: let people browse freely
    noneYet: string; // None yet.
    noPinnedComment: string; // None
    normalAnalysis: string; // Normal analysis
    novelty: string; // Novelty
    onlyContributorsCanRequestAnalysis: string; // Only the study contributors can request a computer analysis.
    onlyMe: string; // Only me
    onlyMove: string; // Only move
    onlyPublicStudiesCanBeEmbedded: string; // Only public studies can be embedded!
    open: string; // Open
    orientation: string; // Orientation
    pasteYourPgnTextHereUpToNbGames: I18nPlural; // Paste your PGN text here, up to %s games
    pgnTags: string; // PGN tags
    pinnedChapterComment: string; // Pinned chapter comment
    pinnedStudyComment: string; // Pinned study comment
    playAgain: string; // Play again
    playing: string; // Playing
    pleaseOnlyInvitePeopleYouKnow: string; // Please only invite people who know you, and who actively want to join this study.
    popularTopics: string; // Popular topics
    prevChapter: string; // Previous chapter
    previous: string; // Previous
    private: string; // Private
    public: string; // Public
    readMoreAboutEmbedding: string; // Read more about embedding
    recentlyUpdated: string; // Recently updated
    rightUnderTheBoard: string; // Right under the board
    save: string; // Save
    saveChapter: string; // Save chapter
    searchByUsername: string; // Search by username
    shareAndExport: string; // Share & export
    shareChanges: string; // Share changes with spectators and save them on the server
    showEvalBar: string; // Evaluation bars
    spectator: string; // Spectator
    start: string; // Start
    startAtInitialPosition: string; // Start at initial position
    startAtX: I18nFormat; // Start at %s
    startFromCustomPosition: string; // Start from custom position
    startFromInitialPosition: string; // Start from initial position
    studiesCreatedByX: I18nFormat; // Studies created by %s
    studiesIContributeTo: string; // Studies I contribute to
    studyActions: string; // Study actions
    studyNotFound: string; // Study not found
    studyPgn: string; // Study PGN
    studyUrl: string; // Study URL
    theChapterIsTooShortToBeAnalysed: string; // The chapter is too short to be analysed.
    timeTrouble: string; // Time trouble
    topics: string; // Topics
    unclearPosition: string; // Unclear position
    unlike: string; // Unlike
    unlisted: string; // Unlisted
    urlOfTheGame: string; // URL of the games, one per line
    visibility: string; // Visibility
    whatAreStudies: string; // What are studies?
    whatWouldYouPlay: string; // What would you play in this position?
    whereDoYouWantToStudyThat: string; // Where do you want to study that?
    whiteIsBetter: string; // White is better
    whiteIsSlightlyBetter: string; // White is slightly better
    whiteIsWinning: string; // White is winning
    withCompensation: string; // With compensation
    withTheIdea: string; // With the idea
    xBroughtToYouByY: I18nFormat; // %1$s, brought to you by %2$s
    yesKeepEveryoneOnTheSamePosition: string; // Yes: keep everyone on the same position
    youAreNowAContributor: string; // You are now a contributor
    youAreNowASpectator: string; // You are now a spectator
    youCanPasteThisInTheForumToEmbed: string; // You can paste this in the forum or your Lichess blog to embed
    youCompletedThisLesson: string; // Congratulations! You completed this lesson.
    zugzwang: string; // Zugzwang
  };
  swiss: {
    absences: string; // Absences
    byes: string; // Byes
    comparison: string; // Comparison
    durationUnknown: string; // Predefined max rounds, but duration unknown
    dutchSystem: string; // Dutch system
    earlyDrawsAnswer: string; // In Swiss games, players cannot draw before 30 moves are played. While this measure cannot prevent pre-arranged draws, it at least makes it harder to agree to a draw on the fly.
    earlyDrawsQ: string; // What happens with early draws?
    FIDEHandbook: string; // FIDE handbook
    forbiddedUsers: string; // If this list is non-empty, then users absent from this list will be forbidden to join. One username per line.
    forbiddenPairings: string; // Forbidden pairings
    forbiddenPairingsHelp: string; // Usernames of players that must not play together (Siblings, for instance). Two usernames per line, separated by a space.
    identicalForbidden: string; // Forbidden
    identicalPairing: string; // Identical pairing
    joinOrCreateTeam: string; // Join or create a team
    lateJoin: string; // Late join
    lateJoinA: string; // Yes, until more than half the rounds have started; for example in a 11-rounds Swiss, players can join before round 6 starts and in a 12-rounds before round 7 starts.
    lateJoinQ: string; // Can players late-join?
    lateJoinUntil: string; // Yes until more than half the rounds have started
    manualPairings: string; // Manual pairings in next round
    manualPairingsHelp: string; // Specify all pairings of the next round manually. One player pair per line. Example:
    moreRoundsThanPlayersA: string; // When all possible pairings have been played, the tournament will be ended and a winner declared.
    moreRoundsThanPlayersQ: string; // What happens if the tournament has more rounds than players?
    mustHavePlayedTheirLastSwissGame: string; // Must have played their last swiss game
    mustHavePlayedTheirLastSwissGameHelp: string; // Only let players join if they have played their last swiss game. If they failed to show up in a recent swiss event, they won't be able to enter yours. This results in a better swiss experience for the players who actually show up.
    nbRounds: I18nPlural; // %s rounds
    newSwiss: string; // New Swiss tournament
    nextRound: string; // Next round
    nowPlaying: string; // Now playing
    numberOfByesA: string; // A player gets a bye of one point every time the pairing system can't find a pairing for them.
    numberOfByesQ: string; // How many byes can a player get?
    numberOfGames: string; // Number of games
    numberOfGamesAsManyAsPossible: string; // As many as can be played in the allotted duration
    numberOfGamesPreDefined: string; // Decided in advance, same for all players
    numberOfRounds: string; // Number of rounds
    numberOfRoundsHelp: string; // An odd number of rounds allows optimal colour balance.
    oneRoundEveryXDays: I18nPlural; // One round every %s days
    ongoingGames: I18nPlural; // Ongoing games
    otherSystemsA: string; // We don't plan to add more tournament systems to Lichess at the moment.
    otherSystemsQ: string; // What about other tournament systems?
    pairingsA: I18nFormat; // With the %1$s, implemented by %2$s, in accordance with the %3$s.
    pairingsQ: string; // How are pairings decided?
    pairingSystem: string; // Pairing system
    pairingSystemArena: string; // Any available opponent with similar ranking
    pairingSystemSwiss: string; // Best pairing based on points and tie breaks
    pairingWaitTime: string; // Pairing wait time
    pairingWaitTimeArena: string; // Fast: doesn't wait for all players
    pairingWaitTimeSwiss: string; // Slow: waits for all players
    pause: string; // Pause
    pauseSwiss: string; // Yes but might reduce the number of rounds
    playYourGames: string; // Play your games
    pointsCalculationA: string; // A win is worth one point, a draw is a half point, and a loss is zero points.
    pointsCalculationQ: string; // How are points calculated?
    possibleButNotConsecutive: string; // Possible, but not consecutive
    predefinedDuration: string; // Predefined duration in minutes
    predefinedUsers: string; // Only allow pre-defined users to join
    protectionAgainstNoShowA: string; // Players who sign up for Swiss events but don't play their games can be problematic.
    protectionAgainstNoShowQ: string; // What is done regarding no-shows?
    restrictedToTeamsA: string; // Swiss tournaments were not designed for online chess. They demand punctuality, dedication and patience from players.
    restrictedToTeamsQ: string; // Why is it restricted to teams?
    roundInterval: string; // Interval between rounds
    roundRobinA: string; // We'd like to add it, but unfortunately Round Robin doesn't work online.
    roundRobinQ: string; // What about Round Robin?
    roundsAreStartedManually: string; // Rounds are started manually
    similarToOTB: string; // Similar to OTB tournaments
    sonnebornBergerScore: string; // Sonneborn–Berger score
    startingIn: string; // Starting in
    startingSoon: string; // Starting soon
    streaksAndBerserk: string; // Streaks and Berserk
    swiss: string; // Swiss
    swissDescription: I18nFormat; // In a Swiss tournament %1$s, each competitor does not necessarily play all other entrants. Competitors meet one-on-one in each round and are paired using a set of rules designed to ensure that each competitor plays opponents with a similar running score, but not the same opponent more than once. The winner is the competitor with the highest aggregate points earned in all rounds. All competitors play in each round unless there is an odd number of players.
    swissTournaments: string; // Swiss tournaments
    swissVsArenaA: string; // In a Swiss tournament, all participants play the same number of games, and can only play each other once.
    swissVsArenaQ: string; // When to use Swiss tournaments instead of arenas?
    teamOnly: I18nFormat; // Swiss tournaments can only be created by team leaders, and can only be played by team members.
    tieBreak: string; // Tie Break
    tiebreaksCalculationA: I18nFormat; // With the %s.
    tiebreaksCalculationQ: string; // How are tie breaks calculated?
    tournDuration: string; // Duration of the tournament
    tournStartDate: string; // Tournament start date
    unlimitedAndFree: string; // Unlimited and free
    viewAllXRounds: I18nPlural; // View all %s rounds
    whatIfOneDoesntPlayA: string; // Their clock will tick, they will flag, and lose the game.
    whatIfOneDoesntPlayQ: string; // What happens if a player doesn't play a game?
    willSwissReplaceArenasA: string; // No. They're complementary features.
    willSwissReplaceArenasQ: string; // Will Swiss replace arena tournaments?
    xMinutesBetweenRounds: I18nPlural; // %s minutes between rounds
    xRoundsSwiss: I18nPlural; // %s rounds Swiss
    xSecondsBetweenRounds: I18nPlural; // %s seconds between rounds
  };
  team: {
    allTeams: string; // All teams
    battleOfNbTeams: I18nPlural; // Battle of %s teams
    beingReviewed: string; // Your join request is being reviewed by a team leader.
    closeTeam: string; // Close team
    closeTeamDescription: string; // Closes the team forever.
    completedTourns: string; // Completed tournaments
    declinedRequests: string; // Declined Requests
    entryCode: string; // Team entry code
    entryCodeDescriptionForLeader: string; // (Optional) An entry code that new members must know to join this team.
    incorrectEntryCode: string; // Incorrect entry code.
    innerTeam: string; // Inner team
    joinLichessVariantTeam: I18nFormat; // Join the official %s team for news and events
    joinTeam: string; // Join team
    kickSomeone: string; // Kick someone out of the team
    leadersChat: string; // Leaders chat
    leaderTeams: string; // Leader teams
    listTheTeamsThatWillCompete: string; // List the teams that will compete in this battle.
    manuallyReviewAdmissionRequests: string; // Manually review admission requests
    manuallyReviewAdmissionRequestsHelp: string; // If checked, players will need to write a request to join the team, which you can decline or accept.
    messageAllMembers: string; // Message all members
    messageAllMembersLongDescription: string; // Send a private message to ALL members of the team.
    messageAllMembersOverview: string; // Send a private message to every member of the team
    myTeams: string; // My teams
    nbLeadersPerTeam: I18nPlural; // %s leaders per team
    nbMembers: I18nPlural; // %s members
    newTeam: string; // New team
    noTeamFound: string; // No team found
    numberOfLeadsPerTeam: string; // Number of leaders per team. The sum of their score is the score of the team.
    numberOfLeadsPerTeamHelp: string; // You really shouldn't change this value after the tournament has started!
    oneTeamPerLine: string; // One team per line. Use the auto-completion.
    oneTeamPerLineHelp: string; // You can copy-paste this list from a tournament to another!
    onlyLeaderLeavesTeam: string; // Please add a new team leader before leaving, or close the team.
    quitTeam: string; // Leave team
    requestDeclined: string; // Your join request was declined by a team leader.
    subToTeamMessages: string; // Subscribe to team messages
    swissTournamentOverview: string; // A Swiss tournament that only members of your team can join
    team: string; // Team
    teamAlreadyExists: string; // This team already exists.
    teamBattle: string; // Team Battle
    teamBattleOverview: string; // A battle of multiple teams, each player scores points for their team
    teamLeaders: I18nPlural; // Team leaders
    teamPage: string; // Team page
    teamRecentMembers: string; // Recent members
    teams: string; // Teams
    teamsIlead: string; // Teams I lead
    teamTournament: string; // Team tournament
    teamTournamentOverview: string; // An Arena tournament that only members of your team can join
    thisTeamBattleIsOver: string; // This tournament is over, and the teams can no longer be updated.
    upcomingTournaments: string; // Upcoming tournaments
    whoToKick: string; // Who do you want to kick out of the team?
    willBeReviewed: string; // Your join request will be reviewed by a team leader.
    xJoinRequests: I18nPlural; // %s join requests
    youWayWantToLinkOneOfTheseTournaments: string; // You may want to link one of these upcoming tournaments?
  };
  tfa: {
    authenticationCode: string; // Authentication code
    disableTwoFactor: string; // Disable two-factor authentication
    enableTwoFactor: string; // Enable two-factor authentication
    enterPassword: string; // Enter your password and the authentication code generated by the app to complete the setup. You will need an authentication code every time you log in.
    ifYouCannotScanEnterX: I18nFormat; // If you cannot scan the code, enter the secret key %s into your app.
    ifYouLoseAccessTwoFactor: I18nFormat; // Note: If you lose access to your two-factor authentication codes, you can do a %s via email.
    openTwoFactorApp: string; // Open the two-factor authentication app on your device to view your authentication code and verify your identity.
    scanTheCode: string; // Scan the QR code with the app.
    setupReminder: string; // Please enable two-factor authentication to secure your account at https://lichess.org/account/twofactor.
    twoFactorAppRecommend: string; // Get an app for two-factor authentication. We recommend the following apps:
    twoFactorAuth: string; // Two-factor authentication
    twoFactorEnabled: string; // Two-factor authentication enabled
    twoFactorHelp: string; // Two-factor authentication adds another layer of security to your account.
    twoFactorToDisable: string; // You need your password and an authentication code from your authenticator app to disable two-factor authentication.
  };
  timeago: {
    completed: string; // completed
    inNbDays: I18nPlural; // in %s days
    inNbHours: I18nPlural; // in %s hours
    inNbMinutes: I18nPlural; // in %s minutes
    inNbMonths: I18nPlural; // in %s months
    inNbSeconds: I18nPlural; // in %s seconds
    inNbWeeks: I18nPlural; // in %s weeks
    inNbYears: I18nPlural; // in %s years
    justNow: string; // just now
    nbDaysAgo: I18nPlural; // %s days ago
    nbHoursAgo: I18nPlural; // %s hours ago
    nbHoursRemaining: I18nPlural; // %s hours remaining
    nbMinutesAgo: I18nPlural; // %s minutes ago
    nbMinutesRemaining: I18nPlural; // %s minutes remaining
    nbMonthsAgo: I18nPlural; // %s months ago
    nbWeeksAgo: I18nPlural; // %s weeks ago
    nbYearsAgo: I18nPlural; // %s years ago
    rightNow: string; // right now
  };
  tourname: {
    classicalShield: string; // Classical Shield
    classicalShieldArena: string; // Classical Shield Arena
    dailyClassical: string; // Daily Classical
    dailyClassicalArena: string; // Daily Classical Arena
    dailyRapid: string; // Daily Rapid
    dailyRapidArena: string; // Daily Rapid Arena
    dailyX: I18nFormat; // Daily %s
    dailyXArena: I18nFormat; // Daily %s Arena
    easternClassical: string; // Eastern Classical
    easternClassicalArena: string; // Eastern Classical Arena
    easternRapid: string; // Eastern Rapid
    easternRapidArena: string; // Eastern Rapid Arena
    easternX: I18nFormat; // Eastern %s
    easternXArena: I18nFormat; // Eastern %s Arena
    eliteX: I18nFormat; // Elite %s
    eliteXArena: I18nFormat; // Elite %s Arena
    hourlyRapid: string; // Hourly Rapid
    hourlyRapidArena: string; // Hourly Rapid Arena
    hourlyX: I18nFormat; // Hourly %s
    hourlyXArena: I18nFormat; // Hourly %s Arena
    monthlyClassical: string; // Monthly Classical
    monthlyClassicalArena: string; // Monthly Classical Arena
    monthlyRapid: string; // Monthly Rapid
    monthlyRapidArena: string; // Monthly Rapid Arena
    monthlyX: I18nFormat; // Monthly %s
    monthlyXArena: I18nFormat; // Monthly %s Arena
    rapidShield: string; // Rapid Shield
    rapidShieldArena: string; // Rapid Shield Arena
    weeklyClassical: string; // Weekly Classical
    weeklyClassicalArena: string; // Weekly Classical Arena
    weeklyRapid: string; // Weekly Rapid
    weeklyRapidArena: string; // Weekly Rapid Arena
    weeklyX: I18nFormat; // Weekly %s
    weeklyXArena: I18nFormat; // Weekly %s Arena
    xArena: I18nFormat; // %s Arena
    xShield: I18nFormat; // %s Shield
    xShieldArena: I18nFormat; // %s Shield Arena
    xTeamBattle: I18nFormat; // %s Team Battle
    yearlyClassical: string; // Yearly Classical
    yearlyClassicalArena: string; // Yearly Classical Arena
    yearlyRapid: string; // Yearly Rapid
    yearlyRapidArena: string; // Yearly Rapid Arena
    yearlyX: I18nFormat; // Yearly %s
    yearlyXArena: I18nFormat; // Yearly %s Arena
  };
  ublog: {
    blogTips: string; // Our simple tips to write great blog posts
    blogTopics: string; // Blog topics
    communityBlogs: string; // Community blogs
    continueReadingPost: string; // Continue reading this post
    createBlogDiscussion: string; // Enable comments
    createBlogDiscussionHelp: string; // A forum topic will be created for people to comment on your post
    deleteBlog: string; // Delete this blog post definitively
    discussThisBlogPostInTheForum: string; // Discuss this blog post in the forum
    drafts: string; // Drafts
    editYourBlogPost: string; // Edit your blog post
    friendBlogs: string; // Friends blogs
    imageAlt: string; // Image alternative text
    imageCredit: string; // Image credit
    inappropriateContentAccountClosed: string; // Anything inappropriate could get your account closed.
    latestBlogPosts: string; // Latest blog posts
    lichessBlogPostsFromXYear: I18nFormat; // Lichess blog posts in %s
    lichessOfficialBlog: string; // Lichess Official Blog
    likedBlogs: string; // Liked blog posts
    moreBlogPostsBy: I18nFormat; // More blog posts by %s
    nbViews: I18nPlural; // %s views
    newPost: string; // New post
    noDrafts: string; // No drafts to show.
    noPostsInThisBlogYet: string; // No posts in this blog, yet.
    postBody: string; // Post body
    postIntro: string; // Post intro
    postTitle: string; // Post title
    previousBlogPosts: string; // Previous blog posts
    published: string; // Published
    publishedNbBlogPosts: I18nPlural; // Published %s blog posts
    publishHelp: string; // If checked, the post will be listed on your blog. If not, it will be private, in your draft posts
    publishOnYourBlog: string; // Publish on your blog
    safeAndRespectfulContent: string; // Please only post safe and respectful content. Do not copy someone else's content.
    safeToUseImages: string; // It is safe to use images from the following websites:
    saveDraft: string; // Save draft
    selectPostTopics: string; // Select the topics your post is about
    thisIsADraft: string; // This is a draft
    thisPostIsPublished: string; // This post is published
    uploadAnImageForYourPost: string; // Upload an image for your post
    useImagesYouMadeYourself: string; // You can also use images that you made yourself, pictures you took, screenshots of Lichess... anything that is not copyrighted by someone else.
    viewAllNbPosts: I18nPlural; // View all %s posts
    xBlog: I18nFormat; // %s's Blog
    xPublishedY: I18nFormat; // %1$s published %2$s
    youBlockedByBlogAuthor: string; // You are blocked by the blog author.
  };
  voiceCommands: {
    cancelTimerOrDenyARequest: string; // Cancel timer or deny a request
    castle: string; // Castle (either side)
    instructions1: I18nFormat; // Use the %1$s button to toggle voice recognition, the %2$s button to open this help dialog, and the %3$s menu to change speech settings.
    instructions2: string; // We show arrows for multiple moves when we are not sure. Speak the colour or number of a move arrow to select it.
    instructions3: I18nFormat; // If an arrow shows a sweeping radar, that move will be played when the circle is complete. During this time, you may only say %1$s to play the move immediately, %2$s to cancel, or speak the colour/number of a different arrow. This timer can be adjusted or turned off in settings.
    instructions4: I18nFormat; // Enable %s in noisy surroundings. Hold shift while speaking commands when this is on.
    instructions5: string; // Use the phonetic alphabet to improve recognition of chessboard files.
    instructions6: I18nFormat; // %s explains the voice move settings in detail.
    moveToE4OrSelectE4Piece: string; // Move to e4 or select e4 piece
    phoneticAlphabetIsBest: string; // Phonetic alphabet is best
    playPreferredMoveOrConfirmSomething: string; // Play preferred move or confirm something
    selectOrCaptureABishop: string; // Select or capture a bishop
    showPuzzleSolution: string; // Show puzzle solution
    sleep: string; // Sleep (if wake word enabled)
    takeRookWithQueen: string; // Take rook with queen
    thisBlogPost: string; // This blog post
    turnOffVoiceRecognition: string; // Turn off voice recognition
    voiceCommands: string; // Voice commands
    watchTheVideoTutorial: string; // Watch the video tutorial
  };
}
