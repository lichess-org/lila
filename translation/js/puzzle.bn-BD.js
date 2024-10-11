"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="অন্য থিম যুক্ত করুন";i['advanced']="বিস্তারিত";i['bestMove']="সেরা মানের মুভ!";i['byOpenings']="ওপেনিং অনুযায়ী";i['clickToSolve']="সমাধানের জন্য ক্লিক করুন";i['continueTheStreak']="পরবর্তী ধাঁধায় যান";i['continueTraining']="প্রশিক্ষন চালিয়ে যান";i['dailyPuzzle']="দৈনিক ধাঁধা";i['didYouLikeThisPuzzle']="আপনি এই ধাঁধা তি কি পছন্দ করেছেন?";i['difficultyLevel']="কঠিনের মাত্রা";i['downVote']="ধাঁধা কে ডাউন-ভোট করুন";i['easier']="সহজ";i['easiest']="খুবই সহজ";i['example']="উদাহরণ";i['failed']="ভুল করা ধাঁদা";i['findTheBestMoveForBlack']="কালোর জন্নে সব থেকে উপযোগী চাল তি খুজে বার করুন।.";i['findTheBestMoveForWhite']="সাদার জন্নে সব থেকে উপযোগী চাল তি খুজে বার করুন।.";i['fromGameLink']=s("%s খেলা থেকে");i['fromMyGames']="আমার খেলা গুলো থেকে";i['fromMyGamesNone']="ডাটাবেজে আপনার কোনো ধাঁধা নেই।\nআপনার খেলা থেকে ধাঁধা নেয়ার সম্ভাবনা বাড়াতে র‍্যাপিড এবং ক্লাসিক খেলুন।";i['fromXGames']=s("%s এর খেলা থেকে নেয়া ধাঁধা");i['fromXGamesFound']=s("%2$s টি খেলা হতে %1$s টি ধাঁধা পাওয়া গেছে।");i['goals']="উদ্দেশ";i['goodMove']="ভালো মানের মুভ";i['harder']="কঠিন";i['hardest']="খুবই কঠিন";i['hidden']="লুকায়িত";i['history']="পূর্বে চেষ্টা করা ধাঁধা";i['improvementAreas']="উন্নতি করার জায়গা";i['improvementAreasDescription']="আপনার প্রগতির জন্য এইগুলোতে প্রশিক্ষণ করুন!";i['jumpToNextPuzzleImmediately']="তাৎক্ষনিক নতুন পাজলে চলে যাই";i['keepGoing']="চালিয়ে যান…";i['lengths']="দৈর্ঘ্য";i['lookupOfPlayer']="একজন খেলোয়াড়ের খেলা থেকে ধাঁধা দেখুন";i['mates']="চেকমেট নিদর্শন";i['motifs']="বিষয়বস্তু";i['nbPlayed']=p({"one":"%s টি খেলেছেন","other":"%s টি খেলেছেন"});i['nbPointsAboveYourPuzzleRating']=p({"one":"আপনার ধাঁধার রেটিং হতে এক পয়েন্ট বেশি।","other":"আপনার ধাঁধার রেটিং হতে %s পয়েন্ট বেশি।"});i['nbPointsBelowYourPuzzleRating']=p({"one":"আপনার ধাঁধার রেটিং হতে এক পয়েন্ট কম।","other":"আপনার ধাঁধার রেটিং হতে %s পয়েন্ট কম।"});i['nbToReplay']=p({"one":"%s টি পুনরায় খেলুন","other":"%s টি পুনরায় খেলুন"});i['newStreak']="নতুন ধাঁধার ধারা";i['nextPuzzle']="পরবর্তী ধাঁধা";i['noPuzzlesToShow']="প্রদর্শনের কিছু নেই। প্রথমে কিছু ধাঁধা খেলুন।";i['normal']="সাধারন";i['notTheMove']="এটা সঠিক চাল না!";i['openingsYouPlayedTheMost']="রেটেড খেলায় আপনি যে ওপেনিংগুলো সবথেকে বেশি খেলেছেন";i['origin']="উত্স";i['percentSolved']=s("%s সমাধান করেছেন।");i['phases']="পর্যায়";i['playedXTimes']=p({"one":"%s বার খেলা হয়েছে","other":"%s বার খেলা হয়েছে"});i['puzzleComplete']="পাজল সম্পন্ন!";i['puzzleDashboard']="পাজল ড্যাশবোর্ড";i['puzzleDashboardDescription']="প্রশিক্ষণ, বিশ্লেষণ, উন্নতি";i['puzzleId']=s("ধাঁধা %s");i['puzzleOfTheDay']="আজকের পাজল";i['puzzles']="পাজল";i['puzzlesByOpenings']="ওপেনিং অনুযায়ী ধাঁধা";i['puzzleSuccess']="সফল হয়েছে!";i['puzzleThemes']="পাজল থিম";i['ratingX']=s("রেটিংস: %s");i['recommended']="প্রস্তাবিত";i['searchPuzzles']="ধাঁধা খুজুন";i['solved']="মীমাংসিত";i['specialMoves']="বিশেশ চাল";i['streakDescription']="ধারাবাহিক জয় বজায় রাখতে ক্রমাগত কঠিন হতে থাকা ধাঁধার সমাধান করুন। কোনো সময়সীমা নেই। তবে একটি ভুল চালে খেলা শেষ। প্রতি সেশনে একটি করে চাল বাদ(skip) দেয়া যাবে।";i['streakSkipExplanation']="আপনার ধারাবাহিক জয় বজায় রাখতে এই চাল বাদ(skip) করুন। প্রতিবার প্রচেষ্টায় একবার করে এমনটা করতে পারবেন।";i['strengthDescription']="আপনি এই ধাঁচের গুলোয় ভালো খেলেন।";i['strengths']="শক্তিমত্তা";i['toGetPersonalizedPuzzles']="আপনার ব্যাক্তিগত পাজল পেতে:";i['trySomethingElse']="অন্য কিছু চেষ্টা করুন.";i['upVote']="ধাঁধা কে আপ-ভোট করুন";i['useCtrlF']="আপনার প্রিয় ওপেনিং খুঁজতে Ctrl + F ব্যবহার করুন!";i['useFindInPage']="আপনার প্রিয় ওপেনিংগুলি খুঁজতে ব্রাউসার মেনুতে \\\"ফাইন্ড ইন পেজ\\\" ব্যবহার করুন!";i['voteToLoadNextOne']="পরের ধাঁধা তি তে জেতে ভতে দিন!";i['yourPuzzleRatingWillNotChange']="আপনার ধাঁধার রেটিং পরিবর্তীত হবে না। উল্লেখ্য, ধাঁধা প্রতিযোগীতামূলক নয়। আপনার রেটিং আপনার দক্ষতা অনুযায়ী ধাঁধা বাছাই করতে সাহায্য করে।";i['yourStreakX']=s("আপনার স্কোর: %s")})()