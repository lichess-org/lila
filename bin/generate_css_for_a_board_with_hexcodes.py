#! /usr/bin/python2

#config
themes = {
  'grey': ['#cacaca', '#fff'],
  'green': ['#86a666', '#ffffdd'],
  'blue': ['#8ca2ad', '#dee3e6'],
  'brown': ['#b58863', '#f0d9b5']
}

blackPattern = 'body.{name} #GameBoard td.blackSquare, body.{name} #GameBoard td.highlightBlackSquare, body.{name} div.lcs.black, body.{name} a.themepicker { background-color: {black}; }'
whitePattern = 'body.{name} #GameBoard td.whiteSquare, body.{name} #GameBoard td.highlightWhiteSquare, body.{name} div.lcs.white { background-color: {white}; }'

for name in themes:
  def formatCss(pattern):
    return pattern.replace('{name}', name).replace('{white}', themes[name][0]).replace('{black}', themes[name][1])
  print formatCss(whitePattern)
  print formatCss(blackPattern)
