/*
 *  Copyright (C) 2010 mathieu
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.datasetdistribution

import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.KeyToGroupMap
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.data.statistics.HistogramDataset
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.tools.VariableExpansion._
import scala.collection.JavaConversions._
import org.jfree.chart.ChartUtilities._
import org.jfree.chart.ChartFactory._
import org.jfree.chart.StandardChartTheme._

class  MultiDatasetDistributionTask(name: String,
                                    xLegends: ArrayList[String],
                                    outputDirectoryPath: String,
                                    nbCategories: String,
                                    chartTitle: String,
                                    xLegend: String,
                                    yLegend: String,
                                    imageWidth: Int,
                                    imageHeight: Int) extends GenericDatasetDistribution(name,
                                                                                         outputDirectoryPath,
                                                                                         nbCategories,
                                                                                         chartTitle,
                                                                                         xLegend,
                                                                                         yLegend,
                                                                                         imageWidth,
                                                                                         imageHeight) {

  private def createChart(dataset: DefaultCategoryDataset, context: IContext): JFreeChart = {
 // private def createChart(dataset: HistogramDataset, context: IContext): JFreeChart = {
    setChartTheme (createLegacyTheme)
  //  dataset:HistogramDataset = dataset.asInstanceOf[HistogramDataset]
  //  val chart = createBarChart(expandData(context, chartTitle),expandData(context, xLegend), expandData(context, yLegend), dataset, PlotOrientation.VERTICAL, false, false, false)


    val chart = createStackedBarChart(expandData(context, chartTitle),expandData(context, xLegend), expandData(context, yLegend), dataset, PlotOrientation.VERTICAL, false, false, false)

  /*  val plot = chart getXYPlot()
    val renderer:GroupedStackedBarRenderer = plot.getRenderer().asInstanceOf[GroupedStackedBarRenderer]
    val map = new KeyToGroupMap("h");
    map.mapKeyToGroup("Product 1 (US)", "h");
    renderer.setSeriesToGroupMap(map);*/


    chart.setAntiAlias(true)
    chart
  }

  override def process(context: IContext) = {
    try {
      val dataset = new DefaultCategoryDataset();
      charts foreach ( chart => {
          val data = context.value(chart._1).get
           val array = new Array[Double](data.size)
          var i = 0
          data foreach ( v => {
        //  val dataset = new HistogramDataset();
              dataset.addValue(v.doubleValue,chart._2,xLegends.get(i));
         // dataset addSeries("", array, expandIntegerData(context, nbCategories))
              //    array(i) = v.doubleValue
          //println("dataset.addValue " + v.doubleValue +", " +"h"+", "+ xLegends.get(i));

              i += 1
            } )

          //   val dataset = new HistogramDataset()
          //   dataset addSeries("", array, expandIntegerData(context, nbCategories))
          //
        } )
          val jfchart = createChart(dataset, context)


       /* GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();
        KeyToGroupMap map = new KeyToGroupMap("G1");*/

        //  println("STORE: " + expandData(global, context, outputDirectoryPath + "/" + "chart.png"));
          val os = new BufferedOutputStream(new FileOutputStream(expandData(context, outputDirectoryPath + "/" + "chart.png")));
          try {
            writeChartAsPNG(os,jfchart,imageWidth,imageHeight)
          } finally {
            os.close()
          }
          context
    } catch {
      case ex: FileNotFoundException => throw new UserBadDataError(ex)
      case ex: IOException => throw new InternalProcessingError(ex)
    }
  }
}

