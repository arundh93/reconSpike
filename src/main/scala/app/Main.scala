package app

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}


object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local[*]").getOrCreate()
    import spark.implicits._
    val posSchema = StructType(StructField("posId", StringType) :: StructField("accountKey", StringType) :: StructField("positionValue", DoubleType) :: StructField("posdate", StringType) :: Nil)
    val tranSchema = StructType(StructField("tranId", StringType) :: StructField("accountKey", StringType) :: StructField("transactionValue", DoubleType) :: StructField("trandate", StringType) :: Nil)

    val posDf: Dataset[Position] = spark.read.option("header", "true").schema(posSchema).csv("src/main/scala/data/sample/sept01/position.csv", "src/main/scala/data/sample/sept02/position.csv").as[Position]
    val tranDf = spark.read.option("header", "true").schema(tranSchema).csv("src/main/scala/data/sample/sept01/transaction.csv", "src/main/scala/data/sample/sept02/transaction.csv").as[Transaction]

    val window = Window.partitionBy("accountKey").orderBy("posdate")

    import org.apache.spark.sql.functions._

    val df = posDf.withColumn("previous_position", lag("positionValue", 1, 0).over(window))
      .withColumn("diff", col("positionValue") - col("previous_position"))


    val aggTransactions: DataFrame = tranDf.groupBy("accountKey", "trandate").sum("transactionValue")

    val joined = aggTransactions
      .join(df, aggTransactions.col("accountKey") === posDf.col("accountKey"))
      .filter($"posdate" === "02/09/2018")
      .withColumn("ReconPassed", $"sum(transactionValue)" <=> $"diff")
    df.show()
    aggTransactions.show()
    joined.show()

  }
}

case class Position(posId: String, accountKey: String, positionValue: Double, posdate: String)

case class Transaction(tranId: String, accountKey: String, transactionValue: Double, trandate: String)


