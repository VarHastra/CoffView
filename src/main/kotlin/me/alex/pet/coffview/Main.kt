package me.alex.pet.coffview

import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    when (args.size) {
        1 -> {
            val file = requirePlainFile(args[0])
            parse(file)
        }
        else -> renderUsagePromptAndQuit()
    }
}

private fun renderUsagePromptAndQuit() {
    println("Usage: java -jar CoffView <filename>")
    exitProcess(1)
}

private fun requirePlainFile(fileName: String): File {
    val file = File(fileName)
    if (!file.isFile || !file.canRead()) {
        println("File $fileName does not exist as a plain file.")
        exitProcess(2)
    }

    return file
}

private fun parse(file: File) {
    val coffFile = CoffFile(file)
    try {
        val details = coffFile.read()
        printDetails(details)
    } catch (e: IOException) {
        println("Oooops... Something went wrong.")
        println("The file has been damaged or has an invalid format.")
        exitProcess(3)
    }
}

private fun printDetails(details: CoffDetails) {
    println("Symbol table offset:     ${details.symbolTableOffset}")
    println("Number of symbols:       ${details.numOfSymbols}")

    println("String table size [B]:   ${details.stringTableSizeInBytes}")
    println("String table offset:     ${details.stringTableOffset}")

    details.symbols.forEachIndexed { index, symbol ->
        println("  $index $symbol")
    }
}