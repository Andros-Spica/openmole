/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.lhs

import org.openmole.core.model.sampling.Factor
import org.openmole.ide.plugin.domain.range.RangeDomainDataUI
import org.openmole.plugin.sampling.lhs._
import org.openmole.core.model.data.Prototype
import org.openmole.plugin.domain.range.Range
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.implementation.sampling._
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.data.{ SamplingDataUI, DomainDataUI }

class LHSSamplingDataUI(val samples: String = "1") extends SamplingDataUI {

  def name = "LHS"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    LHS(
      samples,
      SamplingUtils.toFactors(factorOrSampling).map {
        f ⇒
          Factor(f.prototype.asInstanceOf[Prototype[Double]],
            f.domain.asInstanceOf[Range[Double]])
      }.toSeq: _*)
  }

  def coreClass = classOf[LHS]

  override def imagePath = "img/lhsSampling.png"

  def fatImagePath = "img/lhsSampling_fat.png"

  def buildPanelUI = new LHSSamplingPanelUI(this)

  //FIXME 2.10
  override def isAcceptable(domain: DomainDataUI) =
    domain match {
      case x: RangeDomainDataUI[_] ⇒ x.step match {
        case Some(s: String) ⇒ falseReturn
        case _               ⇒ true
      }
      case _ ⇒ falseReturn
    }

  private def falseReturn = {
    StatusBar().warn("A Bounded range of Double is required for a LHS Sampling")
    false
  }

  def isAcceptable(sampling: SamplingDataUI) = false

  def preview = "LHS (" + samples + ")"
}
