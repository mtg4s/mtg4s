package vdx.mtg4s.terminal

import cats.data.Chain

package object linereader {
  private[linereader] type ByteSeq = Chain[Int]
}
