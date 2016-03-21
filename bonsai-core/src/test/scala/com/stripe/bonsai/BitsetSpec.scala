package com.stripe.bonsai

import org.scalatest.{ WordSpec, Matchers }
import org.scalatest.prop.Checkers

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Arbitrary._

import scala.collection.immutable.BitSet

class IndexedBitSetSpec extends WordSpec with Matchers with Checkers {

  implicit val arbitraryBitSet: Arbitrary[BitSet] =
    Arbitrary(arbitrary[Set[Short]].map(xs => BitSet(xs.map(_ & 0xffff).toSeq: _*)))

  case class BitSetWithIndex(bs: BitSet, i: Int)

  implicit val arbitraryBitSetWithIndex: Arbitrary[BitSetWithIndex] =
    Arbitrary(for {
      len <- arbitrary[Short].map(_.toInt)
      vals <- Gen.listOfN(len, arbitrary[Boolean])
      i <- Gen.choose(0, len)
      bs = BitSet(vals.zipWithIndex.filter(_._1).map(_._2): _*)
    } yield BitSetWithIndex(bs, i))

  "IndexedBitSet" should {
    "be equivalent to Set[Int]" in {
      check { (xs: BitSet) =>
        val bs = IndexedBitSet.fromBitSet(xs)
        xs.isEmpty || (0 to xs.max).forall { x => bs(x) == xs(x) }
      }
    }

    "be isomorphic to BitSet" in {
      check { (xs: BitSet) =>
        val bs = IndexedBitSet.fromBitSet(xs)
        xs == bs.toBitSet
      }
    }

    "rank(i) is equal to number of 1 bits below or at i" in {
      check { (bswi: BitSetWithIndex) =>
        val BitSetWithIndex(xs, i) = bswi
        val bs = IndexedBitSet.fromBitSet(xs)
        if (xs.isEmpty) true
        else bs.rank(i) == xs.filter(_ <= i).size
      }
    }

    "rank(select(i)) == i" in {
      check { (xs: BitSet) =>
        val bs = IndexedBitSet.fromBitSet(xs)
        xs.size == 0 || (1 to xs.size).forall { x =>
          bs(bs.select(x)) == true && bs.rank(bs.select(x)) == x
        }
      }
    }

    "select(i) is index of the i-th 1 bit" in {
      check { (xs: BitSet) =>
        val bs = IndexedBitSet.fromBitSet(xs)
        xs.size == 0 || (1 to xs.size).forall { x =>
          val i = bs.select(x)
          (0 to i).map(bs(_)).filter(_ == true).size == x
        }
      }
    }

    "bitCount returns # of set bits" in {
      check { (xs: BitSet) =>
        xs.size == IndexedBitSet.fromBitSet(xs).bitCount
      }
    }
  }
}
