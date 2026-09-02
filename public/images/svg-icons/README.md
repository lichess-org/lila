# SVG icons

- Use lower camelCase filenames.
- Include `xmlns` and `viewBox` on the `<svg>`. Do not set a fixed width or height.
- Use solid shapes on a transparent background.
- These icons are mostly applied as masks, so all color values save alpha will be ignored. Color is controlled by the host element `color` style.
- Alpha/`fill-opacity`/`stroke-opacity` affects mask strength.
- Keep it static and self-contained. Do not use scripts, animation, external resources, fonts, or embedded images.
- Make details and gaps clear at small icon sizes.
- Usually set the `viewBox` to the smallest rectangle containing the artwork. This may be adjusted for visual center of gravity.
- Use at most one decimal place. Remove redundant points and commands.
- If the icon should be mirrored in rtl languages, set `data-mirror-rtl="true"` on the svg element.
- Run `bin/gen/icons.py` to generate scala, typescript, and scss integration sources after adding a new icon.
