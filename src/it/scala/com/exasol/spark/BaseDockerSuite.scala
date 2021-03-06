package com.exasol.spark

import com.exasol.spark.util.ExasolConfiguration
import com.exasol.spark.util.ExasolConnectionManager
import com.exasol.spark.util.Types._

import com.dimafeng.testcontainers.ExasolDockerContainer
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.Suite

/** A Base Integration Suite with Exasol DB Docker Container Setup */
trait BaseDockerSuite extends ForAllTestContainer { self: Suite =>

  override val container = ExasolDockerContainer()

  lazy val exaConfiguration = ExasolConfiguration(container.configs)

  lazy val exaManager = ExasolConnectionManager(exaConfiguration)

  val EXA_SCHEMA = "TEST_SCHEMA"
  val EXA_TABLE = "TEST_TABLE"
  val EXA_ALL_TYPES_TABLE = "TEST_ALL_TYPES_TABLE"
  val EXA_TYPES_NOT_COVERED_TABLE = "TEST_TYPES_NOT_COVERED_TABLE"

  def runExaQuery(queries: Seq[String]): Unit =
    exaManager.withConnection[Unit] { conn =>
      queries.foreach(conn.createStatement.execute(_))
      ()
    }

  def runExaQuery(queryString: String): Unit =
    runExaQuery(Seq(queryString))

  // scalastyle:off
  def createDummyTable(): Unit = {
    runExaQuery(s"DROP SCHEMA IF EXISTS $EXA_SCHEMA CASCADE")
    runExaQuery(s"CREATE SCHEMA $EXA_SCHEMA")
    runExaQuery(s"""
                   |CREATE OR REPLACE TABLE $EXA_SCHEMA.$EXA_TABLE (
                   |   ID INTEGER IDENTITY NOT NULL,
                   |   NAME VARCHAR(100) UTF8,
                   |   CITY VARCHAR(2000) UTF8,
                   |   DATE_INFO DATE,
                   |   UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                   |   UNICODE_COL VARCHAR(100) UTF8
                   |)""".stripMargin)
    runExaQuery(s"""
                   |INSERT INTO $EXA_SCHEMA.$EXA_TABLE (name, city, date_info, unicode_col)
                   | VALUES ('Germany', 'Berlin', '2017-12-31', 'öäüß')
                   | """.stripMargin)
    runExaQuery(s"""
                   |INSERT INTO $EXA_SCHEMA.$EXA_TABLE (name, city, date_info, unicode_col)
                   | VALUES ('France', 'Paris', '2018-01-01','\u00d6')
                   | """.stripMargin)
    runExaQuery(s"""
                   |INSERT INTO $EXA_SCHEMA.$EXA_TABLE (name, city, date_info, unicode_col)
                   | VALUES ('Portugal', 'Lisbon', '2018-10-01','\u00d9')
                   | """.stripMargin)
    runExaQuery("commit")
  }
  // scalastyle:on

  def createAllTypesTable(): Unit = {
    runExaQuery(s"DROP SCHEMA IF EXISTS $EXA_SCHEMA CASCADE")
    runExaQuery(s"CREATE SCHEMA $EXA_SCHEMA")
    val maxDecimal = " DECIMAL(" + getMaxPrecisionExasol() + "," + getMaxScaleExasol() + ")"
    runExaQuery(
      s"""
         |CREATE OR REPLACE TABLE $EXA_SCHEMA.$EXA_ALL_TYPES_TABLE (
         |   MYID INTEGER,
         |   MYTINYINT DECIMAL(3,0),
         |   MYSMALLINT DECIMAL(9,0),
         |   MYBIGINT DECIMAL(36,0),
         |   MYDECIMALSystemDefault DECIMAL,
         |   MYDECIMALMAX $maxDecimal,
         |   MYNUMERIC DECIMAL( 5,2 ),
         |   MYDOUBLE DOUBLE PRECISION,
         |   MYCHAR CHAR,
         |   MYNCHAR CHAR(2000),
         |   MYLONGVARCHAR VARCHAR( 2000000),
         |   MYBOOLEAN BOOLEAN,
         |   MYDATE DATE,
         |   MYTIMESTAMP TIMESTAMP,
         |   MYGEOMETRY Geometry)""".stripMargin
    )
    runExaQuery("commit")
  }

  def createTypesNotCoveredTable(): Unit = {
    runExaQuery(s"DROP SCHEMA IF EXISTS $EXA_SCHEMA CASCADE")
    runExaQuery(s"CREATE SCHEMA $EXA_SCHEMA")
    runExaQuery(
      s"""
         |CREATE OR REPLACE TABLE $EXA_SCHEMA.$EXA_TYPES_NOT_COVERED_TABLE (
         |   myInterval INTERVAL YEAR TO MONTH )""".stripMargin
    )
    runExaQuery("commit")
  }

}
