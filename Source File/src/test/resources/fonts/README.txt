Put your font files here (and/or directly under assets/).

At runtime the app loads every .ttf and .otf from:
  - assets/fonts/
  - assets/ (root level only)

Run the app from the project root (e:\MiniProject\code) so "assets" resolves correctly.

Maven also copies these fonts into the JAR under /fonts/ for distribution.
