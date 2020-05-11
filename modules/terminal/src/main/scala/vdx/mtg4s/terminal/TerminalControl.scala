package vdx.mtg4s.terminal

object TerminalControl {
  def up(n: Int = 1): String = esc(s"${n}A")

  def down(n: Int = 1) = esc(s"${n}B")

  def forward(n: Int = 1) = esc(s"${n}C")

  def back(n: Int = 1) = esc(s"${n}D")

  def clearScreen() = esc("2J")

  def clearLine() = esc("0K")

  @SuppressWarnings(Array("DisableSyntax.throw"))
  def move(row: Int, column: Int) =
    if (row < 0 || column < 0) throw new Exception(s"Row and column cannot be less than 0: ($row, $column) ")
    else esc(s"${row};${column}H")

  def esc(s: String): String = s"\u001b[$s"
}
