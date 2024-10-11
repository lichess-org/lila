"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Προσθήκη νέου θέματος";i['advanced']="Για προχωρημένους";i['bestMove']="Παίξατε την καλύτερη κίνηση!";i['byOpenings']="Ανά άνοιγμα";i['clickToSolve']="Κάντε κλικ για να τον λύσετε";i['continueTheStreak']="Συνεχίστε το σερί νικών";i['continueTraining']="Συνέχεια εξάσκησης";i['dailyPuzzle']="Ημερήσιος Γρίφος";i['didYouLikeThisPuzzle']="Σας άρεσε αυτός ο γρίφος;";i['difficultyLevel']="Επίπεδο δυσκολίας";i['downVote']="Δε μου άρεσε ο γρίφος";i['easier']="Εύκολο";i['easiest']="Πολύ εύκολο";i['example']="Παράδειγμα";i['failed']="απέτυχε";i['findTheBestMoveForBlack']="Βρείτε την καλύτερη κίνηση για τα μαύρα.";i['findTheBestMoveForWhite']="Βρείτε την καλύτερη κίνηση για τα λευκά.";i['fromGameLink']=s("Από το παιχνίδι %s");i['fromMyGames']="Από τα παιχνίδια μου";i['fromMyGamesNone']="Δεν υπάρχουν γρίφοι από τα παιχνίδια σας στη βάση δεδομένων.\nΠαίξτε κλασικά ή rapid παιχνίδια για να αυξηθούν οι πιθανότητες προσθήκης γρίφων από τα παιχνίδια σας στη βάση δεδομένων!";i['fromXGames']=s("Γρίφοι από τα παιχνίδια του χρήστη %s");i['fromXGamesFound']=s("Βρέθηκαν %1$s γρίφοι στα παιχνίδια του χρήστη %2$s");i['goals']="Στόχοι";i['goodMove']="Καλή κίνηση";i['harder']="Δύσκολο";i['hardest']="Πολύ δύσκολο";i['hidden']="κρυφό";i['history']="Ιστορικό γρίφων";i['improvementAreas']="Τομείς βελτίωσης";i['improvementAreasDescription']="Εκπαιδευτείτε σε αυτά για να βελτιώσετε την πρόοδό σας!";i['jumpToNextPuzzleImmediately']="Μετάβαση στον επόμενο γρίφο αμέσως";i['keepGoing']="Συνεχίστε…";i['lengths']="Μήκος γρίφου";i['lookupOfPlayer']="Αναζητήστε γρίφους από τα παιχνίδια ενός παίκτη";i['mates']="Ματ";i['motifs']="Μοτίβα";i['nbPlayed']=p({"one":"%s παίχτηκε","other":"%s παίχτηκαν"});i['nbPointsAboveYourPuzzleRating']=p({"one":"Έναν πόντο πάνω από την βαθμολογία γρίφων σας","other":"%s πόντους πάνω από την βαθμολογία γρίφων σας"});i['nbPointsBelowYourPuzzleRating']=p({"one":"Έναν πόντο κάτω από τη βαθμολογία γρίφων σας","other":"%s πόντους κάτω από τη βαθμολογία γρίφων σας"});i['nbToReplay']=p({"one":"%s για επανάληψη","other":"%s για επανάληψη"});i['newStreak']="Νέο σερί νικών";i['nextPuzzle']="Επόμενος γρίφος";i['noPuzzlesToShow']="Δεν υπάρχει τίποτα εδώ, ακόμα, λύστε μερικούς γρίφους πρώτα!";i['normal']="Κανονικό";i['notTheMove']="Δεν είναι αυτή η κίνηση!";i['openingsYouPlayedTheMost']="Ανοίγματα που παίξατε πιο συχνά σε βαθμολογημένες παρτίδες σκάκι";i['origin']="Πηγή";i['percentSolved']=s("%s λυμένα");i['phases']="Φάσεις";i['playedXTimes']=p({"one":"Λύθηκε %s φορά","other":"Λύθηκε %s φορές"});i['puzzleComplete']="Ο γρίφος ολοκληρώθηκε!";i['puzzleDashboard']="Ταμπλό γρίφων";i['puzzleDashboardDescription']="Προπονηθείτε, αναλύστε, βελτιωθείτε";i['puzzleId']=s("Γρίφος %s");i['puzzleOfTheDay']="Γρίφος της ημέρας";i['puzzles']="Γρίφοι";i['puzzlesByOpenings']="Γρίφοι ανά άνοιγμα";i['puzzleSuccess']="Επιτυχία!";i['puzzleThemes']="Θέματα γρίφων";i['ratingX']=s("Βαθμολογία: %s");i['recommended']="Προτεινόμενα";i['searchPuzzles']="Αναζήτηση γρίφων";i['solved']="λύθηκε";i['specialMoves']="Ειδικές κινήσεις";i['streakDescription']="Λύστε γρίφους που γίνονται όλο και πιο δύσκολοι και χτίστε σιγά σιγά ένα «σερί νικών». Δεν υπάρχει χρόνος, οπότε μη βιάζεστε. Μια λάθος κίνηση, και το παιχνίδι τέλειωσε! Μπορείτε να παραλείψετε μια κίνηση σε κάθε γύρο.";i['streakSkipExplanation']="Παραλείψτε αυτή την κίνηση για να διατηρήσετε το σερί νικών σας! Λειτουργεί μόνο μία φορά ανά γύρο.";i['strengthDescription']="Τα πάτε καλύτερα στις εξής κατηγορίες";i['strengths']="Δυνατά σημεία";i['toGetPersonalizedPuzzles']="Για να λύνετε εξατομικευμένους γρίφους:";i['trySomethingElse']="Δοκιμάστε κάτι άλλο.";i['upVote']="Μου άρεσε ο γρίφος";i['useCtrlF']="Πατήστε Ctrl+f για να βρείτε το αγαπημένο σας άνοιγμα!";i['useFindInPage']="Πατήστε «Εύρεση στη σελίδα» στο μενού του προγράμματος περιήγησης, για να βρείτε το αγαπημένο σας άνοιγμα!";i['voteToLoadNextOne']="Ψηφίστε για να προχωρήσετε στο επόμενο!";i['yourPuzzleRatingWillNotChange']="Οι βαθμοί αξιολόγησής σας δε θα αλλάξουν. Αυτοί οι βαθμοί χρησιμεύουν στην επιλογή γρίφων για το επίπεδό σας και όχι στον ανταγωνισμό.";i['yourStreakX']=s("Το σερί νικών σας: %s")})()