package shogi
package format
package csa

// from https://github.com/mogproject/mog-core-scala/tree/45232a53d29b5f52c90bc6b153b53c5dd742c937/jvm/src/test/resources/csa/game
object CsaFixtures {
  val csa1 = """V2.2
N+鈴木大介 九段
N-深浦康市 九段
$EVENT:王座戦
$SITE:東京・将棋会館
$START:2017-03-22T01:00:00.000Z
$OPENING:中飛車
P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
P2 * -HI *  *  *  *  * -KA * 
P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
P4 *  *  *  *  *  *  *  *  * 
P5 *  *  *  *  *  *  *  *  * 
P6 *  *  *  *  *  *  *  *  * 
P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
P8 * +KA *  *  *  *  * +HI * 
P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
+
+7776FU
-8384FU
+5756FU
-7162GI
+2858HI
-5142OU
+5948OU
-8485FU
+8877KA
-7374FU
+7968GI
-6273GI
+6766FU
-4232OU
+6867GI
-3142GI
+4838OU
-8272HI
+3828OU
-7364GI
+5878HI
-7475FU
+7788KA
-7576FU
+6776GI
-3334FU
+3938GI
-5354FU
+1716FU
-0077FU
+7877HI
-2266KA
+7667GI
-6677UM
+8877KA
-2133KE
+1615FU
-4131KI
+7768KA
-3122KI
+6846KA
-6162KI
+0065FU
-6473GI
+0074FU
-7382GI
+4682UM
-7282HI
+0046KA
-6364FU
+4664KA
-8284HI
+6491UM
-0088KA
+0075GI
-8483HI
+9192UM
-8353HI
+9281UM
-3345KE
+8171UM
-0061FU
+6758GI
-8899UM
+7564GI
-5351HI
+7473TO
-6273KI
+0053KY
-5121HI
+5352NY
-4233GI
+6473NG
-2324FU
+7153UM
-9989UM
+5354UM
-8956UM
+0055KE
-0031KE
+0067KI
-5667UM
+5867GI
-3223OU
+5445UM
-0044KI
+4563UM
-0057HI
+5242NY
-5767RY
+6341UM
-0032KY
+6958KI
-6766RY
+4231NY
-0057FU
+0045KE
-4445KI
+5543NK
-0016KE
+1916KY
-6616RY
+4332NK
-0019GI
+2839OU
-2425FU
+3222NK
-2324OU
+4123UM
-2435OU
+2333UM
%TORYO"""

