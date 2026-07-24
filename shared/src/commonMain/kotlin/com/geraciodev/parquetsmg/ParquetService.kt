package com.geraciodev.parquetsmg

import java.sql.DriverManager
import java.sql.Connection

class ParquetService {
    private var connection: Connection? = null

    private fun getConnection(): Connection {
        if (connection == null || connection!!.isClosed) {
            connection = DriverManager.getConnection("jdbc:duckdb:")
        }
        return connection!!
    }

    private fun buildWhereClause(columns: List<String>, searchQuery: String, columnFilters: Map<String, Set<String>>): String {
        val conditions = mutableListOf<String>()
        
        // General search
        if (searchQuery.isNotEmpty()) {
            val escapedQuery = searchQuery.replace("'", "''")
            val searchCondition = columns.joinToString(" OR ") {
                "CAST(\"$it\" AS VARCHAR) ILIKE '%$escapedQuery%'"
            }
            conditions.add("($searchCondition)")
        }
        
        // Column specific filters
        columnFilters.forEach { (column, values) ->
            if (values.isNotEmpty()) {
                val escapedValues = values.joinToString(", ") { "'${it.replace("'", "''")}'" }
                conditions.add("CAST(\"$column\" AS VARCHAR) IN ($escapedValues)")
            }
        }
        
        return if (conditions.isEmpty()) "" else " WHERE " + conditions.joinToString(" AND ")
    }

    fun getRows(
        filePath: String, 
        limit: Int, 
        offset: Int, 
        searchQuery: String = "", 
        columnFilters: Map<String, Set<String>> = emptyMap()
    ): List<Map<String, Any?>> {
        val conn = getConnection()
        val columns = getColumns(filePath)
        val whereClause = buildWhereClause(columns, searchQuery, columnFilters)
        
        val stmt = conn.createStatement()
        val query = "SELECT * FROM read_parquet('$filePath') $whereClause LIMIT $limit OFFSET $offset"
        val rs = stmt.executeQuery(query)
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        
        val results = mutableListOf<Map<String, Any?>>()
        while (rs.next()) {
            val row = mutableMapOf<String, Any?>()
            for (i in 1..columnCount) {
                row[metaData.getColumnName(i)] = rs.getObject(i)
            }
            results.add(row)
        }
        return results
    }

    fun getColumns(filePath: String): List<String> {
        val conn = getConnection()
        val stmt = conn.createStatement()
        val query = "SELECT * FROM read_parquet('$filePath') LIMIT 0"
        val rs = stmt.executeQuery(query)
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        val columns = mutableListOf<String>()
        for (i in 1..columnCount) {
            columns.add(metaData.getColumnName(i))
        }
        return columns
    }

    fun getTotalRows(filePath: String, searchQuery: String = "", columnFilters: Map<String, Set<String>> = emptyMap()): Long {
        val conn = getConnection()
        val columns = getColumns(filePath)
        val whereClause = buildWhereClause(columns, searchQuery, columnFilters)
        
        val stmt = conn.createStatement()
        val query = "SELECT count(*) FROM read_parquet('$filePath') $whereClause"
        val rs = stmt.executeQuery(query)
        return if (rs.next()) rs.getLong(1) else 0L
    }

    fun getUniqueValues(filePath: String, column: String): List<String> {
        val conn = getConnection()
        val stmt = conn.createStatement()
        val query = "SELECT DISTINCT CAST(\"$column\" AS VARCHAR) as val FROM read_parquet('$filePath') ORDER BY val NULLS LAST LIMIT 1000"
        val rs = stmt.executeQuery(query)
        val values = mutableListOf<String>()
        while (rs.next()) {
            val v = rs.getString("val")
            values.add(v ?: "null")
        }
        return values
    }

    fun getColumnStats(
        filePath: String,
        column: String,
        searchQuery: String = "",
        columnFilters: Map<String, Set<String>> = emptyMap()
    ): Map<String, Any?> {
        val conn = getConnection()
        val allColumns = getColumns(filePath)
        val whereClause = buildWhereClause(allColumns, searchQuery, columnFilters)

        val stmt = conn.createStatement()
        val query = """
            SELECT 
                count("$column") as row_count,
                count(DISTINCT "$column") as unique_count,
                avg(TRY_CAST("$column" AS DOUBLE)) as average,
                sum(TRY_CAST("$column" AS DOUBLE)) as total_sum
            FROM read_parquet('$filePath') $whereClause
        """.trimIndent()

        val rs = stmt.executeQuery(query)
        val stats = mutableMapOf<String, Any?>()
        if (rs.next()) {
            stats["row_count"] = rs.getLong("row_count")
            stats["unique_count"] = rs.getLong("unique_count")
            stats["average"] = rs.getObject("average")
            stats["total_sum"] = rs.getObject("total_sum")
        }
        return stats
    }

    fun exportToCsv(
        filePath: String,
        destinationPath: String,
        searchQuery: String,
        columnFilters: Map<String, Set<String>>,
        visibleColumns: List<String>
    ) {
        val conn = getConnection()
        val allColumns = getColumns(filePath)
        val whereClause = buildWhereClause(allColumns, searchQuery, columnFilters)
        val columnSelection = if (visibleColumns.isEmpty()) "*" else visibleColumns.joinToString(", ") { "\"$it\"" }
        
        val stmt = conn.createStatement()
        // DuckDB's COPY command to export to CSV
        val query = "COPY (SELECT $columnSelection FROM read_parquet('$filePath') $whereClause) TO '$destinationPath' (FORMAT CSV, HEADER)"
        stmt.execute(query)
    }
}
