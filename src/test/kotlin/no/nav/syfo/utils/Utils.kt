package no.nav.syfo.utils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

fun getFileAsString(filePath: String) =
    String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
