package com.wiva.android.data.remote.telemetry.mvp.cells

import com.wiva.android.domain.model.TelemetryCell
import com.wiva.android.domain.model.TelemetryCellsSnapshot
import com.wiva.android.domain.model.TelemetryProduct
import com.wiva.android.domain.telemetry.DefaultPhysicalCellSchemaProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryCellsMessageCodecTest {

    private val codec = TelemetryCellsMessageCodec()

    @Test
    fun encodeContentReport_shouldSerializeProductUuidOnlyWithoutDenormalizedFields() {
        val cell =
            TelemetryCell(
                uuid = "550e8400-e29b-41d4-a716-446655440001",
                cellNumber = 1,
                productUuid = "prod-cherry",
                productName = "Вишня",
                tasteMediaKey = "cherry",
                blockVolume = 0,
                sosVolume = 100,
                volume = 1200,
                maxVolume = 5000,
                dosage1Price = 9900,
                dosage2Price = 14900,
            )

        val payloadJson = codec.encodeContentReportPayload(listOf(cell))
        val root = codec.contentReportPayloadObject(payloadJson)
        val firstCell = root["cells"]!!.toString()

        assertTrue(firstCell.contains("\"productUuid\":\"prod-cherry\""))
        assertFalse(firstCell.contains("productName"))
        assertFalse(firstCell.contains("tasteMediaKey"))
    }

    @Test
    fun decodeSnapshotPayload_shouldDeserializeProductsAndDenormalizedCells() {
        val payloadJson =
            """
            {
              "schemaHash": "sha256-hex",
              "contentRevision": 42,
              "products": [
                { "uuid": "prod-cherry", "name": "Вишня", "tasteMediaKey": "cherry" }
              ],
              "cells": [
                {
                  "uuid": "550e8400-e29b-41d4-a716-446655440001",
                  "cellNumber": 1,
                  "productUuid": "prod-cherry",
                  "productName": "Вишня",
                  "tasteMediaKey": "cherry",
                  "blockVolume": 0,
                  "sosVolume": 100,
                  "volume": 1200,
                  "maxVolume": 5000,
                  "dosage1Price": 9900,
                  "dosage2Price": 14900
                }
              ]
            }
            """.trimIndent()

        val snapshot = codec.decodeSnapshotPayload(payloadJson, savedAtEpochMs = 1_700_000_000_000L)

        assertEquals("sha256-hex", snapshot.schemaHash)
        assertEquals(42, snapshot.contentRevision)
        assertEquals(
            listOf(TelemetryProduct("prod-cherry", "Вишня", "cherry")),
            snapshot.products,
        )
        assertEquals(
            TelemetryCell(
                uuid = "550e8400-e29b-41d4-a716-446655440001",
                cellNumber = 1,
                productUuid = "prod-cherry",
                productName = "Вишня",
                tasteMediaKey = "cherry",
                blockVolume = 0,
                sosVolume = 100,
                volume = 1200,
                maxVolume = 5000,
                dosage1Price = 9900,
                dosage2Price = 14900,
            ),
            snapshot.cells.single(),
        )
    }

    @Test
    fun encodeSchemaReportPayload_whenSnapshotExists_includesClientRevisionFields() {
        val snapshot =
            TelemetryCellsSnapshot(
                schemaHash = "local-hash",
                contentRevision = 41,
                products = emptyList(),
                cells = emptyList(),
            )
        val provider = DefaultPhysicalCellSchemaProvider()
        val uuids = provider.physicalCells().associate { it.cellNumber to "uuid-${it.cellNumber}" }

        val payloadJson =
            codec.encodeSchemaReportPayloadFromPhysicalCells(
                physicalCells = provider.physicalCells(),
                uuidsByCellNumber = uuids,
                snapshot = snapshot,
            )

        assertTrue(payloadJson.contains("\"clientSchemaHash\":\"local-hash\""))
        assertTrue(payloadJson.contains("\"clientContentRevision\":41"))
    }

    @Test
    fun encodeVolumeReportPayload_serializesUpdates() {
        val payloadJson =
            codec.encodeVolumeReportPayload(
                listOf(
                    CellVolumeUpdateWire(
                        uuid = "u1",
                        volume = 900,
                        blockVolume = 0,
                        sosVolume = 100,
                    ),
                ),
            )

        assertTrue(payloadJson.contains("\"updates\""))
        assertTrue(payloadJson.contains("\"uuid\":\"u1\""))
        assertTrue(payloadJson.contains("\"volume\":900"))
    }
}
