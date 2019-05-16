package org.openmole.gui.client.tool.plot

import org.openmole.gui.ext.data.{ SequenceData, SequenceHeader }
import Plot._
import scaladget.bootstrapnative.DataTable
import scalatags.JsDom.all._

case class IndexedAxis(title: String, fullSequenceIndex: Int)

case class ClosureFilter(closure: String = "", axis: Seq[IndexedAxis] = Seq())

object ClosureFilter {
  def empty = ClosureFilter("", Seq())
}

case class Plotter(
  plotDimension: PlotDimension,
  plotMode:      PlotMode,
  toBePlotted:   ToBePloted,
  error:         Option[IndexedAxis],
  closureFilter: Option[ClosureFilter]
)

object Plotter {
  def default = Plotter(
    ColumnPlot,
    ScatterMode,
    ToBePloted(Seq(0, 1)),
    None,
    None
  )

  def plot(sequenceData: SequenceData, plotter: Plotter) = {

    val dataNbLines = sequenceData.content.length
    val nbDims = plotter.toBePlotted.indexes.length

    if (dataNbLines > 0) {
      val dataRow = sequenceData.content.map {
        scaladget.bootstrapnative.DataTable.DataRow(_)
      }

      val dataNbColumns = sequenceData.header.length
      val indexes = plotter.toBePlotted.indexes.filterNot { _ >= dataNbColumns }

      val dims = plotter.plotDimension match {
        case ColumnPlot ⇒
          if (dataNbColumns >= nbDims) {
            val closureFilter = plotter.closureFilter.getOrElse(ClosureFilter.empty)
            indexes.foldLeft(Array[Dim]()) { (acc, col) ⇒
              acc :+ Dim(DataTable.column(col, dataRow).values.filter(v ⇒ jsClosure(closureFilter, v, col)), sequenceData.header.lift(col).getOrElse(""))
            }
          }
          else Array[Dim]()
        case _ ⇒ sequenceData.content.zipWithIndex.map { case (v, id) ⇒ Dim(v, id.toString) }.toArray
      }

      val (xValues, yValues) = {
        if (nbDims == 2) (dims.head, Array(dims.last))
        else (Dim((1 to dims.length).map(_.toString), ""), dims)
      }

      if (xValues.values.isEmpty || yValues.isEmpty) nothingToplot
      else
        org.openmole.gui.client.tool.plot.Plot(
          "",
          Serie(xValues, yValues),
          false,
          plotter,
          plotter.error.map { e ⇒
            Serie(Dim(DataTable.column(e.fullSequenceIndex, dataRow).values, sequenceData.header.lift(e.fullSequenceIndex).getOrElse("")))
          }
        )
    }
    else nothingToplot
  }

  val nothingToplot = div("No plot to display").render

  def toBePlotted(plotter: Plotter, data: SequenceData): (Plotter, SequenceData) = {
    val (newToBePlotted, newSequenceData) = plotter.plotDimension match {
      case ColumnPlot ⇒
        plotter.plotMode match {
          case SplomMode ⇒ (ToBePloted(plotter.toBePlotted.indexes.take(5)), data)
          case _ ⇒ (ToBePloted(plotter.toBePlotted.indexes.take(2)), data.withRowIndexes)
        }
      case LinePlot ⇒ (ToBePloted((1 to data.content.size)), data)
    }

    (plotter.copy(toBePlotted = newToBePlotted), newSequenceData)
  }

  def availableForError(header: SequenceHeader, plotter: Plotter) =
    plotter.plotDimension match {
      case LinePlot ⇒ Seq()
      case ColumnPlot ⇒
        header.zipWithIndex.filterNot {
          case (x, i) ⇒ plotter.toBePlotted.indexes.contains(i)
        }.map { afe ⇒
          IndexedAxis(afe._1, afe._2)
        }
    }

  def availableForClosureFilter(header: SequenceHeader, plotter: Plotter) = {
    val closure = plotter.closureFilter.map {
      _.closure
    }.getOrElse("")

    ClosureFilter(
      closure,
      header.zipWithIndex.filter {
        case (x, i) ⇒
          plotter.toBePlotted.indexes.contains(i)
      }.map { afe ⇒
        IndexedAxis(afe._1, afe._2)
      })
  }

  def jsClosure(closureFilter: ClosureFilter, value: String, col: Int) = {
    if (closureFilter.closure.isEmpty) true
    else {
      closureFilter.axis.find(_.fullSequenceIndex == col).map { pc ⇒
        closureFilter.closure.replace("x", value)
      }.map { cf ⇒
        scala.util.Try(scala.scalajs.js.eval(s"function func() { return ${cf};} func()").asInstanceOf[Boolean]).toOption.getOrElse(true)
      }.getOrElse(true)
    }
  }

}