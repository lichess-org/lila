# SVG icons

- Use lowercase kebab-case filenames.
- Include `xmlns` and `viewBox` on the `<svg>`. Do not set a fixed width or height.
- Use solid, opaque shapes on a transparent background. Color is ignored.
- Do not rely on color, gradients, or opacity. The icon must work as a single-color silhouette.
- Keep it static and self-contained. Do not use scripts, animation, external resources, fonts, or embedded images.
- Make details and gaps clear at small icon sizes.
- Usually set the `viewBox` to the smallest rectangle containing the artwork. It may be adjusted for visual center of gravity.
- Use at most one decimal place. Remove redundant points and commands.
- Run bin/gen/icons.py to generate scala, typescript, and scss integration sources after adding a new icon.
