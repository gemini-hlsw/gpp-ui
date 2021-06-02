// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ui.reuse

/*
 * Convert (A[, B, ...]) ==> Z into A ~=> (B [~=> ...] ~=> Z).
 */
protected trait ReusableInterop {
  import japgolly.scalajs.react._

  // Fn1
  implicit def toReusableFn1[A, R, B](ra: Reuse[A])(implicit ev: A =:= (R => B)): R ~=> B =
    Reusable.implicitly(new Fn1(ra.map(ev)))

  class Fn1[R, B](val f: Reuse[R => B]) extends Function1[R, B] {
    def apply(r: R): B = f.value()(r)
  }
  implicit def reusablityFn1[R, B]: Reusability[Fn1[R, B]] = Reusability.by(_.f)

  // Fn2
  implicit def toReusableFn2[A, R: Reusability, S, B](ra: Reuse[A])(implicit
    ev:                                                   A =:= ((R, S) => B)
  ): R ~=> (S ~=> B) =
    Reusable.implicitly(new Fn2(ra.map(ev)))

  class Fn2[R: Reusability, S, B](val f: Reuse[(R, S) => B]) extends Function1[R, S ~=> B] {
    def apply(r: R): S ~=> B = f.curry(r)
  }
  implicit def reusablityFn2[R, S, B]: Reusability[Fn2[R, S, B]] = Reusability.by(_.f)

  // Fn3
  implicit def toReusableFn3[A, R: Reusability, S: Reusability, T, B](ra: Reuse[A])(implicit
    ev:                                                                   A =:= ((R, S, T) => B)
  ): R ~=> (S ~=> (T ~=> B)) =
    Reusable.implicitly(new Fn3(ra.map(ev)))

  class Fn3[R: Reusability, S: Reusability, T, B](val f: Reuse[(R, S, T) => B])
      extends Function1[R, S ~=> (T ~=> B)] {
    def apply(r: R): S ~=> (T ~=> B) = f.curry(r)
  }
  implicit def reusablityFn3[R, S, T, B]: Reusability[Fn3[R, S, T, B]] = Reusability.by(_.f)
}
