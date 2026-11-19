package org.simplemodeling.textus.knowledgelake.material

import java.security.MessageDigest

/* Actual-byte comparison, independent of source identity and read stability.
 * @since   Nov. 12, 2026
 * @version Nov. 12, 2026
 */
private[knowledgelake] object MaterialContentIntegrity {
  enum Comparison {
    case Matched, Changed, Unverified, Unavailable
  }

  final case class Baseline private[MaterialContentIntegrity] (sha256: String, byteLength: Long)

  def baseline(bytes: Vector[Byte]): Baseline =
    Baseline(_sha256(bytes), bytes.size.toLong)

  def restore(sha256: String, byteLength: Long): Option[Baseline] =
    Option.when(Option(sha256).exists(_.matches("[0-9a-f]{64}")) && byteLength >= 0L)(
      Baseline(sha256, byteLength))

  def compare(expected: Option[Baseline], bytes: Option[Vector[Byte]]): Comparison =
    (expected, bytes) match {
      case (_, None) => Comparison.Unavailable
      case (None, Some(_)) => Comparison.Unverified
      case (Some(value), Some(actual)) =>
        if (value.byteLength == actual.size.toLong && value.sha256 == _sha256(actual)) Comparison.Matched
        else Comparison.Changed
    }

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(value => f"${value & 0xff}%02x").mkString
}
