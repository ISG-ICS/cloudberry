package edu.uci.ics.cloudberry.zion

import org.joda.time.{DateTime, Duration}

trait IDataSet[T] {

  def name: String

  def filterableFields: Seq[IDataField]

  def groupableFields: Seq[String]

  def filter

  def group

  def aggregate
}

trait IDataField {
  def name: String

  def tpe: String

  def min: AnyVal

  def max: AnyVal

  def avg: AnyVal

  def std: AnyVal


}

trait IDataView[T, S] extends IDataSet[T] {
  def getQuery: IQuery

  // Maybe multiple datasets in case this view is build from join or other things,
  // but will defer this kind of multiple sources view until the actual requirement appears.
  def dataSource: IDataSet[T]

  def lastUpdateTime: DateTime

  def lastAccessTime: DateTime

  def duration: Duration

  def update
}

trait IQuery {

}
