#! /usr/bin/python

#config
schemeName = "wood"
bgImgWhite = "../images/woodenboard_white.jpg"
bgImgBlack = "../images/woodenboard_black.jpg"

# adv. config
squareSize = 64 # width of a square in pixel

#code
white = True
for y in range(0,8):
	for x in range (0, 8):
		if white:
			img = bgImgWhite
		else:
			img = bgImgBlack
		print "#GameBoard."+schemeName+" #tcol"+str(x)+"trow"+str(y)+", div."+schemeName+" #"+str(chr(ord('a') + x))+str(8-y) + " {\n\tbackground: url("+img+") no-repeat "+str((-x)*squareSize)+"px " +str((-y)*squareSize) +"px;\n}";
		white = not white
	white = not white
