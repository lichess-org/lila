import base64
import sys

pieces = {'wP': '.pawn.white', 'wN': '.knight.white', 'wB': '.bishop.white',
          'wR': '.rook.white', 'wQ': '.queen.white', 'wK': '.king.white',
          'bP': '.pawn.black', 'bN': '.knight.black', 'bB': '.bishop.black',
          'bR': '.rook.black', 'bQ': '.queen.black', 'bK': '.king.black'}

piece_style = sys.argv[1]
vertical_adjust = float(sys.argv[2]) if '.' in sys.argv[2] else int(sys.argv[2])

def make_svg_filename(piece_abbrv: str) -> str:
    return f"piece/{piece_style}/{piece_abbrv}.svg"

def get_svg_filename(css_piece_name: str) -> str:
    return make_svg_filename(next(k for k in pieces if pieces[k] == css_piece_name))

def get_css_piece_name(svg_filename: str) -> str:
    return pieces[svg_filename.split('/')[-1].split('.svg')[0]]

def update_svg(piece_abbrv: str) -> None:
    svg_filename = make_svg_filename(piece_abbrv)
    with open(svg_filename, 'r') as f: contents = f.read()
    curr_viewbox_str = contents.split('viewBox="')[-1].split('"')[0]
    # Should be in a form like: 0 0 800 800
    viewbox_vals = curr_viewbox_str.split()
    viewbox_vals[1] = str(vertical_adjust)
    updated_viewbox_str = ' '.join(viewbox_vals)
    contents = contents.replace(f'viewBox="{curr_viewbox_str}"', f'viewBox="{updated_viewbox_str}"', 1)
    with open(svg_filename, 'w') as f: f.write(contents)
    # Todo - add stuff for then updating the css lines.

def main() -> None:
    for piece_abbrv in pieces:
        update_svg(piece_abbrv)

if __name__ == '__main__':
    main()