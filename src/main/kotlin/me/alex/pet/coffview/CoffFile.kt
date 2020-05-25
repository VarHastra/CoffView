package me.alex.pet.coffview

import java.io.*

class CoffFile(private val source: File) {

    private lateinit var file: RandomAccessFile

    private var symbolTableOffset: Long = -1L

    private var numberOfSymbolTableEntries: Long = -1L

    private var stringTableSizeInBytes: Long = -1L

    private val symbolNames = mutableListOf<String>()

    private val stringTableOffset: Long
        get() = symbolTableOffset + numberOfSymbolTableEntries * SYMBOL_TABLE_ENTRY_SIZE_IN_BYTES


    fun read(): CoffDetails {
        file = RandomAccessFile(source, MODE_READ)
        parseCoffFile()
        return CoffDetails(
            symbolTableOffset,
            numberOfSymbolTableEntries,
            stringTableOffset,
            stringTableSizeInBytes,
            symbolNames.toList()
        )
    }

    private fun parseCoffFile() {
        file.use {
            parseHeader()
            parseStringTableSize()
            parseSymbolNames()
        }
    }

    private fun parseHeader() {
        file.seek(8)
        symbolTableOffset = file.read4Bytes()
        numberOfSymbolTableEntries = file.read4Bytes()
    }

    private fun parseStringTableSize() {
        file.seek(stringTableOffset)
        stringTableSizeInBytes = file.read4Bytes()
    }

    private fun parseSymbolNames() {
        file.seek(symbolTableOffset)
        var numOfUpcomingAuxEntries = 0
        for (entryNumber in 0 until numberOfSymbolTableEntries) {
            if (numOfUpcomingAuxEntries > 0) {
                numOfUpcomingAuxEntries--
                continue
            }
            val entry = readEntryBytes(entryNumber)
            numOfUpcomingAuxEntries = readNumOfAuxEntriesFor(entry)
            val symbolName = readSymbolNameFor(entry)
            symbolNames.add(symbolName)
        }
    }

    private fun readEntryBytes(entryNumber: Long): ByteArray {
        val entryBytes = ByteArray(18)
        val entryOffset = computeOffsetForSymbolTableEntry(entryNumber)
        file.seek(entryOffset)
        file.read(entryBytes)
        return entryBytes
    }

    private fun readNumOfAuxEntriesFor(entry: ByteArray): Int {
        return entry[17].toInt() and 0xFF
    }

    private fun readSymbolNameFor(entry: ByteArray): String {
        val lowest4Bytes = entry.read4BytesAt(0)
        return if (lowest4Bytes == 0L) {
            val offsetWithinStringTable = entry.read4BytesAt(4)
            readStringTableEntry(offsetWithinStringTable)
        } else {
            val nonNullBytes = entry.take(8).takeWhile { it != 0.toByte() }.toByteArray()
            return String(nonNullBytes, Charsets.US_ASCII)
        }
    }

    private fun computeOffsetForSymbolTableEntry(entryNumber: Long): Long {
        return symbolTableOffset + entryNumber * SYMBOL_TABLE_ENTRY_SIZE_IN_BYTES
    }

    private fun readStringTableEntry(offsetWithinStringTable: Long): String {
        file.seek(stringTableOffset + offsetWithinStringTable)
        val symbolBytes = ByteArrayOutputStream(16)
        file.transferTo(symbolBytes) { byte, _ -> byte != 0 }
        return symbolBytes.toString(Charsets.US_ASCII.name())
    }
}

private const val SYMBOL_TABLE_ENTRY_SIZE_IN_BYTES = 18

private const val MODE_READ = "r"

private fun RandomAccessFile.read4Bytes(): Long {
    val first = read().toLong()
    val second = read().toLong()
    val third = read().toLong()
    val fourth = read().toLong()
    if ((first or second or third or fourth) < 0) {
        throw IOException()
    }
    return (fourth shl 24) or (third shl 16) or (second shl 8) or first
}

private fun ByteArray.read4BytesAt(offset: Int): Long {
    var result = 0L
    for (i in 0 until 4) {
        result = result or (this.unsignedByteAt(offset + i) shl (8 * i))
    }
    return result
}

private fun ByteArray.unsignedByteAt(index: Int): Long {
    return this[index].toLong() and 0xFF
}

private fun RandomAccessFile.transferTo(outputStream: OutputStream, condition: (Int, Int) -> Boolean) {
    var numOfWrittenBytes = 0
    var b = read()
    while (condition(b, numOfWrittenBytes)) {
        outputStream.write(b)
        numOfWrittenBytes++
        b = read()
    }
}