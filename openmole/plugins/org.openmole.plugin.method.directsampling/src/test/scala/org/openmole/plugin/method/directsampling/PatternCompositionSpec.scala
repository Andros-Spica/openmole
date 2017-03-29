
package org.openmole.plugin.method.directsampling

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.plugin.tool.pattern._
import org.scalatest._

class PatternCompositionSpec extends FlatSpec with Matchers {

  "Direct samplings" should "compose with loop" in {
    val counter = new AtomicInteger(0)

    val step = Val[Long]
    val seed = Val[Long]
    val l = Val[Double]

    val model =
      ClosureTask("model") {
        (context, _, _) ⇒
          counter.incrementAndGet()
          context
      } set (
        (inputs, outputs) += (step, seed, l),
        step := 1L
      )

    val loop = While(model, "step < 4", step)

    val mole =
      DirectSampling(
        Replication(
          loop,
          seed,
          2,
          42
        ),
        ExplicitSampling(l, Seq(0.1, 0.2))
      )

    mole.start.waitUntilEnded

    counter.intValue() should equal(12)
  }
}