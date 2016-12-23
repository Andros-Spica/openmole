package org.openmole.plugin.method.evolution

import shapeless._
import ops._

trait WorkflowIntegrationSelector[L <: HList, U] extends DepFn1[L] with Serializable {
  type Out = U
  def wi: WorkflowIntegration[U]
}

object WorkflowIntegrationSelector {

  def apply[L <: HList, U](implicit selector: WorkflowIntegrationSelector[L, U]): WorkflowIntegrationSelector[L, U] = selector

  implicit def select[H, T <: HList](implicit wii: WorkflowIntegration[H]): WorkflowIntegrationSelector[H :: T, H] =
    new WorkflowIntegrationSelector[H :: T, H] {
      def apply(l: H :: T) = l.head
      def wi = wii
    }

  implicit def recurse[H, T <: HList, U](implicit st: WorkflowIntegrationSelector[T, U], wii: WorkflowIntegration[U]): WorkflowIntegrationSelector[H :: T, U] =
    new WorkflowIntegrationSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.tail)
      def wi = wii
    }
}