  val csa2 = """V2.2
N+elmo YaneuraOu 4.57
N-yaselmo YaneuraOu 4.73
P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
P2 * -HI *  *  *  *  * -KA *.
P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
P4 *  *  *  *  *  *  *  *  *.
P5 *  *  *  *  *  *  *  *  *.
P6 *  *  *  *  *  *  *  *  *.
P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
P8 * +KA *  *  *  *  * +HI *.
P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
P+
P-
+
+2726FU,T102
'45
'P2g-2f P8c-8d P7g-7f P3c-3d G6i-7h P8d-8e P2f-2e G4a-3b P2e-2d P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f R2d-2h P*2c P*8g R8f-8b B8h-7g B2bx7g+ N8ix7g S3a-2b S7i-8h K5a-4b S3i-3h P7c-7d P3g-3f S7a-7b K5i-6h P1c-1d N2i-3g N8a-7c P*2d P2cx2d R2hx2d
-8384FU,T88
'44
'P8c-8d P7g-7f P3c-3d P2f-2e P8d-8e G6i-7h G4a-3b S3i-3h S7a-7b P2e-2d P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f R2d-2f P*2c P*8g R8f-8b K5i-6h K5a-4b P3g-3f P*8f P8gx8f R8bx8f R2f-2e P*8g B8h-7g B2bx7g+ N8ix7g S3a-2b N2i-3g
+2625FU,T83
'66
'P2f-2e P3c-3d P2e-2d P2cx2d R2hx2d G4a-3b G6i-7h P8d-8e R2d-2f P*2c P7g-7f P8e-8f P8gx8f R8bx8f P*8g R8f-8b S3i-3h S7a-7b K5i-6h P7c-7d P3g-3f S7b-7c P3f-3e P3dx3e B8h-7g B2bx7g+ N8ix7g S3a-2b N2i-3g S2b-3c S7i-8h K5a-4b P9g-9f
-8485FU,T81
'57
'P8d-8e P7g-7f P3c-3d G6i-7h G4a-3b P2e-2d P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f S3i-3h S7a-7b R2d-2h P*2c P*8g R8f-8d P3g-3f K5a-4b K5i-6h P7c-7d P*2d P2cx2d R2hx2d P*8f P8gx8f R8dx8f N2i-3g N8a-7c R2dx3d R8fx7f B8hx2b+ S3ax2b
+7776FU,T176
'66
'P7g-7f P3c-3d G6i-7h P8e-8f P8gx8f R8bx8f P2e-2d P2cx2d R2hx2d G4a-3b K5i-5h R8f-8b P*8g P*2c R2dx3d B2bx8h+ S7ix8h S3a-2b N8i-7g B*2g B*3f B2gx3f+ R3dx3f B*2g N7g-6e B2gx3f+ P3gx3f S7a-6b B*4f S2b-3c S3i-3h K5a-5b
-3334FU,T2
'25
'P3c-3d G6i-7h G4a-3b P2e-2d P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f K5i-6h R8f-8b P*8g P*2c R2dx3d B2bx8h+ S7ix8h B*2g S3i-3h B2g-4e+ R3d-3f S3a-2b P9g-9f K5a-4b S8h-7g S7a-7b K6h-7i S2b-3c P4g-4f +B4e-5d R3f-2f P7c-7d P3g-3f P6c-6d N2i-3g N8a-7c S3h-4g R8b-8a
+6978KI,T60
'56
'G6i-7h G4a-3b P2e-2d P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f K5i-6h R8f-8b P*8g P*2c R2d-2h S7a-7b S3i-3h K5a-4b P3g-3f P7c-7d P*2d P2cx2d R2hx2d P*2c R2dx3d S7b-7c P9g-9f B2bx8h+ S7ix8h B*2h B*3g B2hx3g+ N2ix3g
-4132KI,T22
'51
'G4a-3b P2e-2d P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f R2d-2h P*2c P*8g R8f-8b S3i-3h S7a-7b P3g-3f P7c-7d S3h-3g S7b-7c S3g-4f S7c-6d S4f-4e G3b-3c P9g-9f P4c-4d S4e-5f P1c-1d N2i-3g G6a-5b K5i-6h K5a-4b
+2524FU,T60
'92
'K5i-6h P8e-8f P8gx8f R8bx8f P2e-2d P2cx2d R2hx2d R8f-8b P*8g P*2c R2d-2e P1c-1d P9g-9f S7a-7b S3i-3h K5a-6b P4g-4f P7c-7d P*2d B2bx8h+ S7ix8h N2a-3c R2e-2h P2cx2d R2hx2d S3a-2b N8i-7g N8a-7c
-2324FU,T2
'27
'P2cx2d R2hx2d P8e-8f P8gx8f R8bx8f K5i-6h R8f-8b P*8g P*2c R2dx3d B2bx8h+ S7ix8h B*2g S3i-3h B2g-4e+ R3d-3f S3a-2b S8h-7g K5a-4b R3f-2f S2b-3c P3g-3f S7a-7b N2i-3g +B4e-5d G4i-4h P4c-4d K6h-7i P7c-7d R2f-2i N8a-7c P4g-4f P6c-6d S3h-4g P7d-7e
+2824HI,T60
'106
'R2hx2d P8e-8f P8gx8f R8bx8f K5i-6h R8f-8b P*8g P*2c R2d-2e B2b-4d S3i-3h S3a-4b P9g-9f N2a-3c R2e-2h S7a-7b P3g-3f P7c-7d P*2d P2cx2d R2hx2d P*2c R2dx3d S7b-7c P*2d P2cx2d
-8586FU,T2
'14--
'P8e-8f P8gx8f R8bx8f R2dx3d B2b-3c R3d-3f R8f-8d B8hx3c+ N2ax3c S7i-8h S3a-2b N8i-7g K5a-5b
+8786FU,T60
'98
'P8gx8f R8bx8f K5i-6h R8f-8b P*8g P*2c R2dx3d B2bx8h+ S7ix8h B*2g S3i-3h B2g-4e+ R3d-3f S3a-2b B*5f +B4ex3f P3gx3f S7a-7b P4g-4f S2b-3c B*6f P7c-7d P3f-3e N8a-7c P3e-3d S3c-4d P4f-4e
-8286HI,T32
'40
'R8bx8f K5i-6h R8fx7f B8h-7g R7f-7d R2d-2f R7d-8d S7i-8h P*2c S3i-3h K5a-5b P3g-3f P7c-7d P3f-3e P7d-7e P3ex3d S7a-7b K6h-5h R8dx3d R2f-8f P*8d R8f-5f N2a-3c P*3e R3dx3e B7g-5e
+5968OU,T60
'61
'K5i-6h R8f-8b P*8g P*2c R2dx3d B2bx8h+ S7ix8h S3a-2b R3d-3f B*2g S3i-3h B2gx3f+ P3gx3f S7a-7b N2i-3g S2b-3c G7h-7i P6c-6d P4g-4f S7b-6c P9g-9f P9c-9d K6h-7h
-8676HI,T132
'53
'R8f-8e P*8g S7a-7b P9g-9f K5a-4b R2d-2h P*2c S3i-3h P7c-7d P4g-4f N8a-7c P3g-3f P6c-6d N2i-3g R8e-8a S3h-4g S7b-6c G4i-3h G6a-7b R2h-2i K4b-5b P9f-9e B2b-3c P6g-6f S3a-2b K6h-5h
+8877KA,T61
'67
'B8h-7g R7f-7d S7i-8h R7d-8d R2d-2f P*2c S3i-3h K5a-5b B7gx2b+ S3ax2b N8i-7g N2a-3c R2f-5f S7a-6b P*8e R8d-8b R5f-8f B*6d R8f-3f K5b-4a R3fx3d P*8f R3d-3f K4a-3a
-7674HI,T60
'22++
'R7f-7d R2d-2f R7d-8d S7i-8h P*2c S3i-3h K5a-4b P3g-3f R8d-8b P3f-3e P3dx3e K6h-5h P1c-1d P9g-9f S7a-6b R2f-8f B2bx7g+ S8hx7g R8bx8f S7gx8f P*8h
+3938GI,T60
'23
'S3i-3h P*2c R2d-2f R7d-8d S7i-8h K5a-5b B7gx2b+ S3ax2b N8i-7g S2b-3c P*8e R8d-8b R2f-8f B*6d R8f-7f P1c-1d P6g-6f P5c-5d P6f-6e B6d-4b S8h-8g S3c-4d P4g-4f S7a-6b P3g-3f P7c-7d R7fx7d S6b-7c R7dx5d P*5c
-0023FU,T61
'44
'P*2c R2d-2f R7d-8d S7i-8h K5a-4b P3g-3f P1c-1d N2i-3g P*8f P3f-3e P3dx3e N3g-4e P4c-4d P*3c N2ax3c N4ex3c+ B2bx3c K6h-5h B3c-1e R2f-7f N*6d R7f-7e P3e-3f R7ex1e P1dx1e B7gx4d P3f-3g+ S3hx3g
+2426HI,T60
'53
'R2d-2f R7d-8d S7i-8h K5a-4b P3g-3f G6a-5a P9g-9f R8d-8b P*8f S7a-6b S8h-8g P1c-1d N2i-3g B2bx7g+ N8ix7g S3a-2b P4g-4f P7c-7d R2f-2i S2b-3c G4i-4h S3c-4d P4f-4e S4d-5e N7g-8e S5e-6d B*7g
-7484HI,T60
'23
'R7d-8d S7i-8h S7a-6b P*2d P2cx2d R2fx2d P*2c R2d-2e R8d-8b P3g-3f P7c-7d N2i-3g S6b-7c P*8f S7c-6d R2e-8e R8bx8e P8fx8e P*8b P8e-8d K5a-4b P8d-8c+ B2bx7g+ N8ix7g P8bx8c R*8b B*7b
+7988GI,T60
'85
'S7i-8h K5a-4b P3g-3f G6a-5a P9g-9f R8d-8b P*8f B2bx7g+ N8ix7g S3a-2b S8h-8g S7a-6b N2i-3g S2b-3c P4g-4f P7c-7d P3f-3e B*4d N3g-4e S3c-2d R2f-2i S2dx3e S3h-4g N8a-7c G4i-4h
-5152OU,T60
'30--
'K5a-5b P3g-3f S7a-7b P3f-3e P7c-7d P3ex3d P7d-7e K6h-5h N8a-7c P*8f R8d-5d R2f-2e B2bx7g+ N8ix7g B*6d B*6f S3a-2b P*7d B6dx1i+ P7dx7c+ +B1ix7c R2ex7e +B7c-6d R7e-3e +B6dx8f P*7d
+3736FU,T61
'113
'P3g-3f P1c-1d N2i-3g S7a-7b B7gx2b+ S3ax2b P3f-3e P7c-7d B*6f B*7e P*8e R8dx8e N8i-7g R8e-8b B6fx7e P7dx7e R2f-5f B*5d B*6e K5b-4b B6ex5d P5cx5d N3g-4e
-6172KI,T60
'1--
'G6a-7b N2i-3g S7a-6b P9g-9f P1c-1d P*8f B2bx7g+ S8hx7g S3a-2b B*6f R8d-8b P3f-3e P6c-6d P3ex3d G7b-6c R2f-3f P6d-6e B6f-5e
+2937KE,T61
'53
'N2i-3g S7a-6b P9g-9f P1c-1d P*8f B2bx7g+ S8hx7g S3a-2b P4g-4f P9c-9d P*2d P2cx2d R2fx2d P*2c R2d-2e P7c-7d P3f-3e B*3f R2e-2f P3dx3e P*2d P7d-7e P2dx2c+ S2bx2c B*5e
-7162GI,T64
'1
'S7a-6b P4g-4f P1c-1d P9g-9f P*8f P4f-4e P7c-7d P3f-3e N8a-7c P4e-4d B2bx4d B7gx4d P4cx4d P*8g P8fx8g+ S8hx8g N7c-6e G4i-5h S6b-7c P3ex3d R8d-8a P*8f S7c-6d P*4e B*5e
+9796FU,T60
'-13
'P9g-9f P1c-1d P*2d P2cx2d R2fx2d P*2c R2d-2e G7b-7a P*8f B2bx7g+ S8hx7g S3a-2b P4g-4f P7c-7d S3h-4g N8a-7c G4i-4h P*8e B*6f R8d-8a P8fx8e P6c-6d R2e-2i S6b-6c P*2d P2cx2d R2ix2d P*2c R2d-2i
-1314FU,T60
'4
'P1c-1d P4g-4f R8d-8b P4f-4e P6c-6d P3f-3e P3dx3e P4e-4d P4cx4d S3h-4g S3a-4b S4g-4f S4b-3c P*8f G7b-6c S4fx3e B2b-1c R2f-3f P*3d S3ex3d S3cx3d R3fx3d P*3c R3dx1d G6c-5d R1d-1f P7c-7d
+1716FU,T60
'-1
'P1g-1f P9c-9d B7gx2b+ S3ax2b B*6f R8d-5d S8h-8g P7c-7d P*8d N2a-3c S8g-7f N8a-7c K6h-5h P6c-6d N8i-7g P7d-7e S7fx7e P*7f P*7d P7fx7g+ P7dx7c+ +P7gx7h
-9394FU,T60
'30++
'P9c-9d P*8f B2bx7g+ S8hx7g S3a-2b B*6f R8d-8b P3f-3e P3dx3e N3g-4e B*4d P*2d P2cx2d R2fx2d B4dx6f P6gx6f S2b-2c R2d-2i P3e-3f P*7d P7cx7d B*5e
+0086FU,T60
'-1
'P*8f B2bx7g+ S8hx7g S3a-2b B*5f P7c-7d P3f-3e P7d-7e P3ex3d S6b-7c P1f-1e P1dx1e P*2d B*4d R2f-3f P7e-7f S7g-6f P2cx2d S6f-7e R8d-5d S7e-6f R5d-8d
-2277UM,T60
'1
'B2bx7g+ S8hx7g S3a-2b B*6f R8d-7d P8f-8e N2a-3c P4g-4f P9d-9e P9fx9e P*9f S7g-8f R7d-7f S8f-7g R7f-7d rep_draw
+8877GI,T60
'-1
'S8hx7g S3a-2b P4g-4f P7c-7d P3f-3e P7d-7e P*2d P2cx2d R2fx2d P7e-7f S7gx7f R8dx8f G7h-8g P*2c R2d-2e R8f-8b P*8f P*8h G8gx8h R8bx8f G8h-8g
-3122GI,T60
'1
'S3a-2b B*6f R8d-7d P8f-8e N2a-3c G4i-5h S6b-5a S7g-8f R7d-5d S8f-7g R5d-7d rep_draw
+4746FU,T60
'68
'P4g-4f G7b-7a P6g-6f P6c-6d P*2d P2cx2d R2fx2d P*2c R2d-2i P7c-7d B*6g P7d-7e P8f-8e R8d-8b B6gx3d P6d-6e P6fx6e R8bx8e P*8g R8e-8d B3d-5f G7a-7b S7g-8f P7e-7f G4i-5h S6b-6c
-7374FU,T61
'14++
'P7c-7d P3f-3e P7d-7e P3ex3d P7e-7f S7g-6f R8dx8f P*8g R8f-8d R2f-3f B*2h B*5e N8a-7c N3g-2e K5b-4b B5ex2b+ G3bx2b S*3c
+3635FU,T61
'53
'P3f-3e P7d-7e P3ex3d S6b-7c N3g-4e R8dx3d B*5f R3d-5d P1f-1e P*8h S7gx8h P1dx1e S8h-7g P*8h S7gx8h P1e-1f R2f-3f P*3d P*1c L1ax1c P*1e S7c-6d R3fx1f
-8173KE,T60
'33--
'P7d-7e P3ex3d S6b-7c B*5f S7c-6d P3d-3c+ G3bx3c
+3534FU,T61
'42
'P3ex3d P*8h S7gx8h R8dx8f P*8g R8f-8d S8h-7g P6c-6d N3g-4e R8d-8a P1f-1e P1dx1e P*7e S6b-6c P7ex7d S6cx7d P6g-6f P*8h G7hx8h P*3g S3h-4g P1e-1f G8h-7h P1f-1g+ S4g-5f
-8481HI,T60
'-6--
'R8d-8a P6g-6f P6c-6d P*7e S6b-6c P1f-1e P1dx1e B*5f P*8h G7hx8h P6d-6e P7ex7d S6cx7d P*1b L1ax1b P*2d B*3e P3d-3c+ G3bx3c R2f-3f G3cx2d B5f-4e K5b-6b P6fx6e P*3d P6e-6d B3e-4d
+6766FU,T61
'38
'P6g-6f P6c-6d P*7e S6b-6c P7ex7d S6cx7d N3g-4e K5b-6b P*7e S7d-6c R2f-2i P4c-4d N4e-3c+ N2ax3c P3dx3c+ S2bx3c B*5f P*7f S7gx7f R8ax8f G7h-8g R8f-8a P7e-7d P*8f G8g-7g
-6364FU,T60
'-1--
'P6c-6d P*7e S6b-6c P1f-1e P1dx1e B*5f B*4d R2f-3f P*3e R3f-2f P6d-6e P*1b L1ax1b P7ex7d S6cx7d P*2d P2cx2d P3d-3c+ B4dx3c B5fx1b+ S2b-2c +B1bx2a R8ax2a N*6d K5b-6c N6dx7b+ K6cx7b G*6d S7d-6c P6fx6e P*7f
+0075FU,T60
'88
'P*7e S6b-6c P7ex7d S6cx7d N3g-4e K5b-6b P*7e S7d-6c P9f-9e P4c-4d N4e-3c+ N2ax3c P3dx3c+ S2bx3c P7e-7d S6cx7d B*5f P*7f S7g-8h S7d-6c P*7d R8ax8f P7dx7c+ G7bx7c
-6263GI,T61
'-40
'S6b-6c P1f-1e P1dx1e P*1c L1ax1c P7ex7d S6cx7d B*4g G7b-6c P*1d L1cx1d B4gx1d P*8e B1d-3f K5b-6b P8fx8e N7cx8e S7g-8h P*8f R2f-2e P*7g G7h-7i N8e-9g+ N8ix9g P8f-8g+ L*8e P7g-7h+ K6h-5h
+7574FU,T60
'-13
'B*5f P6d-6e N3g-4e K5b-6b P6fx6e B*4d R2f-2i P7dx7e P6e-6d S6cx6d P*6f P7e-7f S7gx7f P*7e S7f-8g B4dx6f N8i-7g P7e-7f S8gx7f R8ax8f P3d-3c+ N2ax3c
-6374GI,T61
'-20--
'S6cx7d B*4e P5c-5d B4ex5d S7d-6c B5d-4e K5b-6b G4i-5h P6d-6e P*7d S6cx7d B4ex7b+ K6bx7b G*6d S7d-6c P6fx6e B*4d R2f-2i P*7f S7gx7f B4dx9i+ P*7d S6cx6d P6ex6d
+3745KE,T60
'5
'N3g-4e K5b-6b P*7e S7d-6c P9f-9e P*8h G7hx8h P6d-6e P9ex9d P6ex6f B*7f P*9b G8h-7h P*8h G7hx8h B*4d R2f-3f P*7d P3d-3c+ N2ax3c P*6d S6c-5d B7fx5d P5cx5d P7ex7d N7c-6e
-5262OU,T60
'-13++
'G7b-6c R2f-3f K5b-6b N4e-3c+ N2ax3c P3dx3c+ S2bx3c N*5e P*7f N5ex6c+ S7dx6c S7gx7f B*5d B*4e B5dx7f P*7d P*7b P7dx7c+ P7bx7c N*7e
+0075FU,T60
'-11
'P*7e S7d-6c P9f-9e P*8h G7hx8h P6d-6e P9ex9d P6ex6f B*7f P*9b G8h-7h B*4d R2f-3f P*8h G7hx8h P*7d P*6d S6cx6d P3d-3c+ N2ax3c N4ex3c+ S2bx3c N*5f S6dx7e N5fx4d S7ex7f N4dx3b+ P6f-6g+ K6h-5i
-7463GI,T61
'1++
'S7d-6c G4i-5h P6d-6e P6fx6e N7cx6e S7g-7f B*4d R2f-2i P*6d G5h-6g B4dx9i+ P*6f P4c-4d P6fx6e P4dx4e N8i-7g
+1615FU,T60
'-52
'P1f-1e P1dx1e P9f-9e P*8h G7hx8h P4c-4d N4e-3c N2ax3c P9ex9d P*3g S3h-4g P*9f P3dx3c+ S2bx3c P9d-9c+ P3g-3h+ S4gx3h L9ax9c G8h-8g P6d-6e P6fx6e
-1415FU,T61
'-114
'P1dx1e B*5f P*8h N8i-9g P6d-6e P3d-3c+ N2ax3c N4ex3c+ G3bx3c N*5e P*7f S7gx8h B*4d N5ex6c+ G7bx6c R2f-3f P*3e R3f-3g B4dx6f S*7d
+0056KA,T60
'-68
'B*5f P6d-6e G4i-5h P*8h G7hx8h P1e-1f P*1d P*8e P8fx8e P4c-4d P3d-3c+ N2ax3c N4ex3c+ G3bx3c N*7f N7cx8e P*8d N8ex7g+ N8ix7g P6ex6f P*6d
-6465FU,T60
'-104++
'P6d-6e G4i-5h P*8e P8fx8e P*8h S7gx8h P4c-4d N4e-3c N2ax3c P3dx3c+ S2bx3c N*7d K6b-5b B5fx2c+ N*7f K6h-5i G3bx2c R2fx2c+
+4958KI,T61
'-27
'G4i-5h P*8h G7hx8h P*8e G8h-8g P4c-4d P3d-3c+ N2ax3c N4ex3c+ G3bx3c P*3d G3c-3b P6fx6e P8ex8f S7gx8f P*6g G5hx6g P*6f G6gx6f N*5d G6f-5e
-0076FU,T60
'-94--
'P*7f S7gx7f P4c-4d N4e-3c N2ax3c P3dx3c+ G3bx3c P6fx6e N*6f G7h-7g N6fx5h+ K6hx5h G*5e N*6d G7b-8c S3h-4g P*7b P*2d G3cx2d N*7d K6b-5a P*1b L1ax1b R2fx2d G5ex5f R2d-2i G5fx4g K5hx4g G8cx7d P7ex7d N*5e
+7776GI,T60
'-4
'S7gx7f P4c-4d P7e-7d S6cx7d P3d-3c+ N2ax3c N4ex3c+ G3bx3c N*6d G7b-8b P*1b P*7e S7f-8g L1ax1b P*2d G3cx2d R2fx2d P2cx2d B5fx1b+ B*5e G*8d S7d-6c +B1bx2b S6cx6d
-4344FU,T60
'-138
'P4c-4d N4e-3c N2ax3c P3dx3c+ G3bx3c P7e-7d S6cx7d N*6d G7b-6c P*7e S7d-8c P6fx6e N*6f G7h-7g N6fx5h+ K6hx5h G*5e P*3d G3c-3b B5f-4g B*8h N*7d K6b-5a B4g-2e G3b-4b P5g-5f G5ex5f L1ix1e B8hx9i+ P3d-3c+ S2bx3c L1ex1a+ +B9ix8i
+6665FU,T60
'-115
'P3d-3c+ N2ax3c N4ex3c+ G3bx3c P6fx6e N*6f G7h-7g N6fx5h+ K6hx5h G*5e N*6d G7b-8c P*3d G3c-3b B5f-4g B*8h N*7d K6b-5a P*1c B8hx9i+ L1ix1e L*5d P1c-1b+ G5e-6f G7g-6g N7cx6e B4gx6e S6cx6d
-4445FU,T61
'-114++
'P4dx4e P6e-6d S6cx6d P7e-7d B*4d P7dx7c+ G7bx7c P*7d G7c-6c P*6e S6d-5e R2f-2i N*6f B5fx4e N6fx7h+ K6hx7h R8ax8f G5h-6g P*6f G6g-7g G*6g S7fx6g P6fx6g+ B4ex6g R8fx4f G*7c G6cx7c P7dx7c+ K6bx7c P*4g R4f-3f P*7d K7c-6c P7d-7c+ K6cx7c rep_inf
+6564FU,T60
'-105
'P6e-6d S6cx6d P7e-7d B*4d R2f-2i N*6f P7dx7c+ G7bx7c G7h-7g N6fx5h+ K6hx5h P*7e S7f-6e G*5e P*7d S6dx6e P7dx7c+ K6bx7c P4fx4e B4d-3e B5fx6e G5ex6e N*6f K7c-6c G*7d K6c-5b N*4d K5b-4c N4dx3b+
-6354GI,T60
'-145--
'S6c-5d P7e-7d N*6f P7dx7c+ G7bx7c G7h-7g N6fx5h+ K6hx5h G*3e R2f-2i G3ex4f N*6f B*3e N6fx5d P5cx5d S*6c K6b-5a
+7574FU,T61
'-302
'P7e-7d N*6f G7h-7g N6fx5h+ K6hx5h G*3e R2f-2g G3ex4f P7dx7c+ G7bx7c N*6f G4fx5f N6fx5d P5cx5d P*7d G7cx6d S*7c K6b-5c S7cx6d+ K5cx6d P5gx5f N*4f K5h-6h K6d-5c P7d-7c+ N4fx3h+ K6h-7h
-0066KE,T60
'-60--
'N*6f G7h-7g N6fx5h+ K6hx5h G*3e R2f-2g G3ex4f P7dx7c+ G7bx7c N*6f S5d-5e B5f-6e S2b-3a P*4g B*3f S7f-7e S5ex6d P*7d S6dx6e P7dx7c+ K6b-5b R2g-2f G4fx5g K5hx5g B*3e G*4f B3ex2f G4fx3f R*5f K5g-6h R5fx3f
+7877KI,T60
'-1
'G7h-7g N6fx5h+ K6hx5h G*3e R2f-2g G3b-4b N*7e G3ex4f N7e-6c+ G7bx6c P6dx6c+ K6bx6c P7dx7c+ K6cx7c N*6f G4fx5f P5gx5f B*3f K5h-6g B*5h K6g-7h B5h-6i+ K7h-8h P*8g S7fx8g
-6658NK,T60
'6
'N6fx5h+ K6hx5h G*3e R2f-2g G3b-4b N*7e B*3f K5h-6h B3fx2g+ P6d-6c+ S5dx6c P7dx7c+ G7bx7c S3hx2g R*4h N*5h P*6g K6hx6g G4b-5b P*7d G7cx7d B*4d R4h-4i+ B4dx3e +R4i-6i P*6h +R6ix8i N7ex6c+ G5bx6c B5fx7d G6cx7d B3e-4d N*7e K6g-5f
+6858OU,T60
'-3
'K6hx5h G*3e R2f-2g G3ex4f P7dx7c+ G7bx7c N*6f B*3f K5h-6h S5d-5e B5f-6e S2b-3a S7f-7e S5ex6d P*7d S6dx6e P7dx7c+ K6bx7c R2g-2f P*7f G*6d K7c-6b G7g-6g B*8g P*4g P*3g P*6c K6b-5b N*2h P3gx3h+
-0035KI,T60
'-81--
'G*3e R2f-2i G3ex4f P7dx7c+ G7bx7c P*4g G4fx5f P5gx5f P*6e N*5e G7cx6d P*6c S5dx6c N5ex6c+ K6bx6c N*5e K6c-5b R2i-6i B*3e G*3f B*7i G3fx3e B7ix3e+ B*5g +B3ex5g K5hx5g B*3e K5g-6g G*6f K6g-7h G6fx7g K7hx7g P*7e S7fx6e G*7f
+2627HI,T60
'111
'R2f-2g B*3f K5h-6h P*6g G7gx6g S5d-5e R2g-3g P*6f G6g-7g S5ex5f P5gx5f G3ex4f S*6c G7bx6c P6dx6c+ K6bx6c P7dx7c+ K6c-5b R3gx3f G4fx3f B*6c K5b-4c K6h-7h R*4h P*6h R4hx3h+ B6cx8a+
-3546KI,T61
'-260--
'G3ex4f N*6f B*3f P*4g S5d-5e P7dx7c+ G7bx7c B5f-6e S2b-3a S7f-7e S5ex6d P*7d S6dx6e P7dx7c+ K6b-5b R2g-2f G4fx5g K5hx5g B*3e K5g-6g B3ex2f N*5e K5b-4b G*6d
+0066KE,T60
'-4
'N*6f S5d-5e P7dx7c+ G7bx7c P*4g G4fx5f P5gx5f S5e-4d P*7d G7cx6d P*6e G6d-6c N*7e B*7i K5h-6g B7i-3e+ R2g-2i B*8h G7g-8g B8hx9i+ N8i-7g +B9i-9h L1ix1e L1ax1e N7ex6c+ K6bx6c
-5455GI,T60
'-75--
'S5d-5e P7dx7c+ G7bx7c B5f-6e S2b-3a S7f-7e B*3f P*4g S5ex6d P*7d S6dx6e P7dx7c+ K6b-5b R2g-2f G4fx5g K5hx5g B*3e K5g-6g B3ex2f G*6d P*7f G7gx7f K5b-4c G7fx6e B2f-4h+ G*3e R*5g K6g-7h G3b-4b P*5d +B4hx3h P5dx5c+ K4c-3b +P5cx4b S3ax4b N*4d K3b-2b G*3b K2b-1b G3ex3f +B3hx4g L1ix1e P*1d L1ex1d P*1c P3d-3c+ S4bx3c
+7473TO,T60
'78
'P7dx7c+ G7bx7c B5f-6e S2b-3a S7f-7e B*3f P*4g S5ex6d P*7d S6dx6e P7dx7c+ K6b-5b N*5e K5b-4a R2g-2f S3a-4b P*2b K4a-3a P2bx2a+ K3ax2a N*4d G4fx5g K5h-4i B3f-1d L1ix1e B*3e R2f-1f B3ex4d L1ex1d N*5f B*3g L1ax1d R1fx1d P*7f K4i-3i P7fx7g+ G*1b K2a-3a
-7273KI,T60
'284
'G7bx7c B5f-6e S2b-3a S7f-7e P*6c P*4g P6cx6d B6e-7f S5ex6f S7ex6f G4f-3f R2g-2e P*7e P*6c G7cx6c B7f-8e P5c-5d S6fx7e N*6e R2ex4e P*4d R4ex4d R8ax8e P8fx8e N6ex7g+ N8ix7g
+5665KA,T60
'258
'B5f-6e B*3f P*4g S2b-3a S7f-7e S5ex6d P*7d S6dx6e P7dx7c+ K6bx7c G*6d K7c-6b G6dx6e K6b-5a N*4d K5a-4b S7e-6d P*7f G7g-7h K4b-4c N4dx3b+ K4cx3b S6dx5c+ K3b-2b +S5c-4c P*3b K5h-6g R8ax8f P*8g B3fx2g+
-2231GI,T60
'277++
'S2b-3a S7f-7e S5ex6d P*7d S6dx6e P7dx7c+ K6b-5b N*5e K5b-4a +P7c-6c B*3f K5h-6h S3a-4b P*4c S4b-5a +P6cx5c B3fx2g+ S3hx2g R*2h B*3h P*7f G7g-7h P*6g K6h-7i G4fx5g P*6i P*8g G7hx8g B*4f P*7h P*5f K7i-8h B4fx5e P*5b S6ex6f P5bx5a+ K4a-3a P4c-4b+ G3bx4b +P5cx4b K3ax4b G*6e
+7675GI,T60
'358
'S7f-7e S5ex6d P*7d S6dx6e P7dx7c+ K6bx7c G*6d K7c-6b G6dx6e P*3g N*6d B*4c S3hx3g B*4g K5h-6h B4gx6e+ P*6c K6b-5a P*4d B4cx3d S3gx4f P4ex4f S*5b B3dx5b N6dx5b+ K5ax5b
-5564GI,T74
'292
'S5ex6d P*7d S6dx6e P7dx7c+ K6b-5b N*5e B*3f K5h-6h K5b-4a +P7c-6c S3a-4b P*4c S4b-5a +P6cx5c B3fx2g+ S3hx2g R*2h B*3h P*7f G7g-7h P*6g K6h-7i G4fx5g P*6i P*8g G7hx8g B*4f P*7h G5g-5f K7i-8h K4a-3a P4c-4b+ S5ax4b +P5cx4b G3bx4b S*4d S6ex6f S7ex6f G5fx6f P*4c
+0074FU,T61
'253
'P*7d S6dx6e P7dx7c+ K6b-5b N*5e B*3f K5h-6h K5b-4a +P7c-6c S3a-4b P*4c S4b-5a +P6cx5c B3fx2g+ S3hx2g R*2h G*3h R2h-2i+ B*6c K4a-3a P4c-4b+ S5ax4b +P5cx4b G3bx4b B6cx8a+ P*7f R*6a K3a-2b S*3a K2b-1c R6ax6e+ +R2ix8i P*1d K1cx1d G7g-7h G4b-5c L1ix1e K1dx1e S*2f K1e-1d P*1e K1d-2d P*2e K2dx3d
-6465GI,T60
'347
'S6dx6e P7dx7c+ K6bx7c G*6d K7c-6b G6dx6e P*7f G7g-7h P*3g P*6d B*3f K5h-6h P3gx3h+ N*5e K6b-5a P6d-6c+ S*6a P*4d B*4g S*5b S6ax5b +P6cx5b K5ax5b P4d-4c+ G3bx4c N5ex4c+ K5bx4c R2gx2c+ P*3c P*4d K4cx4d S*5e K4d-4c G*4d
+7473TO,T60
'207
'P7dx7c+ K6b-5b N*5e B*3f K5h-6h K5b-4a +P7c-6c S3a-4b P*4c S4b-5a +P6cx5c B3fx2g+ S3hx2g R*2h K6h-6i B*2d P3d-3c+ B2dx3c P4c-4b+ S5ax4b B*6c K4a-3a +P5cx4b B3cx4b B6cx8a+ K3a-2b R*6b G4fx5g S*3h P*6c G7g-7h P*3g G*4c P3gx3h+ G4cx3b K2bx3b K6i-7i R2hx2g+ +B8ax6c K3b-2b R6bx4b+ K2b-1c B*3e S*2d
-6252OU,T60
'275++
'K6b-5b N*5e B*3f K5h-6h K5b-4a R2g-2i B*3e G7g-6g P*3g +P7c-6b S3a-2b P*4d S2b-1c S3hx3g G4fx5g G6gx5g B3f-4g+ P*5h P*6g K6hx6g +B4gx2i P4d-4c+ K4a-3a +P4cx3b K3ax3b G*4c K3b-2b P3d-3c+ K2b-1b
+0055KE,T60
'578
'N*5e K5b-4a K5h-6h S3a-4b P*4c G4fx5g R2gx5g B*3e P4cx4b+ K4ax4b S3h-4g B*3g P*4c G3bx4c S7e-6d B3g-5i+ K6hx5i B3ex5g+ N5ex4c+ K4bx4c G*5h +B5g-3e B*6c R8a-3a G*3f R*2i K5i-4h
-5241OU,T60
'648++
'K5b-4a K5h-6h S3a-4b P*4c G4fx5g R2gx5g B*3e G7g-6g S4b-5a P*2b B3ex5g+ K6hx5g R*8h G*6h K4a-3a P2bx2a+ K3ax2a K5g-5h
+5868OU,T60
'863
'K5h-6h B*3f R2g-2i B*3e G7g-6g S3a-4b P*4c S4b-5a P*2b G3bx2b +P7c-6b K4a-3b +P6bx5a P2c-2d S*2f B3ex2f R2ix2f K3b-2c G*3e P2d-2e G3ex2e B3fx2e R2fx2e P*2d R2e-2f G*7f
-0036KA,T60
'851++
'B*3f R2g-2i S3a-4b P*4c B*2e P4cx4b+ K4ax4b K6h-7h G4fx5g K7h-8h P*7f G7g-7h B3f-6i+ P*4c K4b-3a R2ix2e K3a-2b P4c-4b+ G3bx4b R2ex4e +B6ix7h K8hx7h
+2729HI,T60
'1060
'R2g-2i B*3e G7g-6g S3a-4b P*4c S4b-5a P*2b G3bx2b +P7c-6b K4a-3b +P6bx5a P2c-2d S*2f B3ex2f R2ix2f K3b-2c G*3e P2d-2e B*4a S*3b B4ax3b+ G2bx3b S*2d K2c-2b R2fx3f G4fx3f B*4d P*3c
-4657KI,T60
'965++
'G4fx5g K6hx5g B*4f K5g-6g B4fx5e G*4g B3fx4g+ S3hx4g N*5a G*5f P*7f G5fx6e P7fx7g+ N8ix7g B5e-3g+ S4g-5h +B3gx7c S*6d +B7cx6d G6ex6d P*6c P*5b K4ax5b G6d-6e S*8h B*4d G*8g B4dx1a+ S8hx7g K6g-5g G8gx8f +B1ax2a
+6857OU,T61
'975
'K6hx5g B*4f K5g-6g B4fx5e G*4g B3fx4g+ S3hx4g N*5a G*6d P*7f G6dx6e P7fx7g+ K6gx7g B5e-3g+ B*7d P*6c +P7cx6c S3a-4b +P6c-5b K4a-3a +P5bx4b K3ax4b P*4c K4bx4c S4g-5f +B3g-4f G6e-5e P*6c G5ex4e
-0046KA,T60
'1166++
'B*4f K5g-6g B4fx5e G*4g B3fx4g+ S3hx4g S3a-4b B*6c K4a-3a B6cx8a+ B5e-3g+ R*6a G*5a R6ax6e+ +B3gx4g K6g-7f +B4gx2i +R6e-6c P*8d S*7d S*5b +R6c-7b +B2ix1i P*6d +B1i-5e P6d-6c+ G5a-6a +R7b-8b
+5767OU,T60
'1047
'K5g-6g B4fx5e G*4g B3fx4g+ S3hx4g N*5a S4g-5f S6ex5f K6gx5f B5ex7c P*6d S3a-4b P*7d B7cx6d S7ex6d P*6c S6d-7c+ P*7f G7g-8g K4a-3a G*5b S*4a G5b-6b G*7e K5f-6g G7ex8f G8gx7f G8fx7f K6gx7f G*8d
-4655KA,T60
'1095--
'B4fx5e G*4g B3fx4g+ S3hx4g P*6b G*6d B5e-3g+ G6dx6e +B3gx4g B*7d K4a-4b R2i-5i G3b-4c S*5b N*5f
+0047KI,T60
'1306
'G*4g B3fx4g+ S3hx4g N*5a S4g-5f S6ex5f K6gx5f B5ex7c P*6d S3a-4b B*7d S*5b G*8c G*6c P6dx6c+ S5bx6c G8cx7c S6cx7d N6fx7d B*3h B*4g B3hx2i+ B4gx2i R*5i K5f-6e R5ix2i+ N7d-6b+ B*4g K6e-7f K4a-3a B*7b B4g-9b+ G*5b +R2ix8i B7bx8a+
-3647UM,T60
'1191--
'B3fx4g+ S3hx4g S3a-4b B*6c K4a-3a B6cx8a+ B5e-3g+ R*6a G*5a R6ax6e+ +B3gx4g K6g-7f +B4gx2i +R6e-6c P*7d N6fx7d P*7b +R6cx7b N*9c S*6f R*6i
+3847GI,T60
'1302
'S3hx4g S3a-4b B*6c K4a-3a B6cx8a+ B5e-3g+ R*6a G*5a R6ax6e+ +B3gx4g K6g-7f +B4gx2i +R6e-6c K3a-2b +B8ax9a +B2ix1i P*6d +B1i-4f K7f-8e R*3i K8e-8d K2b-1c P*5b R3ix3d+ P5bx5a+ S4bx5a K8dx9d
-3142GI,T60
'1297
'S3a-4b B*6c K4a-3a B6cx8a+ B5e-3g+ R*6a K3a-2b P*2d P2cx2d R6ax6e+ +B3gx4g R2ix2d P*2c S*5f G*5g K6g-7f +B4g-5h P*6g P2cx2d P*2c G3bx2c +B8ax4e G5gx5f +B4ex5f R*4i L1ix1e P*1d +R6e-6a R4ix8i+ P*4c S4bx4c K7f-8e +R8ix9i +R6ax9a S4cx3d
+0063KA,T60
'1317
'B*6c K4a-3a B6cx8a+ B5e-3g+ G*3h G*5f S4gx5f S6ex5f K6g-7f +B3gx3h R2i-7i G*6h +B8ax9a G6hx7i K7f-8e K3a-2b L*4d N*4c R*7b R*3g G*5b K2b-1c G5bx4b K1c-2d +P7c-8c K2d-3e G4bx3b K3e-3f G3bx2a G7ix8i
-4131OU,T60
'1326
'K4a-3a B6cx8a+ B5e-3g+ R*6a G*5a R6ax6e+ +B3gx4g K6g-7f +B4gx2i +R6e-6c +B2ix1i +B8ax9a L*6a +R6c-7b P*7d K7f-8e P7dx7e S*8d K3a-2b K8ex9d P*6b L*4d S4b-3a +R7b-8a +B1i-4f P*5b G5ax5b +R8ax6a R*5a P3d-3c+ N2ax3c
+6381UM,T60
'1405
'B6cx8a+ B5e-3g+ R*6a G*5a R6ax6e+ +B3gx4g +R6e-6c +B4gx2i K6g-7f K3a-2b +B8ax9a +B2ix1i P*6d +B1i-5e K7f-8e K2b-1c +B9a-9b P*6b +R6c-7b P*7d +P7cx7d P*7f G7g-6g R*3g G6gx7f +B5e-5f K8ex9d P*9c K9dx9c R3gx3d+
-5537UM,T60
'1350--
'B5e-3g+ R*6a G*5a R6ax6e+ +B3gx4g +R6e-6c +B4gx2i K6g-7f +B2ix1i P*5b P*7d +P7cx7d L*6a P5bx5a+ L6ax6c +B8ax6c K3a-2b +P5a-4a +B1i-2i G*6e R*5g +P4ax4b G3bx4b L*4d S*6g G7gx6g R5gx6g+ K7fx6g +B2ix6e
+0061HI,T60
'1346
'R*6a G*5a R6ax6e+ +B3gx4g +R6e-6c +B4gx2i K6g-7f K3a-2b +B8ax9a +B2ix1i P*6d R*3i +R6c-7b R3ix8i+ L*4d +B1i-5e L4dx4b+ G5ax4b K7f-8e +R8i-3i +B9a-9b +R3ix3d K8e-8d K2b-1c S*2f G3b-3a K8dx9d
-0051KI,T60
'1371++
'G*5a R6ax6e+ +B3gx4g K6g-7f +B4gx2i +R6e-6c P*8d S7ex8d P*8c S8dx8c+ S*9b +S8cx9b L9ax9b G*7e R*6a +R6cx6a G5ax6a P*2d P2cx2d P*2c R*5g +B8ax4e G3bx2c P*4c S4bx4c P*4d S4cx3d P4d-4c+
+6165RY,T60
'1452
'R6ax6e+ +B3gx4g K6g-7f +B4gx2i +R6e-6c K3a-2b +B8ax9a +B2ix1i P*6d L*6a +R6c-7b L6ax6d K7f-8e L6dx6f G7gx6f N*5d P*5b S*6a +R7b-9b S6ax5b G6f-5f N*6a S*8d R*3i G5fx4e N6ax7c S8dx7c+ R3ix8i+ P3d-3c+ S4bx3c
-3747UM,T60
'1401++
'+B3gx4g +R6e-6c +B4gx2i K6g-7f P*7d N6fx7d P*8d S7ex8d R*6a +R6cx6a G5ax6a P*1b N*7b P1bx1a+ N7bx8d K7f-8e S*7f G7gx7f N8dx7f +P1ax2a K3ax2a S*8d +B2i-6e +B8ax9a P*7b G*7e
+6776OU,T60
'1327
'+R6e-6c +B4gx2i K6g-7f K3a-2b +B8ax9a +B2ix1i P*6d R*3i P*5b G5a-4a +R6c-6a G3b-3a K7f-8e K2b-1c P6d-6c+ R3ix3d+ S7e-8d K1c-1d K8ex9d K1d-2e G*2g L*2f S*3f +R3dx3f G2gx3f K2ex3f P*4c P*3g P4cx4b+ G3ax4b P5b-5a+ G4a-3b
-4765UM,T61
'1405++
'+B4gx2i +R6e-6c P*8d S*7d K3a-2b P*5b P*6b +P7cx6b G5ax6b +R6cx6b P*6a +R6bx6a P*7c S7dx7c+ +B2ix1i +B8ax9a P*7b +R6ax7b S*6d L*3c N2ax3c P3dx3c+ S4bx3c N*2e R*5g
+7665OU,T61
'1447
'K7fx6e R*6a B*6c R6ax8a B6cx8a+ B*3h K6e-7f B3hx2i+ +B8ax9a K3a-2b K7f-8e P*7f G7gx7f S*6e G7f-7g P*7f G7g-7h R*3h S*6g R3hx3d+ R*8d N*5d K8ex9d S6ex6f S6gx6f P*9c K9dx9c +B2ix1i
-0061HI,T60
'1464++
'R*6a B*6c R6ax8a B6cx8a+ B*3h K6e-7f B3hx2i+ +B8ax9a N*6e L*4d K3a-2b L4dx4b+ N6ex7g+ N8ix7g G5ax4b R*6a P*3a N*4d R*3g +P7c-7d R3gx3d+ +B9a-5e
+8163UM,T60
'1412
'+B8a-6c R6ax6c +P7cx6c B*4g P*5f B4gx2i+ B*5e R*5i G*6g +B2ix1i B5ex1a+ S*2b +B1a-1b L*1a P*1c P*6b +P6c-6d N*5e R*7a L1ax1b P1cx1b+ N5ex6g+ G7gx6g G*6a R7ax9a+ R5ix8i+ L*4d +R8i-7h L4dx4b+ G3bx4b L*4d L*7a L*3c N2ax3c
-6163HI,T61
'1330--
'R6ax6c +P7cx6c B*3h P*5f B3hx2i+ L1ix1e R*1f G*6g R1fx1e R*7a P4e-4f K6e-7d S*8b R7a-7b+ P*7c K7d-8e L*7d
+7363TO,T60
'1391
'+P7cx6c B*3h P*5f B3hx2i+ B*4d R*5i G*6g +B2ix1i S*5b P*3c R*8a S*4c R8ax5a+ S4bx5a B4dx5c+ S5a-4b P3dx3c+ N2ax3c G*4a K3a-2b G4ax4b P*6d S7ex6d G3bx4b +B5cx4b S4cx5b +B4bx5b K2b-1c P*1d K1cx1d
-0047KA,T60
'1502++
'B*3h P*5f B3hx2i+ S*5b R*3f S5bx5a+ S4bx5a R*7a S*4b G*5e K3a-2b B*4d P*3c +P6cx5c N*4c +P5cx4b S5ax4b S*2e
+0056FU,T60
'1647
'P*5f B4gx2i+ B*5e R*5i G7g-6g S*7h S*7f S7hx6g+ S7fx6g P*6d S7ex6d P*6b P*4c S4bx4c B5ex1a+ P6bx6c S6dx6c+ R5i-7i+ P*7g P*6b +S6cx5c N*4a L*5e N4ax5c L5ex5c+ +B2ix1i +L5cx4c S*6d K6e-7f G3bx4c
-4729UM,T60
'1323--
'B4gx2i+ S*5b G5ax5b +P6cx5b N*7c K6e-6d +B2ix1i K6d-6c P*6b K6cx6b S*5a +P5bx5a S4bx5a K6bx5a R*4a K5a-6b S*7a K6b-6c +B1i-4f P3d-3c+ N2ax3c G*5b P*6b
+0055KA,T60
'1668
'B*5e R*5i G7g-6g S*7h S*7f S7hx6g+ S7fx6g P*3c P3dx3c+ S4bx3c R*7a G*6a R7ax9a+ N*7a +P6cx5c +B2ix1i B5ex1i R5ix1i+ P*6b G6a-7b P*7c
-0062FU,T60
'1488
'P*6b B5ex1a+ S*2b +B1a-1b P6bx6c R*8a P*7a L*4d +B2ix1i R8ax7a+ R*6a +R7ax6a G5ax6a K6e-7f +B1i-4f P*7d L*1a P*4c L1ax1b P4cx4b+ G3bx4b L4dx4b+ K3ax4b
+0052GI,T60
'2046
'S*5b P6bx6c R*7a S*6d S7ex6d P6cx6d K6e-7f P*7e K7f-8g S*7f G7gx7f P*3c S5bx5a S4bx5a R7ax5a+ N*4a P3dx3c+ N2ax3c S*5b R*5g G7f-7g S*7f K8g-9h S7fx7g+ N8ix7g +B2ix5f
-6263FU,T61
'2068++
'P6bx6c R*7a S*6d S7ex6d P6cx6d K6e-7f P*3c S5bx5a S4bx5a R7ax5a+ S*4a B5ex6d +B2ix5f S*6e +B5fx8i P3dx3c+ N2ax3c P*3d S*4b P3dx3c+ G3bx3c S*2b K3ax2b +R5ax4a
+0071HI,T60
'2296
'R*7a S*6d S7ex6d P6cx6d K6e-7f P*7e K7f-8g S*7f G7gx7f R*5g G7f-7g P*3c P3dx3c+ G3bx3c G*6g R5g-5i+ S*7h +B2ix5f G6gx5f +R5ix5f B5ex6d +R5f-6e B6dx9a+ K3a-2b
-0064GI,T60
'2255++
'S*6d S7ex6d P6cx6d K6e-7f P*3c S5bx5a S4bx5a R7ax5a+ S*4a B5ex6d P*7d S*5b S*7e K7f-6e S7ex6d K6ex6d B*3g S*5e +B2ix5f +R5ax4a K3a-2b S*4d R*6e K6dx5c +B5fx5e P3dx3c+ G3bx3c S4dx5e R6ex5e K5c-6c
+7564GI,T60
'2632
'S7ex6d P6cx6d K6e-7f N*4a S5bx4a+ G5ax4a S*5b S*6e K7f-8g S*5a N*4d P*3c S5bx4a+ K3ax4a P3dx3c+ N2ax3c G*5b K4a-3a G*3d S*4c N4dx3b+ S4cx3b P*4c N*7e K8g-9g S4bx4c G3dx4c S3bx4c S*4b K3a-2b
-6364FU,T60
'2491++
'P6cx6d K6e-7f S*7e K7f-8g P*3c S5bx5a S4bx5a R7ax5a+ N*4a P3dx3c+ S*7f G7gx7f S7ex7f K8gx7f N2ax3c S*4b G3bx4b G*2b K3ax2b +R5ax4b G*3b S*3a K2b-1c L1ix1e P*1d S*2b G3bx2b S3ax2b K1c-2d S2bx3c K2dx1e G*2e K1ex2e S*3f K2ex3f G*3g K3f-3e +R4b-4d
+6576OU,T80
'2812
'K6e-7f P*7e K7f-8g S*7f G7gx7f R*5g G7f-7g P*3c P3dx3c+ N2ax3c G*6g R5g-5i+ S5bx5a +R5ix8i S*8h K3a-2b S5ax4b+ G3bx4b P*3d S*7f G7gx7f P7ex7f P3dx3c+ G4bx3c N*3d K2b-1c R7ax1a+ N*1b
-0073HI,T60
'2810++
'R*7c R7ax7c+ G5ax5b B5ex1a+ +B2ix5f R*7a S*5a R7ax9a+ P*8c S*7e +B5f-6e K7f-8e N*6c P*4c S4bx4c L*3c S*2b S7e-7d S2bx1a K8ex9d N2ax3c L1ix1e K3a-4b L1ex1a+ B*4g S*3e P*9c K9dx9c
+7173RY,T61
'2876
'R7ax7c+ G5ax5b B5ex1a+ +B2ix5f R*7a S*5a R7ax9a+ P*8c S*7e +B5f-6e K7f-8e N*6c K8ex9d S*6b +R7c-8b +B6ex7e L*8e P*7f G7g-7h S*2b +B1a-1b +B7ex6f P3d-3c+ S2bx3c P*7c G3b-2b
-5152KI,T60
'2882++
'G5ax5b B5ex1a+ +B2ix5f R*7a S*5a R7ax9a+ P*8c S*7e S*8d S7ex8d P8cx8d S*7d S*6e K7f-7e P*7b L*3c S6ex7d N6fx7d S*2b +R7cx7b P*7c +B1ax2a K3ax2a K7ex8d +B5fx7d K8d-9c B*5d N*4d S4bx3c P3dx3c+ G3bx3c
+5511UM,T60
'3111
'B5ex1a+ +B2ix5f R*7a S*5a R7ax9a+ P*8c S*7e +B5f-6e K7f-8e N*6c K8ex9d N6cx7e +R7c-8b P*7b P*7d N7e-8g+ K9d-9c S*2b +B1a-1b +N8gx7g K9c-9b +B6ex6f L*1d S*6b N8ix7g G*7a +R8bx7a S6bx7a +R9ax7a
-2956UM,T60
'3084++
'+B2ix5f R*7a S*5a R7ax9a+ P*8c S*7e +B5f-6e K7f-8e N*6c P*4c S4bx4c +R7cx6c S*8d S7ex8d P8cx8d K8ex9d G5bx6c +R9ax5a R*4a G*2b
+0071HI,T60
'3021
'R*7a S*5a R7ax9a+ P*8c S*7e S*8d S7ex8d P8cx8d S*7d S*6e K7f-7e P*7b +R7cx8d S6ex7d N6fx7d P*8c +R8dx8c P*8b N7dx8b+ P*8a +R9ax8a S*7c K7e-8e
-0051GI,T60
'3322
'S*5a R7ax9a+ P*8c S*7e S*6b +R7cx6d P*6c +R6d-7d P*7c +R7dx9d P6c-6d P*4c +B5f-6e K7f-8e G5bx4c P*5b N*6c G*7f N6cx7e G7fx6e P6dx6e P5bx5a+
+7191RY,T60
'3146
'R7ax9a+ P*8c S*7e +B5f-6e K7f-8e N*6c K8ex9d N6cx7e +R7c-8b P*7a L*7d N7e-8g+ G*7f S*2b +B1a-1b +B6e-5e P*5f +N8gx7g P5fx5e +N7gx7f K9d-9c +N7fx6f K9c-9b G*1a
-0083FU,T60
'3296++
'P*8c S*7e +B5f-6e K7f-8e P*7f G7gx7f S*6b G7fx6e S6bx7c P*7d S7c-8d K8ex9d P6dx6e S7ex8d P8cx8d P7d-7c+ S*2b +B1a-1b P*7b +P7cx7b P*7a +P7b-7c P6ex6f L*4d
+0075GI,T60
'3286
'S*7e +B5f-6e K7f-8e N*6c K8ex9d S*6b +R7c-8b +B6ex7e K9dx8c P*7f G7g-6g +B7e-6e L*7d S*7a +R8b-8a N6c-5e G6g-5g N5e-4g+ G5g-6g G5b-6c P*5b G6cx7d N6fx7d L*8b +R8ax8b
-0084GI,T60
'3433
'S*8d S7ex8d P8cx8d S*7d S*6e K7f-7e P*7f L*5i S6ex7d N6fx7d +B5f-6e K7ex8d S*2b +B1a-1b P7fx7g+ K8d-9c G*1a N8ix7g +B6e-6f K9c-9b G1ax1b L1ix1e P*1a S*6a
+7584GI,T61
'3471
'S7ex8d P8cx8d S*7d S*2b +B1a-1b N*6b L*5h +B5fx8i L*5e N*4a P*5d +B8ix9i P5dx5c+ +B9ix7g K7fx7g S4bx5c L5ex5c N4ax5c L5hx5c+ L*7e P*7f N6bx7d +L5cx5b
-8384FU,T60
'3616++
'P8cx8d S*7d N*6c +R7cx8d +B5f-5e +B1ax5e N6cx5e L*4d S*6e K7f-8e B*5h P*7f N5e-6g+ L4dx4b+ G3bx4b B*5e P*3c S7dx6e +N6gx7g S*1a
+0074GI,T60
'3642
'S*7d S*6e K7f-7e P*7b L*3c S6ex7d N6fx7d S*2b L3cx3b+ K3ax3b L*3c S4bx3c +R7cx7b P*7a S*4a K3bx4a +B1ax2a P*3b +R9ax7a P*7c N*6f L*8c +R7bx8c K4a-4b K7ex8d K4b-4c G*3e S3c-4d L*5i +B5f-6e
-0022GI,T60
'3755++
'N*6c +R7cx8d S*6e K7f-8e P*7f G7g-8g +B5fx6f +B1ax6f S6ex6f K8ex9d P*9c +R8dx9c B*4g S7d-8c+ B4g-6e+ L*4d N6c-7e G8g-9g
+1112UM,T60
'3782
'+B1a-1b N*6b G*6g +B5f-4f P*6c N6bx7d P6c-6b+ G5bx6b +R7cx6b S*4a +R6b-8b P6d-6e N6fx7d +B4fx8b N7dx8b+ R*1d K7f-7e R1dx1b K7ex8d S2b-3c L*3f S3cx3d L3fx3d P*3c N*4d B*5e
-4546FU,T60
'4181++
'N*6e G7g-6g P*7e K7fx7e +B5fx6g L*6i +B6g-5f P*6c P*7a G*6b +B5f-5e G6bx5b S5ax5b P6c-6b+ S2b-1a +R9ax7a G*5a L*5h +B5e-4d +P6bx5a S1ax1b +P5ax5b K3a-2b +P5bx4b G3bx4b L1ix1e K2b-1a L1ex1b+ K1ax1b G*8c
+7675OU,T61
'4532
'P*6c N*4a K7f-7e +B5fx8i G*6b +B8ix9i G6bx5b S5ax5b P6c-6b+ G*6a +P6bx5b G6ax5b +R7c-7a S4b-5a L*4e L*4c G7g-7f L4cx4e L*4d L*4c L4dx4c G3bx4c K7ex8d P4f-4g+ L1ix1e G4cx3d
-4647TO,T60
'4331
'N*4a K7ex8d +B5fx8i L9i-9g P*8a K8d-9c +B8i-8h S7d-8c+ +B8hx9g +R9ax8a L*1a P*1c L1ax1b P1cx1b+ G5b-6b +R7c-7a B*6c P*7b +B9g-8h G7g-7f +B8h-8g
+0063FU,T60
'4864
'P*6c P*4a G*6b +B5f-4e L*4i P*7b G6bx7b P6d-6e L4ix4g +B4e-5e L4gx4b+ G5bx4b S*6d +B5ex6d +R7cx6d L*1a P*1c P6ex6f P6c-6b+ L1ax1b P1cx1b+
-0041FU,T60
'4504++
'P*4a G*6b P2c-2d G6bx5b S5ax5b P6c-6b+ S4b-4c +P6bx5b S4cx5b G*6b S2b-2c +B1b-1a G*1b S*2b
+0062KI,T60
'4923
'G*6b +B5fx3d L*4i P6d-6e L4ix4g P6ex6f L*4e P6f-6g+ G6bx5a G5bx5a P6c-6b+ G*5b +P6bx5a G5bx5a G*6b G5ax6b +R7cx6b P*6a G*5b +B3dx5b +R6bx5b G*6b L4ex4b+ P4ax4b
-5634UM,T60
'4527
'+B5fx3d K7ex8d S2b-3c G6b-7b G3b-2b +B1b-1a K3a-3b L*4i P2c-2d L4ix4g K3b-2c L4gx4b+ P4ax4b K8d-8c P2d-2e K8c-8b K2c-2d +R7cx6d K2d-3e +R6d-6e K3e-2f P*7c K2f-3g S7d-8c+ +B3dx8i +R6ex2e +B8ix9i +B1ax2b S3cx2b
+0049KY,T60
'4795
'L*4i N*6e G6bx5b +B3dx5b G*6b G*1a G6bx5b S5ax5b P6c-6b+ G1ax1b L*1d S4b-4c L1dx1b+ N6ex7g+ N8ix7g S2b-1a L1ix1e B*5e P*3c G3bx3c K7ex8d B5ex6f K8d-8c P*1d L1ex1d S1ax1b L1dx1b+
-3425UM,T61
'4618--
'N*6e G7g-7f N6e-5g+ K7ex8d +B3d-4e +R7c-7a P*8a G6bx5b S5ax5b G*6b S4b-4c +R7ax8a G3b-4b K8d-8c K3a-3b S7d-7c+ K3b-3c
+7371RY,T60
'5416
'+R7c-7a P*7b G6bx7b P6d-6e P6c-6b+ P6ex6f P*6c P6f-6g+ G7gx6g P*6a +P6bx6a +B2e-3d L*5f +B3d-4d L1ix1e P*1a L4ix4g +B4dx9i L4gx4b+ G3bx4b +P6ax5a P1ax1b
-0065KE,T61
'4880--
'N*6e G7g-7f N6e-5g+ K7ex8d S2b-3c K8d-8c G3b-2b +B1b-1a K3a-3b L*4e G5bx6b P6cx6b+ G*1b +R7ax5a S4bx5a +R9ax5a P2c-2d
+7776KI,T61
'5266
'G7g-7f N6e-5g+ L*4e +N5g-5f S7d-7c+ S2b-3c K7e-7d +B2e-3f G6bx5b +B3fx4e L4ix4g +N5fx4g P6c-6b+ G3b-2b +B1b-1a S5ax5b +P6bx5b K3a-3b L1ix1e
-6557NK,T60
'4932++
'N6e-5g+ L*4e S2b-3c G6bx5b +B2ex5b G*6b G3b-2b +B1b-1a +B5b-3d L4ex4b+ S3cx4b G6bx5a K3a-3b G5ax4a K3b-3c G4ax4b K3c-4d L4ix4g K4d-3e +B1ax2b +N5gx4g +B2b-5e K3e-3f +R7ax2a G*4f +R2a-3b
+0045KY,T60
'5654
'L*4e S2b-3c G6bx5b +B2ex5b G*6b G3b-2b +B1b-1a +B5b-3d L4ex4b+ S3cx4b G6bx5a K3a-3b G5ax4a K3b-3c G4ax4b P*7c S7dx7c+ K3c-4d S*3f K4d-5e S*3e +B3dx8i +B1ax2b L*3c P6c-6b+ K5e-5f +B2bx2c P*3d K7ex6d
-3243KI,T61
'5166++
'+N5g-5f K7ex8d +P4g-4f G6bx5b S5ax5b P6c-6b+ +P4fx4e G*6c S4b-4c G6cx5b S4cx5b L4ix4e G*6a +P6bx6a S5bx6a P*4c G*6b G*5a +N5fx6f G5ax6a G6bx6a +R7ax6a
+4947KY,T118
'5901
'L4ix4g K3a-3b G6bx5a +N5gx4g G5ax5b K3b-3c L4ex4c+ S4bx4c G5bx5c K3c-2d +B1bx2b S4c-3d +B2b-4d L*4c +B4d-5d P*3f +R7ax4a N2a-3c P*2f +B2ex2f S*1g +B2f-3e G5cx4c
-5747NK,T60
'5852++
'+N5gx4g P*3c S2bx3c G6bx5b S5ax5b P6c-6b+ S3c-2b +P6bx5b K3a-3b +P5bx4b K3b-3c L4ex4c+ K3c-2d +B1bx2b P*3c S*4d G*3d +B2bx2a +B2e-3f G*1g L*2e S*3e G3dx3e S4dx3c K2d-1d +B2a-3b
+0033FU,T61
'6312
'P*3c G4cx3c L4ex4b+ G5bx4b G6bx5a K3a-3b G5ax4a G3c-3d G4ax4b K3b-3c S*2g L*2f S*3f L2fx2g+ S3fx2e G3dx2e B*4c K3c-2d G*3d K2d-1d +B1bx2b P1e-1f +R7ax2a K1d-1e +B2b-4d S*1d N*3i +N4g-3g K7ex6d P1f-1g+
-4333KI,T60
'6143++
'G4cx3c L4ex4b+ G5bx4b G6bx5a K3a-3b G5ax4a G3c-3d G4ax4b K3b-3c +R7ax2a K3c-4d +R2ax2b L*2f +R2bx2c K4d-3e S*5f P*4d N*3i +N4g-4f S5f-5e K3e-3f +R2cx3d +B2ex3d +B1bx3d K3f-3g S5ex4f K3g-3h +B3d-5f K3hx3i +R9a-3a
+4542NY,T60
'6292
'L4ex4b+ G5bx4b G6bx5a K3a-3b G5ax4a G3c-3d G4ax4b K3b-3c S*2g K3c-4d S*3f K4d-5e S3fx2e G3dx2e +B1bx2b K5e-4f +B2b-4d K4f-5g S*5i S*6h S5ix6h K5gx6h +B4d-3d K6h-5i +B3dx2e L*4e S*6g L*5g K7ex6d L5g-5h+ +B2e-3f P*4h +B3fx4e P4h-4i+
-5242KI,T60
'6282++
'G5bx4b G6bx5a L*6a +R9a-8b +N4g-4f G5ax4a K3ax4a +B1bx2a L*5a P6c-6b+ L6ax6b +R8bx6b +B2e-5b S*6c +B5bx6b S6cx6b+
+6251KI,T60
'6380
'G6bx5a K3a-3b G5ax4a G3c-3d G4ax4b K3b-3c S*2g L*2f S*3f L2fx2g+ S3fx2e G3dx2e B*4c K3c-2d G*3d K2d-1d +B1bx2b P1e-1f +R7ax2a K1d-1e +B2b-4d S*1d +R2a-3b P*7a G3d-3e P*3c +R3bx3c S*2d S*3f K1e-2f G3ex2d K2f-3g S3fx2e K3g-4h
-3132OU,T60
'6456++
'L*6a +R9a-8b +B2e-4c S*5b +B4cx7f K7ex7f K3a-3b K7f-7e G3c-3d S5bx4a+ P*7b +R7ax7b K3b-3c +R7bx4b K3c-2d G5ax6a G*1a L*2g L*2e L2gx2e G3dx2e +R4b-4d P*3d G*4e L*3c
+5141KI,T60
'6597
'G5ax4a G3c-3d G4ax4b K3b-3c S*2g K3c-4d S*3f K4d-5e +B1bx2b K5e-4f S3fx2e G3dx2e +B2b-4d L*3c +R7ax2a L3c-3h+ +R2ax2c K4f-3g +R2cx2e K3g-4h K7ex6d +N4g-3g K6dx5c P*4f L1ix1e P4f-4g+ P6c-6b+ K4h-4i K5c-5b +N3gx2g
-3334KI,T60
'6568++
'G3c-3d G4ax4b K3b-3c +R7ax2a K3c-4d +R2ax2b K4d-3e +R2bx2c L*8b +R9ax8b P*3f +R2c-4c P*4d N*3i K3e-4f +B1bx3d +B2ex3d +R4cx3d B*5h G*5i B5hx7f+ K7ex7f
+4142KI,T60
'6708
'G4ax4b K3b-3c S*2g K3c-4d S*3f K4d-5e +B1bx2b K5e-4f S3fx2e G3dx2e +B2b-4d P1e-1f +R7ax2a P*4c G4bx4c S*3e +B4dx5c L*7b P6c-6b+ L*2f +B5cx6d P*5e +P6bx7b L2fx2g+ K7ex8d P1f-1g+ +R2ax2c
-3233OU,T60
'6718++
'K3b-3c +R7ax2a K3c-4d +R2ax2b K4d-3e +R2bx2c L*2d S*5f P*3c +R2cx3c K3e-4f +R3cx3d +B2ex3d +B1bx3d R*3i +B3dx2d K4f-3g +R9a-3a P*3f K7ex6d L*7b +B2d-2e
+0027GI,T60
'6624
'S*2g K3c-4d S*3f K4d-5e +B1bx2b K5e-4f S3fx2e G3dx2e +B2b-4d S*3c +B4dx5c S3cx4b +B5cx4b P*5e +B4bx6d P*6a B*7c G*4e +R7ax6a P*3f +B6d-5d N2a-3c S*4d K4f-3g S4dx3c K3gx2g +B5dx4e K2g-3h
-0026KY,T60
'6385++
'K3c-4d S*3f K4d-5e S3fx2e G3dx2e P*4h K5e-5f P4hx4g L*2f S2g-3h L*3e N*4i K5f-6g G*6i K6g-5f G6i-5h
+0036GI,T60
'6873
'S*3f K3c-2d S3fx2e G3dx2e S2gx2f G2ex2f +B1bx2b K2d-3e +R7ax2a K3e-4e K7ex6d K4e-3f +R9a-3a P*3e K6dx5c +N4g-4h +B2bx2c P*5a G4bx5a S*5e G*4e K3f-3g +R3ax3e G2f-3f
-3324OU,T60
'8434++
'K3c-2d +B1bx2b L*6a S3fx2e G3dx2e S2gx2f G2ex2f +B2b-4d S*3e G*3d K2d-1d G3dx3e S*2d G3e-3d K1d-2e S*4e P*3f +R7ax6a P1e-1f +R6ax2a P1f-1g+ K7ex6d P3f-3g+ L1ix1g
+3625GI,T60
'7155
'S3fx2e G3dx2e S2gx2f G2ex2f +B1bx2b K2d-3e +R7ax2a K3e-4f +B2b-4d S*3e S*5e K4f-3g +B4dx3e K3g-4h +B3ex2f P*3g S*5f P*4f K7ex6d P5c-5d K6dx5d K4h-4i +R2ax2c P3g-3h+ +B2fx1e +N4g-5g +R9a-3a
-3425KI,T60
'6706++
'G3dx2e S2gx2f G2ex2f +B1bx2b L*3c +R7ax2a K2d-3e +B2bx3c K3e-4f +R2ax2c G2f-3f K7ex6d K4f-5g G*5i
+2726GI,T60
'7316
'S2gx2f G2ex2f +B1bx2b K2d-3e +R7ax2a K3e-4f +B2b-4d S*3e S*5e K4f-3g +B4dx3e K3g-4h +B3ex2f P*3g S*5f P*4f K7ex6d K4h-3i K6dx5c P*5g N*5i +N4g-3h S5ex4f P5g-5h+ S4fx3g K3i-4i P6c-6b+ +N3hx3g +B2fx3g
-2526KI,T60
'7004++
'G2ex2f +B1bx2b L*3c +R7ax2a K2d-3e +B2bx3c K3e-4f +R2ax2c G2f-3f K7ex6d +N4g-5h +R2c-2h K4f-5g P6c-6b+ G3f-4g L*4i P*4h +B3c-2d
+1222UM,T60
'7364
'+B1bx2b K2d-3e +R7ax2a P*7c S7dx7c+ K3e-4f +B2b-4d P*7d K7ex7d S*3g +R2a-3b K4f-5g S*5i +N4g-5h N*4i K5g-4g P*4h K4g-3h +B4dx2f
-2435OU,T60
'7147
'K2d-3e S*5f +N4g-4f +B2b-5e S*4d G*3d K3e-3f G3dx4d P5c-5d G4dx5d P*7c S7dx7c+ P*7b +R7ax7b K3f-3g K7ex6d K3g-2h +B5ex4f P*3g +R9ax2a K2hx1i S*3i
+7121RY,T60
'7346
'+R7ax2a K3e-4f +B2b-4d S*3e S*5e K4f-3g +B4dx3e K3g-4h +B3ex2f P*3g S*5f P*4f K7ex6d K4h-4i +R9a-3a S*4h K6dx5c P3g-3h+ S7d-6e P*5g +B2fx1e P5g-5h+ P6c-6b+
-3546OU,T61
'6751++
'K3e-4f +B2b-4d P*7c +B4dx2f P7cx7d N6fx7d P*3d B*7i S*5g S*5e
+2244UM,T60
'7542
'+B2b-4d S*3e S*5e K4f-5g +B4dx3e K5g-4h +B3ex2f P*3g S*5f P*4f K7ex6d K4h-4i +R9a-3a S*4h K6dx5c P3g-3h+ S7d-6e L*8g L1ix1e L8gx8i+ L1e-1c+ +N4g-5g S5ex4f +N5gx5f S6ex5f
-0035GI,T60
'7133
'S*3e B*5e K4f-5g +B4dx3e K5g-5h +B3ex2f P*4h S*5f L*4e K7ex6d P*6g +R2a-3b S*4f B5e-2b+ P6g-6h+ K6dx5c S4f-5g+ S5fx4e P4h-4i+ +R9a-5a +P4i-4h P*4c K5h-4i P6c-6b+ +P6h-5h L1ix1e +N4g-4f
+0055GI,T60
'7604
'S*5e K4f-5g +B4dx3e K5g-4h +B3ex2f P*3g S*5f P*4f K7ex6d P5c-5d N6fx5d P*7h K6d-5c P7h-7i+ L1ix1e K4h-4i L1e-1c+ P3g-3h+ +L1cx2c +N4g-5g S5f-6e S*3g +B2f-1f +P7ix8i
-4637OU,T61
'6897++
'K4f-5g +B4dx3e K5g-4h +B3ex2f P*3g +R2ax2c S*3h K7ex6d P*2g P6c-6b+ P2g-2h+ G7f-6e K4h-3i K6dx5c P*4f +B2fx1e P*5g S7d-6c+ S3h-2g +R9a-3a P3g-3h+ G*2f
+4435UM,T60
'7671
'+B4dx3e K3g-4h +B3ex2f P*3g S*5f P*4f K7ex6d K4h-4i +R9a-3a S*5h K6dx5c P3g-3h+ B*2g P*3g P6c-6b+ K4i-5i S5f-6e P*5f L1ix1e S5h-6g G7f-7g P5f-5g+ L1e-1c+ S6g-5f+
-3748OU,T61
'7088
'K3g-4h +B3ex2f P*3g K7ex6d L*5f +R9a-3a L5f-5g+ K6dx5c P*6g P6c-6b+ P6g-6h+ S7d-6c+ P*7h P9f-9e P7h-7i+ N6f-5d K4h-5i P9ex9d P*4h K5c-5b P3g-3h+ P9d-9c+ P*6g +R2ax2c P*3g L1ix1e +P7ix8i +R3a-3f +P8ix9i
+3526UM,T60
'7641
'+B3ex2f P*3g S*5f P*4f K7ex6d L*2d +B2f-3f P3g-3h+ K6dx5c P*5g +R2ax2c L2d-2h+ L1ix1e P5g-5h+ P6c-6b+ +N4g-5g S5f-4e P4f-4g+ L1e-1c+ S*6g G7f-6e +P3h-3g +B3f-2f +L2h-2g
-0037FU,T60
'7163++
'P*3g K7ex6d P5c-5d N6fx5d P*6g K6d-5c P*5g +R9a-3a P5g-5h+ +R3a-3f P6g-6h+ P6c-6b+ +P5h-5g N*6e K4h-5i K5c-5b P*4h S7d-6c+ P3g-3h+ N6e-5c+ P*7h L*4d P7h-7i+ P*7d +P7ix8i P7d-7c+
+0056GI,T60
'7764
'S*5f S*5h K7ex6d K4h-4i K6dx5c P3g-3h+ B*2g K4i-3i +R9a-3a P*3g P6c-6b+ L*2d P*2e L2dx2e +B2fx2e +N4g-5g S5f-6e S5h-6g G7f-7g +N5g-5f G*4i K3ix4i
-0046FU,T61
'6918++
'P*4f K7ex6d L*2d P*2e L2dx2e +B2fx2e P3g-3h+ +R9a-3a P*3g +R3a-3f K4h-3i K6dx5c +P3h-2h P6c-6b+ P3g-3h+ S7d-6c+ +N4g-5g +R3fx4f +N5gx5f +R4fx5f P*4h G7f-6e P4h-4i+ P9f-9e +P4i-4h P9ex9d +P2hx1i N6f-5d
+7564OU,T61
'7798
'K7ex6d K4h-4i +R9a-3a S*4h K6dx5c P3g-3h+ P6c-6b+ P*7h S7d-6e S4h-3g +B2f-3f P7h-7i+ L1ix1e +P7ix8i L1e-1c+ +P8ix9i +L1cx2c +N4g-5g +R2a-3b +N5gx5f S6ex5f
-0054KY,T60
'6994++
'L*2d P*2e L2dx2e +B2fx2e P3g-3h+ +R9a-3a P*3g +R3a-3f K4h-3i K6dx5c +N4g-5g +R3fx4f +N5gx5f +R4fx5f P*4h G7f-6e P4h-4i+ +R5f-4f K3i-2i L1ix1e S*3c G4b-4c S*4b G4cx4b S3cx4b +R4fx4b K2i-1i
+6654KE,T61
'7911
'N6fx5d P5cx5d K6dx5d P*5g P6c-6b+ P5g-5h+ K5d-5c P*7h +B2fx1e P7h-7i+ +R2ax2c +P7ix8i +R2c-2h K4h-5i +R2h-2i S*4i S7d-6c+ +P8ix9i +R9ax9d K5i-6i P9f-9e +N4g-5g
-5354FU,T60
'7148
'P5cx5d K6dx5d K4h-4i +R9a-3a S*4h L1ix1e P*5g P6c-6b+ P5g-5h+ K5d-5c P3g-3h+ S7d-6c+ N*8g K5c-5b S4h-3g +B2f-3f +P5h-5g S5f-4e S3g-4h+ G7f-6e N8gx9i+ S5ex4f +N4gx4f +B3fx4f +S4h-4g +B4f-3f +N9ix8i +R2ax2c P*3g
+6454OU,T60
'8042
'K6dx5d K4h-5i P6c-6b+ K5i-4i +R9a-3a P*5g +B2fx1e P5g-5h+ +R2ax2c P3g-3h+ S7d-6c+ P*3g +B1e-2d +N4g-5g S5f-4e P4f-4g+ K5d-5c N*8g L1i-1c+ N8gx9i+ N8i-7g
-0057FU,T60
'7208++
'K4h-4i +R9a-3a S*4h K5d-5c P*5g P6c-6b+ P5g-5h+ S7d-6c+ P3g-3h+ L1ix1e N*8g L9i-9g S4h-3g +B2f-1f S3g-4h+ G7f-6f +N4g-5g S5f-4e P4f-4g+ L1e-1c+ N8g-9i+ N8i-7g
+6362TO,T60
'8257
'P6c-6b+ P*7h +B2fx1e P7h-7i+ N8i-7g K4h-3i K5d-5c +P7i-8i S7d-6c+ +P8ix9i +R2ax2c L*2h N7g-6e L2h-2i+ P9f-9e +L2ix1i +R9ax9d P5g-5h+
-4849OU,T60
'7130++
'P5g-5h+ K5d-5c K4h-4i S7d-6c+ P3g-3h+ +R9ax9d +N4g-5g S5f-4e P4f-4g+ K5c-5b P2c-2d G7f-6e P2d-2e +B2fx1e +P3h-4h +R2ax2e P*3h +R9dx8d P*6a +P6bx6a S*5f S4ex5f +N5gx5f S5e-6d P3h-3i+ +R2e-2h
+5453OU,T60
'8504
'L1ix1e P3g-3h+ K5d-5c N*8g L1e-1c+ N8gx9i+ +L1cx2c P5g-5h+ S7d-6c+ +N4g-5g S5f-6e S*5f N8i-7g S5fx6e G7fx6e +N5g-5f K5c-5b +N5fx5e G6ex5e +N9i-8i
-3738TO,T60
'7338++
'P5g-5h+ S7d-6c+ P3g-3h+ K5c-5b +N4g-5g S5f-4e P4f-4g+ +R9ax9d P*6g L1ix1e P2c-2d +R2ax2d +P3h-4h G7f-6e P6g-6h+ S4e-5d K4i-5i +R9dx8d K5i-6i S5d-5c+ P*7h +R2d-2a
+1915KY,T60
'8690
'L1ix1e P2c-2d L1e-1c+ +N4g-4h +R9ax9d N*8b +R9dx8d N8bx7d S5ex4f P5g-5h+ K5c-5b N7dx8f G7fx8f S*8h G8f-7e P2d-2e +R2ax2e S8hx9i+ P9f-9e +S9i-9h
-5758TO,T61
'7383
'P5g-5h+ S7d-6c+ +N4g-5g S5f-4e P4f-4g+ K5c-5b P2c-2d +R2ax2d N*8g +R9ax9d N8gx9i+ +R9dx8d +N9ix8i G7f-6e P*6g +R2d-2a +P3h-4h +R8d-7d K4i-5i S4e-5d K5i-6i S5d-5c+ P*8g S5e-6d P6g-6h+ P9f-9e P8g-8h+ N*5e +N5g-5f L1e-1b+ +P8h-8g
+1513NY,T61
'8903
'L1e-1c+ +N4g-5g S5f-6e P4f-4g+ S7d-6c+ +N5g-6g +L1cx2c +P4g-4f +B2f-3e N*8g S5ex4f P*4g +R9ax9d N8gx9i+ +R9dx8d +N9ix8i +R8d-3d P4g-4h+ K5c-5b
-4757NK,T61
'7196++
'+N4g-5g S5f-4e P4f-4g+ S7d-6c+ P2c-2d +R9a-3a +P4g-3g P9f-9e P9dx9e L9ix9e P*4g G7f-6f P4g-4h+ L9e-9b+ P*7h +R2ax2d P7h-7i+ +R2dx8d +P7ix8i K5c-5b
+5665GI,T60
'8970
'S5f-6e P2c-2d S7d-6c+ P4f-4g+ +R9ax9d N*8g +R9dx8d N8gx9i+ +R8dx2d S*8g G7f-6f S8gx9f P8f-8e +P5h-6h P8e-8d +N9ix8i K5c-5b +P6h-6g G6f-7e S9f-8g+
-4647TO,T61
'7346++
'P4f-4g+ S7d-6c+ S*4f G7f-6f +N5g-6g G6f-5f S4fx5e G5fx5e S*3g +B2f-2g S3g-4f G5e-6d +N6g-6f K5c-5b +N6fx6e G6dx6e K4i-4h
+1323NY,T61
'9016
'+L1cx2c +P4g-4f S5ex4f N*3d +B2f-3f N3dx4f S7d-6c+ +P3h-4h K5c-5b N4f-3h+ +R9ax9d +N5g-6g +B3f-4e +P5h-5g +R9dx8d S*8g +R8d-3d S8gx7f+ S6ex7f P*8h P8f-8e P8hx8i+ P8e-8d +P8ix9i P8d-8c+
-0046GI,T144
'7424
'S*4f S5ex4f +P4gx4f S7d-6c+ P*3f +R9ax9d +P5h-6h K5c-5b P3f-3g+ G*5c P*4g G7f-6f N*8g S6e-6d N8gx9i+ N8i-7g +P6h-6g G6f-6e P4g-4h+ +R9dx8d +P6gx7g +R8d-7d +P4f-3f +B2f-4d
+5546GI,T61
'9293
'S5ex4f +P4gx4f K5c-5b +P5h-6h S7d-6c+ +P6h-7h N8i-7g +P4f-3g +R9ax9d +N5g-6g G7f-7e +P7hx7g G7ex8d N*8g G8d-7c N8gx9i+ +R9d-3d +P7g-8g S6e-5f +P8gx8f S5fx6g +P8fx9f +B2f-1g P*2h +B1g-3e P2h-2i+
-4746TO,T60
'7613
'+P4gx4f S7d-6c+ P*3f +R9ax9d P3f-3g+ K5c-5b +P3g-3f +B2f-1e P*6g G*5c N*8g +R9dx8d N8gx9i+ +R8d-3d P*2f G7f-6f +N9ix8i S6e-6d P6g-6h+ +B1e-3c +N5g-4h G6f-6e P2f-2g+ +L2c-3b
+7463NG,T60
'9421
'K5c-5b P*6g S7d-6c+ P6g-6h+ +R9ax9d +P6h-7h +R9dx8d +P7hx8i +R8d-4d P*4g P8f-8e +P8ix9i G7f-6f P4g-4h+ G*7c +P9i-8i +B2f-3e +P4f-5f S6ex5f +N5gx5f G6fx5f +P8i-9i P8e-8d +P9i-8i P8d-8c+
-5848TO,T60
'7676++
'+P3h-3g K5c-5b P*3h +R9ax9d P*4g G*5c +P3g-3f +B2f-1f P*2f +L2c-3b N*8g S6e-6d N8gx9i+ G7f-6e P4g-4h+ N8i-7g P2f-2g+ +B1f-3d
+9194RY,T61
'9571
'K5c-5b S*8g G7f-7e S8g-8h+ +R9ax9d +S8hx9i G7ex8d +N5g-6g G8d-7c +S9ix8i +R9d-7d K4i-3i +R7d-3d P*8h P8f-8e P*2h S6e-6d +P4f-5g +R2a-9a P2h-2i+ S6d-5c+ +P2i-2h
-0036FU,T61
'7778++
'P*6g K5c-5b P*3f G7f-6f P3f-3g+ G*5c P6g-6h+ S6e-6d P*7h +R9dx8d P7h-7i+ N8i-9g +P3g-3f +B2f-2e P*2f G6f-6e P*5h +R8d-7d P2f-2g+ +L2c-3b
+5352OU,T60
'9907
'+R9dx8d +N5g-5f K5c-5b +P4f-4e +R8d-3d +N5f-5e S6e-6d P3f-3g+ G7f-7e +P4e-3f +B2f-1f +N5e-6f S6d-7c+ P*8h +B1f-1e P8hx8i+ +B1e-3c +N6f-5f
-3637TO,T60
'7821--
'P3f-3g+ G7f-6f +P4f-3f +B2f-1e P*4f G*5c P*6g +R9dx8d P6g-6h+ S6e-6d +N5g-6g G6f-6e +N6g-7h +B1e-3c +N7hx8i +R8d-7d P*5g +L2c-3b +N8ix9i G*6f P4f-4g+
+9484RY,T60
'10240
'+R9dx8d N*8g N8i-7g N8gx9i+ N7g-8e +N9i-9h N8e-7c+ S*8g G7f-7e S8gx9f+ P8f-8e +P3g-3f +B2f-4d +N9h-9g +R8d-7d K4i-5h P8e-8d K5h-4g P8d-8c+ +S9f-8g G*5c K4g-3g +B4d-1a K3g-4g +P8c-9b +P4h-5i +B1a-4d
-3828TO,T60
'7742
'+P3h-2h +R8d-3d +P4f-3f +B2f-1e P*8g S6e-6d P8g-8h+ S6d-5c+ P*4f G7f-6e +P8hx9i +L2c-3b K4i-3h G*4c +N5g-4g +B1e-3c +P9ix8i G6e-6f
+8977KE,T60
'10558
'N8i-7g N*8g N7g-8e N8gx9i+ N8e-7c+ +P3g-3f +B2f-5c +N5g-5f +R8d-7d +P4f-4e P8f-8e +P4e-5e S6e-6d S*4e P8e-8d +P5e-5d +B5c-4c +P5dx6d +R7dx6d +P4h-5h P8d-8c+
-4636TO,T60
'8197--
'P*8g +R8d-3d P8g-8h+ N7g-8e +P4f-3f +B2f-1f +P2h-2g N8e-7c+ +P8hx9i S6e-6d +P9i-9h +B1f-1e P*4f S6d-5c+ +P9h-9g +B1e-3c +P9gx9f +L2c-3b K4i-3h P8f-8e +N5g-4g P8e-8d P*5g P8d-8c+
+2644UM,T60
'10767
'+B2f-4d P*8h N7g-8e P8h-8i+ N8e-7c+ +P4h-5h +R8d-7d +N5g-6g P8f-8e +P8ix9i +B4d-3d +P5h-5g P8e-8d K4i-3h P8d-8c+ +P9i-9h P9f-9e +P9h-9g G*5c K3h-4g +L2c-3c
-0067FU,T60
'8266++
'P*6g +R8d-5d +N5g-4g N7g-8e N*8g N8e-7c+ P6g-6h+ G*5c N8gx9i+ +L2c-3b +N9i-9h S6e-5f P*2f G7f-6e P2f-2g+ +B4d-3c K4i-3h +R5d-3d +N4g-5g S5f-4e
+7785KE,T60
'11026
'N7g-8e +N5g-6h N8e-7c+ +N6h-7h +R8d-7d P*8h P8f-8e P8h-8i+ +B4d-3d P6g-6h+ P9f-9e +P8ix9i P8e-8d K4i-3h P8d-8c+ K3h-2i P9e-9d +P4h-5i +L2c-3c +P9i-8i
-0087KE,T60
'8681--
'P6g-6h+ +R8d-5d +N5g-4g N8e-7c+ P*2f +L2c-3b P*1h +B4d-3c +P6h-7h +R5d-3d P2f-2g+ S6e-5d +P7h-8i G*5c K4i-3h S5d-4c+ P1h-1i+ S*5a +P8ix9i win
+8573NK,T60
'11161
'N8e-7c+ +P2h-2g P9f-9e K4i-3h L9i-9f +P2g-2f P8f-8e K3h-2g +R8d-7d +P3g-3h P8e-8d N8g-9i+ P8d-8c+ +N9i-9h P9e-9d +P4h-5h P9d-9c+ +N9h-9g +R7d-8e +N9gx9f +R8ex9f K2g-3g
-4858TO,T60
'8837
'+P4h-5h +R8d-5d N8gx9i+ S6e-6d +N9i-8i G7f-6e +N8i-7i +L2c-3b K4i-5i S6d-5c+ P6g-6h+ +B4d-6f +P3f-4g +R5d-3d P*4h P8f-8e P4h-4i+ P8e-8d
+9695FU,T60
'11307
'P9f-9e K4i-3h L9i-9f N8g-7i+ P8f-8e P*4f +R8d-7d +P3g-4h P8e-8d K3h-3g P8d-8c+ +P4h-5i +B4d-3d +P2h-3h P9e-9d P4f-4g+ P9d-9c+ +N7i-7h G7f-7e
-8799NK,T60
'8982
'N8gx9i+ S6e-6d +N9i-9h G7f-6e +N9h-9g S6d-5c+ P*4h +L2c-3b K4i-5i +B4d-6f P4h-4i+ P8f-8e K5i-4h +R8d-3d +N9g-9f P8e-8d +N9fx9e G*5a +N9e-8f +B6f-1a P*4f G*4c P4f-4g+ P8d-8c+
+4499UM,T60
'11404
'+B4dx9i K4i-4h P8f-8e K4h-4g +B9i-1a L*9b G*5c +P3f-4f +L2c-3b +P3g-2g P9e-9d K4g-3g +B1a-6f L9bx9d +R8dx9d K3g-3f +R9d-3d K3f-4g
-0046FU,T60
'9178++
'+P2h-3h +B9i-6f P6g-6h+ G*5c +P3h-4h +R8d-3d +P6h-6g +B6f-1a P*8h S6e-6d
+9911UM,T60
'11837
'P8f-8e K4i-5i +B9i-1a +N5g-4h +R8d-7d +N4h-3i P8e-8d +P3g-2g P8d-8c+ +P2h-2i G*5c +P2g-2f P9e-9d +P3f-2g P9d-9c+ P4f-4g+
-2827TO,T60
'9502--
'+N5g-4h +R8d-3d P6g-6h+ G*5c +P5h-5g S6e-6d +P5g-5f +L2c-3b P*3h G7f-6e P4f-4g+
+8685FU,T60
'12037
'P8f-8e K4i-5i +R8d-7d P*2h P8e-8d L*9b P8d-8c+ L9bx9e +R2a-9a L9e-9g+ +R9ax9g +P5h-6i +R9g-9a +P2g-2f +R9a-8a P2h-2i+ G*3c P6g-6h+
-4647TO,T60
'9562++
'P4f-4g+ +R8d-3d +N5g-4h P8e-8d +P3f-4f P8d-8c+ P6g-6h+ G*5c
+8474RY,T60
'12666
'+R8d-7d +P4g-4f P8e-8d P*5a G4bx5a L*8a G*4c +P3f-3e P9e-9d S*3d G*5c S3dx4c G5cx4c K4i-5i P9d-9c+ K5i-6i +S6c-5c +P3g-3f +P9c-8c
-3646TO,T60
'9670--
'+P5h-6h +L2c-3b P*3h G*5c P*4h G*4c P3h-3i+ S6e-6d
+8584FU,T60
'13275
'P8e-8d P*4a +R2ax4a +P4f-4e P8d-8c+ +P4g-4f +R4a-2a +N5g-5f G*5c P*4a G4bx4a L*8a +P8c-7b +N5f-5e +P7bx8a +N5ex6e
-0041FU,T60
'10331--
'L*7a +P6bx7a P*5a
+2141RY,T60
'13601
'+R2ax4a +N5g-5f P8d-8c+ +N5f-5e +B1ax5e +P3g-3h +R4a-2a P*1f G7f-8f K4i-5i +B5e-1a P1f-1g+ G8f-7e K5i-6i P9e-9d +P3h-4i P9d-9c+ P6g-6h+
-4645TO,T60
'10804--
'P6g-6h+ P8d-8c+ +N5g-6g G*5c L*6a +R4ax6a
+8483TO,T60
'14153
'P8d-8c+ +P4e-5e +B1ax5e +N5g-5f +B5ex5f +P4g-5g +B5f-3d +P2g-2f +R4a-2a K4i-3h P*5d K3h-2g P5d-5c+ S*2e N*1i K2g-2h +B3d-4d +P3g-3f P9e-9d K2hx1i P9d-9c+ P6g-6h+ +L2c-3c
-4555TO,T60
'11496++
'L*8d +R7dx8d +P4e-3f
+1155UM,T60
'14332
'+B1ax5e +P4g-4f +B5ex4f P*5d +R4a-2a K4i-4h +R7dx5d +N5g-4g +B4f-4e +P3g-3f +B4ex6g K4h-3g +R5d-7d +N4g-4f +B6g-3d K3g-4g G*3c K4g-3g
-3736TO,T60
'11840++
'+P3g-3f G*5c P*8d +P8cx8d L*9b +P8d-8c L9bx9e +R4a-2a L9e-9g+ G*4c
+4121RY,T60
'14534
'+R4a-2a +P4g-4f G*5c P*5a +P6bx5a L*7a P*7b L7ax7b +N7cx7b +P2g-2f K5b-6b K4i-3h +P5a-5b P*4a G4bx4a +N5g-5f S6ex5f +P4fx5f +B5ex5f K3h-2g G*7c P6g-6h+ G4a-4b
-4746TO,T60
'11577--
'+P4g-4f G*5c P*5a +R2ax5a +N5g-5f +B5e-1a L*8b +N7cx8b
+0053KI,T60
'15176
'G*5c +N5g-5f +B5e-1a P*4a G4bx4a L*8a +P8c-7b S*8c +N7cx8c L8ax8c +R7dx8c N*9a +R8c-7d K4i-3h G4a-4b +P3f-3e +R2ax9a K3h-4g G*4c P6g-6h+
-5756NK,T60
'12411--
'+N5g-5f S6ex5f +P4fx5f +B5ex5f P6g-6h+ G7f-6e S*4g +B5f-6f S4g-3h+ +R7d-3d +P2g-3g +L2c-3b P*4g +B6f-1a
+5511UM,T60
'16155
'+B5e-1a S*4d +B1ax4d +N5f-6f G7fx6f K4i-5i G6fx6g +P4f-5g G6g-7f +P5g-5f S6ex5f +P5h-5g S5f-6e K5i-4i P*7b P*4a G4bx4a +P2g-1g P7b-7a+
-0061KY,T60
'13797++
'L*6a +P6bx6a P*4a G*3i K4i-5i G4bx4a S*9c +P8cx9c K5i-6i +P9c-8c K6i-7i G4a-4b P6g-6h+ P*7b
+6261TO,T60
'17317
'+P6bx6a S*4d +B1ax4d +N5f-5e +B4dx5e +P4f-5f +B5ex5f P6g-6h+ +P6a-6b +P6h-7i +B5f-3d +P7i-7h +B3dx7h +P2g-2f +B7h-3d +P3f-3e +B3dx3e P*4a +B3ex2f P4ax4b +R7d-4d G*4g +R4dx4g K4i-3i
-0072GI,T38
'+Mate:10
'S*7b +P8cx7b P*7a G*3i K4i-5i +P6ax7a +N5f-6f S*5a +N6fx7f win
+7372NK,T10
'+Mate:7
'+N7cx7b P*4a G4bx4a +P4f-3g G*4c P6g-6h+
-0041FU,T2
'+Mate:8
'P*4a G*3i K4i-5i G4bx4a +N5f-6f B*9c +N6fx7f win
+4241KI,T1
'+Mate:5
'G4bx4a +P4f-3g G*4c P6g-6h+
-5666NK,T1
'+Mate:4
'+N5f-6f G*4c +N6fx7f win
+0043KI,T1
'+Mate:3
'G*4c +N6fx7f
-6768TO,T1
'+Mate:2
'P6g-6h+ win
%JISHOGI,T1
'Win by entering king declaration."""

}
