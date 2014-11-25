## Lichess games CSV export

This is a direct export from lichess database.
Some fields are binary encoded.

```
nb games: > 45,000,000
file size: 16.45GB
torrent: http://sacem.lichess.org/lichess-games-2014-11-25.csv.torrent
```

### Head

```csv
_id,so,s,v,if,us,p0.ai,p0.e,p1.ai,p1.e,c,mt,w,an,ra,pg,ca
"04tbfylw",1,33,,,"[ ""miroslavm970"", ""redlightning530"" ]",,1480,,1072,0500000884000B930000000000,4665B5,true,,true,23242B55806A801C6140,2013-11-14T07:59:10.945Z
"04tbgd0m",3,31,,,,5,,,,,00060805070B0A0D0806050F0E150C1B1917141D0C0508070804080A05050F060D0608050900,true,,,1B1C52806D8022356A8076A061A040C06480DE80016A801559A0E76005C860002464846484A440646440C067403A63806384A3404BA02C5D406FA06F4C6F4453A876206FAC6F24D860056620792065A00A53A42B4C60034B609340486448640D58605F6048605960125160702052640453648D408D404D646D204960144E603D4D687620556052606920,2014-01-23T12:40:15.314Z
"04tbiq94",1,33,,,,,,,,0500003D040048DC0000000000,5564275E5B89248C6ABE7CA744D8,false,,,236D8052802453A066A06A801D3A40C01A5580020541A00C3365A05C804680748015668C66446584A58062A01C59401B,2013-04-13T04:06:27.392Z
"04tboc0g",1,33,,,"[ ""lifungkuee"" ]",,1200,,,,CC,true,,,031C22,2013-11-08T09:14:05.455Z
"04tbpis6",1,30,,,"[ ""tarn"", ""steven21ys"" ]",,1997,,1910,03020013100038E90000000000,24042424644A945A2547568758647527AB8B592647484647B85C46CA6754249BA95A678A686648544524252454245448442426442524245282454404,true,,true,1B1C132552806D806A801574A0DE800123A3406384444859A04BA06D8C6D845AA040C040C059A45984144A80564054840D4A804EA02ADF60056140D76000D860057C8064406B805644566468A0043275803B668069200350806C8061803C0A02D06000DE6002335B845B845B645B645B6462202461A02D1494405464B380B3405D60446057A0426465A04560456445A473A40B5EA00C6E200D55A00E4EA44EA46520035D2004562045A04F203C46204CA04E20632046206C204E207520462076242C3D2B3E23BF022A6F404E206648472055A84F204E50,2013-09-18T14:35:55.887Z
"04tbr8kr",1,35,2,"brqbknnr/pppppppp/8/8/8/8/PPPPPPPP/BRQBKNNR w KQkq - 0 1",,,,,,01020017C60003410000000000,44B846B6B5B5A5,false,,,1B0D1214229B409B404E402A454061A04144598044400B7440,2013-05-24T22:17:22.218Z
"04tbx6qs",1,35,,,,,,,,0508006A6900757B0000000000,86652C499B27A897D5FFCAC7EB7ADFDEBAEBEDEF7BA6CACD,true,,,22251B1C5AA06D806A801440C0558064806484A4405E80528064844CA85EA07C405580231B43800574A05640138C408C804B80025A800B43649440548458404CA441605DA059A079AC78205DA06A4068A4724072A47020,2013-11-25T22:53:22.255Z
"04tbxm1g",,30,,,,,,,,03088037A480351B0000000000,5B7A7787585BC75C58DD5C9B7DDDDA,false,,,23241A6D806A805DA05280558061A073803A6D8040C054A05C803D6D8CAD801377600A5BA04860664049A054A05040654042A07A4458607154,2013-02-18T00:55:23.182Z
"04tbxm9c",2,30,,,,,,,,05050006E5001A580000000000,0A575553294844C8AC8C6A588549574686A846CB543449A886CD,false,,,1B6D801335528076A02340C02B1D6A80149480948051405580025B805B849B405C805E805AA0548040C05A845A44254B800D244EA051802DE06005A480A4806C605B445B4C5B846464646464A46584676074805BA8682045A06BA053B4,2013-12-20T20:24:02.620Z
```

### Fields

|key|name|default|description|
|---|----|-------|-----------|
|_id|unique ID|N/A|Append this ID to http://lichess.org/ to get the game URL|
|so|source|1|How the game was created. Values on https://github.com/ornicar/lila/blob/explore/modules/game/src/main/Source.scala|
|s|status|N/A|Current status of the game. Values on https://github.com/ornicar/scalachess/blob/master/src/main/scala/Status.scala|
|v|variant|1|Chess variant in use. Values on https://github.com/ornicar/scalachess/blob/master/src/main/scala/Variant.scala|
|if|initial FEN|*1|Starting position of the game, in case of chess960 or game from a position|
|us|user IDs|[,]|User IDs of both players. If empty, both are anon. If only one element, black is anon. If first element is empty, white is anon. AI is treated as anon: see p0.ai & p1.ai fields|
|p0.ai|white AI level|-|AI level of white player. 1 to 8. Only set if white player is an AI|
|p0.e|white rating|-|Rating of white player. Empty if white is anon or AI|
|p1.ai|black AI level|-|AI level of black player. 1 to 8. Only set if black player is an AI|
|p2.e|black rating|-|Rating of black player. Empty if black is anon or AI|
|c|clock|-|Chess clock configuration, binary encoded. Empty if unlimited game. See https://github.com/ornicar/lila/blob/master/modules/game/src/main/BinaryFormat.scala#L58|
|mt|move times|N/A|Approximated move times, binary encoded. See https://github.com/ornicar/lila/blob/master/modules/game/src/main/BinaryFormat.scala#L25|
|w|winner color|-|Color of the winner player. true = white, false = black, none = no winner|
|an|analysed|false|Tells if an analysis is available for this game|
|ra|rated|false|Tells if the game is rated|
|pg|binary moves|N/A|PGN moves, binary encoded|
|ca|creation date|N/A|Date of creation of the game|


> *1 rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
