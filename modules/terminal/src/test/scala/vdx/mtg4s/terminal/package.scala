package vdx.mtg4s

import cats.kernel.Semigroup

package object terminal {
  def strToChars(s: String): List[Int] = s.toCharArray.map(_.toInt).toList

  def repeat[A](a: A, n: Int)(implicit S: Semigroup[A]): A =
    if (n <= 1) a
    else S.combine(a, repeat(a, n - 1))
}
