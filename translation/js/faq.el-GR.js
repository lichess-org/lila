"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="Λογαριασμοί";i['acplExplanation']="Το εκατοστοπιόνι είναι η μονάδα μέτρησης που χρησιμοποιείται στο σκάκι ως αναπαράσταση του πλεονεκτήματος. Ένα εκατοστοπιόνι είναι ίσο με το 1/100 ενός πιονιού. Επομένως 100 εκατοστοπιόνια = 1 πιόνι. Αυτές οι αξίες δεν παίζουν επίσημο ρόλο στο παιχνίδι, αλλά είναι χρήσιμες για τους παίκτες, και απαραίτητα για τις σκακιστικές μηχανές, για την αξιολόγηση των θέσεων.\n\nΗ καλύτερη κίνηση που προτείνει μια δυνατή μηχανή θα χάσει μηδέν εκατοστοπιόνια, αλλά χειρότερες κινήσεις θα οδηγήσουν σε σταδιακή επιδείνωση της θέσης που θα μετριέται με εκατοστοπιόνια.\n\nΑυτή η τιμή μπορεί να χρησιμοποιηθεί ως ένδειξη της ποιότητας του παιχνιδιού. Όσο λιγότερα εκατοστοπιόνια χάνει κάποιος ανά κίνηση, τόσο καλύτερα παίζει.\n\nΗ ανάλυση του υπολογιστή στο Lichess τροφοδοτείται από τον Stockfish.";i['aHourlyBulletTournament']="ένα ωριαίο τουρνουά Bullet";i['areThereWebsitesBasedOnLichess']="Υπάρχουν άλλες ιστοσελίδες που βασίζονται στο Lichess;";i['asWellAsManyNMtitles']="πολλούς άλλους εθνικούς τίτλους";i['basedOnGameDuration']=s("Οι χρόνοι του Lichess βασίζονται στην εκτιμώμενη διάρκεια παιχνιδιού = %1$s. Για παράδειγμα, η εκτιμώμενη διάρκεια παιχνιδιού για ένα παιχνίδι με χρόνο 5+3 είναι 5 × 60 + 40 × 3 = 420 δευτερόλεπτα.");i['beingAPatron']="γίνετε υποστηρικτής";i['beInTopTen']="να είστε στους καλύτερους 10 σε αυτήν τη βαθμολογία.";i['breakdownOfOurCosts']="έξοδά μας αναλυτικά";i['canIbecomeLM']="Μπορώ να αποκτήσω τον τίτλο Lichess Master (LM);";i['canIChangeMyUsername']="Μπορώ να αλλάξω το όνομα χρήστη μου;";i['configure']="ρυθμίσετε";i['connexionLostCanIGetMyRatingBack']="Έχασα ένα παιχνίδι λόγω καθυστέρησης/αποσύνδεσης. Μπορώ να πάρω τους βαθμούς μου πίσω;";i['desktop']="υπολογιστής";i['discoveringEnPassant']="Γιατί μπορεί ένα πιόνι να αιχμαλωτίσει ένα αντίπαλο που βρίσκεται δίπλα του (en passant);";i['displayPreferences']="προτιμήσεις εμφάνισης";i['durationFormula']="(αρχικός χρόνος χρονομέτρου) + 40 × (προσαύξηση)";i['eightVariants']="8 παραλλαγές σκακιού";i['enableAutoplayForSoundsA']="Τα περισσότερα προγράμματα περιήγησης μπορούν να εμποδίσουν την αναπαραγωγή του ήχου σε μια πρόσφατα φορτωμένη σελίδα για την προστασία των χρηστών. Φανταστείτε εάν κάθε ιστότοπος μπορούσε να σας βομβαρδίσει ξαφνικά με ηχητικές διαφημίσεις.\n\nΤο κόκκινο εικονίδιο σίγασης εμφανίζεται όταν το πρόγραμμα περιήγησής σας εμποδίζει το lichess.org να αναπαράγει έναν ήχο. Συνήθως αυτός ο περιορισμός ενεργοποιήτε μόλις κάνετε κλικ σε κάτι. Σε ορισμένα προγράμματα περιήγησης για κινητά, η μεταφορά ενός πιονιού με άγγιγμα δεν υπολογίζεται ως κλικ. Σε αυτήν την περίπτωση, πρέπει να πατήσετε τον πίνακα για να επιτρέπεται ο ήχος στην αρχή κάθε παιχνιδιού.\n\nΔείχνουμε το κόκκινο εικονίδιο για να σας ειδοποιούμε όταν συμβεί αυτό. Συχνά μπορείτε να επιτρέψετε ρητά στο lichess.org να αναπαράγει ήχους. Ακολουθούν οδηγίες για να το κάνετε αυτό σε πρόσφατες εκδόσεις ορισμένων δημοφιλών προγραμμάτων περιήγησης.";i['enableAutoplayForSoundsChrome']="1. Μεταβείτε στο lichess.org\n2. Κάντε κλικ στο εικονίδιο κλειδώματος στη γραμμή διεύθυνσης ιστοσελίδας\n3. Κάντε κλικ στο στοιχείο Ρυθμίσεις ιστοσελίδας\n4. Να επιτρέπεται ο ήχος";i['enableAutoplayForSoundsFirefox']="1. Μεταβείτε στο lichess.org\n2. Πατήστε Ctrl-i στο Linux/Windows ή cmd-i στο MacOS\n3. Κάντε κλικ στην καρτέλα Δικαιώματα ή Άδειες\n4. Επιτρέψτε τον Ήχο και το Βίντεο στο lichess.org";i['enableAutoplayForSoundsMicrosoftEdge']="1. Κάντε κλικ στις τρεις τελείες στην επάνω δεξιά γωνία\n2. Κάντε κλικ στις Ρυθμίσεις\n3. Κάντε κλικ στην επιλογή Cookies και άδειες ιστοσελίδας\n4. Κάντε κύλιση προς τα κάτω και κάντε κλικ στην αυτόματη αναπαραγωγή πολυμέσων\n5. Προσθέστε το lichess.org στο επιτρέπω";i['enableAutoplayForSoundsQ']="Ενεργοποίηση αυτόματης αναπαραγωγής για ήχους;";i['enableAutoplayForSoundsSafari']="1. Μεταβείτε στο lichess.org\n2. Κάντε κλικ στο Safari στη γραμμή μενού\n3. Κάντε κλικ στις Ρυθμίσεις για το lichess.org ...\n4. Να επιτρέπεται η αυτόματη αναπαραγωγή";i['enableDisableNotificationPopUps']="Ενεργοποίηση ή απενεργοποίηση εμφάνισης ειδοποιήσεων;";i['enableZenMode']=s("Ενεργοποιήστε τη λειτουργία Zen-mode στις %1$s ή πατώντας το πλήκτρο %2$s κατά τη διάρκεια του παιχνιδιού.");i['explainingEnPassant']=s("Αυτή είναι μια νόμιμη κίνηση γνωστή ως «en passant» (αν πασσάν). Το άρθρο της Βικιπαίδειας δίνει μια %1$s.\n\nΠεριγράφεται στην ενότητα 3. (δ) από τoυς %2$s:\n\n«Ένα πιόνι που βρίσκεται σε ένα τετράγωνο στην ίδια γραμμή και σε διπλανή στήλη με ένα πιόνι αντιπάλου, το οποίο έχει μόλις μετακινηθεί δύο τετράγωνα (σε μία κίνηση) από το αρχικό του τετράγωνο, μπορεί να αιχμαλωτίσει το πιόνι αυτό σαν να είχε μετακινηθεί μόνο ένα τετράγωνο. Η κίνηση αυτή μπορεί να παιχτεί μόνο μετά τη μετακίνηση του αντίπαλου πιονιού και ονομάζεται \\\"en passant\\\".»\n\nΔείτε την %3$s σε αυτή την κίνηση για να εξασκηθείτε.");i['fairPlay']="Κανόνες Δικαίου Παιχνιδιού";i['fairPlayPage']="σελίδα fair play";i['faqAbbreviation']="Συχνές ερωτήσεις";i['fideHandbook']="βιβλίου κανονισμών της FIDE";i['fideHandbookX']=s("βιβλίο κανονισμών της FIDE %s");i['findMoreAndSeeHowHelp']=s("Μπορείτε να μάθετε περισσότερα για το πώς να %1$s (καθώς και για τα %2$s). Αν θέλετε να βοηθήσετε το Lichess με τον χρόνο και τις ικανότητές σας, υπάρχουν πολλοί %3$s.");i['frequentlyAskedQuestions']="Συχνές Ερωτήσεις";i['gameplay']="Παιχνίδι";i['goldenZeeExplanation']="Ο ZugAddict έκανε streaming και τις τελευταίες 2 ώρες προσπαθούσε να νικήσει το επίπεδο 8 A.I. σε ένα παιχνίδι 1+0, χωρίς επιτυχία. Ο Thibault του είπε ότι αν τα κατάφερνε, θα έπερνε ένα μοναδικό τρόπαιο. Μια ώρα αργότερα, διέλυσε τον Stockfish, και η υπόσχεση τηρήθηκε.";i['goodIntroduction']="καλή εισαγωγή";i['guidelines']="οδηγίες";i['havePlayedARatedGameAtLeastOneWeekAgo']="έχετε παίξει ένα βαθμολογημένο παιχνίδι την τελευταία εβδομάδα για αυτή τη βαθμολογία,";i['havePlayedMoreThanThirtyGamesInThatRating']="έχετε παίξει τουλάχιστον 30 βαθμολογημένα παιχνίδια σε μια δεδομένη βαθμολογία,";i['hearItPronouncedBySpecialist']="Ακούστε πώς προφέρεται από έναν ειδικό.";i['howBulletBlitzEtcDecided']="Πώς αποφασίζεται ποιοι χρόνοι είναι τι (π.χ. Bullet, Blitz);";i['howCanIBecomeModerator']="Πώς μπορώ να γίνω διαχειριστής;";i['howCanIContributeToLichess']="Πώς μπορώ να συνεισφέρω στο Lichess;";i['howDoLeaderoardsWork']="Πώς λειτουργούν οι κατατάξεις;";i['howToHideRatingWhilePlaying']="Πώς μπορώ να κρύψω τη βαθμολογία μου ενώ παίζω;";i['howToThreeDots']="Πως να...";i['inferiorThanXsEqualYtimeControl']=s("<%1$s δευτερόλεπτα = %2$s");i['inOrderToAppearsYouMust']=s("Για να φτάσετε στοn %1$s πρέπει να:");i['insufficientMaterial']="Χάνοντας από χρόνο, ισοπαλία και ανεπαρκές υλικό";i['isCorrespondenceDifferent']="Είναι το σκάκι αλληλογραφίας διαφορετικό από το κανονικό σκάκι;";i['keyboardShortcuts']="Υπάρχουν συντομεύσεις με το πληκτρολόγιο;";i['keyboardShortcutsExplanation']="Μερικές σελίδες του Lichess έχουν συντομεύσεις πληκτρολογίου που μπορείτε να χρησιμοποιήσετε. Δοκιμάστε να πατήσετε το πλήκτρο «?» σε μελέτη, ανάλυση, γρίφο, ή σελίδα παιχνιδιού, για να δείτε τα διαθέσιμα πλήκτρα.";i['leavingGameWithoutResigningExplanation']="Εάν ένας παίκτης αποχωρεί συχνά ή ακυρώνει πολλά παιχνίδια, τότε τιμωρείται και δεν μπορεί να παίξει άλλα για ένα χρονικό διάστημα. Αυτό δεν αναφέρεται δημοσίως στο προφίλ του. Εάν αυτή η συμπεριφορά συνεχιστεί, το μήκος της τιμωρίας αυξάνεται. Παρατεταμένη συμπεριφορά αυτού του είδους μπορεί να οδηγήσει σε κλείσιμο του λογαριασμού.";i['leechess']="lee-chess (λί-τσες)";i['lichessCanOptionnalySendPopUps']="Το Lichess μπορεί προαιρετικά να στείλει ειδοποιήσεις, όταν για παράδειγμα είναι η σειρά σας ή λαμβάνετε ένα ιδιωτικό μήνυμα.\n\nΚάντε κλικ στο εικονίδιο κλειδώματος δίπλα στη διεύθυνση lichess.org στη γραμμή URL του περιηγητή σας.\n\nΣτη συνέχεια, επιλέξτε αν θα επιτρέπονται ήαποκλείονται ειδοποιήσεις από το Lichess.";i['lichessCombinationLiveLightLibrePronounced']=s("Το Lichess είναι ένας συνδυασμός των «live/light/libre». Προφέρεται ως %1$s.");i['lichessFollowFIDErules']=s("Όταν ένας παίκτης ξεμένει από χρόνο, χάνει συνήθως το παιχνίδι. Ωστόσο, το παιχνίδι είναι ισόπαλο αν δεν υπάρχει σειρά νόμιμων κινήσεων με τις οποίες ο αντίπαλος μπορεί να κάνει ματ τον παίκτη αυτό (%1$s).\n\nΣε σπάνιες περιπτώσεις αυτό είναι δύσκολο να αποφασιστεί αυτόματα (αναγκαστικές γραμμές, φρούρια =). Συνήθως, είμαστε με τον παίκτη του οποίου ο χρόνος δεν έχει εξαντληθεί.\n\nΕπίσης, μπορεί να είναι δυνατόν κάποιος παίκτης να κάνει ματ με έναν ίππο ή έναν αξιωματικό εάν ο αντίπαλος έχει ένα κομμάτι που αποκλείει τις κινήσεις του βασιλιά.");i['lichessPoweredByDonationsAndVolunteers']="Το Lichess υποστηρίζεται από δωρεές των χρηστών του (patrons) καθώς και από τις προσπάθειες μιας ομάδας εθελοντών.";i['lichessRatings']="Αξιολογήσεις Lichess";i['lichessRecognizeAllOTBtitles']=s("Το Lichess αναγνωρίζει όλους τους τίτλους FIDE που αποκτήθηκαν OTB (Over The Board), καθώς και %1$s. Εδώ είναι μια λίστα με όλους τίτλους FIDE:");i['lichessSupportChessAnd']=s("Εκτός από το κανονικό σκάκι, το Lichess υποστηρίζει και %1$s.");i['lichessTraining']="εκπαίδευση του Lichess";i['lMtitleComesToYouDoNotRequestIt']="Αυτός ο τιμητικός τίτλος είναι ανεπίσημος και υπάρχει μόνο στο Lichess.\n\nΤον απονέμουμε σε ιδιαίτερα αξιόλογους παίκτες που είναι καλοί «πολίτες» του Lichess, κατά τη γνώμη μας. Δεν μπορείτε να πάρετε τον τίτλο LM, ο τίτλος LM έρχεται σε σας. Εάν σας επιλέξουμε, θα λάβετε ένα μήνυμα από εμάς σχετικά με αυτό, αλλά και την επιλογή να δεχτείτε ή να αρνηθείτε.\n\nΜην ζητάτε τον τίτλο LM.";i['mentalHealthCondition']="αυτόνομη κατάσταση ψυχικής υγείας";i['notPlayedEnoughRatedGamesAgainstX']=s("Ο παίκτης δεν έχει ολοκληρώσει αρκετά βαθμολογημένα παιχνίδια εναντίον %1$s στην κατηγορία βαθμολογίας.");i['notPlayedRecently']="Ο παίκτης δεν έχει παίξει αρκετά πρόσφατα παιχνίδια. Ανάλογα με τον αριθμό των παιχνιδιών που έχετε παίξει, μπορεί να χρειαστεί περίπου ένα χρόνο αδράνειας για να γίνει ξανά προσωρινή η βαθμολογία σας.";i['notRepeatedMoves']="Δεν επαναλάβαμε κινήσεις. Γιατί το παιχνίδι έληξε ως ισόπαλο λόγω τριπλής επανάληψης;";i['noUpperCaseDot']="Όχι.";i['otherWaysToHelp']="άλλοι τρόποι για να βοηθήσετε";i['ownerUniqueTrophies']=s("Αυτό το τρόπαιο είναι μοναδικό στην ιστορία του Lichess, και κανείς άλλος από τον %1$s δεν θα το πάρει ποτέ.");i['pleaseReadFairPlayPage']=s("Για περισσότερες πληροφορίες, διαβάστε τη %s");i['positions']="θέσεις";i['preventLeavingGameWithoutResigning']="Ποιες είναι οι επιπτώσεις στους παίκτες που φεύγουν από την παρτίδα χωρίς να παραιτηθούν;";i['provisionalRatingExplanation']="Το ερωτηματικό σημαίνει ότι η βαθμολογία είναι προσωρινή. Οι λόγοι συμπεριλαμβάνουν:";i['ratingDeviationLowerThanXinChessYinVariants']=s("έχετε απόκλιση βαθμολογίας μικρότερη από %1$s στο κανονικό σκάκι και από %2$s σε παραλλαγές,");i['ratingDeviationMorethanOneHundredTen']="Συγκεκριμένα, σημαίνει ότι η απόκλιση Glicko-2 είναι μεγαλύτερη από 110. Η απόκλιση είναι το επίπεδο εμπιστοσύνης που έχει το σύστημα στην βαθμολογία. Όσο χαμηλότερη είναι η απόκλιση, τόσο πιο σταθερή είναι η βαθμολογία.";i['ratingLeaderboards']="πίνακα βαθμολογίας";i['ratingRefundExplanation']="Ένα λεπτό μετά την αναφορά ενός παίκτη, εξετάζονται τα 40 τελευταία βαθμολογημένα παιχνίδια του τις τελευταίες 3 ημέρες. Αν είστε αντίπαλος σε αυτά τα παιχνίδια, έχετε χάσει πόντους (ήττα ή ισοπαλία) και η βαθμολογία σας δεν ήταν προσωρινή, μπορείτε να πάρετε αυτούς τους πόντους πίσω. Η επιστροφή πόντων είναι προσαρμοσμένη με βάση την κορυφαία βαθμολογία σας και την πρόοδό της μετά το παιχνίδι.\n(Για παράδειγμα, αν η βαθμολογία σας αυξηθεί σημαντικά μετά από αυτά τα παιχνίδια, μπορεί να επιστραφούν μερικοί ή καθόλου πόντοι. Ποτέ δε θα σας επιστραφούν περισσότεροι από 150 πόντοι.";i['ratingSystemUsedByLichess']="Οι βαθμολογίες υπολογίζονται με τη μέθοδο βαθμολόγησης Glicko-2 που αναπτύχθηκε από τον Mark Glickman. Είναι μια πολύ δημοφιλής μέθοδος και χρησιμοποιείται από έναν σημαντικό αριθμό σκακιστικών οργανισμών (η FIDE είναι ένα αξιοσημείωτο αντίθετο παράδειγμα, καθώς χρησιμοποιεί το σύστημα αξιολόγησης Elo).\n\nΟι αξιολογήσεις Glicko χρησιμοποιούν «διαστήματα εμπιστοσύνης» κατά τον υπολογισμό και την εκπροσώπηση της βαθμολογίας σας. Όταν αρχίσετε να χρησιμοποιείτε το Lichess, η βαθμολογία σας ξεκινά από 1500 ± 700. Το 1500 αντιπροσωπεύει την αξιολόγησή σας, και το 700 το διάστημα εμπιστοσύνης.\n\nΔηλαδή, το σύστημα είναι 90% βέβαιο ότι η βαθμολογία σας είναι κάπου μεταξύ 800 και 2200. Πρακτικά όμως, αυτό είναι αβέβαιο. Έτσι, όταν ένας παίκτης ξεκινάει να παίζει, η βαθμολογία του θα μεταβάλλεται κατά πολύ μετά το τέλος κάθε παρτίδας, κάποιες φορές και παραπάνω από εκατό μονάδες. Αλλά μετά από μερικά παιχνίδια εναντίον «εδραιωμένων» παικτών, το διάστημα εμπιστοσύνης και ο αριθμός των πόντων που κερδίζονται/χάνονται μετά από κάθε παιχνίδι θα μειωθούν.\n\nΕπιπλέον, καθώς περνάει ο χρόνος, το διάστημα εμπιστοσύνης θα αυξάνεται. Αυτό σας επιτρέπει να κερδίσετε/χάσετε πόντους πιο γρήγορα, έτσι ώστε το επίπεδό σας να αντιστοιχεί με τους πόντους που έχετε.";i['repeatedPositionsThatMatters']=s("Η τριπλή επανάληψη αφορά %1$s, όχι κινήσεις. Οι επαναλήψεις δε χρειάζεται να είναι διαδοχικές.");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="Η 2η απαίτηση υπάρχει για να εμποδίσει τους παίκτες που δεν είναι ενεργοί από το να εμφανίζονται στους πίνακες βαθμολογιών.";i['showYourTitle']=s("Αν έχετε έναν τίτλο OTB, μπορείτε να κάνετε αίτηση για να τον εμφανίσετε στο λογαριασμό σας συμπληρώνοντας τη %1$s, όπου θα πρέπει να επισυνάψετε μια καθαρή εικόνα από το σχετικό έγγραφο καθώς και μια φωτογραφία του εαυτού σας ενώ το κρατάτε.\n\nΕάν το Lichess έχει επαληθεύσει τον τίτλο σας, τότε μπορείτε να συμμετέχετε σε τουρνουά Titled Arena.\n\nΤέλος, υπάρχει ένας τιμητικός τίτλος %2$s.");i['similarOpponents']="αντίπαλοι με παρόμοια απόδοση";i['stopMyselfFromPlaying']="Να σταματήσω τον εαυτό μου από το να παίζει;";i['superiorThanXsEqualYtimeControl']=s("≥ %1$s δευτερόλεπτα = %2$s");i['threeFoldHasToBeClaimed']=s("Τριπλή επανάληψη πρέπει να ισχυριστεί ένας από τους δύο παίκτες. Μπορείτε να το κάνετε πατώντας το κουμπί που εμφανίζεται, ή προσφέροντας ισοπαλία πριν την τελική σας κίνηση. Δεν έχει σημασία αν ο αντίπαλός σας απορρίψει την προσφορά, το αίτημα τριπλής επανάληψης θα γίνει ούτως ή άλλως. Μπορείτε επίσης να %1$s το Lichess να κάνει αυτόματα αιτήματα τριπλής επανάληψης. Επιπλέον, η πενταπλή επανάληψη λήγει αμέσως το παιχνίδι.");i['threefoldRepetition']="Τριπλή επανάληψη";i['threefoldRepetitionExplanation']=s("Αν μια θέση επαναληφθεί τρεις φορές, οι παίκτες μπορούν να ισχυριστούν ισοπαλία από τον κανόνα της %1$s. Το Lichess εφαρμόζει τους επίσημους κανόνες FIDE, όπως περιγράφονται στο άρθρο 9.2 του %2$s.");i['threefoldRepetitionLowerCase']="τριπλή επανάληψη";i['titlesAvailableOnLichess']="Τι είδους τίτλοι υπάρχουν στο Lichess;";i['uniqueTrophies']="Μοναδικά τρόπαια";i['usernamesCannotBeChanged']="Όχι, τα ονόματα χρηστών δεν μπορούν να αλλάξουν για τεχνικούς και πρακτικούς λόγους. Τα ονόματα χρηστών υλοποιούνται σε πάρα πολλές θέσεις: βάσεις δεδομένων, εξαγωγές, αρχεία καταγραφής και στο μυαλό των ανθρώπων. Μπορείτε να προσαρμόσετε την κεφαλαιοποίηση μία φορά.";i['usernamesNotOffensive']=s("Γενικά, τα ονόματα χρηστών δεν πρέπει να είναι προσβλητικά, να μιμούνται κάποιο άλλο άτομο ή να είναι διαφημίσεις. Μπορείτε να διαβάσετε περισσότερα στις %1$s.");i['verificationForm']="φόρμα επαλήθευσης";i['viewSiteInformationPopUp']="Προβολή πληροφοριών ιστότοπου σε αναδυόμενο παράθυρο";i['watchIMRosenCheckmate']=s("Παρακολουθήστε τον Διεθνή Μετρ Eric Rosen να κάνει ματ %s.");i['wayOfBerserkExplanation']=s("Για να το πάρει, ο hiimgosu προκάλεσε τον εαυτό του να παίξει με berserk και να κερδίσει το 100%% των παιχνιδιών σε %s.");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="Δυστυχώς, δεν μπορούμε να δώσουμε πίσω τους πόντους που χάσατε λόγω καθυστέρησης ή αποσύνδεσης, ανεξάρτητα από το αν το πρόβλημα ήταν δικό μας (πολύ σπάνιο) ή δικό σας. Επίσης, όταν το Lichess επανεκκινεί, τα παιχνίδια ματαιώνονται αυτόματα.";i['weRepeatedthreeTimesPosButNoDraw']="Η θέση έχει επαναληφθεί τρεις φορές. Γιατί δεν έχει γίνει ισοπαλία;";i['whatIsACPL']="Τι είναι η μέση απώλεια εκατοστοπιονιού (ACPL);";i['whatIsProvisionalRating']="Γιατί υπάρχει ένα ερωτηματικό (?) δίπλα στην βαθμολογία;";i['whatUsernameCanIchoose']="Τι μπορεί να περιλαμβάνει το όνομα χρήστη μου;";i['whatVariantsCanIplay']="Ποιες είναι οι διάφορες εκδοχές που μπορώ να παίξω στο Lichess;";i['whenAmIEligibleRatinRefund']="Πότε μπορώ να πάρω πίσω τις μονάδες elo που έχασα παίζοντας με κλέφτες;";i['whichRatingSystemUsedByLichess']="Ποιοι σύστημα αξιολόγησης χρησιμοποιεί το Lichess;";i['whyAreRatingHigher']="Γιατί οι βαθμολογίες είναι ψηλότερες σε σχέση με άλλες ιστοσελίδες και οργανισμούς όπως οι FIDE, USCF και ICC;";i['whyAreRatingHigherExplanation']="Είναι καλύτερο να μη σκεφτόμαστε τις αξιολογήσεις ως απόλυτους αριθμούς ή να τις συγκρίνουμε με άλλους οργανισμούς. Διαφορετικοί οργανισμοί έχουν διαφορετικά επίπεδα παικτών και διαφορετικά συστήματα διαβάθμισης (Elo, Glicko, Glicko-2, ή μια τροποποιημένη έκδοση αυτών). Οι παράγοντες αυτοί μπορεί να επηρεάσουν τους απόλυτους αριθμούς (αξιολογήσεις).\n\nΕίναι καλύτερο να σκεφτείτε τις βαθμολογίες ως \\\"σχετικές\\\" μορφές: μέσα σε μια ομάδα παικτών, οι σχετικές διαφορές τους στις βαθμολογίες θα σας βοηθήσουν να υπολογίσετε ποιος θα κερδίσει/θα κάνει ισοπαλία/θα χάσει και πόσο συχνά. Το να λέτε \\\"Η βαθμολογία μου είναι Χ\\\" δε σημαίνει τίποτα αν δεν υπάρχουν κι άλλοι παίκτες με διαφορετικές βαθμολογίες με τους οποίους μπορείτε να συγκριθείτε.";i['whyIsLichessCalledLichess']="Γιατί το Lichess ονομάζεται Lichess;";i['whyIsLilaCalledLila']=s("Όμοια, ο πηγαίος κώδικας για το Lichess, %1$s, σημαίνει li[chess in sca]la (lichess σε scala), αφού ένα αρκετά μεγάλο μέρος του Lichess είναι γραμμένο στην %2$s, μια διαισθητική γλώσσα προγραμματισμού.");i['whyLiveLightLibre']="Ζωντανά, επειδή τα παιχνίδια παίζονται και παρακολουθούνται σε πραγματικό χρόνο 24/7, αποδοτικό και καινοτόμο, καθώς είναι ανοικτού κώδικα και αποφεύγει, έτσι, τα προβλήματα με τον κλειστό κώδικα που μαστίζουν άλλες ιστοσελίδες.";i['yesLichessInspiredOtherOpenSourceWebsites']=s("Ναι. Το Lichess έχει πράγματι εμπνεύσει πολλούς άλλους ιστότοπους ανοιχτού κώδικα που χρησιμοποιούν τον %1$s, το %2$s ή τη %3$s.");i['youCannotApply']="Δεν μπορείτε να κάνετε αίτηση για να γίνετε διαχειριστής. Αν δούμε κάποιον που πιστεύουμε ότι θα ήταν καλός διαχειριστής θα επικοινωνήσουμε απευθείας μαζί τους.";i['youCanUseOpeningBookNoEngine']="Στο Lichess, η κύρια διαφορά στους κανόνες για το σκάκι αλληλογραφίας είναι ότι επιτρέπεται ένα opening book. Η χρήση μηχανών εξακολουθεί να απαγορεύεται και μπορεί να έχει ως αποτέλεσμα το κλείσιμο του λογαριασμού σας. Αν και το ICCF επιτρέπει τη χρήση μηχανών, το Lichess δεν το κάνει."})()