/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.systemexec

import java.io.File
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.implementation.tools._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.systemexec.SystemExecTask
import scala.collection.JavaConversions._
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }
import org.openmole.ide.core.implementation.serializer.Update
import org.openmole.ide.misc.tools.util.Converters._

@deprecated
class SystemExecTaskDataUI(val name: String = "",
                           val workdir: String = "",
                           val launchingCommands: String = "",
                           val resources: List[String] = List.empty,
                           val inputMap: List[(PrototypeDataProxyUI, String)] = List.empty,
                           val outputMap: List[(String, PrototypeDataProxyUI)] = List.empty,
                           val variables: List[PrototypeDataProxyUI] = List.empty,
                           val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                           val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                           val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends Update[SystemExecTaskDataUI010] {
  def update = new SystemExecTaskDataUI010(name,
    workdir,
    launchingCommands,
    resources.map { r ⇒ (new File(r), r.getName) },
    inputMap,
    outputMap,
    variables,
    inputs,
    outputs,
    inputParameters)
}

class SystemExecTaskDataUI010(val name: String = "",
                              val workdir: String = "",
                              val launchingCommands: String = "",
                              val resources: List[(File, String)] = List.empty,
                              val inputMap: List[(PrototypeDataProxyUI, String, Int)] = List.empty,
                              val outputMap: List[(String, PrototypeDataProxyUI, Int)] = List.empty,
                              val variables: List[PrototypeDataProxyUI] = List.empty,
                              val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                              val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                              val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty,
                              val stdOut: Option[PrototypeDataProxyUI] = None,
                              val stdErr: Option[PrototypeDataProxyUI] = None) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val syet = SystemExecTask(name, directory = workdir)(plugins)
    syet command Seq(launchingCommands.filterNot(_ == '\n'))
    initialise(syet)
    resources.foreach { case (file, name) ⇒ syet addResource (file, name = Some(name)) }
    variables.foreach {
      p ⇒ syet addVariable (p.dataUI.coreObject.get)
    }

    outputMap.foreach(i ⇒ syet addOutput (i._1, i._2.dataUI.coreObject.get.asInstanceOf[Prototype[File]]))
    inputMap.foreach(i ⇒ syet addInput (i._1.dataUI.coreObject.get.asInstanceOf[Prototype[File]], i._2))
    syet
  }

  def coreClass = classOf[SystemExecTask]

  override def imagePath = "img/systemexec_task.png"

  def fatImagePath = "img/systemexec_task_fat.png"

  def buildPanelUI = new SystemExecTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new SystemExecTaskDataUI010(name,
    workdir,
    launchingCommands,
    resources,
    Proxies.instance.filterListTupleIn(inputMap),
    Proxies.instance.filterListTupleOut(outputMap),
    Proxies.instance.filter(variables),
    ins,
    outs,
    params,
    stdOut,
    stdErr)

}
