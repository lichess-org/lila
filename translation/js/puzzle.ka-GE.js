"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="დაამატე კიდევ მაგალითი";i['advanced']="მოწინავე, გაძლიერებული";i['bestMove']="საუკეთესო სვლა!";i['byOpenings']="დებიუტების მიხედვით";i['clickToSolve']="დააჭირე ამოხსნისთვის";i['continueTheStreak']="გააგრძელე ჯაჭვი";i['continueTraining']="გააგრძელე ვარჯიში";i['dailyPuzzle']="ყოველდღიური ამოცანა";i['didYouLikeThisPuzzle']="მოგეწონა ეს ამოცანა?";i['difficultyLevel']="სირთელის დონე";i['downVote']="არ აირჩიო ამოცანა";i['easier']="უფრო იოლი";i['easiest']="უიოლესი, უმარტივესი";i['example']="მაგალითი";i['failed']="არასწორია";i['findTheBestMoveForBlack']="იპოვნე შავების საუკეთესო სვლა.";i['findTheBestMoveForWhite']="იპოვნე თეთრების საუკეთესო სვლა.";i['fromGameLink']=s("პარტიიდან %s");i['fromMyGames']="ჩემი პარტიებიდან";i['fromMyGamesNone']="თქვენ არ გაქვთ ამოცანები თქვენს ბაზაში , მაგრამ ლიჩეესს ჯერ კიდევ ძალიან უყვარხარ.\nითამაშე სწრაფი და კლასიკური პარტიები და გაზარდე შანსი გქონდეს საკუთრივ დამატებული ამოცანები.";i['fromXGames']=s("ამოცანები %s პარტიებიდან");i['fromXGamesFound']=s("%1$s ამოცანები იპოვნა %2$s პარტიებში");i['goals']="მიზნები";i['goodMove']="კარგი სვლა";i['harder']="უფრო ძნელი";i['hardest']="უძნელესი";i['hidden']="დამალული";i['history']="ამოცანების ისტორია";i['improvementAreas']="წვრთნისა და გაუმჯობესების ადგილი";i['improvementAreasDescription']="ივარჯიშე რომ გააუმჯობესო შენი წინსვლა!";i['jumpToNextPuzzleImmediately']="შემდეგ ამოცანაზე დაუყონებრივ გადასვლა";i['keepGoing']="ასე გაგრძელე…";i['lengths']="სიგრძე";i['lookupOfPlayer']="დაათვალიერე ამოცანები მოთამაშეთა პარტიებიდან";i['mates']="შამათები";i['motifs']="მოტივები";i['newStreak']="ახალი ჯაჭვი";i['nextPuzzle']="შემდეგი ამოცანა";i['noPuzzlesToShow']="არაფერი საჩვენებელი , პირველად  ამოხსენი რამოდენიმე ამოცანა";i['normal']="ნორმალური, ჩვეულებრივი";i['notTheMove']="ეს არ არის სვლა!";i['openingsYouPlayedTheMost']="დებიუტები რომლებსაც თქვენ თამაშობთ რეიტინგიან პარტიებში";i['origin']="წარმოშობა";i['percentSolved']=s("%s ამოხსნილია");i['phases']="ფაზები, ეტაპები";i['playedXTimes']=p({"one":"ნათამაშები %s დრო","other":"ნატამაშები %s ჯერ"});i['puzzleComplete']="ამოცანა ამოხსნილია!";i['puzzleDashboard']="ამოცანების საინფორმაციო დაფა";i['puzzleDashboardDescription']="ვარჯიში, ანალიზი, გაუმჯობესება";i['puzzleId']=s("ამოცანა %s");i['puzzleOfTheDay']="ყოველდიური სავარჯიშოები";i['puzzles']="ამოცანები";i['puzzlesByOpenings']="ამოცანები დებიუტების მიხედვით";i['puzzleSuccess']="წარმატება!";i['puzzleThemes']="lichess. org / ვარჯიში/თემები\nამოცანების თემები";i['ratingX']=s("Რეიტინგი: %s");i['recommended']="გირჩევთ და გთავაზობთ";i['searchPuzzles']="მოძებნე ამოცანები";i['solved']="ამოხსნილია";i['specialMoves']="განსაკუთრებული სვლა";i['streakDescription']="ამოხსენი თანდათანობით უფრო და უფრო ძნელი ამოცანები და შექმენი შენი მომგებიანი ჯაჭვი. საათი არ გაქვს ჩართული, ასე რომ გამოიყენე შენი დრო. ერთი შეცდომა და პარტიაც დამთავრებულია! მაგრამ შეგიძლია გამოტოვო მხოლოდ ერთი სვლა ერთი ცდის განმავლობაში.";i['streakSkipExplanation']="გამოტოვე ეს სვლა და შეინარჩუნე შენი ჯაჭვი! მუშაობს მხოლოდ ერთხელ მიმდინარე ვარჯიშისას.";i['strengthDescription']="თქვენ საუკეთესოდ შეასრულეთ ეს თემები";i['strengths']="გაძლიერება";i['toGetPersonalizedPuzzles']="მიიღე ინდივიდუალური ამოცანები:";i['trySomethingElse']="სცადე რაიმე სხვა.";i['upVote']="აირჩიე სავარჯიშო";i['useCtrlF']="გამოიყენეთ Ctrl+f რომ იპოვოთ თქვენი საყვარელი დებიუტი!";i['useFindInPage']="გამოიყენეთ \\\"find in page\\\" ბროუსერში, რომ იპოვოთ თქვენი საყვარელი დებიუტი!";i['voteToLoadNextOne']="აირჩიე და ჩატვირთე შემდეგი!";i['yourPuzzleRatingWillNotChange']="თქვენი ამოცანების რეიტინგი არ შეიცვალა.. ამოცანების ამოხსნა არ გახლავთ შეჯიბრი. თქვენი რეიტინგი გვეხმარება თქვენი ამჟამინდელი დონისთვის შეგირჩიოთ საუკეთესო სავარჯიშო.";i['yourStreakX']=s("თქვენი ჯაჭვი %s")})()