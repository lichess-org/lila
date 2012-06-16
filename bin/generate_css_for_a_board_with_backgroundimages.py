#! /usr/bin/python2

#config
schemeName = "wood"
bgImgWhite = "../images/woodenboard_white.jpg"
bgImgBlack = "../images/woodenboard_black.jpg"

# adv. config
squareSize = 64 # width of a square in pixel

#code
print "#GameBoard.wood td.whiteSquare, #GameBoard.wood td.highlightWhiteSquare, div.wood div.lcs.white { background: url(../images/woodenboard_white.jpg) no-repeat; }";
print "#GameBoard.wood td.blackSquare, #GameBoard.wood td.highlightBlackSquare, div.wood div.lcs.black, a.colorpicker.wood { background: url(../images/woodenboard_black.jpg) no-repeat; }";
white = True
for y in range(0,8):
  for x in range (0, 8):
    if white:
      img = bgImgWhite
    else:
      img = bgImgBlack
    print "#GameBoard."+schemeName+" #tcol"+str(x)+"trow"+str(y)+", div."+schemeName+" #"+str(chr(ord('a') + x))+str(8-y) + " { background-position: "+str((-x)*squareSize)+"px " +str((-y)*squareSize) +"px; }";
    white = not white
  white = not white
