package vdx.mtg4s

import java.net.URL

package object mtgjson {
  def getResource(name: String): URL =
    Thread.currentThread.getContextClassLoader.getResource(name)
}
