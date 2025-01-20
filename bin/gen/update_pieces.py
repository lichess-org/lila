#!/usr/bin/env python3
import base64
import sys
from pathlib import Path

lila_root_dir = Path(__file__).parents[2]

piece_style = sys.argv[1]

pieces = {'wP': '.pawn.white', 'wN': '.knight.white', 'wB': '.bishop.white',
          'wR': '.rook.white', 'wQ': '.queen.white', 'wK': '.king.white',
          'bP': '.pawn.black', 'bN': '.knight.black', 'bB': '.bishop.black',
          'bR': '.rook.black', 'bQ': '.queen.black', 'bK': '.king.black'}

def str_to_num(val: str) -> int | float:
    return (float if '.' in val else int)(val)

def deduce_svg_filepath(piece_abbrv: str) -> Path:
    return Path.joinpath(lila_root_dir, f"public/piece/{piece_style}/{piece_abbrv}.svg")

def update_svg(piece_abbrv: str, shift_amt: float | int, vertical: bool) -> None:
    svg_filepath = deduce_svg_filepath(piece_abbrv)
    with open(svg_filepath, 'r') as f: contents = f.read()
    curr_viewbox_str = contents.split('viewBox="', 1)[1].split('"', 1)[0]
    # Should be in a form like: 0 0 800 800
    new_viewbox_vals = curr_viewbox_str.split()
    i = int(vertical)
    new_viewbox_vals[i] = str(str_to_num(new_viewbox_vals[i]) + shift_amt)
    new_viewbox_str = ' '.join(new_viewbox_vals)
    new_contents = contents.replace(f'viewBox="{curr_viewbox_str}"', f'viewBox="{new_viewbox_str}"', 1)
    with open(svg_filepath, 'w') as f: f.write(new_contents)

def update_css() -> None:
    new_css_str = ''
    for piece_abbrv in pieces:
        with open(deduce_svg_filepath(piece_abbrv), 'r') as f:
            svg_encoded = base64.b64encode(f.read().encode()).decode()
        new_css_str += (
            ".is2d " + pieces[piece_abbrv] + " {background-image:url('data:image/svg+xml;base64," +
            svg_encoded + "')}\n"
        )
    with open(Path.joinpath(lila_root_dir, f"public/piece-css/{piece_style}.css"), 'w') as f:
        f.write(new_css_str)

def main() -> None:
    """Example: `python update_pieces.py *piece style name* *shift amount* *piece abbrvs, like wP or bR* *optionally, 'horizontal' to do that instead of vertical*
       To test this script, run it with a vertical shift of 0. Then, no files should be modified.
       This tests that the script works correctly, and that the existing svg and css
       files are in sync."""
    for piece_abbrv in pieces:
        update_svg(piece_abbrv, str_to_num(sys.argv[2]) if piece_abbrv in sys.argv else 0, 'horizontal' not in sys.argv)
    update_css()

if __name__ == '__main__':
    main()