// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ui.reuse

import japgolly.scalajs.react.Reusability
import scala.reflect.ClassTag

protected trait AppliedSyntax {

  /*
   * Supports construction via the pattern `Reuse(reusedValue).by(valueWithReusability)`
   */
  class Applied[A](valueA: => A) {
    val value: () => A = () => valueA

    def by[R](reuseByR: R)(implicit classTagR: ClassTag[R], reuseR: Reusability[R]): Reuse[A] =
      Reuse.by(reuseByR)(valueA)

    def always: Reuse[A] = Reuse.by(())(valueA)
  }

  implicit class AppliedFn2Ops[A, R, S, B](aa: Applied[A])(implicit ev: A =:= ((R, S) => B)) {
    /*
     * Given a (R, S) => B, instantiate R and build a S ==> B.
     */
    def apply(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[S => B] =
      Reuse.by(r)(s => ev(aa.value())(r, s))

    /*
     * Given a (R, S) => B, instantiate R and S and build a Reuse[B].
     */
    def apply(
      r:         R,
      s:         S
    )(implicit
      classTagR: ClassTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[B] =
      Reuse.by((r, s))(ev(aa.value())(r, s))
  }

  implicit class AppliedFn3Ops[A, R, S, T, B](aa: Applied[A])(implicit ev: A =:= ((R, S, T) => B)) {
    /*
     * Given a (R, S, T) => B , instantiate R and build a (S, T) ==> B.
     */
    def apply(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[(S, T) => B] =
      Reuse.by(r)((s, t) => ev(aa.value())(r, s, t))

    /*
     * Given a (R, S, T) => B , instantiate R and S and build a T ==> B.
     */
    def apply(
      r:          R,
      s:          S
    )(implicit
      classTagRS: ClassTag[(R, S)],
      reuseR:     Reusability[(R, S)]
    ): Reuse[T => B] =
      Reuse.by((r, s))(t => ev(aa.value())(r, s, t))

    /*
     * Given a (R, S, T) => B , instantiate R, S and T and build a Reuse[B].
     */
    def apply(
      r:          R,
      s:          S,
      t:          T
    )(implicit
      classTagRS: ClassTag[(R, S, T)],
      reuseR:     Reusability[(R, S, T)]
    ): Reuse[B] =
      Reuse.by((r, s, t))(ev(aa.value())(r, s, t))
  }

  implicit class AppliedFn4Ops[A, R, S, T, U, B](aa: Applied[A])(implicit
    ev:                                              A =:= ((R, S, T, U) => B)
  ) {
    /*
     * Given a (R, S, T, U) => B , instantiate R and build a (S, T, U) ==> B.
     */
    def apply(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[(S, T, U) => B] =
      Reuse.by(r)((s, t, u) => ev(aa.value())(r, s, t, u))

    /*
     * Given a (R, S, T, U) => B , instantiate R and S and build a (T, U) ==> B.
     */
    def apply(
      r:          R,
      s:          S
    )(implicit
      classTagRS: ClassTag[(R, S)],
      reuseR:     Reusability[(R, S)]
    ): Reuse[(T, U) => B] =
      Reuse.by((r, s))((t, u) => ev(aa.value())(r, s, t, u))

    /*
     * Given a (R, S, T, U) => B , instantiate R, S and T and build a U ==> B.
     */
    def apply(
      r:          R,
      s:          S,
      t:          T
    )(implicit
      classTagRS: ClassTag[(R, S, T)],
      reuseR:     Reusability[(R, S, T)]
    ): Reuse[U => B] =
      Reuse.by((r, s, t))(u => ev(aa.value())(r, s, t, u))

    /*
     * Given a (R, S, T, U) => B , instantiate R, S, T and U and build a Reuse[B].
     */
    def apply(
      r:          R,
      s:          S,
      t:          T,
      u:          U
    )(implicit
      classTagRS: ClassTag[(R, S, T, U)],
      reuseR:     Reusability[(R, S, T, U)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u))(ev(aa.value())(r, s, t, u))
  }
}
