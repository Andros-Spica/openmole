/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.statistics

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.plugin.task.statistics._
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }

class SumTaskDataUI(val name: String = "",
                    val sequence: List[(PrototypeDataProxyUI, PrototypeDataProxyUI)] = List.empty,
                    val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                    val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                    val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends StatDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = StatisticsTask(name)(plugins)
    sequence foreach {
      s ⇒
        gtBuilder add (
          s._1.dataUI.coreObject.get.asInstanceOf[Prototype[Array[Double]]],
          s._2.dataUI.coreObject.get.asInstanceOf[Prototype[Double]],
          sum)
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[StatisticsTask]

  override def imagePath = "img/sum.png"

  def fatImagePath = "img/sum_fat.png"

  def buildPanelUI = new SumTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new SumTaskDataUI(name, Proxies.instance.filterListTupleInOut(sequence), ins, outs, params)
}
