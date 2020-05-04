package me.alex.pet.coffview

import java.io.*
import kotlin.system.exitProcess

private const val SYMBOL_TABLE_ENTRY_SIZE_IN_BYTES = 18

private const val MODE_READ = "r"

fun main(args: Array<String>) {
    when (args.size) {
        1 -> process(args[0])
        else -> renderUsagePromptAndQuit()
    }
}

private fun renderUsagePromptAndQuit() {
    println("Usage: java -jar CoffView <filename>")
    exitProcess(1)
}

private fun process(fileName: String) {
    val file = File(fileName)
    if (!file.isFile || !file.canRead()) {
        println("File $fileName does not exist as a plain file.")
        exitProcess(2)
    }

    read(file)
}

private fun read(file: File) {
    val raf = RandomAccessFile(file, MODE_READ)
    try {
        readCoffFile(raf)
    } catch (e: IOException) {
        println("Oooops... Something wen't wrong.")
        println("The file has been damaged or has invalid format.")
        exitProcess(3)
    } finally {
        try {
            raf.close()
        } catch (e: IOException) {
            // Ignore the exception
        }
    }
}

private fun readCoffFile(file: RandomAccessFile) {
    file.seek(8)
    val symbolTableOffset = file.read4Bytes()
    val numberOfSymbolTableEntries = file.read4Bytes()
    println("Symbol table offset:     $symbolTableOffset")
    println("Number of symbols:       $numberOfSymbolTableEntries")

    val stringTableOffset = calculateStringTableOffset(symbolTableOffset, numberOfSymbolTableEntries)
    file.seek(stringTableOffset)
    val stringTableSizeInBytes = file.read4Bytes()
    println("String table size [B]:   $stringTableSizeInBytes")
    println("String table offset:     $stringTableOffset")

    println("Symbol table entries:")
    file.seek(symbolTableOffset)
    for (entryNumber in 0 until numberOfSymbolTableEntries) {
        val entryOffset = computeOffsetForSymbolTableEntry(symbolTableOffset, entryNumber)
        file.seek(entryOffset)
        val lowest4Bytes = file.read4Bytes()
        if (lowest4Bytes == 0L) {
            val offsetWithinStringTable = file.read4Bytes()
            file.seek(stringTableOffset + offsetWithinStringTable)
            val symbolBytes = ByteArrayOutputStream(16)
            file.transferTo(symbolBytes) { byte, _ -> byte != 0 }
            println("  $entryNumber ${symbolBytes.toString(Charsets.US_ASCII.name())}")
        } else {
            file.seek(entryOffset)
            val symbolBytes = ByteArrayOutputStream(8)
            file.transferTo(symbolBytes) { byte, numOfWrittenBytes -> byte != 0 && numOfWrittenBytes < 8 }
            println("  $entryNumber ${symbolBytes.toString(Charsets.US_ASCII.name())}")
        }
    }
}

private fun calculateStringTableOffset(symbolTableOffset: Long, numberOfSymbolTableEntries: Long): Long {
    return symbolTableOffset + numberOfSymbolTableEntries * SYMBOL_TABLE_ENTRY_SIZE_IN_BYTES
}

private fun computeOffsetForSymbolTableEntry(symbolTableOffset: Long, entryNumber: Long): Long {
    return symbolTableOffset + entryNumber * SYMBOL_TABLE_ENTRY_SIZE_IN_BYTES
}

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

private fun RandomAccessFile.transferTo(outputStream: OutputStream, condition: (Int, Int) -> Boolean) {
    var numOfWrittenBytes = 0
    var b = read()
    while (condition(b, numOfWrittenBytes)) {
        outputStream.write(b)
        numOfWrittenBytes++
        b = read()
    }
}